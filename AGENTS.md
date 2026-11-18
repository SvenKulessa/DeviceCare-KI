# AGENTS.md — DeviceCare-KI repository execution policy

Version: MIGRATION-P0-1 | 2026-11-18. Scope: DeviceCare-KI only.

## Repository authority
This is the sole repository-wide operating policy. The prior
[AI-DeviceCare AGENTS.md](https://github.com/SvenKulessa/AI-DeviceCare/blob/main/AGENTS.md)
is a reference for approved patterns, not an independent authorization here.
Preserve the single existing Gradle root `settings.gradle.kts` and `:app`,
applicationId, Firebase/Cloud and UI features. Do not bulk-overwrite them with
the former multi-module project. Proven features may be ported as isolated slices.

## Execution and evidence
Work on feature/fix branches and use pull requests. Confirm current main and
open PRs, map Android capability and privacy boundaries, implement the smallest
compatible change, run applicable unit/negative tests, Android lint, build and
security/license scans. Record precise commit SHA, run ID, exit status, APK
hash and signing certificate. No merge while CI or device acceptance is missing.
Do not invent test results. Supply rollback instructions. Only the owner may
approve Google Play publishing and production release signing.

## Safety and consent
No root, ADB, AccessibilityService automation, broad hidden package access,
stealth diagnostics, third-party cache clearing or unapproved permissions.
Device findings must distinguish local evidence from unproven external
freshness, CVEs, Play Protect, Play System patch, OEM OTA and malware verdicts.
No automated changes to Android/Google settings. Sensitive scans require
user initiation, minimal collection, bounded resource budgets and redaction.

Optional Firebase Auth, Firestore and Firebase AI are separate opt-in cloud
data paths: never upload local security evidence automatically. Keep Firebase
configuration and signing keys out of the repository. Never replace or expose
an existing production signing key. Debug APKs are signed for internal testing
and are not production-ready or suitable for Google Play distribution.

## Merge / release gates
Require green Gradle unit tests, lint, signed-debug APK validation, Trivy
vulnerability/secret/license scan and manifest review. Before any public Play
release, independently verify consent, Firebase data rules, package signature
continuity, accessibility, real-device Android 15, privacy policy, Data Safety
and release provenance. Do not create a release key on CI for a production build.
