package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Process
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.evidence.Availability
import com.example.core.evidence.DeviceEvidence
import com.example.core.evidence.ObservationContract
import com.example.core.evidence.RiskLevel
import com.example.core.evidence.SyntheticFixtureOracle
import com.example.core.quality.CheckStatus
import com.example.core.quality.QualityCheck
import com.example.core.quality.QualityPipeline
import com.example.core.quality.QualityReport
import com.example.core.quality.StatisticalValidation
import com.example.core.scoring.DeviceAssessment
import com.example.core.scoring.DeviceModel
import com.example.core.scoring.DeviceScoringDispatcher
import com.example.core.scoring.StaticDeviceModelRegistry
import com.example.data.DesignPreferences
import com.example.data.DesignPreferencesRepository
import com.example.devicecare.AndroidSystemSecurityReader
import com.example.devicecare.InspectionResult
import com.example.devicecare.SystemSecuritySnapshot
import com.example.devicecare.collectSystemHealthSnapshot
import com.example.devicecare.presentInspection
import com.example.features.battery.AndroidBatteryCollector
import com.example.features.battery.AndroidBatteryHealthMonitor
import com.example.features.battery.BatteryDashboardSnapshot
import com.example.features.battery.BatteryTrendPoint
import com.example.features.battery.BatteryTrendRecorder
import com.example.features.battery.ChargingStatus
import com.example.features.battery.LowBatteryAlertEvaluator
import com.example.features.battery.LowBatteryAlertNotifier
import com.example.features.permissions.AndroidPermissionCollector
import com.example.features.permissions.AndroidPermissionScanProfile
import com.example.features.permissions.AppOriginFilter
import com.example.features.permissions.AppPermissionFilter
import com.example.features.permissions.AppSectionCategory
import com.example.features.permissions.CategorizedAppGroup
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppPermissionInspector
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.PermissionScanBudget
import com.example.features.permissions.PermissionSensitivityClassifier
import com.example.features.permissions.SensitivityCategory
import com.example.security.firewall.FirewallAction
import com.example.model.CommercialTierPlan
import com.example.model.DeviceCareRoadmapData
import com.example.model.GovernanceStatus
import com.example.model.GradleModuleSpec
import com.example.model.PlanCategory
import com.example.model.RoadmapTask
import com.example.model.SafeguardRule
import com.example.ui.theme.CornerStyle
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ArchitectureTab(val label: String) {
  PIPELINES("Pipelines & QA-Labor"),
  SAFEGUARDS("10 Schutzregeln"),
  TIERS_MODULES("Module & Stufen"),
}

