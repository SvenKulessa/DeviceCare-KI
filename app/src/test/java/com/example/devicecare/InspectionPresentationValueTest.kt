package com.example.devicecare

import com.example.core.evidence.ObservationStatus
import com.example.features.battery.BatteryEvidenceMapper
import com.example.features.battery.BatteryReading
import com.example.features.permissions.PermissionEvidenceMapper
import com.example.features.permissions.PermissionReading
import com.example.features.permissions.PermissionScanBudget
import com.example.features.permissions.PermissionScanProfile
import com.example.features.permissions.ResourceSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class InspectionPresentationValueTest {
  private val now = 10_000L
  private val profile =
    PermissionScanProfile(
      apiLevel = 35,
      observedAtMillis = now,
      lowRam = ResourceSignal.CLEAR,
      memoryPressure = ResourceSignal.CLEAR,
      powerSaver = ResourceSignal.CLEAR,
      thermal = ResourceSignal.CLEAR,
    )

  private fun summary(
    batteryStatus: ObservationStatus? = null,
    permissionStatus: ObservationStatus? = null,
  ): InspectionResult {
    val battery =
      if (batteryStatus == null)
        BatteryEvidenceMapper.map(
          now,
          BatteryReading(
            level = 75,
            scale = 100,
            capacityPercent = null,
            temperatureTenthsCelsius = 315,
            chargingStatus = 2,
            healthCode = 2,
          ),
        )
      else BatteryEvidenceMapper.missing(now, batteryStatus, "OEM_UNAVAILABLE")
    val permissions =
      if (permissionStatus == null)
        PermissionEvidenceMapper.map(
          now,
          PermissionReading(
            visiblePackages = 12,
            sampledPackages = 7,
            requestedInSample = 14,
            ownRequested = 1,
            ownGranted = 1,
            truncatedByLimit = true,
          ),
        )
      else PermissionEvidenceMapper.missing(now, permissionStatus, "ACCESS_DENIED")
    return presentInspection(
      profile = profile,
      budget = PermissionScanBudget.forProfile(profile),
      battery = battery,
      permissions = permissions,
      optionalReadsDeferred = false,
      elapsedMs = 178,
      cpuMs = 448,
    )
  }

  private fun InspectionResult.metric(label: String): String =
    profileMetrics.single { it.first == label }.second

  @Test
  fun observedValuesShowActualAndroidSignalsWithExplicitUnits() {
    val output = summary()
    assertEquals("75 %", output.metric("Akkustand"))
    assertEquals("Lädt", output.metric("Ladezustand"))
    assertEquals("31.5 °C", output.metric("Akkutemperatur"))
    assertEquals(
      "Gut (Android-Systemmeldung)",
      output.metric("Android-Akkuzustand (nur Systemstatus)"),
    )
    assertEquals("7 Apps", output.metric("App-Stichprobe (nur Android-sichtbar)"))
    assertEquals(178L, output.elapsedMs)
    assertEquals(448L, output.cpuMs)
  }

  @Test
  fun unavailableOrDeniedSignalsDoNotProduceNumbersOrClaimSuccess() {
    val output = summary(
      batteryStatus = ObservationStatus.UNSUPPORTED,
      permissionStatus = ObservationStatus.DENIED,
    )
    assertEquals("nicht verfügbar", output.metric("Akkustand"))
    assertEquals("nicht verfügbar", output.metric("Ladezustand"))
    assertEquals("nicht verfügbar", output.metric("Akkutemperatur"))
    assertEquals(
      "nicht verfügbar",
      output.metric("Android-Akkuzustand (nur Systemstatus)"),
    )
    assertEquals(
      "Zugriff verweigert",
      output.metric("App-Stichprobe (nur Android-sichtbar)"),
    )
    assertFalse(output.profileMetrics.any { (_, value) -> value.contains("von Android gemeldet") })
  }
}
