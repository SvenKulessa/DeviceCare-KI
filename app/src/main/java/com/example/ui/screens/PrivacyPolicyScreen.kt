package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import java.util.Locale

/** Bundled offline privacy notice. Keep in sync with docs/PLAY-M1-PRIVACY-{DE,EN}.md. */
private data class PrivacySection(val tag: String, val title: String, val body: String)

private fun privacySections(isGerman: Boolean): List<PrivacySection> =
  if (isGerman) {
    listOf(
      PrivacySection(
        "controller",
        "1. Verantwortlicher und Kontakt",
        "DeviceCare KI (Paket com.aistudio.formwerk.vktmrx). Rechtlicher Herausgeber, ladungsfähige Anschrift und Datenschutzkontakt: [VOR FREIGABE EINTRAGEN]. Dies ist eine interne Vorabfassung, nicht zur öffentlichen Verwendung freigegeben.",
      ),
      PrivacySection(
        "scope",
        "2. Geltungsbereich",
        "Diese Erklärung gilt für die native Android-App DeviceCare KI (Paket com.aistudio.formwerk.vktmrx). Sie beschreibt aktuelle lokale Prüfungen sowie optional aktivierbare Cloud-Notizen und KI-Beratungsfunktionen, nicht jedoch künftige Drive- oder Enterprise-Control-Center-Backends (NOT_PROVEN).",
      ),
      PrivacySection(
        "access",
        "3. Lokaler Datenzugriff und Zweck",
        "Erst nach manuellem Antippen liest die App Akkuwerte (Ladestand, Status, Temperatur, Spannung, System-Health), grobe Speicher- und Thermiksignale, ein lokales Hardware- und Sensorinventar (einzelne Sensor-Stichproben erst nach separater Bestätigung), Android-sichtbare App- und Berechtigungsmetadaten sowie eigene App-Berechtigungen. Die Paketstichprobe der Schnellprüfung ist je nach Gerätebelastung auf 250, 75 oder 25 sichtbare Apps begrenzt.",
      ),
      PrivacySection(
        "security",
        "4. Optionaler System- und Google-Sicherheitscheck",
        "Eine getrennt manuell gestartete Prüfung liest Android-Version, API-Level, Sicherheitspatchdatum sowie verfügbare Versionsangaben von Google Play-Diensten, Play Store und aktivem WebView-Provider. Für GMS und Play Store bestehen enge Package-Queries. Es erfolgen keine Live-CVE-Abfrage, kein Malware-Scan und kein Nachweis über den tatsächlichen Play-Protect-Status.",
      ),
      PrivacySection(
        "storage",
        "5. Speicherung, Export und Löschung",
        "Diagnose-, Akku-, Hardware- und Berechtigungsergebnisse bleiben flüchtig im Arbeitsspeicher und werden nicht automatisch dauerhaft protokolliert. Design-Einstellungen liegen lokal über DataStore; auf expliziten Exportbefehl (CSV/JSON) geteilte Dateien unterliegen deiner Wahl über Android Share/FileProvider. Android-App-Backups sind über allowBackup=false deaktiviert. Opt-in Cloud Care Notes werden erst nach aktiver Google-Anmeldung in Firebase Firestore unter deinem Nutzerkonto gespeichert und sind dort editier- oder löschbar.",
      ),
      PrivacySection(
        "network",
        "6. Übermittlung, Cloud-Funktionen und KI",
        "Das App-Manifest deklariert die INTERNET-Berechtigung ausschließlich für explizit gestartete Online-Funktionen: optionale Anmeldung per Google Sign-In (Credential Manager) für Firebase Firestore Cloud Care Notes, optionale KI-Textanalyse über Firebase AI Logic (Gemini API) und optional aktivierbares Firebase App Check (Play Integrity in Release-Konfiguration standardmäßig deaktiviert). Ohne deine aktive Nutzung dieser Screens oder Buttons sendet die lokale Prüfung keine Diagnosedaten ins Netzwerk. Werbe- oder Tracking-SDKs sind nicht integriert.",
      ),
      PrivacySection(
        "permission",
        "7. Berechtigungen und Sicherheit",
        "Die App fordert keine gefährlichen Android-Laufzeitberechtigungen wie Kamera, Mikrofon, Standort, Kontakte, VPN oder AccessibilityService und nutzt kein Root oder ADB. Die App-Sichtbarkeit beschränkt sich auf reguläre Launcher-Intents sowie Google Play-Dienste und Play Store; rohe Sensor- oder Paketdetails werden nur bei explizitem Nutzeraufruf lokal verarbeitet. Nicht unterstützte Messwerte werden als unbekannt oder UNSUPPORTED markiert.",
      ),
      PrivacySection(
        "legal",
        "8. Rechtsgrundlage und Rechte",
        "Die endgültige DSGVO-Rechtsgrundlagen- und Drittlandtransferprüfung (insbesondere für optionale Google/Firebase-Dienste) ist vor Veröffentlichung durch den Verantwortlichen abzuschließen. Betroffene haben gesetzliche Rechte auf Auskunft, Berichtigung, Löschung, Einschränkung und Beschwerde bei einer Datenschutzaufsicht. Für freiwillige Supportkontakte gelten noch gesondert festzulegende Aufbewahrungs- und Löschregeln.",
      ),
      PrivacySection(
        "support",
        "9. Supportanfragen und Aufbewahrung",
        "Support- und Kontaktinformationen, die Nutzer außerhalb der App übermitteln, sind nicht Teil der lokalen App-Diagnose. Verantwortliche Stelle, Kontaktweg, Zwecke, Empfänger und Löschfristen für Supportvorgänge müssen vor einer externen Play-Freigabe verbindlich ergänzt werden.",
      ),
      PrivacySection(
        "updates",
        "10. Änderungen dieser Erklärung",
        "Fassung: 24.11.2026 – interner Entwurf. Bei Änderungen am Funktionsumfang, an SDKs, Datenflüssen oder Speicherfristen sind diese Erklärung, die In-App-Ansicht und die Play-Data-Safety-Angaben gemeinsam zu aktualisieren. Eine öffentlich zugängliche HTTPS-Datenschutz-URL ist vor einem Play-Upload nachzuweisen.",
      ),
    )
  } else {
    listOf(
      PrivacySection(
        "controller",
        "1. Controller and contact",
        "DeviceCare KI (package com.aistudio.formwerk.vktmrx). Legal publisher, postal address and privacy contact: [TO BE COMPLETED BEFORE RELEASE]. This is an internal draft and is not approved for public use.",
      ),
      PrivacySection(
        "scope",
        "2. Scope",
        "This notice covers the native Android app DeviceCare KI (package com.aistudio.formwerk.vktmrx). It covers current on-device checks as well as optional cloud notes and AI advisor features, and excludes future Drive or enterprise control-center backends (NOT_PROVEN).",
      ),
      PrivacySection(
        "access",
        "3. On-device access and purpose",
        "Only after explicit user action does the app read battery metrics (level, status, temperature, voltage, health), coarse memory and thermal signals, an on-device hardware and sensor inventory (individual sensor samples only on separate confirmation), Android-visible app and permission metadata, and DeviceCare KI's own permissions. Quick inspection sampling is capped at 250, 75, or 25 visible apps based on device load.",
      ),
      PrivacySection(
        "security",
        "4. Optional system and Google security check",
        "A separate user-initiated check reads Android version, API level, reported security patch date, and available version details for Google Play services, Play Store, and the active WebView provider. Visibility queries are strictly scoped to GMS and Play Store. No live CVE lookup, malware scan, or Play Protect verification is performed.",
      ),
      PrivacySection(
        "storage",
        "5. Retention, export and deletion",
        "Diagnostic, battery, hardware, and permission results remain transient in memory and are not automatically persisted as history. Design preferences use local DataStore; optional CSV/JSON exports occur only on explicit user action via Android Share/FileProvider. Android cloud backup is disabled via allowBackup=false. Opt-in Cloud Care Notes are saved in Firebase Firestore under your Google account only after sign-in and can be edited or deleted there.",
      ),
      PrivacySection(
        "network",
        "6. Transfers, cloud features and AI",
        "The app manifest declares the INTERNET permission solely for user-initiated online features: optional Google Sign-In (Credential Manager) for Firebase Firestore Cloud Care Notes, optional AI analysis via Firebase AI Logic (Gemini API), and optional Firebase App Check (Play Integrity disabled by default). Without explicitly using those screens or buttons, local inspection sends no diagnostic data over the network. No advertising or tracking SDKs are bundled.",
      ),
      PrivacySection(
        "permission",
        "7. Permissions and security",
        "The app requests no dangerous runtime permissions such as camera, microphone, location, contacts, VPN, or AccessibilityService, and never uses root or ADB. Package visibility is limited to standard launcher intents plus Google Play services and Play Store; sensor or package details are processed locally only on explicit action. Unsupported readings are marked unknown or UNSUPPORTED.",
      ),
      PrivacySection(
        "legal",
        "8. Legal basis and rights",
        "Final GDPR legal basis and cross-border transfer assessment (including optional Google/Firebase services) must be completed by the controller before public release. Data subjects have statutory rights of access, rectification, erasure, restriction, and complaint to a supervisory authority. Retention and deletion rules for voluntary support correspondence must be documented separately before release.",
      ),
      PrivacySection(
        "support",
        "9. Support correspondence",
        "Support details users send voluntarily outside the app are not part of on-device diagnostics. Before public Play release, the responsible entity, contact channel, purposes, recipients, and retention period for support requests must be confirmed.",
      ),
      PrivacySection(
        "updates",
        "10. Changes to this policy",
        "Version: 24 November 2026 – internal draft. Changes to shipped features, SDKs, data flows, or retention require updating this notice, the in-app view, and Play Data safety declarations together. A public HTTPS privacy policy URL must be verified before any Play upload.",
      ),
    )
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)
  val german = Locale.getDefault().language.equals("de", ignoreCase = true)
  val sections = privacySections(german)

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("privacy_policy_screen"),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (german) "Datenschutz & Transparenz" else "Privacy Policy",
            modifier = Modifier.semantics { heading() }.testTag("privacy_policy_title"),
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("privacy_policy_back"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.action_back),
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
          ),
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(22.dp),
          )
          Text(
            text =
              if (german) "Interner M1-Entwurf · keine Play-Freigabe · vor Scan jederzeit lesbar"
              else "Internal M1 draft · not approved for Play release · readable prior to any scan",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
      }

      sections.forEach { section ->
        Card(
          modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
          shape = MaterialTheme.shapes.large,
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = section.title,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.semantics { heading() }.testTag("privacy_policy_${section.tag}"),
            )
            Text(
              text = section.body,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Button(
        onClick = onBack,
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 860.dp)
            .minimumInteractiveComponentSize()
            .testTag("privacy_policy_back_footer"),
      ) {
        Text(if (german) "Zurück zur Übersicht" else "Back to overview")
      }
    }
  }
}
