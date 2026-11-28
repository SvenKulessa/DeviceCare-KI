package com.example.security.firewall

import android.net.TrafficStats
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.SensitivityCategory
import com.example.security.malware.Risk
import java.util.Locale

enum class NetworkTelemetryStatus(val label: String) {
  OBSERVED("Datenverkehr beobachtet"),
  OS_RESTRICTED("Durch Android geschützt"),
  NO_NETWORK_CAPABILITY("Kein Internetrecht"),
}

data class AppNetworkActivityProfile(
  val hasInternetPermission: Boolean,
  val hasNetworkStatePermission: Boolean,
  val hasWifiStatePermission: Boolean,
  val declaresBootAutoStart: Boolean,
  val declaresBackgroundSync: Boolean,
  val usesCleartextTraffic: Boolean,
  val observedRxBytes: Long? = null,
  val observedTxBytes: Long? = null,
  val telemetryStatus: NetworkTelemetryStatus =
    when {
      !hasInternetPermission -> NetworkTelemetryStatus.NO_NETWORK_CAPABILITY
      observedRxBytes != null || observedTxBytes != null -> NetworkTelemetryStatus.OBSERVED
      else -> NetworkTelemetryStatus.OS_RESTRICTED
    },
) {
  val totalObservedBytes: Long?
    get() =
      if (observedRxBytes != null || observedTxBytes != null) {
        (observedRxBytes ?: 0L).coerceAtLeast(0L) + (observedTxBytes ?: 0L).coerceAtLeast(0L)
      } else {
        null
      }

  val hasAnyObservedTransfer: Boolean
    get() = (totalObservedBytes ?: 0L) > 0L

  val hasHighObservedTransfer: Boolean
    get() = (totalObservedBytes ?: 0L) >= HIGH_TRANSFER_THRESHOLD_BYTES

  val backgroundNetworkCapabilityCount: Int
    get() =
      listOf(
          declaresBootAutoStart,
          declaresBackgroundSync,
          hasNetworkStatePermission,
          hasWifiStatePermission,
          usesCleartextTraffic,
        )
        .count { it }

  val activityTierLabel: String
    get() =
      when {
        !hasInternetPermission -> "Offline / Kein Netzzugriff"
        hasHighObservedTransfer -> "Hoher Datentransfer beobachtet"
        hasAnyObservedTransfer -> "Aktiver Datentransfer beobachtet"
        declaresBootAutoStart || declaresBackgroundSync -> "Hintergrund-Netzbereitschaft"
        else -> "Standard-Netzbereitschaft"
      }

  val capabilityTags: List<String>
    get() =
      buildList {
        if (!hasInternetPermission) {
          add("Kein Internetrecht")
          return@buildList
        }
        add("Internetrecht")
        if (declaresBootAutoStart) add("Autostart nach Neustart")
        if (declaresBackgroundSync) add("Hintergrund-Sync / Aktivhalten")
        if (usesCleartextTraffic) add("Unverschlüsseltes HTTP erlaubt")
        if (hasWifiStatePermission) add("WLAN-Umfeld / Status")
        if (hasNetworkStatePermission) add("Netzwechsel-Erkennung")
        if (hasAnyObservedTransfer) add("Übertragung gemessen")
      }

  val formattedTransferSummary: String
    get() =
      when (telemetryStatus) {
        NetworkTelemetryStatus.NO_NETWORK_CAPABILITY ->
          "Kein Internetrecht angefordert — App kann regulär keine Netzverbindungen aufbauen."
        NetworkTelemetryStatus.OBSERVED -> {
          val rx = formatByteCount(observedRxBytes ?: 0L)
          val tx = formatByteCount(observedTxBytes ?: 0L)
          "Beobachtet seit Gerätestart: Empfang $rx · Versand $tx"
        }
        NetworkTelemetryStatus.OS_RESTRICTED ->
          "Live-Datenmengen anderer Apps sind durch Android geschützt — Bewertung anhand von Netzwerkrechten & Hintergrundaktivität."
      }

  companion object {
    const val HIGH_TRANSFER_THRESHOLD_BYTES = 1_048_576L

    fun formatByteCount(bytes: Long): String {
      val safe = bytes.coerceAtLeast(0L)
      return when {
        safe >= 1_073_741_824L ->
          String.format(Locale.GERMAN, "%.1f GB", safe / 1_073_741_824.0)
        safe >= 1_048_576L ->
          String.format(Locale.GERMAN, "%.1f MB", safe / 1_048_576.0)
        safe >= 1_024L ->
          String.format(Locale.GERMAN, "%.0f KB", safe / 1_024.0)
        else -> "$safe B"
      }
    }
  }
}

