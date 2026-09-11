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
import com.ssafy.dib.feature.auction.RealtimeBidFeedback
import com.ssafy.dib.feature.auction.BidDepositPaymentScreen
import com.ssafy.dib.feature.auction.ProductImageViewerScreen
import com.ssafy.dib.feature.auction.ProductReportScreen
import com.ssafy.dib.feature.auction.SellerProfileScreen
import com.ssafy.dib.feature.auction.SellerListingsScreen
import com.ssafy.dib.feature.auction.SellerReportScreen
import com.ssafy.dib.feature.auction.SellerReviewsScreen
import com.ssafy.dib.feature.auth.LoginScreen
import com.ssafy.dib.feature.auth.SignupScreen
import com.ssafy.dib.feature.auth.SignupUiState
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
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.data.AuthDependencies
import com.ssafy.dib.domain.auth.SignUpCommand
import com.ssafy.dib.domain.order.OrderRole
import com.ssafy.dib.domain.order.OrderSummary
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import com.ssafy.dib.data.remote.socket.AuctionRealtimeConnection
import com.ssafy.dib.data.remote.socket.SocketEventTypes
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
    var signupState by remember { mutableStateOf(SignupUiState()) }
    var phoneVerificationToken by remember { mutableStateOf<String?>(null) }
    var remoteAuctions by remember { mutableStateOf<List<HomeAuction>?>(null) }
    var auctionsLoading by remember { mutableStateOf(false) }
    var auctionsError by remember { mutableStateOf<String?>(null) }
    var auctionsRevision by remember { mutableStateOf(0) }
    var purchaseOrders by remember { mutableStateOf<List<OrderSummary>?>(null) }
    var saleOrders by remember { mutableStateOf<List<OrderSummary>?>(null) }
    var ordersLoading by remember { mutableStateOf(false) }
    var ordersError by remember { mutableStateOf<String?>(null) }
    var ordersRevision by remember { mutableStateOf(0) }
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

    LaunchedEffect(ordersRevision, signedIn) {
        if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
        ordersLoading = true
        ordersError = null
        val (buyerResult, sellerResult) = withContext(Dispatchers.IO) {
            auth.orderRepository.getOrders(OrderRole.BUYER) to
                auth.orderRepository.getOrders(OrderRole.SELLER)
        }
        when (buyerResult) {
            is ApiResult.Success -> purchaseOrders = buyerResult.value
            is ApiResult.Failure -> ordersError = buyerResult.error.message.ifBlank { "구매 내역을 불러오지 못했어요." }
        }
        when (sellerResult) {
            is ApiResult.Success -> saleOrders = sellerResult.value
            is ApiResult.Failure -> if (ordersError == null) {
                ordersError = sellerResult.error.message.ifBlank { "판매 내역을 불러오지 못했어요." }
            }
        }
        if (
            (buyerResult is ApiResult.Failure && buyerResult.error.requiresLogin) ||
            (sellerResult is ApiResult.Failure && sellerResult.error.requiresLogin)
        ) signedIn = false
        ordersLoading = false
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
                onEmailSignup = { navController.navigate(Screen.SignUp.route) },
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
                onSignUp = { navController.navigate(Screen.SignUp.route) },
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
        composable(Screen.SignUp.route) {
            SignupScreen(
                state = signupState,
                onBack = navController::navigateUp,
                onRequestPhoneVerification = { phoneNumber ->
                    signupState = signupState.copy(
                        phoneRequestLoading = true,
                        phoneError = null,
                        phoneVerified = false,
                        requestedPhone = null,
                        verificationRequestKey = null
                    )
                    phoneVerificationToken = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.repository.requestSignUpPhoneVerification(phoneNumber)
                        }) {
                            is ApiResult.Success -> signupState = signupState.copy(
                                phoneRequestLoading = false,
                                verificationRequestKey = result.value.verificationId,
                                requestedPhone = phoneNumber,
                                retryAfterSeconds = result.value.retryAfterSeconds,
                                phoneError = null
                            )
                            is ApiResult.Failure -> signupState = signupState.copy(
                                phoneRequestLoading = false,
                                phoneError = signupErrorMessage(result.error)
                            )
                        }
                    }
                },
                onConfirmPhoneVerification = { code ->
                    val verificationId = signupState.verificationRequestKey ?: return@SignupScreen
                    signupState = signupState.copy(phoneConfirmationLoading = true, phoneError = null)
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.repository.confirmPhoneVerification(verificationId, code)
                        }) {
                            is ApiResult.Success -> {
                                phoneVerificationToken = result.value.verificationToken
                                signupState = signupState.copy(
                                    phoneConfirmationLoading = false,
                                    phoneVerified = true,
                                    phoneError = null
                                )
                            }
                            is ApiResult.Failure -> signupState = signupState.copy(
                                phoneConfirmationLoading = false,
                                phoneVerified = false,
                                phoneError = signupErrorMessage(result.error)
                            )
                        }
                    }
                },
                onCheckEmail = { email ->
                    signupState = signupState.copy(
                        emailCheckLoading = true,
                        checkedEmail = null,
                        emailAvailable = null,
                        emailError = null
                    )
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.repository.checkEmailAvailability(email)
                        }) {
                            is ApiResult.Success -> signupState = signupState.copy(
                                emailCheckLoading = false,
                                checkedEmail = email,
                                emailAvailable = result.value,
                                emailError = null
                            )
                            is ApiResult.Failure -> signupState = signupState.copy(
                                emailCheckLoading = false,
                                checkedEmail = email,
                                emailAvailable = false,
                                emailError = signupErrorMessage(result.error)
                            )
                        }
                    }
                },
                onSignUp = { form ->
                    val token = phoneVerificationToken ?: return@SignupScreen
                    if (signupState.requestedPhone != form.phoneNumber) return@SignupScreen
                    signupState = signupState.copy(signupLoading = true, signupError = null)
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.repository.signUp(
                                SignUpCommand(
                                    email = form.email,
                                    password = form.password,
                                    name = form.name,
                                    nickname = form.nickname,
                                    gender = form.gender,
                                    birthDate = form.birthDate,
                                    phoneNumber = form.phoneNumber,
                                    phoneVerificationToken = token
                                )
                            )
                        }) {
                            is ApiResult.Success -> {
                                signupState = SignupUiState()
                                phoneVerificationToken = null
                                signedIn = true
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                            is ApiResult.Failure -> signupState = signupState.copy(
                                signupLoading = false,
                                signupError = signupErrorMessage(result.error)
                            )
                        }
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
            var realtimeConnection by remember(productId) { mutableStateOf<AuctionRealtimeConnection?>(null) }
            var pendingBidCommandId by remember(productId) { mutableStateOf<String?>(null) }
            var realtimeBidFeedback by remember(productId) { mutableStateOf<RealtimeBidFeedback?>(null) }
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
                        realtimeConnection = realtime
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
                                    val isBidResult = update.eventType in setOf(
                                        SocketEventTypes.BID_ACCEPTED,
                                        SocketEventTypes.BID_REJECTED
                                    )
                                    if (isBidResult && update.commandId == pendingBidCommandId) {
                                        realtimeBidFeedback = RealtimeBidFeedback(
                                            accepted = update.eventType == SocketEventTypes.BID_ACCEPTED,
                                            message = update.message.orEmpty(),
                                            currentPrice = update.currentPrice,
                                            minAllowedAmount = update.minAllowedAmount,
                                            errorCode = update.errorCode,
                                            eventKey = "${update.eventType}:${update.commandId}:${update.occurredAt.orEmpty()}"
                                        )
                                        pendingBidCommandId = null
                                    } else {
                                        update.message?.let { message ->
                                            realtimeNotice = "$message|${update.occurredAt.orEmpty()}"
                                        }
                                    }
                                }
                            },
                            onState = { state -> coroutineScope.launch { realtimeState = state } }
                        )
                    }
                } else null
                onDispose {
                    realtimeConnection = null
                    pendingBidCommandId = null
                    connection?.close()
                }
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
                realtimeBiddingEnabled = auth.networkConfig.isWebSocketConfigured,
                realtimeBidFeedback = realtimeBidFeedback,
                onRealtimeBid = { amount ->
                    realtimeConnection?.placeBid(amount)?.let { commandId ->
                        pendingBidCommandId = commandId
                        true
                    } ?: false
                },
                onDepositInvalid = {
                    val updatedPaidProducts = depositPaidProductIds - productId
                    depositPaidProductIds = updatedPaidProducts
                    session.edit().putStringSet("paid_deposits", updatedPaidProducts).apply()
                },
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
                onTransactionClick = { role -> navController.navigate(Screen.Transaction.createRoute(role)) },
                remotePurchaseOrders = purchaseOrders,
                remoteSaleOrders = saleOrders,
                remoteLoading = ordersLoading,
                remoteError = ordersError,
                onRetry = { ordersRevision++ }
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

