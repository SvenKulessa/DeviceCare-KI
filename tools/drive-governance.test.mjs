import test from "node:test";
import assert from "node:assert/strict";
import { Status, dailyAdmission, dayKey, evaluateFile, planBatch } from "./drive-governance.mjs";

const sha = "a".repeat(64);
const good = () => ({
  file: {
    id: "file-1", name: "old-logo.png", mimeType: "image/png",
    parentId: "scope-1", projectRootId: "scope-1", ancestryVerified: true,
    modifiedTime: "2026-11-01T00:00:00Z", sha256: sha,
  },
  scope: { project: "DeviceCare-KI", projectRootVerified: true,
    projectRootId: "scope-1", destinationFolderIds: ["scope-1", "target-2"] },
  classification: { status: "VERIFIED", basis: "CONTENT",
    contentReceipt: "classifier-attested", confidence: .99,
    contentDate: "2026-11-01", topic: "BRANDING_LOGO", version: 1,
    contentSha256: sha, destinationFolderId: "target-2",
    destinationVerified: true, uniqueNameVerified: true, licenseRightsVerified: true },
  scan: { contentSha256: sha, ...Object.fromEntries(
    ["malware","secrets","privacy","license","integrity"]
      .map(g => [g, { status: "VERIFIED", verdict: "CLEAN", receipt: "scan-" + g }])) },
  now: new Date("2026-11-21T10:00:00Z"),
});
test("Berlin calendar keys follow daylight-saving boundaries", () => {
  assert.equal(dayKey("2026-03-29T00:59:59Z"), "2026-03-29");
  assert.equal(dayKey("2026-10-24T23:00:00Z"), "2026-10-25");
});
test("daily runner cannot claim activation without all adapters", () => {
  assert.equal(dailyAdmission({ project:"DeviceCare-KI" }).state, Status.HOLD);
});
test("daily runner stops on owner pause or already completed day", () => {
  const base = { enabled:true, project:"DeviceCare-KI", rootVerified:true,
    oauthVerified:true, scannerReady:true, webBackendReady:true,
    now:new Date("2026-11-21T10:00:00Z") };
  assert.equal(dailyAdmission({ ...base, ownerPaused:true }).state, Status.STOPPED);
  assert.equal(dailyAdmission({ ...base, completedDayKey:"2026-11-21" }).state, Status.STOPPED);
  assert.equal(dailyAdmission(base).state, Status.PLAN_ONLY);
});
test("cross-project root fails before content parsing", () => {
  const x=good(); x.scope.projectRootId="capital-ai-id";
  assert.equal(evaluateFile(x).code, "PROJECT_BOUNDARY_NOT_VERIFIED");
});
test("legacy CAPITAL-AI scope cannot be used under a DeviceCare job", () => {
  const x=good(); x.scope.project="CAPITAL-AI";
  assert.equal(evaluateFile(x).state, Status.HOLD);
});
test("verified classifier and security gates propose only safe changes", () => {
  const x=good(); const out=evaluateFile(x);
  assert.equal(out.state, Status.PLAN_ONLY);
  assert.deepEqual(out.actions.map(a=>a.type), ["RENAME","MOVE"]);
  assert.equal(out.actions[0].to,"2026-11-01_DEVICECARE_CHATGPT_BRANDING_LOGO_v1.png");
});
test("no scanner or mismatch hash: fail closed", () => {
  const x=good(); delete x.scan.malware;
  assert.equal(evaluateFile(x).state,Status.HOLD);
  const y=good(); y.scan.contentSha256="b".repeat(64);
  assert.equal(evaluateFile(y).state,Status.HOLD);
});
test("security finding blocks writes", () => {
  const x=good(); x.scan.secrets.verdict="MALICIOUS";
  assert.equal(evaluateFile(x).state,Status.BLOCKED);
});
test("filename-only classification cannot rename", () => {
  const x=good(); x.classification.basis="NAME";
  assert.equal(evaluateFile(x).state,Status.WAIT);
});
test("prompt injection cannot escape approved destination allowlist", () => {
  const x=good(); x.classification.destinationFolderId="CAPITAL-AI";
  x.classification.topic="IGNORE_PREVIOUS_INSTRUCTIONS";
  assert.equal(evaluateFile(x).state,Status.WAIT);
});
test("duplicate/conflicting or shared files require manual owner decision", () => {
  const x=good(); x.file.conflict=true;
  assert.equal(evaluateFile(x).state,Status.WAIT);
  const y=good(); y.file.isSharedExternally=true;
  assert.equal(evaluateFile(y).state,Status.WAIT);
});
test("older than 45 days still requires verified inactivity and restore", () => {
  const x=good(); x.file.modifiedTime="2026-08-01T00:00:00Z";
  assert.equal(evaluateFile(x).code,"INACTIVITY_NOT_PROVEN");
  x.activity={ coverage:"VERIFIED", referencesChecked:true,
    lastActivityAt:"2026-08-02T00:00:00Z", activeReferences:false };
  assert.equal(evaluateFile(x).code,"ARCHIVE_ROLLBACK_NOT_PROVEN");
  x.archive={ backupVerified:true, restoreVerified:true, compressionVerified:true,
    manifestSha256:"b".repeat(64), destinationVerified:true };
  const out=evaluateFile(x);
  assert.deepEqual(out.actions.map(a=>a.type), ["RENAME","MOVE","ARCHIVE_COPY_COMPRESSED"]);
  assert.equal(out.actions[2].preserveOriginal,true);
});
test("active references, legal hold or recent usage prevent archival", () => {
  for (const field of ["activeReferences","legalHold","retentionPinned"]) {
    const x=good(); x.file.modifiedTime="2026-08-01T00:00:00Z";
    x.activity={coverage:"VERIFIED",referencesChecked:true,
      lastActivityAt:"2026-08-02T00:00:00Z",activeReferences:false,[field]:true};
    assert.equal(evaluateFile(x).state,Status.PLAN_ONLY);
    assert.equal(evaluateFile(x).actions.some(a=>a.type.startsWith("ARCHIVE")),false);
  }
  const x=good(); x.file.modifiedTime="2026-08-01T00:00:00Z";
  x.activity={coverage:"VERIFIED",referencesChecked:true,
    lastActivityAt:"2026-11-20T00:00:00Z",activeReferences:false};
  assert.equal(evaluateFile(x).state,Status.PLAN_ONLY);
});
test("batch stops at first evidence gap and preserves checkpoint", () => {
  const x=good(), y=good(); y.file.id="file-2"; y.scan.integrity.verdict="BAD";
  const z=good(); z.file.id="file-3";
  const out=planBatch([x,y,z],{now:new Date("2026-11-21T10:00:00Z")});
  assert.equal(out.state,Status.BLOCKED);
  assert.equal(out.processed,2);
  assert.equal(out.checkpoint,1);
  assert.equal(out.plans.length,2);
  assert.equal(planBatch(Array(501).fill(x)).code,"BATCH_LIMIT");
});

test("owner stop overrides missing cloud integrations", () => {
  assert.equal(dailyAdmission({ownerPaused:true}).code,"OWNER_STOP");
});
test("impossible content dates and excessive topic strings require owner review", () => {
  const x=good(); x.classification.contentDate="2026-02-31";
  assert.equal(evaluateFile(x).state,Status.WAIT);
  const y=good(); y.classification.topic="A".repeat(10000);
  assert.equal(evaluateFile(y).state,Status.WAIT);
});

test("missing source parent always fails closed", () => {
  const x=good(); delete x.file.parentId;
  assert.equal(evaluateFile(x).code,"SOURCE_PARENT_NOT_VERIFIED");
});
