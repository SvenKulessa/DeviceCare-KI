import test from "node:test";
import assert from "node:assert/strict";
import { Roles, authorize, tabsFor, validateOwnerPlanApproval, evaluateTenantRoleChange } from "./iam.mjs";
import { createControlCenterHandler } from "./view.mjs";

const now = new Date("2026-11-21T12:30:00Z");
const identity = () => ({ authenticated: true, serverVerified: true, subjectId:"uid-1",
  sessionExpiresAt:"2026-11-21T13:30:00Z", stepUpAt:"2026-11-21T12:29:00Z",
  stepUpVerified:true });
const member = (role,scope,tenantId="company-1") => ({
  source:"SERVER", status:"VERIFIED", active:true, subjectId:"uid-1",
  role, scope, tenantId, project:"DeviceCare-KI",
});
const a = (role,scope,operation,resource, extra={})=>authorize({
  identity: identity(), membership:member(role,scope), operation,resource,now,...extra
});
test("default deny, no blind trust in browser identity, no forged roles",()=>{
  assert.equal(authorize({}).allowed,false);
  assert.equal(a(Roles.OWNER,"project","project:drive:view",{scope:"project",project:"DeviceCare-KI"},
    {identity:{...identity(),serverVerified:false}}).allowed,false);
  assert.equal(a(Roles.OWNER,"project","project:drive:view",{scope:"project",project:"DeviceCare-KI"},
    {membership:{...member(Roles.OWNER,"project"),source:"CLIENT"}}).allowed,false);
  assert.equal(a(Roles.B2B_ADMIN,"tenant","project:drive:view",{scope:"project",project:"DeviceCare-KI"}).allowed,false);
});
test("project owner can review Drive but tenant admins cannot",()=>{
  assert.equal(a(Roles.OWNER,"project","project:drive:view",{scope:"project",project:"DeviceCare-KI"}).allowed,true);
  assert.equal(a(Roles.OWNER,"project","tenant:dashboard",{scope:"tenant",tenantId:"company-1"}).allowed,false);
  assert.equal(a(Roles.SECURITY_ANALYST,"project","project:drive:approve",{scope:"project",project:"DeviceCare-KI"}).allowed,false);
});
test("hard tenant isolation and role scope",()=>{
  assert.equal(a(Roles.B2B_ADMIN,"tenant","tenant:dashboard",{scope:"tenant",tenantId:"company-2"}).reason,"TENANT_ISOLATION");
  assert.equal(a(Roles.B2B_ADMIN,"tenant","tenant:dashboard",{scope:"tenant",tenantId:"company-1"}).allowed,true);
  assert.equal(a(Roles.B2B_ADMIN,"tenant","tenant:dashboard",{scope:"tenant",tenantId:"company-1",crossTenant:true}).allowed,false);
  assert.equal(a(Roles.OWNER,"tenant","tenant:dashboard",{scope:"tenant",tenantId:"company-1"}).allowed,false);
});
test("personal dashboard cannot read another person's profile",()=>{
  assert.equal(a(Roles.DEVICE_USER,"personal","personal:view",{scope:"personal",subjectId:"uid-2"}).allowed,false);
  assert.equal(a(Roles.DEVICE_USER,"personal","personal:view",{scope:"personal",subjectId:"uid-1"}).allowed,true);
});
test("IAM changes require fresh server-confirmed MFA",()=>{
  const target={scope:"tenant",tenantId:"company-1"};
  assert.equal(a(Roles.B2B_ADMIN,"tenant","tenant:iam:read",target).allowed,true);
  assert.equal(a(Roles.B2B_ADMIN,"tenant","tenant:iam:manage",target).allowed,true);
  assert.equal(a(Roles.AUDITOR,"tenant","tenant:iam:manage",target).allowed,false);
  const stale={...identity(),stepUpAt:"2026-11-21T11:20:00Z"};
  assert.equal(a(Roles.ENTERPRISE_ADMIN,"tenant","tenant:iam:manage",target,{identity:stale}).reason,"STEP_UP_REQUIRED");
});
test("owner plan approval must bind plan hash/version, actor and timeout",()=>{
  const plan={project:"DeviceCare-KI",sha256:"a".repeat(64),resourceVersion:"v3",
    currentReadbackVerified:true,securityGatesVerified:true};
  const approval={actorId:"uid-1",project:"DeviceCare-KI",status:"VERIFIED",
    serverVerified:true,planSha256:plan.sha256,resourceVersion:"v3",
    expiresAt:"2026-11-21T12:35:00Z"};
  const args={identity:identity(), memberships:[member(Roles.OWNER,"project")],plan,approval,now};
  assert.equal(validateOwnerPlanApproval(args).allowed,true);
  assert.equal(validateOwnerPlanApproval({...args,approval:{...approval,resourceVersion:"v1"}}).allowed,false);
  assert.equal(validateOwnerPlanApproval({...args,approval:{...approval,actorId:"uid-2"}}).allowed,false);
  assert.equal(validateOwnerPlanApproval({...args,approval:{...approval,expiresAt:"2026-11-21T12:29:00Z"}}).allowed,false);
  assert.equal(validateOwnerPlanApproval({...args,plan:{...plan,securityGatesVerified:false}}).allowed,false);
});
test("tabs reflect server-verified memberships and current tenant, never client role hint",()=>{
  const owner=tabsFor({identity:identity(),memberships:[member(Roles.OWNER,"project")],tenantId:"company-1",now});
  assert.deepEqual(owner.map(x=>x.id),["drive"]);
  const personal=tabsFor({identity:identity(),memberships:[member(Roles.DEVICE_USER,"personal")],now});
  assert.deepEqual(personal.map(x=>x.id),["personal"]);
  const tenant=tabsFor({identity:identity(),memberships:[member(Roles.ENTERPRISE_ADMIN,"tenant")],
    tenantId:"company-1",now});
  assert.deepEqual(tenant.map(x=>x.id),["enterprise","iam"]);
  assert.equal(tabsFor({identity:identity(),memberships:[member(Roles.B2B_ADMIN,"tenant")],
    tenantId:"another-tenant",now}).length,0);
});
function fakeResponse() {
  const headers={}; let body="";
  return { headers, statusCode:200,
    setHeader(k,v){headers[k.toLowerCase()]=v},
    end(v){body=String(v??"")},
    get body(){return body},
  };
}
async function run(path,{identityValue=identity(),members=[member(Roles.DEVICE_USER,"personal")],
    activeTenantId="company-1", data=null, method="GET", auth=true}={}){
  const response=fakeResponse();
  const handler=createControlCenterHandler({
    ...(auth ? {authenticate:async()=>identityValue, membershipsFor:async()=>({memberships:members,activeTenantId})}:{ }),
    dashboardRead:async()=>data,
    clock:()=>now,
  });
  await handler({method,url:path,headers:{"x-role":"OWNER","x-tenant-id":"company-2"}},response);
  return response;
}
test("unauthenticated adapters or requests fail closed",async()=>{
  assert.equal((await run("/profile/control-center",{auth:false})).statusCode,503);
  assert.equal((await run("/profile/control-center",{identityValue:{...identity(),authenticated:false}})).statusCode,403);
  assert.equal((await run("/profile/control-center/drive")).statusCode,403);
  assert.equal((await run("/profile/control-center",{method:"POST"})).statusCode,405);
});
test("role-filtered private view is SSR and NOT_PROVEN without receipts",async()=>{
  const r=await run("/profile/control-center");
  assert.equal(r.statusCode,200);
  assert.match(r.body,/Mein Geräteprofil/);
  assert.match(r.body,/NOT_PROVEN/);
  assert.doesNotMatch(r.body,/>Drive-Sicherheit<\/a>/);
  assert.equal(r.headers["cache-control"],"no-store, private");
  assert.match(r.headers["content-security-policy"],/default-src 'none'/);
});
test("tenant private dashboard cannot be selected using a forged query string",async()=>{
  const t=await run("/profile/control-center/enterprise?tenant=company-2",{
    members:[member(Roles.B2B_ADMIN,"tenant")],data:{status:"VERIFIED",value:7,evidenceReceipt:"receipt"}});
  assert.equal(t.statusCode,200);
  assert.match(t.body,/Verifizierte Zähler: <strong>7<\/strong>/);
  const blocked=await run("/profile/control-center/enterprise?tenant=company-1",{
    members:[member(Roles.B2B_ADMIN,"tenant")],activeTenantId:"company-2"});
  assert.equal(blocked.statusCode,403);
});
test("untrusted output is escaped and cannot inject HTML or forged counts",async()=>{
  const r=await run("/profile/control-center",{
    data:{status:"VERIFIED",evidenceReceipt:"verified",value:"<img src=x onerror=alert(1)>"}});
  assert.equal(r.statusCode,200);
  assert.doesNotMatch(r.body,/<img src=x/);
  assert.match(r.body,/NOT_PROVEN/);
});

