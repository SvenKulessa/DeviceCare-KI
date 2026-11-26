package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R

data class QuickNavEntry(
  val id: String,
  val route: String,
  val title: String,
  val plainSubtitle: String,
  val icon: ImageVector,
  val isTopLevel: Boolean,
)

fun buildQuickNavigationEntries(): List<QuickNavEntry> =
  listOf(
    QuickNavEntry(
      id = "inspection",
      route = "inspection",
      title = "Start & System-Übersicht",
      plainSubtitle = "Speicher, Akku, Arbeitsspeicher und schneller Sicherheitsstart",
      icon = Icons.Filled.HealthAndSafety,
      isTopLevel = true,
    ),
    QuickNavEntry(
      id = "apps",
      route = "apps",
      title = "Alle Apps, System-Apps & Malware-Check",
      plainSubtitle = "Berechtigungen, Änderungen, Manipulationen und Firewall-Empfehlungen je App",
      icon = Icons.Filled.Apps,
      isTopLevel = true,
    ),
    QuickNavEntry(
      id = "rethink_guide",
      route = "rethink_guide",
      title = "Rethink Firewall, DNS & Netzwerk",
      plainSubtitle = "Klare Einstellungsempfehlungen für Alltag, Datenschutz oder Verdachtsfall",
      icon = Icons.Filled.Dns,
      isTopLevel = true,
    ),
    QuickNavEntry(
      id = "privacy_summary",
      route = "privacy_summary",
      title = "Datenschutz-Cockpit",
      plainSubtitle = "Alle Risiken und nächsten Schritte auf einen Blick",
      icon = Icons.Filled.Shield,
      isTopLevel = true,
    ),
    QuickNavEntry(
      id = "battery",
      route = "battery",
      title = "Akku-Monitor & 20 %-Warnung",
      plainSubtitle = "Live-Akkuzustand, Verlauf und Warnschwelle einstellen",
      icon = Icons.Filled.BatteryChargingFull,
      isTopLevel = false,
    ),
    QuickNavEntry(
      id = "system_security",
      route = "system_security",
      title = "System- & Sicherheitscheck",
      plainSubtitle = "Android-Version, Sicherheitspatch und Geräteschutzstatus",
      icon = Icons.Filled.Security,
      isTopLevel = false,
    ),
    QuickNavEntry(
      id = "ai_advisor",
      route = "ai_advisor",
      title = "KI-Sicherheitsberater (Opt-in)",
      plainSubtitle = "Verständliche Erklärungen und Fragen rund um Geräteschutz",
      icon = Icons.Filled.AutoAwesome,
      isTopLevel = false,
    ),
    QuickNavEntry(
      id = "cloud_sync",
      route = "cloud_sync",
      title = "Cloud-Notizen (Opt-in)",
      plainSubtitle = "Freiwillige Sicherung deiner Prüfnotizen in deinem persönlichen Cloud-Konto",
      icon = Icons.Filled.CloudQueue,
      isTopLevel = false,
    ),
    QuickNavEntry(
      id = "design",
      route = "design",
      title = "Design & Barrierefreiheit",
      plainSubtitle = "Farben, Kontrast, Hell/Dunkel-Modus und kompakte Ansicht",
      icon = Icons.Filled.Palette,
      isTopLevel = true,
    ),
    QuickNavEntry(
      id = "privacy_policy",
      route = "privacy_policy",
      title = "Datenschutz & Transparenz",
      plainSubtitle = "Welche Daten lokal bleiben und warum keine versteckten Scans laufen",
      icon = Icons.Filled.PrivacyTip,
      isTopLevel = false,
    ),
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNavigationSheet(
  currentRoute: String?,
  onSelectEntry: (QuickNavEntry) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val entries = buildQuickNavigationEntries()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier.testTag("quick_nav_sheet"),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      contentPadding = PaddingValues(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        Column(
          modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_devicecare_ki_brand),
            contentDescription = stringResource(R.string.brand_logo_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.widthIn(max = 200.dp).heightIn(min = 36.dp, max = 48.dp),
          )
          Text(
            text = "Schnell-Navigation",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
          )
          Text(
            text =
              "Wähle direkt den gewünschten Bereich — verständlich erklärt und ohne Suchen erreichbar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      items(entries, key = { it.id }) { entry ->
        val active = currentRoute == entry.route
        Card(
          modifier =
            Modifier.fillMaxWidth()
              .clip(MaterialTheme.shapes.large)
              .minimumInteractiveComponentSize()
              .clickable { onSelectEntry(entry) }
              .testTag("quick_nav_item_${entry.id}"),
          shape = MaterialTheme.shapes.large,
          colors =
            CardDefaults.cardColors(
              containerColor =
                if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            ),
          border =
            if (active) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = CircleShape,
              color =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
            ) {
              Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint =
                  if (active) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp).size(18.dp),
              )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                color =
                  if (active) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = entry.plainSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color =
                  if (active) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "${entry.title} öffnen",
              tint =
                if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