data class DeviceCareUiState(
  val preferences: DesignPreferences = DesignPreferences(),
  val inspectionInProgress: Boolean = false,
  val inspectionResult: InspectionResult? = null,
  val isInspectionStale: Boolean = false,
  val settingsMessage: String? = null,
  val searchQuery: String = "",
  val selectedCategory: PlanCategory = PlanCategory.ALL,
  val selectedStatusFilter: GovernanceStatus? = null,
  val allTasks: List<RoadmapTask> = DeviceCareRoadmapData.tasks,
  val filteredTasks: List<RoadmapTask> = DeviceCareRoadmapData.tasks,
  val safeguards: List<SafeguardRule> = DeviceCareRoadmapData.safeguards,
  val gradleModules: List<GradleModuleSpec> = DeviceCareRoadmapData.gradleModules,
  val tierPlans: List<CommercialTierPlan> = DeviceCareRoadmapData.tierPlans,
  val selectedArchitectureTab: ArchitectureTab = ArchitectureTab.PIPELINES,
  val enterprisePreviewMode: Boolean = false,
  // Interactive Pipeline Sandbox states
  val qualitySimulateShaPresent: Boolean = false,
  val qualityReport: QualityReport =
    QualityPipeline.assess(
      checks =
        listOf(
          QualityCheck("governance_policy", CheckStatus.PASS),
          QualityCheck("core_unit_tests", CheckStatus.PASS),
          QualityCheck("android15_physical_qa", CheckStatus.NOT_PROVEN),
        ),
      commitSha = null,
    ),
  val scoringSimulateMissingSignal: Boolean = true,
  val sandboxScoringAssessment: DeviceAssessment =
    DeviceScoringDispatcher(
        StaticDeviceModelRegistry(
          mapOf(
            "p0_fixture_ref" to
              DeviceModel("p0_fixture_ref", "1.0.0", setOf("battery_state", "permission_posture"))
          )
        )
      )
      .assess("p0_fixture_ref", emptyList(), 1_000L),
  val selectedFixtureId: String = "SYN-004",
  val statisticalFalsePositives: Int = 1,
  val statisticalNegativesTotal: Int = 500,
  // Installed Apps & Granted Dangerous Permissions (on-demand, RAM-only)
  val installedAppsScanning: Boolean = false,
  val installedAppsScanResult: InstalledAppsScanResult? = null,
  val appSearchQuery: String = "",
  val selectedAppFilter: AppPermissionFilter = AppPermissionFilter.ALL,
  val selectedOriginFilter: AppOriginFilter = AppOriginFilter.ALL,
  val hideSystemApps: Boolean = false,
  val filteredInstalledApps: List<InstalledAppPermissionInfo> = emptyList(),
  val categorizedAppSections: List<CategorizedAppGroup> = emptyList(),
  val collapsedAppSections: Set<AppSectionCategory> = setOf(AppSectionCategory.STANDARD),
  val appRethinkRuleOverrides: Map<String, Map<String, Boolean>> = emptyMap(),
  // Live Battery Health Monitor Dashboard (foreground/on-demand, RAM-only)
  val batteryMonitorLoading: Boolean = false,
  val batterySnapshot: BatteryDashboardSnapshot? = null,
  val batteryTrendHistory: List<BatteryTrendPoint> = emptyList(),
  // On-demand System & Google Security Check (RAM-only, never persisted)
  val systemSecurityChecking: Boolean = false,
  val systemSecuritySnapshot: SystemSecuritySnapshot? = null,
  val systemSecurityErrorMessage: String? = null,
) {
  val verifiedCount: Int
    get() =
      allTasks.count {
        it.status == GovernanceStatus.VERIFIED || it.id in preferences.localVerifiedTaskIds
      }

  val qaCompletedCount: Int
    get() =
      allTasks
        .filter { it.category == PlanCategory.ANDROID15_QA }
        .count { it.id in preferences.localVerifiedTaskIds }

  val qaTotalCount: Int
    get() = allTasks.count { it.category == PlanCategory.ANDROID15_QA }

  val readinessFraction: Float
    get() =
      if (allTasks.isEmpty()) 0f else verifiedCount.toFloat() / allTasks.size.toFloat()
}

class DeviceCareViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = DesignPreferencesRepository(application.applicationContext)

  private val inspectionInProgressFlow = MutableStateFlow(false)
  private val inspectionResultFlow = MutableStateFlow<InspectionResult?>(null)
  private val isStaleFlow = MutableStateFlow(false)
  private val settingsMessageFlow = MutableStateFlow<String?>(null)

  private val searchQueryFlow = MutableStateFlow("")
  private val selectedCategoryFlow = MutableStateFlow(PlanCategory.ALL)
  private val selectedStatusFlow = MutableStateFlow<GovernanceStatus?>(null)
  private val selectedArchTabFlow = MutableStateFlow(ArchitectureTab.PIPELINES)
  private val enterprisePreviewFlow = MutableStateFlow(false)

  // Pipeline Sandbox Flows
  private val qualityShaPresentFlow = MutableStateFlow(false)
  private val scoringMissingSignalFlow = MutableStateFlow(true)
  private val selectedFixtureIdFlow = MutableStateFlow("SYN-004")
  private val statFpFlow = MutableStateFlow(1)
  private val statNegTotalFlow = MutableStateFlow(500)

  // Installed Apps & Permissions Flows (RAM-only, never persisted)
  private val installedAppsScanningFlow = MutableStateFlow(false)
  private val installedAppsResultFlow = MutableStateFlow<InstalledAppsScanResult?>(null)
  private val appSearchQueryFlow = MutableStateFlow("")
  private val selectedAppFilterFlow = MutableStateFlow(AppPermissionFilter.ALL)
  private val selectedOriginFilterFlow = MutableStateFlow(AppOriginFilter.ALL)
  private val collapsedAppSectionsFlow =
    MutableStateFlow(setOf(AppSectionCategory.STANDARD))
  private val appRethinkRuleOverridesFlow =
    MutableStateFlow<Map<String, Map<String, Boolean>>>(emptyMap())
  private val batteryMonitorLoadingFlow = MutableStateFlow(false)
  private val batterySnapshotFlow = MutableStateFlow<BatteryDashboardSnapshot?>(null)
  private val batteryTrendHistoryFlow = MutableStateFlow<List<BatteryTrendPoint>>(emptyList())
  private var lowBatteryAlertedInCycle = false
  private val systemSecurityCheckingFlow = MutableStateFlow(false)
  private val systemSecuritySnapshotFlow = MutableStateFlow<SystemSecuritySnapshot?>(null)
  private val systemSecurityErrorFlow = MutableStateFlow<String?>(null)

  private val sandboxDispatcher =
    DeviceScoringDispatcher(
      StaticDeviceModelRegistry(
        mapOf(
          "p0_fixture_ref" to
            DeviceModel("p0_fixture_ref", "1.0.0", setOf("battery_state", "permission_posture"))
        )
      )
    )

  private data class FilterTuple(
    val query: String,
    val category: PlanCategory,
    val status: GovernanceStatus?,
    val archTab: ArchitectureTab,
    val enterprise: Boolean,
  )

  private data class SandboxTuple(
    val shaPresent: Boolean,
    val missingSignal: Boolean,
    val fixtureId: String,
    val fp: Int,
    val negTotal: Int,
  )

  private val filterFlow =
    combine(
      searchQueryFlow,
      selectedCategoryFlow,
      selectedStatusFlow,
      selectedArchTabFlow,
      enterprisePreviewFlow,
    ) { query, category, status, archTab, enterprise ->
      FilterTuple(query, category, status, archTab, enterprise)
    }

  private val sandboxFlow =
    combine(
      qualityShaPresentFlow,
      scoringMissingSignalFlow,
      selectedFixtureIdFlow,
      statFpFlow,
      statNegTotalFlow,
    ) { sha, missing, fixtureId, fp, negTotal ->
      SandboxTuple(sha, missing, fixtureId, fp, negTotal)
    }

  private val batteryTupleFlow =
    combine(
      batteryMonitorLoadingFlow,
      batterySnapshotFlow,
      batteryTrendHistoryFlow,
    ) { loading, snapshot, history ->
      Triple(loading, snapshot, history)
    }

  private val systemSecurityTupleFlow =
    combine(
      systemSecurityCheckingFlow,
      systemSecuritySnapshotFlow,
      systemSecurityErrorFlow,
    ) { checking, snapshot, err ->
      Triple(checking, snapshot, err)
    }

  private val inspectionTupleFlow =
    combine(
      inspectionInProgressFlow,
      inspectionResultFlow,
      isStaleFlow,
      settingsMessageFlow,
      batteryTupleFlow,
    ) { busy, result, stale, msg, batteryTriple ->
      InspectionBundle(
        busy = busy,
        result = result,
        stale = stale,
        msg = msg,
        batteryLoading = batteryTriple.first,
        batterySnapshot = batteryTriple.second,
        batteryTrendHistory = batteryTriple.third,
      )
    }

  private val combinedInspectionTupleFlow =
    combine(inspectionTupleFlow, systemSecurityTupleFlow) { insp, sec ->
      insp to sec
    }

  private data class InspectionBundle(
    val busy: Boolean,
    val result: InspectionResult?,
    val stale: Boolean,
    val msg: String?,
    val batteryLoading: Boolean,
    val batterySnapshot: BatteryDashboardSnapshot?,
    val batteryTrendHistory: List<BatteryTrendPoint>,
  )

  private data class AppPermissionsBundle(
    val scanning: Boolean,
    val result: InstalledAppsScanResult?,
    val query: String,
    val filter: AppPermissionFilter,
    val originFilter: AppOriginFilter,
    val collapsedSections: Set<AppSectionCategory>,
    val rethinkRuleOverrides: Map<String, Map<String, Boolean>>,
  )

  private val appFilterSubFlow =
    combine(
      appSearchQueryFlow,
      selectedAppFilterFlow,
      selectedOriginFilterFlow,
      collapsedAppSectionsFlow,
    ) { query, filter, originFilter, collapsedSections ->
      Triple(query, filter, originFilter to collapsedSections)
    }

  private val appPermissionsTupleFlow =
    combine(
      installedAppsScanningFlow,
      installedAppsResultFlow,
      appFilterSubFlow,
      appRethinkRuleOverridesFlow,
    ) { scanning, result, (query, filter, flags), ruleOverrides ->
      AppPermissionsBundle(
        scanning = scanning,
        result = result,
        query = query,
        filter = filter,
        originFilter = flags.first,
        collapsedSections = flags.second,
        rethinkRuleOverrides = ruleOverrides,
      )
    }

  val uiState: StateFlow<DeviceCareUiState> =
    combine(
        repository.preferencesFlow,
        combinedInspectionTupleFlow,
        filterFlow,
        sandboxFlow,
        appPermissionsTupleFlow,
      ) { prefs, inspPair, filter, sandbox, appPerms ->
        val (insp, sec) = inspPair
        val filtered =
          DeviceCareRoadmapData.tasks.filter { task ->
            val effectiveStatus =
              if (task.id in prefs.localVerifiedTaskIds) GovernanceStatus.VERIFIED else task.status
            val matchesCat =
              filter.category == PlanCategory.ALL || task.category == filter.category
            val matchesStatus = filter.status == null || effectiveStatus == filter.status
            val matchesQuery =
              filter.query.isBlank() ||
                task.title.contains(filter.query, ignoreCase = true) ||
                task.subtitle.contains(filter.query, ignoreCase = true) ||
                task.code.contains(filter.query, ignoreCase = true) ||
                task.description.contains(filter.query, ignoreCase = true)
            matchesCat && matchesStatus && matchesQuery
          }

        val qualityReport =
          QualityPipeline.assess(
            checks =
              listOf(
                QualityCheck("governance_policy", CheckStatus.PASS),
                QualityCheck("core_unit_tests", CheckStatus.PASS),
                QualityCheck(
                  "android15_physical_qa",
                  if (sandbox.shaPresent) CheckStatus.PASS else CheckStatus.NOT_PROVEN,
                ),
              ),
            commitSha =
              if (sandbox.shaPresent) "a1b2c3d4e5f60718293a4b5c6d7e8f9012345678" else null,
          )

        val now = 10_000L
        val sampleEvidence =
          if (sandbox.missingSignal) {
            listOf(
              DeviceEvidence(
                id = "battery_state",
                source = "synthetic_fixture",
                availability = Availability.OBSERVED,
                observedAtMillis = 9_000L,
                expiresAtMillis = 69_000L,
                riskLevel = RiskLevel.NONE,
              )
            )
          } else {
            listOf(
              DeviceEvidence(
                id = "battery_state",
                source = "synthetic_fixture",
                availability = Availability.OBSERVED,
                observedAtMillis = 9_000L,
                expiresAtMillis = 69_000L,
                riskLevel = RiskLevel.NONE,
              ),
              DeviceEvidence(
                id = "permission_posture",
                source = "synthetic_fixture",
                availability = Availability.OBSERVED,
                observedAtMillis = 9_000L,
                expiresAtMillis = 69_000L,
                riskLevel = RiskLevel.LOW,
              ),
            )
          }

          val filteredApps =
            filterInstalledApps(
              apps = appPerms.result?.apps.orEmpty(),
              query = appPerms.query,
              filter = appPerms.filter,
              originFilter = appPerms.originFilter,
            )

        DeviceCareUiState(
          preferences = prefs,
          inspectionInProgress = insp.busy,
          inspectionResult = insp.result,
          isInspectionStale = insp.stale,
          settingsMessage = insp.msg,
          searchQuery = filter.query,
          selectedCategory = filter.category,
          selectedStatusFilter = filter.status,
          filteredTasks = filtered,
          selectedArchitectureTab = filter.archTab,
          enterprisePreviewMode = filter.enterprise,
          qualitySimulateShaPresent = sandbox.shaPresent,
          qualityReport = qualityReport,
          scoringSimulateMissingSignal = sandbox.missingSignal,
          sandboxScoringAssessment =
            sandboxDispatcher.assess("p0_fixture_ref", sampleEvidence, now),
          selectedFixtureId = sandbox.fixtureId,
          statisticalFalsePositives = sandbox.fp,
          statisticalNegativesTotal = sandbox.negTotal,
          installedAppsScanning = appPerms.scanning,
          installedAppsScanResult = appPerms.result,
          appSearchQuery = appPerms.query,
          selectedAppFilter = appPerms.filter,
          selectedOriginFilter = appPerms.originFilter,
          hideSystemApps = appPerms.originFilter == AppOriginFilter.USER_ONLY,
          filteredInstalledApps = filteredApps,
          categorizedAppSections = PermissionSensitivityClassifier.categorizeApps(filteredApps),
          collapsedAppSections =
            if (
              appPerms.query.isNotBlank() ||
                appPerms.filter != AppPermissionFilter.ALL ||
                appPerms.originFilter == AppOriginFilter.SYSTEM_ONLY
            ) {
              emptySet()
            } else {
              appPerms.collapsedSections
            },
          appRethinkRuleOverrides = appPerms.rethinkRuleOverrides,
          batteryMonitorLoading = insp.batteryLoading,
          batterySnapshot = insp.batterySnapshot,
          batteryTrendHistory = insp.batteryTrendHistory,
          systemSecurityChecking = sec.first,
          systemSecuritySnapshot = sec.second,
          systemSecurityErrorMessage = sec.third,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeviceCareUiState(),
      )

  /**
   * Expliziter On-Demand-Scan auf Dispatchers.IO.
   * Kein Auto-Start beim App-Öffnen, keine Persistenz der Rohdaten.
   */
  fun startManualInspection() {
    if (inspectionInProgressFlow.value) return
    inspectionInProgressFlow.value = true
    isStaleFlow.value = false
    settingsMessageFlow.value = null

    viewModelScope.launch {
      val startElapsed = SystemClock.elapsedRealtime()
      val startCpu = Process.getElapsedCpuTime()
      try {
        val context = getApplication<Application>().applicationContext
        val result =
          withContext(Dispatchers.IO) {
            val scanProfile = AndroidPermissionScanProfile(context)
            val profile = scanProfile.read()
            val budget = PermissionScanBudget.forProfile(profile)
            val battery = AndroidBatteryCollector(context).collect()
            var deferred = false
            val permissions =
              AndroidPermissionCollector(context)
                .collect(
                  sampleLimit = budget.packageLimit,
                  shouldDeferOptionalReads = {
                    scanProfile.hasHighPressure().also { deferred = it }
                  },
                  checkCancellation = { coroutineContext.ensureActive() },
                )
            val systemHealth = collectSystemHealthSnapshot(context)
            val elapsedMs = (SystemClock.elapsedRealtime() - startElapsed).coerceAtLeast(0)
            val cpuMs = (Process.getElapsedCpuTime() - startCpu).coerceAtLeast(0)
            presentInspection(
              profile = profile,
              budget = budget,
              battery = battery,
              permissions = permissions,
              optionalReadsDeferred = deferred,
              elapsedMs = elapsedMs,
              cpuMs = cpuMs,
              systemHealthSnapshot = systemHealth,
            )
          }
        inspectionResultFlow.value = result
        val inspectionChargingStatus =
          when (result.batteryChargingState) {
            "CHARGING" -> ChargingStatus.CHARGING
            "FULL" -> ChargingStatus.FULL
            "DISCHARGING" -> ChargingStatus.DISCHARGING
            "NOT_CHARGING" -> ChargingStatus.NOT_CHARGING
            else -> ChargingStatus.UNKNOWN
          }
        evaluateLowBatteryNotification(
          percent = result.batteryPercent,
          chargingStatus = inspectionChargingStatus,
        )
        // Mark snapshot stale after ObservationContract.MAX_AGE_MILLIS (60s)
        launch {
          delay(ObservationContract.MAX_AGE_MILLIS)
          if (inspectionResultFlow.value?.collectedAtMillis == result.collectedAtMillis) {
            isStaleFlow.value = true
          }
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: RuntimeException) {
        inspectionResultFlow.value =
          InspectionResult(
            errorMessage = "Lokale Prüfung nicht verfügbar. Keine Daten gespeichert."
          )
      } finally {
        inspectionInProgressFlow.value = false
      }
    }
  }

  /**
   * Expliziter On-Demand-Scan sichtbarer installierter Apps und deren gewährter
   * Laufzeitberechtigungen. Bleibt rein im Arbeitsspeicher (keine Persistenz, kein Auto-Start).
   */
  fun startInstalledAppsScan() {
    if (installedAppsScanningFlow.value) return
    installedAppsScanningFlow.value = true

    viewModelScope.launch {
      try {
        val context = getApplication<Application>().applicationContext
        val result =
          withContext(Dispatchers.IO) {
            InstalledAppPermissionInspector(context).inspectVisibleApps()
          }
        installedAppsResultFlow.value = result
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: RuntimeException) {
        installedAppsResultFlow.value =
          InstalledAppsScanResult(
            scannedAtMillis = System.currentTimeMillis(),
            visiblePackageCount = 0,
            apps = emptyList(),
            errorMessage = "Installierte Apps konnten lokal nicht gelesen werden.",
          )
      } finally {
        installedAppsScanningFlow.value = false
      }
    }
  }

  fun refreshBatteryMonitor(batteryIntent: Intent? = null) {
    if (batteryIntent == null && batteryMonitorLoadingFlow.value) return
    if (batteryIntent == null) {
      batteryMonitorLoadingFlow.value = true
    }

    viewModelScope.launch {
      try {
        val context = getApplication<Application>().applicationContext
        val snapshot =
          withContext(Dispatchers.IO) {
            AndroidBatteryHealthMonitor(context).readSnapshot(batteryIntent)
          }
        batterySnapshotFlow.value = snapshot
        batteryTrendHistoryFlow.value =
          BatteryTrendRecorder.append(batteryTrendHistoryFlow.value, snapshot)
        evaluateLowBatteryNotification(
          percent = snapshot.percent,
          chargingStatus = snapshot.chargingStatus,
        )
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: RuntimeException) {
        batterySnapshotFlow.value =
          BatteryDashboardSnapshot(
            observedAtMillis = System.currentTimeMillis(),
            errorMessage = "Akkudaten konnten lokal nicht gelesen werden.",
          )
      } finally {
        batteryMonitorLoadingFlow.value = false
      }
    }
  }

  private fun evaluateLowBatteryNotification(
    percent: Int?,
    chargingStatus: ChargingStatus,
    forceEnabledOverride: Boolean? = null,
  ) {
    val enabled = forceEnabledOverride ?: uiState.value.preferences.lowBatteryAlertsEnabled
    val decision =
      LowBatteryAlertEvaluator.evaluate(
        percent = percent,
        chargingStatus = chargingStatus,
        alertsEnabled = enabled,
        alreadyAlertedBelowThreshold = lowBatteryAlertedInCycle,
      )
    lowBatteryAlertedInCycle = decision.nextAlertedState
    if (decision.shouldNotify && percent != null) {
      val context = getApplication<Application>().applicationContext
      LowBatteryAlertNotifier(context).notifyLowBattery(percent)
    }
  }

  fun setLowBatteryAlertsEnabled(enabled: Boolean) {
    viewModelScope.launch { repository.setLowBatteryAlertsEnabled(enabled) }
    if (!enabled) {
      lowBatteryAlertedInCycle = false
      return
    }
    val latest = batterySnapshotFlow.value
    if (latest != null) {
      evaluateLowBatteryNotification(
        percent = latest.percent,
        chargingStatus = latest.chargingStatus,
        forceEnabledOverride = true,
      )
    } else {
      val inspected = inspectionResultFlow.value
      if (inspected != null) {
        val status =
          when (inspected.batteryChargingState) {
            "CHARGING" -> ChargingStatus.CHARGING
            "FULL" -> ChargingStatus.FULL
            "DISCHARGING" -> ChargingStatus.DISCHARGING
            else -> ChargingStatus.UNKNOWN
          }
        evaluateLowBatteryNotification(
          percent = inspected.batteryPercent,
          chargingStatus = status,
          forceEnabledOverride = true,
        )
      }
    }
  }

  /**
   * Expliziter On-Demand System- & Google-Sicherheitscheck (RAM-only, keine Persistenz).
   */
  fun runSystemSecurityCheck() {
    if (systemSecurityCheckingFlow.value) return
    systemSecurityCheckingFlow.value = true
    systemSecurityErrorFlow.value = null

    viewModelScope.launch {
      try {
        val context = getApplication<Application>().applicationContext
        val snapshot =
          withContext(Dispatchers.IO) {
            AndroidSystemSecurityReader(context).read()
          }
        systemSecuritySnapshotFlow.value = snapshot
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: RuntimeException) {
        systemSecurityErrorFlow.value =
          "System- & Google-Abfrage lokal fehlgeschlagen. Keine Daten gespeichert."
      } finally {
        systemSecurityCheckingFlow.value = false
      }
    }
  }

  /**
   * Startet Berechtigungsscan, System-Sicherheitscheck und lokale Prüfung auf expliziten Nutzerwunsch.
   */
  fun runPrivacyAndSecurityOverview() {
    startInstalledAppsScan()
    runSystemSecurityCheck()
    startManualInspection()
  }

  fun updateAppSearchQuery(query: String) {
    appSearchQueryFlow.value = query
  }

  fun selectAppFilter(filter: AppPermissionFilter) {
    selectedAppFilterFlow.value =
      if (filter != AppPermissionFilter.ALL && selectedAppFilterFlow.value == filter) {
        AppPermissionFilter.ALL
      } else {
        filter
      }
  }

  fun toggleHideSystemApps() {
    selectedOriginFilterFlow.value =
      if (selectedOriginFilterFlow.value == AppOriginFilter.USER_ONLY) {
        AppOriginFilter.ALL
      } else {
        AppOriginFilter.USER_ONLY
      }
  }

  fun selectAppOriginFilter(origin: AppOriginFilter) {
    selectedOriginFilterFlow.value = origin
  }

  fun toggleAppSectionCollapsed(category: AppSectionCategory) {
    val current = collapsedAppSectionsFlow.value
    collapsedAppSectionsFlow.value =
      if (category in current) current - category else current + category
  }

  fun setAppRethinkRuleEnabled(packageName: String, ruleId: String, enabled: Boolean) {
    val current = appRethinkRuleOverridesFlow.value.toMutableMap()
    val pkgMap = current[packageName].orEmpty().toMutableMap()
    pkgMap[ruleId] = enabled
    current[packageName] = pkgMap
    appRethinkRuleOverridesFlow.value = current
  }

  fun resetAppRethinkRules(packageName: String) {
    if (packageName !in appRethinkRuleOverridesFlow.value) return
    appRethinkRuleOverridesFlow.value = appRethinkRuleOverridesFlow.value - packageName
  }

  private fun filterInstalledApps(
    apps: List<InstalledAppPermissionInfo>,
    query: String,
    filter: AppPermissionFilter,
    originFilter: AppOriginFilter,
  ): List<InstalledAppPermissionInfo> {
    val trimmedQuery = query.trim()
    return apps.filter { app ->
      val matchesOrigin =
        when (originFilter) {
          AppOriginFilter.ALL -> true
          AppOriginFilter.USER_ONLY -> !app.isSystemApp
          AppOriginFilter.SYSTEM_ONLY -> app.isSystemApp
        }
      val matchesCategory =
        when (filter) {
          AppPermissionFilter.ALL -> true
          AppPermissionFilter.SENSITIVE_ONLY -> app.hasHighSensitivityAccess
          AppPermissionFilter.MANIPULATION_RISK ->
            app.securityPosture?.hasManipulationWarnings == true ||
              app.securityPosture?.recentlyUpdatedWithin7Days == true
          AppPermissionFilter.FIREWALL_BLOCK -> {
            val action = app.securityPosture?.firewallRecommendation?.action
            action == FirewallAction.BLOCK_ALL || action == FirewallAction.ALLOW_FOREGROUND_ONLY
          }
          AppPermissionFilter.CAMERA -> SensitivityCategory.CAMERA in app.categories
          AppPermissionFilter.LOCATION -> SensitivityCategory.LOCATION in app.categories
          AppPermissionFilter.MICROPHONE -> SensitivityCategory.MICROPHONE in app.categories
          AppPermissionFilter.PHONE_SMS -> SensitivityCategory.PHONE_SMS in app.categories
          AppPermissionFilter.CONTACTS -> SensitivityCategory.CONTACTS in app.categories
        }
      val matchesQuery =
        trimmedQuery.isEmpty() ||
          app.appName.contains(trimmedQuery, ignoreCase = true) ||
          app.packageName.contains(trimmedQuery, ignoreCase = true) ||
          app.grantedDangerousPermissions.any { perm ->
            perm.shortName.contains(trimmedQuery, ignoreCase = true) ||
              perm.fullName.contains(trimmedQuery, ignoreCase = true) ||
              perm.category.label.contains(trimmedQuery, ignoreCase = true)
          } ||
          app.securityPosture?.installerSourceLabel?.contains(trimmedQuery, ignoreCase = true) == true ||
          app.securityPosture?.firewallRecommendation?.action?.title?.contains(trimmedQuery, ignoreCase = true) == true ||
          app.securityPosture?.tamperSignals?.any {
            it.label.contains(trimmedQuery, ignoreCase = true)
          } == true
      matchesOrigin && matchesCategory && matchesQuery
    }
  }

  fun clearSettingsMessage() {
    settingsMessageFlow.value = null
  }

  fun setSettingsMessage(message: String?) {
    settingsMessageFlow.value = message
  }

  fun updateSearchQuery(query: String) {
    searchQueryFlow.value = query
  }

  fun selectCategory(category: PlanCategory) {
    selectedCategoryFlow.value = category
  }

  fun selectStatusFilter(status: GovernanceStatus?) {
    selectedStatusFlow.value = if (selectedStatusFlow.value == status) null else status
  }

  fun selectArchitectureTab(tab: ArchitectureTab) {
    selectedArchTabFlow.value = tab
  }

  fun setEnterprisePreviewMode(enabled: Boolean) {
    enterprisePreviewFlow.value = enabled
  }

  fun toggleQualityShaSimulation() {
    qualityShaPresentFlow.value = !qualityShaPresentFlow.value
  }

  fun toggleScoringMissingSignalSimulation() {
    scoringMissingSignalFlow.value = !scoringMissingSignalFlow.value
  }

  fun selectFixture(id: String) {
    selectedFixtureIdFlow.value = id
  }

  fun setStatisticalFalsePositives(fp: Int) {
    statFpFlow.value = fp.coerceIn(0, 15)
  }

  fun computeStatisticalCounts(): StatisticalValidation.Counts {
    val neg = statNegTotalFlow.value.toLong()
    val fp = statFpFlow.value.toLong().coerceAtMost(neg)
    val tn = neg - fp
    return StatisticalValidation.Counts(tp = 95L, fp = fp, tn = tn, fn = 5L)
  }

  fun getTaskById(id: String): RoadmapTask? = DeviceCareRoadmapData.tasks.find { it.id == id }

  fun getSelectedFixture(): SyntheticFixtureOracle.Fixture =
    SyntheticFixtureOracle.referenceCases.find { it.id == selectedFixtureIdFlow.value }
      ?: SyntheticFixtureOracle.referenceCases.first()

  fun toggleLocalTaskVerified(taskId: String) {
    viewModelScope.launch { repository.toggleLocalTaskVerified(taskId) }
  }

  fun toggleBookmark(taskId: String) {
    viewModelScope.launch { repository.toggleBookmark(taskId) }
  }

  fun setPalette(palette: ThemePalette) {
    viewModelScope.launch { repository.setPalette(palette) }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { repository.setThemeMode(mode) }
  }

  fun cycleThemeMode() {
    val next =
      when (uiState.value.preferences.themeMode) {
        ThemeMode.SYSTEM -> ThemeMode.DARK
        ThemeMode.DARK -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.SYSTEM
      }
    setThemeMode(next)
  }

  fun setCornerStyle(style: CornerStyle) {
    viewModelScope.launch { repository.setCornerStyle(style) }
  }

  fun setCompactDensity(enabled: Boolean) {
    viewModelScope.launch { repository.setCompactDensity(enabled) }
  }

  fun setHapticPreview(enabled: Boolean) {
    viewModelScope.launch { repository.setHapticPreview(enabled) }
  }

  fun resetDesignDefaults() {
    viewModelScope.launch { repository.resetDefaults() }
  }
}