test("tenant admin cannot grant own admin, project owner, cross-tenant or service roles",()=>{
  const base={identity:identity(),membership:member(Roles.ENTERPRISE_ADMIN,"tenant"),now,
    change:{ status:"VERIFIED",serverVerified:true,actorIntentVerified:true,
      tenantId:"company-1",targetSubjectId:"uid-2",newRole:Roles.FLEET_OPERATOR,
      expectedMembershipVersion:"v4",targetMembershipVerified:true,
      lastAdminProtected:true }};
  assert.equal(evaluateTenantRoleChange(base).reason,"PLAN_ONLY");
  assert.equal(evaluateTenantRoleChange({...base,
    change:{...base.change,newRole:Roles.OWNER}}).allowed,false);
  assert.equal(evaluateTenantRoleChange({...base,
    change:{...base.change,targetSubjectId:"uid-1"}}).allowed,false);
  assert.equal(evaluateTenantRoleChange({...base,
    change:{...base.change,tenantId:"company-2"}}).allowed,false);
  assert.equal(evaluateTenantRoleChange({...base,
    change:{...base.change,serverVerified:false}}).allowed,false);
  assert.equal(evaluateTenantRoleChange({...base,
    identity:{...identity(),stepUpVerified:false}}).allowed,false);
  assert.equal(a(Roles.FLEET_OPERATOR,"tenant","tenant:dashboard",
    {scope:"tenant",tenantId:"company-1"}).allowed,true);
  assert.equal(a(Roles.FLEET_OPERATOR,"tenant","tenant:iam:manage",
    {scope:"tenant",tenantId:"company-1"}).allowed,false);
});

test("unknown/prototype route is not an authorized dashboard",async()=>{
  assert.equal((await run("/__proto__")).statusCode,404);
  assert.equal((await run("/profile/control-center/garbage")).statusCode,404);
});