enum class RethinkRulePriority(val label: String) {
  CRITICAL("KRITISCH"),
  HIGH("HOCH"),
  MEDIUM("MITTEL"),
  OPTIONAL("OPTIONAL"),
}

enum class RethinkRuleType(
  val rethinkFlag: String,
  val shortBadge: String,
  val rethinkMenuPath: String,
) {
  BLOCK_ALL_NETWORKS(
    rethinkFlag = "block_both",
    shortBadge = "WLAN + MOBIL",
    rethinkMenuPath = "Rethink → Apps → App wählen → WLAN & Mobil blockieren",
  ),
  BLOCK_BACKGROUND(
    rethinkFlag = "block_background",
    shortBadge = "HINTERGRUND",
    rethinkMenuPath = "Rethink → Apps → App wählen → Hintergrund blockieren",
  ),
  BLOCK_SCREEN_OFF(
    rethinkFlag = "block_screen_off",
    shortBadge = "BILDSCHIRM AUS",
    rethinkMenuPath = "Rethink → Apps → App wählen → Bei Bildschirm-Aus blockieren",
  ),
  BLOCK_MOBILE_METERED(
    rethinkFlag = "block_metered",
    shortBadge = "NUR WLAN",
    rethinkMenuPath = "Rethink → Apps → App wählen → Mobile Daten blockieren",
  ),
  ENFORCE_DNS_AND_BLOCK_CLEARTEXT(
    rethinkFlag = "enforce_doh_dns",
    shortBadge = "DNS-FILTER",
    rethinkMenuPath = "Rethink → Apps → App wählen → DNS-Filter & Tracker-Schutz",
  ),
  ISOLATE_LAN(
    rethinkFlag = "block_lan",
    shortBadge = "LAN-ISOLATION",
    rethinkMenuPath = "Rethink → Apps → App wählen → Lokales Netzwerk (LAN) sperren",
  ),
  PREVENTIVE_ISOLATE(
    rethinkFlag = "preventive_isolate",
    shortBadge = "VORSORGE",
    rethinkMenuPath = "Rethink → Apps → App wählen → Vorsorglich blockieren",
  ),
}

data class RethinkFirewallRule(
  val id: String,
  val ruleType: RethinkRuleType,
  val title: String,
  val rethinkSettingPath: String,
  val rethinkCompatCode: String,
  val reason: String,
  val triggerSignals: List<String>,
  val priority: RethinkRulePriority,
  val recommendedEnabled: Boolean,
)

object RethinkFirewallRuleEngine {

  fun probeUidTraffic(uid: Int): Pair<Long?, Long?> {
    if (uid < 0) return null to null
    return try {
      val rx = TrafficStats.getUidRxBytes(uid)
      val tx = TrafficStats.getUidTxBytes(uid)
      val validRx = if (rx >= 0L) rx else null
      val validTx = if (tx >= 0L) tx else null
      validRx to validTx
    } catch (_: RuntimeException) {
      null to null
    } catch (_: LinkageError) {
      null to null
    }
  }

