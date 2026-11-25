# Datenschutzerklärung – DeviceCare KI

> **INTERNER ENTWURF / RELEASE GESPERRT.** Verantwortlicher, Postadresse, Datenschutzkontakt, Rechtsgrundlage, Support-Aufbewahrung und öffentlich erreichbare HTTPS-URL sind vor einem Google-Play-Upload verbindlich zu ergänzen. Wortlaut synchron mit der In-App-Ansicht, vorbehaltlich finaler juristischer Prüfung.

- Produkt: DeviceCare KI
- Paket: `com.aistudio.formwerk.vktmrx`
- Fassung: 2026-11-24
- Öffentliche Datenschutz-URL: **NOT_PROVEN – vor Freigabe nachweisen**

## 1. Verantwortlicher und Kontakt

DeviceCare KI (Paket com.aistudio.formwerk.vktmrx). Rechtlicher Herausgeber, ladungsfähige Anschrift und Datenschutzkontakt: [VOR FREIGABE EINTRAGEN]. Dies ist eine interne Vorabfassung, nicht zur öffentlichen Verwendung freigegeben.

## 2. Geltungsbereich

Diese Erklärung gilt für die native Android-App DeviceCare KI (Paket com.aistudio.formwerk.vktmrx). Sie beschreibt aktuelle lokale Prüfungen sowie optional aktivierbare Cloud-Notizen und KI-Beratungsfunktionen, nicht jedoch künftige Drive- oder Enterprise-Control-Center-Backends (NOT_PROVEN).

## 3. Lokaler Datenzugriff und Zweck

Erst nach manuellem Antippen liest die App Akkuwerte (Ladestand, Status, Temperatur, Spannung, System-Health), grobe Speicher- und Thermiksignale, ein lokales Hardware- und Sensorinventar (einzelne Sensor-Stichproben erst nach separater Bestätigung), Android-sichtbare App- und Berechtigungsmetadaten sowie eigene App-Berechtigungen. Die Paketstichprobe der Schnellprüfung ist je nach Gerätebelastung auf 250, 75 oder 25 sichtbare Apps begrenzt.

## 4. Optionaler System- und Google-Sicherheitscheck

Eine getrennt manuell gestartete Prüfung liest Android-Version, API-Level, Sicherheitspatchdatum sowie verfügbare Versionsangaben von Google Play-Diensten, Play Store und aktivem WebView-Provider. Für GMS und Play Store bestehen enge Package-Queries. Es erfolgen keine Live-CVE-Abfrage, kein Malware-Scan und kein Nachweis über den tatsächlichen Play-Protect-Status.

## 5. Speicherung, Export und Löschung

Diagnose-, Akku-, Hardware- und Berechtigungsergebnisse bleiben flüchtig im Arbeitsspeicher und werden nicht automatisch dauerhaft protokolliert. Design-Einstellungen liegen lokal über DataStore; auf expliziten Exportbefehl (CSV/JSON) geteilte Dateien unterliegen deiner Wahl über Android Share/FileProvider. Android-App-Backups sind über allowBackup=false deaktiviert. Opt-in Cloud Care Notes werden erst nach aktiver Google-Anmeldung in Firebase Firestore unter deinem Nutzerkonto gespeichert und sind dort editier- oder löschbar.

## 6. Übermittlung, Cloud-Funktionen und KI

Das App-Manifest deklariert die INTERNET-Berechtigung ausschließlich für explizit gestartete Online-Funktionen: optionale Anmeldung per Google Sign-In (Credential Manager) für Firebase Firestore Cloud Care Notes, optionale KI-Textanalyse über Firebase AI Logic (Gemini API) und optional aktivierbares Firebase App Check (Play Integrity in Release-Konfiguration standardmäßig deaktiviert). Ohne deine aktive Nutzung dieser Screens oder Buttons sendet die lokale Prüfung keine Diagnosedaten ins Netzwerk. Werbe- oder Tracking-SDKs sind nicht integriert.

## 7. Berechtigungen und Sicherheit

Die App fordert keine gefährlichen Android-Laufzeitberechtigungen wie Kamera, Mikrofon, Standort, Kontakte, VPN oder AccessibilityService und nutzt kein Root oder ADB. Die App-Sichtbarkeit beschränkt sich auf reguläre Launcher-Intents sowie Google Play-Dienste und Play Store; rohe Sensor- oder Paketdetails werden nur bei explizitem Nutzeraufruf lokal verarbeitet. Nicht unterstützte Messwerte werden als unbekannt oder UNSUPPORTED markiert.

## 8. Rechtsgrundlage und Rechte

Die endgültige DSGVO-Rechtsgrundlagen- und Drittlandtransferprüfung (insbesondere für optionale Google/Firebase-Dienste) ist vor Veröffentlichung durch den Verantwortlichen abzuschließen. Betroffene haben gesetzliche Rechte auf Auskunft, Berichtigung, Löschung, Einschränkung und Beschwerde bei einer Datenschutzaufsicht. Für freiwillige Supportkontakte gelten noch gesondert festzulegende Aufbewahrungs- und Löschregeln.

## 9. Supportanfragen und Aufbewahrung

Support- und Kontaktinformationen, die Nutzer außerhalb der App übermitteln, sind nicht Teil der lokalen App-Diagnose. Verantwortliche Stelle, Kontaktweg, Zwecke, Empfänger und Löschfristen für Supportvorgänge müssen vor einer externen Play-Freigabe verbindlich ergänzt werden.

## 10. Änderungen dieser Erklärung

Fassung: 24.11.2026 – interner Entwurf. Bei Änderungen am Funktionsumfang, an SDKs, Datenflüssen oder Speicherfristen sind diese Erklärung, die In-App-Ansicht und die Play-Data-Safety-Angaben gemeinsam zu aktualisieren. Eine öffentlich zugängliche HTTPS-Datenschutz-URL ist vor einem Play-Upload nachzuweisen.

---

**Freigabesperre:** Öffentliches Hosting mit gültigem TLS, weltweiter Erreichbarkeit und ohne Login sowie Abgleich mit dem endgültigen signierten AAB und Play-Data-Safety-Formular sind noch nicht belegt.
