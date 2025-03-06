package com.touchin.lockplay

import com.google.gson.annotations.SerializedName


data class DeviceResponse(
    @SerializedName("id") var id: String = "",
    @SerializedName("code_unlock") var codeUnlock: String = "",
    @SerializedName("dateDue") var dateDue: String = "",
    @SerializedName("company") var company: String = "",
    @SerializedName("contact1") var contact1: String = "",
    @SerializedName("contact2") var contact2: String = "",
    @SerializedName("dateLock") var dateLock: String = "",
    @SerializedName("lock") var lock: Boolean = false,
    @SerializedName("provisioned") var provisioned: Boolean = false,
    @SerializedName("image") var image: String = ""
)


data class ObjectEnrollment(
    @SerializedName("imei") var imei: String,
    @SerializedName("serie") var serie: String,
    @SerializedName("modelo") var model: String,
    @SerializedName("code_enrollment") val code_enrollment: CodeEnrollmentID,
    @SerializedName("token") val token: String,
    @SerializedName("version_apk") val version_apk: String

)


data class CodeEnrollmentID(@SerializedName("code") var code: String)

data class DeviceProfile(
    val codeEnrollmentProfile: String,
    val company: String,
    val contact1: String,
    val contact2: String,
    val codeUnlock: String,
    val imageCompany: String,
    val accounts: List<String> = emptyList()
)

data class ResponseEnrrolment(var statusCode: Int, var message: String, var dataResponse: DeviceProfile)
data class ResponseSims(var message: String)
data class ResponseApprovedSim(var approved: Boolean)



data class ObjectSim(
    val iccid: String,
    val imsi: String,
    val number: String?,
    val approved: Boolean
)

data class ObjectEditDevice(
    @SerializedName("lock") val lock: Any,
    @SerializedName("date_lock") val dateLock: String? = null,
    @SerializedName("location") val location: String? = "noLocation",
    @SerializedName("token") val token: String? = "noToken"
)
