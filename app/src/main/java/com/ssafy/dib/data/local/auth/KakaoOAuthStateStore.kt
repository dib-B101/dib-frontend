package com.ssafy.dib.data.local.auth

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

class KakaoOAuthStateStore(
    context: Context,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun create(): String {
        val bytes = ByteArray(32).also(secureRandom::nextBytes)
        val state = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        preferences.edit()
            .putString(KEY_STATE, state)
            .putLong(KEY_CREATED_AT, now())
            .commit()
        return state
    }

    fun consume(returnedState: String?): Boolean {
        val expected = preferences.getString(KEY_STATE, null)
        val createdAt = preferences.getLong(KEY_CREATED_AT, 0L)
        preferences.edit().clear().commit()
        return !expected.isNullOrBlank() &&
            returnedState == expected &&
            now() - createdAt in 0..STATE_TTL_MILLIS
    }

    private companion object {
        const val PREFERENCES_NAME = "dib_kakao_oauth"
        const val KEY_STATE = "state"
        const val KEY_CREATED_AT = "created_at"
        const val STATE_TTL_MILLIS = 10 * 60 * 1_000L
    }
}
