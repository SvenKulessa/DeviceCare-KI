# DeviceCare KI — Android 15 Screenreader-Abnahme (QA-04)

Status: **NOT_PROVEN**. Gilt für die führende App `SvenKulessa/DeviceCare-KI`.
Dieser Test ersetzt **produktspezifisch benannte TalkBack-Nachweise** durch einen
**Screenreader-Nachweis**. Die Android-Funktion TalkBack darf als tatsächlich
verwendeter Screenreader im Messprotokoll genannt werden, ist aber keine
vorgeschriebene Marke oder Freigabebehauptung.

## Owner-Entscheidung vom 22.11.2026 — stumme Geräte-Evidence

Eine **stumme Bildschirmaufnahme** ist zulässige **physische Geräte-Evidence**
für sichtbar prüfbare App-Funktionen, auch bei eingeschaltetem Screenreader.
Die Aufnahme muss den betroffenen Build, die manuelle Screenreader-Aktivierung,
Fokus- und Navigationsschritte sowie die sichtbaren Ergebnisse nachvollziehbar
zuordnen lassen. Ein separates Audiofile ist für diesen **visuellen
Funktionsumfang** keine Pflicht.

Ohne Ton oder ein unabhängiges detailliertes Ansageprotokoll sind **Wortlaut,
Sprachausgabe und akustische Status-/Fehleransagen `NOT_PROVEN`**. Diese
Behauptungen dürfen nicht stillschweigend aus sichtbaren Fokusbewegungen
abgeleitet werden. Ein erfolgreich geprüfter visueller Umfang darf als
`VERIFIED` mit `verification_scope=VISUAL_DEVICE_FUNCTIONS` dokumentiert
werden; das ist **keine** Freigabe der vollständigen Sprach-/Release-Abnahme.
Für `FULL_SCREENREADER` bleibt ein belastbarer Ansagenachweis erforderlich.
`release_accessibility_readiness` bleibt ohne vollständige Prüfung
`NOT_PROVEN`. Der QA-04-Ausgangsstatus ist unverändert `NOT_PROVEN`,
bis konkrete Geräte-Evidence eingesehen wurde.

## Versionierung und Bestandsschutz

Die vorherige Vertragsversion `devicecare-screenreader-evidence/1` verlangt alle
fünf Checks einschließlich Ansagenachweis. Historische v1-Berichte behalten
ihre ursprüngliche Bewertung. Die Owner-Freigabe stummer visueller Nachweise
ist die neue, separat versionierte `devicecare-screenreader-evidence/2`-
Bewertung mit explizitem Geltungsbereich. Keine automatische Statusmigration
alter Aufnahmen, ohne tatsächlichen unabhängigen Re-Review.

## Gegenstand und Grenzen

- **Automatisiert:** Compose-Semantik, Überschriften, Content-Descriptions,
  Fokuszielgrößen (mindestens 48dp) und Live-Regionen können durch passende
  Unit-/UI-/Emulator-Tests überprüft werden. Diese Ergebnisse sind `CI_EVIDENCE`,
  **nicht** `DEVICE_SCREENREADER_EVIDENCE`.
- **Manuell:** Ein auf dem physischen Android-15-Gerät aktivierter Screenreader
  muss die beobachtbaren Fokus- und Bedienpfade der installierten Build-Version
  zeigen. Eine stumme Aufnahme genügt für diese sichtbaren Funktionen; hörbare
  Ansagen erfordern Audio oder ein separates nachvollziehbares Ansageprotokoll.
  Keine App-eigene AccessibilityService-Berechtigung, ADB-, Root- oder
  Screenreader-Fernsteuerung verwenden.
- **Ausgabe:** QA-04 bleibt bis zum echten Readback `NOT_PROVEN`.
  Die visuelle und die hörbare Prüfbehauptung werden unabhängig bewertet;
  eine stumme Aufnahme ist ausdrücklich als Prüfmedium zulässig.

## QA-04 — Durchzuführen auf Z2459 / Android 15

1. **Build-Identität**: Repository, Commit-SHA, APK-SHA-256, Signer-Fingerprint,
   Versionsname, Android-API-Level und Sicherheits-Patchmonat notieren.
2. **Screenreader-Identität**: tatsächlichen Screenreader mit Name, Version,
   sichtbarem Aktivierungsnachweis und Testdatum/Zeitzone notieren.
   Ob Sprachfeedback aktiv und korrekt hörbar war, nur bei belastbarem Beleg angeben.
