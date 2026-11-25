package com.example.devicecare

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import com.example.core.evidence.DeviceObservation
import com.example.core.evidence.DeviceObservationBatch
import com.example.core.evidence.ObservationStatus
import com.example.features.permissions.PermissionScanBudget
import com.example.features.permissions.PermissionScanProfile
import com.example.features.permissions.PermissionScanReason
import com.example.features.permissions.ResourceSignal
import java.util.Locale

data class ObservationDisplayRow(
  val id: String,
  val label: String,
  val valueText: String,
  val status: ObservationStatus,
  val limitationCode: String,
)

data class SystemHealthSnapshot(
  val storageTotalBytes: Long? = null,
  val storageAvailableBytes: Long? = null,
  val memoryTotalBytes: Long? = null,
  val memoryAvailableBytes: Long? = null,
  val lowMemoryReported: Boolean = false,
) {
  val storageUsedBytes: Long?
    get() =
      if (storageTotalBytes != null && storageAvailableBytes != null && storageTotalBytes > 0L) {
        (storageTotalBytes - storageAvailableBytes).coerceAtLeast(0L)
      } else null

  val storageUsedFraction: Float?
    get() =
      if (storageTotalBytes != null && storageUsedBytes != null && storageTotalBytes > 0L) {
        (storageUsedBytes!!.toFloat() / storageTotalBytes.toFloat()).coerceIn(0f, 1f)
      } else null

  val memoryUsedBytes: Long?
    get() =
      if (memoryTotalBytes != null && memoryAvailableBytes != null && memoryTotalBytes > 0L) {
        (memoryTotalBytes - memoryAvailableBytes).coerceAtLeast(0L)
      } else null

  val memoryUsedFraction: Float?
    get() =
      if (memoryTotalBytes != null && memoryUsedBytes != null && memoryTotalBytes > 0L) {
        (memoryUsedBytes!!.toFloat() / memoryTotalBytes.toFloat()).coerceIn(0f, 1f)
      } else null

  fun formattedStorageUsed(): String = formatBytesGiB(storageUsedBytes)
  fun formattedStorageFree(): String = formatBytesGiB(storageAvailableBytes)
  fun formattedStorageTotal(): String = formatBytesGiB(storageTotalBytes)
  fun formattedMemoryUsed(): String = formatBytesGiB(memoryUsedBytes)
  fun formattedMemoryFree(): String = formatBytesGiB(memoryAvailableBytes)
  fun formattedMemoryTotal(): String = formatBytesGiB(memoryTotalBytes)

  companion object {
    fun formatBytesGiB(bytes: Long?): String {
      if (bytes == null || bytes < 0L) return "Nicht verfügbar"
      val gib = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
      return String.format(Locale.GERMANY, "%.1f GiB", gib)
    }
  }
}

fun collectSystemHealthSnapshot(context: Context): SystemHealthSnapshot {
  var memTotal: Long? = null
  var memAvail: Long? = null
  var lowMem = false
  try {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    if (activityManager != null) {
      val info = ActivityManager.MemoryInfo()
      activityManager.getMemoryInfo(info)
      if (info.totalMem > 0L) {
        memTotal = info.totalMem
        memAvail = info.availMem.coerceAtLeast(0L)
      }
      lowMem = info.lowMemory
    }
  } catch (_: RuntimeException) {
    // Fail closed: keep unavailable fields null.
  }

  var storeTotal: Long? = null
  var storeAvail: Long? = null
  try {
    val stat = StatFs(context.filesDir.absolutePath)
    if (stat.totalBytes > 0L) {
      storeTotal = stat.totalBytes
      storeAvail = stat.availableBytes.coerceAtLeast(0L)
    }
  } catch (_: RuntimeException) {
    // Fail closed: keep unavailable fields null.
  }

  return SystemHealthSnapshot(
    storageTotalBytes = storeTotal,
    storageAvailableBytes = storeAvail,
    memoryTotalBytes = memTotal,
    memoryAvailableBytes = memAvail,
    lowMemoryReported = lowMem,
  )
}

