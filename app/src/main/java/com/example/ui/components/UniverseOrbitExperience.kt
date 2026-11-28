package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.devicecare.PrivacyPostureTier
import com.example.devicecare.PrivacySummaryAggregator
import com.example.ui.DeviceCareUiState
import com.example.ui.theme.MonoTokenTextStyle
import com.example.ui.theme.ThemeMode
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Exakt 7 Universumsfarben rund um den Planeten.
 * Jede Farbe steht eindeutig für eine Diagnose- oder Werkzeug-Kategorie.
 */
enum class UniverseDiagnosticCategory(
  val id: String,
  val title: String,
  val shortLabel: String,
  val summary: String,
  val actionLabel: String,
  val accentColor: Color,
  val icon: ImageVector,
) {
  SYSTEM_HEALTH(
    id = "system_health",
    title = "System & Speicher",
    shortLabel = "System",
    summary = "Prüft Arbeitsspeicher, Gerätespeicher und Sensor-Inventar manuell vor Ort.",
    actionLabel = "Systemprüfung starten",
    accentColor = Color(0xFF38BDF8),
    icon = Icons.Filled.Memory,
  ),
  BATTERY(
    id = "battery",
    title = "Akku & Energie",
    shortLabel = "Akku",
    summary = "Ladestand, Temperaturverlauf, Restlaufzeit und 20 %-Schutzwarnung.",
    actionLabel = "Akku-Monitor öffnen",
    accentColor = Color(0xFFF5C24B),
    icon = Icons.Filled.BatteryChargingFull,
  ),
  APPS_SECURITY(
    id = "apps_security",
    title = "Apps & Schutz",
    shortLabel = "Apps",
    summary = "Prüft installierte Apps & System-Apps auf Berechtigungen und Änderungen.",
    actionLabel = "App-Analyse öffnen",
    accentColor = Color(0xFFFF3B4D),
    icon = Icons.Filled.Apps,
  ),
  NETWORK_RETHINK(
    id = "network_rethink",
    title = "Netz & Firewall",
    shortLabel = "Netzwerk",
    summary = "Klare Empfehlungen für Firewall-Regeln, DNS-Filter und Netzwerksicherheit.",
    actionLabel = "Netzwerk-Empfehlungen",
    accentColor = Color(0xFF00E6FF),
    icon = Icons.Filled.Dns,
  ),
  PRIVACY_COCKPIT(
    id = "privacy_cockpit",
    title = "Datenschutz-Cockpit",
    shortLabel = "Datenschutz",
    summary = "Gebündelte Einschätzung zu Sensorrechten, Patch-Stand und nächsten Schritten.",
    actionLabel = "Cockpit öffnen",
    accentColor = Color(0xFF2CEAA3),
    icon = Icons.Filled.Shield,
  ),
  AI_ADVISOR(
    id = "ai_advisor",
    title = "KI-Berater (Opt-in)",
    shortLabel = "KI-Berater",
    summary = "Verständliche Erklärungen zu Akku- und Berechtigungsfragen auf Abruf.",
    actionLabel = "KI-Berater öffnen",
    accentColor = Color(0xFF8A3DFF),
    icon = Icons.Filled.AutoAwesome,
  ),
  CLOUD_NOTES(
    id = "cloud_notes",
    title = "Cloud-Notizen (Opt-in)",
    shortLabel = "Cloud",
    summary = "Freiwillige Sicherung deiner Prüfnotizen in deinem persönlichen Konto.",
    actionLabel = "Notizen öffnen",
    accentColor = Color(0xFFC8D6EA),
    icon = Icons.Filled.CloudQueue,
  ),
}

private data class CategoryStatusReadout(
  val badge: String,
  val detail: String,
  val fillFraction: Float,
)

