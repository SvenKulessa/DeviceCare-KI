package com.example

import android.content.ActivityNotFoundException
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.devicecare.SettingsLaunchResult
import com.example.devicecare.appPermissionSettingsIntent
import com.example.devicecare.openDeviceSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AI-DeviceCare", appName)
  }

  @Test
  fun `appPermissionSettingsIntent targets specific app details settings`() {
    val targetPkg = "com.example.cameraapp"
    val intent = appPermissionSettingsIntent(targetPkg)
    assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
    assertEquals("package:$targetPkg", intent.dataString)

    val directResult = openDeviceSettings(intent) { launched ->
      assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, launched.action)
    }
    assertEquals(SettingsLaunchResult.DIRECT, directResult)

    var fallbackAction: String? = null
    val fallbackResult =
      openDeviceSettings(intent) { launched ->
        if (launched.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
          throw ActivityNotFoundException("no handler")
        }
        fallbackAction = launched.action
      }
    assertEquals(SettingsLaunchResult.GENERAL_SETTINGS, fallbackResult)
    assertEquals(Settings.ACTION_SETTINGS, fallbackAction)
  }

  @Test
  fun `mainActivity starts and navigates across all tabs without crashing`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hero_inspection_card").assertExists()

    composeTestRule.onNodeWithTag("inspection_start_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_apps").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("app_permissions_screen").assertExists()
    composeTestRule.onNodeWithTag("scan_apps_permissions_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_roadmap").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_architecture").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_design").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `battery dashboard opens from inspection hero displays cards and navigates back`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("open_battery_monitor_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("battery_dashboard_screen").assertExists()
    composeTestRule.onNodeWithTag("battery_level_card").assertExists()
    composeTestRule.onNodeWithTag("remaining_time_card").assertExists()
    composeTestRule
      .onNodeWithTag("battery_dashboard_list")
      .performScrollToNode(hasTestTag("battery_health_telemetry_grid"))
    composeTestRule.onNodeWithTag("battery_health_telemetry_grid").assertExists()

    composeTestRule.onNodeWithTag("battery_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hero_inspection_card").assertExists()
  }

  @Test
  fun `ai advisor opens from inspection hero renders controls and navigates back`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("open_ai_advisor_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("ai_advisor_screen").assertExists()
    composeTestRule.onNodeWithTag("ai_context_toggle").assertExists()
    composeTestRule.onNodeWithTag("ai_prompt_input").assertExists()
    composeTestRule.onNodeWithTag("ai_send_button").assertExists()

    composeTestRule.onNodeWithTag("ai_context_toggle").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("ai_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hero_inspection_card").assertExists()
  }
}
