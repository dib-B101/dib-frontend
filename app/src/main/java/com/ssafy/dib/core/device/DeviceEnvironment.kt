package com.ssafy.dib.core.device

import android.os.Build

/**
 * 기기 종류 판별을 한 곳에 모아둔다.
 * 카메라 송출은 에뮬레이터의 가짜 카메라로는 검증이 안 돼서, 실기기에서만 시도하도록 걸러내야 한다.
 */
object DeviceEnvironment {

    val isEmulator: Boolean by lazy { detectEmulator() }

    private fun detectEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val model = Build.MODEL.orEmpty()
        val product = Build.PRODUCT.orEmpty()
        val hardware = Build.HARDWARE.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val device = Build.DEVICE.orEmpty()
        val manufacturer = Build.MANUFACTURER.orEmpty()

        return fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            fingerprint.contains("generic/google_sdk") ||
            fingerprint.contains("generic/vbox") ||
            fingerprint.contains("emulator", ignoreCase = true) ||
            model.contains("google_sdk", ignoreCase = true) ||
            model.contains("Emulator", ignoreCase = true) ||
            model.contains("Android SDK built for", ignoreCase = true) ||
            model.contains("sdk_gphone", ignoreCase = true) ||
            manufacturer.contains("Genymotion", ignoreCase = true) ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product == "google_sdk" ||
            product.contains("sdk_gphone", ignoreCase = true) ||
            product.contains("emulator", ignoreCase = true) ||
            product.contains("simulator", ignoreCase = true) ||
            hardware.contains("goldfish", ignoreCase = true) ||
            hardware.contains("ranchu", ignoreCase = true) ||
            hardware.contains("vbox86", ignoreCase = true)
    }
}
