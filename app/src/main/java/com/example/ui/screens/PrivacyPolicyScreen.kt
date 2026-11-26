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
import androidx.compose.ui.graphics.Color
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
        "DeviceCare KI. Rechtlicher Herausgeber, ladungsfähige Anschrift und Datenschutzkontakt: [VOR FREIGABE EINTRAGEN]. Dies ist eine Vorabfassung, noch nicht für den öffentlichen Store-Vertrieb freigegeben.",
      ),
      PrivacySection(
        "scope",
        "2. Geltungsbereich",
        "Diese Erklärung gilt für die Android-App DeviceCare KI. Sie beschreibt alle aktuellen lokalen Geräteprüfungen sowie die optional aktivierbaren Cloud-Notizen und die KI-Beratung.",
      ),
      PrivacySection(
        "access",
        "3. Lokaler Datenzugriff und Zweck",
        "Erst nach deinem aktiven Antippen liest die App Akkuwerte (Ladestand, Status, Temperatur, Spannung, Systemzustand), grobe Speicher- und Wärmesignale, ein lokales Hardware- und Sensorinventar (einzelne Sensortests erst nach weiterer Bestätigung) sowie vom System freigegebene App- und Berechtigungsangaben. Die Schnellprüfung begrenzt große App-Listen je nach Geräteauslastung automatisch.",
      ),
      PrivacySection(
        "security",
        "4. Optionaler System- und Google-Sicherheitscheck",
        "Eine getrennt manuell gestartete Prüfung liest Android-Version, gemeldetes Sicherheitspatchdatum sowie verfügbare Versionsangaben von Google Play-Diensten, Play Store und System-WebView. Es erfolgen keine Online-Schwachstellenabfrage, kein versteckter Malware-Scan und keine Aussage über den tatsächlichen Play-Protect-Status.",
      ),
      PrivacySection(
        "storage",
        "5. Speicherung, Export und Löschung",
        "Diagnose-, Akku-, Hardware- und Berechtigungsergebnisse bleiben ausschließlich flüchtig im Arbeitsspeicher und werden nicht automatisch als Verlauf gespeichert. Darstellungs- und Designwünsche liegen nur lokal auf deinem Gerät; ein Export als Datei (CSV/JSON) erfolgt nur auf deinen ausdrücklichen Wunsch über den Teilen-Dialog. Automatische Cloud-App-Backups sind deaktiviert. Optionale Cloud-Notizen werden erst nach aktiver Google-Anmeldung unter deinem Konto gesichert und können dort jederzeit geändert oder gelöscht werden.",
      ),
      PrivacySection(
        "network",
        "6. Übermittlung, Cloud-Funktionen und KI",
        "Netzwerkzugriffe erfolgen ausschließlich für von dir ausdrücklich gestartete Online-Funktionen: optionale Anmeldung mit deinem Google-Konto für persönliche Cloud-Notizen sowie optionale Text- und Datenschutzberatung über den integrierten KI-Berater. Ohne deine aktive Nutzung dieser Funktionen sendet die lokale Prüfung keinerlei Diagnosedaten ins Internet. Werbe- oder Tracking-Dienste sind nicht enthalten.",
      ),
      PrivacySection(
        "permission",
        "7. Berechtigungen und Sicherheit",
        "Die App fordert keine sensiblen Zugriffsrechte wie Kamera, Mikrofon, Standort, Kontakte, VPN oder Bedienungshilfen an und nutzt keine System-Sonderrechte. Ab Android 13 wird eine Mitteilungsberechtigung nur dann angefragt, wenn du die lokale Akku-Warnung unter 20 % selbst einschaltest. Nicht verfügbare Messwerte werden klar als unbekannt oder nicht verfügbar gekennzeichnet.",
      ),
      PrivacySection(
        "legal",
        "8. Rechtsgrundlage und Rechte",
        "Die abschließende rechtliche Einordnung (einschließlich optional genutzter Cloud- und KI-Dienste) erfolgt vor einer öffentlichen Freigabe durch den Herausgeber. Betroffene haben gesetzliche Rechte auf Auskunft, Berichtigung, Löschung, Einschränkung sowie Beschwerde bei einer Datenschutzaufsicht.",
      ),
      PrivacySection(
        "support",
        "9. Supportanfragen und Aufbewahrung",
        "Kontaktdaten oder Hinweise, die außerhalb der App an den Support gesendet werden, sind nicht Bestandteil der lokalen Geräteprüfung. Verantwortliche Stelle, Kontaktweg und Löschfristen für Supportvorgänge werden vor einer öffentlichen Freigabe ergänzt.",
      ),
      PrivacySection(
        "updates",
        "10. Änderungen dieser Erklärung",
        "Fassung: 24.11.2026 – Vorabfassung. Bei Änderungen am Funktionsumfang, an Datenflüssen oder Speicherfristen werden diese Erklärung und die In-App-Ansicht gemeinsam aktualisiert. Vor einer öffentlichen Veröffentlichung wird eine frei erreichbare HTTPS-Datenschutzseite bereitgestellt.",
      ),
    )
  } else {
    listOf(
      PrivacySection(
        "controller",
        "1. Controller and contact",
        "DeviceCare KI. Legal publisher, postal address and privacy contact: [TO BE COMPLETED BEFORE RELEASE]. This is a draft notice not yet approved for public store distribution.",
      ),
      PrivacySection(
        "scope",
        "2. Scope",
        "This notice covers the Android app DeviceCare KI. It describes all current on-device checks as well as the optional Cloud Notes and AI Advisor features.",
      ),
      PrivacySection(
        "access",
        "3. On-device access and purpose",
        "Only after explicit user action does the app read battery metrics (level, status, temperature, voltage, health), coarse memory and thermal signals, an on-device hardware and sensor inventory (individual sensor samples only after separate confirmation), and system-visible app and permission details. Quick inspection caps large app lists automatically based on device load.",
      ),
      PrivacySection(
        "security",
        "4. Optional system and Google security check",
        "A separate user-initiated check reads the Android version, reported security patch date, and available version details for Google Play services, Play Store, and the active WebView component. No live vulnerability lookup, hidden malware scan, or Play Protect verification is performed.",
      ),
      PrivacySection(
        "storage",
        "5. Retention, export and deletion",
        "Diagnostic, battery, hardware, and permission results remain transient in memory and are not automatically saved as history. Display preferences stay locally on your device; optional CSV/JSON file exports run only on explicit request via the system share sheet. Automatic cloud app backups are disabled. Optional Cloud Notes are stored under your Google account only after sign-in and can be edited or deleted there at any time.",
      ),
      PrivacySection(
        "network",
        "6. Transfers, cloud features and AI",
        "Network access is used solely for online features you explicitly start: optional Google Sign-In for personal Cloud Notes and optional text/privacy guidance in the integrated AI Advisor. Without actively using those features, local inspection sends no diagnostic data over the internet. No advertising or tracking services are included.",
      ),
      PrivacySection(
        "permission",
        "7. Permissions and security",
        "The app never requests sensitive permissions such as camera, microphone, location, contacts, VPN, or accessibility services, and never uses privileged system overrides. On Android 13+, notification permission is requested only if you explicitly enable local low-battery (< 20%) alerts. Unavailable readings are clearly marked as unknown or unavailable.",
      ),
      PrivacySection(
        "legal",
        "8. Legal basis and rights",
        "Final GDPR legal basis and transfer assessments (including optional cloud and AI services) will be completed by the publisher prior to public release. Data subjects have statutory rights of access, rectification, erasure, restriction, and complaint to a supervisory authority.",
      ),
      PrivacySection(
        "support",
        "9. Support correspondence",
        "Support details sent voluntarily outside the app are not part of on-device diagnostics. Before public release, the responsible entity, contact channel, and retention period for support requests will be confirmed.",
      ),
      PrivacySection(
        "updates",
        "10. Changes to this policy",
        "Version: 24 November 2026 – draft notice. Changes to features, data flows, or retention require updating this notice and the in-app view together. A publicly accessible HTTPS privacy page will be provided before public distribution.",
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
    containerColor = Color.Transparent,
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
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
              if (german) "Vorabfassung · nicht zur öffentlichen Verbreitung freigegeben · vor jedem Scan einsehbar"
              else "Draft notice · not approved for public release · readable prior to any scan",
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
