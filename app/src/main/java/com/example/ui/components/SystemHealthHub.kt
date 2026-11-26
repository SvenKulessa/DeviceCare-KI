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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
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

enum class HealthIndicatorLevel(val rank: Int) {
  UNVERIFIED(0),
  GOOD(1),
  CAUTION(2),
  CRITICAL(3),
}

data class HealthMetricIndicator(
  val level: HealthIndicatorLevel,
  val label: String,
  val icon: ImageVector,
  val contentDescription: String,
)

internal fun batteryHealthIndicator(
  percent: Int?,
  chargingState: String?,
): HealthMetricIndicator {
  if (percent == null) {
    return HealthMetricIndicator(
      level = HealthIndicatorLevel.UNVERIFIED,
      label = "UNBEKANNT",
      icon = Icons.Filled.Shield,
      contentDescription = "Akkustatus unbekannt vor manuellem Scan",
    )
  }
  val charging = chargingState == "CHARGING" || chargingState == "FULL"
  return when {
    percent < 20 && !charging ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.CRITICAL,
        label = "KRITISCH",
        icon = Icons.Filled.BatteryAlert,
        contentDescription = "Akkustatus kritisch: unter 20 Prozent",
      )
    charging ->
      HealthMetricIndicator(
        level = if (percent < 20) HealthIndicatorLevel.CAUTION else HealthIndicatorLevel.GOOD,
        label = if (chargingState == "FULL") "VOLL" else "LÄDT",
        icon = Icons.Filled.BatteryChargingFull,
        contentDescription = "Akkustatus: wird geladen",
      )
    percent < 50 ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.CAUTION,
        label = "NIEDRIG",
        icon = Icons.Filled.Battery3Bar,
        contentDescription = "Akkustatus niedrig: unter 50 Prozent",
      )
    else ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.GOOD,
        label = "GUT",
        icon = Icons.Filled.BatteryFull,
        contentDescription = "Akkustatus gut",
      )
  }
}

internal fun memoryHealthIndicator(
  fraction: Float?,
  lowMemoryReported: Boolean,
): HealthMetricIndicator =
  when {
    fraction == null ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.UNVERIFIED,
        label = "UNBEKANNT",
        icon = Icons.Filled.Shield,
        contentDescription = "Arbeitsspeicherstatus unbekannt vor manuellem Scan",
      )
    lowMemoryReported || fraction >= 0.85f ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.CRITICAL,
        label = "HOCH",
        icon = Icons.Filled.WarningAmber,
        contentDescription = "Arbeitsspeicherauslastung kritisch hoch",
      )
    fraction >= 0.72f ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.CAUTION,
        label = "ERHÖHT",
        icon = Icons.Filled.GppMaybe,
        contentDescription = "Arbeitsspeicherauslastung erhöht",
      )
    else ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.GOOD,
        label = "GUT",
        icon = Icons.Filled.VerifiedUser,
        contentDescription = "Arbeitsspeicherauslastung im normalen Bereich",
      )
  }

internal fun storageHealthIndicator(fraction: Float?): HealthMetricIndicator =
  when {
    fraction == null ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.UNVERIFIED,
        label = "UNBEKANNT",
        icon = Icons.Filled.Shield,
        contentDescription = "Speicherstatus unbekannt vor manuellem Scan",
      )
    fraction >= 0.88f ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.CRITICAL,
        label = "FAST VOLL",
        icon = Icons.Filled.WarningAmber,
        contentDescription = "Interner Speicher kritisch voll",
      )
    fraction >= 0.80f ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.CAUTION,
        label = "BELEGT",
        icon = Icons.Filled.GppMaybe,
        contentDescription = "Interner Speicher stark belegt",
      )
    else ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.GOOD,
        label = "GUT",
        icon = Icons.Filled.VerifiedUser,
        contentDescription = "Interner Speicher mit ausreichend Reserven",
      )
  }

