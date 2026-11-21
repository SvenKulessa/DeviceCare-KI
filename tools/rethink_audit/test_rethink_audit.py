"""Synthetic-only tests: no customer backup or network access."""
import io
import json
from pathlib import Path
import sqlite3
import tempfile
import unittest
import zipfile

from rethink_audit import audit, compare_baseline, inspect_sqlite, main, MAX_SQLITE

SECRET = "NEVER_PRINT_PRIVATE_KEY_123456"


def make_db(*, config=True, count=1):
    conn = sqlite3.connect(":memory:")
    if config:
        conn.execute("CREATE TABLE AppInfo (id INTEGER PRIMARY KEY, private_notes TEXT)")
        conn.execute("CREATE TABLE CustomDomain (domain TEXT)")
        for _ in range(count):
            conn.execute("INSERT INTO CustomDomain(domain) VALUES (?)", (SECRET,))
        conn.execute("INSERT INTO AppInfo(private_notes) VALUES (?)", (SECRET,))
    else:
        conn.execute("CREATE TABLE ConnectionTracker (private_host TEXT)")
        conn.execute("INSERT INTO ConnectionTracker VALUES (?)", (SECRET,))
    value = conn.serialize()
    conn.close()
    return value


def backup(path: Path, *, count=1, wg=True, corrupt=False, logs=False):
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("bravedns.db", b"SQLite format 3\x00BROKEN" if corrupt else make_db(count=count))
        if wg:
            archive.writestr("wireguard/2.conf", "[Interface]\nPrivateKey = " + SECRET)
        if logs:
            archive.writestr("rethink_logs.db", make_db(config=False))


class RethinkAuditTests(unittest.TestCase):
    def test_inventory_and_redaction(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.rbk"
            backup(p, logs=True)
            result = audit(p)
            self.assertEqual(result["schema"], "rethink-audit-v2")
            self.assertEqual(result["policy_version"], "1.0")
            self.assertEqual(result["databases"]["config"]["counts"]["custom_domain_rules"], 1)
            self.assertEqual(result["databases"]["config"]["profile"], "UNVERIFIED_SCHEMA_SEMANTICS")
            self.assertEqual(result["databases"]["logs"]["status"], "OK")
            self.assertEqual(result["live_state"], "NOT_PROVEN")
            self.assertEqual(result["wireguard_profiles_in_archive"], 1)
            self.assertNotIn(SECRET, json.dumps(result))

    def test_missing_config_is_not_proven(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "empty.rbk"
            with zipfile.ZipFile(p, "w") as archive:
                archive.writestr("readme.txt", "no useful config")
            self.assertEqual(audit(p)["policy_checks"][0]["state"], "NOT_PROVEN")

    def test_invalid_zip_fails_closed(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "bad.rbk"
            p.write_bytes(b"not zip")
            with self.assertRaises(ValueError):
                audit(p)
            self.assertEqual(main([str(p), "--out", str(Path(d) / "result.json")]), 2)
            self.assertFalse((Path(d) / "result.json").exists())

    def test_duplicate_critical_entries_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "duplicate.rbk"
            with zipfile.ZipFile(p, "w") as archive:
                archive.writestr("bravedns.db", make_db())
                archive.writestr("bravedns.db", make_db())
            with self.assertRaisesRegex(ValueError, "Duplicate"):
                audit(p)

    def test_zip_bomb_detected_without_extraction(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "bomb.rbk"
            with zipfile.ZipFile(p, "w", zipfile.ZIP_DEFLATED) as archive:
                archive.writestr("other.dat", b"X" * 2_000_000)
            with self.assertRaisesRegex(ValueError, "compression ratio"):
                audit(p)

    def test_corrupt_sqlite_is_not_proven(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "corrupt.rbk"
            backup(p, corrupt=True)
            self.assertNotEqual(audit(p)["databases"]["config"]["status"], "OK")

    def test_baseline_v1_to_v2_drift(self):
        with tempfile.TemporaryDirectory() as d:
            first, second = Path(d) / "1.rbk", Path(d) / "2.rbk"
            backup(first, count=1)
            backup(second, count=3)
            old = audit(first)
            old["schema"] = "rethink-audit-v1"
            baseline = Path(d) / "baseline.json"
            baseline.write_text(json.dumps(old))
            current = audit(second)
            compare_baseline(current, baseline)
            self.assertEqual(current["drift_count_delta"], {"custom_domain_rules": 2})
            self.assertEqual(current["baseline_schema"], "rethink-audit-v1")
            self.assertNotIn(SECRET, json.dumps(current))

    def test_invalid_baseline_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.rbk"
            backup(p)
            old = Path(d) / "baseline.json"
            old.write_text(json.dumps({"schema": "rethink-audit-v2", "databases": {"config": {"status": "OK", "counts": {"custom_domain_rules": -1}}}}))
            with self.assertRaises(ValueError):
                compare_baseline(audit(p), old)

    def test_output_private_and_cli_success(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.rbk"
            backup(p)
            out = Path(d) / "private" / "result.json"
            self.assertEqual(main([str(p), "--out", str(out)]), 0)
            self.assertEqual(out.stat().st_mode & 0o777, 0o600)
            self.assertNotIn(SECRET, out.read_text())

    def test_symlink_destination_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.rbk"
            backup(p)
            target = Path(d) / "target.json"
            target.write_text("important")
            out = Path(d) / "out.json"
            out.symlink_to(target)
            self.assertEqual(main([str(p), "--out", str(out)]), 2)
            self.assertEqual(target.read_text(), "important")

    def test_symlinked_report_parent_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.rbk"
            backup(p)
            private = Path(d) / "private"
            private.mkdir()
            redirected = Path(d) / "shared"
            redirected.symlink_to(private, target_is_directory=True)
            out = redirected / "report.json"
            self.assertEqual(main([str(p), "--out", str(out)]), 2)
            self.assertFalse((private / "report.json").exists())

    def test_backup_cannot_be_overwritten_by_report(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "sample.rbk"
            backup(p)
            original = p.read_bytes()
            self.assertEqual(main([str(p), "--out", str(p)]), 2)
            self.assertEqual(p.read_bytes(), original)

    def test_invalid_database_header(self):
        self.assertEqual(inspect_sqlite(b"invalid", config=True)["status"], "INVALID_SQLITE_HEADER")

    def test_database_over_size(self):
        self.assertEqual(inspect_sqlite(b"X" * (MAX_SQLITE + 1), config=True)["status"], "SKIPPED_TOO_LARGE")


if __name__ == "__main__":
    unittest.main()
