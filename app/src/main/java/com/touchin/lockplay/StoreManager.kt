package com.touchin.lockplay

import android.content.Context
import androidx.datastore.preferences.PreferencesProto.Value
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.*


private val Context.dataStore by preferencesDataStore(name = "CODE_ENROLLMENT_PREFERENCES")
class StoreManager(context: Context) {
    private val PREFS_CODE_DEVICE_ID = "MyPrefsFile"
    val TAG = "DataStoreManager"

    private val dataStore = context.dataStore

    suspend fun saveDataEnrollment(
        codeEnrollment: String,
        company: String,
        contact1: String,
        contact2: String,
        imageCompany: String,
        codeUnlock: String,
    ) {

        dataStore.edit { preferences ->
            preferences[STRING_CODE_ENRROLLMENT] = codeEnrollment
            preferences[STRING_COMPANY] = company
            preferences[STRING_CONTACT1] = contact1
            preferences[STRING_CONTACT2] = contact2
            preferences[STRING_IMAGE_COMPANY] = imageCompany
            preferences[STRING_CODE_UNLOCK] = codeUnlock
        }
    }
    suspend fun getStringValue(key: Preferences.Key<String>): String  {
        val preferences = dataStore.data.first()
        return preferences[key] ?: DEFAULT_STRING_VALUE
    }
    suspend fun setStringValue(key: Preferences.Key<String>, value: String) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun getBooleanValue(key: Preferences.Key<Boolean>):Boolean{
        val preferences = dataStore.data.first()
        return preferences[key] ?: false
    }
    suspend fun getCodeUnlock(key: Preferences.Key<String>): String? {
        val preferences = dataStore.data.first()
        return preferences[key]
    }
    suspend fun saveCodeUnlock(codeUnlock: String){
        dataStore.edit { preferences ->
            preferences[STRING_CODE_UNLOCK] = codeUnlock
        }
    }
    suspend fun setModeKiosk(isActiveKiosk: Boolean){
        dataStore.edit { preferences ->
            preferences[BOOLEAN_MODE_KIOSK] = isActiveKiosk
        }
    }

    companion object {
        val STRING_CODE_ENRROLLMENT: Preferences.Key<String> = stringPreferencesKey("codeEnrollmentProfile")
        val STRING_COMPANY: Preferences.Key<String> = stringPreferencesKey("company")
        val STRING_CONTACT1: Preferences.Key<String> = stringPreferencesKey("contact1")
        val STRING_CONTACT2: Preferences.Key<String> = stringPreferencesKey("contact2")
        val STRING_IMAGE_COMPANY: Preferences.Key<String> = stringPreferencesKey("imageCompany")
        val STRING_CODE_UNLOCK: Preferences.Key<String> = stringPreferencesKey("codeUnlock")
        val STRING_SIM_ICCID_1: Preferences.Key<String> = stringPreferencesKey("simICCID_1")
        val BOOLEAN_MODE_KIOSK: Preferences.Key<Boolean> = booleanPreferencesKey("modeKiosk")
        val STRING_TOKEN_FIREBASE: Preferences.Key<String> = stringPreferencesKey("token")
        private const val DEFAULT_STRING_VALUE = "string_null"
        private const val DEFAULT_UNLOCK_VALUE = "14523"
    }

}