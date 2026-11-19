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
  fun validEventIsObservedButNeverHardwareHealthVerified() {
    assertEquals(HardwareSampleStatus.OBSERVED, HardwareTestPolicy.status(true, floatArrayOf(0.0f, 9.81f, 0.0f)))
  }
}