private fun resolveCategoryReadout(
  category: UniverseDiagnosticCategory,
  uiState: DeviceCareUiState,
): CategoryStatusReadout {
  val inspection = uiState.inspectionResult
  val sys = inspection?.systemHealthSnapshot
  val apps = uiState.installedAppsScanResult
  val battery = uiState.batterySnapshot
  val summaryReport =
    PrivacySummaryAggregator.aggregate(
      appsScanResult = apps,
      securitySnapshot = uiState.systemSecuritySnapshot,
      inspectionResult = inspection,
    )

  return when (category) {
    UniverseDiagnosticCategory.SYSTEM_HEALTH -> {
      val memPct = sys?.memoryUsedFraction?.let { (it * 100f).roundToInt() }
      val stoPct = sys?.storageUsedFraction?.let { (it * 100f).roundToInt() }
      if (inspection == null || sys == null) {
        CategoryStatusReadout(
          badge = "BEREIT",
          detail = "Tippe auf Start, um Speicher, RAM und Sensoren lokal einzulesen.",
          fillFraction = 0.14f,
        )
      } else {
        CategoryStatusReadout(
          badge = if (uiState.isInspectionStale) "VERALTET" else "LIVE",
          detail =
            "RAM ${memPct?.let { "$it%" } ?: "—"} · Speicher ${stoPct?.let { "$it%" } ?: "—"} belegt",
          fillFraction = ((sys.memoryUsedFraction ?: 0.5f) + (sys.storageUsedFraction ?: 0.5f)) / 2f,
        )
      }
    }
    UniverseDiagnosticCategory.BATTERY -> {
      val pct = battery?.percent ?: inspection?.batteryPercent
      if (pct == null) {
        CategoryStatusReadout(
          badge = if (uiState.preferences.lowBatteryAlertsEnabled) "SCHUTZ <20%" else "BEREIT",
          detail = "Live-Akkudaten, Temperaturverlauf und 20 %-Schutzschwelle prüfen.",
          fillFraction = 0.25f,
        )
      } else {
        CategoryStatusReadout(
          badge = "$pct%",
          detail =
            "${battery?.chargingStatus?.label ?: inspection?.batteryChargingState ?: "Aktiv"} · ${
              if (uiState.preferences.lowBatteryAlertsEnabled) "Warnung <20 % ein" else "Warnung aus"
            }",
          fillFraction = (pct / 100f).coerceIn(0.08f, 1f),
        )
      }
    }
    UniverseDiagnosticCategory.APPS_SECURITY -> {
      if (apps == null || apps.errorMessage != null) {
        CategoryStatusReadout(
          badge = "SCAN BEREIT",
          detail = "Alle regulär sichtbaren Nutzer- & System-Apps lokal analysieren.",
          fillFraction = 0.18f,
        )
      } else {
        val ratio =
          if (apps.visiblePackageCount > 0) {
            apps.appsWithHighSensitivityCount.toFloat() / apps.visiblePackageCount.toFloat()
          } else 0.2f
        CategoryStatusReadout(
          badge = "${apps.visiblePackageCount} APPS",
          detail =
            "${apps.appsWithHighSensitivityCount} mit hochsensiblen Rechten · Firewall-Tipps bereit",
          fillFraction = ratio.coerceIn(0.15f, 1f),
        )
      }
    }
    UniverseDiagnosticCategory.NETWORK_RETHINK -> {
      val sensitiveCount = apps?.appsWithHighSensitivityCount ?: 0
      CategoryStatusReadout(
        badge = if (sensitiveCount > 0) "3 PROFILE" else "EMPFEHLUNG",
        detail =
          if (sensitiveCount > 0) "$sensitiveCount sensible App(s) · abgestimmte Firewall- & DNS-Regeln"
          else "Alltags-, Datenschutz- und Verdachtsschutz-Profile für dein Netzwerk.",
        fillFraction = if (sensitiveCount > 0) 0.72f else 0.48f,
      )
    }
    UniverseDiagnosticCategory.PRIVACY_COCKPIT -> {
      val tierLabel =
        when (summaryReport.postureTier) {
          PrivacyPostureTier.UNASSESSED -> "BEREIT"
          PrivacyPostureTier.PARTIAL -> "TEILSTATUS"
          PrivacyPostureTier.REVIEW_RECOMMENDED -> "PRÜFEN"
          PrivacyPostureTier.CHECKED_BASELINE -> "OK"
        }
      CategoryStatusReadout(
        badge = tierLabel,
        detail =
          "${summaryReport.checksCompletedCount} von ${summaryReport.checksTotalCount} lokalen Prüfungen abgeschlossen.",
        fillFraction = summaryReport.coverageFraction.coerceAtLeast(0.12f),
      )
    }
    UniverseDiagnosticCategory.AI_ADVISOR -> {
      CategoryStatusReadout(
        badge = "OPT-IN",
        detail = "Stelle Fragen zu Akku, Datenschutz oder App-Rechten — nur auf deinen Wunsch.",
        fillFraction = 0.55f,
      )
    }
    UniverseDiagnosticCategory.CLOUD_NOTES -> {
      CategoryStatusReadout(
        badge = "KONTO",
        detail = "Sichere eigene Prüfnotizen und Wartungslisten optional in deinem Konto.",
        fillFraction = 0.40f,
      )
    }
  }
}

