package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.core.evidence.ObservationStatus
import com.example.devicecare.ObservationDisplayRow
import com.example.ui.theme.FormwerkTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regression for physical Android-15 QA-02: narrow German diagnostics may not collapse. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InspectionEvidenceLayoutTest {
  @get:Rule val rule = createComposeRule()

  private val longLabel = "App-Berechtigungen"
  private val longValue =
    "Nur für die von Android sichtbare Stichprobe verfügbar, kein vollständiger App-Zugriff"

  private fun render(widthDp: Int, fontScale: Float) {
    rule.setContent {
      CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = fontScale)) {
        FormwerkTheme {
          Column(modifier = Modifier.width(widthDp.dp).testTag("qa_viewport")) {
            InspectionLabelValueRow(
              label = longLabel,
              value = longValue,
              testId = "qa_profile",
            )
            ObservationGroupCard(
              title = "Akku-Beobachtungen (LOCAL_BATTERY)",
              rows =
                listOf(
                  ObservationDisplayRow(
                    id = "battery_percent",
                    label = "Akkuladestand",
                    valueText = "75 %",
                    status = ObservationStatus.OBSERVED,
                    limitationCode = "NONE",
                  ),
                  ObservationDisplayRow(
                    id = "battery_temperature",
                    label = "Akkutemperatur",
                    valueText = "UNSUPPORTED (OEM_UNAVAILABLE)",
                    status = ObservationStatus.UNSUPPORTED,
                    limitationCode = "OEM_UNAVAILABLE",
                  ),
                ),
            )
          }
        }
      }
    }
    rule.waitForIdle()
  }

  private fun assertNonOverlappingMetric(widthDp: Int) {
    val viewport =
      rule.onNodeWithTag("qa_viewport", useUnmergedTree = true)
        .fetchSemanticsNode().boundsInRoot
    val label =
      rule.onNodeWithTag("qa_profile_label", useUnmergedTree = true)
        .fetchSemanticsNode().boundsInRoot
    val value =
      rule.onNodeWithTag("qa_profile_value", useUnmergedTree = true)
        .fetchSemanticsNode().boundsInRoot

    assertTrue("Viewport must be positive", viewport.width > 0f)
    assertTrue("Width $widthDp: label may not overflow to the right", label.right <= viewport.right + 1f)
    assertTrue("Width $widthDp: value may not overflow to the right", value.right <= viewport.right + 1f)
    assertTrue("Width $widthDp: label/value must not overlap", label.bottom <= value.top + 1f)
    assertTrue("Width $widthDp: labels must not collapse to one character", label.width > 80f)
    assertTrue("Width $widthDp: values must use the available width", value.width > 80f)
  }

  @Test
  fun resultLabelsAndValuesStayReadableAt320WithLargeFont() {
    render(widthDp = 320, fontScale = 1.8f)
    assertNonOverlappingMetric(320)
    rule.onNodeWithTag("qa_profile").assertTextContains(longLabel, substring = true)
    rule.onNodeWithTag("qa_profile").assertTextContains(longValue, substring = true)
  }

  @Test
  fun resultLabelsAndValuesStayReadableAt390WithLargeFont() {
    render(widthDp = 390, fontScale = 2.0f)
    assertNonOverlappingMetric(390)
    rule.onNodeWithTag("inspection_observation_battery_percent").assertTextContains("75 %", substring = true)
    rule.onNodeWithTag("inspection_observation_battery_temperature")
      .assertTextContains("UNSUPPORTED", substring = true)
  }

  @Test
  fun screenreaderSemanticsRetainLabelValueAndUnavailableStatus() {
    render(widthDp = 390, fontScale = 1.0f)
    rule.onNodeWithTag("qa_profile").assertTextContains(longLabel, substring = true)
    rule.onNodeWithTag("qa_profile").assertTextContains(longValue, substring = true)
    rule.onNodeWithTag("inspection_observation_battery_percent")
      .assertTextContains("Akkuladestand", substring = true)
    rule.onNodeWithTag("inspection_observation_battery_temperature")
      .assertTextContains("OEM_UNAVAILABLE", substring = true)
    rule.onNodeWithTag("inspection_observation_battery_temperature_value", useUnmergedTree = true)
      .assertExists()
  }
}
