// DeviceCare-KI only. Pure policy engine; NO Drive API, no scheduler, no mutations.
// A production web service must provide authenticated adapters and independent evidence.
export const Status = Object.freeze({
  VERIFIED: "VERIFIED", WAIT: "WAIT_OWNER", HOLD: "HOLD",
  BLOCKED: "BLOCKED", PLAN_ONLY: "PLAN_ONLY", STOPPED: "STOPPED",
});
const PROJECT = "DeviceCare-KI";
const CLEAN_GATES = ["malware", "secrets", "privacy", "license", "integrity"];
const HEX64 = /^[a-f0-9]{64}$/i;
const DAY = 86_400_000;
const FILE_EXTENSIONS = Object.freeze({
  "image/png": "png", "image/jpeg": "jpg", "image/svg+xml": "svg",
  "application/pdf": "pdf", "text/plain": "txt", "text/markdown": "md",
  "application/json": "json", "application/zip": "zip",
  "video/mp4": "mp4", "application/vnd.openxmlformats-officedocument.wordprocessingml.document": "docx",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": "xlsx",
});
const NATIVE_MIME = new Set([
  "application/vnd.google-apps.document",
  "application/vnd.google-apps.spreadsheet",
  "application/vnd.google-apps.presentation",
]);

function result(state, code, actions = []) {
  return { state, code, actions }; // Never include raw content or filenames in audit output.
}
export function dayKey(now = new Date()) {
  const date = new Date(now);
  if (!Number.isFinite(date.valueOf())) throw new Error("INVALID_DATE");
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Berlin", year: "numeric", month: "2-digit", day: "2-digit",
  }).formatToParts(date);
  const byType = Object.fromEntries(parts.map(p => [p.type, p.value]));
  return `${byType.year}-${byType.month}-${byType.day}`;
}

// The web scheduler calls this atomically under a server-side per-tenant lease.
// Calling this function does NOT schedule a job or claim a durable lock.
export function dailyAdmission({ enabled, project, rootVerified, oauthVerified, scannerReady,
  webBackendReady, ownerPaused, completedDayKey, now = new Date() } = {}) {
  if (project !== PROJECT || !rootVerified) return result(Status.HOLD, "SCOPE_NOT_VERIFIED");
  if (!enabled || !webBackendReady || !oauthVerified || !scannerReady)
    return result(Status.HOLD, "ADAPTER_NOT_READY");
  if (ownerPaused) return result(Status.STOPPED, "OWNER_STOP");
  if (completedDayKey === dayKey(now)) return result(Status.STOPPED, "DAY_ALREADY_COMPLETED");
  return result(Status.PLAN_ONLY, "ADMITTED_TO_DRY_RUN");
}
function cleanScan(scan, sha256) {
  if (!HEX64.test(sha256 || "") || !scan || scan.contentSha256 !== sha256)
    return result(Status.HOLD, "SCANNER_OR_HASH_MISSING");
  for (const gate of CLEAN_GATES) {
    const e = scan[gate];
    if (!e || e.status !== "VERIFIED" || !e.receipt)
      return result(Status.HOLD, "SECURITY_GATE_NOT_VERIFIED");
    if (e.verdict !== "CLEAN") return result(Status.BLOCKED, "SECURITY_FINDING");
  }
  return null;
}
function validName(classification, mime) {
  const c = classification;
  if (!c || c.status !== "VERIFIED" || c.basis !== "CONTENT" ||
    !c.contentReceipt || !(c.confidence >= 0.95) ||
    !/^\d{4}-\d{2}-\d{2}$/.test(c.contentDate || "") ||
    !/^([A-Z0-9]+_?){1,12}$/.test(c.topic || "") ||
    !Number.isSafeInteger(c.version) || c.version < 1 || c.version > 999)
    return null;
  if (!Number.isFinite(Date.parse(c.contentDate + "T00:00:00Z"))) return null;
  const extension = NATIVE_MIME.has(mime) ? "" : FILE_EXTENSIONS[mime];
  if (extension === undefined) return null;
  return `${c.contentDate}_DEVICECARE_CHATGPT_${c.topic}_v${c.version}${extension ? "." + extension : ""}`;
}
function archiveEligibility(file, activity, archive, now) {
  const modified = Date.parse(file.modifiedTime || "");
  if (!Number.isFinite(modified)) return result(Status.WAIT, "ACTIVITY_TIMESTAMP_MISSING");
  if (Number(now) - modified <= 45 * DAY) return null;
  if (!activity || activity.coverage !== "VERIFIED" ||
      !activity.referencesChecked || !activity.lastActivityAt ||
      !Number.isFinite(Date.parse(activity.lastActivityAt)) ||
      activity.activeReferences === null || activity.activeReferences === undefined)
    return result(Status.WAIT, "INACTIVITY_NOT_PROVEN");
  if (activity.activeReferences || activity.legalHold || activity.retentionPinned)
    return null; // Keep active or legally protected files without treating them as errors.
  const last = Math.max(modified, Date.parse(activity.lastActivityAt));
  if (Number(now) - last <= 45 * DAY) return null;
  if (!archive || !archive.backupVerified || !archive.restoreVerified ||
      !archive.compressionVerified || !HEX64.test(archive.manifestSha256 || "") ||
      !archive.destinationVerified)
    return result(Status.WAIT, "ARCHIVE_ROLLBACK_NOT_PROVEN");
  return { type: "ARCHIVE_COPY_COMPRESSED", preserveOriginal: true,
    format: "zip", integrityManifestSha256: archive.manifestSha256 };
}

