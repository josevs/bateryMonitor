package com.espmonitor

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.os.SystemClock

class BatteryMonitorService : Service() {
    private lateinit var batteryReceiver: BatteryStatusReceiver
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("BatteryMonitorPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        batteryReceiver = BatteryStatusReceiver(this)
        registerReceiver(batteryReceiver, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        scheduleNextCheck()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleNextCheck() {
        val intervalMillis = prefs.getLong("intervalMillis", 15 * 60 * 1000L) // Padrão: 15 minutos
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, BatteryMonitorService::class.java)
        val pendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + intervalMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + intervalMillis,
                pendingIntent
            )
        }
    }
}
