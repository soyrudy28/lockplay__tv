package com.touchin.lockplay.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.touchin.lockplay.AdminReceiver
import com.touchin.lockplay.R
import com.touchin.lockplay.ui.WelcomeActivity

class AdminRequiredActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_required)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        val btnGrantPermission: Button = findViewById(R.id.btnGrantPermission)
        val txtMessage: TextView = findViewById(R.id.txtMessage)

        txtMessage.text = "Se requieren permisos de administrador para continuar usando Lock Play TV."

        btnGrantPermission.setOnClickListener {
            finish() // Cierra la actividad actual
            startActivity(intent) // Reinicia la misma actividad
        }
    }

    override fun onResume() {
        super.onResume()
        // Si el usuario ya otorgó los permisos, cerrar esta pantalla
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }
}
