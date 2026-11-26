package com.example.security.firewall

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.SensitivityCategory
import com.example.security.malware.AppPurpose
import com.example.security.malware.Indicator
import com.example.security.malware.LocalMalwareDiagnosticEngine
import com.example.security.malware.Request
import com.example.security.malware.Risk
import com.example.security.malware.Verdict
import java.security.MessageDigest

enum class FirewallAction(
  val badge: String,
  val title: String,
) {
  BLOCK_ALL("BLOCKIEREN", "Komplett blockieren"),
  ALLOW_FOREGROUND_ONLY("NUR VORDERGRUND", "Hintergrunddaten blockieren"),
  ALLOW_WIFI_ONLY("NUR WLAN", "Mobile Hintergrunddaten sparen/sperren"),
  ALLOW_RESTRICTED("EINGESCHRÄNKT", "System-/Kernfunktion prüfen"),
  NO_NETWORK_PERMISSION("KEIN NETZ", "Kein Internetrecht im Manifest"),
}

data class FirewallRecommendation(
  val action: FirewallAction,
  val rethinkRuleHint: String,
  val reason: String,
)

enum class TamperSignalSeverity {
  INFO,
  WARN,
  CRITICAL,
}

data class TamperSignal(
  val id: String,
  val label: String,
  val detail: String,
  val severity: TamperSignalSeverity,
)

data class AppSecurityPosture(
  val installerSourceLabel: String,
  val isSideloadedOrUnknown: Boolean,
  val isDebuggable: Boolean,
  val isTestOnly: Boolean,
  val lastUpdateTimeMillis: Long,
  val recentlyUpdatedWithin7Days: Boolean,
  val signerCount: Int,
  val signerSha256Prefix: String?,
  val hasInternetPermission: Boolean,
  val declaresAccessibilityService: Boolean,
  val tamperSignals: List<TamperSignal>,
  val malwareVerdict: Verdict,
  val firewallRecommendation: FirewallRecommendation,
  val networkActivityProfile: AppNetworkActivityProfile =
    AppNetworkActivityProfile(
      hasInternetPermission = hasInternetPermission,
      hasNetworkStatePermission = false,
      hasWifiStatePermission = false,
      declaresBootAutoStart = false,
      declaresBackgroundSync = false,
      usesCleartextTraffic = false,
    ),
  val rethinkFirewallRules: List<RethinkFirewallRule> = emptyList(),
) {
  val hasManipulationWarnings: Boolean
    get() = tamperSignals.any { it.severity != TamperSignalSeverity.INFO }

  val highestTamperSeverity: TamperSignalSeverity
    get() =
      when {
        tamperSignals.any { it.severity == TamperSignalSeverity.CRITICAL } ->
          TamperSignalSeverity.CRITICAL
        tamperSignals.any { it.severity == TamperSignalSeverity.WARN } -> TamperSignalSeverity.WARN
        else -> TamperSignalSeverity.INFO
      }
}

enum class RethinkProfile(
  val title: String,
  val subtitle: String,
) {
  EVERYDAY(
    title = "Alltag (Ausgewogen)",
    subtitle = "Schützt vor Trackern ohne Banking-, Push- oder Login-Abbrüche.",
  ),
  STRICT_PRIVACY(
    title = "Strenger Datenschutz",
    subtitle = "Blockiert Hintergrund-Telemetrie und unerwartete Systemverbindungen.",
  ),
  LOCKDOWN(
    title = "Notfall / Verdachtsfall",
    subtitle = "Maximale Abschottung bei verdächtigen oder manipulierten Apps.",
  ),
}

enum class RethinkSettingCategory(val label: String) {
  FIREWALL("Firewall"),
  DNS("DNS"),
  NETWORK("Netzwerk & VPN"),
}

data class RethinkSettingRecommendation(
  val id: String,
  val category: RethinkSettingCategory,
  val title: String,
  val settingPath: String,
  val recommendedValue: String,
  val plainExplanation: String,
  val cautionNote: String? = null,
  val priorityLabel: String,
)

