package com.example.features.battery

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.DesignPreferences
import com.example.ui.DeviceCareUiState
import com.example.ui.screens.BatteryDashboardScreen
import com.example.ui.theme.FormwerkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LowBatteryAlertNotifierTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun evaluatorRespectsTwentyPercentBoundaryChargingAndHysteresis() {
    // At 20% -> does NOT trigger
    val atTwenty =
      LowBatteryAlertEvaluator.evaluate(
        percent = 20,
        chargingStatus = ChargingStatus.DISCHARGING,
        alertsEnabled = true,
        alreadyAlertedBelowThreshold = false,
      )
    assertFalse(atTwenty.shouldNotify)
    assertFalse(atTwenty.nextAlertedState)

    // Drops below 20% while discharging and enabled -> notifies once
    val belowTwenty =
      LowBatteryAlertEvaluator.evaluate(
        percent = 19,
        chargingStatus = ChargingStatus.DISCHARGING,
        alertsEnabled = true,
        alreadyAlertedBelowThreshold = false,
      )
    assertTrue(belowTwenty.shouldNotify)
    assertTrue(belowTwenty.nextAlertedState)

    // Still at 18% in same cycle -> suppressed by hysteresis
    val repeatBelow =
      LowBatteryAlertEvaluator.evaluate(
        percent = 18,
        chargingStatus = ChargingStatus.DISCHARGING,
        alertsEnabled = true,
        alreadyAlertedBelowThreshold = true,
      )
    assertFalse(repeatBelow.shouldNotify)
    assertTrue(repeatBelow.nextAlertedState)

    // Plugged into charger below 20% -> suppressed and hysteresis reset
    val chargingBelow =
      LowBatteryAlertEvaluator.evaluate(
        percent = 15,
        chargingStatus = ChargingStatus.CHARGING,
        alertsEnabled = true,
        alreadyAlertedBelowThreshold = true,
      )
    assertFalse(chargingBelow.shouldNotify)
    assertFalse(chargingBelow.nextAlertedState)

    // Opt-in disabled -> suppressed
    val disabled =
      LowBatteryAlertEvaluator.evaluate(
        percent = 12,
        chargingStatus = ChargingStatus.DISCHARGING,
        alertsEnabled = false,
        alreadyAlertedBelowThreshold = false,
      )
    assertFalse(disabled.shouldNotify)
    assertFalse(disabled.nextAlertedState)
  }

  @Test
  fun notifierRequiresPostNotificationsPermissionAndPostsWhenGranted() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val shadowApp = shadowOf(app)
    val notifier = LowBatteryAlertNotifier(app)
    val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNm = shadowOf(nm)

    shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
    assertFalse(notifier.hasNotificationPermission())
    assertFalse(notifier.notifyLowBattery(14))
    assertEquals(0, shadowNm.allNotifications.size)

    shadowApp.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    assertTrue(notifier.hasNotificationPermission())
    assertTrue(notifier.notifyLowBattery(14))
    assertEquals(1, shadowNm.allNotifications.size)
    val posted = shadowNm.getNotification(LowBatteryAlertNotifier.NOTIFICATION_ID)
    assertTrue(posted.extras.getString("android.title")?.contains("14 %") == true)
  }

  @Test
  fun batteryDashboardRendersAlertCardAndInvokesOptInToggle() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    var toggledValue: Boolean? = null

    rule.setContent {
      FormwerkTheme {
        BatteryDashboardScreen(
          uiState =
            DeviceCareUiState(
              preferences = DesignPreferences(lowBatteryAlertsEnabled = false),
              batterySnapshot =
                BatteryDashboardSnapshot(
                  percent = 16,
                  chargingStatus = ChargingStatus.DISCHARGING,
                  observedAtMillis = 123_456L,
                ),
            ),
          onBack = {},
          onRefresh = {},
          onShowSnackbar = {},
          onToggleLowBatteryAlerts = { toggledValue = it },
        )
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag("battery_dashboard_list")
      .performScrollToNode(hasTestTag("low_battery_alert_card"))
    rule.onNodeWithTag("low_battery_alert_card").assertExists()
    rule.onNodeWithText("Akku-Warnung (< 20 %)").assertExists()
    rule.onNodeWithTag("low_battery_alert_switch").performClick()
    assertEquals(true, toggledValue)
  }
}
