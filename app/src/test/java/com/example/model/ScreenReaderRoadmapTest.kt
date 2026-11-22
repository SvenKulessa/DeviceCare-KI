package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenReaderRoadmapTest {
  @Test
  fun `qa04 requires independent real device screenreader evidence`() {
    val qa = DeviceCareRoadmapData.tasks.single { it.id == "qa-4-a11y" }
    assertEquals(GovernanceStatus.NOT_PROVEN, qa.status)
    assertTrue(qa.blockingRelease)
    // Local checkboxes cannot substitute observed screenreader speech and focus.
    assertFalse(qa.canMarkVerifiedLocally)
    assertTrue(qa.title.contains("Screenreader"))
    assertTrue(qa.sourceDoc.contains("SCREENREADER-QA.md"))
    assertTrue(qa.acceptanceCriteria.any { it.contains("Screenreader") })
  }

  @Test
  fun `silent physical screenreader video is allowed for observable app functions`() {
    val qa = DeviceCareRoadmapData.tasks.single { it.id == "qa-4-a11y" }
    assertTrue(qa.acceptanceCriteria.any { it.contains("Stumme Screenreader-Aufnahmen") })
    assertTrue(qa.nonNegotiableBoundary.contains("hörbare Ansagen"))
    assertTrue(qa.nonNegotiableBoundary.contains("NOT_PROVEN"))
    assertFalse(qa.canMarkVerifiedLocally)
  }

  @Test
  fun `emulator semantics remain distinct from screenreader acceptance`() {
    val emulator = DeviceCareRoadmapData.tasks.single { it.id == "rem-p1-emu" }
    assertEquals(GovernanceStatus.IN_PROGRESS, emulator.status)
    assertTrue(emulator.description.contains("keine manuelle Screenreader-Prüfung"))
    assertFalse(emulator.description.contains("TalkBack", ignoreCase = true))
  }

  @Test
  fun `roadmap uses vendor neutral screenreader wording`() {
    val text = DeviceCareRoadmapData.tasks.joinToString(" ") {
      listOf(it.title, it.subtitle, it.description, it.acceptanceCriteria.joinToString(" ")).joinToString(" ")
    }
    assertFalse(text.contains("TalkBack", ignoreCase = true))
    assertTrue(text.contains("Screenreader"))
  }
}
