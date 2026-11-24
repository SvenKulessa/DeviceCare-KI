package com.example.ui.theme

import com.example.data.DesignPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class UniverseBrandingTest {
  @Test fun firstLaunchDefaultsToOfficialUniverseBrand() {
    val defaults = DesignPreferences()
    assertEquals(ThemePalette.UNIVERSE, defaults.palette)
    assertEquals(ThemeMode.DARK, defaults.themeMode)
    assertEquals("DeviceCare KI · Universe", defaults.palette.displayName)
  }

  @Test fun approvedPaletteContainsSevenNamedColors() {
    assertEquals(androidx.compose.ui.graphics.Color(0xFF0B1435), UniverseBrand.Navy)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFF5C24B), UniverseBrand.Gold)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF00E6FF), UniverseBrand.Cyan)
  }
}