data class InspectionResult(
  val collectedAtMillis: Long = 0L,
  val apiLevel: Int = 0,
  val packageLimit: Int = PermissionScanBudget.STANDARD_LIMIT,
  val budgetSummary: String = "",
  val optionalReadsDeferred: Boolean = false,
  val elapsedMs: Long = 0L,
  val cpuMs: Long = 0L,
  val profileMetrics: List<Pair<String, String>> = emptyList(),
  val batteryObservations: List<ObservationDisplayRow> = emptyList(),
  val permissionObservations: List<ObservationDisplayRow> = emptyList(),
  val batteryPercent: Int? = null,
  val batteryChargingState: String? = null,
  val batteryTemperatureDisplay: String? = null,
  val systemHealthSnapshot: SystemHealthSnapshot? = null,
  val errorMessage: String? = null,
)

fun presentInspection(
  profile: PermissionScanProfile,
  budget: PermissionScanBudget,
  battery: DeviceObservationBatch,
  permissions: DeviceObservationBatch,
  optionalReadsDeferred: Boolean,
  elapsedMs: Long,
  cpuMs: Long,
  systemHealthSnapshot: SystemHealthSnapshot? = null,
): InspectionResult {
  val batteryPercentObs = battery.observations.firstOrNull { it.id == "battery_percent" }
  val batteryPercent =
    if (batteryPercentObs?.status == ObservationStatus.OBSERVED) {
      batteryPercentObs.value?.toIntOrNull()
    } else null
  val chargingObs = battery.observations.firstOrNull { it.id == "battery_charging" }
  val chargingState =
    if (chargingObs?.status == ObservationStatus.OBSERVED) chargingObs.value else null
  val tempText = battery.observedValueText("battery_temperature")

  return InspectionResult(
    collectedAtMillis = maxOf(battery.collectedAtMillis, permissions.collectedAtMillis),
    apiLevel = profile.apiLevel,
    packageLimit = budget.packageLimit,
    budgetSummary = budget.describe(profile.apiLevel),
    optionalReadsDeferred = optionalReadsDeferred,
    elapsedMs = elapsedMs,
    cpuMs = cpuMs,
    profileMetrics =
      listOf(
        "RAM-Geräteklasse" to profile.lowRam.describe("kein Low-RAM-Gerät", "Low-RAM-Gerät"),
        "Speicherdruck" to profile.memoryPressure.describe("nicht gemeldet", "aktiv"),
        "Energiesparmodus" to profile.powerSaver.describe("aus", "ein"),
        "Thermische Belastung" to profile.thermal.describe("keine oder leicht", "moderat"),
        "Akkustand" to battery.observedValueText("battery_percent"),
        "Ladezustand" to battery.observedValueText("battery_charging"),
        "Akkutemperatur" to tempText,
        "Android-Akkuzustand (nur Systemstatus)" to
          battery.observedValueText("battery_health_reported"),
        "App-Stichprobe (nur Android-sichtbar)" to
          permissions.observedValueText("sampled_packages"),
      ),
    batteryObservations = battery.observations.map { it.toDisplayRow() },
    permissionObservations = permissions.observations.map { it.toDisplayRow() },
    batteryPercent = batteryPercent,
    batteryChargingState = chargingState,
    batteryTemperatureDisplay = tempText,
    systemHealthSnapshot = systemHealthSnapshot,
  )
}

