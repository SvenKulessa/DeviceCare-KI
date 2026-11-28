package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.devicecare.InspectionResult
import com.example.devicecare.SystemHealthSnapshot
import com.example.ui.DeviceCareUiState
import com.example.ui.theme.FormwerkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SystemHealthHubTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun snapshotComputesFractionsSafelyAndFailsClosedWhenUnavailable() {
    val empty = SystemHealthSnapshot()
    assertNull(empty.storageUsedBytes)
    assertNull(empty.storageUsedFraction)
    assertNull(empty.memoryUsedBytes)
    assertNull(empty.memoryUsedFraction)
    assertEquals("Nicht verfügbar", empty.formattedStorageUsed())
    assertEquals("Nicht verfügbar", empty.formattedMemoryTotal())

    val zero = SystemHealthSnapshot(storageTotalBytes = 0L, storageAvailableBytes = 0L)
    assertNull(zero.storageUsedFraction)

    val valid =
      SystemHealthSnapshot(
        storageTotalBytes = 100L * 1024L * 1024L * 1024L,
        storageAvailableBytes = 25L * 1024L * 1024L * 1024L,
        memoryTotalBytes = 8L * 1024L * 1024L * 1024L,
        memoryAvailableBytes = 2L * 1024L * 1024L * 1024L,
      )
    assertEquals(0.75f, valid.storageUsedFraction!!, 0.001f)
    assertEquals(0.75f, valid.memoryUsedFraction!!, 0.001f)
  }

  @Test
  fun dashboardHubShowsIdleReadyStateAndRoutesTileClicks() {
    var batteryClicks = 0
    var aiClicks = 0
    var inspectClicks = 0

    rule.setContent {
      FormwerkTheme {
        Column(modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) {
          SystemHealthDashboardHub(
            uiState = DeviceCareUiState(),
            isWide = false,
            onStartInspection = { inspectClicks++ },
            onOpenBatteryMonitor = { batteryClicks++ },
            onOpenAppPermissions = {},
            onOpenSystemSecurity = {},
            onOpenPrivacySummary = {},
            onOpenAiAdvisor = { aiClicks++ },
            onOpenCloudSync = {},
          )
        }
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag("system_health_dashboard_hub").assertExists()
    rule.onNodeWithTag("hub_battery_card").assertExists()
    rule.onNodeWithTag("hub_memory_card").assertExists()
    rule.onNodeWithTag("hub_storage_card").assertExists()
    rule.onNodeWithTag("hub_health_overview_badge").assertExists()
    rule.onNodeWithTag("hub_battery_status_icon", useUnmergedTree = true).assertExists()
    rule.onNodeWithTag("hub_memory_status_icon", useUnmergedTree = true).assertExists()
    rule.onNodeWithTag("hub_storage_status_icon", useUnmergedTree = true).assertExists()
    rule.onNodeWithTag("hub_modules_grid").assertExists()
    rule.onNodeWithText("BEREIT · LOKAL").assertExists()

    rule.onNodeWithTag("hub_battery_card").performScrollTo().performClick()
    rule.onNodeWithTag("hub_storage_card").performScrollTo().performClick()
    rule.onNodeWithTag("hub_module_ai").performScrollTo().performClick()
    assertEquals(1, batteryClicks)
    assertEquals(1, inspectClicks)
    assertEquals(1, aiClicks)
  }

  @Test
  fun dashboardHubRendersLiveStorageBatteryAndMemoryTelemetryAfterInspection() {
    val result =
      InspectionResult(
        collectedAtMillis = 12_345L,
        batteryPercent = 82,
        batteryChargingState = "CHARGING",
        batteryTemperatureDisplay = "29.4 °C",
        systemHealthSnapshot =
          SystemHealthSnapshot(
            storageTotalBytes = 64L * 1024L * 1024L * 1024L,
            storageAvailableBytes = 16L * 1024L * 1024L * 1024L,
            memoryTotalBytes = 8L * 1024L * 1024L * 1024L,
            memoryAvailableBytes = 3L * 1024L * 1024L * 1024L,
          ),
      )

    rule.setContent {
      FormwerkTheme {
        SystemHealthDashboardHub(
          uiState = DeviceCareUiState(inspectionResult = result, isInspectionStale = false),
          isWide = true,
          onStartInspection = {},
          onOpenBatteryMonitor = {},
          onOpenAppPermissions = {},
          onOpenSystemSecurity = {},
          onOpenPrivacySummary = {},
          onOpenAiAdvisor = {},
          onOpenCloudSync = {},
        )
      }
    }
    rule.waitForIdle()

    rule.onNodeWithText("LOKAL · LIVE").assertExists()
    rule.onNodeWithText("82%").assertExists()
    rule.onNodeWithText("63%").assertExists()
    rule.onNodeWithText("75%").assertExists()
    rule.onNodeWithText("LÄDT").assertExists()
    assertEquals(2, rule.onAllNodesWithText("GUT").fetchSemanticsNodes().size)
    assertTrue(result.systemHealthSnapshot!!.storageUsedFraction!! > 0.7f)
  }

  @Test
  fun dashboardHubHighlightsCriticalHealthIndicatorLevelsWhenThresholdsExceeded() {
    val criticalResult =
      InspectionResult(
        collectedAtMillis = 55_000L,
        batteryPercent = 14,
        batteryChargingState = "DISCHARGING",
        batteryTemperatureDisplay = "35.0 °C",
        systemHealthSnapshot =
          SystemHealthSnapshot(
            storageTotalBytes = 100L * 1024L * 1024L * 1024L,
            storageAvailableBytes = 5L * 1024L * 1024L * 1024L,
            memoryTotalBytes = 8L * 1024L * 1024L * 1024L,
            memoryAvailableBytes = 512L * 1024L * 1024L,
            lowMemoryReported = true,
          ),
      )

    rule.setContent {
      FormwerkTheme {
        SystemHealthDashboardHub(
          uiState = DeviceCareUiState(inspectionResult = criticalResult, isInspectionStale = false),
          isWide = true,
          onStartInspection = {},
          onOpenBatteryMonitor = {},
          onOpenAppPermissions = {},
          onOpenSystemSecurity = {},
          onOpenPrivacySummary = {},
          onOpenAiAdvisor = {},
          onOpenCloudSync = {},
        )
      }
    }
    rule.waitForIdle()

    rule.onNodeWithText("ACHTUNG").assertExists()
    rule.onNodeWithText("KRITISCH").assertExists()
    rule.onNodeWithText("HOCH").assertExists()
    rule.onNodeWithText("FAST VOLL").assertExists()
  }
}
