package com.example.ui.screens

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.features.permissions.AppOriginFilter
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.PermissionSensitivityClassifier
import com.example.security.firewall.AppProtectionAnalyzer
import com.example.ui.DeviceCareUiState
import com.example.ui.theme.FormwerkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RethinkAndAccessibilityUiTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun rethinkScreenRendersProfilesCategoriesAndScanCounts() {
    val flagged =
      InstalledAppPermissionInfo(
        packageName = "com.example.sideloadtool",
        appName = "Sideload Tool",
        isSystemApp = false,
        totalRequestedPermissions = 2,
        grantedDangerousPermissions = emptyList(),
      ).let { app ->
        app.copy(
          securityPosture =
            AppProtectionAnalyzer.analyzeMetadata(
              app = app,
              requestedPermissions = setOf("android.permission.INTERNET"),
              grantedPermissions = emptySet(),
              installerPackage = "com.google.android.packageinstaller",
              isDebuggable = true,
              isTestOnly = false,
              lastUpdateTimeMillis = 500_000L,
              signerSha256Prefixes = listOf("AABBCC"),
              hasInternetPermission = true,
              declaresAccessibilityService = false,
            )
        )
      }

    var openedApps = false
    rule.setContent {
      FormwerkTheme {
        RethinkRecommendationsScreen(
          uiState =
            DeviceCareUiState(
              installedAppsScanResult =
                InstalledAppsScanResult(
                  scannedAtMillis = 1_700_000_000_000L,
                  visiblePackageCount = 1,
                  apps = listOf(flagged),
                )
            ),
          onBack = {},
          onOpenAppPermissions = { openedApps = true },
          onShowSnackbar = {},
        )
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag("rethink_recommendations_screen").assertExists()
    rule.onNodeWithTag("rethink_scan_bridge_card").assertExists()
    rule.onNodeWithText("1 sichtbare Apps geprüft: 1× Komplett blockieren · 0× Nur Vordergrund · 1× Manipulations-/Quellenhinweis.")
      .assertExists()

    // Switch to LOCKDOWN profile & filter DNS only
    rule.onNodeWithTag("rethink_recommendations_list")
      .performScrollToNode(hasTestTag("rethink_profile_lockdown"))
    rule.onNodeWithTag("rethink_profile_lockdown").performClick()
    rule.onNodeWithTag("rethink_recommendations_list")
      .performScrollToNode(hasTestTag("rethink_category_section"))
    rule.onNodeWithTag("rethink_cat_dns").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag("rethink_recommendations_list")
      .performScrollToNode(hasTestTag("rethink_rec_dns_doh_rdns"))
    rule.onNodeWithTag("rethink_rec_dns_doh_rdns").assertExists()
    rule.onNodeWithTag("rethink_rec_fw_screen_off").assertDoesNotExist()

    rule.onNodeWithTag("rethink_recommendations_list")
      .performScrollToNode(hasTestTag("rethink_scan_bridge_card"))
    rule.onNodeWithTag("rethink_open_apps_button").performClick()
    assertTrue(openedApps)
  }

  @Test
  fun appPermissionsShowsSystemFilterChipsTamperStatusAndFirewallBadges() {
    val systemApp =
      InstalledAppPermissionInfo(
        packageName = "com.android.vendor.telemetry",
        appName = "System Telemetry",
        isSystemApp = true,
        totalRequestedPermissions = 1,
        grantedDangerousPermissions = emptyList(),
      ).let { app ->
        app.copy(
          securityPosture =
            AppProtectionAnalyzer.analyzeMetadata(
              app = app,
              requestedPermissions = setOf("android.permission.INTERNET"),
              grantedPermissions = emptySet(),
              installerPackage = null,
              isDebuggable = false,
              isTestOnly = false,
              lastUpdateTimeMillis = 100L,
              signerSha256Prefixes = listOf("123456"),
              hasInternetPermission = true,
              declaresAccessibilityService = false,
            )
        )
      }

    var selectedOrigin: AppOriginFilter? = null
    var openedRethink = false
    val scanResult =
      InstalledAppsScanResult(
        scannedAtMillis = 1_700_000_000_000L,
        visiblePackageCount = 1,
        apps = listOf(systemApp),
      )

    rule.setContent {
      FormwerkTheme {
        AppPermissionsScreen(
          uiState =
            DeviceCareUiState(
              installedAppsScanResult = scanResult,
              filteredInstalledApps = scanResult.apps,
              categorizedAppSections = PermissionSensitivityClassifier.categorizeApps(scanResult.apps),
              collapsedAppSections = emptySet(),
            ),
          onStartScan = {},
          onSearchQueryChange = {},
          onSelectFilter = {},
          onToggleHideSystemApps = {},
          onToggleSectionCollapsed = {},
          onShowSnackbar = {},
          onSelectOriginFilter = { selectedOrigin = it },
          onOpenRethinkGuide = { openedRethink = true },
        )
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag("only_system_apps_chip").performClick()
    assertEquals(AppOriginFilter.SYSTEM_ONLY, selectedOrigin)

    rule.onNodeWithTag("installed_apps_grid")
      .performScrollToNode(hasTestTag("open_rethink_guide_from_apps"))
    rule.onNodeWithTag("open_rethink_guide_from_apps").performClick()
    assertTrue(openedRethink)

    rule.onNodeWithTag("installed_apps_grid")
      .performScrollToNode(hasTestTag("installed_app_card_com.android.vendor.telemetry"))
    rule.onNodeWithTag("app_tamper_status_com.android.vendor.telemetry", useUnmergedTree = true)
      .assertExists()
    rule.onNodeWithTag("app_firewall_rec_com.android.vendor.telemetry", useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun inspectionScreenExposesQuickNavAndRethinkActions() {
    var quickNavOpened = false
    var rethinkOpened = false

    rule.setContent {
      FormwerkTheme {
        InspectionScreen(
          uiState = DeviceCareUiState(),
          onStartInspection = {},
          onCycleThemeMode = {},
          onSettingsMessage = {},
          onOpenBatteryMonitor = {},
          onOpenAiAdvisor = {},
          onOpenCloudSync = {},
          onOpenSystemSecurity = {},
          onOpenPrivacySummary = {},
          onOpenAppPermissions = {},
          onOpenRoadmap = {},
          onOpenRethinkGuide = { rethinkOpened = true },
          onOpenQuickNav = { quickNavOpened = true },
        )
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag("quick_nav_menu_button").performClick()
    assertTrue(quickNavOpened)

    rule.onNodeWithTag("open_rethink_guide_button").performScrollTo().performClick()
    assertTrue(rethinkOpened)
  }
}
