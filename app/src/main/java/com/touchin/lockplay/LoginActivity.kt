package com.touchin.lockplay

import android.app.admin.DevicePolicyManager
import android.app.admin.FactoryResetProtectionPolicy
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import retrofit2.Response
import android.Manifest
import android.app.Dialog
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.touchin.lockplay.Workers.Job

class LoginActivity : AppCompatActivity() {
    val TAG = "LOGIN"
    private lateinit var storeManager: StoreManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var firebaseMessaging: FirebaseMessaging
    private lateinit var mcomponentName: ComponentName

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mcomponentName = AdminReceiver.getComponentName(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        storeManager = StoreManager(applicationContext)
        checkPermission()
        setContentView(R.layout.activity_login)
        firebaseMessaging = FirebaseMessaging.getInstance()
        firebaseMessaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "Token: $token")
            } else {
                Log.w(TAG, "Failed to get token")
            }
        }

        // Bloquear la actualización del sistema utilizando SystemUpdatePolicy
        //   setSystemUpdatePolicy()

        val btnLogin = findViewById<AppCompatButton>(R.id.btnLogin)
        val inputCodeCompany = findViewById<AppCompatEditText>(R.id.inputCodeCompany)
        if (!RetrofitClient.connectedInternet(this)) {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.modal_information)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val textModal = dialog.findViewById<TextView>(R.id.textModal)
            textModal.text = "No hay conexión a internet"
            dialog.show()
        }
        btnLogin.setOnClickListener {
            val textCode = inputCodeCompany.text.toString()
            val loadingDialog = Dialog(this)
            loadingDialog.setContentView(R.layout.activity_carga)
            loadingDialog.window!!.setLayout(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            loadingDialog.setCancelable(false)
            loadingDialog.show()
            lifecycleScope.launch {
                try {
                    enrollmentDevice(textCode, loadingDialog)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en el enrollment", e)
                    loadingDialog.dismiss()
                }
            }
        }
    }

    /*private fun preventLocationDisable() {
        try {
            // Prevenir que el usuario desactive la ubicación desde los ajustes del dispositivo
            if (!devicePolicyManager.isAdminActive(mcomponentName)) {
                Log.e(TAG, "El dispositivo no está registrado como administrador.")
                return
            }

            // Restringir la configuración de ubicación
            devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_CONFIG_LOCATION)
            Log.d(TAG, "Restricción configurada para evitar la desactivación de la ubicación.")

            // Asegurarse de que la ubicación esté activada
            val locationMode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)

            // Si la ubicación está desactivada, forzar su activación en alta precisión
            if (locationMode == Settings.Secure.LOCATION_MODE_OFF) {
                // Activar la ubicación en alta precisión
                Settings.Secure.putInt(contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_HIGH_ACCURACY)
                Log.d(TAG, "La ubicación fue activada automáticamente en alta precisión.")
            } else {
                Log.d(TAG, "La ubicación ya está activada.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error al establecer la restricción de ubicación: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error desconocido al forzar la ubicación: ${e.message}")
        }
    }*/

    private fun getSerie(): String? {
        val serialNumber = Build.getSerial()
        return serialNumber
    }
    private fun getModel(): String? {
        return Build.MODEL
    }

    private fun getDeviceDetails(): String {
        return "Modelo: ${Build.MODEL}\n - " +
                "Fabricante: ${Build.MANUFACTURER}\n - " +
                "Marca: ${Build.BRAND}\n - " +
                "Nombre interno: ${Build.DEVICE}\n - " +
                "Producto: ${Build.PRODUCT}\n - " +
                "Versión de Android: ${Build.VERSION.RELEASE}\n - " +
                "SDK: ${Build.VERSION.SDK_INT}\n"
    }

    // Inscribir dispositivo.
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun enrollmentDevice(texCodeCompany: String, loadingDialog: Dialog) {
        val numberSerie = getSerie().toString()
        val modelDevice = getModel().toString()
        val imeiDevice = getSerie().toString()

        lifecycleScope.launch {
            val datSend = ObjectEnrollment(
                imeiDevice,
                numberSerie,
                modelDevice,
                code_enrollment = CodeEnrollmentID(texCodeCompany),
                token = storeManager.getStringValue(StoreManager.STRING_TOKEN_FIREBASE),
                version_apk = BuildConfig.VERSION_NAME
            )
            val call: Response<ResponseEnrrolment> = RetrofitClient.retrofit.enrrollmentDevice(datSend)
            val res = call.body()
            Log.i(TAG, datSend.toString())

            if (res?.statusCode == 200) {
                saveCodeEnrollment(res.dataResponse)
                /*devicePolicyManager.setApplicationRestrictions(
                    mcomponentName,
                    packageName,
                    bundleOf("ignore_battery_optimizations" to true)
                )*/

                devicePolicyManager.setTimeZone(mcomponentName, "America/Guayaquil")
                devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_CONFIG_DATE_TIME)

                /*accountFactoryReset(res.dataResponse.accounts)
                devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_DEBUGGING_FEATURES)
                devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_CONFIG_CELL_BROADCASTS)*/

                val intent = Intent(this@LoginActivity, InformationActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
                loadingDialog.dismiss()
            } else {
                Toast.makeText(this@LoginActivity, res?.message, Toast.LENGTH_LONG).show()
                Log.i(TAG, res.toString())
                loadingDialog.dismiss()
            }
        }
    }


    private suspend fun saveCodeEnrollment(dataResponse: DeviceProfile) {
        storeManager.saveDataEnrollment(
            dataResponse.codeEnrollmentProfile,
            dataResponse.company,
            dataResponse.contact1,
            dataResponse.contact2,
            dataResponse.imageCompany,
            dataResponse.codeUnlock
        )


        Log.i(TAG, "GUARDADO$dataResponse")
    }

    private fun isPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startJobSchulder() {
        val serviceName = ComponentName(this, Job::class.java)
        val builder = JobInfo.Builder(20, serviceName)
        val intervalMillis = 20 * 60 * 1000
        builder.setPeriodic(intervalMillis.toLong())
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        builder.setPersisted(true)
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        jobScheduler.schedule(builder.build())
    }