3. **Navigation**: Startbildschirm, Prüf-Tab, Tabs, Überschriften,
   Bedienelemente und Zurück-Navigation ausschließlich über Screenreader-Gesten
   nachvollziehbar erreichen. Fokus darf nicht festhängen oder unsichtbar sein.
4. **Kontrollen**: Scan startet nur nach ausdrücklicher Aktion; Schaltflächen
   haben aussagekräftigen Namen, Rolle und gegebenenfalls Zustand. Keine
   unbeschrifteten oder mehrfach redundanten Fokusziele.
5. **Status- und Fehlerfeedback**: Sichtbare Ergebniswechsel und Fehlermeldungen
   nachvollziehen; hörbare Ansagen nur mit Audio oder unabhängigem Ansageprotokoll
   als bestanden markieren. Keine erfundenen Sicherheits-/Malware-Urteile oder
   stillen Hintergrund-Scans.
6. **Layout**: bei 200 % Schrift, kleiner Breite (320dp, sofern einstellbar),
   Portrait/Querformat und Hell/Dunkel keine unbedienbaren, abgeschnittenen
   oder überlagerten Kontrollen.
7. **Datenschutz**: Bildschirm-/Audioaufnahme vor Weitergabe auf E-Mails,
   App-Listen, Notifications, Identifikatoren, API-Keys und sonstige
   personenbezogene Informationen prüfen und nötigenfalls redigieren.

## Evidence-Schema — ein Eintrag je geprüfter Build-/Geräte-Kombination

```json
{
  "contract": "devicecare-screenreader-evidence/2",
  "qa_id": "QA-04",
  "status": "NOT_PROVEN",
  "verification_scope": "VISUAL_DEVICE_FUNCTIONS",
  "evidence_mode": "SILENT_SCREENRECORDING",
  "release_accessibility_readiness": "NOT_PROVEN",
  "device_model": "Z2459",
  "android_version": "15",
  "android_patch_month": null,
  "app_commit_sha": null,
  "apk_sha256": null,
  "apk_signer_sha256": null,
  "screenreader_name": null,
  "screenreader_version": null,
  "speech_feedback_enabled": null,
  "screenreader_activation_visibly_verified": null,
  "test_timestamp_iso8601": null,
  "checks": {
    "navigation": "NOT_PROVEN",
    "controls": "NOT_PROVEN",
    "announcements": "NOT_PROVEN",
    "large_font_and_layout": "NOT_PROVEN",
    "privacy_redaction": "NOT_PROVEN"
  },
  "evidence_location": null,
  "audio_claim_status": "NOT_PROVEN",
  "examiner": null
}
```

**Statusregel (scoped):** Bei `verification_scope=VISUAL_DEVICE_FUNCTIONS`
darf `status=VERIFIED` nur gesetzt werden, wenn `navigation`, `controls`,
`large_font_and_layout` und `privacy_redaction` jeweils `PASS` sind,
Build/Gerät/Screenreader-Identität vollständig und nachprüfbar vorliegen und
eine datensparsame echte Geräteaufnahme visuell geprüft wurde. Stumme
Bildschirmaufnahmen sind dabei ausdrücklich zugelassen. `announcements` und
`audio_claim_status` bleiben ohne Audio oder überprüfbares Ansageprotokoll
`NOT_PROVEN`; sie sind kein stillschweigendes `PASS`.

Für `verification_scope=FULL_SCREENREADER` ist zusätzlich
`announcements=PASS` mit eigenständigem Ansagenbeleg erforderlich.
`release_accessibility_readiness` darf nur nach der vollständigen, unabhängigen
Release-Abnahme auf `VERIFIED` gesetzt werden. Bei belegtem Defekt `FAIL`,
sonst `NOT_PROVEN`. Emulator-/Compose-Tests oder eine bloß installierte APK
sind niemals unabhängige Geräte-Evidence.

## Rücknahme / Governance

Das hier ist ein Akzeptanz- und Evidence-Vertrag, keine Änderung an Android-
Berechtigungen, Firebase, Signierung oder produktivem Scan. Bei Problemen
den zugehörigen PR revertieren. Bestehende Release-/CI-Gates und Owner-
Freigaben bleiben unverändert.
