package com.touchin.lockplay.admin

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.touchin.lockplay.R

class WifiListActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiListView: ListView
    private lateinit var connectivityManager: ConnectivityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_list)

        wifiListView = findViewById(R.id.wifiListView)
        val btnBack = findViewById<Button>(R.id.btnBack)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            scanWifiNetworks()
        }

        btnBack.setOnClickListener {
            finish()
        }

        updateConnectedNetwork() // Actualiza el estado de la red conectada

        // Registrar el receptor para monitorear el estado de la red
        registerReceiver(wifiStateReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiStateReceiver)
    }

    private fun scanWifiNetworks() {
        if (wifiManager.isWifiEnabled) {
            wifiManager.startScan()
            val results = wifiManager.scanResults
            val wifiNames = results.map { it.SSID }.filter { it.isNotEmpty() }

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, wifiNames)
            wifiListView.adapter = adapter

            wifiListView.setOnItemClickListener { _, _, position, _ ->
                val selectedSSID = wifiNames[position]
                showPasswordDialog(selectedSSID)
            }
        } else {
            Toast.makeText(this, "Wi-Fi no está habilitado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPasswordDialog(ssid: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Conectarse a $ssid")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val input = EditText(this)
        input.hint = "Ingresa la contraseña"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(input)

        val checkBox = CheckBox(this)
        checkBox.text = "Mostrar Contraseña"
        layout.addView(checkBox)

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                input.inputType = android.text.InputType.TYPE_CLASS_TEXT
            } else {
                input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        }

        builder.setView(layout)

        builder.setPositiveButton("Conectar") { _, _ ->
            val password = input.text.toString()
            if (password.isNotEmpty()) {
                connectToWifi(ssid, password)
            } else {
                Toast.makeText(this, "La contraseña no puede estar vacía", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }


    private fun connectToWifi(ssid: String, password: String) {
        val wifiConfig = WifiConfiguration().apply {
            SSID = String.format("\"%s\"", ssid)
            preSharedKey = String.format("\"%s\"", password)
        }

        val netId = wifiManager.addNetwork(wifiConfig)
        if (netId != -1) {
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()

            Toast.makeText(this, "Conectando a $ssid...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error al conectar con $ssid", Toast.LENGTH_SHORT).show()
        }
    }

    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val networkInfo = intent?.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            if (networkInfo?.isConnected == true) {
                val wifiInfo = wifiManager.connectionInfo
                Toast.makeText(this@WifiListActivity, "Conectado a ${wifiInfo.ssid}", Toast.LENGTH_LONG).show()
            } else if (networkInfo?.state == NetworkInfo.State.DISCONNECTED) {
                Toast.makeText(this@WifiListActivity, "Conexión fallida. Verifica la contraseña.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateConnectedNetwork() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo.ssid

        val connectedNetworkTextView = findViewById<TextView>(R.id.connectedNetwork)
        if (ssid != null && ssid != "<unknown ssid>") {
            connectedNetworkTextView.text = "Conectado a: ${ssid.replace("\"", "")}"
        } else {
            connectedNetworkTextView.text = "Conectado a: Ninguna red"
        }
    }

}
