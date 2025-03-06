package com.touchin.lockplay

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, AdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                dpm.setKeyguardDisabled(adminComponent, true)
                dpm.setStatusBarDisabled(adminComponent, true)
            }
        }
    }
}
