package com.espmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class BatteryStatusReceiver() : BroadcastReceiver() {
    private lateinit var context: Context

    private var statusTextView: TextView? = null

    private var deviceId: String = ""
    private var batteryLevel: Int = -1
    private var isCharging: Boolean = false
    private var isPlugged: Boolean = false
    private var isFull: Boolean = false

    constructor(context: Context) : this() {
        this.context = context
    }


    fun setStatusTextView(textView: TextView) {
        statusTextView = textView
    }

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
            sendBatteryStatus()
        }
    }

    fun updateBatteryStatus() {
        val prefs: SharedPreferences = this.context.getSharedPreferences("BatteryMonitorPrefs", Context.MODE_PRIVATE)
        val batteryIntent = this.context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryIntent?.let {
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            deviceId = prefs.getString("deviceId", "123") ?: "123"
            batteryLevel = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
            isFull = status == BatteryManager.BATTERY_STATUS_FULL
            isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                    plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        }
    }

    fun sendBatteryStatus() {
        updateBatteryStatus()
        CoroutineScope(Dispatchers.IO).launch {
            val intent = Intent("com.espmonitor.BATTERY_STATUS")
            try {
                val prefs = context.getSharedPreferences("BatteryMonitorPrefs", Context.MODE_PRIVATE)
                val serverUrl = prefs.getString("serverUrl", "http://192.168.31.91:3000") ?: "http://192.168.31.91:3000"
                val url = URL("$serverUrl/?d=$deviceId&nivelBat=$batteryLevel&carregando=${if (isCharging) 1 else 0}&plugado=${if (isPlugged) 1 else 0}&carregado=${if (isFull) 1 else 0}")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                val responseMessage = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                intent.putExtra("message", "Resposta do servidor: $responseMessage")
                context.sendBroadcast(intent)

                Log.d("BatteryLevelSender", "Dados enviados com sucesso: nível=$batteryLevel, carregando=$isCharging, plugado=$isPlugged")

            } catch (e: Exception) {
                Log.e("BatteryLevelSender", "Erro ao enviar nível de bateria", e)

                intent.putExtra("message", "Erro ao enviar dados: ${e.message}")
                context.sendBroadcast(intent)
            }
        }
    }

}