package com.touchin.lockplay.Workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WorkerServices(appContext: Context, params: WorkerParameters): Worker(appContext, params){

    val TAG = "WORKERMANAGER"
    override fun doWork(): Result {
        try {
            Log.d(TAG, "SUCCES")
            return Result.success()
        }catch (e:Throwable){
            return Result.failure()
        }
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://hf292544-3000.brs.devtunnels.ms/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}