internal fun overallHealthIndicator(
  hasResult: Boolean,
  isStale: Boolean,
  battery: HealthMetricIndicator,
  memory: HealthMetricIndicator,
  storage: HealthMetricIndicator,
): HealthMetricIndicator {
  if (!hasResult) {
    return HealthMetricIndicator(
      level = HealthIndicatorLevel.UNVERIFIED,
      label = "BEREIT · LOKAL",
      icon = Icons.Filled.Shield,
      contentDescription = "System-Übersicht wartet auf manuellen Scan",
    )
  }
  val worst = listOf(battery, memory, storage).maxBy { it.level.rank }
  return when {
    isStale ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.CAUTION,
        label = "VERALTET",
        icon = Icons.Filled.GppMaybe,
        contentDescription = "Messwerte veraltet, erneuter Scan empfohlen",
      )
    worst.level == HealthIndicatorLevel.CRITICAL ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.CRITICAL,
        label = "ACHTUNG",
        icon = Icons.Filled.WarningAmber,
        contentDescription = "Mindestens ein Systemwert ist kritisch",
      )
    worst.level == HealthIndicatorLevel.CAUTION ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.CAUTION,
        label = "PRÜFEN",
        icon = Icons.Filled.GppMaybe,
        contentDescription = "Mindestens ein Systemwert benötigt Aufmerksamkeit",
      )
    worst.level == HealthIndicatorLevel.GOOD ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.GOOD,
        label = "LOKAL · LIVE",
        icon = Icons.Filled.VerifiedUser,
        contentDescription = "Alle erfassten Systemwerte unauffällig",
      )
    else ->
      HealthMetricIndicator(
        level = HealthIndicatorLevel.UNVERIFIED,
        label = "LOKAL · LIVE",
        icon = Icons.Filled.Shield,
        contentDescription = "Lokale Momentaufnahme aktiv",
      )
  }
}

@Composable
private fun HealthIndicatorLevel.colors(): Triple<Color, Color, Color> =
  when (this) {
    HealthIndicatorLevel.GOOD ->
      Triple(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
      )
    HealthIndicatorLevel.CAUTION ->
      Triple(
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
      )
    HealthIndicatorLevel.CRITICAL ->
      Triple(
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
      )
    HealthIndicatorLevel.UNVERIFIED ->
      Triple(
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.onSurfaceVariant,
      )
  }

