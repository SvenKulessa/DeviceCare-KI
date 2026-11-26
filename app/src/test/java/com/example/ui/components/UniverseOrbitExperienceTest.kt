package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.DeviceCareUiState
import com.example.ui.theme.FormwerkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UniverseOrbitExperienceTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun universeCategoriesHaveExactlySevenDistinctColors() {
    val categories = UniverseDiagnosticCategory.entries
    assertEquals(7, categories.size)
    assertEquals(7, categories.map { it.accentColor }.toSet().size)
    assertEquals(7, categories.map { it.id }.toSet().size)
  }

  @Test
  fun orbitCardAndMatrixRenderAllSevenCategoriesAndTriggerSelectionAndAction() {
    val selectedCalls = mutableListOf<UniverseDiagnosticCategory>()
    val triggeredCalls = mutableListOf<UniverseDiagnosticCategory>()
    var planetTapCount = 0
    var activeSnapshot = UniverseDiagnosticCategory.SYSTEM_HEALTH

    rule.setContent {
      var active by remember { mutableStateOf(UniverseDiagnosticCategory.SYSTEM_HEALTH) }
      FormwerkTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          InteractiveUniverseOrbitCard(
            uiState = DeviceCareUiState(),
            selectedCategory = active,
            planetPulseNonce = 1,
            onSelectCategory = {
              selectedCalls.add(it)
              active = it
              activeSnapshot = it
            },
            onPlanetTap = { planetTapCount++ },
            onTriggerCategoryAction = { triggeredCalls.add(it) },
          )
          InteractiveDiagnosticMatrixCard(
            uiState = DeviceCareUiState(),
            selectedCategory = active,
            onSelectCategory = {
              selectedCalls.add(it)
              active = it
              activeSnapshot = it
            },
          )
        }
      }
    }
    rule.waitForIdle()

    rule.onNodeWithTag("interactive_universe_orbit_card").assertExists()
    rule.onNodeWithTag("universe_planet_core").assertExists()
    rule.onNodeWithTag("universe_orbit_segments_row").assertExists()
    rule.onNodeWithTag("diagnostic_matrix_card").assertExists()

    UniverseDiagnosticCategory.entries.forEach { cat ->
      rule.onNodeWithTag("universe_orbit_segment_${cat.id}").assertExists()
      rule.onNodeWithTag("matrix_cell_${cat.id}").assertExists()
    }

    rule.onNodeWithTag("universe_planet_core").performScrollTo().performClick()
    rule.waitForIdle()
    assertEquals(1, planetTapCount)

    rule
      .onNodeWithTag("universe_orbit_segment_${UniverseDiagnosticCategory.NETWORK_RETHINK.id}")
      .performScrollTo()
      .performClick()
    rule.waitForIdle()
    assertEquals(UniverseDiagnosticCategory.NETWORK_RETHINK, selectedCalls.last())

    rule
      .onNodeWithTag("matrix_cell_${UniverseDiagnosticCategory.BATTERY.id}")
      .performScrollTo()
      .performClick()
    rule.waitForIdle()
    assertEquals(UniverseDiagnosticCategory.BATTERY, selectedCalls.last())

    rule.onNodeWithTag("universe_orbit_action_button").performScrollTo().performClick()
    rule.waitForIdle()
    assertEquals(listOf(activeSnapshot), triggeredCalls)
  }
}
