# QA-02 · Physisches Android 15 – Diagnoseanzeige und Messwerte

## Quellen und Grenzen
- Repository: `SvenKulessa/DeviceCare-KI`; Baseline: `9242f21834da1c7df0f2263066dd493401abb9f5`.
- Quelle: **vom Nutzer bereitgestellter Screenshot eines physischen Android-15-Geräts**, nicht Emulator-CI. Original-SHA-256: `31516abd07d7bacaeda2ffdbea80bc9e26735a5e392274763d17309fb069701a`.
- Der Screenshot selbst wird **nicht** in das öffentliche GitHub-Repository übernommen (Datensparsamkeit).
- Bildbeleg: Akkustand/Ladestatus/Akkutemperatur als generisches „von Android gemeldet“, Text des Geräteprofils kollabiert bis auf einzelne Zeichen. Ergebnisansicht existiert. Sichtbar sind 178 ms Ablauf und 448 ms Prozess-CPU, **nur App-Ausgaben, keine unabhängigen Messungen**.
- Nutzerbeobachtung: Ruckeln beim ersten Aufruf. Ursache, erster vs. zweiter Tap, automatische Ergebnisnavigation, physische Android-15-Abnahme, App-Build-SHA und TalkBack-Ansagen bleiben `NOT_PROVEN`.

## Reproduzierbarer Codepfad
- `InspectionScreen.kt`: `DeviceProfileSummaryCard` zeigte einen begrenzt gewichteten Label-Text neben einer **nicht breitenbegrenzten** langen Monospace-Wertspalte. `ObservationGroupCard` hatte dieselbe Zwei-Spalten-Gefahr.
- `InspectionPresentation.kt`: `availabilityText` gab bei beobachteten Akku-/App-Feldern nur Statusfloskeln statt vorhandener, validierter `DeviceObservation.value`/Einheiten aus.

## Begrenzter Fix
1. Diagnostik-Label und Wert vertikal, volle verfügbare Breite, weicher Textumbruch; zusammengefasste Accessibility-Semantik, separate Label-/Wert-Test-Tags.
2. Verfügbare Akkuwerte (%, °C), Lade-/Android-Systemstatus und Zahl **nur Android-sichtbarer** geprüfter Apps werden aus der vorhandenen erlaubten Observation abgebildet. Unverfügbare, verweigerte oder fehlgeschlagene Signale bleiben explizit unverfügbar; keine Schätzung.
3. Keine Änderung an Scan-Auslösung, Android-Berechtigungen, Manifest, Signierung, Netzwerk, Firebase, Speicherort oder CI-Schutzregeln.

## Testumfang und Negativfälle
- `InspectionPresentationValueTest`: gemessene Werte inkl. Einheiten sowie `UNSUPPORTED`/`DENIED` ohne erfundene Zahlen.
- `InspectionEvidenceLayoutTest`: Compose bei 320 dp/1,8x Schrift und 390 dp/2,0x Schrift (`density=1`: logische px); Bounds von Label und Wert dürfen nicht horizontal aus dem Testviewport ragen oder sich vertikal überschneiden; lesbare Screenreader-Semantik; ungemessene Temperaturen bleiben ausdrücklich `UNSUPPORTED`.
- Laufzeitbeleg nur über den echten CI-Job `android-ci.yml`: Trivy, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, APK-Signatur/Hash/Build-Identity. **Ein definierter Test ist kein PASS.**
- Danach erneute physische Abnahme am Android-15-Gerät (Ein-Tap-Scan, Scrollposition, 320/390 und große Schrift, TalkBack, Screenshots ohne sensible Daten).

## Freigabe
**Solange CI, Rulesets oder physische Android-15-Readbacks nicht vollständig belegt sind: `NOT_PROVEN`.**
Kein automatischer Merge, Release, Deployment, Publishing oder produktive Datenmutation aus diesem Nachweis.
Rollback: Feature-PR schließen bzw. den isolierten Diff revertieren; keine Migration oder Backfill nötig.


## P0: lokale Installationsidentität (SOLO2 · 2026-11-22)

Der Nutzer kann in der bestehenden Prüfoberfläche **explizit** auf
„App-Version und Signatur prüfen“ tippen. Ein einmaliger, nicht persistierter
`PackageManager`-Readback zeigt die eigene Paket-ID, Version und die SHA-256-
Fingerprints der **aktuell installierten Signer-Zertifikate**. Es werden keine
fremden Apps inventarisiert, keine neuen Android-Berechtigungen beantragt und
keine Daten an einen Provider übertragen. Bei unzugänglichen Signaturen wird
`NOT_PROVEN` statt eines angenommenen Signers angezeigt.

**Grenzen:** `OBSERVED` ist nur eine lokale Android-API-Beobachtung, keine
unabhängige Zertifikats- oder APK-Dateihash-Prüfung. Der CI-Artefakt-SHA-256 und
PR-HEAD/Test-Merge-SHA müssen getrennt dokumentiert werden; durch Vergleich mit
signiertem CI-Artefakt, sicherem Backup-/Rollback-Pfad und unabhängigem
physischen Readback entsteht erst Update-Evidence. Keine Installation,
Deinstallation oder Überschreibung aus dieser Anzeige ableiten.

Rollback: den isolierten PR-Diff revertieren; keine Datenmigration notwendig.
