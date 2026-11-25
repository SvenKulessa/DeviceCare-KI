package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** UI-only checks: not a legal approval or proof of on-device network behavior. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrivacyPolicyUiTest {

  @get:Rule val rule = createAndroidComposeRule<MainActivity>()

  @Test
  fun privacyAvailableBeforeInspectionAndBackNavigationWorks() {
    rule.waitForIdle()
    rule.onNodeWithTag("privacy_policy_open").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag("privacy_policy_screen").assertExists()
    rule.onNodeWithTag("privacy_policy_title").assertExists()
    rule.onNodeWithTag("privacy_policy_controller").assertExists()
    rule.onNodeWithTag("privacy_policy_access").assertExists()
    rule.onNodeWithTag("privacy_policy_network").assertExists()
    // Scanning must not be accessible from the standalone policy view.
    rule.onNodeWithTag("inspection_start_button").assertDoesNotExist()

    rule.onNodeWithTag("privacy_policy_back").performClick()
    rule.waitForIdle()

    rule.onNodeWithTag("hero_inspection_card").assertExists()
    rule.onNodeWithTag("inspection_start_button").assertExists()
    rule.onNodeWithTag("privacy_policy_open").assertExists()
  }
}
