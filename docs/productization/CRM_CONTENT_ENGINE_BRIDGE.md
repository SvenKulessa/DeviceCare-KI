# Device-Care-KI | CRM & Content Engine Bridge (DRAFT)

Stand: 2026-11-22. Owner scope: Device-Care-KI. Local repository AGENTS.md is authoritative. Cross-project marketing *metadata* handoff explicitly requested by owner, without provider or authority sharing.

## Operational slice

1. Device-Care-KI exports only explicitly owner-prepared **public product descriptions** using tools/productization/export_public_product_candidate.py. Never export hardware diagnostics, PII, app inventories, sensitive device evidence, logs or customer identities.
2. An owner-reviewed JSON document (PRODUCT_SALES_CANDIDATE@1) can be copied deliberately into the corresponding Drive Sales_Enablement folder. The local exporter never connects to Drive, GitHub, Salesforce, or CAPITAL-AI.
3. On the CAPITAL-AI side, src/contracts/growthSalesCrm.ts accepts the sanitized envelope and returns CRM-neutral internal opportunity and marketing drafts. It does NOT create a live Salesforce record.
4. Device-Care-KI branding is distinct. The CAPITAL-AI Content Engine's seven-color Universe identity is not reused as DeviceCare creative without an explicit branded adapter and licensing checks. The bridge returns DEVICECARE_BRAND_ADAPTER_PENDING.
5. Export outputs remain NOT_PROVEN until read back from the chosen local file, Drive transfer remains NOT_PROVEN until verified, CRM connection NOT_CONNECTED, commercial release HOLD.

## Sample input fields

Only productId, productName, valueProposition, audience, differentiators, channels, sourceEvidenceRef, rightsStatus, optional rightsEvidenceRef, canonicalUrl, economics. All other keys are refused. This is product metadata, not a real lead, customer, diagnosis, or validated financial projection.

Prepare reviewed input JSON yourself in a non-public working directory and invoke:

~~~shell
python3 -m unittest discover -s tools/productization -p 'test_*.py'
python3 tools/productization/export_public_product_candidate.py --input /path/to/reviewed-product.json --output /path/to/DEVICECARE_PRODUCT_SALES_CANDIDATE_v1.json
~~~

The file is exclusive-created with mode 0600, no overwrite or silent directory creation. Inspect/redact the output before separately transferring to Drive.

## CRM field plan

Product → CRM catalog/custom product mapping after provider connection and permissions check.
Opportunity → not created without verified account, contact lawful basis, StageName, CloseDate, consent, and explicit approved scope. No synthetic pipeline, deal amount or customer count.
Sales enablement → landing page copy, pricing hypothesis, visual brief, license proof, product facts, support/FAQ and risk register.
Graphics → separate original asset generation with exact SHA-256, source/license evidence, alt text, approved usage modes, editorial QA. No graphic bytes or brand grant currently proven.
Metrics → opportunity conversion, attributed campaign traffic, cost per qualified lead, contribution margin. No actual KPIs are inferred from a draft.

## Acceptance and rollback

Tests: stdlib unit/negative tests for privacy, unsafe URLs, rights, deterministic SHA-256 and economic unknowns. No Android execution. Rollback: remove only the feature-branch files, preserve other app code and backups. No credential or backend changes.


## Owner decision: Twenty CRM selected (2026-11-22)

Provider: Twenty (self-hosted open-source core preferred; paid cloud is not automatically activated). No Salesforce sandbox is required by Device-Care-KI, and no CAPITAL-AI provider credential, runtime or Salesforce adapter is imported. Any separate CAPITAL-AI CRM decision is independent.

Twenty has built-in People, Companies and Opportunities; **Products is a custom workspace object** and MUST NOT be assumed to exist. The owner configures this **in the dedicated Device-Care-KI workspace** through Twenty Settings -> Data Model, with exact API plural name `products` (singular Product), scalar text `name` label plus custom text fields `externalKey` and `sourceSha256`. Record identity is a namespaced `externalKey=DEVICE_CARE_KI:<productId>` (e.g. DEVICE_CARE_KI:device-health). Validate exact field API names against the authenticated workspace's generated API documentation before first call. Prefer a unique constraint on externalKey where supported; the adapter rejects any duplicate return.

Readback implementation:
- `tools/productization/twenty_readonly.py` consumes exclusively a previously reviewed PRODUCT_SALES_CANDIDATE@1 with `PUBLIC_PRODUCT_METADATA` classification. It calls **only GET** on `https://<owner-configured-host>/rest/products` with `fields=id,name,externalKey,sourceSha256`, `limit=2` and a strict exact-match filter. It rejects unsafe origins, credentials in URLs, redirects, unknown fields, mismatched project identity, truncated/ambiguous results, mismatched source digest and malformed UUIDs.
- Local readback result includes the verified record identity and deterministic SHA-256 of the **minimal public product projection**, never People, Company, Opportunity, device diagnostics or provider token data.
- No Twenty product, opportunity, note, asset attachment, custom schema, API key, permission or CRM record is ever created by this adapter.
- Marketing assets require **separately verified owner approval, exact content bytes SHA-256, license/rights evidence and editorial QA**. Even a product match yields `HOLD_EDITORIAL_RIGHTS_AND_ASSET_PROOF`; it is not publication or commercial authorization.
- API auth: dedicated Twenty read-only API key with object-level `Products:read` only, created by owner in Settings -> API & Webhooks and scoped in Members -> Roles. Exact availability of field-/row-level permissions depends on chosen Twenty version/plan; verify and fail closed if unavailable. Never paste the key into chat, Drive, PR, CI logs or mobile diagnostic logs.

Reproduce (from DeviceCare-KI repository root):
```sh
python3 -m unittest discover -s tools/productization -p 'test_*.py'
TWENTY_BASE_URL=https://crm.example.invalid \
TWENTY_READONLY_API_KEY=<secret-in-owner-protected-runtime> \
python3 tools/productization/twenty_readonly.py --input /private/reviewed-product-candidate.json
```
Do **not** use the example host as a real endpoint. Set environment variables through a protected owner-controlled runtime; never write raw secrets in shell history. The example above is syntax only, not a real connection.

Official Twenty reference: https://docs.twenty.com/developers/extend/api ; https://docs.twenty.com/getting-started/core-concepts/data-model .
The first live Twenty readback remains `NOT_PROVEN` until the workspace is provisioned, the read-only role has been independently confirmed, and a real provider response is checked. Local tests are synthetic. Existing Android CI already runs all `tools/productization/test_*.py` via the existing test step; no new required CI gate. Rollback: revert this scoped feature branch; no CRM/device state changed.
