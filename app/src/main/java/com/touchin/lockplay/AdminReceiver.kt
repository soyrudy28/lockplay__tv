package com.touchin.lockplay

import android.app.ActivityManager
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.touchin.lockplay.ui.WelcomeActivity

class AdminReceiver : DeviceAdminReceiver() {
    private lateinit var firebaseMessaging: FirebaseMessaging

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        firebaseMessaging = FirebaseMessaging.getInstance()
        firebaseMessaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "Token: $token")
            } else {
                Log.w(TAG, "Failed to get token")
            }
        }

        // Obtener el DevicePolicyManager
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, AdminReceiver::class.java)

        // Verificar si la app es Device Owner
        if (devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
            Log.d(TAG, " ✅ La app es Device Owner. Aplicando restricciones...")

            // Bloquear el restablecimiento de fábrica
            /*devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_FACTORY_RESET)*/
            Log.d(TAG, " ✅ Restablecimiento de fábrica bloqueado.")

            // Bloquear el reinicio en modo seguro
            devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_SAFE_BOOT)
            Log.d(TAG, " ✅ Modo seguro bloqueado.")

            // Bloquear ajustes de volumen (opcional)
            devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_ADJUST_VOLUME)
            Log.d(TAG, " ✅ Ajustes de volumen bloqueados.")

            // Bloquear la desinstalación de apps
            devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_UNINSTALL_APPS)
            Log.d(TAG, " ✅ Desinstalación de aplicaciones bloqueada.")

            // 🚀 **Activar Modo Kiosco para evitar menú de apagado**
            devicePolicyManager.setLockTaskPackages(componentName, arrayOf(context.packageName))
            Log.d(TAG, " 🔒 MODO KIOSKO ACTIVADO - Bloqueando menú de apagado.")

            // Iniciar el modo Kiosco si la app está en primer plano
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (activityManager.appTasks.isNotEmpty()) {
                activityManager.appTasks[0].taskInfo?.baseIntent?.component?.className?.let { currentApp ->
                    if (currentApp == context.packageName) {
                        startLockTaskMode(context)
                    }
                }
            }

        } else {
            Log.e(TAG, " ❌ La app NO es Device Owner. No se puede bloquear el menú de apagado.")
        }
    }

    fun startLockTaskMode(context: Context) {
        val intent = Intent(context, WelcomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin disabled")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, AdminReceiver::class.java)
        devicePolicyManager.setShortSupportMessage(componentName, "No se puede eliminar el administrador")
        return "No puedes desactivar este administrador de dispositivo"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "KIOSK") {
            Log.i(TAG, "Modo KIOSK activado")
        }
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context.applicationContext, AdminReceiver::class.java)
        }

        private val TAG = AdminReceiver::class.java.simpleName
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.d(TAG, "MODO KIOSK ACTIVADO")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.d(TAG, "MODO KIOSK DESACTIVADO")
    }
}
