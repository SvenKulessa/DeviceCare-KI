package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ai.AiAdvisorViewModel
import com.example.ui.components.QuickNavigationSheet
import com.example.ui.components.UniverseCosmicBackground
import com.example.ui.components.UniverseDiagnosticCategory
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.AppPermissionsScreen
import com.example.ui.screens.BatteryDashboardScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.DesignSystemScreen
import com.example.ui.screens.InspectionScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.PrivacySummaryScreen
import com.example.ui.screens.RethinkRecommendationsScreen
import com.example.ui.screens.SystemSecurityFeatureScreen
import com.example.ui.theme.FormwerkTheme
import kotlinx.coroutines.launch

object Routes {
  const val ROUTE_INSPECTION = "inspection"
  const val ROUTE_BATTERY = "battery"
  const val ROUTE_AI_ADVISOR = "ai_advisor"
  const val ROUTE_CLOUD_SYNC = "cloud_sync"
  const val ROUTE_SYSTEM_SECURITY = "system_security"
  const val ROUTE_PRIVACY_SUMMARY = "privacy_summary"
  const val ROUTE_RETHINK_GUIDE = "rethink_guide"
  const val ROUTE_APPS = "apps"
  const val ROUTE_DESIGN = "design"
  const val ROUTE_PRIVACY_POLICY = "privacy_policy"
}