/**
 * Fest integrierte Haupt-Universumsgrafik im Hintergrund der gesamten App
 * inklusive elektronischem Lichtimpuls passend zur aktiven Diagnose-Kategorie.
 */
@Composable
fun UniverseCosmicBackground(
  activeCategory: UniverseDiagnosticCategory,
  planetPulseNonce: Int,
  themeMode: ThemeMode,
  modifier: Modifier = Modifier,
) {
  val isLightTheme =
    when (themeMode) {
      ThemeMode.LIGHT -> true
      ThemeMode.DARK -> false
      ThemeMode.SYSTEM -> !isSystemInDarkTheme()
    }

  val accentColor by
    animateColorAsState(
      targetValue = activeCategory.accentColor,
      animationSpec = tween(durationMillis = 420),
      label = "universeBackgroundAccent",
    )

  val pulseProgress = remember { Animatable(0f) }
  LaunchedEffect(planetPulseNonce) {
    if (planetPulseNonce > 0) {
      pulseProgress.snapTo(1f)
      pulseProgress.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 880, easing = FastOutSlowInEasing),
      )
    }
  }

  Box(modifier = modifier.fillMaxSize().testTag("universe_cosmic_background")) {
    Image(
      painter = painterResource(id = R.drawable.bg_analytics_universe_orbit),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )

    // Elektronischer Hintergrund-Impuls bei Nutzeraktionen
    val pulseStrength = pulseProgress.value
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width * 0.5f, size.height * 0.28f)
      val maxR = hypot(size.width, size.height) * 0.65f
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                accentColor.copy(alpha = 0.12f + pulseStrength * 0.22f),
                accentColor.copy(alpha = 0.04f + pulseStrength * 0.08f),
                Color.Transparent,
              ),
            center = center,
            radius = maxR * (0.65f + (1f - pulseStrength) * 0.35f),
          ),
        center = center,
        radius = maxR,
      )
    }

    // Zugänglichkeits-Scrim für klaren Kontrast auf allen Displays
    val scrimColors =
      if (isLightTheme) {
        listOf(
          Color(0xFFF3F7FD).copy(alpha = 0.78f),
          Color(0xFFEAF1FA).copy(alpha = 0.85f),
          Color(0xFFF5F8FE).copy(alpha = 0.90f),
        )
      } else {
        listOf(
          Color(0xFF060916).copy(alpha = 0.66f),
          Color(0xFF070C1F).copy(alpha = 0.74f),
          Color(0xFF050814).copy(alpha = 0.86f),
        )
      }
    Box(
      modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = scrimColors))
    )
  }
}

