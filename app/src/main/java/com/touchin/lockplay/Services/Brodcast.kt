package com.touchin.lockplay.Services

import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.admin.DevicePolicyManager
import android.content.*
import android.os.Build
import android.os.UserManager
import android.util.Log
import com.touchin.lockplay.AdminReceiver
import com.touchin.lockplay.Workers.Job

class Brodcast : BroadcastReceiver() {
    private val TAG = "BROADCAST"

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.i(TAG, "🔵 Pantalla Encendida")
                aplicarRestricciones(context)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "♻️ Dispositivo Reiniciado")
                aplicarRestricciones(context)
                startJobScheduler(context)
                iniciarAplicacion(context)
            }

            "CLOSE_ACTIVITIES" -> {
                if (context is Activity) {
                    context.finish()
                }
            }
        }
    }

    private fun aplicarRestricciones(context: Context) {
        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, AdminReceiver::class.java)

        if (devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
            Log.i(TAG, "✅ La app sigue siendo Device Owner. Aplicando restricciones...")

            devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_FACTORY_RESET)
            devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_SAFE_BOOT)
            devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_ADJUST_VOLUME)

            Log.i(TAG, "🔒 Restricciones aplicadas correctamente.")
        } else {
            Log.e(TAG, "🚨 La app NO es Device Owner. No se pueden aplicar restricciones.")
        }
    }

    private fun startJobScheduler(context: Context) {
        val serviceName = ComponentName(context, Job::class.java)
        val builder = JobInfo.Builder(20, serviceName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setMinimumLatency(5 * 60 * 1000) // 5 minutos mínimo
        } else {
            builder.setPeriodic(15 * 60 * 1000) // Mínimo 15 minutos en API 24+
        }

        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        builder.setPersisted(true)

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val result = jobScheduler.schedule(builder.build())

        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.i(TAG, "📅 JobScheduler registrado correctamente.")
        } else {
            Log.e(TAG, "⚠️ Error al registrar JobScheduler.")
        }
    }

    private fun iniciarAplicacion(context: Context) {
        val launchIntent = Intent(context, com.touchin.lockplay.ui.WelcomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(launchIntent)

        Log.i(TAG, "🚀 Aplicación iniciada después del reinicio.")
    }
}