private data class NavDestinationItem(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val topLevelDestinations =
  listOf(
    NavDestinationItem(
      route = Routes.ROUTE_INSPECTION,
      labelRes = R.string.nav_inspection,
      selectedIcon = Icons.Filled.HealthAndSafety,
      unselectedIcon = Icons.Outlined.HealthAndSafety,
      testTag = "nav_inspection",
    ),
    NavDestinationItem(
      route = Routes.ROUTE_APPS,
      labelRes = R.string.nav_apps,
      selectedIcon = Icons.Filled.Apps,
      unselectedIcon = Icons.Outlined.Apps,
      testTag = "nav_apps",
    ),
    NavDestinationItem(
      route = Routes.ROUTE_RETHINK_GUIDE,
      labelRes = R.string.nav_network,
      selectedIcon = Icons.Filled.Dns,
      unselectedIcon = Icons.Outlined.Dns,
      testTag = "nav_network",
    ),
    NavDestinationItem(
      route = Routes.ROUTE_PRIVACY_SUMMARY,
      labelRes = R.string.nav_cockpit,
      selectedIcon = Icons.Filled.Shield,
      unselectedIcon = Icons.Outlined.Shield,
      testTag = "nav_cockpit",
    ),
    NavDestinationItem(
      route = Routes.ROUTE_DESIGN,
      labelRes = R.string.nav_design,
      selectedIcon = Icons.Filled.Palette,
      unselectedIcon = Icons.Outlined.Palette,
      testTag = "nav_design",
    ),
  )

@Composable
fun DeviceCareApp(
  viewModel: DeviceCareViewModel = viewModel(),
  aiAdvisorViewModel: AiAdvisorViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val aiState by aiAdvisorViewModel.uiState.collectAsStateWithLifecycle()
  val privacyAnalysisState by aiAdvisorViewModel.privacyAnalysisState.collectAsStateWithLifecycle()
  val postureNextStepsState by aiAdvisorViewModel.postureNextStepsState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val haptics = LocalHapticFeedback.current
  var showQuickNavSheet by remember { mutableStateOf(false) }
  var selectedUniverseCategory by remember {
    mutableStateOf(UniverseDiagnosticCategory.SYSTEM_HEALTH)
  }
  var planetPulseNonce by remember { mutableIntStateOf(0) }

  fun performHaptic() {
    if (uiState.preferences.hapticPreview) {
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
  }

  fun pulsePlanet(category: UniverseDiagnosticCategory? = null) {
    if (category != null) {
      selectedUniverseCategory = category
    }
    planetPulseNonce += 1
  }

  fun showMessage(message: String) {
    scope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(message)
    }
  }

  fun navigateTopLevel(route: String) {
    performHaptic()
    val mapped =
      when (route) {
        Routes.ROUTE_INSPECTION -> UniverseDiagnosticCategory.SYSTEM_HEALTH
        Routes.ROUTE_APPS -> UniverseDiagnosticCategory.APPS_SECURITY
        Routes.ROUTE_RETHINK_GUIDE -> UniverseDiagnosticCategory.NETWORK_RETHINK
        Routes.ROUTE_PRIVACY_SUMMARY -> UniverseDiagnosticCategory.PRIVACY_COCKPIT
        else -> null
      }
    pulsePlanet(mapped)
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  fun launchUniverseCategoryAction(category: UniverseDiagnosticCategory) {
    performHaptic()
    pulsePlanet(category)
    when (category) {
      UniverseDiagnosticCategory.SYSTEM_HEALTH -> viewModel.startManualInspection()
      UniverseDiagnosticCategory.BATTERY -> navController.navigate(Routes.ROUTE_BATTERY)
      UniverseDiagnosticCategory.APPS_SECURITY -> navigateTopLevel(Routes.ROUTE_APPS)
      UniverseDiagnosticCategory.NETWORK_RETHINK -> navigateTopLevel(Routes.ROUTE_RETHINK_GUIDE)
      UniverseDiagnosticCategory.PRIVACY_COCKPIT -> navigateTopLevel(Routes.ROUTE_PRIVACY_SUMMARY)
      UniverseDiagnosticCategory.AI_ADVISOR -> navController.navigate(Routes.ROUTE_AI_ADVISOR)
      UniverseDiagnosticCategory.CLOUD_NOTES -> navController.navigate(Routes.ROUTE_CLOUD_SYNC)
    }
  }

  val showBottomOrRailNav =
    currentDestination?.route == null ||
      currentDestination.route in topLevelDestinations.map { it.route }

  FormwerkTheme(
    palette = uiState.preferences.palette,
    themeMode = uiState.preferences.themeMode,
    cornerStyle = uiState.preferences.cornerStyle,
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background,
    ) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        UniverseCosmicBackground(
          activeCategory = selectedUniverseCategory,
          planetPulseNonce = planetPulseNonce,
          themeMode = uiState.preferences.themeMode,
          modifier = Modifier.fillMaxSize(),
        )

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = Color.Transparent,
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          bottomBar = {
            if (!useNavRail && showBottomOrRailNav) {
              NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.90f),
              ) {
                topLevelDestinations.forEach { dest ->
                  val selected =
                    currentDestination?.hierarchy?.any { it.route == dest.route } == true
                  val label = stringResource(dest.labelRes)
                  NavigationBarItem(
                    selected = selected,
                    onClick = { navigateTopLevel(dest.route) },
                    icon = {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label,
                      )
                    },
                    label = { Text(text = label, maxLines = 1) },
                    modifier =
                      Modifier.testTag(dest.testTag).semantics {
                        stateDescription = if (selected) "Ausgewählt" else "Nicht ausgewählt"
                      },
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (useNavRail && showBottomOrRailNav) {
              NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.90f),
              ) {
                Spacer(modifier = Modifier.weight(1f))
                topLevelDestinations.forEach { dest ->
                  val selected =
                    currentDestination?.hierarchy?.any { it.route == dest.route } == true
                  val label = stringResource(dest.labelRes)
                  NavigationRailItem(
                    selected = selected,
                    onClick = { navigateTopLevel(dest.route) },
                    icon = {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label,
                      )
                    },
                    label = { Text(text = label, maxLines = 1) },
                    modifier =
                      Modifier.testTag(dest.testTag).semantics {
                        stateDescription = if (selected) "Ausgewählt" else "Nicht ausgewählt"
                      },
                  )
                }
                Spacer(modifier = Modifier.weight(1f))
              }
              VerticalDivider(
                modifier = Modifier.fillMaxHeight().width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
              )
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
              NavHost(
                navController = navController,
                startDestination = Routes.ROUTE_INSPECTION,
                modifier = Modifier.fillMaxSize(),
              ) {
                composable(Routes.ROUTE_INSPECTION) {
                  InspectionScreen(
                    uiState = uiState,
                    onStartInspection = {
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.SYSTEM_HEALTH)
                      viewModel.startManualInspection()
                    },
                    onCycleThemeMode = {
                      performHaptic()
                      pulsePlanet()
                      viewModel.cycleThemeMode()
                    },
                    onSettingsMessage = viewModel::setSettingsMessage,
                    onOpenBatteryMonitor = {
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.BATTERY)
                      navController.navigate(Routes.ROUTE_BATTERY)
                    },
                    onOpenAiAdvisor = {
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.AI_ADVISOR)
                      navController.navigate(Routes.ROUTE_AI_ADVISOR)
                    },
                    onOpenCloudSync = {
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.CLOUD_NOTES)
                      navController.navigate(Routes.ROUTE_CLOUD_SYNC)
                    },
                    onOpenSystemSecurity = {
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.PRIVACY_COCKPIT)
                      navController.navigate(Routes.ROUTE_SYSTEM_SECURITY)
                    },
                    onOpenPrivacySummary = { navigateTopLevel(Routes.ROUTE_PRIVACY_SUMMARY) },
                    onOpenAppPermissions = { navigateTopLevel(Routes.ROUTE_APPS) },
                    onOpenPrivacyPolicy = {
                      performHaptic()
                      navController.navigate(Routes.ROUTE_PRIVACY_POLICY)
                    },
                    onOpenRethinkGuide = { navigateTopLevel(Routes.ROUTE_RETHINK_GUIDE) },
                    onOpenQuickNav = {
                      performHaptic()
                      pulsePlanet()
                      showQuickNavSheet = true
                    },
                    selectedUniverseCategory = selectedUniverseCategory,
                    planetPulseNonce = planetPulseNonce,
                    onSelectUniverseCategory = { cat ->
                      performHaptic()
                      pulsePlanet(cat)
                    },
                    onPlanetTap = {
                      performHaptic()
                      val nextIdx =
                        (selectedUniverseCategory.ordinal + 1) %
                          UniverseDiagnosticCategory.entries.size
                      pulsePlanet(UniverseDiagnosticCategory.entries[nextIdx])
                    },
                    onTriggerUniverseAction = ::launchUniverseCategoryAction,
                  )
                }

                composable(Routes.ROUTE_BATTERY) {
                  BatteryDashboardScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onRefresh = {
                      pulsePlanet(UniverseDiagnosticCategory.BATTERY)
                      viewModel.refreshBatteryMonitor(it)
                    },
                    onShowSnackbar = ::showMessage,
                    onToggleLowBatteryAlerts = { enabled ->
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.BATTERY)
                      viewModel.setLowBatteryAlertsEnabled(enabled)
                    },
                  )
                }

                composable(Routes.ROUTE_AI_ADVISOR) {
                  AiAdvisorScreen(
                    aiState = aiState,
                    hasOptionalContextAvailable =
                      uiState.batterySnapshot != null || uiState.installedAppsScanResult != null,
                    onBack = { navController.popBackStack() },
                    onSendPrompt = { prompt ->
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.AI_ADVISOR)
                      aiAdvisorViewModel.sendPrompt(
                        rawPrompt = prompt,
                        batterySnapshot = uiState.batterySnapshot,
                        appsScanResult = uiState.installedAppsScanResult,
                      )
                    },
                    onToggleContext = {
                      performHaptic()
                      aiAdvisorViewModel.toggleIncludeAnonymizedContext()
                    },
                    onClearChat = {
                      performHaptic()
                      aiAdvisorViewModel.clearChat()
                    },
                  )
                }

                composable(Routes.ROUTE_CLOUD_SYNC) {
                  CloudSyncScreen(
                    onBack = { navController.popBackStack() },
                    onShowSnackbar = ::showMessage,
                  )
                }

                composable(Routes.ROUTE_SYSTEM_SECURITY) {
                  SystemSecurityFeatureScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.ROUTE_PRIVACY_POLICY) {
                  PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.ROUTE_PRIVACY_SUMMARY) {
                  PrivacySummaryScreen(
                    uiState = uiState,
                    privacyAnalysisState = privacyAnalysisState,
                    postureNextStepsState = postureNextStepsState,
                    onBack = { navController.popBackStack() },
                    onRunCombinedCheck = {
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.PRIVACY_COCKPIT)
                      viewModel.runPrivacyAndSecurityOverview()
                    },
                    onAnalyzePostureWithGemini = { summaryReport ->
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.AI_ADVISOR)
                      aiAdvisorViewModel.generatePostureNextSteps(
                        summaryReport = summaryReport,
                        batterySnapshot = uiState.batterySnapshot,
                      )
                    },
                    onClearPostureNextSteps = {
                      performHaptic()
                      aiAdvisorViewModel.clearPostureNextSteps()
                    },
                    onOpenAppPermissions = { navigateTopLevel(Routes.ROUTE_APPS) },
                    onOpenSystemSecurity = {
                      performHaptic()
                      navController.navigate(Routes.ROUTE_SYSTEM_SECURITY)
                    },
                    onOpenAiAdvisor = {
                      performHaptic()
                      navController.navigate(Routes.ROUTE_AI_ADVISOR)
                    },
                    onShowSnackbar = ::showMessage,
                    onScanInstalledApps = {
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.APPS_SECURITY)
                      viewModel.startInstalledAppsScan()
                    },
                  )
                }

                composable(Routes.ROUTE_RETHINK_GUIDE) {
                  RethinkRecommendationsScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onOpenAppPermissions = { navigateTopLevel(Routes.ROUTE_APPS) },
                    onShowSnackbar = ::showMessage,
                  )
                }

                composable(Routes.ROUTE_APPS) {
                  AppPermissionsScreen(
                    uiState = uiState,
                    privacyAnalysisState = privacyAnalysisState,
                    onStartScan = {
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.APPS_SECURITY)
                      viewModel.startInstalledAppsScan()
                    },
                    onAnalyzePrivacy = {
                      performHaptic()
                      pulsePlanet(UniverseDiagnosticCategory.AI_ADVISOR)
                      aiAdvisorViewModel.analyzePermissionsPrivacy(uiState.installedAppsScanResult)
                    },
                    onClearPrivacyAnalysis = {
                      performHaptic()
                      aiAdvisorViewModel.clearPrivacyAnalysis()
                    },
                    onSearchQueryChange = viewModel::updateAppSearchQuery,
                    onSelectFilter = { filter ->
                      performHaptic()
                      viewModel.selectAppFilter(filter)
                    },
                    onToggleHideSystemApps = {
                      performHaptic()
                      viewModel.toggleHideSystemApps()
                    },
                    onToggleSectionCollapsed = { section ->
                      performHaptic()
                      viewModel.toggleAppSectionCollapsed(section)
                    },
                    onShowSnackbar = ::showMessage,
                    onSelectOriginFilter = { origin ->
                      performHaptic()
                      viewModel.selectAppOriginFilter(origin)
                    },
                    onOpenRethinkGuide = { navigateTopLevel(Routes.ROUTE_RETHINK_GUIDE) },
                    onToggleAppRethinkRule = { pkg, ruleId, enabled ->
                      performHaptic()
                      viewModel.setAppRethinkRuleEnabled(pkg, ruleId, enabled)
                    },
                    onResetAppRethinkRules = { pkg ->
                      performHaptic()
                      viewModel.resetAppRethinkRules(pkg)
                    },
                  )
                }

                composable(Routes.ROUTE_DESIGN) {
                  DesignSystemScreen(
                    uiState = uiState,
                    onSelectPalette = { palette ->
                      performHaptic()
                      pulsePlanet()
                      viewModel.setPalette(palette)
                      showMessage("Palette: ${palette.displayName}")
                    },
                    onSelectThemeMode = { mode ->
                      performHaptic()
                      pulsePlanet()
                      viewModel.setThemeMode(mode)
                    },
                    onSelectCornerStyle = { style ->
                      performHaptic()
                      viewModel.setCornerStyle(style)
                    },
                    onToggleCompactDensity = { enabled ->
                      performHaptic()
                      viewModel.setCompactDensity(enabled)
                    },
                    onToggleHapticPreview = { enabled -> viewModel.setHapticPreview(enabled) },
                    onResetDefaults = {
                      performHaptic()
                      pulsePlanet()
                      viewModel.resetDesignDefaults()
                      showMessage("Standarddesign wiederhergestellt")
                    },
                    onShowSnackbar = { msg ->
                      performHaptic()
                      showMessage(msg)
                    },
                  )
                }
              }
            }
          }
        }
      }

      if (showQuickNavSheet) {
        QuickNavigationSheet(
          currentRoute = currentDestination?.route,
          onSelectEntry = { entry ->
            showQuickNavSheet = false
            if (entry.isTopLevel) {
              navigateTopLevel(entry.route)
            } else {
              performHaptic()
              pulsePlanet()
              navController.navigate(entry.route) { launchSingleTop = true }
            }
          },
          onDismiss = { showQuickNavSheet = false },
        )
      }
    }
  }
}
