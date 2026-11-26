package com.example.security.firewall

import com.example.features.permissions.GrantedDangerousPermission
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.SensitivityCategory
import com.example.security.malware.Risk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppProtectionAnalyzerTest {

  private fun baseApp(
    packageName: String = "com.example.sample",
    appName: String = "Sample App",
    isSystemApp: Boolean = false,
    perms: List<GrantedDangerousPermission> = emptyList(),
  ): InstalledAppPermissionInfo =
    InstalledAppPermissionInfo(
      packageName = packageName,
      appName = appName,
      isSystemApp = isSystemApp,
      totalRequestedPermissions = perms.size + 1,
      grantedDangerousPermissions = perms,
      versionName = "1.0.0",
    )

  @Test
  fun detectsDebuggableSideloadAccessibilityAndRecentUpdateWithoutFalseMalwareCertainty() {
    val now = 1_700_000_000_000L
    val posture =
      AppProtectionAnalyzer.analyzeMetadata(
        app = baseApp(packageName = "com.suspicious.helper", appName = "Quick Helper"),
        requestedPermissions =
          setOf(
            "android.permission.INTERNET",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.SYSTEM_ALERT_WINDOW",
          ),
        grantedPermissions = setOf("android.permission.READ_SMS"),
        installerPackage = "com.google.android.packageinstaller",
        isDebuggable = true,
        isTestOnly = false,
        lastUpdateTimeMillis = now - (2L * 24L * 60L * 60L * 1000L),
        signerSha256Prefixes = listOf("AABBCC112233"),
        hasInternetPermission = true,
        declaresAccessibilityService = true,
        nowMillis = now,
      )

    assertTrue(posture.hasManipulationWarnings)
    assertTrue(posture.isDebuggable)
    assertTrue(posture.isSideloadedOrUnknown)
    assertTrue(posture.recentlyUpdatedWithin7Days)
    assertEquals(TamperSignalSeverity.CRITICAL, posture.highestTamperSeverity)
    assertTrue(posture.tamperSignals.any { it.id == "debuggable_build" })
    assertTrue(posture.tamperSignals.any { it.id == "sideload_installer" })
    assertTrue(posture.tamperSignals.any { it.id == "request_install_packages" })
    assertTrue(posture.tamperSignals.any { it.id == "system_alert_window" })
    assertTrue(posture.tamperSignals.any { it.id == "accessibility_service" })
    assertTrue(posture.tamperSignals.any { it.id == "recent_update" })

    // Fail-closed contract: static-only observation without verified runtime trace stays <= MITTEL
    assertTrue(posture.malwareVerdict.risk <= Risk.MITTEL)
    assertEquals(FirewallAction.BLOCK_ALL, posture.firewallRecommendation.action)
  }

  @Test
  fun recommendsBlockAllForOfflineUtilitiesAndNoNetworkWhenInternetMissing() {
    val calc =
      AppProtectionAnalyzer.analyzeMetadata(
        app = baseApp(packageName = "com.example.calculator", appName = "Taschenrechner"),
        requestedPermissions = setOf("android.permission.INTERNET"),
        grantedPermissions = emptySet(),
        installerPackage = "com.android.vending",
        isDebuggable = false,
        isTestOnly = false,
        lastUpdateTimeMillis = 100_000L,
        signerSha256Prefixes = listOf("112233445566"),
        hasInternetPermission = true,
        declaresAccessibilityService = false,
        nowMillis = 1_700_000_000_000L,
      )
    assertFalse(calc.hasManipulationWarnings)
    assertEquals(FirewallAction.BLOCK_ALL, calc.firewallRecommendation.action)

    val offlineNotes =
      AppProtectionAnalyzer.analyzeMetadata(
        app = baseApp(packageName = "com.example.localnotes", appName = "Local Notes"),
        requestedPermissions = emptySet(),
        grantedPermissions = emptySet(),
        installerPackage = "com.android.vending",
        isDebuggable = false,
        isTestOnly = false,
        lastUpdateTimeMillis = 100_000L,
        signerSha256Prefixes = listOf("112233445566"),
        hasInternetPermission = false,
        declaresAccessibilityService = false,
        nowMillis = 1_700_000_000_000L,
      )
    assertEquals(FirewallAction.NO_NETWORK_PERMISSION, offlineNotes.firewallRecommendation.action)
  }

  @Test
  fun differentiatesCoreSystemAppsFromOtherSystemAndSensitiveApps() {
    val gms =
      AppProtectionAnalyzer.analyzeMetadata(
        app =
          baseApp(
            packageName = "com.google.android.gms",
            appName = "Google Play-Dienste",
            isSystemApp = true,
          ),
        requestedPermissions = setOf("android.permission.INTERNET"),
        grantedPermissions = emptySet(),
        installerPackage = null,
        isDebuggable = false,
        isTestOnly = false,
        lastUpdateTimeMillis = 100_000L,
        signerSha256Prefixes = listOf("112233445566"),
        hasInternetPermission = true,
        declaresAccessibilityService = false,
        nowMillis = 1_700_000_000_000L,
      )
    assertEquals(FirewallAction.ALLOW_RESTRICTED, gms.firewallRecommendation.action)
    assertFalse(gms.isSideloadedOrUnknown)

    val vendorTelemetry =
      AppProtectionAnalyzer.analyzeMetadata(
        app =
          baseApp(
            packageName = "com.oem.analytics",
            appName = "System Diagnostics",
            isSystemApp = true,
          ),
        requestedPermissions = setOf("android.permission.INTERNET"),
        grantedPermissions = emptySet(),
        installerPackage = null,
        isDebuggable = false,
        isTestOnly = false,
        lastUpdateTimeMillis = 100_000L,
        signerSha256Prefixes = listOf("112233445566"),
        hasInternetPermission = true,
        declaresAccessibilityService = false,
        nowMillis = 1_700_000_000_000L,
      )
    assertEquals(FirewallAction.ALLOW_FOREGROUND_ONLY, vendorTelemetry.firewallRecommendation.action)

    val cameraApp =
      baseApp(
        packageName = "com.example.cam",
        appName = "Snap Cam",
        perms =
          listOf(
            GrantedDangerousPermission(
              fullName = "android.permission.CAMERA",
              shortName = "CAMERA",
              category = SensitivityCategory.CAMERA,
              description = null,
            )
          ),
      )
    val camPosture =
      AppProtectionAnalyzer.analyzeMetadata(
        app = cameraApp,
        requestedPermissions = setOf("android.permission.INTERNET", "android.permission.CAMERA"),
        grantedPermissions = setOf("android.permission.CAMERA"),
        installerPackage = "com.android.vending",
        isDebuggable = false,
        isTestOnly = false,
        lastUpdateTimeMillis = 100_000L,
        signerSha256Prefixes = listOf("ABCDEF123456"),
        hasInternetPermission = true,
        declaresAccessibilityService = false,
        nowMillis = 1_700_000_000_000L,
      )
    assertEquals(FirewallAction.ALLOW_FOREGROUND_ONLY, camPosture.firewallRecommendation.action)
  }

  @Test
  fun buildsRethinkRecommendationsAcrossProfilesAndReflectsScanCounts() {
    val flagged =
      baseApp(packageName = "com.example.flagged", appName = "Flagged Tool").let { info ->
        info.copy(
          securityPosture =
            AppProtectionAnalyzer.analyzeMetadata(
              app = info,
              requestedPermissions = setOf("android.permission.INTERNET"),
              grantedPermissions = emptySet(),
              installerPackage = null,
              isDebuggable = true,
              isTestOnly = false,
              lastUpdateTimeMillis = 100_000L,
              signerSha256Prefixes = listOf("112233"),
              hasInternetPermission = true,
              declaresAccessibilityService = false,
            )
        )
      }
    val scanResult =
      InstalledAppsScanResult(
        scannedAtMillis = 1_700_000_000_000L,
        visiblePackageCount = 1,
        apps = listOf(flagged),
      )

    val lockdown =
      AppProtectionAnalyzer.rethinkRecommendationsFor(RethinkProfile.LOCKDOWN, scanResult)
    assertTrue(lockdown.any { it.category == RethinkSettingCategory.FIREWALL })
    assertTrue(lockdown.any { it.category == RethinkSettingCategory.DNS })
    assertTrue(lockdown.any { it.category == RethinkSettingCategory.NETWORK })

    val blockRec = lockdown.first { it.id == "fw_block_flagged" }
    assertTrue(blockRec.recommendedValue.contains("1 App(s) blockieren"))
    assertEquals("KRITISCH", blockRec.priorityLabel)
  }
}
