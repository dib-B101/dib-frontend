package com.ssafy.dib.core.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
}
