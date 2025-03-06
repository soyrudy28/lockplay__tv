package com.touchin.lockplay.Services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.touchin.lockplay.FirsAppActivity
import com.touchin.lockplay.SocketService
import com.touchin.lockplay.SocketService.Companion.ACTION_SOCKET

class SimChangeReceiver: BroadcastReceiver(){
    private val TAG = "SIM_RECEIVER"
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "ENTRO EN EL INTENT DEL SIM")

    }
    private fun activeModeKiosk(context: Context) {
        val kioskIntent = Intent(context, FirsAppActivity::class.java)
        kioskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        kioskIntent.putExtra(ACTION_SOCKET, "ON")
        context.startActivity(kioskIntent)

    }
}