// En caso de reseteo
    @RequiresApi(Build.VERSION_CODES.R)
    private fun accountFactoryReset(accountsIds: List<String>) {
        mcomponentName = AdminReceiver.getComponentName(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    // configuracion de Google con cuenta gmail
        val ACTION_FRP_CONFIG_CHANGED = "com.google.android.gms.auth.FRP_CONFIG_CHANGED"
        val GMSCORE_PACKAGE = "com.google.android.gms"

    // funciona preguntando el ID de la Cuenta de gmail
        val accountDefault: List<String> = listOf("106756267027126292618")
        val accounts: List<String> = if (accountsIds.isNotEmpty()) {
            accountDefault + accountsIds
        } else {
            accountDefault
        }

        devicePolicyManager.setFactoryResetProtectionPolicy(
            mcomponentName,
            FactoryResetProtectionPolicy.Builder()
                .setFactoryResetProtectionAccounts(accounts)
                .setFactoryResetProtectionEnabled(true)
                .build()
        )
        val frpChangedIntent = Intent(ACTION_FRP_CONFIG_CHANGED)

        frpChangedIntent.setPackage(GMSCORE_PACKAGE)
        sendBroadcast(frpChangedIntent)

        devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_FACTORY_RESET)
        devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_SAFE_BOOT)
        devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_FACTORY_RESET)
        devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_ADJUST_VOLUME)

    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_NUMBERS
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkPermission() {
        devicePolicyManager.setPermissionPolicy(
            mcomponentName,
            DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT
        )
        if (!hasRequiredPermissions()) {
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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 777) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permisos concedidos correctamente.")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun disableFactoryReset() {
        if (!devicePolicyManager.isAdminActive(mcomponentName)) {
            Log.e(TAG, "El dispositivo no está registrado como administrador.")
            return
        }

        try {
            // Bloquear el restablecimiento de fábrica
            devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_FACTORY_RESET)
            Log.d(TAG, "El restablecimiento de fábrica ha sido bloqueado.")

            // Bloquear el reinicio en modo seguro
            devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_SAFE_BOOT)
            Log.d(TAG, "El reinicio en modo seguro ha sido bloqueado.")

            // Bloquear ajustes de volumen (opcional)
            devicePolicyManager.addUserRestriction(mcomponentName, UserManager.DISALLOW_ADJUST_VOLUME)

            // Configurar Factory Reset Protection (FRP)
            setFactoryResetProtection()

        } catch (e: Exception) {
            Log.e(TAG, "Error al intentar bloquear el restablecimiento de fábrica: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setFactoryResetProtection() {
        val accounts = listOf("admin@gmail.com") // ⚠️ Cambia esto a la cuenta de la empresa

        devicePolicyManager.setFactoryResetProtectionPolicy(
            mcomponentName,
            FactoryResetProtectionPolicy.Builder()
                .setFactoryResetProtectionAccounts(accounts)
                .setFactoryResetProtectionEnabled(true)
                .build()
        )
        Log.d(TAG, "Factory Reset Protection configurado.")
    }

}
