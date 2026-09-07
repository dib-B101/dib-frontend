package com.ssafy.dib.core.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
}
