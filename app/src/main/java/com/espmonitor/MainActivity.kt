package com.espmonitor

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.Locale


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
        val batteryReceiver = BatteryStatusReceiver(this)
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
                    statusTextView.text = buildString {
                        append("Status do serviço: ")
                        append(if (isActive) "Ativo" else "Inativo")
                    }
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
                this.setText(String.format(Locale.getDefault(),"%d",prefs.getLong("intervalMillis", 15 * 60 * 1000L) / (60 * 1000)))
            }
            addView(intervalInput)

            val saveButton = Button(context).apply {
                text = getString(R.string.save_config)
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
                text = getString(R.string.send_now)
                setOnClickListener {
                    batteryReceiver.sendBatteryStatus()
                }
            }
            addView(sendButton)
        }
        setContentView(layout)

        startService(Intent(this, BatteryMonitorService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryStatusReceiver)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.espmonitor.BATTERY_STATUS")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+ (Android 14 ou superior)
            registerReceiver(batteryStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            // Para versões mais antigas do Android
            registerReceiver(batteryStatusReceiver, filter)
        }

    }
}
