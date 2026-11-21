# DeviceCare-KI: Drive Deep-Scan governance contract (proposal)
Status: **NOT_PROVEN** · Issue [#21](https://github.com/SvenKulessa/DeviceCare-KI/issues/21) · Implemented: deterministic, offline, no-I/O planner only.

## Scope verified 2026-11-21
The earlier owner-provided Branding folder `1z5yoQYYYgecC8nobYtyy0CNKNYKDTf8s` is `CAPITAL-AI/10_Product/Branding`, **not DeviceCare-KI**. Its direct child listing returned no results. **Never** use it as a DeviceCare-KI source, destination, training source, evidence pool or audit inheritance unless the owner explicitly authorizes a cross-project integration with separate scopes. A separate DeviceCare-KI project-owned root has now been verified: `1qeqbDBwTsQJ_zUrcWJa3Q8DQYXIlcI89`, title `DeviceCare-KI`, private, with four direct children `PLATFORM`, `PRODUCT`, `TRUST`, `90 ZIP-Archiv`. This is a metadata/ownership verification, **not** evidence of configured OAuth for the future web backend, full descendant inventory or clean security scans.

The repository currently contains one Android `:app` module and no running web backend/scheduler. The code in `tools/drive-governance.mjs` is a pure policy component, not an activated cloud scan. Neither device state nor Drive security state was measured.

## Daily server-side web integration (not yet present)
- Owner chooses a **separate DeviceCare-KI-owned Drive root**; verify root ID, ancestry, OAuth principal, destination folder allowlist and RBAC on each run and before each write.
- Integrate into a real authenticated backend with exactly one **Europe/Berlin local calendar** job per day. Recommended nominal time: 03:00 Europe/Berlin. Durable compare-and-set calendar key + distributed lease + bounded retries are mandatory to prevent overlapping runs, especially at DST changes. Never rely only on an in-process timer.
- The job enumerates only descendants of the validated root, paginates at <=500 files per batch, bounds bytes and archive time, captures revision/ETag before updates, and never follows contents as agent commands.
- PRODUCT produces classification from verifiable file **contents** and checked references (not filename alone), with explicit confidence, content digest, date/topic/version, intended destination, license/right provenance and a uniqueness check.
- TRUST invokes **real, authorized security adapters**, always per run: malware engine with current signatures (e.g., an isolated licensed ClamAV worker if the deployment verifies its availability), secret detector, privacy review, licensing review, integrity hashing. Native Google editor files require separate export and applicable provider-specific checks; missing scan evidence is HOLD and cannot be labelled malware-free. Scan bytes locally in an isolated worker; do not log content, secret matches, tokens or PII.
- Independent QUALITY validates outputs, duplicate names, references, retention/holds, source/destination ancestry, archive restore receipt and the final Drive metadata readback.
- Default `PLAN_ONLY` / no write. Applying `RENAME` or `MOVE` requires a configured owner consent policy and matching current revision + content digest. `ARCHIVE_COPY_COMPRESSED` requires **>45 full days since verified last activity**, zero live references, no legal/retention hold, reliable reversible ZIP, signed/hashed manifest and test restore. Preserve the original until explicit retention decision. Compression may increase size for JPEG/MP4/PDF; skip if not useful, without losing the original.
- Never use server-side broad Drive searches outside the authorized root, never access another product's Drive roots, and never grant permissions to the mobile app for global Drive scans.

## State transitions and manual stop
```text
DISABLED --(verified integration)--> READY
READY --(daily lease acquired)--> SCANNING --> PLAN_ONLY
PLAN_ONLY --(policy/owner permits)--> APPLYING --> VERIFYING --> DONE
                        |               |
                        v               v
                    WAIT_OWNER       BLOCKED
                        |               |
              explicit approval       revalidation
                        \               /
                         ---> REVALIDATE
OWNER_STOP (from any state) -> STOPPED (no automatic resume)
```
- `WAIT_OWNER` must persist a checkpoint, plan digest, specific file IDs and **redacted** risk summary; pause processing and all writes until explicit approve/deny/skip or stop.
- Approvals are authenticated, time-limited and bound to tenant, plan hash, file IDs and revisions; no prompt may self-approve.
- `BLOCKED` includes failing security gates, missing permissions, stale hash or unsafe destination. `HOLD` includes no authenticated backend/Drive root/scanner.
- `STOPPED` remains stopped across restarts. `REVALIDATE` must rerun every security/scope/inactivity check before continuing. Failed backup/restore is never auto-retried as a write.
- Each action requires before/after file ID + parent/name readback, a reversible change ledger, minimal hashed/redacted receipts and explicit `NOT_PROVEN` for absent evidence.

## Verification
Local policy-only test command: `node --test tools/drive-governance.test.mjs`. Tests are synthetic and cannot prove Google Drive reads, malware scan, archive export, compression safety, OAuth or deployed scheduling. The Android CI, test device QA, production deployment and connector integration remain independent gates.
Rollback: revert this branch/PR. No database migrations, OAuth secrets, live scanning or file writes were introduced.

## Control Center backend and enterprise dashboard design (2026-11-21)
The owner authorizes **design and integration into the DeviceCare-KI Control Center** with separate private-profile dashboard tabs and future enterprise/B2B views. This is NOT authorization to share private Drive data with customer tenants.

### Verified Drive folder boundary
- Root ID: `1qeqbDBwTsQJ_zUrcWJa3Q8DQYXIlcI89`, name `DeviceCare-KI` (already correct; no rename needed).
- Direct folders: PLATFORM `1CIDAXoUfmjOQlWMm-MFNXrX7a5TSyEyI`, PRODUCT `1s7WbD1rLHIt_krWijN8kleztD18kHjrm`, TRUST `1OZpjFRhAEhYofuTZ854mKAyh5PUsja1i`, ZIP archive `1UnN6eYN0JkepuOXGY5Gp5HH7Wvt68BEW`.
- These are **observed identifiers**, not automatic move destinations. Subfolders, archive rules and content-based taxonomy must be verified before file mutations.
- No Capital-AI folder is part of the allowlist. No shared OAuth principal or storage authority is inferred.

### Proposed web Control Center tabs
1. **Private / My Devices**: on-demand device diagnostics and privacy reports; explicit opt-in and user-specific access.
2. **Private / Drive Hygiene**: scan inventory, content classification, candidate rename/move/archive, redacted risk findings, dry-run diff, approvals, restore receipts.
3. **Private / SOLO2 Agent Runs**: daily schedule, checkpoint, queue, `WAIT_OWNER` decisions, STOP/RESUME with new evidence, signed audit.
4. **Enterprise / Fleet Posture**: tenant-scoped device compliance aggregates and enrolled endpoints only.
5. **Enterprise / IAM & Audit**: tenant users, RBAC, least-privilege policy, audit and retention controls. No cross-tenant raw scans.

### IAM contract
- `platform_owner`: DeviceCare-KI root configuration, approval policies and emergency stop, but no unlogged bypass of security gates.
- `private_owner`: own files/devices, personal run approvals and audit only.
- `tenant_admin`: own tenant membership, policies and fleet summaries; **no access** to owner's personal Drive root.
- `security_auditor`: redacted, tenant-scoped security evidence; no write/approval privilege.
- `fleet_operator`: tenant-scoped scan requests and fleet views; no global Drive root, IAM edits or destructive actions.
- `tenant_viewer`: read-only tenant aggregates, never private files.
- `agent_service`: short-lived, root-scoped delegated token, policy-bound actions only; cannot approve its own plan.
- Enforce server-side tenant_id, user_id, resource ownership, RLS/ABAC and negative cross-tenant tests; hiding UI tabs alone is not authorization. Private data must never be copied to tenant storage by default.

### Execution prerequisites
- Real backend implementation and authenticated Control Center deployment are **NOT_PROVEN**.
- Daily 03:00 Europe/Berlin scheduler remains **HOLD** until persistent lease, per-day idempotency, OAuth scopes, rate/cost limits and verified security workers exist.
- Malware scan of Drive content remains **NOT_PROVEN**; a metadata listing is not a malware scan.
- Do not change Android M0/M1 permission boundaries or connect another project's infrastructure.
