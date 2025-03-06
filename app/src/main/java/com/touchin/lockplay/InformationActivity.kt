package com.touchin.lockplay

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.admin.DevicePolicyManager
import android.app.admin.FactoryResetProtectionPolicy
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.touchin.lockplay.Services.Brodcast
import com.touchin.lockplay.Workers.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import android.util.Base64
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatEditText
import com.touchin.lockplay.Services.SimChangeReceiver

lateinit var storeManager: StoreManager

class InformationActivity : AppCompatActivity() {
    private lateinit var mcomponentName: ComponentName
    private lateinit var textNameCompanie: TextView
    private lateinit var deviceID: TextView
    private lateinit var textDateDue: TextView
    private lateinit var textContact1: TextView
    private lateinit var textContact2: TextView
    private lateinit var textTime: TextView
    private lateinit var imageView: ImageView
    private lateinit var dateNow: TextView
    private var isLoading = true
    private lateinit var devicePolicyManager: DevicePolicyManager
    val TAG = "ADMINMANAGER"
    private lateinit var protectionPolicy: FactoryResetProtectionPolicy
    private lateinit var closeActivitiesBrodcast: Brodcast


    private val responseLauncher = registerForActivityResult(StartActivityForResult()) { activityResult ->
        mcomponentName = AdminReceiver.getComponentName(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (activityResult.resultCode == RESULT_OK) {
            Toast.makeText(this, "Activado el administrador", Toast.LENGTH_SHORT).show()
        } else {
            devicePolicyManager.setShortSupportMessage(componentName, "No se elimina")
        }
    }

    //    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {

        mcomponentName = AdminReceiver.getComponentName(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
//        devicePolicyManager.setDeviceAdminDisabled(receiver.componentName, false)


        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { isLoading }
        if (isAdmin()) {
            Log.i(TAG, "Si es admin")
            checkPermission()
        } else {
            Log.i(TAG, "No es admin")
        }
        storeManager = StoreManager(applicationContext)

        closeActivitiesBrodcast = Brodcast()
        registerReceiver(closeActivitiesBrodcast, IntentFilter("CLOSE_ACTIVITIES"), RECEIVER_EXPORTED)

//        val simChangeReceiver = SimChangeReceiver()
//        val intentFilter = IntentFilter("android.intent.action.SIM_STATE_CHANGED")
//        registerReceiver(simChangeReceiver, intentFilter)

        CoroutineScope(Dispatchers.IO).launch {
            val codeEnrollment = storeManager.getStringValue(StoreManager.STRING_CODE_ENRROLLMENT)
            if (codeEnrollment == "string_null") {
                val intent = Intent(this@InformationActivity, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                startJobSchulder()
//                val serviceIntent = Intent(this@InformationActivity, SocketService::class.java)
//                startService(serviceIntent)
            }
//            }

        }

        setContentView(R.layout.activity_information)

        storeManager = StoreManager(applicationContext)
        getInformationDevice()

        textDateDue = findViewById(R.id.textDateDue)
        textContact1 = findViewById(R.id.textContact1)
        textContact2 = findViewById(R.id.textContact2)
        deviceID = findViewById(R.id.deviceID)
        textTime = findViewById(R.id.textTime)
        dateNow = findViewById(R.id.dateNow)
        imageView = findViewById(R.id.imageView)
        val ButtonContact1 = findViewById<CardView>(R.id.ButtonContact1)
        val ButtonContact2 = findViewById<CardView>(R.id.ButtonContact2)


        ButtonContact1.setOnClickListener {
            lifecycleScope.launch {
                val contact1 = storeManager.getCodeUnlock(StoreManager.STRING_CONTACT1).toString()
                makePhoneCall(contact1)
            }
        }

        ButtonContact2.setOnClickListener {
            lifecycleScope.launch {
                val contact2 = storeManager.getCodeUnlock(StoreManager.STRING_CONTACT2).toString()
                makePhoneCall(contact2)
            }
        }

    }

    private fun isAdmin() = devicePolicyManager.isDeviceOwnerApp(packageName)


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

    private fun getInformationDevice() {
        if (RetrofitClient.connectedInternet(this)) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val codeEnrollment = storeManager.getStringValue(StoreManager.STRING_CODE_ENRROLLMENT)
                    val call: Response<DeviceResponse> = RetrofitClient.retrofit.getInfortmationDevice(codeEnrollment)
                    val device: DeviceResponse? = call.body()
                    val (formattedDate, timePart) = formatDate(device?.dateDue)
                    val (textDateNow, textTimeNow) = formatDate(getCurrentDateTime())
                    val imgBitmap = b64_image(device?.image)
                    Log.i("RETROFIT", device.toString())
                    print(device)
                    runOnUiThread {
                        val contact1 = device?.contact1
                        val contact2 = device?.contact2
                        val dateDue = formattedDate
                        textContact1.text = contact1
                        textContact2.text = contact2
                        deviceID.text = codeEnrollment
                        textDateDue.text = dateDue?.takeIf { it.isNotEmpty() } ?: "N/A"
                        textTime.text = timePart.takeIf { it.isNotEmpty() } ?: "Hora: 00:00"
                        dateNow.text = "Fecha: $textDateNow | $textTimeNow"
                        imageView.setImageBitmap(imgBitmap)
                        println(call.body())
                        isLoading = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ocurrio un error al traer los datos")
                }
            }

        } else {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.modal_information)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val textModal = dialog.findViewById<TextView>(R.id.textModal)
            textModal.text = "No hay conexión a internet"
            Log.e(TAG, "No hay internet")

            isLoading = false
            dialog.show()
        }
    }

    private fun b64_image(b64: String?): Bitmap? {
        if (b64 != null) {
            if (b64.isNotEmpty()) {
                val decodedBytes = Base64.decode(b64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                return bitmap
            }
        }
        return null
    }

    private fun formatDate(date: String?): Pair<String?, String> {
        if (date?.isNotEmpty() == true) {
            val (datePart, time) = date.split(" ")
            val inputDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputDate = SimpleDateFormat("dd-MMMM-yyyy", Locale("es", "EC"))
            val dateParsed = inputDate.parse(datePart)
            val dateFormated = dateParsed?.let {
                outputDate.format(it)
                    .uppercase(Locale("es", "EC"))
            }
            return Pair(dateFormated, "Hora: $time")
        }
        return Pair("N/A", "00:00:00")
    }

    private fun makePhoneCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), 1)
        } else {
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:$phoneNumber")
            startActivity(callIntent)
        }
    }

    fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es", "EC"))
        val currentDate = Date()
        return dateFormat.format(currentDate)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(closeActivitiesBrodcast)
    }

    /*override fun onBackPressed() {
        val intent2 = Intent("CLOSE_ACTIVITIES")
        this.sendBroadcast(intent2)
        super.onBackPressed()
    }*/
    private fun checkPermission() {
        devicePolicyManager.setPermissionPolicy(
            mcomponentName,
            DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT
        )
        if (!hasRequiredPermissions()){
            requestPermission()
        }
    }
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            777
        )
    }
    private fun hasRequiredPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            }
        }
    }
}




