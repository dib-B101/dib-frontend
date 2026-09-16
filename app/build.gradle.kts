plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ssafy.dib"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ssafy.dib"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiBaseUrl = providers.gradleProperty("DIB_API_BASE_URL").orElse("").get()
        val webSocketUrl = providers.gradleProperty("DIB_WS_URL").orElse("").get()
        val tossClientKey = providers.gradleProperty("DIB_TOSS_CLIENT_KEY").orElse("").get()
        val sessionIdleTimeoutMinutes = providers.gradleProperty("DIB_SESSION_IDLE_TIMEOUT_MINUTES")
            .orElse("30")
            .get()
            .toLongOrNull()
            ?.takeIf { it > 0 }
            ?: 30L
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "WEB_SOCKET_URL", "\"${webSocketUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "TOSS_CLIENT_KEY", "\"${tossClientKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("long", "SESSION_IDLE_TIMEOUT_MILLIS", "${sessionIdleTimeoutMinutes * 60_000L}L")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui.compose)
}
