package com.touchin.lockplay

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.GsonBuilder
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

object RetrofitClient {
    private fun getRetrofit(): Retrofit {
        val local = false
        var baseUrl: String
        if (local == true) {
            baseUrl = "https://768p81cz-3000.brs.devtunnels.ms/api/"
        } else {
            baseUrl = "https://lockplay.payplay-ec.com/api/"
        }
        val gson = GsonBuilder()
            .serializeNulls()
            .create()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val retrofit: APIService by lazy {
        getRetrofit().create(APIService::class.java)
    }

    fun connectedInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

interface APIService {
    // INFORMACION DEL EQUIPO IMAGEN, FECHA DE PAGO Y INFORMACION DE EMPRESA
    @GET("devices/informationForDevice/{codeEnrolmment}")
    suspend fun getInfortmationDevice(@Path("codeEnrolmment") codeEnrolmment: String): Response<DeviceResponse>

    // VINCULACION DE LICENCIA.
    @POST("devices")
    suspend fun enrrollmentDevice(@Body requestBody: ObjectEnrollment): Response<ResponseEnrrolment>

    //CONULTAR EL SIM SI TIENE
    @POST("devices/sims/{codeEnrollment}")
    suspend fun createSim(@Path("codeEnrollment") codeEnrollment: String, @Body simBody: Any): Response<ResponseSims>

    //VERIFICA SI TIENE CHIP APROVADO
    @GET("devices/sims/approved/{codeEnrollment}/{iccid}")
    suspend fun getSimApproved(@Path("codeEnrollment") codeEnrollment: String, @Path("iccid") iccid: String): Response<ResponseApprovedSim>

    //PARA DESBLOQUEAR EL EQUIPO POR CODIGO, SIN INTERNTE
    @POST("devices/editDevice/{codeEnrollment}")
    suspend fun editDevice(
        @Path("codeEnrollment") codeEnrollment: String,
        @Body requestBody: ObjectEditDevice
    ): Response<Any>
}