object AppProtectionAnalyzer {

  private const val SEVEN_DAYS_MILLIS = 7L * 24L * 60L * 60L * 1000L

  fun inspectPackagePosture(
    pm: PackageManager,
    pkg: PackageInfo,
    appInfo: InstalledAppPermissionInfo,
    nowMillis: Long = System.currentTimeMillis(),
  ): AppSecurityPosture {
    val applicationInfo = pkg.applicationInfo
    val flags = applicationInfo?.flags ?: 0
    val isDebuggable = (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val isTestOnly = (flags and ApplicationInfo.FLAG_TEST_ONLY) != 0
    val usesCleartextTraffic = (flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
    val (observedRxBytes, observedTxBytes) =
      RethinkFirewallRuleEngine.probeUidTraffic(applicationInfo?.uid ?: -1)
    val installerPackage = readInstallerPackage(pm, pkg.packageName)
    val requestedPerms = pkg.requestedPermissions?.toSet().orEmpty()
    val hasInternet = "android.permission.INTERNET" in requestedPerms
    val declaresAccessibilityService =
      hasAccessibilityDeclaration(pm, pkg.packageName, requestedPerms)
    val signerDigestPrefixes = readSignerSha256Prefixes(pm, pkg.packageName)

    return analyzeMetadata(
      app = appInfo,
      requestedPermissions = requestedPerms,
      grantedPermissions = appInfo.grantedDangerousPermissions.mapTo(mutableSetOf()) { it.fullName },
      installerPackage = installerPackage,
      isDebuggable = isDebuggable,
      isTestOnly = isTestOnly,
      lastUpdateTimeMillis = pkg.lastUpdateTime,
      signerSha256Prefixes = signerDigestPrefixes,
      hasInternetPermission = hasInternet,
      declaresAccessibilityService = declaresAccessibilityService,
      nowMillis = nowMillis,
      usesCleartextTraffic = usesCleartextTraffic,
      observedRxBytes = observedRxBytes,
      observedTxBytes = observedTxBytes,
    )
  }

  /**
   * Pure deterministic inspection & recommendation builder used by both PackageManager scans
   * and JVM unit tests. Fails closed: never claims proven malware without verified runtime trace.
   */
  fun analyzeMetadata(
    app: InstalledAppPermissionInfo,
    requestedPermissions: Set<String>,
    grantedPermissions: Set<String>,
    installerPackage: String?,
    isDebuggable: Boolean,
    isTestOnly: Boolean,
    lastUpdateTimeMillis: Long,
    signerSha256Prefixes: List<String>,
    hasInternetPermission: Boolean,
    declaresAccessibilityService: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
    usesCleartextTraffic: Boolean = false,
    observedRxBytes: Long? = null,
    observedTxBytes: Long? = null,
  ): AppSecurityPosture {
    val installerLabel = classifyInstallerLabel(installerPackage, app.isSystemApp)
    val isSideloaded =
      !app.isSystemApp &&
        installerPackage != "com.android.vending" &&
        installerPackage != "com.sec.android.app.samsungapps" &&
        installerPackage != "com.amazon.venezia"

    val recentlyUpdated =
      lastUpdateTimeMillis > 0L && (nowMillis - lastUpdateTimeMillis) in 0L..SEVEN_DAYS_MILLIS

    val signals =
      buildList {
        if (isDebuggable) {
          add(
            TamperSignal(
              id = "debuggable_build",
              label = "Debug-Modus aktiv (FLAG_DEBUGGABLE)",
              detail =
                "App erlaubt Debugging und Speicherinspektion. Bei produktiven Store-Apps ungewöhnlich.",
              severity = TamperSignalSeverity.CRITICAL,
            )
          )
        }
        if (isTestOnly) {
          add(
            TamperSignal(
              id = "test_only_build",
              label = "Test-Build-Markierung (FLAG_TEST_ONLY)",
              detail = "Markiert als Entwickler-/Testpaket außerhalb regulärer Releases.",
              severity = TamperSignalSeverity.WARN,
            )
          )
        }
        if (isSideloaded) {
          add(
            TamperSignal(
              id = "sideload_installer",
              label = "Quelle außerhalb Play Store ($installerLabel)",
              detail =
                "Nicht über Google Play installiert. Prüfe Herkunft und Signatur besonders sorgfältig.",
              severity = TamperSignalSeverity.WARN,
            )
          )
        }
        if ("android.permission.REQUEST_INSTALL_PACKAGES" in requestedPermissions && !app.isSystemApp) {
          add(
            TamperSignal(
              id = "request_install_packages",
              label = "Darf weitere APKs zur Installation anstoßen",
              detail =
                "Angefordertes Recht REQUEST_INSTALL_PACKAGES (typisches Risiko bei Dropper-/Sideload-Pfaden).",
              severity = TamperSignalSeverity.WARN,
            )
          )
        }
        if ("android.permission.SYSTEM_ALERT_WINDOW" in requestedPermissions && !app.isSystemApp) {
          add(
            TamperSignal(
              id = "system_alert_window",
              label = "Darf Überlagerungen (Overlays) anzeigen",
              detail =
                "SYSTEM_ALERT_WINDOW kann über anderen Apps liegen (Tapjacking-Risiko prüfen).",
              severity = TamperSignalSeverity.WARN,
            )
          )
        }
        if (declaresAccessibilityService && !app.isSystemApp) {
          add(
            TamperSignal(
              id = "accessibility_service",
              label = "Deklariert Bedienungshilfe (AccessibilityService)",
              detail =
                "Kann potenziell Bildschirminhalte lesen/steuern. Nur vertrauenswürdigen Hilfs-Apps erlauben.",
              severity = TamperSignalSeverity.CRITICAL,
            )
          )
        }
        if (signerSha256Prefixes.size > 1) {
          add(
            TamperSignal(
              id = "multiple_signers",
              label = "Mehrfache Signaturzertifikate (${signerSha256Prefixes.size})",
              detail = "Signaturhistorie oder Multi-Signer prüfen.",
              severity = TamperSignalSeverity.INFO,
            )
          )
        }
        if (recentlyUpdated) {
          add(
            TamperSignal(
              id = "recent_update",
              label = "Kürzlich installiert/aktualisiert (< 7 Tage)",
              detail = "Änderung in den letzten 7 Tagen beobachtet — gut für Änderungskontrolle.",
              severity = TamperSignalSeverity.INFO,
            )
          )
        }
      }

    val indicators =
      buildSet {
        if ("android.permission.READ_SMS" in grantedPermissions) add(Indicator.SMS_FLOW)
        if ("android.permission.READ_CONTACTS" in grantedPermissions) add(Indicator.CONTACT_FLOW)
        if (declaresAccessibilityService) add(Indicator.ACCESSIBILITY_CONTROL)
      }

    val safePackageName =
      if (app.packageName.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))) {
        app.packageName
      } else {
        "local.unknown.pkg"
      }
    val safeVersion =
      (app.versionName ?: "unknown")
        .filter { !Character.isISOControl(it) }
        .take(64)
        .ifBlank { "unknown" }

    val verdict =
      LocalMalwareDiagnosticEngine.diagnose(
        Request(
          packageName = safePackageName,
          appVersion = safeVersion,
          purpose = inferAppPurpose(app),
          userExpectedFeature = null,
          grantedPermissions = grantedPermissions,
          declaresAccessibilityService = declaresAccessibilityService,
          indicators = indicators,
          facts = emptyList(),
          collectedAtMs = nowMillis.coerceAtLeast(0L),
        )
      )

    val networkProfile =
      RethinkFirewallRuleEngine.buildNetworkActivityProfile(
        requestedPermissions = requestedPermissions,
        grantedPermissions = grantedPermissions,
        hasInternetPermission = hasInternetPermission,
        usesCleartextTraffic = usesCleartextTraffic,
        observedRxBytes = observedRxBytes,
        observedTxBytes = observedTxBytes,
      )

    val rethinkRules =
      RethinkFirewallRuleEngine.suggestRules(
        app = app,
        networkActivity = networkProfile,
        requestedPermissions = requestedPermissions,
        grantedPermissions = grantedPermissions,
        isSideloaded = isSideloaded,
        isDebuggable = isDebuggable,
        declaresAccessibilityService = declaresAccessibilityService,
        malwareRisk = verdict.risk,
      )

    val firewall =
      recommendFirewallRule(
        app = app,
        hasInternetPermission = networkProfile.hasInternetPermission,
        isSideloaded = isSideloaded,
        isDebuggable = isDebuggable,
        declaresAccessibilityService = declaresAccessibilityService,
        verdict = verdict,
      )

    return AppSecurityPosture(
      installerSourceLabel = installerLabel,
      isSideloadedOrUnknown = isSideloaded,
      isDebuggable = isDebuggable,
      isTestOnly = isTestOnly,
      lastUpdateTimeMillis = lastUpdateTimeMillis,
      recentlyUpdatedWithin7Days = recentlyUpdated,
      signerCount = signerSha256Prefixes.size,
      signerSha256Prefix = signerSha256Prefixes.firstOrNull(),
      hasInternetPermission = networkProfile.hasInternetPermission,
      declaresAccessibilityService = declaresAccessibilityService,
      tamperSignals = signals,
      malwareVerdict = verdict,
      firewallRecommendation = firewall,
      networkActivityProfile = networkProfile,
      rethinkFirewallRules = rethinkRules,
    )
  }

