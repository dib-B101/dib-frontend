package com.ssafy.dib.data.local.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.ssafy.dib.domain.auth.AuthSession
import com.ssafy.dib.domain.auth.AuthSessionStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SecureAuthSessionStore(context: Context) : AuthSessionStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun read(): AuthSession? {
        val encrypted = preferences.getString(KEY_SESSION, null) ?: return null
        return runCatching {
            val payload = json.decodeFromString<EncryptedPayload>(encrypted)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, payload.iv.decode()))
            }
            json.decodeFromString<AuthSessionPayload>(cipher.doFinal(payload.ciphertext.decode()).decodeToString())
                .toDomain()
        }.getOrElse {
            clear()
            null
        }
    }

    override fun save(session: AuthSession) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        val plaintext = json.encodeToString(AuthSessionPayload.from(session)).encodeToByteArray()
        val payload = EncryptedPayload(
            iv = cipher.iv.encode(),
            ciphertext = cipher.doFinal(plaintext).encode()
        )
        preferences.edit().putString(KEY_SESSION, json.encodeToString(payload)).apply()
    }

    override fun clear() {
        preferences.edit().remove(KEY_SESSION).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.decode(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    @Serializable
    private data class EncryptedPayload(val iv: String, val ciphertext: String)

    @Serializable
    private data class AuthSessionPayload(
        val memberId: String,
        val email: String,
        val nickname: String,
        val accessToken: String,
        val refreshToken: String,
        val accessExpiresAtEpochMillis: Long
    ) {
        fun toDomain() = AuthSession(
            memberId = memberId,
            email = email,
            nickname = nickname,
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessExpiresAtEpochMillis = accessExpiresAtEpochMillis
        )

        companion object {
            fun from(session: AuthSession) = AuthSessionPayload(
                memberId = session.memberId,
                email = session.email,
                nickname = session.nickname,
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                accessExpiresAtEpochMillis = session.accessExpiresAtEpochMillis
            )
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "dib_secure_session"
        const val KEY_SESSION = "encrypted_auth_session"
        const val KEY_ALIAS = "dib_auth_session_key"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
    }
}
