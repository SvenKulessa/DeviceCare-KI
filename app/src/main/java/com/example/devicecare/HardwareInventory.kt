package com.example.devicecare

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.StatFs
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** RAM-only information explicitly collected after the user starts local inspection. */
data class HardwareMetric(val label: String, val value: String)
data class HardwareSensor(
  val type: Int,
  val name: String,
  val vendor: String,
  val typeName: String,
  val canTestLocally: Boolean,
)
data class HardwareFeature(
  val id: String,
  val label: String,
  val present: Boolean,
  val testHint: String,
)
data class HardwareSnapshot(
  val observedAtMillis: Long,
  val metrics: List<HardwareMetric>,
  val sensors: List<HardwareSensor>,
  val sensorTotal: Int?,
  val features: List<HardwareFeature>,
  val featureTotal: Int?,
  val sensorError: String? = null,
  val featureError: String? = null,
)

enum class HardwareSampleStatus { OBSERVED, NOT_PROVEN, UNSUPPORTED, DENIED }

data class HardwareSample(val status: HardwareSampleStatus, val message: String)

/**
 * Only opt-in, single-event, low-rate samples from non-sensitive continuous/on-change sensors.
 * Trigger, motion-activity, health, step, body, vendor and location sensors are excluded.
 */
object HardwareTestPolicy {
  private val allowedTypes = setOf(
    Sensor.TYPE_ACCELEROMETER,
    Sensor.TYPE_MAGNETIC_FIELD,
    Sensor.TYPE_ORIENTATION,
    Sensor.TYPE_GYROSCOPE,
    Sensor.TYPE_LIGHT,
    Sensor.TYPE_PRESSURE,
    Sensor.TYPE_PROXIMITY,
    Sensor.TYPE_GRAVITY,
    Sensor.TYPE_LINEAR_ACCELERATION,
    Sensor.TYPE_ROTATION_VECTOR,
    Sensor.TYPE_RELATIVE_HUMIDITY,
    Sensor.TYPE_AMBIENT_TEMPERATURE,
    Sensor.TYPE_GAME_ROTATION_VECTOR,
    Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
  )
  fun canSample(sensorType: Int): Boolean = sensorType in allowedTypes

  fun status(registered: Boolean, values: FloatArray?): HardwareSampleStatus =
    if (registered && values != null && values.isNotEmpty() && values.all { it.isFinite() }) {
      HardwareSampleStatus.OBSERVED
    } else {
      HardwareSampleStatus.NOT_PROVEN
    }
}

internal object HardwareInventoryLimits {
  const val MAX_ENTRIES = 128

  /** Reserve space for known features before adding any unrecognized system flags. */
  fun additionalFeatureSlots(knownCount: Int): Int =
    (MAX_ENTRIES - knownCount.coerceAtLeast(0)).coerceAtLeast(0)
}

object AndroidHardwareInventory {

  private data class KnownFeature(val id: String, val label: String, val hint: String)
  private val knownFeatures = listOf(
    KnownFeature(PackageManager.FEATURE_CAMERA_ANY, "Kamera", "Kamera-App manuell öffnen, Vorschaubild und Foto prüfen."),
    KnownFeature(PackageManager.FEATURE_MICROPHONE, "Mikrofon", "Mit eigener Audio-App nach separater Einwilligung Testaufnahme prüfen."),
    KnownFeature(PackageManager.FEATURE_LOCATION_GPS, "GPS", "Standort in Android-Einstellungen prüfen; kein Standort wird gelesen."),
    KnownFeature(PackageManager.FEATURE_WIFI, "WLAN", "WLAN in Android-Einstellungen prüfen; kein Netzwerkscan."),
    KnownFeature(PackageManager.FEATURE_BLUETOOTH, "Bluetooth", "Bluetooth-Kopplung selbst prüfen; keine Gerätesuche."),
    KnownFeature(PackageManager.FEATURE_NFC, "NFC", "NFC-Einstellungen bzw. eigene Testkarte manuell prüfen."),
    KnownFeature(PackageManager.FEATURE_FINGERPRINT, "Fingerabdruck", "Biometrie in Android-Einstellungen prüfen; keine biometrischen Daten."),
    KnownFeature(PackageManager.FEATURE_USB_HOST, "USB-Host", "Mit eigenem USB-Zubehör und Einwilligung manuell prüfen."),
    KnownFeature(PackageManager.FEATURE_TOUCHSCREEN, "Touchscreen", "Bildschirmbereiche manuell antippen; keine Touch-Aufzeichnung."),
    KnownFeature(PackageManager.FEATURE_TELEPHONY, "Mobilfunk", "Mobilfunkstatus in Android-Einstellungen prüfen; keine SIM-IDs."),
    KnownFeature("android.hardware.audio.output", "Audioausgabe", "Lautsprecherausgabe mit eigener Testdatei prüfen."),
    KnownFeature("android.hardware.vibrator", "Vibration", "Haptik in Android-Einstellungen manuell prüfen."),
  )

