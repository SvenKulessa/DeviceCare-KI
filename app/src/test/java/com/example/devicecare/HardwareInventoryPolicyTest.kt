package com.example.devicecare

import android.hardware.Sensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareInventoryPolicyTest {
  @Test
  fun passiveSensorsAreExplicitlyWhitelisted() {
    assertTrue(HardwareTestPolicy.canSample(Sensor.TYPE_ACCELEROMETER))
    assertTrue(HardwareTestPolicy.canSample(Sensor.TYPE_LIGHT))
    assertTrue(HardwareTestPolicy.canSample(Sensor.TYPE_PROXIMITY))
  }

  @Test
  fun protectedMotionHealthAndUnknownSensorsFailClosed() {
    assertFalse(HardwareTestPolicy.canSample(Sensor.TYPE_HEART_RATE))
    assertFalse(HardwareTestPolicy.canSample(Sensor.TYPE_STEP_COUNTER))
    assertFalse(HardwareTestPolicy.canSample(Sensor.TYPE_SIGNIFICANT_MOTION))
    assertFalse(HardwareTestPolicy.canSample(65_535))
  }

  @Test
  fun registrationDoesNotProveFunctionalityWithoutFiniteEvent() {
    assertEquals(HardwareSampleStatus.NOT_PROVEN, HardwareTestPolicy.status(false, floatArrayOf(1f)))
    assertEquals(HardwareSampleStatus.NOT_PROVEN, HardwareTestPolicy.status(true, null))
    assertEquals(HardwareSampleStatus.NOT_PROVEN, HardwareTestPolicy.status(true, floatArrayOf()))
    assertEquals(HardwareSampleStatus.NOT_PROVEN, HardwareTestPolicy.status(true, floatArrayOf(Float.NaN)))
    assertEquals(HardwareSampleStatus.NOT_PROVEN, HardwareTestPolicy.status(true, floatArrayOf(Float.POSITIVE_INFINITY)))
  }

  @Test
  fun staleAndSupersededSamplesNeverBecomeCurrentEvidence() {
    assertTrue(HardwareSamplePublicationPolicy.isCurrent(4, 4, 1000L, 1000L, false))
    // An older callback must fail closed even if two scans share a millisecond timestamp.
    assertFalse(HardwareSamplePublicationPolicy.isCurrent(4, 5, 1000L, 1000L, false))
    assertFalse(HardwareSamplePublicationPolicy.isCurrent(4, 4, 1000L, 1001L, false))
    assertFalse(HardwareSamplePublicationPolicy.isCurrent(4, 4, 1000L, 1000L, true))
  }

  @Test
  fun validEventIsObservedButNeverHardwareHealthVerified() {
    assertEquals(HardwareSampleStatus.OBSERVED, HardwareTestPolicy.status(true, floatArrayOf(0.0f, 9.81f, 0.0f)))
  }
}
