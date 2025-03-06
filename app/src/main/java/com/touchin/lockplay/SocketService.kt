package com.touchin.lockplay

import android.annotation.SuppressLint
import android.app.*
import android.app.admin.DeviceAdminService
import android.app.admin.DevicePolicyManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.touchin.lockplay.StoreManager.Companion.STRING_TOKEN_FIREBASE
import com.touchin.lockplay.Workers.Job
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class SocketService : DeviceAdminService() {
    private lateinit var storeManager: StoreManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var telephonyManager: TelephonyManager
    private val TAG = "SOCKET_SERVICE"
    private val CHANNEL_ID = "LockPlayServiceChannel"

    companion object {
        const val ACTION_SOCKET = "MODE_KIOSK"

    }

    private val subscriptionCallback = object : SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() {
            super.onSubscriptionsChanged()
            checkSimCard()
        }
    }


    @SuppressLint("ForegroundServiceType")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "SE INICIO EL SOCKET")
        if (RetrofitClient.connectedInternet(this)) {
            devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            createNotificationChannel()
            val notification = createNotification()
            startForeground(1, notification)
//            startJobSchulder()
            setupMonitoring()

        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setupMonitoring() {
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        subscriptionManager.addOnSubscriptionsChangedListener(ContextCompat.getMainExecutor(this), subscriptionCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        return
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun checkSimCard() {
//        if (isCheckingSim) return
//        isCheckingSim = true
//        if (!hasRequiredPermissions() || !isDeviceOwner()) return
        CoroutineScope(Dispatchers.IO).launch {
            storeManager = StoreManager(applicationContext)
            try {
                val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList ?: emptyList()
                var codeEnrollment = storeManager.getStringValue(StoreManager.STRING_CODE_ENRROLLMENT)
                val iccidFound = storeManager.getStringValue(StoreManager.STRING_SIM_ICCID_1)
                Log.i(TAG, "Este es ${iccidFound} ")

                Log.i(TAG, "LLEGO 1")

                if (activeSubscriptionInfoList.isEmpty()) {
                    if (iccidFound != "string_null") {
                        Log.d(TAG, "CODIGO, ${generateHourlyCode()}")
                        storeManager.setStringValue(StoreManager.STRING_CODE_UNLOCK, generateHourlyCode())
                        activeModeKiosk("ON")
                        Log.i(TAG, "SIM NO DETECTADA 1 Y HAY UNA SIM GUARDADA")
                    } else {
                        Log.d(TAG, "NO HAY SIM Y NO HAY SIM GUARDADA")
                    }
                }
                // Obtener información de la primera ranura SIM (índice 0)
                val firstSlotInfo = activeSubscriptionInfoList.firstOrNull {
                    it.simSlotIndex == 0
                }

                if (firstSlotInfo == null) {
                    Log.i(TAG, "SIM ICCID: $iccidFound")
                    if (iccidFound != "string_null") {
                        Log.d(TAG, "CODIGO, ${generateHourlyCode()}")
                        storeManager.setStringValue(StoreManager.STRING_CODE_UNLOCK, generateHourlyCode())
                        activeModeKiosk("ON")
                        Log.i(TAG, "SIM NO DETECTADA 2 Y HAY UNA SIM GUARDADA")

                    } else {

                        Log.d(TAG, "NO HAY SIM Y NO HAY SIM GUARDADA")
                    }
                }
                // Obtener el telephonyManager específico para la primera ranura
                val firstSlotTelephonyManager =
                    firstSlotInfo?.let { telephonyManager.createForSubscriptionId(it.subscriptionId) }

                val phoneNumber = firstSlotTelephonyManager?.line1Number     // Obtener número de teléfono
                val imsi = firstSlotTelephonyManager?.subscriberId           // Obtener IMSI
                val iccId = firstSlotInfo?.iccId                             // Obtener ICCID

                Log.i(TAG, "CODE ENROLLMENT: $codeEnrollment")

                var approved = false
                if (iccId != null) {
                    if (codeEnrollment == "string_null") {
                        codeEnrollment = getIMEI(this@SocketService)
                    }
                    if (iccidFound == "string_null" && iccId.isNotEmpty()) {
                        approved = true
                        storeManager.setStringValue(StoreManager.STRING_SIM_ICCID_1, iccId)
                        Log.i(TAG, "SE CREO LA NUEVA SIM Y NO HAY SIM GUARDADA")
                    } else if (iccidFound != "string_null" && iccId.isNotEmpty()) {
                        Log.i(TAG, "Tipo de iccidFound: ${iccidFound::class.simpleName} y ${iccidFound}")
                        Log.i(TAG, "Tipo de iccId: ${iccId::class.simpleName} y ${iccId}")
                        if(isAuthorizedSim(iccId, codeEnrollment)){
                            storeManager.setStringValue(StoreManager.STRING_SIM_ICCID_1, iccId)
                            desactiveModeKiosk()
                        }
                        if (iccidFound == iccId) {
                            Log.d(TAG, "ENTRO EN EL DESACTIVAR POR IGUALDAD")
                        }
                        Log.i(TAG, "SE CREO LA NUEVA SIM Y HAY SIM GUARDADA ${iccidFound} y ${iccId}")
                    }
                    if(imsi != null ){
                        Log.i(TAG, "EL ESTADO DEL APPROVED ES $approved")
                        val call: Response<ResponseSims> = RetrofitClient.retrofit.createSim(
                            codeEnrollment,
                            ObjectSim(imsi = imsi, iccid = iccId, number = phoneNumber, approved = approved)
                        )
                        Log.i(TAG, "SIM REGISTRADA")
                        Log.i(TAG, call.body().toString())
                    }
                }
            } catch (e: Exception) {
                Log.e("ERROR", "${e.message}")
            }

        }
    }


    private fun getIMEI(context: Context): String {
        if (isPermissionGranted(context)) {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return telephonyManager.imei
        } else {
            return "Permiso no concedido"
        }
    }

    private fun isPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun isAuthorizedSim(iccId: String, codeEnrollment: String): Boolean {
        val call: Response<ResponseApprovedSim> = RetrofitClient.retrofit.getSimApproved(
            codeEnrollment,
            iccId
        )
        Log.i(TAG, "CONSULTA DE SIM APROBADA")
        Log.i(TAG, call.body().toString())
        return call.body()?.approved ?: false
    }


    private fun startJobSchulder() {
        val serviceName = ComponentName(this, Job::class.java)
        val builder = JobInfo.Builder(20, serviceName)
        val intervalMillis = 20 * 60 * 1000 // Ejecutar cada 5 minutos
        builder.setPeriodic(intervalMillis.toLong())
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY) // Requerir cualquier tipo de conexión de red
        builder.setPersisted(true)
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        jobScheduler.schedule(builder.build())
    }

    fun activeModeKiosk(eventData: String) {
        val intent = Intent("CLOSE_ACTIVITIES")
        this.sendBroadcast(intent)
        val kioskIntent = Intent(this, FirsAppActivity::class.java)
        kioskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        kioskIntent.putExtra(ACTION_SOCKET, eventData)
        kioskIntent.putExtra("LOCK_SIM", true )
        startActivity(kioskIntent)
    }

    private fun desactiveModeKiosk() {
        val intent2 = Intent("CLOSE_ACTIVITIES")
        this.sendBroadcast(intent2)
        val intent = Intent(ACTION_SOCKET)
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun generateHourlyCode(): String {
        // Obtener la hora UTC redondeada a la hora actual (ej. YYYYMMDDHH)
        val localNow = Instant.now().atZone(ZoneId.of("America/Guayaquil"))
        val formattedHour = localNow.format(DateTimeFormatter.ofPattern("yyyyMMddHH"))

        val data = formattedHour + "LOCK-PLAY" + "sim"
        Log.i(TAG, "Valor sin hash $data")

        val digest = MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }.substring(0, 8)
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Lock Play Sevice",
                NotificationManager.IMPORTANCE_MAX
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, SocketService::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lock Play Sevirce")
            .setContentText("notificaion en Escucha")
            .setSmallIcon(R.drawable.logo)
            .setContentIntent(pendingIntent)
            .build()
    }
}
