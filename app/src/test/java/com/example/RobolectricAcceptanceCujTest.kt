package com.example

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric (SDK 36 JVM) end-to-end CUJ acceptance checks.
 * Note: This verifies Compose/ViewModel/Android-JVM semantics only — real Android Emulator / ADB
 * and physical Android-15 OEM acceptance remain external owner gates.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RobolectricAcceptanceCujTest {

  @get:Rule val rule = createAndroidComposeRule<MainActivity>()

  @Test
  fun qa01NoAutoScanBeforeTapAndManualInspectionRevealsHardwareInventory() {
    rule.waitForIdle()
    rule.onNodeWithTag("hero_inspection_card").assertExists()
    rule.onNodeWithTag("hardware_inspection").assertDoesNotExist()

    rule.onNodeWithTag("inspection_start_button").performClick()
    rule.waitUntil(timeoutMillis = 5_000) {
      runCatching {
          rule
            .onNodeWithTag("devicecare_scroll_content")
            .performScrollToNode(hasTestTag("hardware_inspection"))
          true
        }
        .getOrDefault(false)
    }
    rule.onNodeWithTag("hardware_inspection").assertExists()
  }

  @Test
  fun systemSecurityWorkflowRequiresExplicitStartAndReturnsToHome() {
    rule.waitForIdle()
    rule.onNodeWithTag("open_system_security_button").performScrollTo().performClick()
    rule.waitForIdle()

    rule.onNodeWithTag("system_security_workflow").assertExists()
    rule.onNodeWithText("Noch keine Prüfung ausgeführt (NOT_PROVEN).").assertExists()

    rule.onNodeWithTag("system_security_start").performClick()
    rule.waitUntil(timeoutMillis = 5_000) {
      runCatching {
          rule.onAllNodes(hasText("Lokale Evidence")).fetchSemanticsNodes().isNotEmpty()
        }
        .getOrDefault(false)
    }
    rule.onNodeWithText("Lokale Evidence").assertExists()

    rule.onNodeWithTag("system_security_back").performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("hero_inspection_card").assertExists()
  }

  @Test
  fun ownAppInstallIdentityReadsOnlyAfterExplicitTap() {
    rule.waitForIdle()
    rule
      .onNodeWithTag("devicecare_scroll_content")
      .performScrollToNode(hasTestTag("own_app_identity_card"))
    rule.onNodeWithTag("own_app_identity_readback").assertDoesNotExist()

    rule.onNodeWithTag("own_app_identity_read_button").performScrollTo().performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("own_app_identity_readback").assertExists()
  }
}
