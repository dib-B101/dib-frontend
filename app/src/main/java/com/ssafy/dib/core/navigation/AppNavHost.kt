package com.ssafy.dib.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.feature.auction.ProductDetailScreen
import com.ssafy.dib.feature.auction.ProductImageViewerScreen
import com.ssafy.dib.feature.auction.ProductReportScreen
import com.ssafy.dib.feature.auction.SellerProfileScreen
import com.ssafy.dib.feature.auction.SellerListingsScreen
import com.ssafy.dib.feature.auction.SellerReportScreen
import com.ssafy.dib.feature.auction.SellerReviewsScreen
import com.ssafy.dib.feature.auction.BidDepositPaymentScreen
import com.ssafy.dib.feature.auth.LoginScreen
import com.ssafy.dib.feature.auth.SplashScreen
import com.ssafy.dib.feature.auth.WelcomeScreen
import com.ssafy.dib.feature.feed.LiveFeedScreen
import com.ssafy.dib.feature.home.HomeScreen
import com.ssafy.dib.feature.home.AuctionSearchScreen
import com.ssafy.dib.feature.home.NotificationCenterScreen
import com.ssafy.dib.feature.home.CategoryScreen
import com.ssafy.dib.feature.main.AddressManagementScreen
import com.ssafy.dib.feature.main.FavoriteAuctionsScreen
import com.ssafy.dib.feature.main.InquiryHistoryScreen
import com.ssafy.dib.feature.main.MyPageScreen
import com.ssafy.dib.feature.main.MyTradesScreen
import com.ssafy.dib.feature.main.NotificationSettingsScreen
import com.ssafy.dib.feature.main.ProfileEditScreen
import com.ssafy.dib.feature.main.ProductRegisterScreen
import com.ssafy.dib.feature.main.RegisteredProductsScreen
import com.ssafy.dib.feature.main.ReportHistoryScreen
import com.ssafy.dib.feature.main.SettlementAccountsScreen
import com.ssafy.dib.feature.main.TransactionScreen
import com.ssafy.dib.feature.main.WithdrawalScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val session = remember(context) { context.getSharedPreferences("dib_session", 0) }
    var signedIn by remember { mutableStateOf(session.getBoolean("signed_in", false)) }
    var depositPaidProductIds by remember {
        mutableStateOf(session.getStringSet("paid_deposits", emptySet()).orEmpty().toSet())
    }

    fun completeLogin() {
        signedIn = true
        session.edit().putBoolean("signed_in", true).apply()
        navController.navigate(Screen.Home.route) { popUpTo(Screen.Welcome.route) { inclusive = true } }
    }

    fun navigateMain(tab: DibMainTab) {
        val route = when (tab) {
            DibMainTab.Home -> Screen.Home.route
            DibMainTab.Feed -> Screen.Feed.route
            DibMainTab.Register -> Screen.Register.route
            DibMainTab.Trades -> Screen.Trades.route
            DibMainTab.My -> Screen.My.route
        }
        navController.navigate(route) {
            if (tab != DibMainTab.Register) popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = tab != DibMainTab.Register
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onFinished = {
                navController.navigate(if (signedIn) Screen.Home.route else Screen.Welcome.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onKakaoStart = ::completeLogin,
                onEmailSignup = { navController.navigate(Screen.Login.route) },
                onLogin = { navController.navigate(Screen.Login.route) }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onBack = navController::navigateUp,
                onLogin = ::completeLogin
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onLiveClick = { navController.navigate(Screen.Feed.route) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onCategoryClick = { navController.navigate(Screen.Categories.route) },
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.Categories.route) {
            CategoryScreen(
                onBack = navController::navigateUp,
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.Search.route) {
            AuctionSearchScreen(
                onBack = navController::navigateUp,
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.Notifications.route) {
            NotificationCenterScreen(onBack = navController::navigateUp, onTabSelected = ::navigateMain)
        }
        composable(Screen.Feed.route) { backStackEntry ->
            val paidBidAmount by backStackEntry.savedStateHandle
                .getStateFlow("paidBidAmount", 0).collectAsState()
            val paidBidProductId by backStackEntry.savedStateHandle
                .getStateFlow("paidBidProductId", "").collectAsState()
            LiveFeedScreen(
                paidBidAmount = paidBidAmount,
                onPaymentConsumed = {
                    backStackEntry.savedStateHandle["paidBidAmount"] = 0
                    backStackEntry.savedStateHandle["paidBidProductId"] = ""
                },
                onClose = { navController.navigateUp() },
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                onDepositPayment = { productId, amount ->
                    if (productId in depositPaidProductIds) {
                        backStackEntry.savedStateHandle["paidBidProductId"] = productId
                        backStackEntry.savedStateHandle["paidBidAmount"] = amount
                    } else {
                        navController.navigate(Screen.BidDepositPayment.createRoute(productId, amount))
                    }
                }
            )
        }
        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val paidBidAmount by backStackEntry.savedStateHandle
                .getStateFlow("paidBidAmount", 0).collectAsState()
            ProductDetailScreen(
                productId = backStackEntry.arguments?.getString("productId").orEmpty(),
                onBack = navController::navigateUp,
                onImageClick = { page ->
                    navController.navigate(
                        Screen.ProductImages.createRoute(
                            backStackEntry.arguments?.getString("productId").orEmpty(),
                            page
                        )
                    )
                },
                onSellerClick = { navController.navigate(Screen.SellerProfile.route) },
                onReportClick = {
                    navController.navigate(
                        Screen.ProductReport.createRoute(
                            backStackEntry.arguments?.getString("productId").orEmpty()
                        )
                    )
                },
                onTransactionClick = { navController.navigate(Screen.Transaction.createRoute("buyer")) },
                paidBidAmount = paidBidAmount,
                onPaymentConsumed = { backStackEntry.savedStateHandle["paidBidAmount"] = 0 },
                onDepositPayment = { amount ->
                    val productId = backStackEntry.arguments?.getString("productId").orEmpty()
                    if (productId in depositPaidProductIds) {
                        backStackEntry.savedStateHandle["paidBidAmount"] = amount
                    } else {
                        navController.navigate(
                            Screen.BidDepositPayment.createRoute(
                                productId,
                                amount
                            )
                        )
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            ProductRegisterScreen(onBack = navController::navigateUp)
        }
        composable(Screen.Trades.route) {
            MyTradesScreen(
                onTabSelected = ::navigateMain,
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                onTransactionClick = { role -> navController.navigate(Screen.Transaction.createRoute(role)) }
            )
        }
        composable(
            route = Screen.Transaction.route,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            TransactionScreen(role = backStackEntry.arguments?.getString("role").orEmpty(), onBack = navController::navigateUp)
        }
        composable(Screen.My.route) {
            MyPageScreen(
                onTabSelected = ::navigateMain,
                onProfileEditClick = { navController.navigate(Screen.ProfileEdit.route) },
                onFavoritesClick = { navController.navigate(Screen.FavoriteAuctions.route) },
                onRegisteredProductsClick = { navController.navigate(Screen.RegisteredProducts.route) },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onInquiriesClick = { navController.navigate(Screen.Inquiries.route) },
                onAddressesClick = { navController.navigate(Screen.Addresses.route) },
                onAccountsClick = { navController.navigate(Screen.SettlementAccounts.route) },
                onNotificationSettingsClick = { navController.navigate(Screen.NotificationSettings.route) },
                onReportsClick = { navController.navigate(Screen.ReportHistory.route) },
                onWithdrawalClick = { navController.navigate(Screen.Withdrawal.route) },
                onLogout = {
                    signedIn = false
                    session.edit().putBoolean("signed_in", false).apply()
                    navController.navigate(Screen.Welcome.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                }
            )
        }
        composable(Screen.Addresses.route) {
            AddressManagementScreen(onBack = navController::navigateUp, onTabSelected = ::navigateMain)
        }
        composable(Screen.SettlementAccounts.route) {
            SettlementAccountsScreen(onBack = navController::navigateUp, onTabSelected = ::navigateMain)
        }
        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(onBack = navController::navigateUp, onTabSelected = ::navigateMain)
        }
        composable(Screen.ProfileEdit.route) {
            ProfileEditScreen(onBack = navController::navigateUp)
        }
        composable(Screen.FavoriteAuctions.route) {
            FavoriteAuctionsScreen(
                onBack = navController::navigateUp,
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.RegisteredProducts.route) {
            RegisteredProductsScreen(
                onBack = navController::navigateUp,
                onRegister = { navController.navigate(Screen.Register.route) },
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.Inquiries.route) {
            InquiryHistoryScreen(onBack = navController::navigateUp, onTabSelected = ::navigateMain)
        }
        composable(Screen.ReportHistory.route) {
            ReportHistoryScreen(onBack = navController::navigateUp)
        }
        composable(Screen.Withdrawal.route) {
            WithdrawalScreen(
                onBack = navController::navigateUp,
                onOpenTrades = { navigateMain(DibMainTab.Trades) },
                onComplete = {
                    signedIn = false
                    session.edit().clear().apply()
                    navController.navigate(Screen.Welcome.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                }
            )
        }
        composable(
            route = Screen.ProductImages.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("initialPage") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            ProductImageViewerScreen(
                productId = backStackEntry.arguments?.getString("productId").orEmpty(),
                initialPage = backStackEntry.arguments?.getInt("initialPage") ?: 0,
                onClose = navController::navigateUp
            )
        }
        composable(Screen.SellerProfile.route) {
            SellerProfileScreen(
                onBack = navController::navigateUp,
                onReviewsClick = { navController.navigate(Screen.SellerReviews.route) },
                onListingsClick = { navController.navigate(Screen.SellerListings.route) },
                onReportClick = { navController.navigate(Screen.SellerReport.route) }
            )
        }
        composable(Screen.SellerReviews.route) {
            SellerReviewsScreen(onBack = navController::navigateUp)
        }
        composable(Screen.SellerListings.route) {
            SellerListingsScreen(
                onBack = navController::navigateUp,
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) }
            )
        }
        composable(Screen.SellerReport.route) {
            SellerReportScreen(
                onBack = navController::navigateUp,
                onSubmitted = { navController.navigateUp() }
            )
        }
        composable(
            route = Screen.ProductReport.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) {
            ProductReportScreen(
                onBack = navController::navigateUp,
                onSubmitted = { navController.navigateUp() }
            )
        }
        composable(
            route = Screen.BidDepositPayment.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("bidAmount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            val bidAmount = backStackEntry.arguments?.getInt("bidAmount") ?: 0
            BidDepositPaymentScreen(
                productId = productId,
                bidAmount = bidAmount,
                onBack = navController::navigateUp,
                onReturnToAuction = {
                    val updatedPaidProducts = depositPaidProductIds + productId
                    depositPaidProductIds = updatedPaidProducts
                    session.edit().putStringSet("paid_deposits", updatedPaidProducts).apply()
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("paidBidProductId", productId)
                        set("paidBidAmount", bidAmount)
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}
