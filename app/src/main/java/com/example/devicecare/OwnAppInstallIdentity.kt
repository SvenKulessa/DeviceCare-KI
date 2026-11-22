package com.example.devicecare

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * User-triggered, in-memory self-package identity only.
 * Does not read other apps, store diagnostics, or claim APK hash/update readiness.
 * OBSERVED means Android PackageManager returned the current signer; it is not
 * an independent installation/build or release-identity verification.
 */
internal data class OwnAppInstallIdentity(
  val packageName: String,
  val versionName: String?,
  val versionCode: Long?,
  val signerFingerprintsSha256: List<String>,
  val evidenceStatus: String,
  val limitationCode: String? = null,
)

internal fun ownAppIdentityFromCertificates(
  packageName: String,
  versionName: String?,
  versionCode: Long,
  signerCertificates: List<ByteArray>,
): OwnAppInstallIdentity {
  val hashes =
    signerCertificates
      .filter { it.isNotEmpty() }
      .map { bytes ->
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
          "%02x".format(it.toInt() and 0xff)
        }
      }
      .distinct()
      .sorted()
  return OwnAppInstallIdentity(
    packageName = packageName,
    versionName = versionName,
    versionCode = versionCode,
    signerFingerprintsSha256 = hashes,
    evidenceStatus = if (hashes.isEmpty()) "NOT_PROVEN" else "OBSERVED",
    limitationCode = if (hashes.isEmpty()) "NO_SIGNING_CERTIFICATE" else null,
  )
}

@Suppress("DEPRECATION")
internal fun readOwnAppInstallIdentity(context: Context): OwnAppInstallIdentity {
  val packageName = context.packageName
  return try {
    val pm = context.packageManager
    val flags =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES
      else PackageManager.GET_SIGNATURES
    val info = pm.getPackageInfo(packageName, flags)
    val certificates =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.signingInfo?.apkContentsSigners?.map { it.toByteArray() } ?: emptyList()
      } else {
        info.signatures?.map { it.toByteArray() } ?: emptyList()
      }
    ownAppIdentityFromCertificates(
      packageName = packageName,
      versionName = info.versionName,
      versionCode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
        else info.versionCode.toLong(),
      signerCertificates = certificates,
    )
  } catch (_: PackageManager.NameNotFoundException) {
    OwnAppInstallIdentity(packageName, null, null, emptyList(), "NOT_PROVEN", "PACKAGE_NOT_FOUND")
  } catch (_: SecurityException) {
    OwnAppInstallIdentity(packageName, null, null, emptyList(), "NOT_PROVEN", "ACCESS_DENIED")
  } catch (_: RuntimeException) {
    OwnAppInstallIdentity(packageName, null, null, emptyList(), "NOT_PROVEN", "PACKAGE_READ_ERROR")
  }
}
