package com.espmonitor

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity


class MainActivity : ComponentActivity() {
    private lateinit var statusTextView: TextView
    private val batteryStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("message") ?: "Erro desconhecido"
            statusTextView.text = message
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var batteryReceiver = BatteryLevelReceiver(this)
        val prefs by lazy { getSharedPreferences("BatteryMonitorPrefs", Context.MODE_PRIVATE) }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)

            statusTextView = TextView(context).apply {
                text = if (prefs.getBoolean("serviceActive", false)) "Status do serviço: Ativo" else "Status do serviço: Inativo"
            }
            addView(statusTextView)

            batteryReceiver.setStatusTextView(statusTextView)

            val toggleServiceButton = Button(context).apply {
                text = if (prefs.getBoolean("serviceActive", false)) "Desativar Serviço" else "Ativar Serviço"
                setOnClickListener {
                    val isActive = !prefs.getBoolean("serviceActive", false)
                    prefs.edit().putBoolean("serviceActive", isActive).apply()
                    if (isActive) {
                        startService(Intent(context, BatteryMonitorService::class.java))
                    } else {
                        stopService(Intent(context, BatteryMonitorService::class.java))
                    }
                    text = if (isActive) "Desativar Serviço" else "Ativar Serviço"
                    statusTextView.text = "Status do serviço: " + if (isActive) "Ativo" else "Inativo"
                }
            }
            addView(toggleServiceButton)

            val serverInput = EditText(context).apply {
                hint = "Servidor"
                setText(prefs.getString("serverUrl", "http://192.168.31.91:3000"))
            }
            addView(serverInput)

            val deviceInput = EditText(context).apply {
                hint = "ID do dispositivo"
                setText(prefs.getString("deviceId", "123"))
            }
            addView(deviceInput)

            val intervalInput = EditText(context).apply {
                hint = "Intervalo (min)"
                setText((prefs.getLong("intervalMillis", 15 * 60 * 1000L) / (60 * 1000)).toString())
            }
            addView(intervalInput)

            val saveButton = Button(context).apply {
                text = "Salvar Configuração"
                setOnClickListener {
                    prefs.edit().apply {
                        putString("serverUrl", serverInput.text.toString())
                        putString("deviceId", deviceInput.text.toString())
                        putLong("intervalMillis", intervalInput.text.toString().toLong() * 60 * 1000)
                        apply()
                    }
                }
            }
            addView(saveButton)

            val sendButton = Button(context).apply {
                text = "Enviar Agora"
                setOnClickListener {
                    val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    if (batteryIntent != null) {
                        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val deviceId = prefs.getString("deviceId", "123") ?: "123"
                        batteryReceiver.sendBatteryLevel(deviceId, level)
                    }
                }
            }
            addView(sendButton)
        }
        setContentView(layout)

        // Registrar o BroadcastReceiver
        val filter = IntentFilter("com.espmonitor.BATTERY_STATUS")
        registerReceiver(batteryStatusReceiver, filter)

        startService(Intent(this, BatteryMonitorService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryStatusReceiver)
    }

}
