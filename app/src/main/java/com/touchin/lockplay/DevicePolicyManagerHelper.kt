package com.touchin.lockplay

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.util.Log

class DevicePolicyManagerHelper(private val context: Context) {

    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val componentName = AdminReceiver.getComponentName(context)

    fun applyKioskPolicies() {
        try {
            if (devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
                devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_FACTORY_RESET)
                devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_SAFE_BOOT)
                devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_ADJUST_VOLUME)
                //bloquea la barra para deslizar
                devicePolicyManager.setStatusBarDisabled(componentName, false)
                //
                devicePolicyManager.setKeyguardDisabled(componentName, true)

                Log.d("DevicePolicyHelper", "Políticas de kiosko aplicadas correctamente")
            } else {
                Log.e("DevicePolicyHelper", "La aplicación no es Device Owner")
            }
        } catch (e: SecurityException) {
            Log.e("DevicePolicyHelper", "Error al aplicar políticas de kiosko: ${e.message}")
        }
    }

    fun clearKioskPolicies() {
        try {
            devicePolicyManager.clearUserRestriction(componentName, UserManager.DISALLOW_FACTORY_RESET)
            devicePolicyManager.clearUserRestriction(componentName, UserManager.DISALLOW_SAFE_BOOT)
            devicePolicyManager.clearUserRestriction(componentName, UserManager.DISALLOW_ADJUST_VOLUME)
            devicePolicyManager.setStatusBarDisabled(componentName, false)
            devicePolicyManager.setKeyguardDisabled(componentName, false)

            Log.d("DevicePolicyHelper", "Políticas de kiosko eliminadas correctamente")
        } catch (e: SecurityException) {
            Log.e("DevicePolicyHelper", "Error al eliminar políticas de kiosko: ${e.message}")
        }
    }
}