private fun signupErrorMessage(error: ApiFailure): String = when (error.code) {
    ApiErrorCodes.CLIENT_NOT_CONFIGURED -> "개발 서버 주소가 설정되지 않았어요. 연결 설정을 확인해주세요."
    "INVALID_PHONE" -> "휴대폰 번호 형식을 확인해주세요."
    "RATE_LIMITED" -> "요청이 너무 많아요. 잠시 후 다시 시도해주세요."
    "INVALID_CODE" -> "인증번호가 올바르지 않아요."
    "VERIFICATION_EXPIRED" -> "인증 시간이 만료됐어요. 인증번호를 다시 요청해주세요."
    "ATTEMPTS_EXCEEDED" -> "인증 시도 횟수를 초과했어요. 인증번호를 다시 요청해주세요."
    "INVALID_EMAIL" -> "이메일 형식을 확인해주세요."
    "EMAIL_DUPLICATED" -> "이미 가입된 이메일이에요."
    "PHONE_DUPLICATED" -> "이미 가입된 휴대폰 번호예요."
    "INVALID_PASSWORD" -> "비밀번호 조건을 확인해주세요."
    "INVALID_VERIFICATION" -> "휴대폰 인증이 만료됐어요. 다시 인증해주세요."
    else -> error.message.ifBlank { "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요." }
}
