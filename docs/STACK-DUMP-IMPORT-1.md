# STACK-DUMP-IMPORT@1 — offline diagnostic contract

Entry point: OfflineStackDumpAnalyzer.import(input: InputStream), in existing :app module.

- Explicit user-provided UTF-8 text only. No process inspection, root, ADB, Android permissions, AccessibilityService, package enumeration, networking, remote inference, background capture, or file persistence.
- Caller opens and closes the stream. A future user-facing file picker can use Android ACTION_OPEN_DOCUMENT; this code-only contract does not include a file-picker UI.
- Limits: 4 MiB, 100,000 lines, 16,384 characters per line. Invalid UTF-8/NUL and oversize input fail closed with constant codes.
- Aggregates only: recognized JVM header states, generic Thread-N counts, captured Go header states, bounded (64) Go identifiers, and occurrences of known network stack frames. Raw names, domains, endpoints, and stack text are never returned.
- Go identifiers are IDs, not counts. Go header count is NOT a census of active goroutines; network stack-frame occurrences are NOT packet or connection counts.
- VERIFIED means observed in user-supplied text. SUSPECTED flags a diagnostic hypothesis only (64+ generic JVM headers). NOT_PROVEN rules out automatic claims of resource leaks or malware.
- No automatic scoring upgrade; no modifications to existing Android observation contracts.
- Synthetic positive/negative JUnit tests cover headers, IDs, redaction, network markers, caps and malformed input.
- Gate: existing android-ci.yml runs unit tests, Android lint, Trivy vulnerability/secret/license/misconfig scanning, debug APK signing and SHA evidence. Physical Android 15 acceptance stays NOT_PROVEN until performed.
- Rollback: revert this standalone commit without affecting user data, Android permissions, Firebase, or existing QA screens.
- Independent review lenses: PRODUCT (contract), TRUST (privacy/budgets), QUALITY (tests/CI).
