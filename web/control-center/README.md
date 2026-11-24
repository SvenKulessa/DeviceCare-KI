# DeviceCare-KI Control Center: private profile + B2B/Enterprise
Implementation status: **repository-only MVP / NOT_PROVEN deployment**. Roadmap Issue [#23](https://github.com/SvenKulessa/DeviceCare-KI/issues/23). Closely related Drive Issue [#21](https://github.com/SvenKulessa/DeviceCare-KI/issues/21) and Draft PR #22.

## What exists
- `iam.mjs`: central server-verifiable role/tenant authorization, strict personal/project/tenant resource boundaries, step-up MFA guard, plan-hash approval guard and a restrictive tenant-IAM grant planner.
- `view.mjs`: server-rendered private profile dashboard and three extra tabs. Requires injected authentication and membership adapters to expose any page. Returns 503 if adapters are absent; 403 on denied access. No fake device results, user identity data, or browser-controlled role selection. UI is responsive; full mobile accessibility QA is pending.
- `control-center.test.mjs`: synthetic positive/negative authorization, XSS and isolation tests. Node built-in test runner; no extra packages and no Google Drive access.
- `../../tools/drive-governance.mjs`: separate no-I/O Drive classifier/scan/archive policy, still PLAN_ONLY.

## Routes and operation scopes
| Private route | Required server-side operation | Audience |
|---|---|---|
| `/profile/control-center` | `personal:view` | device user for their own identity only |
| `/profile/control-center/drive` | `project:drive:view` | project owner, project security analyst or auditor |
| `/profile/control-center/enterprise` | `tenant:dashboard` | members of that exact B2B/enterprise tenant |
| `/profile/control-center/iam` | `tenant:iam:read` | B2B/enterprise tenant administrators and tenant auditors |

No write endpoint exists. All visible metrics default to **NOT_PROVEN**, and become VERIFIED only when the server supplies a validated nonnegative aggregate plus evidence receipt. Never load or show raw file, malware, per-device or identity evidence to unrelated tenants.

## Naming reconciliation (concept ↔ code)
- `platform_owner` → `OWNER` **with project-scoped authoritative membership**.
- `private_owner` → `DEVICE_USER` with personal-scoped membership and own subject ID.
- `tenant_admin` → `B2B_ADMIN` or `ENTERPRISE_ADMIN` with tenant-scoped membership.
- `security_auditor` → `AUDITOR` with tenant/project scope decided by authoritative membership (never global automatically).
- `fleet_operator` → `FLEET_OPERATOR` with tenant-scoped read access only in this MVP.
- `tenant_viewer` → `VIEWER` scoped to a single tenant.
- `agent_service` → a **separate noninteractive service principal**, NOT an IAM user role in this UI. Its authenticated job policy/least privilege and action ledger must be built separately.

The client must never submit a role, a different tenant ID or membership status as authority. The server must derive roles/tenants from a configured and independently verified identity provider and membership database. Tenant admins may propose limited non-admin grants but cannot create owners/admins, grant themselves any role or write across tenants; an actual IAM mutation endpoint is not implemented.

## Integration (only after server readiness)
The web backend attaches Node `createControlCenterHandler({authenticate, membershipsFor, dashboardRead})` under the paths above. All three functions must be real **server-side** adapters, never stubs for production:
- `authenticate(req)`: session validation with audience, issuer, CSRF where relevant, expiry, verified subject ID, server verification flag and step-up signal.
- `membershipsFor(identity)`: database membership for that subject, server-verified roles and active tenant from trusted server-side session; return at most 100 memberships. Never trust query or HTTP role headers.
- `dashboardRead({identity, tenantId, view, operation})`: data reads under the **same server-side authorization** and tenant row-level policy, not merely a UI check. Return `{status: "VERIFIED", evidenceReceipt, value: 0|integer}` only for measured aggregate counts; otherwise return null.

For Drive, the owner-designated root has already been renamed to `DeviceCare-KI` in Google Drive with metadata readback. Keep the root ID in *private backend configuration* and ensure the service can only list descendants of that root. Historical AI-DeviceCare content is labeled legacy, never automatically promoted to current project evidence. No other project's Drive root is whitelisted.

## Still required before production
1. Independently verified IAM/OIDC provider, per-tenant membership store, authorized backend host and session hardening. No client-side fake auth.
2. Durable job lease, per-day Europe/Berlin idempotency, manual STOP/WAIT_OWNER persistence, bounded retries, and trigger execution, separately verified.
3. Real authorized malware, secret, license, privacy and integrity scanning of file bytes including Google-native conversions. No scan verdict if a provider is missing.
4. Archive ZIP + SHA manifest + test restore + legal hold checks + evidence receipts; originals retained.
5. RBAC/RLS negative and penetration tests, accessibility QA for 320/390 widths, backup/rollback, cost/quota approval and exact production build-identity readback.

### Local test command
```sh
node --check web/control-center/iam.mjs
node --check web/control-center/view.mjs
node --test tools/drive-governance.test.mjs web/control-center/control-center.test.mjs
```

### Rollback
Revert Draft PR #22; no Drive mutations, Android permissions, user accounts, tenant data, identity settings, deployments or production scheduler have been changed by this code.
