# DeviceCare KI — Android 15 Screenreader-Abnahme (QA-04)

Status: **NOT_PROVEN**. Gilt für die führende App `SvenKulessa/DeviceCare-KI`.
Dieser Test ersetzt **produktspezifisch benannte TalkBack-Nachweise** durch einen
**Screenreader-Nachweis**. Die Android-Funktion TalkBack darf als tatsächlich
verwendeter Screenreader im Messprotokoll genannt werden, ist aber keine
vorgeschriebene Marke oder Freigabebehauptung.

## Gegenstand und Grenzen

- **Automatisiert:** Compose-Semantik, Überschriften, Content-Descriptions,
  Fokuszielgrößen (mindestens 48dp) und Live-Regionen können durch passende
  Unit-/UI-/Emulator-Tests überprüft werden. Diese Ergebnisse sind `CI_EVIDENCE`,
  **nicht** `DEVICE_SCREENREADER_EVIDENCE`.
- **Manuell:** Ein auf dem physischen Android-15-Gerät aktivierter Screenreader
  muss die echten Ansagen und Bedienpfade der installierten Build-Version zeigen.
  Keine App-eigene AccessibilityService-Berechtigung, ADB-, Root- oder
  Screenreader-Fernsteuerung verwenden.
- **Ausgabe:** QA-04 bleibt `NOT_PROVEN` bis alle folgenden Prüfpunkte mit
  einer datensparsamen, prüfbaren Aufnahme oder einem nachvollziehbaren,
  von der testenden Person protokollierten Ansage-/Fokusprotokoll belegt sind.

## QA-04 — Durchzuführen auf Z2459 / Android 15

1. **Build-Identität**: Repository, Commit-SHA, APK-SHA-256, Signer-Fingerprint,
   Versionsname, Android-API-Level und Sicherheits-Patchmonat notieren.
2. **Screenreader-Identität**: tatsächlichen Screenreader mit Name, Version,
   aktiviertem Sprachfeedback und Testdatum/Zeitzone notieren.
3. **Navigation**: Startbildschirm, Prüf-Tab, Tabs, Überschriften,
   Bedienelemente und Zurück-Navigation ausschließlich über Screenreader-Gesten
   nachvollziehbar erreichen. Fokus darf nicht festhängen oder unsichtbar sein.
4. **Kontrollen**: Scan startet nur nach ausdrücklicher Aktion; Schaltflächen
   haben aussagekräftigen Namen, Rolle und gegebenenfalls Zustand. Keine
   unbeschrifteten oder mehrfach redundanten Fokusziele.
5. **Status- und Fehleransagen**: Ergebniswechsel und Fehlermeldungen
   werden korrekt, verständlich und ohne Datenverlust angesagt. Keine
   erfundenen Sicherheits-/Malware-Urteile oder stillen Hintergrund-Scans.
6. **Layout**: bei 200 % Schrift, kleiner Breite (320dp, sofern einstellbar),
   Portrait/Querformat und Hell/Dunkel keine unbedienbaren, abgeschnittenen
   oder überlagerten Kontrollen.
7. **Datenschutz**: Bildschirm-/Audioaufnahme vor Weitergabe auf E-Mails,
   App-Listen, Notifications, Identifikatoren, API-Keys und sonstige
   personenbezogene Informationen prüfen und nötigenfalls redigieren.

## Evidence-Schema — ein Eintrag je geprüfter Build-/Geräte-Kombination

```json
{
  "contract": "devicecare-screenreader-evidence/1",
  "qa_id": "QA-04",
  "status": "NOT_PROVEN",
  "device_model": "Z2459",
  "android_version": "15",
  "android_patch_month": null,
  "app_commit_sha": null,
  "apk_sha256": null,
  "apk_signer_sha256": null,
  "screenreader_name": null,
  "screenreader_version": null,
  "speech_feedback_enabled": null,
  "test_timestamp_iso8601": null,
  "checks": {
    "navigation": "NOT_PROVEN",
    "controls": "NOT_PROVEN",
    "announcements": "NOT_PROVEN",
    "large_font_and_layout": "NOT_PROVEN",
    "privacy_redaction": "NOT_PROVEN"
  },
  "evidence_location": null,
  "examiner": null
}
```

**Statusregel:** `VERIFIED` ausschließlich, wenn alle fünf Checks `PASS`
sind, die Identitätsfelder vollständig sind und ein überprüfbarer
Screenreader-Nachweis existiert. `FAIL` bei belegtem Defekt; sonst
`NOT_PROVEN`. Ein fehlerfreier Emulator-/Compose-Test, eine installierte
APK oder eine stumme Bildschirmaufnahme darf QA-04 **nicht** auf
`VERIFIED` setzen.

## Rücknahme / Governance

Das hier ist ein Akzeptanz- und Evidence-Vertrag, keine Änderung an Android-
Berechtigungen, Firebase, Signierung oder produktivem Scan. Bei Problemen
den zugehörigen PR revertieren. Bestehende Release-/CI-Gates und Owner-
Freigaben bleiben unverändert.