private fun DeviceObservation.toDisplayRow(): ObservationDisplayRow {
  val label =
    when (id) {
      "battery_percent" -> "Akkuladestand"
      "battery_temperature" -> "Akkutemperatur"
      "battery_charging" -> "Ladestatus"
      "battery_health_reported" -> "System-Akkuzustand"
      "visible_packages" -> "Sichtbare Apps (OS-gefiltert)"
      "sampled_packages" -> "Geprüfte Stichprobe"
      "sampled_requested_permissions" -> "Angeforderte Berechtigungen (Stichprobe)"
      "own_requested_permissions" -> "Eigene angeforderte Berechtigungen"
      "own_granted_permissions" -> "Eigene gewährte Berechtigungen"
      "sample_truncated" -> "Stichprobe gekürzt"
      else -> id
    }
  val display =
    if (status == ObservationStatus.OBSERVED) {
      "${value}${unit?.let { " $it" } ?: ""}"
    } else {
      "${status.name} ($limitationCode)"
    }
  return ObservationDisplayRow(
    id = id,
    label = label,
    valueText = display,
    status = status,
    limitationCode = limitationCode,
  )
}

private fun ResourceSignal.describe(clear: String, active: String): String =
  when (this) {
    ResourceSignal.CLEAR -> clear
    ResourceSignal.ACTIVE -> active
    ResourceSignal.HIGH -> "hoch"
    ResourceSignal.UNSUPPORTED -> "nicht unterstützt"
    ResourceSignal.DENIED -> "Zugriff verweigert"
    ResourceSignal.ERROR -> "Abfrage fehlgeschlagen"
    ResourceSignal.UNKNOWN -> "unbekannt"
  }

/** Display only allowlisted, actually observed values. Never infer an unavailable signal. */
private fun DeviceObservationBatch.observedValueText(id: String): String {
  val observation = observations.firstOrNull { it.id == id }
  return when (observation?.status) {
    ObservationStatus.OBSERVED -> {
      val raw = observation.value ?: return "unbekannt"
      val translated =
        when (id) {
          "battery_charging" ->
            when (raw) {
              "CHARGING" -> "Lädt"
              "DISCHARGING" -> "Entlädt"
              "NOT_CHARGING" -> "Lädt nicht"
              "FULL" -> "Voll geladen"
              else -> raw
            }
          "battery_health_reported" ->
            when (raw) {
              "GOOD" -> "Gut (Android-Systemmeldung)"
              "OVERHEAT" -> "Überhitzt (Android-Systemmeldung)"
              "DEAD" -> "Defekt (Android-Systemmeldung)"
              "OVER_VOLTAGE" -> "Überspannung (Android-Systemmeldung)"
              "UNSPECIFIED_FAILURE" -> "Fehler (Android-Systemmeldung)"
              "COLD" -> "Kalt (Android-Systemmeldung)"
              else -> raw
            }
          else -> raw
        }
      when (observation.unit) {
        "%" -> "$translated %"
        "C" -> "$translated °C"
        "apps" -> "$translated Apps"
        "count" -> translated
        null -> translated
        else -> "$translated ${observation.unit}"
      }
    }
    ObservationStatus.UNSUPPORTED -> "nicht verfügbar"
    ObservationStatus.DENIED -> "Zugriff verweigert"
    ObservationStatus.ERROR -> "Abfrage fehlgeschlagen"
    null -> "unbekannt"
  }
}

private fun PermissionScanBudget.describe(apiLevel: Int): String {
  val reason =
    if (reasons.isEmpty()) "Standardumfang"
    else
      reasons.joinToString(", ") {
        when (it) {
          PermissionScanReason.MEMORY_PRESSURE -> "Speicherdruck"
          PermissionScanReason.THERMAL_PRESSURE -> "hohe thermische Belastung"
          PermissionScanReason.LOW_RAM -> "Gerät mit wenig RAM"
          PermissionScanReason.POWER_SAVER -> "Energiesparmodus"
          PermissionScanReason.MODERATE_THERMAL -> "thermische Belastung"
          PermissionScanReason.SIGNAL_UNAVAILABLE -> "Ressourcensignale teilweise nicht verfügbar"
        }
      }
  return "Prüfumfang (Android API $apiLevel): bis zu $packageLimit sichtbare Apps · $reason."
}
