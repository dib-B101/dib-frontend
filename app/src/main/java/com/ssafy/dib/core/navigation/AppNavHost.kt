package com.ssafy.dib.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.feature.auction.ProductDetailScreen
import com.ssafy.dib.feature.auction.BidDepositPaymentScreen
import com.ssafy.dib.feature.auction.ProductImageViewerScreen
import com.ssafy.dib.feature.auction.ProductReportScreen
import com.ssafy.dib.feature.auction.SellerProfileScreen
import com.ssafy.dib.feature.auction.SellerListingsScreen
import com.ssafy.dib.feature.auction.SellerReportScreen
import com.ssafy.dib.feature.auction.SellerReviewsScreen
import com.ssafy.dib.feature.auth.LoginScreen
import com.ssafy.dib.feature.auth.SplashScreen
import com.ssafy.dib.feature.auth.WelcomeScreen
import com.ssafy.dib.feature.feed.LiveFeedScreen
import com.ssafy.dib.feature.home.HomeScreen
import com.ssafy.dib.feature.home.HomeAuction
import com.ssafy.dib.feature.home.toHomeAuction
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
import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.AuthDependencies
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val auth = remember(context) { AuthDependencies(context) }
    val coroutineScope = rememberCoroutineScope()
    val session = remember(context) { context.getSharedPreferences("dib_session", 0) }
    var signedIn by remember { mutableStateOf<Boolean?>(null) }
    var loginLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var remoteAuctions by remember { mutableStateOf<List<HomeAuction>?>(null) }
    var auctionsLoading by remember { mutableStateOf(false) }
    var auctionsError by remember { mutableStateOf<String?>(null) }
    var auctionsRevision by remember { mutableStateOf(0) }
    var depositPaidProductIds by remember {
        mutableStateOf(session.getStringSet("paid_deposits", emptySet()).orEmpty().toSet())
    }

    fun navigateMain(tab: DibMainTab) {
        if (signedIn != true && tab in setOf(DibMainTab.Register, DibMainTab.Trades, DibMainTab.My)) {
            navController.navigate(Screen.Login.route)
            return
        }
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

    LaunchedEffect(signedIn) {
        while (signedIn == true) {
            val current = withContext(Dispatchers.IO) { auth.repository.currentSession() } ?: break
            val waitMillis = max(5_000L, current.accessExpiresAtEpochMillis - System.currentTimeMillis() - 60_000L)
            delay(waitMillis)
            when (val refreshed = withContext(Dispatchers.IO) { auth.repository.refresh(auth.deviceId) }) {
                is ApiResult.Success -> Unit
                is ApiResult.Failure -> if (refreshed.error.requiresLogin) {
                    signedIn = false
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                } else {
                    delay(30_000L)
                }
            }
        }
    }

    LaunchedEffect(auctionsRevision, signedIn) {
        if (!auth.networkConfig.isRestConfigured) return@LaunchedEffect
        auctionsLoading = true
        auctionsError = null
        when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getActiveGeneralAuctions() }) {
            is ApiResult.Success -> remoteAuctions = result.value.map { it.toHomeAuction() }
            is ApiResult.Failure -> {
                auctionsError = result.error.message.ifBlank { "경매 목록을 불러오지 못했어요." }
                if (result.error.requiresLogin) signedIn = false
            }
        }
        auctionsLoading = false
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onFinished = {
                coroutineScope.launch {
                    val restored = withContext(Dispatchers.IO) {
                        val current = auth.repository.currentSession()
                        when {
                            current == null -> false
                            !current.needsRefresh(System.currentTimeMillis()) -> true
                            else -> auth.repository.refresh(auth.deviceId) is ApiResult.Success
                        }
                    }
                    signedIn = restored
                    navController.navigate(if (restored) Screen.Home.route else Screen.Welcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            })
        }
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onKakaoStart = { navController.navigate(Screen.Login.route) },
                onEmailSignup = { navController.navigate(Screen.Login.route) },
                onLogin = { navController.navigate(Screen.Login.route) },
                onBrowse = {
                    signedIn = false
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Welcome.route) { inclusive = true } }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onBack = navController::navigateUp,
                isLoading = loginLoading,
                errorMessage = loginError,
                onLogin = { email, password ->
                    loginLoading = true
                    loginError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.repository.login(email, password, auth.deviceId)
                        }) {
                            is ApiResult.Success -> {
                                signedIn = true
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                            is ApiResult.Failure -> {
                                loginError = when (result.error.code) {
                                    ApiErrorCodes.CLIENT_NOT_CONFIGURED -> "개발 서버 주소가 설정되지 않았어요. 이전 화면에서 둘러보기를 이용해주세요."
                                    "INVALID_CREDENTIALS" -> "이메일 또는 비밀번호가 올바르지 않아요."
                                    "ACCOUNT_SUSPENDED", "ACCOUNT_BLOCKED" -> result.error.message
                                    else -> result.error.message.ifBlank { "로그인하지 못했습니다. 잠시 후 다시 시도해주세요." }
                                }
                            }
                        }
                        loginLoading = false
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                isAuthenticated = signedIn == true,
                remoteAuctions = remoteAuctions,
                remoteLoading = auctionsLoading,
                remoteError = auctionsError,
                onRetry = { auctionsRevision++ },
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onLiveClick = { navController.navigate(Screen.Feed.route) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onNotificationsClick = {
                    if (signedIn == true) navController.navigate(Screen.Notifications.route)
                    else navController.navigate(Screen.Login.route)
                },
                onCategoryClick = { navController.navigate(Screen.Categories.route) },
                onLoginRequired = { navController.navigate(Screen.Login.route) },
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.Categories.route) {
            CategoryScreen(
                onBack = navController::navigateUp,
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onNotificationsClick = {
                    if (signedIn == true) navController.navigate(Screen.Notifications.route)
                    else navController.navigate(Screen.Login.route)
                },
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
            LiveFeedScreen(
                isAuthenticated = signedIn == true,
                paidBidAmount = paidBidAmount,
                depositPaid = "camera" in depositPaidProductIds,
                onPaymentConsumed = { backStackEntry.savedStateHandle["paidBidAmount"] = 0 },
                onClose = { navController.navigateUp() },
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                onLoginRequired = { navController.navigate(Screen.Login.route) },
                onDepositPayment = { productId, submission ->
                    if (signedIn != true) {
                        navController.navigate(Screen.Login.route)
                        return@LiveFeedScreen
                    }
                    backStackEntry.savedStateHandle["pendingPaymentMethodId"] = submission.paymentMethodId
                    backStackEntry.savedStateHandle["pendingAddressId"] = submission.addressId
                    if (productId in depositPaidProductIds) {
                        backStackEntry.savedStateHandle["paidBidAmount"] = submission.amount
                    } else {
                        navController.navigate(Screen.BidDepositPayment.createRoute(productId, submission.amount))
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
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            var remoteDetail by remember(productId) {
                mutableStateOf(remoteAuctions?.firstOrNull { it.id == productId })
            }
            var detailLoading by remember(productId) { mutableStateOf(false) }
            var detailError by remember(productId) { mutableStateOf<String?>(null) }
            var detailRevision by remember(productId) { mutableStateOf(0) }
            var realtimeState by remember(productId) { mutableStateOf<RealtimeConnectionState?>(null) }
            var realtimeNotice by remember(productId) { mutableStateOf<String?>(null) }
            LaunchedEffect(productId, detailRevision, signedIn) {
                if (!auth.networkConfig.isRestConfigured) return@LaunchedEffect
                detailLoading = true
                detailError = null
                when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getAuction(productId) }) {
                    is ApiResult.Success -> remoteDetail = result.value.toHomeAuction()
                    is ApiResult.Failure -> {
                        detailError = result.error.message.ifBlank { "경매 상세를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                detailLoading = false
            }
            DisposableEffect(productId, auth.networkConfig.isWebSocketConfigured) {
                val connection = if (auth.networkConfig.isWebSocketConfigured) {
                    auth.createAuctionRealtimeConnection().also { realtime ->
                        realtime.start(
                            auctionId = productId,
                            onUpdate = { update ->
                                coroutineScope.launch {
                                    val base = remoteDetail ?: remoteAuctions?.firstOrNull { it.id == productId }
                                    if (base != null) {
                                        remoteDetail = base.copy(
                                            price = update.currentPrice ?: base.price,
                                            bidCount = update.bidCount ?: base.bidCount,
                                            remainingSeconds = update.remainingSeconds ?: base.remainingSeconds,
                                            status = update.status ?: base.status,
                                            isHighestBidder = update.isHighestBidder ?: base.isHighestBidder
                                        )
                                    }
                                    update.message?.let { message ->
                                        realtimeNotice = "$message|${update.occurredAt.orEmpty()}"
                                    }
                                }
                            },
                            onState = { state -> coroutineScope.launch { realtimeState = state } }
                        )
                    }
                } else null
                onDispose { connection?.close() }
            }
            ProductDetailScreen(
                productId = productId,
                remoteAuction = remoteDetail,
                remoteLoading = detailLoading,
                remoteError = detailError,
                onRetry = { detailRevision++ },
                realtimeStatus = when (realtimeState) {
                    RealtimeConnectionState.Connecting -> "실시간 연결 중"
                    RealtimeConnectionState.Connected -> "실시간 연결됨"
                    RealtimeConnectionState.Reconnecting -> "실시간 재연결 중"
                    RealtimeConnectionState.Disconnected, null -> null
                },
                realtimeNotice = realtimeNotice,
                isAuthenticated = signedIn == true,
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
                    if (signedIn == true) {
                        navController.navigate(
                            Screen.ProductReport.createRoute(
                                backStackEntry.arguments?.getString("productId").orEmpty()
                            )
                        )
                    } else navController.navigate(Screen.Login.route)
                },
                onTransactionClick = {
                    if (signedIn == true) navController.navigate(Screen.Transaction.createRoute("buyer"))
                    else navController.navigate(Screen.Login.route)
                },
                onLoginRequired = { navController.navigate(Screen.Login.route) },
                paidBidAmount = paidBidAmount,
                depositPaid = productId in depositPaidProductIds,
                onPaymentConsumed = { backStackEntry.savedStateHandle["paidBidAmount"] = 0 },
                onDepositPayment = { submission ->
                    if (signedIn != true) {
                        navController.navigate(Screen.Login.route)
                        return@ProductDetailScreen
                    }
                    backStackEntry.savedStateHandle["pendingPaymentMethodId"] = submission.paymentMethodId
                    backStackEntry.savedStateHandle["pendingAddressId"] = submission.addressId
                    if (productId in depositPaidProductIds) {
                        backStackEntry.savedStateHandle["paidBidAmount"] = submission.amount
                    } else {
                        navController.navigate(Screen.BidDepositPayment.createRoute(productId, submission.amount))
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
                    coroutineScope.launch(Dispatchers.IO) { auth.repository.logout(auth.deviceId) }
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
                    coroutineScope.launch(Dispatchers.IO) { auth.repository.logout(auth.deviceId) }
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
                        set("paidBidAmount", bidAmount)
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}
