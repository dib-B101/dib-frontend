package com.ssafy.dib.core.network

import kotlinx.serialization.json.Json

object DibJson {
    val instance = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
}