  fun collect(context: Context): HardwareSnapshot {
    val metrics = mutableListOf(
      HardwareMetric("Hersteller / Modell", listOf(Build.MANUFACTURER, Build.MODEL).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Nicht verfügbar" }),
      HardwareMetric("Android / API", Build.VERSION.RELEASE + " / " + Build.VERSION.SDK_INT),
      HardwareMetric("CPU-Hardwarekennung", Build.HARDWARE.ifBlank { "Nicht verfügbar" }),
      HardwareMetric("CPU (für App verfügbare Kerne)", Runtime.getRuntime().availableProcessors().toString()),
      HardwareMetric("CPU-ABIs", Build.SUPPORTED_ABIS.joinToString(", ").ifBlank { "Nicht verfügbar" }),
    )
    try {
      val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
      if (manager == null) {
        metrics += HardwareMetric("RAM", "Nicht verfügbar (Dienst fehlt)")
      } else {
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)
        metrics += HardwareMetric("RAM (Kernel zugänglich)", formatBytes(memory.totalMem))
        metrics += HardwareMetric("RAM (aktuell verfügbar)", formatBytes(memory.availMem))
        metrics += HardwareMetric("Niedriger RAM", if (memory.lowMemory) "Von Android gemeldet" else "Nicht gemeldet")
      }
    } catch (_: RuntimeException) {
      metrics += HardwareMetric("RAM", "Abfrage fehlgeschlagen")
    }
    try {
      val storage = StatFs(context.filesDir.absolutePath)
      metrics += HardwareMetric("Interne Datenpartition", formatBytes(storage.totalBytes))
      metrics += HardwareMetric("Für Apps freier Speicher", formatBytes(storage.availableBytes))
    } catch (_: RuntimeException) {
      metrics += HardwareMetric("Speicherpartition", "Abfrage fehlgeschlagen")
    }

    var sensorError: String? = null
    val allSensors: List<Sensor>? = try {
      val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
      if (manager == null) {
        sensorError = "Sensor-Dienst nicht verfügbar"
        null
      } else manager.getSensorList(Sensor.TYPE_ALL)
    } catch (_: RuntimeException) {
      sensorError = "Sensorliste konnte nicht gelesen werden"
      null
    }
    val sensors = allSensors.orEmpty().take(HardwareInventoryLimits.MAX_ENTRIES).map {
      HardwareSensor(
        type = it.type,
        name = it.name,
        vendor = it.vendor,
        typeName = it.stringType,
        canTestLocally = HardwareTestPolicy.canSample(it.type),
      )
    }

    var featureError: String? = null
    var features: List<HardwareFeature> = emptyList()
    var featureTotal: Int? = null
    try {
      val pm = context.packageManager
      val observed = pm.systemAvailableFeatures.orEmpty().mapNotNull { it.name }.toSet()
      val knownIds = knownFeatures.map { it.id }.toSet()
      val known = knownFeatures.take(HardwareInventoryLimits.MAX_ENTRIES).map {
        HardwareFeature(it.id, it.label, pm.hasSystemFeature(it.id), it.hint)
      }
      val other = observed.filterNot { it in knownIds }.sorted()
        .take(HardwareInventoryLimits.additionalFeatureSlots(known.size)).map {
        HardwareFeature(it, it, true, "Systemmerkmal erkannt; kein sicherer automatischer Funktionstest verfügbar.")
      }
      features = known + other
      featureTotal = knownFeatures.size + observed.count { it !in knownIds }
    } catch (_: RuntimeException) {
      featureError = "Hardwaremerkmale konnten nicht gelesen werden"
      features = emptyList()
      featureTotal = null
    }
    return HardwareSnapshot(
      observedAtMillis = System.currentTimeMillis(),
      metrics = metrics,
      sensors = sensors,
      sensorTotal = allSensors?.size,
      features = features,
      featureTotal = featureTotal,
      sensorError = sensorError,
      featureError = featureError,
    )
  }

