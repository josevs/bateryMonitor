package com.espmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class BatteryLevelReceiver(private val context: Context) : BroadcastReceiver() {
    private val prefs: SharedPreferences = context.getSharedPreferences("BatteryMonitorPrefs", Context.MODE_PRIVATE)
    private var statusTextView: TextView? = null

    fun setStatusTextView(textView: TextView) {
        statusTextView = textView
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val deviceId = prefs.getString("deviceId", "123") ?: "123"
            sendBatteryLevel(deviceId, level)
        }
    }

    fun sendBatteryLevel(deviceId: String, level: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverUrl = prefs.getString("serverUrl", "http://192.168.31.91:3000") ?: "http://192.168.31.91:3000"
                val url = URL("$serverUrl/?d=$deviceId&nivelBat=$level")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                val responseMessage = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                val intent = Intent("com.espmonitor.BATTERY_STATUS")
                intent.putExtra("message", "Resposta do servidor: $responseMessage")
                context.sendBroadcast(intent)

                Log.d("BatteryLevelSender", "Nível de bateria enviado com sucesso: $level")
            } catch (e: Exception) {
                Log.e("BatteryLevelSender", "Erro ao enviar nível de bateria", e)

                val intent = Intent("com.espmonitor.BATTERY_STATUS")
                intent.putExtra("message", "Erro ao enviar dados: ${e.message}")
                context.sendBroadcast(intent)
            }
        }
    }
}