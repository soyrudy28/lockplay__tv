package com.touchin.lockplay

import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import androidx.annotation.RequiresApi

class KioskManager(context: Context) {
    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = AdminReceiver.getComponentName(context)

    @RequiresApi(Build.VERSION_CODES.P)
    fun setKioskPolicies(enable: Boolean, isAdmin: Boolean, context: Context) {
        if (isAdmin) {
            setLockTask(enable, context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setLockTask(start: Boolean, context: Context) {
        if (start) {
            devicePolicyManager.setLockTaskFeatures(
                adminComponent,
                DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO
            )
            devicePolicyManager.setLockTaskPackages(
                adminComponent, arrayOf(
                    "com.android.chrome",
                    "com.android.phone",
                    "com.android.server.telecom",
                    "com.samsung.android.incallui",
                    context.packageName
                )
            )
        } else {
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf())
            context.stopService(Intent(context, SocketService::class.java))
        }
    }

    fun setRestrictions(disallow: Boolean) {
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, disallow)
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, disallow)
        setUserRestriction(UserManager.DISALLOW_ADD_USER, disallow)
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, disallow)
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, disallow)
        devicePolicyManager.setStatusBarDisabled(adminComponent, disallow)
    }

    private fun setUserRestriction(restriction: String, disallow: Boolean) {
        if (disallow) {
            devicePolicyManager.addUserRestriction(adminComponent, restriction)
        } else {
            devicePolicyManager.clearUserRestriction(adminComponent, restriction)
        }
    }

    fun setUpdatePolicy(enable: Boolean) {
        if (enable) {
            devicePolicyManager.setSystemUpdatePolicy(
                adminComponent,
                SystemUpdatePolicy.createWindowedInstallPolicy(60, 120)
            )
        } else {
            devicePolicyManager.setSystemUpdatePolicy(adminComponent, null)
        }
    }

    companion object {
        private const val TAG = "KIOSK_MANAGER"
    }
}