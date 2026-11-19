package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.devicecare.AndroidHardwareInventory
import com.example.devicecare.AndroidHardwareSensorTester
import com.example.devicecare.HardwareSample
import com.example.devicecare.HardwareSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Inline section of the existing user-initiated local inspection; no cloud calls or persistence. */
@Composable
fun HardwareInspectionSection(inspectionTimestamp: Long, isStale: Boolean) {
  val context = LocalContext.current
  var snapshot by remember { mutableStateOf<HardwareSnapshot?>(null) }
  var inventoryError by remember { mutableStateOf<String?>(null) }
  var showAllSensors by remember { mutableStateOf(false) }
  var showAllFeatures by remember { mutableStateOf(false) }
  var activeSensorKey by remember { mutableStateOf<String?>(null) }
  val sensorResults = remember { mutableStateMapOf<String, HardwareSample>() }
  val scope = rememberCoroutineScope()

  LaunchedEffect(inspectionTimestamp) {
    snapshot = null
    inventoryError = null
    showAllSensors = false
    showAllFeatures = false
    activeSensorKey = null
    sensorResults.clear()
    try {
      snapshot = withContext(Dispatchers.IO) { AndroidHardwareInventory.collect(context) }
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: RuntimeException) {
      inventoryError = "Hardwareinventur fehlgeschlagen; keine Werte erfunden."
    }
  }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("hardware_inspection"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("Hardware & Gerätefunktionen · lokal",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() })
      Text(
        "CPU, RAM, interne Datenpartition, alle von Android gemeldeten Sensoren und Systemmerkmale. "
          + "Keine Gerätekennungen, kein Export und kein Hintergrundtest. "
          + "Erkennung ist kein Funktions- oder Defektnachweis.",
        style = MaterialTheme.typography.bodySmall,
      )
      if (isStale) {
        Text("VERALTET – für aktuelle Gerätewerte die lokale Prüfung erneut starten.",
          color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
      }
      if (inventoryError != null) {
        Text(inventoryError!!, color = MaterialTheme.colorScheme.error)
      }
      val data = snapshot
      if (data == null && inventoryError == null) {
        Text("Inventur wird lokal gelesen …", style = MaterialTheme.typography.bodyMedium)
      }
      if (data != null) {
        HorizontalDivider()
        data.metrics.forEach { metric ->
          Column {
            Text(metric.label, style = MaterialTheme.typography.labelMedium)
            Text(metric.value, style = MaterialTheme.typography.bodyMedium)
          }
        }
        Text("CPU-Kerne sind aus App-Sicht verfügbar, nicht zwingend physische SoC-Kerne. "
          + "RAM ist Kernel-RAM; Speicher bezieht sich nur auf die interne App-Datenpartition.",
          style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()

        Text("Sensoren (" + (data.sensorTotal?.toString() ?: "unbekannt") + " gemeldet)",
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.semantics { heading() })
        if (data.sensorError != null) {
          Text(data.sensorError, color = MaterialTheme.colorScheme.error)
        }
        if (data.sensorTotal != null && data.sensorTotal > data.sensors.size) {
          Text("Inventur begrenzt auf " + data.sensors.size + " Sensoren; Rest nicht geprüft.",
            style = MaterialTheme.typography.bodySmall)
        }
        if (data.sensorTotal == 0) {
          Text("Android meldet keine Sensoren.", style = MaterialTheme.typography.bodySmall)
        }
        data.sensors.take(if (showAllSensors) data.sensors.size else 12).forEachIndexed { index, sensor ->
          val key = sensor.type.toString() + "|" + sensor.name + "|" + sensor.vendor
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(sensor.name, style = MaterialTheme.typography.bodyMedium)
            Text(sensor.typeName + " · " + sensor.vendor,
              style = MaterialTheme.typography.labelSmall)
            if (sensor.canTestLocally) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilledTonalButton(
                  enabled = activeSensorKey == null && !isStale,
                  onClick = {
                    if (activeSensorKey == null) {
                      activeSensorKey = key
                      scope.launch {
                        try {
                          sensorResults[key] = AndroidHardwareSensorTester.sample(context, sensor)
                        } finally {
                          activeSensorKey = null
                        }
                      }
                    }
                  },
                  modifier = Modifier.testTag("hardware_test_sensor_" + index),
                ) {
                  Text(if (activeSensorKey == key) "Messung läuft …" else "Sensor einmal messen")
                }
              }
              Text("Nur nach Antippen; höchstens 3 Sekunden, normale Abtastrate.",
                style = MaterialTheme.typography.labelSmall)
            } else {
              Text("Automatischer Test nicht freigegeben (Berechtigung, Privatsphäre oder Sensortyp).",
                style = MaterialTheme.typography.labelSmall)
            }
            val observation = sensorResults[key]
            if (observation != null) {
              Text(observation.status.name + " · " + observation.message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            } else {
              Text("NOT_PROVEN · Funktion noch nicht getestet.",
                style = MaterialTheme.typography.labelSmall)
            }
          }
          HorizontalDivider()
        }
        if (data.sensors.size > 12) {
          FilledTonalButton(
            onClick = { showAllSensors = !showAllSensors },
            modifier = Modifier.testTag("hardware_expand_sensors"),
          ) { Text(if (showAllSensors) "Sensorliste kürzen" else "Alle Sensoren anzeigen") }
        }

        Text("Weitere Gerätefunktionen (" + (data.featureTotal?.toString() ?: "unbekannt") + " Einträge)",
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.semantics { heading() })
        if (data.featureError != null) {
          Text(data.featureError, color = MaterialTheme.colorScheme.error)
        }
        if (data.featureTotal != null && data.featureTotal > data.features.size) {
          Text("Systemmerkmal-Liste auf " + data.features.size + " Einträge begrenzt.",
            style = MaterialTheme.typography.bodySmall)
        }
        data.features.take(if (showAllFeatures) data.features.size else 12).forEach { feature ->
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(feature.label + ": " + if (feature.present) "ERKANNT" else "NICHT GEMELDET",
              style = MaterialTheme.typography.bodyMedium)
            if (feature.present) {
              Text("Testoption: " + feature.testHint + " · NOT_PROVEN bis zum echten Nutzertest.",
                style = MaterialTheme.typography.bodySmall)
            }
          }
        }
        if (data.features.size > 12) {
          FilledTonalButton(
            onClick = { showAllFeatures = !showAllFeatures },
            modifier = Modifier.testTag("hardware_expand_features"),
          ) { Text(if (showAllFeatures) "Funktionsliste kürzen" else "Alle Funktionen anzeigen") }
        }
        Text("Ein empfangenes Sensorereignis beweist weder korrekte Kalibrierung noch "
          + "ausfallsichere Hardware. Kamera, Mikrofon, Standort, Biometrie und "
          + "andere geschützte Funktionen werden nicht automatisch aktiviert.",
          style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}
