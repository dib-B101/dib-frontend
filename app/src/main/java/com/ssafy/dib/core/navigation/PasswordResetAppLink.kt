package com.ssafy.dib.core.navigation

import java.net.URI

internal fun passwordResetTokenFromAppLink(link: String, configuredKakaoRedirectUri: String): String? {
    val expectedHost = runCatching { URI(configuredKakaoRedirectUri).host }.getOrNull() ?: return null
    val uri = runCatching { URI(link) }.getOrNull() ?: return null
    if (uri.scheme != "https" || !uri.host.equals(expectedHost, ignoreCase = true) ||
        uri.port != -1 || uri.userInfo != null || uri.fragment != null || uri.path != "/password/reset"
    ) return null
    val query = uri.rawQuery ?: return null
    val token = query.removePrefix("token=")
    return token.takeIf { query.startsWith("token=") && it.matches(Regex("[A-Za-z0-9_-]{43}")) }
}