  private fun recommendFirewallRule(
    app: InstalledAppPermissionInfo,
    hasInternetPermission: Boolean,
    isSideloaded: Boolean,
    isDebuggable: Boolean,
    declaresAccessibilityService: Boolean,
    verdict: Verdict,
  ): FirewallRecommendation {
    if (!hasInternetPermission) {
      return FirewallRecommendation(
        action = FirewallAction.NO_NETWORK_PERMISSION,
        rethinkRuleHint = "Rethink: Keine Regel nötig (kein INTERNET-Recht).",
        reason = "Die App fordert kein Netzwerk-/Internetrecht an und kann keine Online-Verbindungen aufbauen.",
      )
    }

    val pkgLower = app.packageName.lowercase()
    val nameLower = app.appName.lowercase()
    val looksOfflineUtility =
      !app.isSystemApp &&
        (pkgLower.contains("calc") ||
          pkgLower.contains("flashlight") ||
          pkgLower.contains("torch") ||
          pkgLower.contains("compass") ||
          pkgLower.contains("notes") ||
          nameLower.contains("taschenlampe") ||
          nameLower.contains("rechner"))

    if (isDebuggable || (isSideloaded && (declaresAccessibilityService || verdict.risk >= Risk.MITTEL))) {
      return FirewallRecommendation(
        action = FirewallAction.BLOCK_ALL,
        rethinkRuleHint = "Rethink → Apps → \"Immer blockieren\" (WLAN + Mobil)",
        reason =
          "Auffällige statische Merkmale (z. B. Debug-Build, Sideload oder Eingriffshilfe). Netzverkehr vorsorglich komplett blockieren, bis Herkunft geklärt ist.",
      )
    }

    if (looksOfflineUtility) {
      return FirewallRecommendation(
        action = FirewallAction.BLOCK_ALL,
        rethinkRuleHint = "Rethink → Apps → \"Immer blockieren\"",
        reason =
          "Offline-Werkzeug benötigt für seine Kernfunktion meist kein Internet. Blockieren verhindert ungewollte Telemetrie.",
      )
    }

    if (app.isSystemApp) {
      val coreConnectivityPackage =
        pkgLower == "com.google.android.gms" ||
          pkgLower == "com.android.vending" ||
          pkgLower.contains("telephony") ||
          pkgLower.contains("ims") ||
          pkgLower.contains("captiveportallogin")
      return if (coreConnectivityPackage) {
        FirewallRecommendation(
          action = FirewallAction.ALLOW_RESTRICTED,
          rethinkRuleHint = "Rethink → Apps → \"Erlauben (nicht isolieren)\"",
          reason =
            "System-Kernkomponente für Push, Store oder Netzprüfung. Blockieren kann Anrufe, WLAN-Logins oder Sicherheitsupdates stören.",
        )
      } else {
        FirewallRecommendation(
          action = FirewallAction.ALLOW_FOREGROUND_ONLY,
          rethinkRuleHint = "Rethink → Apps → \"Hintergrunddaten blockieren\"",
          reason =
            "System- oder Hersteller-App mit Internetrecht: Hintergrund-Telemetrie stoppen, Vordergrundnutzung bei Bedarf erlauben.",
        )
      }
    }

    val hasSensitiveBgExposure =
      app.hasBackgroundLocation ||
        app.criticalSensorTags.size >= 2 ||
        SensitivityCategory.CAMERA in app.categories ||
        SensitivityCategory.LOCATION in app.categories ||
        SensitivityCategory.MICROPHONE in app.categories

    if (hasSensitiveBgExposure || isSideloaded) {
      return FirewallRecommendation(
        action = FirewallAction.ALLOW_FOREGROUND_ONLY,
        rethinkRuleHint = "Rethink → Apps → \"Hintergrund blockieren\" + \"Bildschirm aus blockieren\"",
        reason =
          "Sensible Rechte (Kamera, Standort oder Mikrofon) oder Sideload-Herkunft: Datenverkehr auf aktive Vordergrund-Nutzung begrenzen.",
      )
    }

    return FirewallRecommendation(
      action = FirewallAction.ALLOW_WIFI_ONLY,
      rethinkRuleHint = "Rethink → Apps → \"Nur WLAN\" (oder bei Bedarf erlauben)",
      reason =
        "Keine auffälligen Manipulationssignale. Mobile Hintergrunddaten sparen und DNS-Tracker-Filter über Rethink aktiv lassen.",
    )
  }

