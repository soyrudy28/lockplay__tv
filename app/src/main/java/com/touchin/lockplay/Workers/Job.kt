package com.touchin.lockplay.Workers

import android.app.admin.DevicePolicyManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import com.google.firebase.messaging.FirebaseMessaging
import com.touchin.lockplay.*
import com.touchin.lockplay.Services.SimChangeReceiver
import com.touchin.lockplay.SocketService.Companion.ACTION_SOCKET
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset


class Job: JobService()  {
    private lateinit var devicePolicyManager: DevicePolicyManager
    val TAG = "WORKERJOB"
    init {
        Configuration.Builder().setJobSchedulerJobIdRange(0, 1000).build()
    }
    private lateinit var firebaseMessaging: FirebaseMessaging
    override fun onStartJob(params: JobParameters?): Boolean {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                firebaseMessaging = FirebaseMessaging.getInstance()
                val storeManager = StoreManager(applicationContext)
                var token: String? = null
                firebaseMessaging.token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        token = task.result
                        Log.d(TAG, "Token: $token")
                    } else {
                        Log.w(TAG, "Failed to get token")
                    }
                }
                Log.d(TAG, "Worker starting...")
                val modeKiosk = storeManager.getBooleanValue(StoreManager.BOOLEAN_MODE_KIOSK)
                editDevice("noLock", "noEdit",token)

                val information = getInformationDevice()
                if (information != null) {
                    storeManager.setStringValue(StoreManager.STRING_CONTACT1, information.contact1)
                    storeManager.setStringValue(StoreManager.STRING_CONTACT2, information.contact2)
                    storeManager.setStringValue(StoreManager.STRING_IMAGE_COMPANY,information.image)
                }
                val isDateLock = information?.dateLock?.let { compareDateLock(it) }
                if(isDateLock == true){
                    storeManager.saveCodeUnlock(information.codeUnlock)
                    editDevice(true,null, null)
                    activeModeKiosk("ON")
                }
                if( information?.lock == true ){
                    storeManager.saveCodeUnlock(information.codeUnlock)
                    activeModeKiosk("ON")
                } else if(information?.lock == false && modeKiosk){
                    desactiveModeKiosk()
                }
                if( information?.provisioned == false ) {
                    unrollDevice()
                }

            } catch (e: Exception) {
                Log.i(TAG, "Aqui esta el error ")
                e.printStackTrace()
            } finally {
                jobFinished(params, false)
            }
        }
        val intent = Intent(this, SocketService::class.java)
        startForegroundService(intent)
        return true
    }


    private suspend fun editDevice(modeKiosk: Any, dateLock:String? =null, token: String?) {
        var kiosk: Any
        if (modeKiosk == "noLock"){
            kiosk = "noLock"
        } else {
            kiosk = modeKiosk
        }
        storeManager = StoreManager(applicationContext)
        val tokenStore = storeManager.getStringValue(StoreManager.STRING_TOKEN_FIREBASE)
        var newToken = "noToken"
        if(token != tokenStore){
            if (token != null) {
                newToken = token
            }
        }
        val deviceLock = ObjectEditDevice(lock=kiosk , dateLock= dateLock, token=newToken)
        val codeEnrollment = storeManager.getStringValue(StoreManager.STRING_CODE_ENRROLLMENT)
        var id = codeEnrollment
        if (codeEnrollment == "string_null"){
            id = getIMEI(this).toString()
        }
        RetrofitClient.retrofit.editDevice(codeEnrollment = id, requestBody = deviceLock)
    }

    private suspend fun getInformationDevice(): DeviceResponse? {
        storeManager = StoreManager(applicationContext)
        val codeEnrollment = storeManager.getStringValue(StoreManager.STRING_CODE_ENRROLLMENT)
        val responnse: Response<DeviceResponse>  = RetrofitClient.retrofit.getInfortmationDevice(codeEnrollment)
        return responnse.body()
    }

    private fun isPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getIMEI(context: Context): String? {
        if (isPermissionGranted(context)) {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return telephonyManager.imei
        } else {
            return null
        }
    }

    private fun unrollDevice(){
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
//            devicePolicyManager.wipeData(DevicePolicyManager.WIPE_RESET_PROTECTION_DATA) //formatea el dispositivo
        devicePolicyManager.removeActiveAdmin(AdminReceiver.getComponentName(this))
        devicePolicyManager.clearDeviceOwnerApp(AdminReceiver.getComponentName(this).packageName)
    }
    fun activeModeKiosk( eventData: String) {
        val intent = Intent("CLOSE_ACTIVITIES")
        this.sendBroadcast(intent)
        Log.i(TAG, "LLego hasta aqui")
        val kioskIntent = Intent(this, FirsAppActivity::class.java)
        kioskIntent.putExtra(ACTION_SOCKET, eventData)
        kioskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(kioskIntent)
    }
    private fun desactiveModeKiosk() {
        val intent2 = Intent("CLOSE_ACTIVITIES")
        this.sendBroadcast(intent2)
        val intent = Intent(ACTION_SOCKET)
        sendBroadcast(intent)
    }
    fun compareDateLock(dateStr: String): Boolean {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dateTime = LocalDateTime.parse(dateStr, formatter)
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val result = dateTime.isBefore(now) || dateTime.isEqual(now)
        Log.i(TAG,result.toString())
        return result
    }
    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }

}