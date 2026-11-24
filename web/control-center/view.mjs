import { authorizedMembership, tabsFor } from "./iam.mjs";

// SSR-only private adapter. No OAuth provider, scan worker, database or server startup
// is included. These must be injected after independent verification.
const paths = Object.freeze({
  "/profile/control-center": { id: "personal", operation: "personal:view", heading: "Mein Geräteprofil" },
  "/profile/control-center/drive": { id: "drive", operation: "project:drive:view", heading: "Drive-Sicherheit" },
  "/profile/control-center/enterprise": { id: "enterprise", operation: "tenant:dashboard", heading: "Enterprise-Übersicht" },
  "/profile/control-center/iam": { id: "iam", operation: "tenant:iam:read", heading: "Identitäten & Rollen" },
});
const h = value => String(value ?? "").replace(/[&<>"']/g,
  symbol => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;", '"':"&quot;", "'":"&#39;" })[symbol]);
function displayProof(data) {
  if (!data || data.status !== "VERIFIED" || !data.evidenceReceipt)
    return { status: "NOT_PROVEN", label: "Noch keine geprüften Daten vorhanden", value: "–" };
  if (!Number.isSafeInteger(data.value) || data.value < 0)
    return { status: "NOT_PROVEN", label: "Messwert nicht verifiziert", value: "–" };
  return { status: "VERIFIED", label: "Nachweis vorhanden", value: String(data.value) };
}
function document({ title, tabId, tabs, metric, status }) {
  const nav = tabs.map(tab=> {
    const href = tab.id==="personal" ? "/profile/control-center" : "/profile/control-center/"+tab.id;
    return `<a href="${h(href)}" ${tab.id===tabId ? 'aria-current="page"' : ""}>${h(tab.label)}</a>`;
  }).join("");
  return `<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>${h(title)} – DeviceCare-KI</title>
<style>
:root{font-family:system-ui,sans-serif;color-scheme:light dark}
body{max-width:1100px;margin:auto;padding:clamp(12px,3vw,32px);line-height:1.5}
header{border-bottom:1px solid #8885;padding-bottom:1rem}
nav{display:flex;flex-wrap:wrap;gap:.7rem;margin:1.3rem 0}
nav a{padding:.65rem 1rem;border:1px solid #8888;border-radius:12px;color:inherit;text-decoration:none}
nav a[aria-current=page]{border:2px solid currentColor;font-weight:700}
section{border:1px solid #8888;border-radius:16px;padding:1.2rem;margin:1rem 0}
small{opacity:.8}
strong{word-break:break-word}
@media(max-width:420px){nav a{width:100%}}
</style></head>
<body><header><strong>DeviceCare-KI · Privates Control Center</strong>
<p>Mandantengetrennte Ansichten · Keine automatische Geräte- oder Drive-Aktion</p></header>
<nav aria-label="Control-Center-Reiter">${nav}</nav>
<main><h1>${h(title)}</h1>
<section aria-labelledby="scanproof"><h2 id="scanproof">Evidenzstatus</h2>
<p><strong>${h(status)}</strong> – ${h(metric.label)}</p>
<p>Verifizierte Zähler: <strong>${h(metric.value)}</strong></p>
<small>Ohne freigegebene Datenquelle werden keine Gerätezustände, Scan-Ergebnisse oder Mandantenkennzahlen angenommen.</small></section>
<section><h2>Sicherheitsgrenzen</h2>
<p>Funktionen zum Umbenennen, Verschieben, Archivieren, Löschen oder Ändern von Berechtigungen sind hier nicht aktiviert.
Anträge wechseln bei fehlender Evidenz in WAIT_OWNER, HOLD oder BLOCKED.</p></section>
</main></body></html>`;
}
const baseHeaders = {
  "Cache-Control": "no-store, private",
  "X-Content-Type-Options": "nosniff",
  "X-Frame-Options": "DENY",
  "Referrer-Policy": "no-referrer",
  "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'",
};
export function createControlCenterHandler({ authenticate, membershipsFor, dashboardRead, clock = () => new Date() } = {}) {
  // These adapters MUST be server-side and cannot rely on caller-supplied role headers.
  return async function handler(req, res) {
    for (const [k,v] of Object.entries(baseHeaders)) res.setHeader(k,v);
    const fail = (status, msg) => { res.statusCode=status; res.setHeader("Content-Type","text/plain; charset=utf-8");res.end(msg); };
    if (req.method !== "GET") return fail(405,"METHOD_NOT_ALLOWED");
    const path = req.url?.split("?")[0];
    const route = Object.hasOwn(paths, path) ? paths[path] : null;
    if (!route) return fail(404,"NOT_FOUND");
    if (typeof authenticate !== "function" || typeof membershipsFor !== "function")
      return fail(503,"IAM_NOT_CONFIGURED");
    try {
      const identity = await authenticate(req); // must be verified server-side
      const context = await membershipsFor(identity); // server-side memberships, tenant ID from trusted session
      const memberships = context?.memberships;
      const tenantId = context?.activeTenantId;
      const now = clock();
      const decision = authorizedMembership({ identity, memberships,
        operation:route.operation, tenantId, now });
      if (!decision.allowed) return fail(403,"FORBIDDEN");
      const tabs = tabsFor({ identity, memberships, tenantId, now });
      const evidence = typeof dashboardRead==="function"
        ? await dashboardRead({ identity, tenantId, view: route.id, operation:route.operation })
        : null;
      const metric = displayProof(evidence);
      res.statusCode = 200;
      res.setHeader("Content-Type","text/html; charset=utf-8");
      res.end(document({ title:route.heading, tabId:route.id, tabs, metric, status:metric.status }));
    } catch {
      return fail(503,"SERVICE_UNAVAILABLE"); // never leak errors or authentication internals
    }
  };
}