  fun buildNetworkActivityProfile(
    requestedPermissions: Set<String>,
    grantedPermissions: Set<String>,
    hasInternetPermission: Boolean,
    usesCleartextTraffic: Boolean = false,
    observedRxBytes: Long? = null,
    observedTxBytes: Long? = null,
  ): AppNetworkActivityProfile {
    val allPerms = requestedPermissions + grantedPermissions
    val internet = hasInternetPermission || "android.permission.INTERNET" in allPerms
    val hasNetState = "android.permission.ACCESS_NETWORK_STATE" in allPerms
    val hasWifiState =
      "android.permission.ACCESS_WIFI_STATE" in allPerms ||
        "android.permission.CHANGE_WIFI_STATE" in allPerms
    val bootAutoStart = "android.permission.RECEIVE_BOOT_COMPLETED" in allPerms
    val backgroundSync =
      "android.permission.FOREGROUND_SERVICE_DATA_SYNC" in allPerms ||
        "android.permission.FOREGROUND_SERVICE" in allPerms ||
        "android.permission.WAKE_LOCK" in allPerms

    val validRx = observedRxBytes?.takeIf { it >= 0L }
    val validTx = observedTxBytes?.takeIf { it >= 0L }

    return AppNetworkActivityProfile(
      hasInternetPermission = internet,
      hasNetworkStatePermission = hasNetState,
      hasWifiStatePermission = hasWifiState,
      declaresBootAutoStart = bootAutoStart,
      declaresBackgroundSync = backgroundSync,
      usesCleartextTraffic = usesCleartextTraffic,
      observedRxBytes = validRx,
      observedTxBytes = validTx,
      telemetryStatus =
        when {
          !internet -> NetworkTelemetryStatus.NO_NETWORK_CAPABILITY
          validRx != null || validTx != null -> NetworkTelemetryStatus.OBSERVED
          else -> NetworkTelemetryStatus.OS_RESTRICTED
        },
    )
  }

