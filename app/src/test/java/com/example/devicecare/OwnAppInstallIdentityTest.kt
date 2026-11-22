package com.example.devicecare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic certificate bytes; no device identity, installed APK, or actual signer is implied. */
class OwnAppInstallIdentityTest {
  @Test
  fun knownSha256IsStableAndNeverReportsApkHash() {
    val snapshot =
      ownAppIdentityFromCertificates("com.example.test", "1.0", 1, listOf("abc".toByteArray()))
    assertEquals("OBSERVED", snapshot.evidenceStatus)
    assertEquals("com.example.test", snapshot.packageName)
    assertEquals(
      listOf("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
      snapshot.signerFingerprintsSha256,
    )
    assertNull(snapshot.limitationCode)
  }

  @Test
  fun missingCertificatesFailClosedEvenWhenVersionExists() {
    val snapshot = ownAppIdentityFromCertificates("com.example.test", "1.0", 1, emptyList())
    assertEquals("NOT_PROVEN", snapshot.evidenceStatus)
    assertEquals("NO_SIGNING_CERTIFICATE", snapshot.limitationCode)
    assertTrue(snapshot.signerFingerprintsSha256.isEmpty())
    assertFalse(snapshot.evidenceStatus == "VERIFIED")
  }

  @Test
  fun duplicateAndEmptyCertificateBytesNeverInflateSignerCount() {
    val snapshot = ownAppIdentityFromCertificates(
      "com.example.test", null, 42,
      listOf(byteArrayOf(), "abc".toByteArray(), "abc".toByteArray()),
    )
    assertEquals(1, snapshot.signerFingerprintsSha256.size)
    assertEquals(42L, snapshot.versionCode)
  }
}
