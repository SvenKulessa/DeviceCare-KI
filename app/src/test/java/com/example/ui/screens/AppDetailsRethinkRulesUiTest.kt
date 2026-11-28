package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.features.permissions.GrantedDangerousPermission
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.PermissionSensitivityClassifier
import com.example.features.permissions.SensitivityCategory
import com.example.security.firewall.AppProtectionAnalyzer
import com.example.ui.DeviceCareUiState
import com.example.ui.theme.FormwerkTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class AppDetailsRethinkRulesUiTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun appDetailsViewDisplaysNetworkActivityAndToggleableRethinkFirewallRules() {
    val pkg = "com.example.camstream"
    val baseApp =
      InstalledAppPermissionInfo(
        packageName = pkg,
        appName = "Cam Stream",
        isSystemApp = false,
        totalRequestedPermissions = 5,
        grantedDangerousPermissions =
          listOf(
            GrantedDangerousPermission(
              fullName = "android.permission.CAMERA",
              shortName = "CAMERA",
              category = SensitivityCategory.CAMERA,
              description = null,
            )
          ),
      )
    val analyzedApp =
      baseApp.copy(
        securityPosture =
          AppProtectionAnalyzer.analyzeMetadata(
            app = baseApp,
            requestedPermissions =
              setOf(
                "android.permission.INTERNET",
                "android.permission.CAMERA",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
              ),
            grantedPermissions = setOf("android.permission.CAMERA"),
            installerPackage = "com.android.vending",
            isDebuggable = false,
            isTestOnly = false,
            lastUpdateTimeMillis = 100_000L,
            signerSha256Prefixes = listOf("AA11BB22"),
            hasInternetPermission = true,
            declaresAccessibilityService = false,
            usesCleartextTraffic = true,
            observedRxBytes = 3_145_728L,
            observedTxBytes = 1_048_576L,
          )
      )
    val scanResult =
      InstalledAppsScanResult(
        scannedAtMillis = 1_700_000_000_000L,
        visiblePackageCount = 1,
        apps = listOf(analyzedApp),
      )

    val snackbars = mutableListOf<String>()
    rule.setContent {
      var overrides by remember { mutableStateOf<Map<String, Map<String, Boolean>>>(emptyMap()) }
      FormwerkTheme {
        AppPermissionsScreen(
          uiState =
            DeviceCareUiState(
              installedAppsScanResult = scanResult,
              filteredInstalledApps = scanResult.apps,
              categorizedAppSections = PermissionSensitivityClassifier.categorizeApps(scanResult.apps),
              collapsedAppSections = emptySet(),
              appRethinkRuleOverrides = overrides,
            ),
          onStartScan = {},
          onSearchQueryChange = {},
          onSelectFilter = {},
          onToggleHideSystemApps = {},
          onToggleSectionCollapsed = {},
          onShowSnackbar = { snackbars.add(it) },
          onToggleAppRethinkRule = { packageName, ruleId, enabled ->
            val next = overrides.toMutableMap()
            val appMap = next[packageName].orEmpty().toMutableMap()
            appMap[ruleId] = enabled
            next[packageName] = appMap
            overrides = next
          },
          onResetAppRethinkRules = { packageName -> overrides = overrides - packageName },
        )
      }
    }
    rule.waitForIdle()

    // Open app details bottom sheet
    rule.onNodeWithTag("installed_apps_grid").performScrollToNode(hasTestTag("app_details_button_$pkg"))
    rule.onNodeWithTag("app_details_button_$pkg").performScrollTo().performClick()

    val sheetList = "app_detail_sheet_list_$pkg"
    rule.waitUntil(timeoutMillis = 5_000) {
      rule.onAllNodes(hasTestTag(sheetList)).fetchSemanticsNodes().isNotEmpty()
    }
    rule.waitForIdle()

    rule.onNodeWithTag(sheetList).performScrollToNode(hasTestTag("app_network_activity_card_$pkg"))
    rule.onNodeWithTag("app_network_activity_card_$pkg").assertExists()

    rule.onNodeWithTag(sheetList).performScrollToNode(hasTestTag("app_rethink_rules_section_$pkg"))
    rule.onNodeWithTag("app_rethink_rules_section_$pkg").assertExists()
    // Default recommended: block_background, block_screen_off, block_mobile, enforce_dns = 4 of 5 active
    rule.onNodeWithText("4/5 aktiv").assertExists()

    // Toggle rule_block_all ON -> becomes 5/5 aktiv
    rule.onNodeWithTag(sheetList).performScrollToNode(hasTestTag("rethink_rule_switch_rule_block_all"))
    rule.onNodeWithTag("rethink_rule_switch_rule_block_all").performClick()
    rule.waitForIdle()
    rule.onNodeWithText("5/5 aktiv").assertExists()

    // Copy active rules summary
    rule.onNodeWithTag(sheetList).performScrollToNode(hasTestTag("copy_rethink_rules_button_$pkg"))
    rule.onNodeWithTag("copy_rethink_rules_button_$pkg").performClick()
    rule.waitForIdle()
    assertTrue(snackbars.any { it.contains("Cam Stream") && it.contains("kopiert") })

    // Reset overrides -> restores 4/5 aktiv
    rule.onNodeWithTag(sheetList).performScrollToNode(hasTestTag("app_rethink_reset_$pkg"))
    rule.onNodeWithTag("app_rethink_reset_$pkg").performClick()
    rule.waitForIdle()
    rule.onNodeWithText("4/5 aktiv").assertExists()
  }
}