  fun suggestRules(
    app: InstalledAppPermissionInfo,
    networkActivity: AppNetworkActivityProfile,
    requestedPermissions: Set<String>,
    grantedPermissions: Set<String>,
    isSideloaded: Boolean,
    isDebuggable: Boolean,
    declaresAccessibilityService: Boolean,
    malwareRisk: Risk,
  ): List<RethinkFirewallRule> {
    if (!networkActivity.hasInternetPermission) {
      return listOf(
        RethinkFirewallRule(
          id = "rule_preventive_isolate",
          ruleType = RethinkRuleType.PREVENTIVE_ISOLATE,
          title = "Vorsorglich in Rethink sperren",
          rethinkSettingPath = RethinkRuleType.PREVENTIVE_ISOLATE.rethinkMenuPath,
          rethinkCompatCode = "rethink://rule/${app.packageName}/block_both",
          reason =
            "Diese App besitzt aktuell kein Internetrecht. Optional aktivierbar als Vorsorgeschutz für zukünftige Updates.",
          triggerSignals = listOf("Kein Internetrecht"),
          priority = RethinkRulePriority.OPTIONAL,
          recommendedEnabled = false,
        )
      )
    }

    val pkgLower = app.packageName.lowercase()
    val nameLower = app.appName.lowercase()
    val isCoreConnectivity =
      app.isSystemApp &&
        (pkgLower == "com.google.android.gms" ||
          pkgLower == "com.android.vending" ||
          pkgLower.contains("telephony") ||
          pkgLower.contains("ims") ||
          pkgLower.contains("captiveportallogin"))

    val looksOfflineUtility =
      !app.isSystemApp &&
        (pkgLower.contains("calc") ||
          pkgLower.contains("flashlight") ||
          pkgLower.contains("torch") ||
          pkgLower.contains("compass") ||
          pkgLower.contains("notes") ||
          nameLower.contains("taschenlampe") ||
          nameLower.contains("rechner"))

    val looksInteractiveComms =
      pkgLower.contains("whatsapp") ||
        pkgLower.contains("signal") ||
        pkgLower.contains("telegram") ||
        pkgLower.contains("messenger") ||
        pkgLower.contains("maps") ||
        pkgLower.contains("navigation")

    val sensitiveCategories = app.categories.filter { it.isHighSensitivity }
    val hasSensitiveBgExposure =
      app.hasBackgroundLocation ||
        app.criticalSensorTags.size >= 2 ||
        SensitivityCategory.CAMERA in app.categories ||
        SensitivityCategory.LOCATION in app.categories ||
        SensitivityCategory.MICROPHONE in app.categories ||
        SensitivityCategory.CONTACTS in app.categories ||
        SensitivityCategory.PHONE_SMS in app.categories

    val shouldBlockAll =
      !isCoreConnectivity &&
        (isDebuggable ||
          looksOfflineUtility ||
          (isSideloaded && (declaresAccessibilityService || malwareRisk >= Risk.MITTEL)))

    val rules = mutableListOf<RethinkFirewallRule>()

    // 1. Complete network isolation
    val blockAllTriggers =
      buildList {
        if (isDebuggable) add("Debug-Build")
        if (isSideloaded) add("Externe Quelle")
        if (declaresAccessibilityService) add("Bedienungshilfe")
        if (looksOfflineUtility) add("Offline-Werkzeug")
        if (networkActivity.hasHighObservedTransfer) add("Hoher Transfer")
      }.ifEmpty { listOf("Internetrecht") }

    rules.add(
      RethinkFirewallRule(
        id = "rule_block_all",
        ruleType = RethinkRuleType.BLOCK_ALL_NETWORKS,
        title = "Immer blockieren (WLAN & Mobil)",
        rethinkSettingPath = RethinkRuleType.BLOCK_ALL_NETWORKS.rethinkMenuPath,
        rethinkCompatCode = "rethink://rule/${app.packageName}/block_both",
        reason =
          when {
            isCoreConnectivity ->
              "Nicht empfohlen für System-Kernfunktionen (Push, Store oder Telefonie würden blockiert)."
            shouldBlockAll ->
              "Empfohlen: App fordert Online-Zugriff an, zeigt aber Prüfhinweise oder ist ein reines Offline-Werkzeug."
            else ->
              "Schaltet jeglichen Internetverkehr für diese App komplett ab — ideal, wenn du sie nur offline nutzt."
          },
        triggerSignals = blockAllTriggers,
        priority =
          when {
            shouldBlockAll -> RethinkRulePriority.CRITICAL
            else -> RethinkRulePriority.OPTIONAL
          },
        recommendedEnabled = shouldBlockAll,
      )
    )

    // 2. Background network restriction
    val recommendBlockBackground =
      !isCoreConnectivity &&
        (shouldBlockAll ||
          networkActivity.declaresBootAutoStart ||
          networkActivity.declaresBackgroundSync ||
          app.hasBackgroundLocation ||
          hasSensitiveBgExposure ||
          app.isSystemApp ||
          isSideloaded ||
          networkActivity.hasHighObservedTransfer)

    val bgTriggers =
      buildList {
        if (app.hasBackgroundLocation) add("Hintergrund-Standort")
        if (networkActivity.declaresBootAutoStart) add("Autostart")
        if (networkActivity.declaresBackgroundSync) add("Dauer-Sync")
        sensitiveCategories.take(3).forEach { add(it.label) }
        if (networkActivity.hasHighObservedTransfer) add("Hoher Transfer")
        if (app.isSystemApp && !isCoreConnectivity) add("System-Telemetrie")
      }.ifEmpty { listOf("Internetrecht") }

    rules.add(
      RethinkFirewallRule(
        id = "rule_block_background",
        ruleType = RethinkRuleType.BLOCK_BACKGROUND,
        title = "Hintergrunddaten blockieren",
        rethinkSettingPath = RethinkRuleType.BLOCK_BACKGROUND.rethinkMenuPath,
        rethinkCompatCode = "rethink://rule/${app.packageName}/block_background",
        reason =
          when {
            isCoreConnectivity ->
              "Für Kern-Systemdienste optional — Deaktivieren im Hintergrund kann Push-Nachrichten verzögern."
            recommendBlockBackground ->
              "Begrenzt Verbindungen auf aktive Nutzung im Vordergrund und verhindert stillen Datenaustausch sensibler Rechte."
            else ->
              "Erlaubt Verbindungen nur, solange die App sichtbar geöffnet ist."
          },
        triggerSignals = bgTriggers,
        priority =
          if (recommendBlockBackground) RethinkRulePriority.HIGH else RethinkRulePriority.MEDIUM,
        recommendedEnabled = recommendBlockBackground,
      )
    )

    // 3. Block when screen is off
    val recommendScreenOff =
      !isCoreConnectivity &&
        (networkActivity.declaresBackgroundSync ||
          networkActivity.declaresBootAutoStart ||
          app.isCriticalSensorApp ||
          networkActivity.hasAnyObservedTransfer ||
          shouldBlockAll)

    val screenOffTriggers =
      buildList {
        if (networkActivity.declaresBackgroundSync) add("Aktivhalten / Sync")
        if (networkActivity.declaresBootAutoStart) add("Autostart")
        if (app.isCriticalSensorApp) add("Sensoren")
        if (networkActivity.hasAnyObservedTransfer) add("Aktiver Transfer")
      }.ifEmpty { listOf("Standby-Schutz") }

    rules.add(
      RethinkFirewallRule(
        id = "rule_block_screen_off",
        ruleType = RethinkRuleType.BLOCK_SCREEN_OFF,
        title = "Bei ausgeschaltetem Bildschirm sperren",
        rethinkSettingPath = RethinkRuleType.BLOCK_SCREEN_OFF.rethinkMenuPath,
        rethinkCompatCode = "rethink://rule/${app.packageName}/block_screen_off",
        reason =
          if (recommendScreenOff) {
            "Unterbindet Standby-Verbindungen im gesperrten Zustand und schont zugleich den Akku."
          } else {
            "Optionale Standby-Sperre, um Verbindungen bei ausgeschaltetem Display sicher zu stoppen."
          },
        triggerSignals = screenOffTriggers,
        priority =
          if (recommendScreenOff) RethinkRulePriority.HIGH else RethinkRulePriority.OPTIONAL,
        recommendedEnabled = recommendScreenOff,
      )
    )

    // 4. Block metered / mobile data (Wi-Fi only)
    val hasHeavyMediaOrTransfer =
      SensitivityCategory.STORAGE_MEDIA in app.categories ||
        networkActivity.hasHighObservedTransfer ||
        (!isCoreConnectivity && !looksInteractiveComms && !app.isSystemApp)

    val recommendMobileBlock =
      !isCoreConnectivity && !looksInteractiveComms && (shouldBlockAll || hasHeavyMediaOrTransfer)

    val mobileTriggers =
      buildList {
        if (networkActivity.hasHighObservedTransfer) add("Hoher Transfer")
        if (SensitivityCategory.STORAGE_MEDIA in app.categories) add("Dateien & Medien")
        if (networkActivity.hasNetworkStatePermission) add("Netzwechsel")
      }.ifEmpty { listOf("Mobilfunk-Schutz") }

    rules.add(
      RethinkFirewallRule(
        id = "rule_block_mobile",
        ruleType = RethinkRuleType.BLOCK_MOBILE_METERED,
        title = "Mobile Daten sperren (Nur WLAN)",
        rethinkSettingPath = RethinkRuleType.BLOCK_MOBILE_METERED.rethinkMenuPath,
        rethinkCompatCode = "rethink://rule/${app.packageName}/block_metered",
        reason =
          if (recommendMobileBlock) {
            "Schützt unterwegs vor großem Datentransfer und unerwünschten Mobilfunk-Verbindungen."
          } else {
            "Optional aktivierbar, falls diese App unterwegs keine mobilen Daten verbrauchen soll."
          },
        triggerSignals = mobileTriggers,
        priority =
          if (networkActivity.hasHighObservedTransfer) RethinkRulePriority.HIGH
          else if (recommendMobileBlock) RethinkRulePriority.MEDIUM
          else RethinkRulePriority.OPTIONAL,
        recommendedEnabled = recommendMobileBlock,
      )
    )

    // 5. Encrypted DNS & Cleartext defense
    val dnsTriggers =
      buildList {
        if (networkActivity.usesCleartextTraffic) add("Unverschlüsseltes HTTP")
        if (networkActivity.hasInternetPermission) add("Internetrecht")
        if (networkActivity.hasAnyObservedTransfer) add("Aktiver Transfer")
      }
    rules.add(
      RethinkFirewallRule(
        id = "rule_enforce_dns",
        ruleType = RethinkRuleType.ENFORCE_DNS_AND_BLOCK_CLEARTEXT,
        title = "Verschlüsseltes DNS & Tracker-Filter erzwingen",
        rethinkSettingPath = RethinkRuleType.ENFORCE_DNS_AND_BLOCK_CLEARTEXT.rethinkMenuPath,
        rethinkCompatCode = "rethink://rule/${app.packageName}/enforce_doh_dns",
        reason =
          if (networkActivity.usesCleartextTraffic) {
            "App erlaubt unverschlüsselte HTTP-Verbindungen — filtert Tracker-Domains und schützt Namensauflösungen über Rethink-DNS."
          } else {
            "Filtert bekannte Tracking- und Werbedomains dieser App ohne die reguläre Nutzung zu blockieren."
          },
        triggerSignals = dnsTriggers,
        priority =
          if (networkActivity.usesCleartextTraffic) RethinkRulePriority.HIGH
          else RethinkRulePriority.MEDIUM,
        recommendedEnabled = true,
      )
    )

    // 6. Local LAN isolation when Wi-Fi/Nearby/Risk signals exist
    val hasLanSurface =
      networkActivity.hasWifiStatePermission ||
        SensitivityCategory.NEARBY in app.categories ||
        "android.permission.CHANGE_WIFI_STATE" in requestedPermissions ||
        "android.permission.CHANGE_WIFI_STATE" in grantedPermissions ||
        isSideloaded ||
        isDebuggable

    if (hasLanSurface && !isCoreConnectivity) {
      val recommendLanBlock = isSideloaded || isDebuggable || shouldBlockAll
      val lanTriggers =
        buildList {
          if (networkActivity.hasWifiStatePermission) add("WLAN-Status")
          if (SensitivityCategory.NEARBY in app.categories) add("Geräte in der Nähe")
          if (isSideloaded) add("Externe Quelle")
        }.ifEmpty { listOf("Heimnetz-Schutz") }

      rules.add(
        RethinkFirewallRule(
          id = "rule_isolate_lan",
          ruleType = RethinkRuleType.ISOLATE_LAN,
          title = "Lokales Netzwerk (LAN) isolieren",
          rethinkSettingPath = RethinkRuleType.ISOLATE_LAN.rethinkMenuPath,
          rethinkCompatCode = "rethink://rule/${app.packageName}/block_lan",
          reason =
            "Verhindert Zugriffe auf lokale Geräte im Heimnetz (Router, Drucker, NAS) durch diese App.",
          triggerSignals = lanTriggers,
          priority =
            if (recommendLanBlock) RethinkRulePriority.HIGH else RethinkRulePriority.OPTIONAL,
          recommendedEnabled = recommendLanBlock,
        )
      )
    }

    return rules
  }

  fun resolveEffectiveStates(
    rules: List<RethinkFirewallRule>,
    overrides: Map<String, Boolean>,
  ): Map<String, Boolean> =
    rules.associate { rule -> rule.id to (overrides[rule.id] ?: rule.recommendedEnabled) }

  fun formatRethinkRuleSummary(
    app: InstalledAppPermissionInfo,
    rules: List<RethinkFirewallRule>,
    overrides: Map<String, Boolean>,
  ): String {
    val effective = resolveEffectiveStates(rules, overrides)
    val activeRules = rules.filter { effective[it.id] == true }
    return buildString {
      appendLine("Rethink-Firewall-Regeln für ${app.appName} (${app.packageName})")
      if (activeRules.isEmpty()) {
        appendLine("• Keine Sperrregeln ausgewählt (Standardzugriff)")
      } else {
        activeRules.forEach { rule ->
          appendLine("• [AKTIV] ${rule.title} (${rule.ruleType.rethinkFlag}) — ${rule.rethinkSettingPath}")
        }
      }
    }.trimEnd()
  }
}
