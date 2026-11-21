// DeviceCare-KI Control Center: offline, framework-independent authorization core.
// This module does not authenticate users or provision IAM. It requires server-supplied
// verified identity + membership; never accept roles or tenant IDs from browser input.
export const Roles = Object.freeze({
  OWNER: "OWNER", DEVICE_USER: "DEVICE_USER", B2B_ADMIN: "B2B_ADMIN",
  ENTERPRISE_ADMIN: "ENTERPRISE_ADMIN", SECURITY_ANALYST: "SECURITY_ANALYST",
  AUDITOR: "AUDITOR", VIEWER: "VIEWER",
});
const grants = Object.freeze({
  "personal:view": [Roles.DEVICE_USER, Roles.OWNER, Roles.VIEWER],
  "tenant:dashboard": [Roles.ENTERPRISE_ADMIN, Roles.B2B_ADMIN, Roles.SECURITY_ANALYST, Roles.AUDITOR, Roles.VIEWER],
  "tenant:iam:read": [Roles.ENTERPRISE_ADMIN, Roles.B2B_ADMIN, Roles.AUDITOR],
  "tenant:iam:manage": [Roles.ENTERPRISE_ADMIN, Roles.B2B_ADMIN],
  "project:drive:view": [Roles.OWNER, Roles.SECURITY_ANALYST, Roles.AUDITOR],
  "project:drive:review": [Roles.OWNER, Roles.SECURITY_ANALYST],
  "project:drive:approve": [Roles.OWNER],
  "project:drive:pause": [Roles.OWNER],
  "project:drive:resume": [Roles.OWNER],
  "project:drive:audit": [Roles.OWNER, Roles.AUDITOR, Roles.SECURITY_ANALYST],
});
const stepUp = new Set([
  "project:drive:approve", "project:drive:pause", "project:drive:resume",
  "tenant:iam:manage",
]);
const MAX_STEPUP_AGE_MS = 5 * 60_000;
const ROLE_SCOPE = Object.freeze({
  [Roles.OWNER]: ["project", "personal"],
  [Roles.DEVICE_USER]: ["personal"],
  [Roles.ENTERPRISE_ADMIN]: ["tenant"],
  [Roles.B2B_ADMIN]: ["tenant"],
  [Roles.SECURITY_ANALYST]: ["tenant", "project"],
  [Roles.AUDITOR]: ["tenant", "project"],
  [Roles.VIEWER]: ["tenant", "personal"],
});
const deny = (reason) => ({ allowed: false, reason });
const allow = () => ({ allowed: true, reason: "VERIFIED" });

export function authorize({ identity, membership, operation, resource, now = new Date() } = {}) {
  const clock = new Date(now).getTime();
  if (!Number.isFinite(clock)) return deny("CLOCK_INVALID");
  if (!identity || identity.authenticated !== true || identity.serverVerified !== true ||
    typeof identity.subjectId !== "string" || !identity.subjectId ||
    !Number.isFinite(Date.parse(identity.sessionExpiresAt || "")) ||
    Date.parse(identity.sessionExpiresAt) <= clock)
    return deny("AUTH_NOT_VERIFIED");
  if (!membership || membership.source !== "SERVER" || membership.status !== "VERIFIED" ||
    membership.active !== true || membership.subjectId !== identity.subjectId ||
    !Object.values(Roles).includes(membership.role) ||
    !ROLE_SCOPE[membership.role]?.includes(membership.scope))
    return deny("MEMBERSHIP_NOT_VERIFIED");
  if (!resource || !grants[operation]?.includes(membership.role))
    return deny("PERMISSION_DENIED");
  const prefix = operation.split(":")[0];
  if (membership.scope !== prefix || resource.scope !== prefix)
    return deny("SCOPE_MISMATCH");
  if (prefix === "personal") {
    if (!resource.subjectId || resource.subjectId !== identity.subjectId)
      return deny("PERSONAL_RESOURCE_MISMATCH");
  } else if (prefix === "tenant") {
    if (!resource.tenantId || membership.tenantId !== resource.tenantId)
      return deny("TENANT_ISOLATION");
    if (resource.crossTenant === true) return deny("TENANT_ISOLATION");
  } else if (prefix === "project") {
    if (membership.project !== "DeviceCare-KI" || resource.project !== "DeviceCare-KI")
      return deny("PROJECT_ISOLATION");
  } else return deny("UNKNOWN_SCOPE");
  if (stepUp.has(operation)) {
    const mfaAt = Date.parse(identity.stepUpAt || "");
    if (identity.stepUpVerified !== true || !Number.isFinite(mfaAt) ||
      mfaAt > clock || clock - mfaAt > MAX_STEPUP_AGE_MS)
      return deny("STEP_UP_REQUIRED");
  }
  return allow();
}
const targetFor = (operation, { identity, tenantId } = {}) => {
  if (operation.startsWith("personal:")) return { scope: "personal", subjectId: identity?.subjectId };
  if (operation.startsWith("tenant:")) return { scope: "tenant", tenantId };
  return { scope: "project", project: "DeviceCare-KI" };
};
export function authorizedMembership({ memberships, identity, operation, tenantId, now } = {}) {
  if (!Array.isArray(memberships) || memberships.length > 100)
    return { ...deny("MEMBERSHIPS_UNAVAILABLE"), membership: null };
  for (const membership of memberships) {
    const decision = authorize({ identity, membership, operation,
      resource: targetFor(operation, { identity, tenantId }), now });
    if (decision.allowed) return { ...decision, membership };
  }
  return { ...deny("PERMISSION_DENIED"), membership: null };
}
export function tabsFor({ identity, memberships, tenantId, now } = {}) {
  const choices = [
    { id: "personal", label: "Mein Profil", operation: "personal:view" },
    { id: "drive", label: "Drive-Sicherheit", operation: "project:drive:view" },
    { id: "enterprise", label: "B2B / Enterprise", operation: "tenant:dashboard" },
    { id: "iam", label: "Zugriffe & IAM", operation: "tenant:iam:read" },
  ];
  return choices.filter(c=>authorizedMembership({
    identity, memberships, operation: c.operation, tenantId, now
  }).allowed).map(({ id, label })=>({ id,label }));
}
export function validateOwnerPlanApproval({ identity, memberships, approval, plan, now = new Date() } = {}) {
  const projectDecision = authorizedMembership({
    identity, memberships, operation:"project:drive:approve", now
  });
  if (!projectDecision.allowed) return deny(projectDecision.reason);
  if (!approval || !plan || approval.actorId !== identity.subjectId ||
    approval.project !== "DeviceCare-KI" || plan.project !== "DeviceCare-KI" ||
    approval.status !== "VERIFIED" || !approval.serverVerified ||
    !/^[a-f0-9]{64}$/i.test(approval.planSha256 || "") ||
    approval.planSha256 !== plan.sha256 ||
    approval.resourceVersion !== plan.resourceVersion ||
    plan.currentReadbackVerified !== true || plan.securityGatesVerified !== true ||
    !Number.isFinite(Date.parse(approval.expiresAt || "")) ||
    Date.parse(approval.expiresAt) <= new Date(now).getTime())
    return deny("APPROVAL_STALE_OR_INVALID");
  // This is a validation decision only, never a Drive or IAM mutation.
  return allow();
}
