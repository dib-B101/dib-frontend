package com.ssafy.dib.core.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Feed : Screen("feed")
    data object Search : Screen("search")
    data object Notifications : Screen("notifications")
    data object Categories : Screen("categories")
    data object Register : Screen("register")
    data object Trades : Screen("trades")
    data object My : Screen("my")
    data object Transaction : Screen("transaction/{role}") {
        fun createRoute(role: String) = "transaction/$role"
    }
    data object Addresses : Screen("my/addresses")
    data object SettlementAccounts : Screen("my/accounts")
    data object NotificationSettings : Screen("my/notification-settings")
    data object ProfileEdit : Screen("my/profile-edit")
    data object FavoriteAuctions : Screen("my/favorites")
    data object RegisteredProducts : Screen("my/registered-products")
    data object Inquiries : Screen("my/inquiries")
    data object ReportHistory : Screen("my/reports")
    data object Withdrawal : Screen("my/withdrawal")
    data object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
    data object ProductImages : Screen("product/{productId}/images/{initialPage}") {
        fun createRoute(productId: String, initialPage: Int) = "product/$productId/images/$initialPage"
    }
    data object SellerProfile : Screen("seller/seller01")
    data object SellerReviews : Screen("seller/seller01/reviews")
    data object SellerListings : Screen("seller/seller01/listings")
    data object SellerReport : Screen("seller/seller01/report")
    data object ProductReport : Screen("product/{productId}/report") {
        fun createRoute(productId: String) = "product/$productId/report"
    }
}
