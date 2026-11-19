#!/usr/bin/env python3
"""Offline read-only Rethink backup audit. Never write into Rethink or print private values."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import sqlite3
import sys
import tempfile
import zipfile

REPORT_SCHEMA = "rethink-audit-v2"
POLICY_VERSION = "1.0"
MAX_BACKUP = 256 * 1024 * 1024
MAX_TOTAL_UNCOMPRESSED = 512 * 1024 * 1024
MAX_SQLITE = 48 * 1024 * 1024
MAX_MEMBERS = 2048
MAX_BASELINE = 1024 * 1024
MAX_RATIO = 200
APP_TABLES = {
    "AppInfo": "tracked_apps",
    "CustomIp": "custom_ip_rules",
    "CustomDomain": "custom_domain_rules",
    "DoHEndpoint": "doh_endpoints",
    "DoTEndpoint": "dot_endpoints",
    "DnsCryptEndpoint": "dnscrypt_endpoints",
    "DnsProxyEndpoint": "dns_proxy_endpoints",
    "ProxyEndpoint": "proxy_endpoints",
    "ProxyApplicationMapping": "app_proxy_assignments",
    "WgConfigFiles": "wireguard_configs_in_db",
    "WgHopMap": "wireguard_multihop_mappings",
    "RethinkDnsEndpoint": "rethink_dns_endpoints",
    "RethinkLocalFileTag": "local_blocklists",
    "RethinkRemoteFileTag": "remote_blocklists",
}
DATA_NAMES = ("bravedns.db", "rethink_logs.db")
SECTIONS = DATA_NAMES + ("rethink_backup.txt", "shared_prefs_backup.txt")
WG_PATTERN = re.compile(r"(?:^|/)wireguard/[^/]+\.conf$", re.I)


def inspect_sqlite(raw: bytes, *, config: bool) -> dict:
    """In-memory and query-only; table names come from a fixed allowlist."""
    if len(raw) > MAX_SQLITE:
        return {"status": "SKIPPED_TOO_LARGE"}
    if not raw.startswith(b"SQLite format 3\x00"):
        return {"status": "INVALID_SQLITE_HEADER"}
    conn = sqlite3.connect(":memory:")
    try:
        if not hasattr(conn, "deserialize"):
            return {"status": "UNSUPPORTED_SQLITE_DESERIALIZE"}
        conn.deserialize(raw)
        conn.execute("PRAGMA query_only=ON")
        # Bounded instruction budget for untrusted SQLite databases.
        budget = [0]
        def progress() -> int:
            budget[0] += 1
            return int(budget[0] > 10000)
        conn.set_progress_handler(progress, 1000)
        check = conn.execute("PRAGMA quick_check(1)").fetchone()
        if not check or check[0] != "ok":
            return {"status": "DATABASE_INTEGRITY_FAILED"}
        version = int(conn.execute("PRAGMA user_version").fetchone()[0])
        tables = {row[0] for row in conn.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        result = {"status": "OK", "schema_user_version": version}
        if config:
            result["counts"] = {
                label: int(conn.execute('SELECT COUNT(*) FROM "' + table + '"').fetchone()[0])
                for table, label in APP_TABLES.items() if table in tables
            }
            # PRAGMA user_version does not establish Rethink application version.
            result["profile"] = "UNVERIFIED_SCHEMA_SEMANTICS"
        else:
            result["table_count"] = len(tables)
        return result
    except (sqlite3.Error, MemoryError, OverflowError):
        return {"status": "DATABASE_READ_ERROR"}
    finally:
        conn.close()


def _check_archive(infos: list[zipfile.ZipInfo]) -> None:
    if len(infos) > MAX_MEMBERS:
        raise ValueError("Too many archive entries")
    if sum(i.file_size for i in infos) > MAX_TOTAL_UNCOMPRESSED:
        raise ValueError("Uncompressed archive limit exceeded")
    if any(i.flag_bits & 1 for i in infos):
        raise ValueError("Encrypted archive entries are not supported")
    if any(i.file_size > max(1, i.compress_size) * MAX_RATIO and i.file_size > 1024 * 1024 for i in infos):
        raise ValueError("Suspicious compression ratio")
    if any(sum(i.filename == name for i in infos) > 1 for name in SECTIONS):
        raise ValueError("Duplicate recognized backup entry")


def audit(path: str | Path) -> dict:
    src = Path(path)
    if not src.is_file():
        raise ValueError("Backup file is missing")
    size = src.stat().st_size
    if size > MAX_BACKUP:
        raise ValueError("Backup file exceeds local resource budget")
    if not zipfile.is_zipfile(src):
        raise ValueError("Not a ZIP-structured Rethink backup")
    output = {
        "schema": REPORT_SCHEMA,
        "policy_version": POLICY_VERSION,
        "collected_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "mode": "LOCAL_OFFLINE_READ_ONLY",
        "live_state": "NOT_PROVEN",
        "source_size_bytes": size,
        "secret_values_exported": False,
        "databases": {},
        "policy_checks": [],
    }
    with zipfile.ZipFile(src) as archive:
        infos = archive.infolist()
        _check_archive(infos)
        names = {info.filename: info for info in infos if info.filename in SECTIONS}
        wg_count = sum(bool(WG_PATTERN.search(info.filename)) for info in infos)
        output["wireguard_profiles_in_archive"] = wg_count
        output["backup_may_contain_private_keys"] = wg_count > 0 or "bravedns.db" in names
        output["sections_present"] = {name: name in names for name in SECTIONS}
        for name, kind in (("bravedns.db", "config"), ("rethink_logs.db", "logs")):
            info = names.get(name)
            if info is None:
                status = {"status": "MISSING"}
            elif info.file_size > MAX_SQLITE:
                status = {"status": "SKIPPED_TOO_LARGE"}
            else:
                try:
                    status = inspect_sqlite(archive.read(info), config=(kind == "config"))
                except (RuntimeError, zipfile.BadZipFile, EOFError, OSError):
                    status = {"status": "UNREADABLE_ARCHIVE_ENTRY"}
            output["databases"][kind] = status
    config_ok = output["databases"]["config"]["status"] == "OK"
    output["policy_checks"] = [
        {"id": "RT-CONFIG-001", "state": "PASS" if config_ok else "NOT_PROVEN", "detail": "Configuration database integrity (offline only)"},
        {"id": "RT-WG-001", "state": "REVIEW" if wg_count else "NOT_APPLICABLE", "detail": "Original backup is sensitive if WireGuard .conf entries are included"},
        {"id": "RT-LIVE-001", "state": "NOT_PROVEN", "detail": "Effective firewall rules, active DNS, always-on VPN and WireGuard status require device-side verification"},
    ]
    return output


def _counts_from_baseline(baseline: dict) -> dict[str, int]:
    if baseline.get("schema") not in ("rethink-audit-v1", REPORT_SCHEMA):
        raise ValueError("Unsupported baseline report schema")
    cfg = baseline.get("databases", {}).get("config", {})
    if cfg.get("status") != "OK":
        raise ValueError("Baseline did not inventory a valid config database")
    counts = cfg.get("counts")
    if not isinstance(counts, dict) or any(
        key not in APP_TABLES.values() or type(val) is not int or val < 0
        for key, val in counts.items()
    ):
        raise ValueError("Invalid baseline aggregate counts")
    return counts


def compare_baseline(result: dict, path: Path) -> None:
    if path.stat().st_size > MAX_BASELINE:
        raise ValueError("Baseline report is too large")
    baseline = json.loads(path.read_text(encoding="utf-8"))
    before = _counts_from_baseline(baseline)
    config = result["databases"]["config"]
    if config["status"] != "OK":
        raise ValueError("Current config database cannot be compared")
    after = config["counts"]
    result["baseline_schema"] = baseline["schema"]
    result["drift_count_delta"] = {
        k: after[k] - before[k] for k in sorted(before.keys() & after.keys()) if before[k] != after[k]
    }
    result["drift_missing_categories"] = sorted(before.keys() - after.keys())
    result["drift_new_categories"] = sorted(after.keys() - before.keys())
    result["policy_checks"].append({
        "id": "RT-DRIFT-001", "state": "REVIEW" if result["drift_count_delta"] or result["drift_missing_categories"] or result["drift_new_categories"] else "PASS",
        "detail": "Aggregate inventory drift; effective configuration changes are NOT_PROVEN",
    })


def _atomic_private_write(destination: Path, value: dict) -> None:
    destination.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    if destination.is_symlink() or not destination.parent.is_dir():
        raise ValueError("Unsafe destination")
    # Create in the same directory and replace only after the full JSON is written.
    fd, tmp = tempfile.mkstemp(prefix=".rethink-audit-", dir=destination.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as output:
            json.dump(value, output, indent=2, ensure_ascii=False)
            output.write("\n")
            output.flush()
            os.fsync(output.fileno())
        os.chmod(tmp, 0o600)
        os.replace(tmp, destination)
    finally:
        if os.path.exists(tmp):
            os.unlink(tmp)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Private read-only Rethink backup security inventory")
    parser.add_argument("backup", help="Locally exported private .rbk archive")
    parser.add_argument("--out", required=True, help="Private JSON report path")
    parser.add_argument("--baseline", help="Earlier sanitized v1/v2 report")
    args = parser.parse_args(argv)
    # Never let a report replace a source backup or its comparison baseline.
    output_path = Path(args.out).resolve()
    if output_path in {Path(args.backup).resolve(), *( [Path(args.baseline).resolve()] if args.baseline else [] )} or output_path.suffix.lower() in {".rbk", ".rbka"}:
        print("Rethink audit failed: unsafe report destination", file=sys.stderr)
        return 2
    old_mask = os.umask(0o077)
    try:
        result = audit(args.backup)
        if args.baseline:
            compare_baseline(result, Path(args.baseline))
        _atomic_private_write(Path(args.out), result)
        print("Rethink audit: report created; live configuration NOT_PROVEN")
        return 0
    except (OSError, ValueError, TypeError, json.JSONDecodeError, zipfile.BadZipFile, RuntimeError):
        # Do not echo paths or untrusted archive text into CI/logs.
        print("Rethink audit failed: invalid or unsupported input", file=sys.stderr)
        return 2
    finally:
        os.umask(old_mask)


if __name__ == "__main__":
    sys.exit(main())
