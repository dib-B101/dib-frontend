package com.ssafy.dib.core.auth

import android.net.Uri

data class KakaoOAuthConfig(
    val restApiKey: String,
    val redirectUri: String
) {
    val isConfigured: Boolean
        get() = restApiKey.isNotBlank() && runCatching {
            val uri = Uri.parse(redirectUri)
            uri.scheme == "https" && !uri.host.isNullOrBlank() && !uri.path.isNullOrBlank()
        }.getOrDefault(false)

    fun authorizationUri(state: String): Uri = Uri.parse(AUTHORIZATION_URL).buildUpon()
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("client_id", restApiKey)
        .appendQueryParameter("redirect_uri", redirectUri)
        .appendQueryParameter("state", state)
        .build()

    fun parseCallback(uri: Uri): KakaoOAuthCallback {
        if (!matchesRedirect(uri)) return KakaoOAuthCallback.Invalid("잘못된 카카오 로그인 콜백입니다.")
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            return KakaoOAuthCallback.Failure(
                state = state,
                message = if (error == "access_denied") "카카오 로그인이 취소됐습니다."
                else uri.getQueryParameter("error_description") ?: "카카오 로그인에 실패했습니다."
            )
        }
        val code = uri.getQueryParameter("code")
        return if (code.isNullOrBlank() || state.isNullOrBlank()) {
            KakaoOAuthCallback.Invalid("카카오 로그인 응답이 올바르지 않습니다.")
        } else {
            KakaoOAuthCallback.Success(code, state)
        }
    }

    private fun matchesRedirect(uri: Uri): Boolean {
        val expected = Uri.parse(redirectUri)
        return uri.scheme == expected.scheme && uri.host == expected.host &&
            uri.port == expected.port && uri.path == expected.path
    }

    private companion object {
        const val AUTHORIZATION_URL = "https://kauth.kakao.com/oauth/authorize"
    }
}

sealed interface KakaoOAuthCallback {
    data class Success(val authorizationCode: String, val state: String) : KakaoOAuthCallback
    data class Failure(val state: String?, val message: String) : KakaoOAuthCallback
    data class Invalid(val message: String) : KakaoOAuthCallback
}