  private fun formatBytes(bytes: Long): String =
    if (bytes <= 0) "Nicht verfügbar"
    else String.format(Locale.GERMANY, "%.2f GiB", bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
}

/** Prevent sensor observations from an earlier inspection being shown as current evidence. */
object HardwareSamplePublicationPolicy {
  fun isCurrent(
    requestedEpoch: Int,
    currentEpoch: Int,
    requestedInspectionAt: Long,
    currentInspectionAt: Long,
    isStale: Boolean,
  ): Boolean =
    requestedEpoch == currentEpoch &&
      requestedInspectionAt == currentInspectionAt &&
      !isStale
}

object AndroidHardwareSensorTester {
  /** Call only from a visible, user-pressed sensor button; never from the scan itself. */
  suspend fun sample(context: Context, descriptor: HardwareSensor): HardwareSample =
    withContext(Dispatchers.Main.immediate) {
      if (!HardwareTestPolicy.canSample(descriptor.type)) {
        return@withContext HardwareSample(HardwareSampleStatus.UNSUPPORTED, "Nur manuell bzw. mit gesonderter Berechtigung prüfbar.")
      }
      val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        ?: return@withContext HardwareSample(HardwareSampleStatus.UNSUPPORTED, "Sensor-Dienst fehlt.")
      val sensor = manager.getSensorList(Sensor.TYPE_ALL).firstOrNull {
        it.type == descriptor.type && it.name == descriptor.name && it.vendor == descriptor.vendor
      } ?: return@withContext HardwareSample(HardwareSampleStatus.UNSUPPORTED, "Sensor nicht mehr verfügbar.")
      val deferred = CompletableDeferred<FloatArray>()
      val listener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        override fun onSensorChanged(event: SensorEvent) {
          if (!deferred.isCompleted) deferred.complete(event.values.copyOf())
        }
      }
      var registered = false
      try {
        registered = manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        if (!registered) {
          HardwareSample(HardwareSampleStatus.NOT_PROVEN, "Sensorregistrierung fehlgeschlagen.")
        } else {
          val values = withTimeoutOrNull(3_000L) { deferred.await() }
          if (HardwareTestPolicy.status(registered, values) == HardwareSampleStatus.OBSERVED) {
            val rounded = values!!.take(3).joinToString(", ") {
              String.format(Locale.GERMANY, "%.2f", it)
            }
            HardwareSample(HardwareSampleStatus.OBSERVED,
              "Messereignis empfangen (" + rounded + "). Keine Aussage zu Kalibrierung oder Defekten.")
          } else {
            HardwareSample(HardwareSampleStatus.NOT_PROVEN,
              "Kein gültiges Messereignis in 3 Sekunden; kein Defektnachweis.")
          }
        }
      } catch (_: SecurityException) {
        HardwareSample(HardwareSampleStatus.DENIED, "Android hat den Sensorzugriff verweigert.")
      } catch (cancelled: CancellationException) {
        // Parent cancellation must propagate; finally unregisters the listener.
        throw cancelled
      } catch (_: RuntimeException) {
        HardwareSample(HardwareSampleStatus.NOT_PROVEN, "Messung nicht verfügbar.")
      } finally {
        if (registered) manager.unregisterListener(listener)
      }
    }
}