// Only a current, verified DeviceCare-KI ancestry and destination allowlist may be used.
// Agent-produced classification is DATA and never an instruction to override policy.
export function evaluateFile({ file, scope, classification, scan, activity, archive,
  now = new Date() } = {}) {
  if (!file || !file.id || !scope ||
    scope.project !== PROJECT || scope.projectRootVerified !== true ||
    file.ancestryVerified !== true || file.projectRootId !== scope.projectRootId ||
    !scope.projectRootId || !Array.isArray(scope.destinationFolderIds) ||
    !scope.destinationFolderIds.length)
    return result(Status.HOLD, "PROJECT_BOUNDARY_NOT_VERIFIED");
  if (file.ownerDecisionRequired || file.conflict || file.locked ||
      file.legalHold || file.shortcut || file.isSharedExternally)
    return result(Status.WAIT, "OWNER_DECISION_REQUIRED");
  const gate = cleanScan(scan, file.sha256);
  if (gate) return gate;
  if (classification?.contentSha256 !== file.sha256)
    return result(Status.HOLD, "CLASSIFICATION_STALE");
  const name = validName(classification, file.mimeType);
  if (!name || !scope.destinationFolderIds.includes(classification.destinationFolderId) ||
      !classification.destinationVerified || !classification.uniqueNameVerified ||
      !classification.licenseRightsVerified)
    return result(Status.WAIT, "CONTENT_OR_TARGET_AMBIGUOUS");
  const actions = [];
  if (name !== file.name) actions.push({ type: "RENAME", from: file.name, to: name });
  if (file.parentId !== classification.destinationFolderId)
    actions.push({ type: "MOVE", fromId: file.parentId,
      toId: classification.destinationFolderId });
  const candidate = archiveEligibility(file, activity, archive, new Date(now));
  if (candidate?.state) return candidate;
  if (candidate) actions.push(candidate);
  return result(Status.PLAN_ONLY, "DRY_RUN_READY", actions);
}

// Multi-agent output must be externally verified, not trusted as an instruction.
// Stop on the FIRST required owner decision or unsafe file, checkpoint before resume.
export function planBatch(items, options = {}) {
  if (!Array.isArray(items) || items.length > 500)
    return { state: Status.HOLD, code: "BATCH_LIMIT", processed: 0, plans: [] };
  const plans = [];
  for (const item of items) {
    const evaluation = evaluateFile({ ...item, now: options.now });
    plans.push({ id: item?.file?.id || null, ...evaluation });
    if (evaluation.state !== Status.PLAN_ONLY)
      return { state: evaluation.state, code: evaluation.code,
        processed: plans.length, checkpoint: plans.length - 1, plans };
  }
  return { state: Status.PLAN_ONLY, code: "BATCH_PLANNED",
    processed: plans.length, checkpoint: plans.length, plans };
}
