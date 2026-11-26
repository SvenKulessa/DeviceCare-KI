package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.devicecare.SettingsLaunchResult
import com.example.devicecare.openDeviceSettings
import com.example.security.firewall.AppProtectionAnalyzer
import com.example.security.firewall.FirewallAction
import com.example.security.firewall.RethinkProfile
import com.example.security.firewall.RethinkSettingCategory
import com.example.security.firewall.RethinkSettingRecommendation
import com.example.ui.DeviceCareUiState
import com.example.ui.theme.MonoTokenTextStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RethinkRecommendationsScreen(
  uiState: DeviceCareUiState,
  onBack: () -> Unit,
  onOpenAppPermissions: () -> Unit,
  onShowSnackbar: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val compact = uiState.preferences.compactDensity
  var selectedProfile by remember { mutableStateOf(RethinkProfile.EVERYDAY) }
  var selectedCategory by remember { mutableStateOf<RethinkSettingCategory?>(null) }

  val scanResult = uiState.installedAppsScanResult
  val allRecommendations =
    remember(selectedProfile, scanResult) {
      AppProtectionAnalyzer.rethinkRecommendationsFor(selectedProfile, scanResult)
    }
  val visibleRecommendations =
    remember(allRecommendations, selectedCategory) {
      if (selectedCategory == null) allRecommendations
      else allRecommendations.filter { it.category == selectedCategory }
    }

  fun openVpnSettings() {
    when (openDeviceSettings(Intent(Settings.ACTION_VPN_SETTINGS)) { context.startActivity(it) }) {
      SettingsLaunchResult.DIRECT -> Unit
      SettingsLaunchResult.GENERAL_SETTINGS ->
        onShowSnackbar("Öffne in Android: Netzwerk & Internet → VPN → Rethink.")
      SettingsLaunchResult.UNAVAILABLE ->
        onShowSnackbar("Android-VPN-Einstellungen nicht direkt verfügbar.")
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("rethink_recommendations_screen"),
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Rethink-Schutz & Firewall",
              style = MaterialTheme.typography.titleLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.semantics { heading() },
            )
            Text(
              text = "Empfehlungen für Firewall, DNS & Netzwerk",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("rethink_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.action_back),
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f)
          ),
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .testTag("rethink_recommendations_list")
          .semantics { liveRegion = LiveRegionMode.Polite },
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Plain-language explanation card for clarity and accessibility
      item {
        Card(
          modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp),
          shape = MaterialTheme.shapes.large,
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
          border =
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(top = 2.dp).size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Kurz erklärt: Wie nutzt du diese Empfehlungen?",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.semantics { heading() },
              )
              Text(
                text =
                  "DeviceCare KI verändert keine VPN- oder Rethink-Einstellungen automatisch. " +
                    "Wähle dein gewünschtes Sicherheitsprofil und übertrage die passenden Firewall-, " +
                    "DNS- und Netzwerk-Schalter direkt in Rethink oder den Android-VPN-Einstellungen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }
      }

      // Live local scan bridge summary
      item {
        Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
          val apps = scanResult?.apps.orEmpty()
          val blockCount =
            apps.count { it.securityPosture?.firewallRecommendation?.action == FirewallAction.BLOCK_ALL }
          val fgCount =
            apps.count {
              it.securityPosture?.firewallRecommendation?.action ==
                FirewallAction.ALLOW_FOREGROUND_ONLY
            }
          val flaggedCount = apps.count { it.securityPosture?.hasManipulationWarnings == true }

          Card(
            modifier = Modifier.fillMaxWidth().testTag("rethink_scan_bridge_card"),
            shape = MaterialTheme.shapes.large,
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border =
              BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = "Abgleich mit deinen installierten Apps & System-Apps",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
              )
              if (scanResult == null) {
                Text(
                  text =
                    "Noch kein App-Scan ausgeführt. Starte im Bereich „Apps“ den Scan für konkrete " +
                      "Firewall-Empfehlungen je App und System-App.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              } else {
                Text(
                  text =
                    "${scanResult.visiblePackageCount} sichtbare Apps geprüft: " +
                      "$blockCount× Komplett blockieren · $fgCount× Nur Vordergrund · " +
                      "$flaggedCount× Manipulations-/Quellenhinweis.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                FilledTonalButton(
                  onClick = onOpenAppPermissions,
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("rethink_open_apps_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("App-Regeln ansehen", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                  onClick = ::openVpnSettings,
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("rethink_open_vpn_settings_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.VpnKey,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("VPN-Einstellungen", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }
          }
        }
      }

      // Security profile selector
      item {
        Column(
          modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "1. Schutzprofil wählen",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            RethinkProfile.entries.forEach { profile ->
              val selected = profile == selectedProfile
              FilterChip(
                selected = selected,
                onClick = { selectedProfile = profile },
                label = { Text(profile.title) },
                leadingIcon =
                  if (selected) {
                    {
                      Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  } else null,
                modifier = Modifier.testTag("rethink_profile_${profile.name.lowercase()}"),
              )
            }
          }
          Text(
            text = selectedProfile.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Category filter row
      item {
        Column(
          modifier =
            Modifier.fillMaxWidth().widthIn(max = 880.dp).testTag("rethink_category_section"),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "2. Empfehlungen nach Bereich",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = selectedCategory == null,
              onClick = { selectedCategory = null },
              label = { Text("Alle Bereiche") },
              modifier = Modifier.testTag("rethink_cat_all"),
            )
            RethinkSettingCategory.entries.forEach { category ->
              FilterChip(
                selected = selectedCategory == category,
                onClick = {
                  selectedCategory = if (selectedCategory == category) null else category
                },
                label = { Text(category.label) },
                modifier = Modifier.testTag("rethink_cat_${category.name.lowercase()}"),
              )
            }
          }
        }
      }

      items(visibleRecommendations, key = { it.id }) { rec ->
        Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
          RethinkSettingRecommendationCard(rec = rec)
        }
      }

      // Honest NOT_PROVEN transparency notice
      item {
        Card(
          modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp),
          shape = MaterialTheme.shapes.large,
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Outlined.PrivacyTip,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(top = 2.dp).size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Transparenz-Grenze",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.semantics { heading() },
              )
              Text(
                text =
                  "DeviceCare KI liest keine laufenden Rethink-Regeln direkt aus dem VPN-Speicher. " +
                    "Ob Always-on VPN und DNS-Filter aktiv sind, bleibt ohne deine Prüfung in Rethink " +
                    "oder den Android-VPN-Einstellungen lokal unbestätigt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
          }
        }
      }

      item {
        Button(
          onClick = onBack,
          modifier =
            Modifier.fillMaxWidth()
              .widthIn(max = 880.dp)
              .minimumInteractiveComponentSize()
              .testTag("rethink_back_footer_button"),
        ) {
          Text("Zurück zur Übersicht")
        }
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun RethinkSettingRecommendationCard(rec: RethinkSettingRecommendation) {
  val icon =
    when (rec.category) {
      RethinkSettingCategory.FIREWALL -> Icons.Filled.Shield
      RethinkSettingCategory.DNS -> Icons.Filled.Dns
      RethinkSettingCategory.NETWORK -> Icons.Filled.Security
    }
  val isUrgent = rec.priorityLabel == "KRITISCH" || rec.priorityLabel == "HOCH"
  val badgeBg =
    when (rec.priorityLabel) {
      "KRITISCH" -> MaterialTheme.colorScheme.error
      "HOCH" -> MaterialTheme.colorScheme.primaryContainer
      "MITTEL" -> MaterialTheme.colorScheme.tertiaryContainer
      else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
  val badgeFg =
    when (rec.priorityLabel) {
      "KRITISCH" -> MaterialTheme.colorScheme.onError
      "HOCH" -> MaterialTheme.colorScheme.onPrimaryContainer
      "MITTEL" -> MaterialTheme.colorScheme.onTertiaryContainer
      else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("rethink_rec_${rec.id}"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border =
      BorderStroke(
        width = if (isUrgent) 1.2.dp else 1.dp,
        color =
          if (rec.priorityLabel == "KRITISCH") MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(6.dp).size(16.dp),
            )
          }
          Text(
            text = rec.category.label,
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Surface(shape = MaterialTheme.shapes.small, color = badgeBg) {
          Text(
            text = "PRIORITÄT · ${rec.priorityLabel}",
            style = MonoTokenTextStyle,
            color = badgeFg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }

      Text(
        text = rec.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
      )

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "Pfad: ${rec.settingPath}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Empfehlung: ${rec.recommendedValue}",
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Text(
        text = rec.plainExplanation,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      if (!rec.cautionNote.isNullOrBlank()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "Hinweis: ${rec.cautionNote}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
