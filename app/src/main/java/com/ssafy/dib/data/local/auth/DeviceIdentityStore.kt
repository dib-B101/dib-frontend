package com.ssafy.dib.data.local.auth

import android.content.Context
import java.util.UUID

class DeviceIdentityStore(context: Context) {
    private val preferences = context.getSharedPreferences("dib_device", Context.MODE_PRIVATE)

    fun getOrCreate(): String {
        preferences.getString(KEY_DEVICE_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also { generated ->
            preferences.edit().putString(KEY_DEVICE_ID, generated).apply()
        }
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}
