# DeviceCare-KI · lokale Hardware-Inventur (PRODUCT-HW-01)

## Ausführung und Quellen

Das bestehende UI **„Lokale Prüfung starten“** startet ausschließlich auf Nutzereingabe. Nach dem Akku- und Berechtigungsdurchlauf erfasst die neue Anzeige im Arbeitsspeicher:

- Hersteller/Modell, Android-Version, Build.HARDWARE und unterstützte App-CPU-ABIs (Android Build); verfügbare Prozessoren sind **keine** bestätigte physische SoC-Kernzahl.
- Kernel-sichtbarer RAM und verfügbarer RAM (ActivityManager.MemoryInfo), Zustand „low memory“.
- Gesamtkapazität und für Apps verfügbare Bytes der **internen App-Datenpartition** (StatFs); weder die Gesamtkapazität aller Speichermedien noch eine SD-Karteninventur wird behauptet.
- Alle über SensorManager.getSensorList(TYPE_ALL) gemeldeten Sensoren, inklusive Name, Typ und Hersteller, bis zu einem verifizierbaren Budget von 128 Listeneinträgen. Gesamtsumme und Kürzung werden ausdrücklich angezeigt.
- Android PackageManager System-Features: Kategorien Kamera, Mikrofon, GPS, WLAN, Bluetooth, NFC, Biometrie, USB, Touch, Mobilfunk, Audio und Vibration sowie weitere vorhandene deklarierte Features bis zum angegebenen Limit. **FEATURE gemeldet** bedeutet nicht **funktional getestet**.

Die UI zeigt für fehlende oder verweigerte Dienste **Nicht verfügbar / fehlgeschlagen**, niemals den Wert 0 als Ersatz. Sensor- und Merkmalslisten sind nur so vollständig, wie Android sie der App zugänglich macht. Kein Root, ADB, IMEI, Seriennummer, Advertising ID, Netzwerkzugriff, Cloud-Upload, Firebase-Nutzung oder neue Android-Berechtigung.

## Test- und Privatsphärengrenzen

Das Inventar startet **keinen** Sensorstream. Ein separater Button pro explizit erlaubtem, unkritischem Sensor wartet maximal 3 Sekunden auf **ein** Ereignis mit normaler Samplingrate. Listener wird bei Erfolg, Timeout und Abbruch deregistriert. Erkannt / OBSERVED ist **nicht** VERIFIED, „kein Ereignis“ ist **NOT_PROVEN**, kein Defektnachweis.

Bewegungs-Trigger, Schrittzählung, Herz-/Körpersensorik, herstellerspezifische Sensoren und alle unbekannten Typen haben keinen Mess-Button. Kamera, Mikrofon, GPS, NFC, Biometrie, WLAN, Bluetooth und weitere Funktionen erhalten nur eine Anleitung zur selbstbestimmten Prüfung, ohne dass die App diese Funktionen aktivieren oder Nutzdaten erfassen muss.

## Pflicht- und Negativtests

1. Android-Unit: erlaubte passive Sensortypen, ausgeschlossene sensitive/unbekannte Typen, leerer/nicht-finiter Messwert als NOT_PROVEN, gültiger Sensorwert nur als OBSERVED.
2. Android-Lint und signierte interne Debug-APK, apksigner, Trivy-/Lizenz-/Secret-/Manifest-Scan aus der bestehenden CI.
3. Reale Geräteabnahme: ZTE Blade V70, Android 15. CPU/RAM/Speicher gegen Systemanzeige plausibilisieren, alle sichtbaren Sensoren und Systemmerkmale zählen, opt-in Sensor-Event mit echten Bewegungen/Lichtänderungen testen, Timeout, Sperrbildschirm/Abbruch, TalkBack/Screenreader-Fokus, große Schrift und Wiederanlauf prüfen. 320/390/1440 CSS-Pixel-ähnliche Android-DP-Breiten bzw. relevante Layouts separat prüfen.
4. Synthetische Tests dürfen reale Gerätefunktion nicht bestätigen; QA-04 bleibt bis zu echten gesprochenen Screenreader-Nachweisen NOT_PROVEN.

## Revert

Den Feature-PR zurücksetzen. Es gibt keine Datenschema-, Berechtigungs-, Firebase- oder Konfigurationsmigration. Interne Debug-Builds sind keine Play-Releases.