/**
 * Interaktiver Universums-Planet mit 7 elektronischen Farb-Ringsegmenten (neuronalen Links),
 * der auf Benutzeraktionen sichtbar aufleuchtet und direkt zur gewählten Kategorie führt.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveUniverseOrbitCard(
  uiState: DeviceCareUiState,
  selectedCategory: UniverseDiagnosticCategory,
  planetPulseNonce: Int,
  onSelectCategory: (UniverseDiagnosticCategory) -> Unit,
  onPlanetTap: () -> Unit,
  onTriggerCategoryAction: (UniverseDiagnosticCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  val categories = UniverseDiagnosticCategory.entries
  val selectedIndex = selectedCategory.ordinal

  val pulseAnim = remember { Animatable(0f) }
  LaunchedEffect(planetPulseNonce) {
    if (planetPulseNonce > 0) {
      pulseAnim.snapTo(1f)
      pulseAnim.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing),
      )
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "universeOrbitPulse")
  val travelPhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2600, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "neuralLinkTravelPhase",
    )
  val ambientGlow by
    infiniteTransition.animateFloat(
      initialValue = 0.45f,
      targetValue = 0.85f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2100, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "planetAmbientGlow",
    )

  val activeColor by
    animateColorAsState(
      targetValue = selectedCategory.accentColor,
      animationSpec = tween(durationMillis = 360),
      label = "selectedCategoryAccent",
    )

  val readout = remember(selectedCategory, uiState) { resolveCategoryReadout(selectedCategory, uiState) }

  Card(
    modifier = modifier.fillMaxWidth().testTag("interactive_universe_orbit_card"),
    shape = MaterialTheme.shapes.extraLarge,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f)
      ),
    border = BorderStroke(1.2.dp, activeColor.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "Diagnose-Universum · 7 Schutzbereiche",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
          )
          Text(
            text = "Tippe auf den Planeten oder ein Farbring-Segment",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color = activeColor.copy(alpha = 0.18f),
          border = BorderStroke(1.dp, activeColor.copy(alpha = 0.55f)),
        ) {
          Text(
            text = readout.badge,
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      // Interaktives Canvas: Zentraler leuchtender Planet + 7 Farb-Ringsegmente + Neuronale Links
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(238.dp)
            .clip(MaterialTheme.shapes.large)
            .background(Color(0xFF060B1D))
            .testTag("universe_planet_core")
            .semantics {
              contentDescription =
                "Interaktiver Schutzplanet. Aktiver Bereich: ${selectedCategory.title}. Doppeltippen für Impuls und Aktion."
            }
            .pointerInput(categories, selectedCategory) {
              detectTapGestures { tapOffset ->
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f
                val dx = tapOffset.x - cx
                val dy = tapOffset.y - cy
                val minSide = minOf(size.width, size.height).toFloat()
                val planetR = minSide * 0.21f
                if (hypot(dx, dy) <= planetR * 1.15f) {
                  onPlanetTap()
                } else {
                  var deg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
                  if (deg < -90f) deg += 360f
                  val normalized = ((deg + 90f) + 360f) % 360f
                  val segmentSweep = 360f / categories.size
                  val tappedIndex = (normalized / segmentSweep).toInt().coerceIn(0, categories.lastIndex)
                  onSelectCategory(categories[tappedIndex])
                }
              }
            }
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width * 0.5f
          val cy = size.height * 0.5f
          val center = Offset(cx, cy)
          val minDim = minOf(size.width, size.height)
          val planetRadius = minDim * 0.205f
          val orbitRadiusX = minDim * 0.42f
          val orbitRadiusY = minDim * 0.36f
          val pulse = pulseAnim.value

          // Hintergrund-Sternengitter
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    activeColor.copy(alpha = 0.22f + pulse * 0.34f),
                    Color(0xFF0A1432).copy(alpha = 0.55f),
                    Color.Transparent,
                  ),
                center = center,
                radius = minDim * 0.49f,
              ),
            center = center,
            radius = minDim * 0.49f,
          )

          // Elektronische Schockwelle beim Antippen
          if (pulse > 0.01f) {
            val waveR = planetRadius + (orbitRadiusX - planetRadius * 0.4f) * (1f - pulse)
            drawCircle(
              color = activeColor.copy(alpha = pulse * 0.72f),
              radius = waveR,
              center = center,
              style = Stroke(width = (3.5.dp.toPx()) * pulse + 1.dp.toPx()),
            )
          }

          val totalCount = categories.size
          val fullSweep = 360f / totalCount
          val arcGap = 8f

          // 7 Farbige Ring-Segmente + Neuronale Links
          categories.forEachIndexed { idx, cat ->
            val isSelected = idx == selectedIndex
            val startDeg = -90f + idx * fullSweep + arcGap * 0.5f
            val sweepDeg = fullSweep - arcGap
            val midRad = ((startDeg + sweepDeg * 0.5f) * PI / 180.0).toFloat()

            val arcCenterTopLeft = Offset(cx - orbitRadiusX, cy - orbitRadiusY)
            val arcSize = Size(orbitRadiusX * 2f, orbitRadiusY * 2f)

            // Äußerer Neon-Halo für aktives Ring-Segment
            if (isSelected) {
              drawArc(
                color = cat.accentColor.copy(alpha = 0.34f + pulse * 0.40f),
                startAngle = startDeg - 1.5f,
                sweepAngle = sweepDeg + 3f,
                useCenter = false,
                topLeft = arcCenterTopLeft,
                size = arcSize,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
              )
            }

            // Haupt-Farbsegment
            drawArc(
              color = cat.accentColor.copy(alpha = if (isSelected) 1f else 0.52f),
              startAngle = startDeg,
              sweepAngle = sweepDeg,
              useCenter = false,
              topLeft = arcCenterTopLeft,
              size = arcSize,
              style =
                Stroke(
                  width = if (isSelected) 8.5.dp.toPx() else 5.dp.toPx(),
                  cap = StrokeCap.Round,
                ),
            )

            // Ankerpunkt auf dem Ringsegment
            val anchor =
              Offset(
                x = cx + orbitRadiusX * cos(midRad),
                y = cy + orbitRadiusY * sin(midRad),
              )

            // Dynamische neuronale Verknüpfung zwischen Planet & Segment
            val planetEdge =
              Offset(
                x = cx + planetRadius * 0.85f * cos(midRad),
                y = cy + planetRadius * 0.85f * sin(midRad),
              )
            val perp = Offset(-sin(midRad), cos(midRad))
            val ctrl =
              Offset(
                x = (planetEdge.x + anchor.x) * 0.5f + perp.x * (if (isSelected) 22.dp.toPx() else 8.dp.toPx()),
                y = (planetEdge.y + anchor.y) * 0.5f + perp.y * (if (isSelected) 22.dp.toPx() else 8.dp.toPx()),
              )

            val neuralPath =
              Path().apply {
                moveTo(planetEdge.x, planetEdge.y)
                quadraticTo(ctrl.x, ctrl.y, anchor.x, anchor.y)
              }

            drawPath(
              path = neuralPath,
              color =
                cat.accentColor.copy(
                  alpha = if (isSelected) (0.72f + pulse * 0.28f) else 0.16f
                ),
              style =
                Stroke(
                  width = if (isSelected) 2.8.dp.toPx() else 1.dp.toPx(),
                  cap = StrokeCap.Round,
                ),
            )

            // Wandernder Energie-Impuls auf dem aktiven neuronalen Link
            if (isSelected) {
              val t = travelPhase
              val omt = 1f - t
              val pulseDot =
                Offset(
                  x = omt * omt * planetEdge.x + 2f * omt * t * ctrl.x + t * t * anchor.x,
                  y = omt * omt * planetEdge.y + 2f * omt * t * ctrl.y + t * t * anchor.y,
                )
              drawCircle(
                color = Color.White,
                radius = 4.2.dp.toPx() + pulse * 2.dp.toPx(),
                center = pulseDot,
              )
              drawCircle(
                color = cat.accentColor.copy(alpha = 0.65f),
                radius = 8.dp.toPx() + pulse * 3.dp.toPx(),
                center = pulseDot,
              )
            }

            // Farbknoten direkt auf dem Segment
            drawCircle(
              color = cat.accentColor,
              radius = if (isSelected) 6.dp.toPx() else 3.6.dp.toPx(),
              center = anchor,
            )
            if (isSelected) {
              drawCircle(
                color = Color.White,
                radius = 2.6.dp.toPx(),
                center = anchor,
              )
            }
          }

          // Zentraler Planet (elektronisch beleuchtet bei Interaktion)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    activeColor.copy(alpha = 0.45f + pulse * 0.45f),
                    activeColor.copy(alpha = 0.16f * ambientGlow),
                    Color.Transparent,
                  ),
                center = center,
                radius = planetRadius * 1.48f,
              ),
            center = center,
            radius = planetRadius * 1.48f,
          )

          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color.White.copy(alpha = 0.25f + pulse * 0.55f),
                    activeColor.copy(alpha = 0.72f + pulse * 0.28f),
                    Color(0xFF102454),
                    Color(0xFF070F28),
                  ),
                center = Offset(cx - planetRadius * 0.28f, cy - planetRadius * 0.28f),
                radius = planetRadius * 1.25f,
              ),
            center = center,
            radius = planetRadius,
          )

          // Elektronische Längen- & Breitengrade auf der Planetenoberfläche
          val meridianAlpha = 0.35f + pulse * 0.55f
          drawArc(
            color = Color.White.copy(alpha = meridianAlpha),
            startAngle = -65f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(cx - planetRadius * 0.55f, cy - planetRadius * 0.92f),
            size = Size(planetRadius * 1.1f, planetRadius * 1.84f),
            style = Stroke(width = 1.4.dp.toPx()),
          )
          drawArc(
            color = activeColor.copy(alpha = 0.55f + pulse * 0.45f),
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(cx - planetRadius * 0.92f, cy - planetRadius * 0.36f),
            size = Size(planetRadius * 1.84f, planetRadius * 0.72f),
            style = Stroke(width = 1.6.dp.toPx()),
          )
          drawCircle(
            color = Color.White.copy(alpha = 0.55f + pulse * 0.45f),
            radius = planetRadius,
            center = center,
            style = Stroke(width = (1.6.dp.toPx()) + pulse * 1.6.dp.toPx()),
          )
        }
      }

      // 7 barrierefreie Auswahl-Chips für exakte Farben & Kategorien
      FlowRow(
        modifier = Modifier.fillMaxWidth().testTag("universe_orbit_segments_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        categories.forEach { cat ->
          val selected = cat == selectedCategory
          Surface(
            modifier =
              Modifier.clip(CircleShape)
                .minimumInteractiveComponentSize()
                .clickable(role = Role.RadioButton) { onSelectCategory(cat) }
                .testTag("universe_orbit_segment_${cat.id}")
                .semantics {
                  stateDescription = if (selected) "Ausgewählt" else "Nicht ausgewählt"
                },
            shape = CircleShape,
            color =
              if (selected) cat.accentColor.copy(alpha = 0.22f)
              else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            border =
              BorderStroke(
                width = if (selected) 1.5.dp else 1.dp,
                color =
                  if (selected) cat.accentColor
                  else cat.accentColor.copy(alpha = 0.35f),
              ),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(10.dp).clip(CircleShape).background(cat.accentColor)
              )
              Text(
                text = cat.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }

      // Aktive Kategorie-Zusammenfassung + Startknopf
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, activeColor.copy(alpha = 0.35f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(shape = CircleShape, color = activeColor.copy(alpha = 0.22f)) {
              Icon(
                imageVector = selectedCategory.icon,
                contentDescription = null,
                tint = activeColor,
                modifier = Modifier.padding(8.dp).size(18.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = selectedCategory.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = readout.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Button(
            onClick = { onTriggerCategoryAction(selectedCategory) },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = activeColor,
                contentColor = Color(0xFF060B1D),
              ),
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("universe_orbit_action_button"),
          ) {
            Text(text = selectedCategory.actionLabel, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }
  }
}

/**
 * Interaktive Diagnose-Matrix & Echtzeit-Verteilungsdiagramm für alle 7 Bereiche.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveDiagnosticMatrixCard(
  uiState: DeviceCareUiState,
  selectedCategory: UniverseDiagnosticCategory,
  onSelectCategory: (UniverseDiagnosticCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  val categories = UniverseDiagnosticCategory.entries
  val inspection = uiState.inspectionResult
  val sys = inspection?.systemHealthSnapshot
  val hasLiveMetrics = inspection != null && sys != null

  Card(
    modifier = modifier.fillMaxWidth().testTag("diagnostic_matrix_card"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.90f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "Interaktive System-Matrix & Messverteilung",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
          )
          Text(
            text =
              if (hasLiveMetrics) "Aktuelle Auslastung und Status je Schutzbereich"
              else "Wartet auf manuellen Start — Werte erscheinen direkt nach der Prüfung",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Interaktives Verteilungsdiagramm (Speicher / RAM / Akku / Berechtigungen)
      val ramFraction by
        animateFloatAsState(
          targetValue = (sys?.memoryUsedFraction ?: 0.25f).coerceIn(0.05f, 1f),
          animationSpec = tween(550),
          label = "matrixRamFraction",
        )
      val storageFraction by
        animateFloatAsState(
          targetValue = (sys?.storageUsedFraction ?: 0.25f).coerceIn(0.05f, 1f),
          animationSpec = tween(550),
          label = "matrixStorageFraction",
        )
      val batteryFraction by
        animateFloatAsState(
          targetValue =
            ((uiState.batterySnapshot?.percent ?: inspection?.batteryPercent) ?: 50) / 100f,
          animationSpec = tween(550),
          label = "matrixBatteryFraction",
        )

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          val labels =
            listOf(
              Triple("RAM", ramFraction, UniverseDiagnosticCategory.SYSTEM_HEALTH.accentColor),
              Triple("Speicher", storageFraction, UniverseDiagnosticCategory.PRIVACY_COCKPIT.accentColor),
              Triple("Akku", batteryFraction, UniverseDiagnosticCategory.BATTERY.accentColor),
            )
          labels.forEach { (name, fraction, color) ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(64.dp),
              )
              Canvas(modifier = Modifier.weight(1f).height(9.dp)) {
                val radius = CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(
                  color = Color.White.copy(alpha = 0.12f),
                  size = size,
                  cornerRadius = radius,
                )
                drawRoundRect(
                  color = color,
                  size = Size(size.width * fraction, size.height),
                  cornerRadius = radius,
                )
              }
              Text(
                text = if (hasLiveMetrics || (name == "Akku" && uiState.batterySnapshot?.percent != null)) {
                  "${(fraction * 100f).roundToInt()}%"
                } else {
                  "Bereit"
                },
                style = MonoTokenTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // 7-Kategorien Interaktive Matrix
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2,
      ) {
        categories.forEach { cat ->
          val readout = resolveCategoryReadout(cat, uiState)
          val selected = cat == selectedCategory
          Surface(
            modifier =
              Modifier.weight(1f, fill = false)
                .clip(MaterialTheme.shapes.medium)
                .minimumInteractiveComponentSize()
                .clickable { onSelectCategory(cat) }
                .testTag("matrix_cell_${cat.id}")
                .semantics {
                  stateDescription = if (selected) "Ausgewählt" else "Nicht ausgewählt"
                },
            shape = MaterialTheme.shapes.medium,
            color =
              if (selected) cat.accentColor.copy(alpha = 0.18f)
              else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
            border =
              BorderStroke(
                width = if (selected) 1.5.dp else 1.dp,
                color =
                  if (selected) cat.accentColor
                  else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
              ),
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(8.dp).clip(CircleShape).background(cat.accentColor)
                )
                Text(
                  text = cat.shortLabel,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = readout.badge,
                style = MonoTokenTextStyle,
                color = cat.accentColor,
                maxLines = 1,
              )
              Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                val r = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                drawRoundRect(
                  color = cat.accentColor.copy(alpha = 0.22f),
                  size = size,
                  cornerRadius = r,
                )
                drawRoundRect(
                  color = cat.accentColor,
                  size = Size(size.width * readout.fillFraction.coerceIn(0.08f, 1f), size.height),
                  cornerRadius = r,
                )
              }
            }
          }
        }
      }
    }
  }
}
