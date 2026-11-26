package com.example.features.battery

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object LowBatteryAlertEvaluator {
  const val LOW_BATTERY_THRESHOLD_PERCENT = 20

  data class AlertDecision(
    val shouldNotify: Boolean,
    val nextAlertedState: Boolean,
  )

  /**
   * Pure hysteresis check for low-battery alerts.
   * Triggers strictly once when enabled, battery is < 20%, and not charging/full.
   * Resets once charge rises to >= 20% or a charger is connected.
   */
  fun evaluate(
    percent: Int?,
    chargingStatus: ChargingStatus,
    alertsEnabled: Boolean,
    alreadyAlertedBelowThreshold: Boolean,
  ): AlertDecision {
    if (!alertsEnabled) {
      return AlertDecision(shouldNotify = false, nextAlertedState = false)
    }
    if (percent == null) {
      return AlertDecision(shouldNotify = false, nextAlertedState = alreadyAlertedBelowThreshold)
    }
    if (chargingStatus == ChargingStatus.CHARGING || chargingStatus == ChargingStatus.FULL) {
      return AlertDecision(shouldNotify = false, nextAlertedState = false)
    }
    if (percent >= LOW_BATTERY_THRESHOLD_PERCENT) {
      return AlertDecision(shouldNotify = false, nextAlertedState = false)
    }
    return if (!alreadyAlertedBelowThreshold) {
      AlertDecision(shouldNotify = true, nextAlertedState = true)
    } else {
      AlertDecision(shouldNotify = false, nextAlertedState = true)
    }
  }
}

class LowBatteryAlertNotifier(private val context: Context) {

  fun hasNotificationPermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

  fun ensureChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      ?: return
    val existing = manager.getNotificationChannel(CHANNEL_ID)
    if (existing != null) return

    val channel =
      NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.low_battery_alert_channel_name),
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = context.getString(R.string.low_battery_alert_channel_desc)
      }
    manager.createNotificationChannel(channel)
  }

  @SuppressLint("MissingPermission")
  fun notifyLowBattery(percent: Int): Boolean {
    if (!hasNotificationPermission()) return false
    ensureChannel()

    val launchIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_battery_alert_notification)
        .setContentTitle("Niedriger Akkustand ($percent %)")
        .setContentText(
          "Der Akkustand liegt unter 20 %. Verbinde ein Ladegerät oder aktiviere den Energiesparmodus."
        )
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText(
              "Der Akkustand liegt bei $percent % (unter 20 %). Verbinde bei Bedarf ein Ladegerät oder öffne DeviceCare KI für Live-Telemetrie."
            )
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    return try {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
      true
    } catch (_: SecurityException) {
      false
    }
  }

  companion object {
    const val CHANNEL_ID = "devicecare_battery_alerts"
    const val NOTIFICATION_ID = 2001
  }
}
