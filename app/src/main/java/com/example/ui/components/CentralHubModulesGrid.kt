package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.DeviceCareUiState
import com.example.ui.theme.MonoTokenTextStyle

private data class HubModuleItem(
  val title: String,
  val statusText: String,
  val icon: ImageVector,
  val tag: String,
  val onClick: () -> Unit,
)

@Composable
internal fun CentralHubModulesGrid(
  uiState: DeviceCareUiState,
  isWide: Boolean,
  onOpenAppPermissions: () -> Unit,
  onOpenSystemSecurity: () -> Unit,
  onOpenBatteryMonitor: () -> Unit,
  onOpenPrivacySummary: () -> Unit,
  onOpenAiAdvisor: () -> Unit,
  onOpenCloudSync: () -> Unit,
) {
  val items =
    listOf(
      HubModuleItem(
        title = "App-Rechte",
        statusText =
          uiState.installedAppsScanResult?.let { "${it.visiblePackageCount} Apps" }
            ?: "Bereit · lokal",
        icon = Icons.Filled.Apps,
        tag = "hub_module_apps",
        onClick = onOpenAppPermissions,
      ),
      HubModuleItem(
        title = "System-Check",
        statusText =
          uiState.systemSecuritySnapshot?.let { "API ${it.apiLevel}" } ?: "Patch & Play",
        icon = Icons.Filled.Security,
        tag = "hub_module_security",
        onClick = onOpenSystemSecurity,
      ),
      HubModuleItem(
        title = "Akku-Monitor",
        statusText = uiState.batterySnapshot?.percent?.let { "$it% live" } ?: "Detail-Telemetrie",
        icon = Icons.Filled.BatteryChargingFull,
        tag = "hub_module_battery",
        onClick = onOpenBatteryMonitor,
      ),
      HubModuleItem(
        title = "Datenschutz",
        statusText = "Risiko-Cockpit",
        icon = Icons.Filled.Shield,
        tag = "hub_module_privacy_summary",
        onClick = onOpenPrivacySummary,
      ),
      HubModuleItem(
        title = "KI-Berater",
        statusText = "Opt-in · Gemini",
        icon = Icons.Filled.AutoAwesome,
        tag = "hub_module_ai",
        onClick = onOpenAiAdvisor,
      ),
      HubModuleItem(
        title = "Cloud-Sync",
        statusText = "Opt-in · Notizen",
        icon = Icons.Filled.CloudQueue,
        tag = "hub_module_cloud",
        onClick = onOpenCloudSync,
      ),
    )

  val columns = if (isWide) 3 else 2
  val rows = items.chunked(columns)

  Column(
    modifier = Modifier.fillMaxWidth().testTag("hub_modules_grid"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = stringResource(R.string.hub_modules_title),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.semantics { heading() },
    )
    rows.forEach { rowItems ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        rowItems.forEach { item ->
          Card(
            modifier =
              Modifier.weight(1f)
                .testTag(item.tag)
                .clip(MaterialTheme.shapes.medium)
                .minimumInteractiveComponentSize()
                .clickable(onClick = item.onClick),
            shape = MaterialTheme.shapes.medium,
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
              ),
            border =
              BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(7.dp).size(18.dp),
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = item.statusText,
                  style = MonoTokenTextStyle,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
        repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
      }
    }
  }
}
