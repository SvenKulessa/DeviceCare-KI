# DeviceCare KI – prüfbare Play Data-Safety-Matrix

Stand: 2026-11-24. **NICHT als Play-Console-Abgabe freigegeben.** Diese Matrix dokumentiert den Source-Stand für `com.aistudio.formwerk.vktmrx`. Finale AAB-, SBOM- und Laufzeit-Netzwerk-Nachweise sowie juristische Owner-Prüfung bleiben **NOT_PROVEN**.

Im Unterschied zur reinen M1-Offline-Vorschau enthält `DeviceCare KI` explizit opt-in-gesteuerte Cloud- und KI-Funktionen (Firebase Auth via Credential Manager, Firebase Firestore, Firebase AI Logic und Firebase App Check) sowie `INTERNET` im Manifest. Diese Matrix trennt deshalb strikt zwischen rein lokalen Diagnosen und opt-in Online-Pfaden.

| Kategorie / konkrete Daten | Zugriff / Zweck | Speicherung | Übermittlung / Empfänger | Vorläufige Play-Einschätzung | Nachweis / Vorbehalt |
|---|---|---|---|---|---|
| Akkuwerte (Ladestand, Status, Temperatur, Spannung, Health) | Manuelle lokale Diagnose (`InspectionScreen`, `BatteryDashboardScreen`) | Nur im RAM | Kein Upload aus der Diagnose | Nicht gesammelt / nicht geteilt (vorbehaltlich finaler Netzwerkmessung) | `BatteryMonitor.kt`, `InspectionCollector.kt` |
| Hardware- & Sensorinventar, manuelle Sensor-Stichproben | Einmalige lokale Inventur / Einzelsample auf separaten Tap | Nur im RAM | Kein Upload aus dem Hardware-Inventar | Nicht gesammelt / nicht geteilt (vorbehaltlich finaler Netzwerkmessung) | `HardwareInventory.kt`, `HardwareInspectionSection.kt` |
| Android-sichtbare App-Liste & Berechtigungsmetadaten | Lokaler Berechtigungs-Scan (250/75/25 bzw. Apps-Tab) | RAM; optionaler CSV/JSON-Export nur nach expliziter Nutzeraktion | Kein automatischer Upload; Exportziel durch Nutzer wählbar | Lokal verarbeitet; opt-in Export über OS-Share-Sheet | `InstalledAppsExporter.kt`, `FileProvider`, `<queries>` |
| Design-Einstellungen (Palette, Theme, Kachelform) | Lokale App-Konfiguration | Gerätelokal via DataStore (`allowBackup=false`) | Keine Übermittlung | Nur lokal gespeichert | `DesignPreferencesRepository.kt`, `AndroidManifest.xml` |
| System- & Google-Sicherheitscheck | Manuell gestarteter lokaler Versions-/Patch-Readback | Nur im RAM | Kein automatischer Upload (externe Links nur nach explizitem Tap) | Nicht gesammelt / nicht geteilt | `SystemSecurityAudit.kt`, `<queries>` für GMS & Vending |
| Google Sign-In (`CredentialManager` / Firebase Auth) | Opt-in Anmeldung für Cloud Care Notes | Firebase Auth Token / Session | Google Identity Services & Firebase Authentication (USA/global je nach Projekt) | **Opt-in Nutzerkonto / Anmeldung**: in Play Data Safety anzugeben | `CloudSyncScreen.kt`, `GoogleAuthHelper.kt` |
| Cloud Care Notes (Titel, Inhalt, Erledigt-Status, Zeitstempel) | Opt-in Nutzer-Notizen im `CloudSyncScreen` | Cloud Firestore (`users/{uid}/notes`), editier-/löschbar | Google Cloud / Firebase Firestore | **Opt-in Nutzerinhalt**: in Play Data Safety inkl. Löschpfad anzugeben | `CloudSyncScreen.kt`, `firestore.rules` |
| KI-Advisor-Prompts & optionaler anonymisierter Kontext | Opt-in Chat / Datenschutz-Tipps / Posture-Nächste-Schritte | Flüchtiger Chat-Verlauf im ViewModel; Serververarbeitung durch Gemini API | Firebase AI Logic / Google Gemini API | **Opt-in App-Interaktion / Nutzer-Prompts**: Übermittlung in Play Data Safety zu prüfen | `AiAdvisorViewModel.kt`, `GeminiAdvisorService.kt` |
| Firebase App Check (Play Integrity) | Optionaler Missbrauchsschutz für Firebase-Requests (`USE_PLAY_INTEGRITY=false` Standard) | App-Check-Token im SDK | Google Play Integrity / Firebase App Check (nur wenn aktiviert & Firebase aufgerufen) | Konfigurationsabhängig **NOT_PROVEN** am finalen Release-AAB | `FirebaseAppCheckInitializer.kt`, `app/build.gradle.kts` |
| Werbe-IDs, Ad-SDKs, Tracking, Background-VPN, Accessibility | Nicht vorhanden / nicht angefordert | Keine | Keine | Nicht vorhanden | `AndroidManifest.xml`, `app/build.gradle.kts` |
| Rechtlicher Herausgeber, Anschrift, Supportkontakt & Privacy-URL | Veröffentlichungs-Pflichtangaben | **NICHT VORHANDEN** | – | **BLOCKED** | Owner/Legal Review & öffentliches HTTPS-Hosting erforderlich |

## Go/No-Go-Prüfpunkte vor Google Play
1. Finale `applicationId`, Version (`versionCode`/`versionName`) und exakten Git-Commit-SHA mit dem signierten Release-AAB abgleichen.
2. Über `apkanalyzer` / `aapt2` das finale Merged Manifest, Permissions, Export-Komponenten und transitive SDKs prüfen.
3. Opt-in Kontolöschung / Note-Löschung sowie Play-Data-Safety-Antworten für Firebase Auth, Firestore und Firebase AI Logic exakt in der Play Console eintragen.
4. Rechtlichen Herausgeber, Supportkontakt und eine ohne Login erreichbare HTTPS-HTML-Datenschutzerklärung bereitstellen und `docs/PLAY-M1-PRIVACY-RELEASE-APPROVAL.json` nur nach Verifikation pflegen.
