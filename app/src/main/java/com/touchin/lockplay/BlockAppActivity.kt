package com.touchin.lockplay

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.Dialog
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.UserManager
import android.util.Base64
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.touchin.lockplay.admin.WifiListActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

lateinit var mcomponentNamex: ComponentName

//lateinit var devicePolicyManager: DevicePolicyManager
val TAGX = "BLOCKMANAGER"

class EnableKioskModeReceiverx : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent?) {

        if (intent?.action == SocketService.ACTION_SOCKET) {
            /*    Log.i("Broadcast", "Modo kiosco habilitado")
                val activityIntent = Intent(context, FirsAppActivity::class.java)
                context.startActivity(activityIntent)*/
            Log.i("Broadcast-EnableKioskModeReceiver", "Modo kiosco Fuerada")
        }
    }
}

class BlockAppActivity : AppCompatActivity() {
    private lateinit var devicePolicyManager: DevicePolicyManager

    private lateinit var deviceID: TextView
    private lateinit var textReasonLock: TextView
    private lateinit var textContact1: TextView
    private lateinit var textContact2: TextView
    private lateinit var imageView: ImageView
    private lateinit var dateNow: TextView
    private lateinit var storeManager: StoreManager

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {

        mcomponentNamex = AdminReceiver.getComponentName(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        storeManager = StoreManager(applicationContext)

        val isAdmin = isAdmin()
        if (isAdmin) {
            Log.i(TAGX, "Si es admin")

        } else {
            Log.i(TAGX, "No es admin")
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_app)
        getInformationDevice()
        val filter = IntentFilter(SocketService.ACTION_SOCKET)
        registerReceiver(receptorEvento, filter, RECEIVER_EXPORTED)
        val containterUnlock = findViewById<LinearLayout>(R.id.containerUnlock1)
        containterUnlock.setOnClickListener {
            showModalUnlock()
        }
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
        val parametro = intent.getStringExtra(SocketService.ACTION_SOCKET)
        val isSim = intent?.getBooleanExtra("LOCK_SIM", false)
        if (parametro == "ON") {
            if(isSim == true){
                textReasonLock = findViewById(R.id.texto0)
                textReasonLock.text = "Dispositivo bloqueado por tarjeta SIM"
            }
            setKioskPolicies(true, isAdmin)
        }
        if (parametro == "OFF") {
            Log.i(TAG, "CHAO")
            setKioskPolicies(false, isAdmin)
            finish()
        }
//        textNameCompanie = findViewById(R.id.textNameCompany)
        textContact1 = findViewById(R.id.textContact1)
        textContact2 = findViewById(R.id.textContact2)
        deviceID = findViewById(R.id.deviceID)
        dateNow = findViewById(R.id.dateNow)
        imageView = findViewById(R.id.imageView)

        val btnWifiBlock = findViewById<Button>(R.id.btnWifiblock)
        btnWifiBlock.setOnClickListener {
            val intent = Intent(this, WifiListActivity::class.java)
            startActivity(intent)
        }

        setRestrictions(false)


    }

    private fun showModalUnlock() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


        val inputCodeUnlock = dialog.findViewById<AppCompatEditText>(R.id.inputCodeUnlock)
        val btnCancel = dialog.findViewById<AppCompatButton>(R.id.btnCancel)
        val btnSend = dialog.findViewById<AppCompatButton>(R.id.btnSend)

        btnSend.setOnClickListener {
            val textCodeInput = inputCodeUnlock.text.toString()
            lifecycleScope.launch {
                val storeCode = storeManager.getCodeUnlock(StoreManager.STRING_CODE_UNLOCK).toString()
                if (textCodeInput == storeCode) {
                    dialog.dismiss()
                    storeManager.setModeKiosk(false)
                    setKioskPolicies(false, isAdmin())
                    Toast.makeText(this@BlockAppActivity, "Código correcto", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@BlockAppActivity, "Código incorrecto", Toast.LENGTH_LONG).show()
                }
            }
        }
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun handleSocketEvent(eventData: String?) {
        if (eventData != null) {
            Log.i("SOCKETSERVICE", eventData)
        }
        setKioskPolicies(true, isAdmin())
    }

    private fun isAdmin() = devicePolicyManager.isDeviceOwnerApp(packageName)

    public fun setKioskPolicies(enable: Boolean, isAdmin: Boolean) {
//        if (isAdmin) {
//            setRestrictions(enable)
//            enableStayOnWhilePluggedIn(enable)
//            setUpdatePolicy(enable)
//            setAsHomeApp(enable)
//            setKeyGuardEnabled(enable)
//        }
        setLockTask(enable, isAdmin)
//        setImmersiveMode(enable)
    }

    private fun setRestrictions(disallow: Boolean) {
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, disallow)
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, disallow)
        setUserRestriction(UserManager.DISALLOW_ADD_USER, disallow)
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, disallow)
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, disallow)
        devicePolicyManager.setStatusBarDisabled(mcomponentNamex, disallow)
    }

    private fun setUserRestriction(restriction: String, disallow: Boolean) = if (disallow) {
        devicePolicyManager.addUserRestriction(mcomponentNamex, restriction)

    } else {
        devicePolicyManager.clearUserRestriction(mcomponentNamex, restriction)
    }


    @SuppressLint("NewApi")
    private fun setLockTask(start: Boolean, isAdmin: Boolean) {
        if (isAdmin) {
            if (start) {
                devicePolicyManager.setLockTaskFeatures(
                    mcomponentNamex,
                    DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO
                )
                devicePolicyManager.setLockTaskPackages(
                    mcomponentNamex,
                    arrayOf(
                        "com.android.chrome",
                        "com.android.phone",
                        "com.android.server.telecom",
                        "com.samsung.android.incallui",
                        packageName
                    )
                )
            } else {
                devicePolicyManager.setLockTaskPackages(mcomponentNamex, emptyArray())
                finish()
                val intent2 = Intent("CLOSE_ACTIVITIES")
                this.sendBroadcast(intent2)
                val kioskIntent = Intent(this, InformationActivity::class.java)
                kioskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(kioskIntent)
                Log.i(TAG, "POWER")
                stopLockTask()
            }
        }
    }

    private fun setUpdatePolicy(enable: Boolean) {
        if (enable) {
            devicePolicyManager.setSystemUpdatePolicy(
                mcomponentNamex,
                SystemUpdatePolicy.createWindowedInstallPolicy(60, 120)
            )
        } else {
            devicePolicyManager.setSystemUpdatePolicy(mcomponentNamex, null)
        }
    }

    private val receptorEvento = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SocketService.ACTION_SOCKET) {
                Log.i("Broadcastssss", "Modo kiosco Fuerada")
                setKioskPolicies(false, true)
                val intentNew = Intent(applicationContext, FirsAppActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                intentNew.putExtra("MANAGER", false)
            }
        }
    }

    private fun getInformationDevice() {
        CoroutineScope(Dispatchers.IO).launch {
            val company = storeManager.getStringValue(StoreManager.STRING_COMPANY)
            val contact1 = storeManager.getStringValue(StoreManager.STRING_CONTACT1)
            val contact2 = storeManager.getStringValue(StoreManager.STRING_CONTACT2)
            val device = storeManager.getStringValue(StoreManager.STRING_CODE_ENRROLLMENT)
            val (textDateNow, textTimeNow) = formatDate(getCurrentDateTime())
            val imgBitmap = b64_image(storeManager.getStringValue(StoreManager.STRING_IMAGE_COMPANY))

            runOnUiThread {
                textContact1.text = contact1
                textContact2.text = contact2
                deviceID.text = device
                dateNow.text = "Fecha: $textDateNow | $textTimeNow"
                imageView.setImageBitmap(imgBitmap)
            }
        }
    }

    private fun b64_image(b64: String?): Bitmap? {
        val decodedBytes = Base64.decode(b64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun makePhoneCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), 1)
        } else {
            val callIntent = Intent(Intent.ACTION_CALL)
            val options = ActivityOptions.makeBasic()
            options.setLockTaskEnabled(true)
            val launchIntent = packageManager.getLaunchIntentForPackage("com.android.server.telecom")
            if (launchIntent != null) {
                startActivity(launchIntent, options.toBundle())
            }
            callIntent.data = Uri.parse("tel:$phoneNumber")
            startActivity(callIntent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                makePhoneCall("0998134831")
            }
        }
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

    fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es", "EC"))
        val currentDate = Date()
        return dateFormat.format(currentDate)
    }

}