/**
 * Central System Health Hub widget displaying Storage, Battery, and Memory status
 * with at-a-glance color-coded health indicators plus quick navigation cards.
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
  onOpenRethinkGuide: () -> Unit = {},
) {
  val result = uiState.inspectionResult
  val isStale = uiState.isInspectionStale
  val sys = result?.systemHealthSnapshot

  val batteryIndicator =
    batteryHealthIndicator(
      percent = result?.batteryPercent,
      chargingState = result?.batteryChargingState,
    )
  val memoryIndicator =
    memoryHealthIndicator(
      fraction = sys?.memoryUsedFraction,
      lowMemoryReported = sys?.lowMemoryReported == true,
    )
  val storageIndicator = storageHealthIndicator(fraction = sys?.storageUsedFraction)
  val overallIndicator =
    overallHealthIndicator(
      hasResult = result != null,
      isStale = isStale,
      battery = batteryIndicator,
      memory = memoryIndicator,
      storage = storageIndicator,
    )

  val (overallAccent, overallBg, overallFg) = overallIndicator.level.colors()
  val headerBg =
    when {
      result == null -> MaterialTheme.colorScheme.tertiaryContainer
      isStale -> MaterialTheme.colorScheme.errorContainer
      else -> overallBg
    }
  val headerFg =
    when {
      result == null -> MaterialTheme.colorScheme.onTertiaryContainer
      isStale -> MaterialTheme.colorScheme.onErrorContainer
      else -> overallFg
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
      Surface(
        shape = MaterialTheme.shapes.small,
        color = headerBg,
        modifier = Modifier.testTag("hub_health_overview_badge"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = overallIndicator.icon,
            contentDescription = overallIndicator.contentDescription,
            tint = if (result == null || isStale) headerFg else overallAccent,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = overallIndicator.label,
            style = MonoTokenTextStyle,
            color = headerFg,
          )
        }
      }
    }

    if (isWide) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        BatteryTelemetryHubCard(
          result = result,
          indicator = batteryIndicator,
          onClick = onOpenBatteryMonitor,
          modifier = Modifier.weight(1f),
        )
        MemoryTelemetryHubCard(
          result = result,
          indicator = memoryIndicator,
          onClick = onStartInspection,
          modifier = Modifier.weight(1f),
        )
        StorageTelemetryHubCard(
          result = result,
          indicator = storageIndicator,
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
          indicator = batteryIndicator,
          onClick = onOpenBatteryMonitor,
          modifier = Modifier.weight(1f),
        )
        MemoryTelemetryHubCard(
          result = result,
          indicator = memoryIndicator,
          onClick = onStartInspection,
          modifier = Modifier.weight(1f),
        )
      }
      StorageTelemetryHubCard(
        result = result,
        indicator = storageIndicator,
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
      onOpenRethinkGuide = onOpenRethinkGuide,
    )
  }
}

@Composable
private fun HealthIndicatorStatusPill(
  indicator: HealthMetricIndicator,
  testTag: String,
) {
  val (accentColor, bgColor, fgColor) = indicator.level.colors()
  Surface(
    shape = MaterialTheme.shapes.small,
    color = bgColor,
    modifier = Modifier.testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = indicator.icon,
        contentDescription = indicator.contentDescription,
        tint = accentColor,
        modifier = Modifier.size(13.dp),
      )
      Text(
        text = indicator.label,
        style = MonoTokenTextStyle,
        color = fgColor,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun BatteryTelemetryHubCard(
  result: InspectionResult?,
  indicator: HealthMetricIndicator,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val pct = result?.batteryPercent
  val progress = (pct ?: 0) / 100f
  val (accentColor, _, _) = indicator.level.colors()
  val subtitle =
    when {
      result == null -> "Nach Scan"
      result.batteryChargingState == "CHARGING" -> "Lädt · ${result.batteryTemperatureDisplay ?: "—"}"
      result.batteryChargingState == "FULL" -> "Voll geladen"
      result.batteryChargingState != null -> result.batteryTemperatureDisplay ?: result.batteryChargingState
      else -> "Nicht verfügbar"
    }

  TelemetryGaugeCard(
    title = "Akku",
    icon = Icons.Filled.BatteryChargingFull,
    indicator = indicator,
    indicatorTag = "hub_battery_status_icon",
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
  indicator: HealthMetricIndicator,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sys = result?.systemHealthSnapshot
  val fraction = sys?.memoryUsedFraction
  val pct = fraction?.let { (it * 100).roundToInt() }
  val (accentColor, _, _) = indicator.level.colors()

  TelemetryGaugeCard(
    title = "Arbeitsspeicher",
    icon = Icons.Filled.Memory,
    indicator = indicator,
    indicatorTag = "hub_memory_status_icon",
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
  indicator: HealthMetricIndicator,
  indicatorTag: String,
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
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (indicator.level == HealthIndicatorLevel.CRITICAL) accentColor.copy(alpha = 0.55f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
      ),
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
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(4.dp))
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

      HealthIndicatorStatusPill(indicator = indicator, testTag = indicatorTag)

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
  indicator: HealthMetricIndicator,
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
  val (fillColor, iconContainerColor, iconTint) = indicator.level.colors()
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
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (indicator.level == HealthIndicatorLevel.CRITICAL) fillColor.copy(alpha = 0.55f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
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
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(shape = CircleShape, color = iconContainerColor) {
            Icon(
              imageVector = Icons.Filled.Storage,
              contentDescription = null,
              tint = iconTint,
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
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = if (pct != null) "$pct%" else "—",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          HealthIndicatorStatusPill(indicator = indicator, testTag = "hub_storage_status_icon")
        }
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
          text = "LOKAL",
          style = MonoTokenTextStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