  fun rethinkRecommendationsFor(
    profile: RethinkProfile,
    scanResult: InstalledAppsScanResult?,
  ): List<RethinkSettingRecommendation> {
    val blockCount =
      scanResult?.apps?.count {
        it.securityPosture?.firewallRecommendation?.action == FirewallAction.BLOCK_ALL
      } ?: 0
    val fgCount =
      scanResult?.apps?.count {
        it.securityPosture?.firewallRecommendation?.action == FirewallAction.ALLOW_FOREGROUND_ONLY
      } ?: 0
    val tamperCount =
      scanResult?.apps?.count { it.securityPosture?.hasManipulationWarnings == true } ?: 0

    return buildList {
      add(
        RethinkSettingRecommendation(
          id = "fw_screen_off",
          category = RethinkSettingCategory.FIREWALL,
          title = "Verbindungen bei ausgeschaltetem Bildschirm blockieren",
          settingPath = "Rethink → Konfigurieren → Firewall → Universal",
          recommendedValue =
            when (profile) {
              RethinkProfile.EVERYDAY -> "Aktiv (Messenger ggf. ausnehmen)"
              RethinkProfile.STRICT_PRIVACY, RethinkProfile.LOCKDOWN -> "Strikt aktiv für alle Nutzer-Apps"
            },
          plainExplanation =
            if (fgCount > 0) {
              "Verhindert heimliche Hintergrund-Uploads im Standby. Aktuell betrifft dies u. a. $fgCount App(s) auf deinem Gerät mit Hintergrund-/Sensoren-Hinweis."
            } else {
              "Stoppt unerwartete Tracker- und Hintergrundverbindungen, sobald das Display ausgeschaltet ist."
            },
          cautionNote = "Push-Nachrichten wichtiger Messenger kommen verzögert an, wenn du sie nicht ausnimmst.",
          priorityLabel = "HOCH",
        )
      )
      add(
        RethinkSettingRecommendation(
          id = "fw_block_newly_installed",
          category = RethinkSettingCategory.FIREWALL,
          title = "Neu installierte Apps automatisch blockieren",
          settingPath = "Rethink → Konfigurieren → Firewall → Regeln",
          recommendedValue = "EIN (Zuerst prüfen, dann freigeben)",
          plainExplanation =
            "Neue Apps oder unbekannte Nachinstallationen können beim ersten Start keine Daten senden, bevor du sie manuell freischaltest.",
          priorityLabel = "HOCH",
        )
      )
      add(
        RethinkSettingRecommendation(
          id = "fw_block_flagged",
          category = RethinkSettingCategory.FIREWALL,
          title = "Verdächtige & Offline-Apps sperren",
          settingPath = "Rethink → Apps → Filter: Erlaubt / System",
          recommendedValue =
            if (blockCount > 0 || tamperCount > 0) {
              "$blockCount App(s) blockieren ($tamperCount mit Prüfhinweis)"
            } else {
              "Offline-Tools & unbenötigte Hersteller-Apps auf \"Immer blockieren\""
            },
          plainExplanation =
            "Apps ohne nötige Online-Funktion (z. B. Rechner, Offline-Notizen) sowie Pakete mit Debug-/Sideload-Warnungen vollständig vom Netz trennen.",
          priorityLabel = if (blockCount > 0) "KRITISCH" else "MITTEL",
        )
      )
      add(
        RethinkSettingRecommendation(
          id = "dns_doh_rdns",
          category = RethinkSettingCategory.DNS,
          title = "Verschlüsseltes DNS (DoH) mit Tracker-Blockliste",
          settingPath = "Rethink → Konfigurieren → DNS → DNS über HTTPS",
          recommendedValue =
            when (profile) {
              RethinkProfile.EVERYDAY -> "Rethink DNS / Quad9 / Mullvad (Basis-Trackerliste)"
              RethinkProfile.STRICT_PRIVACY -> "RDNS Plus mit Hagezi Multi + Threat Intelligence"
              RethinkProfile.LOCKDOWN -> "Maximal restriktive Blockliste + Neu-Domains sperren"
            },
          plainExplanation =
            "Verschlüsselt alle Namensauflösungen gegen Mitlesen im WLAN/Mobilfunk und filtert bekannte Werbe-, Tracking- und Phishing-Domains schon vor dem Verbindungsaufbau.",
          cautionNote = "Zu aggressive Listen können Shopping-Links oder Captchas blockieren – ggf. Basisliste wählen.",
          priorityLabel = "HOCH",
        )
      )
      add(
        RethinkSettingRecommendation(
          id = "dns_no_bypass_fallback",
          category = RethinkSettingCategory.DNS,
          title = "System-/Provider-DNS-Fallback verhindern",
          settingPath = "Rethink → Konfigurieren → DNS → Erweitert",
          recommendedValue =
            "System-DNS-Umgehung AUS · Private-DNS in Android auf \"Automatisch\" (damit Rethink filtert)",
          plainExplanation =
            "Stellt sicher, dass Apps keine Klartext-DNS-Anfragen am Rethink-Tunnel vorbeischicken.",
          priorityLabel = "MITTEL",
        )
      )
      add(
        RethinkSettingRecommendation(
          id = "net_always_on_lockdown",
          category = RethinkSettingCategory.NETWORK,
          title = "Android Always-on VPN & Kill-Switch prüfen",
          settingPath = "Android-Einstellungen → Netzwerk & Internet → VPN → Rethink",
          recommendedValue =
            when (profile) {
              RethinkProfile.EVERYDAY -> "Immer aktives VPN EIN (Blockieren optional bei WLAN-Wechsel)"
              RethinkProfile.STRICT_PRIVACY, RethinkProfile.LOCKDOWN ->
                "Immer aktives VPN EIN + Verbindungen ohne VPN blockieren EIN"
            },
          plainExplanation =
            "Verhindert Datenlecks beim Einschalten, App-Neustart oder während Netzwechseln. Keine App kommt am lokalen VPN-Schutz vorbei.",
          cautionNote = "Öffentliche Hotel-/Zug-WLAN-Anmeldeseiten benötigen kurzzeitig eine Ausnahme (Captive-Portal-Bypass).",
          priorityLabel = "HOCH",
        )
      )
      add(
        RethinkSettingRecommendation(
          id = "net_bypass_unmetered_split",
          category = RethinkSettingCategory.NETWORK,
          title = "Netzwerk-Bypass & UDP-/LAN-Schutz richtig trennen",
          settingPath = "Rethink → Konfigurieren → Netzwerk",
          recommendedValue =
            when (profile) {
              RethinkProfile.LOCKDOWN -> "LAN-Zugriff sperren · Kein App-Bypass"
              else -> "Lokales Netzwerk (Drucker/Router) erlauben · Rest über Firewall leiten"
            },
          plainExplanation =
            "Hält lokale Heimgeräte (Router, NAS, Drucker) erreichbar, während Internetverkehr weiter durch Firewall- und DNS-Regeln geprüft wird.",
          priorityLabel = "NIEDRIG",
        )
      )
    }
  }

