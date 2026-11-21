# Rethink Security Audit Kit — DeviceCare-KI

**Scope:** PRODUCT / TRUST evidence collector, v0.2 (report `rethink-audit-v2`, policy `1.0`). It is an optional **offline CLI** used with a user-initiated Rethink `.rbk` export. It is **not** part of the Android APK, cannot run unattended in Rethink, and does not change system settings.

## Privacy and capability boundary

- Requires Python 3.11+ with `sqlite3.Connection.deserialize` on AcodeX/Termux or a workstation. No root, ADB, AccessibilityService, network access or Firebase.
- Reads `bravedns.db` and `rethink_logs.db` from the ZIP **in memory**; never extracts `.conf`, private keys, logs, app identities, domain names, or databases to disk.
- Emits aggregate record counts, SQLite `PRAGMA user_version`, schema profile `UNVERIFIED_SCHEMA_SEMANTICS`, an offline integrity status and policy checks. **Row counts are not the effective firewall configuration.** In particular, active DNS/WireGuard state, Android Always-on VPN, proxy lockdown, leak-test results and malware verdicts are **NOT_PROVEN**.
- A backup may contain WireGuard private keys and other secrets. Store the original locally with user-controlled protection; never commit, attach, upload to Drive shared mirror, or send to Firebase/AI services. Report is also private by default, mode `0600`.
- Archive limits: 256 MiB input, 512 MiB total uncompressed, 48 MiB per SQLite DB, 2048 members, suspicious compression-ratio rejection, duplicate and encrypted-entry rejection. These are mobile resource budgets, not claims that all real backups will fit.

## Local execution

```sh
umask 077
python3 tools/rethink_audit/rethink_audit.py "$HOME/private/rethink-export.rbk" \
  --out "$HOME/private/rethink-audit.json"

# Drift against prior v1 or v2 redacted report
python3 tools/rethink_audit/rethink_audit.py "$HOME/private/rethink-export-new.rbk" \
  --baseline "$HOME/private/rethink-audit.json" \
  --out "$HOME/private/rethink-audit-new.json"
```

Exit code **0** means a report was safely generated, *not* that configuration is good. Exit **2** means unusable input, untrusted baseline or unsafe output destination. `policy_checks[*].state` is one of `PASS`, `REVIEW`, `NOT_PROVEN`, `NOT_APPLICABLE`. No automatic remediations.

## Versioning, negative tests and GitHub CI

`schema = rethink-audit-v2`, `policy_version = 1.0`. Policy v1 checks only offline evidence (SQLite integrity, backup-secret exposure and limits). Source `PRAGMA user_version` is informational, **not** an assertion of compatibility with a Rethink app release. Unknown or modified backing schemas are handled with inventory-only semantics. v1 reports may be used for *aggregate-only* drift comparisons.

```sh
python3 -m unittest discover -s tools/rethink_audit -p 'test_*.py' -v
```

The repository's existing `.github/workflows/android-ci.yml` retains its Android/Trivy/APK gates and adds an isolated `rethink-audit` Python test job. CI runs **only synthetic data**. `gitignore` prevents accidental staging of `.rbk`, `.rbka`, `.pcap` and `.pcapng` exports. No Play, Firebase or app permissions change.

## Rollback and acceptance

Rollback: revert the specific PR commit, leaving the Android binary untouched. User-device acceptance requires a local manually exported `.rbk`, confirmation of report redaction, and interactive validation of UI features and network connectivity. Any real-device backup stays off GitHub and cloud. A future native Rethink bridge requires explicit user consent, authenticated field-limited APIs, strong permission boundaries, tests, and new approval.
