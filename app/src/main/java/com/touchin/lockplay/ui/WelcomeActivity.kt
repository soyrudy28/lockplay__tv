package com.touchin.lockplay.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.touchin.lockplay.AdminReceiver
import com.touchin.lockplay.InformationActivity
import com.touchin.lockplay.LoginActivity
import com.touchin.lockplay.R
import com.touchin.lockplay.admin.AdminRequiredActivity
import com.touchin.lockplay.StoreManager
import kotlinx.coroutines.launch  // Import necesario para usar corrutinas

class WelcomeActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var storeManager: StoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)
        storeManager = StoreManager(applicationContext)

        Handler(Looper.getMainLooper()).postDelayed({
            if (devicePolicyManager.isAdminActive(adminComponent)) {
                // Iniciamos una corrutina para llamar a la función suspendida
                lifecycleScope.launch {
                    val codeEnrollment = storeManager.getStringValue(StoreManager.STRING_CODE_ENRROLLMENT)

                    // Verificar si el usuario ya está registrado
                    if (codeEnrollment != "string_null" && codeEnrollment.isNotEmpty()) {
                        startActivity(Intent(this@WelcomeActivity, InformationActivity::class.java))
                    } else {
                        startActivity(Intent(this@WelcomeActivity, LoginActivity::class.java))  // Ir al Login si no está registrado
                    }
                    finish()
                }
            } else {
                startActivity(Intent(this, AdminRequiredActivity::class.java)) // Pedir permisos de administrador
                finish()
            }
        }, 3000) // Espera 3 segundos antes de continuar
    }
}
