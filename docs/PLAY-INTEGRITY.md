# Play Integrity für DeviceCare-KI

Firebase App Check verwendet in einem gezielt konfigurierten Release-Build
den Play-Integrity-Anbieter für `com.aistudio.formwerk.vktmrx`. Er wird beim
App-Start vor der Oberfläche registriert. Firebase bewertet die Attestierung
bei Firebase-Anfragen. Die lokale Geräteinspektion verwendet weiterhin ihre
eigenen Datenquellen. App Check liefert der App kein separates Geräteurteil
für den Inspektionsbericht.

## Interner Test

1. In der Play Console unter App-Integrität das zur App gehörende Firebase-
   bzw. Google-Cloud-Projekt mit der Play Integrity API verknüpfen.
2. In Firebase unter App Check den Play-Integrity-Anbieter für dasselbe
   Android-App-Objekt registrieren. Für den Play-Testtrack den SHA-256-
   Fingerabdruck des **Play-App-Signing-Zertifikats** verwenden. Die
   projektbezogene `google-services.json` lokal bzw. im freigegebenen
   Build-System bereitstellen. Die effektive Enforcement-Konfiguration
   für die genutzten Firebase-Produkte prüfen.
3. Mit dem bestehenden Upload-Schlüssel und einer im Testtrack zulässigen
   Versionsnummer bauen. Die Signaturkonfiguration des Projekts verwenden.

   ```sh
   gradle --no-daemon -Pdevicecare.playIntegrity=true :app:bundleRelease
   ```

4. Das Bundle nach Freigabe im internen Play-Testtrack bereitstellen. Auf
   dem ZTE Blade V70 mit dem Testkonto über Google Play installieren. Einen
   normalen Firebase-Aufruf ausführen und im Firebase-Projekt die
   App-Check-Anfragen prüfen. Den Ergebnisstatus, Build und Installationsweg
   dokumentieren. Zusätzlich die lokale Inspektion im Flugmodus ausführen.

Debug-Builds verwenden Firebase Debug App Check. Ein Debug-Build und die
Integritätsprüfung in der Play-Store-App liefern keinen Nachweis für die
App-Check-Integration des DeviceCare-Release-Builds.

## Rückweg

Ohne `-Pdevicecare.playIntegrity=true` verwendet ein Release-Build den
bisherigen reCAPTCHA-Anbieter. Bei einem Rückbau nach einer Provider-
Umstellung im Firebase-Projekt zuerst prüfen, welcher Anbieter dort
registriert ist und unter der aktuellen Richtlinie akzeptiert wird.

Quellen

- [Firebase App Check mit Play Integrity](https://firebase.google.com/docs/app-check/android/play-integrity-provider)
- [Interne Tests in Google Play](https://support.google.com/googleplay/android-developer/answer/9845334?hl=de)
