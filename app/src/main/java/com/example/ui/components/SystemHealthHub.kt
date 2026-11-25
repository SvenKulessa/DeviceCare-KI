package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.devicecare.InspectionResult
import com.example.ui.DeviceCareUiState
import com.example.ui.theme.MonoTokenTextStyle
import kotlin.math.roundToInt

/**
 * Central System Health Hub widget displaying Storage, Battery, and Memory status
 * plus quick navigation cards across DeviceCare KI's functional pillars.
 * Never fabricates telemetry before explicit user scan.
 */
@Composable
fun SystemHealthDashboardHub(
  uiState: DeviceCareUiState,
  isWide: Boolean,
  onStartInspection: () -> Unit,
  onOpenBatteryMonitor: () -> Unit,
  onOpenAppPermissions: () -> Unit,
  onOpenSystemSecurity: () -> Unit,
  onOpenPrivacySummary: () -> Unit,
  onOpenAiAdvisor: () -> Unit,
  onOpenCloudSync: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val result = uiState.inspectionResult
  val isStale = uiState.isInspectionStale
  val statusLabel =
    when {
      result == null -> "BEREIT · NOT_PROVEN"
      isStale -> "VERALTET"
      else -> "LOKAL · LIVE"
    }
  val statusBg =
    when {
      result == null -> MaterialTheme.colorScheme.tertiaryContainer
      isStale -> MaterialTheme.colorScheme.errorContainer
      else -> MaterialTheme.colorScheme.secondaryContainer
    }
  val statusFg =
    when {
      result == null -> MaterialTheme.colorScheme.onTertiaryContainer
      isStale -> MaterialTheme.colorScheme.onErrorContainer
      else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

  Column(
    modifier = modifier.fillMaxWidth().testTag("system_health_dashboard_hub"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.hub_telemetry_title),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          text =
            if (result == null) stringResource(R.string.hub_telemetry_subtitle_idle)
            else stringResource(R.string.hub_telemetry_subtitle_live),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Surface(shape = MaterialTheme.shapes.small, color = statusBg) {
        Text(
          text = statusLabel,
          style = MonoTokenTextStyle,
          color = statusFg,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }

    if (isWide) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        BatteryTelemetryHubCard(
          result = result,
          onClick = onOpenBatteryMonitor,
          modifier = Modifier.weight(1f),
        )
        MemoryTelemetryHubCard(
          result = result,
          onClick = onStartInspection,
          modifier = Modifier.weight(1f),
        )
        StorageTelemetryHubCard(
          result = result,
          onClick = onStartInspection,
          modifier = Modifier.weight(1.15f),
        )
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        BatteryTelemetryHubCard(
          result = result,
          onClick = onOpenBatteryMonitor,
          modifier = Modifier.weight(1f),
        )
        MemoryTelemetryHubCard(
          result = result,
          onClick = onStartInspection,
          modifier = Modifier.weight(1f),
        )
      }
      StorageTelemetryHubCard(
        result = result,
        onClick = onStartInspection,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    CentralHubModulesGrid(
      uiState = uiState,
      isWide = isWide,
      onOpenAppPermissions = onOpenAppPermissions,
      onOpenSystemSecurity = onOpenSystemSecurity,
      onOpenBatteryMonitor = onOpenBatteryMonitor,
      onOpenPrivacySummary = onOpenPrivacySummary,
      onOpenAiAdvisor = onOpenAiAdvisor,
      onOpenCloudSync = onOpenCloudSync,
    )
  }
}

@Composable
private fun BatteryTelemetryHubCard(
  result: InspectionResult?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val pct = result?.batteryPercent
  val progress = (pct ?: 0) / 100f
  val accentColor =
    when {
      pct == null -> MaterialTheme.colorScheme.outline
      pct <= 20 -> MaterialTheme.colorScheme.error
      pct >= 75 -> MaterialTheme.colorScheme.secondary
      else -> MaterialTheme.colorScheme.primary
    }
  val subtitle =
    when {
      result == null -> "Nach Scan"
      result.batteryChargingState == "CHARGING" -> "Lädt · ${result.batteryTemperatureDisplay ?: "—"}"
      result.batteryChargingState == "FULL" -> "Voll geladen"
      result.batteryChargingState != null -> result.batteryTemperatureDisplay ?: result.batteryChargingState
      else -> "UNSUPPORTED"
    }

  TelemetryGaugeCard(
    title = "Akku",
    icon = Icons.Filled.BatteryChargingFull,
    fraction = progress,
    centerText = if (pct != null) "$pct%" else "—",
    primaryDetail =
      when {
        result == null -> "Manuell starten"
        result.batteryChargingState == "CHARGING" -> "Netzbetrieb"
        result.batteryChargingState == "DISCHARGING" -> "Akkubetrieb"
        else -> "Lokal gelesen"
      },
    monoFooter = subtitle,
    accentColor = accentColor,
    testTag = "hub_battery_card",
    onClick = onClick,
    modifier = modifier,
  )
}

@Composable
private fun MemoryTelemetryHubCard(
  result: InspectionResult?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sys = result?.systemHealthSnapshot
  val fraction = sys?.memoryUsedFraction
  val pct = fraction?.let { (it * 100).roundToInt() }
  val accentColor =
    when {
      fraction == null -> MaterialTheme.colorScheme.outline
      fraction >= 0.85f || sys.lowMemoryReported -> MaterialTheme.colorScheme.error
      else -> MaterialTheme.colorScheme.primary
    }

  TelemetryGaugeCard(
    title = "Arbeitsspeicher",
    icon = Icons.Filled.Memory,
    fraction = fraction ?: 0f,
    centerText = if (pct != null) "$pct%" else "—",
    primaryDetail =
      if (sys?.memoryTotalBytes != null) {
        "${sys.formattedMemoryUsed()} / ${sys.formattedMemoryTotal()}"
      } else if (result == null) {
        "Manuell starten"
      } else {
        "Nicht verfügbar"
      },
    monoFooter =
      if (sys?.memoryAvailableBytes != null) "Frei: ${sys.formattedMemoryFree()}"
      else "RAM · lokal",
    accentColor = accentColor,
    testTag = "hub_memory_card",
    onClick = onClick,
    modifier = modifier,
  )
}

@Composable
private fun TelemetryGaugeCard(
  title: String,
  icon: ImageVector,
  fraction: Float,
  centerText: String,
  primaryDetail: String,
  monoFooter: String,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val animatedFraction by
    animateFloatAsState(
      targetValue = fraction.coerceIn(0f, 1f),
      animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
      label = "hub_gauge_$testTag",
    )
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

  Card(
    modifier =
      modifier
        .testTag(testTag)
        .clip(MaterialTheme.shapes.large)
        .minimumInteractiveComponentSize()
        .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(18.dp),
        )
      }

      Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val strokeWidth = 7.dp.toPx()
          drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )
          if (animatedFraction > 0.005f) {
            drawArc(
              color = accentColor,
              startAngle = -90f,
              sweepAngle = 360f * animatedFraction,
              useCenter = false,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
          }
        }
        Text(
          text = centerText,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Text(
        text = primaryDetail,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = monoFooter,
        style = MonoTokenTextStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun StorageTelemetryHubCard(
  result: InspectionResult?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sys = result?.systemHealthSnapshot
  val fraction = sys?.storageUsedFraction ?: 0f
  val animatedFraction by
    animateFloatAsState(
      targetValue = fraction.coerceIn(0f, 1f),
      animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
      label = "storage_bar_progress",
    )
  val pct = sys?.storageUsedFraction?.let { (it * 100).roundToInt() }
  val fillColor =
    when {
      sys?.storageUsedFraction == null -> MaterialTheme.colorScheme.outline
      sys.storageUsedFraction!! >= 0.88f -> MaterialTheme.colorScheme.error
      else -> MaterialTheme.colorScheme.tertiary
    }
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

  Card(
    modifier =
      modifier
        .testTag("hub_storage_card")
        .clip(MaterialTheme.shapes.large)
        .minimumInteractiveComponentSize()
        .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
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
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
            Icon(
              imageVector = Icons.Filled.Storage,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(6.dp).size(18.dp),
            )
          }
          Column {
            Text(
              text = "Interner Speicher",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                if (sys?.storageTotalBytes != null) {
                  "${sys.formattedStorageUsed()} von ${sys.formattedStorageTotal()} belegt"
                } else if (result == null) {
                  "Wird erst nach manuellem Prüfstart gelesen"
                } else {
                  "Speicherpartition nicht verfügbar"
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Text(
          text = if (pct != null) "$pct%" else "—",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
        if (animatedFraction > 0.005f) {
          drawRoundRect(
            color = fillColor,
            topLeft = Offset.Zero,
            size = Size(width = size.width * animatedFraction, height = size.height),
            cornerRadius = radius,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text =
            if (sys?.storageAvailableBytes != null) "Frei: ${sys.formattedStorageFree()}"
            else "Interne App-Datenpartition",
          style = MonoTokenTextStyle,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = if (result == null) "LOKAL" else "StatFs",
          style = MonoTokenTextStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
