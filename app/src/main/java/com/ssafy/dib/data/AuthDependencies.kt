package com.ssafy.dib.data

import android.content.Context
import com.ssafy.dib.core.network.AccessTokenProvider
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.core.network.GuestSessionProvider
import com.ssafy.dib.core.network.NetworkConfig
import com.ssafy.dib.data.local.auth.DeviceIdentityStore
import com.ssafy.dib.data.local.auth.SecureAuthSessionStore
import com.ssafy.dib.data.remote.auth.AuthRemoteDataSource
import com.ssafy.dib.data.repository.AuthRepositoryImpl
import com.ssafy.dib.domain.auth.AuthRepository
import java.util.UUID

class AuthDependencies(context: Context) {
    private val appContext = context.applicationContext
    private val sessionStore = SecureAuthSessionStore(appContext)
    private val deviceStore = DeviceIdentityStore(appContext)
    private val guestPreferences = appContext.getSharedPreferences("dib_guest", Context.MODE_PRIVATE)

    val networkConfig: NetworkConfig = NetworkConfig.fromBuildConfig()
    val deviceId: String = deviceStore.getOrCreate()
    val repository: AuthRepository

    init {
        val client = DibHttpClient(
            config = networkConfig,
            accessTokenProvider = AccessTokenProvider { sessionStore.read()?.accessToken },
            guestSessionProvider = GuestSessionProvider { guestSessionId() }
        )
        repository = AuthRepositoryImpl(AuthRemoteDataSource(client), sessionStore)
    }

    private fun guestSessionId(): String {
        guestPreferences.getString(KEY_GUEST_SESSION_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also { generated ->
            guestPreferences.edit().putString(KEY_GUEST_SESSION_ID, generated).apply()
        }
    }

    private companion object {
        const val KEY_GUEST_SESSION_ID = "guest_session_id"
    }
}
