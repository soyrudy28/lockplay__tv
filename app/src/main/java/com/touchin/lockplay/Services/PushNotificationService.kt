package com.touchin.lockplay.Services

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.touchin.lockplay.*
import com.touchin.lockplay.SocketService.Companion.ACTION_SOCKET
import com.touchin.lockplay.StoreManager.Companion.STRING_TOKEN_FIREBASE
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.Manifest
import android.app.*
import android.location.Geocoder
import android.os.UserManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import java.util.*

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class PushNotificationService: FirebaseMessagingService(){
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var storeManager: StoreManager
    private val TAG = "PUSH-NOTIFICATION"
    override  fun onNewToken(token:String){
        super.onNewToken(token)
        storeManager = StoreManager(applicationContext)
        GlobalScope.launch {
            storeManager.setStringValue(STRING_TOKEN_FIREBASE, token)
        }
        Log.d(TAG, "Refreshed token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        storeManager = StoreManager(applicationContext)
        super.onMessageReceived(message)
        message.data.isNotEmpty().let {
            val data = message.data
            Log.i(TAG, data.toString())
            GlobalScope.launch {
                data["code_unlock"]?.let { it1 -> storeManager.saveCodeUnlock(it1) }
                data["lock"]?.let { it1 ->
                    if(it1 == "OFF"){
                        desactivedModeKiosk()
                    }else if(it1 == "ON"){
                        storeManager.setModeKiosk(false)
                        activeModeKiosk(it1)
                    }else if(it1 == "UNROLL"){
                        unrollDevice()
                    }else if(it1 == "LOCATION"){
                        getLocation()
                    }
                }
                data["policies"]?.let {it1 ->
                    val policies = it1.split("-")
                    processPoliciesItem(policies)
                }
                data["sim_approved"]?.let { it1 -> approvedSim(it1)  }
                data["notifications"]?.let { it1 -> showNotification(it1)  }

            }
        }
    }

    fun processPoliciesItem(policies: List<String>) {
        for (item in policies) {
            when (item) {
                "ON_DEBUGGING" -> handleDebugging(true)
                "OFF_DEBUGGING" -> handleDebugging(false)
                "ON_UNKNOWN_APPS" -> handleUnknowApps(true)
                "OFF_UNKNOWN_APPS" -> handleUnknowApps(false)
                else -> Log.i(TAG, "Comando no reconocido: $item")
            }
        }
    }
    private fun unrollDevice(){
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
//            devicePolicyManager.wipeData(DevicePolicyManager.WIPE_RESET_PROTECTION_DATA) //formatea el dispositivo
        devicePolicyManager.removeActiveAdmin(AdminReceiver.getComponentName(this))
        devicePolicyManager.clearDeviceOwnerApp(AdminReceiver.getComponentName(this).packageName)
    }
    private suspend fun approvedSim(status:String){

        val (approved, iccid) = status.split("-")
        val iccidFound = storeManager.getStringValue(StoreManager.STRING_SIM_ICCID_1)

        if(approved == "ON"){
            storeManager.setStringValue(StoreManager.STRING_SIM_ICCID_1  , iccid)
            desactivedModeKiosk()
        }else {
            if(iccidFound == iccid){
                activeModeKiosk("ON")
            }
        }
    }
    fun activeModeKiosk(eventData: String) {
        val intent = Intent("CLOSE_ACTIVITIES")
        this.sendBroadcast(intent)
        val kioskIntent = Intent(this, FirsAppActivity::class.java)
        kioskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        kioskIntent.putExtra(ACTION_SOCKET, eventData)
        startActivity(kioskIntent)
    }
    private fun desactivedModeKiosk() {
        val intent2 = Intent("CLOSE_ACTIVITIES")
        this.sendBroadcast(intent2)
        val intent = Intent(ACTION_SOCKET)
        sendBroadcast(intent)
    }
    private fun handleDebugging(active: Boolean) {
        val component = AdminReceiver.getComponentName(this)
        if(!active){
            devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            devicePolicyManager.addUserRestriction(component, UserManager.DISALLOW_DEBUGGING_FEATURES )
        }
        if(active){
            devicePolicyManager.clearUserRestriction(component, UserManager.DISALLOW_DEBUGGING_FEATURES)
        }
    }

    private fun handleUnknowApps(active: Boolean){
        val component = AdminReceiver.getComponentName(this)
        if(!active){
            devicePolicyManager.addUserRestriction(component, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
        }else {
            Log.i(TAG, "APK ACTIVADOS")
            devicePolicyManager.clearUserRestriction(component, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
        }
    }

    private suspend fun getLocation() {
        val kioskIntent = Intent(this, InformationActivity::class.java)
        kioskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(kioskIntent)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permiso de ubicación no concedido")
            return
        }

        try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
//                val address = getAddress(location)  //Este obtiene las calles
                val latitude = location.latitude
                val longitude = location.longitude
                Log.i(TAG, "Dirección obtenida: $location")
                try {
                    editLocation("$latitude,$longitude")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al editar la ubicación", e)
                }
            } else {
                Log.i(TAG, "La ubicación es nula")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener la ubicación", e)
        }
    }

    private fun handleNow() {
        // Lógica para activar el modo Kiosk
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, AdminReceiver::class.java)

        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            val intent = devicePolicyManager.createAdminSupportIntent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
//            startActivity(intent)
            devicePolicyManager.setLockTaskPackages(componentName, arrayOf(packageName))
            this.startActivity(intent)
        }
    }

    private suspend fun editLocation(address: String){
        storeManager = StoreManager(applicationContext)
        val codeEnrollment = storeManager.getStringValue(StoreManager.STRING_CODE_ENRROLLMENT)
        val modeKiosk = storeManager.getBooleanValue(StoreManager.BOOLEAN_MODE_KIOSK)
        val location = ObjectEditDevice(lock= modeKiosk, dateLock = "noEdit", location = address)
        Log.i(TAG, location.toString())
        RetrofitClient.retrofit.editDevice(codeEnrollment = codeEnrollment, requestBody = location)
    }

    private fun getAddress(location: android.location.Location):String {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val addressComplete = address.getAddressLine(0)
                return  addressComplete
            } else {
                Log.e(TAG, "No se logro obtener la direccion")
                return "noLocation"
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se logro obtener la direccion")
            return "noLocation"
        }
    }

    private fun showNotification(message: String) {
        val (title, description) = message.split("-")
        val channelId = "LockPlayChannel"
        val notificationId = 1001

        val intent = Intent(this, InformationActivity::class.java)
        val pendingIntent =  PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "My Notifications", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.logo) // Icono de la notificación
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

}