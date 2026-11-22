package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.example.devicecare.InspectionResult
import com.example.ui.DeviceCareUiState
import com.example.ui.theme.FormwerkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Android 15 QA touch regression. Robolectric mdpi makes viewport dp equal physical px.
 * These tests prove Compose scroll/tap semantics, not physical touchscreen latency or ANR freedom.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InspectionTouchScrollTest {
  @get:Rule val rule = createComposeRule()

  private val syntheticResult = mutableStateOf<InspectionResult?>(null)
  private var deliberateScanTaps = 0

  private fun renderInspection() {
    rule.setContent {
      FormwerkTheme {
        InspectionScreen(
          uiState = DeviceCareUiState(inspectionResult = syntheticResult.value),
          onStartInspection = { deliberateScanTaps++ },
          onCycleThemeMode = {},
          onSettingsMessage = {},
          onOpenBatteryMonitor = {},
          onOpenAiAdvisor = {},
          onOpenCloudSync = {},
          onOpenSystemSecurity = {},
          onOpenPrivacySummary = {},
          onOpenAppPermissions = {},
          onOpenRoadmap = {},
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
    rule.waitForIdle()
  }

  private fun verticalOffset(): Float {
    val semantics =
      rule.onNodeWithTag("devicecare_scroll_content").fetchSemanticsNode().config
    val range = semantics[SemanticsProperties.VerticalScrollAxisRange]
    assertTrue("Inspection must offer vertical scrolling", range.maxValue() > 0f)
    return range.value()
  }

  private fun verifySwipeAfterResultRecomposition(expectedWidthPx: Int) {
    renderInspection()

    val scrollNode = rule.onNodeWithTag("devicecare_scroll_content")
    val width = scrollNode.fetchSemanticsNode().boundsInRoot.width
    assertEquals("Robolectric mdpi viewport width", expectedWidthPx.toFloat(), width, 2f)

    // A deliberate tap must be distinguishable from a drag; neither scroll initiates a scan.
    rule.onNodeWithTag("inspection_start_button").performClick()
    assertEquals(1, deliberateScanTaps)

    val beforeFirstSwipe = verticalOffset()
    scrollNode.performTouchInput { swipeUp() }
    rule.waitForIdle()
    assertTrue("Touch drag must move LazyColumn", verticalOffset() > beforeFirstSwipe + 1f)
    assertEquals("Touch drag must not re-trigger manual inspection", 1, deliberateScanTaps)

    // Emit a synthetic result as the UI state would after the explicit local inspection.
    // This exercises result insertion and recomposition without any device/API/provider reads.
    rule.runOnIdle {
      syntheticResult.value =
        InspectionResult(
          collectedAtMillis = 1_234L,
          batteryPercent = 75,
          profileMetrics = (1..12).map { "Synthetisches Feld $it" to "SYNTHETIC_ONLY_$it" },
        )
    }
    rule.waitForIdle()

    // A new result can change lazy item heights and leave the viewport at its scroll end.
    // Reposition with a real reverse swipe before testing a second forward touch gesture.
    // This avoids asserting movement past the current end of the scrollable content.
    scrollNode.performTouchInput { swipeDown() }
    rule.waitForIdle()
    assertEquals("Reverse swipe must not start a scan", 1, deliberateScanTaps)
    val afterRecomposition = verticalOffset()
    scrollNode.performTouchInput { swipeUp() }
    rule.waitForIdle()
    assertTrue(
      "A fresh touch drag must scroll after result recomposition",
      verticalOffset() > afterRecomposition + 1f,
    )
    assertEquals("Recomposition and drag must not initiate a second scan", 1, deliberateScanTaps)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w320dp-h640dp-mdpi")
  fun touchScroll320PxIsIndependentOfExplicitScanTap() {
    verifySwipeAfterResultRecomposition(expectedWidthPx = 320)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w390dp-h700dp-mdpi")
  fun touchScroll390PxIsIndependentOfExplicitScanTap() {
    verifySwipeAfterResultRecomposition(expectedWidthPx = 390)
  }
}
