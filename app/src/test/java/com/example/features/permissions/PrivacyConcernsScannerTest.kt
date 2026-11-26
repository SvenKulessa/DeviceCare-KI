package com.example.features.permissions

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.example.ui.DeviceCareUiState
import com.example.ui.screens.AppPermissionsScreen
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
class PrivacyConcernsScannerTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun classifiesCameraLocationAndBackgroundLocationPrivacyConcernsTruthfully() {
    val bgLocationApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.tracker",
        appName = "Background Tracker",
        isSystemApp = false,
        totalRequestedPermissions = 4,
        grantedDangerousPermissions =
          listOf(
            GrantedDangerousPermission(
              fullName = "android.permission.ACCESS_BACKGROUND_LOCATION",
              shortName = "ACCESS_BACKGROUND_LOCATION",
              category = SensitivityCategory.LOCATION,
              description = null,
            ),
            GrantedDangerousPermission(
              fullName = "android.permission.CAMERA",
              shortName = "CAMERA",
              category = SensitivityCategory.CAMERA,
              description = null,
            ),
          ),
      )
    assertEquals(PrivacyConcernLevel.HIGH, bgLocationApp.privacyConcernLevel)
    assertTrue(bgLocationApp.privacyConcernReasons.any { it.contains("ACCESS_BACKGROUND_LOCATION") })
    assertTrue(bgLocationApp.privacyConcernReasons.any { it.contains("Kamera und Gerätestandort") })

    val cameraOnlyApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.scanner",
        appName = "Doc Scanner",
        isSystemApp = false,
        totalRequestedPermissions = 2,
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
    assertEquals(PrivacyConcernLevel.ELEVATED, cameraOnlyApp.privacyConcernLevel)
    assertTrue(cameraOnlyApp.privacyConcernReasons.any { it.contains("Kamera") })

    val cleanApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.calc",
        appName = "Offline Calculator",
        isSystemApp = false,
        totalRequestedPermissions = 1,
        grantedDangerousPermissions = emptyList(),
      )
    assertEquals(PrivacyConcernLevel.NONE, cleanApp.privacyConcernLevel)
    assertTrue(cleanApp.privacyConcernReasons.isEmpty())
  }

  @Test
  fun appPermissionsScreenHighlightsPrivacyConcernsOnSummaryAndCards() {
    val bgApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.lensmap",
        appName = "LensMap Pro",
        isSystemApp = false,
        totalRequestedPermissions = 5,
        grantedDangerousPermissions =
          listOf(
            GrantedDangerousPermission(
              fullName = "android.permission.CAMERA",
              shortName = "CAMERA",
              category = SensitivityCategory.CAMERA,
              description = null,
            ),
            GrantedDangerousPermission(
              fullName = "android.permission.ACCESS_FINE_LOCATION",
              shortName = "ACCESS_FINE_LOCATION",
              category = SensitivityCategory.LOCATION,
              description = null,
            ),
          ),
      )
    val scanResult =
      InstalledAppsScanResult(
        scannedAtMillis = 99_000L,
        visiblePackageCount = 1,
        apps = listOf(bgApp),
      )
    assertEquals(1, scanResult.appsWithPrivacyConcernsCount)
    assertEquals(1, scanResult.highPrivacyConcernCount)

    val sections = PermissionSensitivityClassifier.categorizeApps(scanResult.apps)
    rule.setContent {
      FormwerkTheme {
        AppPermissionsScreen(
          uiState =
            DeviceCareUiState(
              installedAppsScanResult = scanResult,
              filteredInstalledApps = scanResult.apps,
              categorizedAppSections = sections,
              collapsedAppSections = emptySet(),
            ),
          onStartScan = {},
          onSearchQueryChange = {},
          onSelectFilter = {},
          onToggleHideSystemApps = {},
          onToggleSectionCollapsed = {},
          onShowSnackbar = {},
        )
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag("privacy_concerns_highlight_banner").assertExists()
    rule.onNodeWithText("1 Apps mit möglichen Datenschutz-Hinweisen").assertExists()
    rule.onNodeWithTag("installed_apps_grid")
      .performScrollToNode(hasTestTag("installed_app_card_com.example.lensmap"))
    rule.onNodeWithTag("app_privacy_concern_com.example.lensmap", useUnmergedTree = true)
      .assertExists()
    rule.onNodeWithText("Datenschutz-Hinweis · Hoher Hinweis").assertExists()
  }
}
