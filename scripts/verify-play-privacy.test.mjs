import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { validatePrivacy } from "./verify-play-privacy.mjs";

const read = (path) => readFileSync(join(process.cwd(), path), "utf8");
const fixtures = () => ({
  screen: read("app/src/main/java/com/example/ui/screens/PrivacyPolicyScreen.kt"),
  home: read("app/src/main/java/com/example/ui/screens/InspectionScreen.kt"),
  app: read("app/src/main/java/com/example/ui/DeviceCareApp.kt"),
  de: read("docs/PLAY-M1-PRIVACY-DE.md"),
  en: read("docs/PLAY-M1-PRIVACY-EN.md"),
});

test("DE and EN privacy docs match the exact on-device copy and stay accessible", () => {
  assert.deepEqual(validatePrivacy(fixtures()), []);
});

test("negative: changing a section of the policy without updating the app fails closed", () => {
  const f = fixtures();
  f.en = f.en.replace("## 6. Transfers, cloud features and AI", "## 6. Missing");
  assert.ok(validatePrivacy(f).includes("POLICY_DRIFT_EN_network"));
});

test("negative: a missing home privacy entry fails closed", () => {
  const f = fixtures();
  f.home = f.home.replace('testTag("privacy_policy_open")', 'testTag("other")');
  assert.ok(validatePrivacy(f).includes("NO_HOME_POLICY_ENTRY"));
});

test("negative: a missing NavHost privacy route fails closed", () => {
  const f = fixtures();
  f.app = f.app.replaceAll("ROUTE_PRIVACY_POLICY", "ROUTE_OTHER");
  assert.ok(validatePrivacy(f).includes("NO_PRIVACY_NAVIGATION"));
});

test("negative: incomplete legal contact and unverified public URL block release", () => {
  const errors = validatePrivacy({ ...fixtures(), release: true });
  assert.ok(errors.includes("POLICY_RELEASE_METADATA_INCOMPLETE"));
  assert.ok(errors.includes("PUBLIC_PRIVACY_URL_NOT_APPROVED"));
});
