package com.ssafy.dib.core.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object SignUp : Screen("signup")
    data object Login : Screen("login")
    data object FindEmail : Screen("find-email")
    data object PasswordResetLink : Screen("password-reset-link")
    data object PasswordReset : Screen("password-reset?token={resetToken}") {
        fun createRoute(resetToken: String) = "password-reset?token=${Uri.encode(resetToken)}"
    }
    data object Home : Screen("home")
    data object Feed : Screen("feed")
    data object LiveList : Screen("live-list")
    data object Search : Screen("search")
    data object RecommendedAuctions : Screen("recommended-auctions")
    data object Notifications : Screen("notifications")
    data object Categories : Screen("categories")
    data object Register : Screen("register")
    data object Trades : Screen("trades")
    data object My : Screen("my")
    data object Transaction : Screen("transaction/{role}/{orderId}") {
        fun createRoute(role: String, orderId: String) = "transaction/$role/$orderId"
    }
    data object OrderChat : Screen("order/{orderId}/chat") {
        fun createRoute(orderId: String) = "order/$orderId/chat"
    }
    data object Addresses : Screen("my/addresses")
    data object PaymentMethods : Screen("my/payment-methods")
    data object SettlementAccounts : Screen("my/accounts")
    data object Settlements : Screen("my/settlements")
    data object SettlementDetail : Screen("my/settlements/{settlementId}") {
        fun createRoute(settlementId: String) = "my/settlements/$settlementId"
    }
    data object NotificationSettings : Screen("my/notification-settings")
    data object ProfileEdit : Screen("my/profile-edit")
    data object FavoriteAuctions : Screen("my/favorites")
    data object MyAuctions : Screen("my/auctions")
    data object RegisteredProducts : Screen("my/registered-products")
    data object LiveManagement : Screen("my/live-broadcasts")
    data object LiveBroadcastConsole : Screen("my/live-broadcasts/{liveBroadcastId}/console") {
        fun createRoute(liveBroadcastId: String) = "my/live-broadcasts/$liveBroadcastId/console"
    }

    data object ProductEdit : Screen("product/{productId}/edit") {
        fun createRoute(productId: String) = "product/$productId/edit"
    }
    data object AuctionRegister : Screen("auction/register/{productId}") {
        fun createRoute(productId: String) = "auction/register/$productId"
    }
    data object Inquiries : Screen("my/inquiries")
    data object ReportHistory : Screen("my/reports")
    data object Withdrawal : Screen("my/withdrawal")
    data object ProductDetail : Screen("auction/{auctionId}") {
        fun createRoute(auctionId: String) = "auction/$auctionId"
    }
    data object ProductOverview : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
    data object ProductOverviewImages : Screen("product/{productId}/images/{initialPage}") {
        fun createRoute(productId: String, initialPage: Int) = "product/$productId/images/$initialPage"
    }
    data object ProductImages : Screen("auction/{auctionId}/images/{initialPage}") {
        fun createRoute(auctionId: String, initialPage: Int) = "auction/$auctionId/images/$initialPage"
    }
    data object SellerProfile : Screen("seller/{memberId}") {
        fun createRoute(memberId: String) = "seller/$memberId"
    }
    data object SellerReviews : Screen("seller/{memberId}/reviews") {
        fun createRoute(memberId: String) = "seller/$memberId/reviews"
    }
    data object SellerListings : Screen("seller/{memberId}/listings") {
        fun createRoute(memberId: String) = "seller/$memberId/listings"
    }
    data object SellerReport : Screen("seller/{memberId}/report") {
        fun createRoute(memberId: String) = "seller/$memberId/report"
    }
    data object ProductReport : Screen("auction/{auctionId}/report") {
        fun createRoute(auctionId: String) = "auction/$auctionId/report"
    }
}
