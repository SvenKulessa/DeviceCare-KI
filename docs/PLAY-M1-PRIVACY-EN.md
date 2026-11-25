# Privacy policy – DeviceCare KI

> **INTERNAL DRAFT / RELEASE BLOCKED.** The legal publisher, postal address, privacy contact, legal-basis review, support-data retention and public HTTPS URL must be completed before any Google Play upload. Text matches the in-app view, subject to final legal review.

- Product: DeviceCare KI
- Package: `com.aistudio.formwerk.vktmrx`
- Version: 2026-11-24
- Public privacy URL: **NOT_PROVEN – verify before release**

## 1. Controller and contact

DeviceCare KI (package com.aistudio.formwerk.vktmrx). Legal publisher, postal address and privacy contact: [TO BE COMPLETED BEFORE RELEASE]. This is an internal draft and is not approved for public use.

## 2. Scope

This notice covers the native Android app DeviceCare KI (package com.aistudio.formwerk.vktmrx). It covers current on-device checks as well as optional cloud notes and AI advisor features, and excludes future Drive or enterprise control-center backends (NOT_PROVEN).

## 3. On-device access and purpose

Only after explicit user action does the app read battery metrics (level, status, temperature, voltage, health), coarse memory and thermal signals, an on-device hardware and sensor inventory (individual sensor samples only on separate confirmation), Android-visible app and permission metadata, and DeviceCare KI's own permissions. Quick inspection sampling is capped at 250, 75, or 25 visible apps based on device load.

## 4. Optional system and Google security check

A separate user-initiated check reads Android version, API level, reported security patch date, and available version details for Google Play services, Play Store, and the active WebView provider. Visibility queries are strictly scoped to GMS and Play Store. No live CVE lookup, malware scan, or Play Protect verification is performed.

## 5. Retention, export and deletion

Diagnostic, battery, hardware, and permission results remain transient in memory and are not automatically persisted as history. Design preferences use local DataStore; optional CSV/JSON exports occur only on explicit user action via Android Share/FileProvider. Android cloud backup is disabled via allowBackup=false. Opt-in Cloud Care Notes are saved in Firebase Firestore under your Google account only after sign-in and can be edited or deleted there.

## 6. Transfers, cloud features and AI

The app manifest declares the INTERNET permission solely for user-initiated online features: optional Google Sign-In (Credential Manager) for Firebase Firestore Cloud Care Notes, optional AI analysis via Firebase AI Logic (Gemini API), and optional Firebase App Check (Play Integrity disabled by default). Without explicitly using those screens or buttons, local inspection sends no diagnostic data over the network. No advertising or tracking SDKs are bundled.

## 7. Permissions and security

The app requests no dangerous runtime permissions such as camera, microphone, location, contacts, VPN, or AccessibilityService, and never uses root or ADB. Package visibility is limited to standard launcher intents plus Google Play services and Play Store; sensor or package details are processed locally only on explicit action. Unsupported readings are marked unknown or UNSUPPORTED.

## 8. Legal basis and rights

Final GDPR legal basis and cross-border transfer assessment (including optional Google/Firebase services) must be completed by the controller before public release. Data subjects have statutory rights of access, rectification, erasure, restriction, and complaint to a supervisory authority. Retention and deletion rules for voluntary support correspondence must be documented separately before release.

## 9. Support correspondence

Support details users send voluntarily outside the app are not part of on-device diagnostics. Before public Play release, the responsible entity, contact channel, purposes, recipients, and retention period for support requests must be confirmed.

## 10. Changes to this policy

Version: 24 November 2026 – internal draft. Changes to shipped features, SDKs, data flows, or retention require updating this notice, the in-app view, and Play Data safety declarations together. A public HTTPS privacy policy URL must be verified before any Play upload.

---

**Release gate:** Public hosting with valid TLS, global availability and no login, plus reconciliation with the final signed AAB and Play Data safety form, remain unverified.
