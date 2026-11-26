package com.example.security.firewall

import com.example.features.permissions.GrantedDangerousPermission
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.SensitivityCategory
import com.example.security.malware.Risk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RethinkFirewallRuleEngineTest {

  private fun sampleApp(
    packageName: String = "com.example.socialcam",
    appName: String = "Social Cam",
    isSystemApp: Boolean = false,
    perms: List<GrantedDangerousPermission> = emptyList(),
  ) =
    InstalledAppPermissionInfo(
      packageName = packageName,
      appName = appName,
      isSystemApp = isSystemApp,
      totalRequestedPermissions = perms.size + 2,
      grantedDangerousPermissions = perms,
      versionName = "2.1.0",
    )

  @Test
  fun noInternetPermissionYieldsOfflineProfileAndOptionalPreventiveRuleOnly() {
    val posture =
      AppProtectionAnalyzer.analyzeMetadata(
        app = sampleApp(packageName = "com.example.notes", appName = "Offline Notizen"),
        requestedPermissions = emptySet(),
        grantedPermissions = emptySet(),
        installerPackage = "com.android.vending",
        isDebuggable = false,
        isTestOnly = false,
        lastUpdateTimeMillis = 100_000L,
        signerSha256Prefixes = listOf("AA11BB"),
        hasInternetPermission = false,
        declaresAccessibilityService = false,
      )

    assertEquals(
      NetworkTelemetryStatus.NO_NETWORK_CAPABILITY,
      posture.networkActivityProfile.telemetryStatus,
    )
    assertEquals(1, posture.rethinkFirewallRules.size)
    val rule = posture.rethinkFirewallRules.first()
    assertEquals(RethinkRuleType.PREVENTIVE_ISOLATE, rule.ruleType)
    assertFalse(rule.recommendedEnabled)
  }

  @Test
  fun permissionsAndObservedNetworkActivitySuggestTargetedRethinkRules() {
    val app =
      sampleApp(
        packageName = "com.example.streamcam",
        appName = "Stream Cam",
        perms =
          listOf(
            GrantedDangerousPermission(
              fullName = "android.permission.CAMERA",
              shortName = "CAMERA",
              category = SensitivityCategory.CAMERA,
              description = null,
            ),
            GrantedDangerousPermission(
              fullName = "android.permission.ACCESS_BACKGROUND_LOCATION",
              shortName = "ACCESS_BACKGROUND_LOCATION",
              category = SensitivityCategory.LOCATION,
              description = null,
            ),
          ),
      )

    val posture =
      AppProtectionAnalyzer.analyzeMetadata(
        app = app,
        requestedPermissions =
          setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
          ),
        grantedPermissions =
          setOf(
            "android.permission.CAMERA",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
          ),
        installerPackage = "com.android.vending",
        isDebuggable = false,
        isTestOnly = false,
        lastUpdateTimeMillis = 100_000L,
        signerSha256Prefixes = listOf("CC22DD"),
        hasInternetPermission = true,
        declaresAccessibilityService = false,
        usesCleartextTraffic = true,
        observedRxBytes = 8_500_000L,
        observedTxBytes = 2_400_000L,
      )

    val net = posture.networkActivityProfile
    assertEquals(NetworkTelemetryStatus.OBSERVED, net.telemetryStatus)
    assertTrue(net.hasHighObservedTransfer)
    assertTrue(net.usesCleartextTraffic)
    assertTrue(net.declaresBootAutoStart)
    assertTrue(net.declaresBackgroundSync)
    assertTrue(net.formattedTransferSummary.contains("Beobachtet seit Gerätestart"))

    val rulesById = posture.rethinkFirewallRules.associateBy { it.id }
    assertFalse(rulesById.getValue("rule_block_all").recommendedEnabled)
    assertTrue(rulesById.getValue("rule_block_background").recommendedEnabled)
    assertTrue(rulesById.getValue("rule_block_screen_off").recommendedEnabled)
    assertTrue(rulesById.getValue("rule_block_mobile").recommendedEnabled)
    assertTrue(rulesById.getValue("rule_enforce_dns").recommendedEnabled)
    assertEquals(RethinkRulePriority.HIGH, rulesById.getValue("rule_enforce_dns").priority)
    assertTrue(rulesById.containsKey("rule_isolate_lan"))
    assertFalse(rulesById.getValue("rule_isolate_lan").recommendedEnabled)
  }

  @Test
  fun debuggableSideloadWithWifiStateRecommendsFullIsolationAndLanBlocking() {
    val app = sampleApp(packageName = "com.unknown.sideload", appName = "Unknown Helper")
    val posture =
      AppProtectionAnalyzer.analyzeMetadata(
        app = app,
        requestedPermissions =
          setOf("android.permission.INTERNET", "android.permission.ACCESS_WIFI_STATE"),
        grantedPermissions = emptySet(),
        installerPackage = "com.google.android.packageinstaller",
        isDebuggable = true,
        isTestOnly = false,
        lastUpdateTimeMillis = 100_000L,
        signerSha256Prefixes = listOf("EE33FF"),
        hasInternetPermission = true,
        declaresAccessibilityService = false,
      )

    assertEquals(NetworkTelemetryStatus.OS_RESTRICTED, posture.networkActivityProfile.telemetryStatus)
    val rulesById = posture.rethinkFirewallRules.associateBy { it.id }
    assertTrue(rulesById.getValue("rule_block_all").recommendedEnabled)
    assertEquals(RethinkRulePriority.CRITICAL, rulesById.getValue("rule_block_all").priority)
    assertTrue(rulesById.getValue("rule_isolate_lan").recommendedEnabled)

    // User toggles off block_all and activates preventive rule summary
    val overrides = mapOf("rule_block_all" to false)
    val effective =
      RethinkFirewallRuleEngine.resolveEffectiveStates(posture.rethinkFirewallRules, overrides)
    assertFalse(effective.getValue("rule_block_all"))
    val summary =
      RethinkFirewallRuleEngine.formatRethinkRuleSummary(
        app = app,
        rules = posture.rethinkFirewallRules,
        overrides = overrides,
      )
    assertTrue(summary.contains(app.packageName))
    assertFalse(summary.contains("(block_both)"))
    assertTrue(summary.contains("(block_lan)"))
  }

  @Test
  fun coreConnectivitySystemAppKeepsBackgroundOpenByDefaultWhileKeepingDnsFilter() {
    val gms =
      sampleApp(
        packageName = "com.google.android.gms",
        appName = "Google Play-Dienste",
        isSystemApp = true,
      )
    val network =
      RethinkFirewallRuleEngine.buildNetworkActivityProfile(
        requestedPermissions = setOf("android.permission.INTERNET", "android.permission.WAKE_LOCK"),
        grantedPermissions = emptySet(),
        hasInternetPermission = true,
      )
    val rules =
      RethinkFirewallRuleEngine.suggestRules(
        app = gms,
        networkActivity = network,
        requestedPermissions = setOf("android.permission.INTERNET", "android.permission.WAKE_LOCK"),
        grantedPermissions = emptySet(),
        isSideloaded = false,
        isDebuggable = false,
        declaresAccessibilityService = false,
        malwareRisk = Risk.NIEDRIG,
      )
    val byId = rules.associateBy { it.id }
    assertFalse(byId.getValue("rule_block_all").recommendedEnabled)
    assertFalse(byId.getValue("rule_block_background").recommendedEnabled)
    assertTrue(byId.getValue("rule_enforce_dns").recommendedEnabled)
  }
}