  private fun inferAppPurpose(app: InstalledAppPermissionInfo): AppPurpose {
    val pkg = app.packageName.lowercase()
    return when {
      pkg.contains("flashlight") || pkg.contains("torch") -> AppPurpose.FLASHLIGHT
      pkg.contains("messaging") || pkg.contains("whatsapp") || pkg.contains("signal") ->
        AppPurpose.MESSENGER
      pkg.contains("backup") -> AppPurpose.BACKUP
      else -> AppPurpose.UNKNOWN
    }
  }

  private fun classifyInstallerLabel(installerPackage: String?, isSystemApp: Boolean): String =
    when {
      installerPackage == "com.android.vending" -> "Google Play Store"
      installerPackage == "com.sec.android.app.samsungapps" -> "Samsung Galaxy Store"
      installerPackage == "com.google.android.packageinstaller" ||
        installerPackage == "com.android.packageinstaller" -> "Paketinstaller (Sideload)"
      installerPackage == "com.android.shell" -> "ADB / Shell"
      !installerPackage.isNullOrBlank() -> installerPackage
      isSystemApp -> "Vorinstalliert / System-Image"
      else -> "Unbekannt / Direkt-APK"
    }

  @Suppress("DEPRECATION")
  private fun readInstallerPackage(pm: PackageManager, packageName: String): String? =
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        pm.getInstallSourceInfo(packageName).installingPackageName
      } else {
        pm.getInstallerPackageName(packageName)
      }
    } catch (_: PackageManager.NameNotFoundException) {
      null
    } catch (_: RuntimeException) {
      null
    }

  @Suppress("DEPRECATION")
  private fun hasAccessibilityDeclaration(
    pm: PackageManager,
    packageName: String,
    requestedPerms: Set<String>,
  ): Boolean {
    if ("android.permission.BIND_ACCESSIBILITY_SERVICE" in requestedPerms) return true
    return try {
      val info =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_SERVICES.toLong()),
          )
        } else {
          pm.getPackageInfo(packageName, PackageManager.GET_SERVICES)
        }
      info.services?.any { it.permission == "android.permission.BIND_ACCESSIBILITY_SERVICE" } == true
    } catch (_: PackageManager.NameNotFoundException) {
      false
    } catch (_: RuntimeException) {
      false
    }
  }

  @Suppress("DEPRECATION")
  private fun readSignerSha256Prefixes(pm: PackageManager, packageName: String): List<String> =
    try {
      val signatures =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val pkg =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              pm.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
              )
            } else {
              pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            }
          val signingInfo = pkg.signingInfo
          when {
            signingInfo == null -> emptyArray()
            signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
            else -> signingInfo.signingCertificateHistory
          }
        } else {
          pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        }
      signatures.orEmpty().mapNotNull { sig ->
        val digest = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
        digest.take(6).joinToString("") { "%02X".format(it) }
      }
    } catch (_: PackageManager.NameNotFoundException) {
      emptyList()
    } catch (_: RuntimeException) {
      emptyList()
    }
}
