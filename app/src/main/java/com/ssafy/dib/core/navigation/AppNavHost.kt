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
import com.ssafy.dib.feature.auction.AuctionRegisterScreen
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
import com.ssafy.dib.feature.live.LiveManagementScreen
import com.ssafy.dib.feature.home.HomeScreen
import com.ssafy.dib.feature.home.HomeAuction
import com.ssafy.dib.feature.home.toHomeAuction
import com.ssafy.dib.feature.home.AuctionSearchScreen
import com.ssafy.dib.feature.home.AuctionSearchFilters
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
import com.ssafy.dib.feature.main.ProductEditScreen
import com.ssafy.dib.feature.main.ProductRegistrationForm
import com.ssafy.dib.feature.main.RegisteredProductsScreen
import com.ssafy.dib.feature.main.ReportHistoryScreen
import com.ssafy.dib.feature.main.SettlementAccountsScreen
import com.ssafy.dib.feature.main.SettlementHistoryScreen
import com.ssafy.dib.feature.main.SettlementDetailScreen
import com.ssafy.dib.feature.main.TransactionScreen
import com.ssafy.dib.feature.main.OrderChatScreen
import com.ssafy.dib.feature.main.WithdrawalScreen
import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.data.AuthDependencies
import com.ssafy.dib.domain.auth.SignUpCommand
import com.ssafy.dib.domain.order.OrderRole
import com.ssafy.dib.domain.order.OrderSummary
import com.ssafy.dib.domain.support.InquiryDetail
import com.ssafy.dib.domain.support.InquirySummary
import com.ssafy.dib.domain.report.ReportSummary
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.domain.product.ProductImageUpload
import com.ssafy.dib.domain.product.ProductRegistration
import com.ssafy.dib.domain.product.ProductRegistrationResult
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
    var memberProfile by remember { mutableStateOf<com.ssafy.dib.domain.member.MemberProfile?>(null) }
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
    var bidHistory by remember { mutableStateOf<List<com.ssafy.dib.domain.auction.BidHistoryItem>?>(null) }
    var bidHistoryLoading by remember { mutableStateOf(false) }
    var bidHistoryError by remember { mutableStateOf<String?>(null) }
    var bidHistoryRevision by remember { mutableStateOf(0) }
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

    fun updateBookmark(auctionId: String, bookmarked: Boolean) {
        if (signedIn != true) return
        remoteAuctions = remoteAuctions?.map { auction ->
            if (auction.id == auctionId) auction.copy(bookmarked = bookmarked) else auction
        }
        coroutineScope.launch {
            when (val result = withContext(Dispatchers.IO) {
                auth.auctionRepository.setBookmark(auctionId, bookmarked, java.util.UUID.randomUUID().toString())
            }) {
                is ApiResult.Success -> remoteAuctions = remoteAuctions?.map { auction ->
                    if (auction.id == auctionId) auction.copy(bookmarked = result.value) else auction
                }
                is ApiResult.Failure -> {
                    remoteAuctions = remoteAuctions?.map { auction ->
                        if (auction.id == auctionId) auction.copy(bookmarked = !bookmarked) else auction
                    }
                    auctionsError = result.error.message.ifBlank { "찜 상태를 변경하지 못했어요." }
                    if (result.error.requiresLogin) signedIn = false
                }
            }
        }
    }

    LaunchedEffect(signedIn) {
        if (signedIn != true) memberProfile = null
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

    LaunchedEffect(bidHistoryRevision, signedIn) {
        if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
        bidHistoryLoading = true
        bidHistoryError = null
        when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getMyBids() }) {
            is ApiResult.Success -> bidHistory = result.value
            is ApiResult.Failure -> {
                bidHistoryError = result.error.message.ifBlank { "입찰 내역을 불러오지 못했어요." }
                if (result.error.requiresLogin) signedIn = false
            }
        }
        bidHistoryLoading = false
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
                onBookmarkChange = ::updateBookmark,
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
            var categoryList by remember { mutableStateOf<List<ProductCategory>?>(null) }
            var categoryAuctions by remember { mutableStateOf<List<HomeAuction>?>(null) }
            var categoryLoading by remember { mutableStateOf(false) }
            var categoryError by remember { mutableStateOf<String?>(null) }
            var selectedCategoryId by remember { mutableStateOf<String?>(null) }

            fun loadCategory(categoryId: String) {
                selectedCategoryId = categoryId
                if (!auth.networkConfig.isRestConfigured) {
                    categoryAuctions = null
                    categoryError = null
                    return
                }
                categoryLoading = true
                categoryError = null
                coroutineScope.launch {
                    when (val result = withContext(Dispatchers.IO) {
                        auth.auctionRepository.getActiveGeneralAuctions(categoryId = categoryId)
                    }) {
                        is ApiResult.Success -> categoryAuctions = result.value.map { it.toHomeAuction() }
                        is ApiResult.Failure -> categoryError = result.error.message.ifBlank { "경매 목록을 불러오지 못했어요." }
                    }
                    categoryLoading = false
                }
            }

            LaunchedEffect(signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                when (val result = withContext(Dispatchers.IO) { auth.productRepository.getCategories() }) {
                    is ApiResult.Success -> categoryList = result.value
                    is ApiResult.Failure -> if (result.error.requiresLogin) signedIn = false
                }
            }
            CategoryScreen(
                onBack = navController::navigateUp,
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onNotificationsClick = {
                    if (signedIn == true) navController.navigate(Screen.Notifications.route)
                    else navController.navigate(Screen.Login.route)
                },
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                onTabSelected = ::navigateMain,
                remoteCategories = categoryList,
                remoteAuctions = categoryAuctions,
                isLoading = categoryLoading,
                errorMessage = categoryError,
                onCategorySelected = ::loadCategory,
                onRetry = { selectedCategoryId?.let(::loadCategory) }
            )
        }
        composable(Screen.Search.route) {
            var searchCategories by remember { mutableStateOf<List<ProductCategory>?>(null) }
            var searchAuctions by remember { mutableStateOf<List<HomeAuction>?>(null) }
            var searchLoading by remember { mutableStateOf(false) }
            var searchError by remember { mutableStateOf<String?>(null) }
            var lastSearchFilters by remember { mutableStateOf<AuctionSearchFilters?>(null) }

            fun search(filters: AuctionSearchFilters) {
                lastSearchFilters = filters
                if (!auth.networkConfig.isRestConfigured) {
                    searchAuctions = null
                    searchError = null
                    return
                }
                searchLoading = true
                searchError = null
                coroutineScope.launch {
                    when (val result = withContext(Dispatchers.IO) {
                        auth.auctionRepository.getGeneralAuctions(
                            size = 100,
                            categoryId = filters.categoryId,
                            status = filters.status,
                            minPrice = filters.minPrice,
                            maxPrice = filters.maxPrice
                        )
                    }) {
                        is ApiResult.Success -> searchAuctions = result.value.map { it.toHomeAuction() }
                        is ApiResult.Failure -> {
                            searchError = result.error.message.ifBlank { "검색 결과를 불러오지 못했어요." }
                            if (result.error.requiresLogin) signedIn = false
                        }
                    }
                    searchLoading = false
                }
            }

            LaunchedEffect(signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                when (val result = withContext(Dispatchers.IO) { auth.productRepository.getCategories() }) {
                    is ApiResult.Success -> searchCategories = result.value
                    is ApiResult.Failure -> if (result.error.requiresLogin) signedIn = false
                }
            }
            AuctionSearchScreen(
                onBack = navController::navigateUp,
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                onTabSelected = ::navigateMain,
                remoteCategories = searchCategories,
                remoteAuctions = searchAuctions,
                isLoading = searchLoading,
                errorMessage = searchError,
                onSearch = ::search,
                onRetry = { lastSearchFilters?.let(::search) }
            )
        }
        composable(Screen.Notifications.route) {
            NotificationCenterScreen(onBack = navController::navigateUp, onTabSelected = ::navigateMain)
        }
        composable(Screen.Feed.route) { backStackEntry ->
            val paidBidAmount by backStackEntry.savedStateHandle
                .getStateFlow("paidBidAmount", 0).collectAsState()
            var liveFeedItems by remember { mutableStateOf<List<com.ssafy.dib.domain.live.LiveFeedItem>?>(null) }
            var liveFeedLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var liveFeedError by remember { mutableStateOf<String?>(null) }
            var liveFeedRevision by remember { mutableStateOf(0) }
            var activeLiveBroadcastId by remember { mutableStateOf<String?>(null) }
            var liveAuctionLists by remember { mutableStateOf<Map<String, List<com.ssafy.dib.domain.auction.AuctionSummary>>>(emptyMap()) }
            var liveDetailLoading by remember { mutableStateOf(false) }
            var liveDetailError by remember { mutableStateOf<String?>(null) }
            var liveComments by remember { mutableStateOf<List<com.ssafy.dib.domain.live.LiveChatMessage>>(emptyList()) }
            var liveChatError by remember { mutableStateOf<String?>(null) }
            var liveChatState by remember { mutableStateOf<RealtimeConnectionState?>(null) }
            var liveChatConnection by remember { mutableStateOf<com.ssafy.dib.data.remote.socket.LiveChatConnection?>(null) }
            var pendingLiveBidCommandId by remember { mutableStateOf<String?>(null) }
            var liveBidFeedback by remember { mutableStateOf<RealtimeBidFeedback?>(null) }
            var liveReportSubmitting by remember { mutableStateOf(false) }
            var liveReportError by remember { mutableStateOf<String?>(null) }
            var liveReportCompleted by remember { mutableStateOf(false) }
            LaunchedEffect(liveFeedRevision, signedIn) {
                if (!auth.networkConfig.isRestConfigured) return@LaunchedEffect
                liveFeedLoading = true
                liveFeedError = null
                when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getFeed() }) {
                    is ApiResult.Success -> liveFeedItems = result.value
                    is ApiResult.Failure -> {
                        liveFeedError = result.error.message.ifBlank { "Live 피드를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                liveFeedLoading = false
            }
            LaunchedEffect(activeLiveBroadcastId, signedIn) {
                val liveId = activeLiveBroadcastId ?: return@LaunchedEffect
                if (signedIn != true || !auth.networkConfig.isRestConfigured) {
                    liveComments = emptyList()
                    return@LaunchedEffect
                }
                liveChatError = null
                when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getMessages(liveId) }) {
                    is ApiResult.Success -> liveComments = result.value
                    is ApiResult.Failure -> {
                        liveChatError = result.error.message.ifBlank { "Live 댓글을 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
            }
            LaunchedEffect(activeLiveBroadcastId, liveFeedRevision) {
                val liveId = activeLiveBroadcastId ?: return@LaunchedEffect
                if (!auth.networkConfig.isRestConfigured) return@LaunchedEffect
                liveDetailLoading = true
                liveDetailError = null
                when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getDetail(liveId) }) {
                    is ApiResult.Success -> {
                        val detail = result.value
                        liveAuctionLists = liveAuctionLists + (liveId to detail.auctions)
                        liveFeedItems = liveFeedItems?.map { item ->
                            if (item.liveBroadcastId == liveId) item.copy(
                                title = detail.title,
                                description = detail.description,
                                streamUrl = detail.streamUrl,
                                viewCount = detail.viewCount,
                                currentAuction = detail.currentAuction
                            ) else item
                        }
                    }
                    is ApiResult.Failure -> {
                        liveDetailError = result.error.message.ifBlank { "Live 상품 목록을 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                liveDetailLoading = false
            }
            DisposableEffect(activeLiveBroadcastId, signedIn, auth.networkConfig.isWebSocketConfigured) {
                val liveId = activeLiveBroadcastId
                val connection = if (liveId != null && auth.networkConfig.isWebSocketConfigured) {
                    auth.createLiveChatConnection().also { created ->
                        liveChatConnection = created
                        created.start(
                            liveBroadcastId = liveId,
                            activeAuctionId = liveFeedItems?.firstOrNull { it.liveBroadcastId == liveId }?.currentAuction?.auctionId,
                            onMessage = { message -> coroutineScope.launch {
                                liveComments = (liveComments + message).distinctBy { it.liveChattingId }
                            } },
                            onUpdate = { update -> coroutineScope.launch {
                                val targetLiveId = update.liveBroadcastId ?: liveId
                                liveFeedItems = liveFeedItems?.map { item ->
                                    if (item.liveBroadcastId != targetLiveId) return@map item
                                    val current = item.currentAuction
                                    val updatedAuction = when {
                                        update.eventType == SocketEventTypes.LIVE_AUCTION_OPENED && update.auctionId != null ->
                                            com.ssafy.dib.domain.auction.AuctionSummary(
                                                auctionId = update.auctionId,
                                                sellerMemberId = item.memberId,
                                                productId = update.productId.orEmpty(),
                                                title = update.title ?: "Live 경매 상품",
                                                categoryName = "",
                                                currentPrice = update.currentPrice ?: update.startPrice ?: 0,
                                                startPrice = update.startPrice ?: update.currentPrice ?: 0,
                                                bidCount = update.bidCount ?: 0,
                                                remainingSeconds = update.remainingSeconds ?: 0,
                                                status = update.status ?: "ACTIVE",
                                                bookmarked = false,
                                                imageUrls = listOfNotNull(update.thumbnailUrl)
                                            )
                                        current != null && (update.auctionId == null || update.auctionId == current.auctionId) -> current.copy(
                                            currentPrice = update.currentPrice ?: current.currentPrice,
                                            bidCount = update.bidCount ?: current.bidCount,
                                            remainingSeconds = update.remainingSeconds ?: current.remainingSeconds,
                                            status = update.status ?: current.status
                                        )
                                        else -> current
                                    }
                                    item.copy(
                                        viewCount = update.viewerCount ?: item.viewCount,
                                        currentAuction = updatedAuction
                                    )
                                }
                                update.auctionId?.let { auctionId ->
                                    liveAuctionLists[targetLiveId]?.let { auctions ->
                                        liveAuctionLists = liveAuctionLists + (targetLiveId to auctions.map { auction ->
                                            if (auction.auctionId == auctionId) auction.copy(
                                                currentPrice = update.currentPrice ?: auction.currentPrice,
                                                bidCount = update.bidCount ?: auction.bidCount,
                                                remainingSeconds = update.remainingSeconds ?: auction.remainingSeconds,
                                                status = update.status ?: auction.status
                                            ) else auction
                                        })
                                    }
                                }
                                if (update.bidAccepted != null && update.commandId == pendingLiveBidCommandId) {
                                    liveBidFeedback = RealtimeBidFeedback(
                                        accepted = update.bidAccepted,
                                        message = update.message.orEmpty(),
                                        currentPrice = update.currentPrice,
                                        minAllowedAmount = update.minAllowedAmount,
                                        errorCode = update.errorCode,
                                        eventKey = "${update.eventType}:${update.commandId}:${update.occurredAt.orEmpty()}"
                                    )
                                    pendingLiveBidCommandId = null
                                }
                                update.message?.takeIf { update.bidAccepted == null }?.let { liveChatError = it }
                            } },
                            onError = { message -> coroutineScope.launch { liveChatError = message } },
                            onState = { state -> coroutineScope.launch { liveChatState = state } }
                        )
                    }
                } else null
                onDispose {
                    liveChatConnection = null
                    pendingLiveBidCommandId = null
                    connection?.close()
                }
            }
            LiveFeedScreen(
                remoteItems = liveFeedItems,
                isLoading = liveFeedLoading,
                errorMessage = liveFeedError,
                onRetry = { liveFeedRevision++ },
                activeLiveBroadcastId = activeLiveBroadcastId,
                liveComments = liveComments,
                liveAuctionsByBroadcast = liveAuctionLists,
                productListLoading = liveDetailLoading,
                productListError = liveDetailError,
                reportSubmitting = liveReportSubmitting,
                reportError = liveReportError,
                reportCompleted = liveReportCompleted,
                chatError = liveChatError,
                chatConnectionState = liveChatState,
                onLiveVisible = { liveId ->
                    if (activeLiveBroadcastId != liveId) {
                        activeLiveBroadcastId = liveId
                        liveComments = emptyList()
                        liveChatError = null
                    }
                },
                onSendComment = { content -> liveChatConnection?.send(content) == true },
                isAuthenticated = signedIn == true,
                paidBidAmount = paidBidAmount,
                depositPaidAuctionIds = depositPaidProductIds,
                realtimeBidFeedback = liveBidFeedback,
                onRealtimeBid = { auctionId, amount ->
                    liveChatConnection?.placeBid(auctionId, amount)?.let { commandId ->
                        pendingLiveBidCommandId = commandId
                        true
                    } ?: false
                },
                onPaymentConsumed = { backStackEntry.savedStateHandle["paidBidAmount"] = 0 },
                onClose = { navController.navigateUp() },
                onProductClick = { auctionId -> navController.navigate(Screen.ProductDetail.createRoute(auctionId)) },
                onLoginRequired = { navController.navigate(Screen.Login.route) },
                onReportParticipant = { liveBroadcastId, memberId, content ->
                    if (signedIn != true) {
                        navController.navigate(Screen.Login.route)
                    } else {
                        liveReportSubmitting = true
                        liveReportError = null
                        liveReportCompleted = false
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.reportRepository.reportLiveParticipant(liveBroadcastId, memberId, content)
                            }) {
                                is ApiResult.Success -> liveReportCompleted = true
                                is ApiResult.Failure -> {
                                    liveReportError = result.error.message.ifBlank { "신고를 접수하지 못했어요." }
                                    if (result.error.requiresLogin) signedIn = false
                                }
                            }
                            liveReportSubmitting = false
                        }
                    }
                },
                onDismissReport = {
                    liveReportError = null
                    liveReportCompleted = false
                },
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
            var remoteProduct by remember(productId) { mutableStateOf<com.ssafy.dib.domain.product.ProductDetail?>(null) }
            var detailLoading by remember(productId) { mutableStateOf(false) }
            var detailError by remember(productId) { mutableStateOf<String?>(null) }
            var detailRevision by remember(productId) { mutableStateOf(0) }
            var realtimeState by remember(productId) { mutableStateOf<RealtimeConnectionState?>(null) }
            var realtimeNotice by remember(productId) { mutableStateOf<String?>(null) }
            var realtimeConnection by remember(productId) { mutableStateOf<AuctionRealtimeConnection?>(null) }
            var pendingBidCommandId by remember(productId) { mutableStateOf<String?>(null) }
            var realtimeBidFeedback by remember(productId) { mutableStateOf<RealtimeBidFeedback?>(null) }
            var bookmarkLoading by remember(productId) { mutableStateOf(false) }
            var bookmarkError by remember(productId) { mutableStateOf<String?>(null) }
            LaunchedEffect(productId, detailRevision, signedIn) {
                if (!auth.networkConfig.isRestConfigured) return@LaunchedEffect
                detailLoading = true
                detailError = null
                when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getAuction(productId) }) {
                    is ApiResult.Success -> {
                        remoteDetail = result.value.toHomeAuction()
                        if (signedIn == true && result.value.productId.isNotBlank()) {
                            when (val productResult = withContext(Dispatchers.IO) { auth.productRepository.getProduct(result.value.productId) }) {
                                is ApiResult.Success -> remoteProduct = productResult.value
                                is ApiResult.Failure -> if (productResult.error.requiresLogin) signedIn = false
                            }
                        }
                    }
                    is ApiResult.Failure -> {
                        detailError = result.error.message.ifBlank { "경매 상세를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                detailLoading = false
            }
            LaunchedEffect(productId, signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                when (val result = withContext(Dispatchers.IO) { auth.bidDepositRepository.getMine(productId) }) {
                    is ApiResult.Success -> {
                        val updated = if (result.value.status == "PAID") {
                            depositPaidProductIds + productId
                        } else {
                            depositPaidProductIds - productId
                        }
                        depositPaidProductIds = updated
                        session.edit().putStringSet("paid_deposits", updated).apply()
                    }
                    is ApiResult.Failure -> if (result.error.requiresLogin) signedIn = false
                }
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
                productDetail = remoteProduct,
                remoteLoading = detailLoading,
                remoteError = detailError,
                onRetry = { detailRevision++ },
                bookmarkLoading = bookmarkLoading,
                bookmarkError = bookmarkError,
                onBookmarkChange = { selected ->
                    bookmarkLoading = true
                    bookmarkError = null
                    remoteDetail = remoteDetail?.copy(bookmarked = selected)
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.auctionRepository.setBookmark(productId, selected, java.util.UUID.randomUUID().toString())
                        }) {
                            is ApiResult.Success -> {
                                remoteDetail = remoteDetail?.copy(bookmarked = result.value)
                                remoteAuctions = remoteAuctions?.map { auction ->
                                    if (auction.id == productId) auction.copy(bookmarked = result.value) else auction
                                }
                            }
                            is ApiResult.Failure -> {
                                remoteDetail = remoteDetail?.copy(bookmarked = !selected)
                                bookmarkError = result.error.message.ifBlank { "찜 상태를 변경하지 못했어요." }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        bookmarkLoading = false
                    }
                },
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
                    backStackEntry.savedStateHandle["productImageUrls"] = ArrayList(remoteProduct?.imageUrls?.takeIf { it.isNotEmpty() } ?: remoteDetail?.imageUrls.orEmpty())
                    navController.navigate(
                        Screen.ProductImages.createRoute(
                            backStackEntry.arguments?.getString("productId").orEmpty(),
                            page
                        )
                    )
                },
                onSellerClick = { sellerMemberId ->
                    navController.navigate(Screen.SellerProfile.createRoute(sellerMemberId.ifBlank { "seller01" }))
                },
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
            var productCategories by remember {
                mutableStateOf(
                    if (auth.networkConfig.isRestConfigured) emptyList() else listOf(
                        ProductCategory("1", "디지털"),
                        ProductCategory("2", "패션"),
                        ProductCategory("3", "라이프")
                    )
                )
            }
            var categoriesLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var categoriesError by remember { mutableStateOf<String?>(null) }
            var categoriesRevision by remember { mutableStateOf(0) }
            var productSubmitLoading by remember { mutableStateOf(false) }
            var productSubmitError by remember { mutableStateOf<String?>(null) }
            var productResult by remember { mutableStateOf<ProductRegistrationResult?>(null) }

            LaunchedEffect(categoriesRevision) {
                if (!auth.networkConfig.isRestConfigured) return@LaunchedEffect
                categoriesLoading = true
                categoriesError = null
                when (val result = withContext(Dispatchers.IO) { auth.productRepository.getCategories() }) {
                    is ApiResult.Success -> productCategories = result.value
                    is ApiResult.Failure -> {
                        categoriesError = result.error.message.ifBlank { "카테고리를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                categoriesLoading = false
            }

            ProductRegisterScreen(
                categories = productCategories,
                categoriesLoading = categoriesLoading,
                categoriesError = categoriesError,
                submitLoading = productSubmitLoading,
                submitError = productSubmitError,
                result = productResult,
                onRetryCategories = { categoriesRevision++ },
                onSubmit = { form: ProductRegistrationForm ->
                    productSubmitLoading = true
                    productSubmitError = null
                    coroutineScope.launch {
                        val uploads = withContext(Dispatchers.IO) {
                            runCatching {
                                val types = listOf("LEFT", "RIGHT", "TOP", "BOTTOM", "BACK")
                                form.imageUris.mapIndexed { index, uri ->
                                    ProductImageUpload(
                                        fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "product-$index.jpg",
                                        mediaType = context.contentResolver.getType(uri) ?: "image/jpeg",
                                        bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                            ?: error("선택한 사진을 읽을 수 없습니다."),
                                        type = if (index == 0) "FRONT" else types[(index - 1) % types.size]
                                    )
                                }
                            }
                        }
                        uploads.fold(
                            onSuccess = { images ->
                                when (val result = withContext(Dispatchers.IO) {
                                    auth.productRepository.registerProduct(
                                        ProductRegistration(
                                            title = form.title,
                                            description = form.description,
                                            categoryId = form.categoryId,
                                            condition = form.condition,
                                            images = images
                                        )
                                    )
                                }) {
                                    is ApiResult.Success -> productResult = result.value
                                    is ApiResult.Failure -> productSubmitError = productSubmissionMessage(result.error)
                                }
                            },
                            onFailure = { productSubmitError = it.message ?: "선택한 사진을 읽지 못했어요." }
                        )
                        productSubmitLoading = false
                    }
                },
                onComplete = { navController.navigateUp() },
                onBack = navController::navigateUp
            )
        }
        composable(Screen.Trades.route) {
            MyTradesScreen(
                onTabSelected = ::navigateMain,
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                onTransactionClick = { role, orderId ->
                    navController.navigate(Screen.Transaction.createRoute(role, orderId))
                },
                remotePurchaseOrders = purchaseOrders,
                remoteSaleOrders = saleOrders,
                remoteBids = bidHistory,
                remoteLoading = ordersLoading,
                remoteError = ordersError,
                bidsLoading = bidHistoryLoading,
                bidsError = bidHistoryError,
                onRetry = { ordersRevision++ },
                onBidsRetry = { bidHistoryRevision++ }
            )
        }
        composable(
            route = Screen.Transaction.route,
            arguments = listOf(
                navArgument("role") { type = NavType.StringType },
                navArgument("orderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role").orEmpty()
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            var remoteOrder by remember(orderId) { mutableStateOf<OrderSummary?>(null) }
            var orderDetailLoading by remember(orderId) { mutableStateOf(orderId != "sample") }
            var orderDetailError by remember(orderId) { mutableStateOf<String?>(null) }
            var orderDetailRevision by remember(orderId) { mutableStateOf(0) }
            var confirmationLoading by remember(orderId) { mutableStateOf(false) }
            var confirmationError by remember(orderId) { mutableStateOf<String?>(null) }
            var paymentPreparation by remember(orderId) { mutableStateOf<com.ssafy.dib.domain.payment.PaymentPreparation?>(null) }
            var paymentLoading by remember(orderId) { mutableStateOf(false) }
            var paymentError by remember(orderId) { mutableStateOf<String?>(null) }
            var paymentPrepareKey by remember(orderId) { mutableStateOf(java.util.UUID.randomUUID().toString()) }
            var shipment by remember(orderId) { mutableStateOf<com.ssafy.dib.domain.order.OrderShipment?>(null) }
            var shipmentLoading by remember(orderId) { mutableStateOf(false) }
            var shipmentError by remember(orderId) { mutableStateOf<String?>(null) }
            var shipmentKey by remember(orderId) { mutableStateOf(java.util.UUID.randomUUID().toString()) }

            LaunchedEffect(orderId, orderDetailRevision) {
                if (orderId == "sample") return@LaunchedEffect
                if (!auth.networkConfig.isRestConfigured) {
                    orderDetailLoading = false
                    orderDetailError = "개발 서버 주소가 설정되지 않았어요."
                    return@LaunchedEffect
                }
                orderDetailLoading = true
                orderDetailError = null
                when (val result = withContext(Dispatchers.IO) { auth.orderRepository.getOrder(orderId) }) {
                    is ApiResult.Success -> {
                        remoteOrder = result.value
                        if (result.value.status.uppercase() in setOf("SHIPPED", "DELIEVERED", "DELIVERED")) {
                            shipmentLoading = true
                            when (val shipmentResult = withContext(Dispatchers.IO) { auth.orderRepository.getShipment(orderId) }) {
                                is ApiResult.Success -> shipment = shipmentResult.value
                                is ApiResult.Failure -> shipmentError = shipmentResult.error.message.ifBlank { "배송 정보를 불러오지 못했어요." }
                            }
                            shipmentLoading = false
                        }
                    }
                    is ApiResult.Failure -> {
                        orderDetailError = result.error.message.ifBlank { "주문 상세를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                orderDetailLoading = false
            }

            TransactionScreen(
                role = role,
                remoteOrder = remoteOrder,
                isLoading = orderDetailLoading,
                errorMessage = orderDetailError,
                confirmationLoading = confirmationLoading,
                confirmationError = confirmationError,
                paymentPreparation = paymentPreparation,
                paymentLoading = paymentLoading,
                paymentError = paymentError,
                shipment = shipment,
                shipmentLoading = shipmentLoading,
                shipmentError = shipmentError,
                onPreparePayment = {
                    paymentLoading = true
                    paymentError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.paymentRepository.prepare(orderId, "CARD", paymentPrepareKey)
                        }) {
                            is ApiResult.Success -> paymentPreparation = result.value
                            is ApiResult.Failure -> {
                                paymentError = result.error.message.ifBlank { "결제를 준비하지 못했어요." }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        paymentLoading = false
                    }
                },
                onCheckPayment = {
                    paymentLoading = true
                    paymentError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.orderRepository.getOrder(orderId) }) {
                            is ApiResult.Success -> {
                                remoteOrder = result.value
                                if (result.value.status.uppercase() != "PENDING") {
                                    paymentPreparation = null
                                    ordersRevision++
                                }
                            }
                            is ApiResult.Failure -> paymentError = result.error.message.ifBlank { "결제 상태를 확인하지 못했어요." }
                        }
                        paymentLoading = false
                    }
                },
                onResetPayment = {
                    paymentPreparation = null
                    paymentError = null
                    paymentPrepareKey = java.util.UUID.randomUUID().toString()
                },
                onRegisterShipment = { trackingNumber ->
                    shipmentLoading = true
                    shipmentError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.orderRepository.registerShipment(orderId, trackingNumber, shipmentKey)
                        }) {
                            is ApiResult.Success -> {
                                shipment = result.value
                                remoteOrder = remoteOrder?.copy(status = result.value.status)
                                shipmentKey = java.util.UUID.randomUUID().toString()
                                ordersRevision++
                            }
                            is ApiResult.Failure -> {
                                shipmentError = when (result.error.code) {
                                    "PAYMENT_REQUIRED" -> "결제가 완료된 주문만 발송할 수 있어요."
                                    "INVALID_TRACKING" -> "송장번호를 다시 확인해주세요."
                                    else -> result.error.message.ifBlank { "배송 정보를 등록하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        shipmentLoading = false
                    }
                },
                onRefreshShipment = {
                    shipmentLoading = true
                    shipmentError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.orderRepository.getShipment(orderId) }) {
                            is ApiResult.Success -> {
                                shipment = result.value
                                remoteOrder = remoteOrder?.copy(status = result.value.status)
                            }
                            is ApiResult.Failure -> {
                                shipmentError = result.error.message.ifBlank { "배송 상태를 확인하지 못했어요." }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        shipmentLoading = false
                    }
                },
                onRetry = { orderDetailRevision++ },
                onConfirmPurchase = {
                    confirmationLoading = true
                    confirmationError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.orderRepository.confirmPurchase(orderId)
                        }) {
                            is ApiResult.Success -> {
                                remoteOrder = remoteOrder?.copy(status = result.value)
                                ordersRevision++
                            }
                            is ApiResult.Failure -> {
                                confirmationError = when (result.error.code) {
                                    "DELIVERY_NOT_COMPLETED" -> "배송 완료 후 구매를 확정할 수 있어요."
                                    "ALREADY_CONFIRMED" -> "이미 구매 확정된 주문이에요."
                                    else -> result.error.message.ifBlank { "구매를 확정하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        confirmationLoading = false
                    }
                },
                onOpenChat = { navController.navigate(Screen.OrderChat.createRoute(orderId)) },
                onBack = navController::navigateUp
            )
        }
        composable(
            route = Screen.OrderChat.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            var chatMessages by remember(orderId) { mutableStateOf<List<com.ssafy.dib.domain.order.OrderMessage>>(emptyList()) }
            var chatLoading by remember(orderId) { mutableStateOf(true) }
            var chatError by remember(orderId) { mutableStateOf<String?>(null) }
            var chatRevision by remember(orderId) { mutableStateOf(0) }
            var currentMemberId by remember(orderId) { mutableStateOf("") }
            var chatConnectionState by remember(orderId) { mutableStateOf<RealtimeConnectionState?>(null) }
            var chatConnection by remember(orderId) { mutableStateOf<com.ssafy.dib.data.remote.socket.OrderChatConnection?>(null) }

            LaunchedEffect(orderId, chatRevision) {
                if (!auth.networkConfig.isRestConfigured) {
                    chatLoading = false
                    chatError = "개발 서버 주소가 설정되지 않았어요."
                    return@LaunchedEffect
                }
                chatLoading = true
                chatError = null
                val messagesResult = withContext(Dispatchers.IO) { auth.orderRepository.getMessages(orderId) }
                val memberResult = withContext(Dispatchers.IO) { auth.memberRepository.getMe() }
                when (messagesResult) {
                    is ApiResult.Success -> chatMessages = messagesResult.value
                    is ApiResult.Failure -> chatError = messagesResult.error.message.ifBlank { "채팅 내역을 불러오지 못했어요." }
                }
                if (memberResult is ApiResult.Success) currentMemberId = memberResult.value.memberId
                if (
                    (messagesResult is ApiResult.Failure && messagesResult.error.requiresLogin) ||
                    (memberResult is ApiResult.Failure && memberResult.error.requiresLogin)
                ) signedIn = false
                chatLoading = false
            }

            DisposableEffect(orderId, auth.networkConfig.isWebSocketConfigured) {
                val connection = if (auth.networkConfig.isWebSocketConfigured) {
                    auth.createOrderChatConnection().also { created ->
                        chatConnection = created
                        created.start(
                            orderId = orderId,
                            lastChattingId = chatMessages.lastOrNull()?.chattingId,
                            onMessage = { message -> coroutineScope.launch {
                                chatMessages = (chatMessages + message).distinctBy { it.chattingId }
                            } },
                            onError = { message -> coroutineScope.launch { chatError = message } },
                            onState = { state -> coroutineScope.launch { chatConnectionState = state } }
                        )
                    }
                } else null
                onDispose {
                    chatConnection = null
                    connection?.close()
                }
            }

            OrderChatScreen(
                orderId = orderId,
                currentMemberId = currentMemberId,
                messages = chatMessages,
                isLoading = chatLoading,
                errorMessage = chatError,
                connectionState = chatConnectionState,
                onRetry = { chatRevision++ },
                onSend = { content -> chatConnection?.send(content) == true },
                onBack = navController::navigateUp
            )
        }
        composable(Screen.My.route) {
            var myProfileLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var myProfileError by remember { mutableStateOf<String?>(null) }
            var myProfileRevision by remember { mutableStateOf(0) }
            LaunchedEffect(myProfileRevision, signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                myProfileLoading = true
                myProfileError = null
                when (val result = withContext(Dispatchers.IO) { auth.memberRepository.getMe() }) {
                    is ApiResult.Success -> memberProfile = result.value
                    is ApiResult.Failure -> {
                        myProfileError = result.error.message.ifBlank { "내 정보를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                myProfileLoading = false
            }
            MyPageScreen(
                profile = memberProfile,
                profileLoading = myProfileLoading,
                profileError = myProfileError,
                onRetryProfile = { myProfileRevision++ },
                onTabSelected = ::navigateMain,
                onProfileEditClick = { navController.navigate(Screen.ProfileEdit.route) },
                onFavoritesClick = { navController.navigate(Screen.FavoriteAuctions.route) },
                onRegisteredProductsClick = { navController.navigate(Screen.RegisteredProducts.route) },
                onLiveManagementClick = { navController.navigate(Screen.LiveManagement.route) },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onInquiriesClick = { navController.navigate(Screen.Inquiries.route) },
                onAddressesClick = { navController.navigate(Screen.Addresses.route) },
                onAccountsClick = { navController.navigate(Screen.SettlementAccounts.route) },
                onSettlementsClick = { navController.navigate(Screen.Settlements.route) },
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
        composable(Screen.LiveManagement.route) {
            var broadcasts by remember { mutableStateOf<List<com.ssafy.dib.domain.live.LiveBroadcastSummary>?>(null) }
            var assignedAuctions by remember { mutableStateOf<Map<String, List<com.ssafy.dib.domain.auction.AuctionSummary>>>(emptyMap()) }
            var availableLiveAuctions by remember { mutableStateOf<List<com.ssafy.dib.domain.auction.AuctionSummary>>(emptyList()) }
            var liveManagementLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var liveManagementError by remember { mutableStateOf<String?>(null) }
            var liveManagementRevision by remember { mutableStateOf(0) }
            var liveActionLoading by remember { mutableStateOf(false) }
            var liveActionError by remember { mutableStateOf<String?>(null) }
            var liveActionMessage by remember { mutableStateOf<String?>(null) }
            var liveActionRevision by remember { mutableStateOf(0) }

            LaunchedEffect(liveManagementRevision, signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                liveManagementLoading = true
                liveManagementError = null
                val profile = memberProfile ?: when (val result = withContext(Dispatchers.IO) { auth.memberRepository.getMe() }) {
                    is ApiResult.Success -> result.value.also { memberProfile = it }
                    is ApiResult.Failure -> null
                }
                when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getMine() }) {
                    is ApiResult.Success -> {
                        broadcasts = result.value
                        val loadedAssignments = mutableMapOf<String, List<com.ssafy.dib.domain.auction.AuctionSummary>>()
                        result.value.filter { it.status == "SCHEDULED" || it.status == "LIVE" }.forEach { live ->
                            when (val detail = withContext(Dispatchers.IO) { auth.liveRepository.getDetail(live.liveBroadcastId) }) {
                                is ApiResult.Success -> loadedAssignments[live.liveBroadcastId] = detail.value.auctions
                                is ApiResult.Failure -> Unit
                            }
                        }
                        assignedAuctions = loadedAssignments
                    }
                    is ApiResult.Failure -> {
                        liveManagementError = result.error.message.ifBlank { "내 Live 방송을 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getAuctions("GENERAL", "SCHEDULED") }) {
                    is ApiResult.Success -> availableLiveAuctions = profile?.memberId?.let { memberId ->
                        result.value.filter { auction -> auction.sellerMemberId == memberId }
                    }.orEmpty()
                    is ApiResult.Failure -> if (result.error.requiresLogin) signedIn = false
                }
                liveManagementLoading = false
            }

            LiveManagementScreen(
                broadcasts = broadcasts,
                assignedAuctions = assignedAuctions,
                availableAuctions = availableLiveAuctions,
                isLoading = liveManagementLoading,
                errorMessage = liveManagementError,
                actionLoading = liveActionLoading,
                actionError = liveActionError,
                actionMessage = liveActionMessage,
                actionRevision = liveActionRevision,
                onRetry = { liveManagementRevision++ },
                onCreate = { title, description, scheduledAt, streamUrl ->
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.create(title, description, scheduledAt, streamUrl) }) {
                            is ApiResult.Success -> { liveActionRevision++; liveManagementRevision++ }
                            is ApiResult.Failure -> {
                                liveActionError = result.error.message.ifBlank { "Live 방송을 예약하지 못했어요." }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        liveActionLoading = false
                    }
                },
                onUpdate = { liveId, title, description, scheduledAt, streamUrl ->
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.update(liveId, title, description, scheduledAt, streamUrl) }) {
                            is ApiResult.Success -> { liveActionMessage = "Live 예약 정보를 수정했어요."; liveActionRevision++; liveManagementRevision++ }
                            is ApiResult.Failure -> {
                                liveActionError = liveControlError(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        liveActionLoading = false
                    }
                },
                onSetItems = { liveId, auctionIds ->
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.setItems(liveId, auctionIds) }) {
                            is ApiResult.Success -> { assignedAuctions = assignedAuctions + (liveId to result.value); liveActionRevision++; liveManagementRevision++ }
                            is ApiResult.Failure -> {
                                liveActionError = result.error.message.ifBlank { "Live 상품 편성을 저장하지 못했어요." }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        liveActionLoading = false
                    }
                },
                onPrepareStream = { liveId ->
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.prepareStream(liveId) }) {
                            is ApiResult.Success -> {
                                broadcasts = broadcasts?.map { live -> if (live.liveBroadcastId == liveId) live.copy(streamUrl = result.value.streamUrl) else live }
                                liveActionMessage = "송출 연결 정보를 준비했어요."
                                liveManagementRevision++
                            }
                            is ApiResult.Failure -> { liveActionError = liveControlError(result.error); if (result.error.requiresLogin) signedIn = false }
                        }
                        liveActionLoading = false
                    }
                },
                onStartLive = { liveId ->
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.start(liveId) }) {
                            is ApiResult.Success -> { liveActionMessage = "Live 방송을 시작했어요."; liveManagementRevision++ }
                            is ApiResult.Failure -> { liveActionError = liveControlError(result.error); if (result.error.requiresLogin) signedIn = false }
                        }
                        liveActionLoading = false
                    }
                },
                onStartAuction = { liveId, auctionId ->
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.startAuction(liveId, auctionId) }) {
                            is ApiResult.Success -> { liveActionMessage = "Live 상품 경매를 시작했어요."; liveManagementRevision++ }
                            is ApiResult.Failure -> { liveActionError = liveControlError(result.error); if (result.error.requiresLogin) signedIn = false }
                        }
                        liveActionLoading = false
                    }
                },
                onEndLive = { liveId ->
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.end(liveId) }) {
                            is ApiResult.Success -> { liveActionMessage = "Live 방송을 종료했어요."; liveManagementRevision++ }
                            is ApiResult.Failure -> { liveActionError = liveControlError(result.error); if (result.error.requiresLogin) signedIn = false }
                        }
                        liveActionLoading = false
                    }
                },
                onBack = navController::navigateUp
            )
        }
        composable(Screen.Addresses.route) {
            AddressManagementScreen(onBack = navController::navigateUp, onTabSelected = ::navigateMain)
        }
        composable(Screen.SettlementAccounts.route) {
            var settlementAccount by remember { mutableStateOf<com.ssafy.dib.domain.settlement.SettlementAccount?>(null) }
            var settlementLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var settlementError by remember { mutableStateOf<String?>(null) }
            var settlementRevision by remember { mutableStateOf(0) }
            var settlementChallengeId by remember { mutableStateOf<String?>(null) }
            var settlementVerificationToken by remember { mutableStateOf<String?>(null) }
            var settlementActionLoading by remember { mutableStateOf(false) }
            var settlementActionError by remember { mutableStateOf<String?>(null) }
            var settlementActionRevision by remember { mutableStateOf(0) }

            LaunchedEffect(settlementRevision, signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                settlementLoading = true
                settlementError = null
                when (val result = withContext(Dispatchers.IO) { auth.settlementAccountRepository.getAccount() }) {
                    is ApiResult.Success -> settlementAccount = result.value
                    is ApiResult.Failure -> when (result.error.code) {
                        "ACCOUNT_NOT_FOUND" -> settlementAccount = null
                        else -> {
                            settlementError = result.error.message.ifBlank { "정산 계좌를 불러오지 못했어요." }
                            if (result.error.requiresLogin) signedIn = false
                        }
                    }
                }
                settlementLoading = false
            }

            SettlementAccountsScreen(
                account = settlementAccount,
                isLoading = settlementLoading,
                errorMessage = settlementError,
                verificationRequested = settlementChallengeId != null,
                verificationConfirmed = settlementVerificationToken != null,
                actionLoading = settlementActionLoading,
                actionError = settlementActionError,
                actionRevision = settlementActionRevision,
                onRetry = { settlementRevision++ },
                onRequestVerification = { phone ->
                    settlementActionLoading = true
                    settlementActionError = null
                    settlementVerificationToken = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.repository.requestSensitivePhoneVerification(phone) }) {
                            is ApiResult.Success -> settlementChallengeId = result.value.verificationId
                            is ApiResult.Failure -> settlementActionError = signupErrorMessage(result.error)
                        }
                        settlementActionLoading = false
                    }
                },
                onConfirmVerification = { code ->
                    val challengeId = settlementChallengeId ?: return@SettlementAccountsScreen
                    settlementActionLoading = true
                    settlementActionError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.repository.confirmPhoneVerification(challengeId, code) }) {
                            is ApiResult.Success -> settlementVerificationToken = result.value.verificationToken
                            is ApiResult.Failure -> settlementActionError = signupErrorMessage(result.error)
                        }
                        settlementActionLoading = false
                    }
                },
                onSave = { bankName, accountNumber, accountHolder ->
                    val token = settlementVerificationToken ?: return@SettlementAccountsScreen
                    settlementActionLoading = true
                    settlementActionError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.settlementAccountRepository.saveAccount(token, bankName, accountNumber, accountHolder) }) {
                            is ApiResult.Success -> {
                                settlementAccount = result.value
                                settlementActionRevision++
                                settlementChallengeId = null
                                settlementVerificationToken = null
                            }
                            is ApiResult.Failure -> {
                                settlementActionError = when (result.error.code) {
                                    "INVALID_VERIFICATION" -> "본인 인증이 만료됐어요. 다시 인증해주세요."
                                    "ACCOUNT_VERIFICATION_FAILED" -> "은행·계좌번호·예금주가 일치하는지 확인해주세요."
                                    else -> result.error.message.ifBlank { "정산 계좌를 저장하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        settlementActionLoading = false
                    }
                },
                onResetVerification = {
                    settlementChallengeId = null
                    settlementVerificationToken = null
                    settlementActionError = null
                },
                onBack = navController::navigateUp,
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.Settlements.route) {
            var settlements by remember { mutableStateOf<List<com.ssafy.dib.domain.settlement.SettlementSummary>?>(null) }
            var settlementsLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var settlementsError by remember { mutableStateOf<String?>(null) }
            var settlementsCursor by remember { mutableStateOf<String?>(null) }
            var settlementsHasNext by remember { mutableStateOf(false) }
            var settlementsRevision by remember { mutableStateOf(0) }

            fun loadSettlements(cursor: String?, append: Boolean) {
                settlementsLoading = true
                settlementsError = null
                coroutineScope.launch {
                    when (val result = withContext(Dispatchers.IO) { auth.settlementRepository.getSettlements(cursor) }) {
                        is ApiResult.Success -> {
                            settlements = if (append) settlements.orEmpty() + result.value.items else result.value.items
                            settlementsCursor = result.value.nextCursor
                            settlementsHasNext = result.value.hasNext
                        }
                        is ApiResult.Failure -> {
                            settlementsError = result.error.message.ifBlank { "정산 내역을 불러오지 못했어요." }
                            if (result.error.requiresLogin) signedIn = false
                        }
                    }
                    settlementsLoading = false
                }
            }

            LaunchedEffect(settlementsRevision, signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                loadSettlements(cursor = null, append = false)
            }

            SettlementHistoryScreen(
                settlements = settlements,
                isLoading = settlementsLoading,
                errorMessage = settlementsError,
                hasNext = settlementsHasNext,
                onRetry = { settlementsRevision++ },
                onLoadMore = { if (!settlementsLoading && settlementsHasNext) loadSettlements(settlementsCursor, append = true) },
                onSettlementClick = { navController.navigate(Screen.SettlementDetail.createRoute(it)) },
                onBack = navController::navigateUp
            )
        }
        composable(
            route = Screen.SettlementDetail.route,
            arguments = listOf(navArgument("settlementId") { type = NavType.StringType })
        ) { backStackEntry ->
            val settlementId = backStackEntry.arguments?.getString("settlementId").orEmpty()
            var settlement by remember { mutableStateOf<com.ssafy.dib.domain.settlement.SettlementDetail?>(null) }
            var settlementLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var settlementError by remember { mutableStateOf<String?>(null) }
            var settlementRevision by remember { mutableStateOf(0) }

            LaunchedEffect(settlementId, settlementRevision, signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                settlementLoading = true
                settlementError = null
                when (val result = withContext(Dispatchers.IO) { auth.settlementRepository.getSettlement(settlementId) }) {
                    is ApiResult.Success -> settlement = result.value
                    is ApiResult.Failure -> {
                        settlementError = when (result.error.code) {
                            "SETTLEMENT_NOT_FOUND" -> "정산 내역을 찾을 수 없어요."
                            "FORBIDDEN" -> "이 정산 내역을 볼 권한이 없어요."
                            else -> result.error.message.ifBlank { "정산 상세를 불러오지 못했어요." }
                        }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                settlementLoading = false
            }

            SettlementDetailScreen(
                detail = settlement,
                isLoading = settlementLoading,
                errorMessage = settlementError,
                onRetry = { settlementRevision++ },
                onBack = navController::navigateUp
            )
        }
        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(onBack = navController::navigateUp, onTabSelected = ::navigateMain)
        }
        composable(Screen.ProfileEdit.route) {
            var profileLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var profileError by remember { mutableStateOf<String?>(null) }
            var profileRevision by remember { mutableStateOf(0) }
            var profileSaveLoading by remember { mutableStateOf(false) }
            var profileSaveError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(profileRevision, signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                profileLoading = true
                profileError = null
                when (val result = withContext(Dispatchers.IO) { auth.memberRepository.getMe() }) {
                    is ApiResult.Success -> memberProfile = result.value
                    is ApiResult.Failure -> {
                        profileError = result.error.message.ifBlank { "내 정보를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                profileLoading = false
            }
            ProfileEditScreen(
                profile = memberProfile,
                isLoading = profileLoading,
                errorMessage = profileError,
                saveLoading = profileSaveLoading,
                saveError = profileSaveError,
                onRetry = { profileRevision++ },
                onSave = { nickname ->
                    profileSaveLoading = true
                    profileSaveError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.memberRepository.updateNickname(nickname) }) {
                            is ApiResult.Success -> {
                                memberProfile = memberProfile?.copy(nickname = result.value.nickname)
                                navController.navigateUp()
                            }
                            is ApiResult.Failure -> {
                                profileSaveError = when (result.error.code) {
                                    "NICKNAME_DUPLICATED" -> "이미 사용 중인 닉네임이에요."
                                    else -> result.error.message.ifBlank { "닉네임을 변경하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        profileSaveLoading = false
                    }
                },
                onBack = navController::navigateUp
            )
        }
        composable(Screen.FavoriteAuctions.route) {
            var favorites by remember { mutableStateOf<List<HomeAuction>?>(null) }
            var favoritesLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var favoritesError by remember { mutableStateOf<String?>(null) }
            var favoritesRevision by remember { mutableStateOf(0) }
            var removingAuctionId by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(favoritesRevision, signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                favoritesLoading = true
                favoritesError = null
                when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getBookmarks() }) {
                    is ApiResult.Success -> favorites = result.value.map { it.toHomeAuction() }
                    is ApiResult.Failure -> {
                        favoritesError = result.error.message.ifBlank { "찜한 경매를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                favoritesLoading = false
            }
            FavoriteAuctionsScreen(
                onBack = navController::navigateUp,
                onProductClick = { auctionId -> navController.navigate(Screen.ProductDetail.createRoute(auctionId)) },
                onTabSelected = ::navigateMain,
                remoteFavorites = favorites,
                isLoading = favoritesLoading,
                errorMessage = favoritesError,
                removingAuctionId = removingAuctionId,
                onRetry = { favoritesRevision++ },
                onRemove = { auctionId ->
                    removingAuctionId = auctionId
                    favoritesError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.auctionRepository.setBookmark(auctionId, false, java.util.UUID.randomUUID().toString())
                        }) {
                            is ApiResult.Success -> {
                                favorites = favorites?.filterNot { it.id == auctionId }
                                remoteAuctions = remoteAuctions?.map { auction ->
                                    if (auction.id == auctionId) auction.copy(bookmarked = false) else auction
                                }
                            }
                            is ApiResult.Failure -> {
                                favoritesError = result.error.message.ifBlank { "찜을 해제하지 못했어요." }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        removingAuctionId = null
                    }
                }
            )
        }
        composable(Screen.RegisteredProducts.route) { backStackEntry ->
            val productsRefresh by backStackEntry.savedStateHandle.getStateFlow("refreshProducts", 0L).collectAsState()
            var registeredProducts by remember { mutableStateOf<List<com.ssafy.dib.domain.product.RegisteredProduct>?>(null) }
            var registeredProductsLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var registeredProductsError by remember { mutableStateOf<String?>(null) }
            var registeredProductsRevision by remember { mutableStateOf(0) }
            var deletingProductId by remember { mutableStateOf<String?>(null) }
            var productDeleteError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(registeredProductsRevision, productsRefresh) {
                if (!auth.networkConfig.isRestConfigured) return@LaunchedEffect
                registeredProductsLoading = true
                registeredProductsError = null
                when (val result = withContext(Dispatchers.IO) { auth.productRepository.getMyProducts() }) {
                    is ApiResult.Success -> registeredProducts = result.value
                    is ApiResult.Failure -> {
                        registeredProductsError = result.error.message.ifBlank { "등록 상품을 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                registeredProductsLoading = false
            }
            RegisteredProductsScreen(
                onBack = navController::navigateUp,
                onRegister = { navController.navigate(Screen.Register.route) },
                onTabSelected = ::navigateMain,
                remoteProducts = registeredProducts,
                isLoading = registeredProductsLoading,
                errorMessage = registeredProductsError,
                deleteError = productDeleteError,
                deletingProductId = deletingProductId,
                onRetry = { registeredProductsRevision++ },
                onAuctionRegister = { productId -> navController.navigate(Screen.AuctionRegister.createRoute(productId)) },
                onEditProduct = { productId -> navController.navigate(Screen.ProductEdit.createRoute(productId)) },
                onDeleteProduct = { productId ->
                    deletingProductId = productId
                    productDeleteError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.productRepository.deleteProduct(productId) }) {
                            is ApiResult.Success -> registeredProducts = registeredProducts?.filterNot { it.productId == productId }
                            is ApiResult.Failure -> {
                                productDeleteError = when (result.error.code) {
                                    "PRODUCT_NOT_DELETABLE" -> "진행 중인 경매나 거래 이력이 있어 삭제할 수 없어요."
                                    "PRODUCT_NOT_FOUND" -> "이미 삭제됐거나 찾을 수 없는 상품이에요."
                                    "FORBIDDEN" -> "본인이 등록한 상품만 삭제할 수 있어요."
                                    else -> result.error.message.ifBlank { "상품을 삭제하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        deletingProductId = null
                    }
                }
            )
        }
        composable(
            route = Screen.ProductEdit.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            var editDetail by remember(productId) { mutableStateOf<com.ssafy.dib.domain.product.ProductDetail?>(null) }
            var editCategories by remember(productId) { mutableStateOf<List<ProductCategory>>(emptyList()) }
            var editLoading by remember(productId) { mutableStateOf(true) }
            var editError by remember(productId) { mutableStateOf<String?>(null) }
            var editRevision by remember(productId) { mutableStateOf(0) }
            var editSubmitLoading by remember(productId) { mutableStateOf(false) }
            var editSubmitError by remember(productId) { mutableStateOf<String?>(null) }
            var editResult by remember(productId) { mutableStateOf<com.ssafy.dib.domain.product.ProductUpdateResult?>(null) }

            LaunchedEffect(productId, editRevision) {
                editLoading = true
                editError = null
                val detailResult = withContext(Dispatchers.IO) { auth.productRepository.getProduct(productId) }
                val categoriesResult = withContext(Dispatchers.IO) { auth.productRepository.getCategories() }
                if (detailResult is ApiResult.Success && categoriesResult is ApiResult.Success) {
                    editDetail = detailResult.value
                    editCategories = categoriesResult.value
                } else {
                    val failure = (detailResult as? ApiResult.Failure) ?: (categoriesResult as? ApiResult.Failure)
                    editError = failure?.error?.message?.ifBlank { "상품 정보를 불러오지 못했어요." }
                }
                editLoading = false
            }

            ProductEditScreen(
                detail = editDetail,
                categories = editCategories,
                isLoading = editLoading,
                errorMessage = editError,
                submitLoading = editSubmitLoading,
                submitError = editSubmitError,
                result = editResult,
                onRetry = { editRevision++ },
                onSubmit = { update, imageUris ->
                    editSubmitLoading = true
                    editSubmitError = null
                    coroutineScope.launch {
                        val replacementImages = withContext(Dispatchers.IO) {
                            runCatching {
                                imageUris?.mapIndexed { index, uri ->
                                    val secondaryTypes = listOf("LEFT", "RIGHT", "TOP", "BOTTOM", "BACK")
                                    ProductImageUpload(
                                        fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "product-update-$index.jpg",
                                        mediaType = context.contentResolver.getType(uri) ?: "image/jpeg",
                                        bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("선택한 사진을 읽을 수 없습니다."),
                                        type = if (index == 0) "FRONT" else secondaryTypes[(index - 1) % secondaryTypes.size]
                                    )
                                }
                            }
                        }
                        replacementImages.fold(
                            onSuccess = { images ->
                                when (val result = withContext(Dispatchers.IO) { auth.productRepository.updateProduct(productId, update.copy(replacementImages = images)) }) {
                                    is ApiResult.Success -> editResult = result.value
                                    is ApiResult.Failure -> {
                                        editSubmitError = when (result.error.code) {
                                            "PRODUCT_NOT_EDITABLE" -> "진행 중인 경매나 거래가 있어 수정할 수 없어요."
                                            "PRODUCT_NOT_FOUND" -> "상품을 찾을 수 없어요."
                                            "FORBIDDEN" -> "본인이 등록한 상품만 수정할 수 있어요."
                                            "IMAGE_REQUIRED" -> "상품 이미지를 한 장 이상 선택해주세요."
                                            "FILE_TOO_LARGE" -> "이미지 용량이 너무 커요."
                                            "FILE_COUNT_EXCEEDED" -> "상품 이미지는 최대 10장까지 등록할 수 있어요."
                                            else -> result.error.message.ifBlank { "상품을 수정하지 못했어요." }
                                        }
                                    }
                                }
                            },
                            onFailure = { editSubmitError = it.message ?: "선택한 사진을 읽지 못했어요." }
                        )
                        editSubmitLoading = false
                    }
                },
                onComplete = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refreshProducts", System.currentTimeMillis())
                    navController.popBackStack()
                },
                onBack = navController::navigateUp
            )
        }
        composable(
            route = Screen.AuctionRegister.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            var createResult by remember(productId) { mutableStateOf<com.ssafy.dib.domain.auction.AuctionCommandResult?>(null) }
            var auctionStarted by remember(productId) { mutableStateOf(false) }
            var auctionCancelled by remember(productId) { mutableStateOf(false) }
            var commandLoading by remember(productId) { mutableStateOf(false) }
            var commandError by remember(productId) { mutableStateOf<String?>(null) }
            val createKey = remember(productId) { java.util.UUID.randomUUID().toString() }
            val startKey = remember(productId) { java.util.UUID.randomUUID().toString() }
            val cancelKey = remember(productId) { java.util.UUID.randomUUID().toString() }

            AuctionRegisterScreen(
                productId = productId,
                result = createResult,
                started = auctionStarted,
                cancelled = auctionCancelled,
                isLoading = commandLoading,
                errorMessage = commandError,
                onCreate = { startPrice, auctionTime ->
                    commandLoading = true
                    commandError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.createAuction(productId, startPrice, auctionTime, createKey) }) {
                            is ApiResult.Success -> createResult = result.value
                            is ApiResult.Failure -> {
                                commandError = auctionCommandError(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        commandLoading = false
                    }
                },
                onUpdate = { startPrice, auctionTime ->
                    val auctionId = createResult?.auctionId ?: return@AuctionRegisterScreen
                    commandLoading = true
                    commandError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.updateAuction(auctionId, startPrice, auctionTime) }) {
                            is ApiResult.Success -> createResult = result.value
                            is ApiResult.Failure -> commandError = auctionCommandError(result.error)
                        }
                        commandLoading = false
                    }
                },
                onCancel = {
                    val auctionId = createResult?.auctionId ?: return@AuctionRegisterScreen
                    commandLoading = true
                    commandError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.cancelAuction(auctionId, cancelKey) }) {
                            is ApiResult.Success -> { auctionCancelled = true; auctionsRevision++ }
                            is ApiResult.Failure -> commandError = auctionCommandError(result.error)
                        }
                        commandLoading = false
                    }
                },
                onStart = {
                    val auctionId = createResult?.auctionId ?: return@AuctionRegisterScreen
                    commandLoading = true
                    commandError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.startAuction(auctionId, startKey) }) {
                            is ApiResult.Success -> { auctionStarted = true; auctionsRevision++ }
                            is ApiResult.Failure -> commandError = auctionCommandError(result.error)
                        }
                        commandLoading = false
                    }
                },
                onOpenAuction = { createResult?.auctionId?.let { navController.navigate(Screen.ProductDetail.createRoute(it)) } },
                onFinish = navController::navigateUp,
                onBack = navController::navigateUp
            )
        }
        composable(Screen.Inquiries.route) {
            var inquiries by remember { mutableStateOf<List<InquirySummary>?>(null) }
            var inquiriesLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var inquiriesError by remember { mutableStateOf<String?>(null) }
            var inquiriesRevision by remember { mutableStateOf(0) }
            var selectedInquiry by remember { mutableStateOf<InquiryDetail?>(null) }
            var inquiryDetailLoading by remember { mutableStateOf(false) }
            var inquiryDetailError by remember { mutableStateOf<String?>(null) }
            var inquirySubmitLoading by remember { mutableStateOf(false) }
            var inquirySubmitError by remember { mutableStateOf<String?>(null) }
            var inquirySubmissionRevision by remember { mutableStateOf(0) }

            LaunchedEffect(inquiriesRevision) {
                if (!auth.networkConfig.isRestConfigured) return@LaunchedEffect
                inquiriesLoading = true
                inquiriesError = null
                when (val result = withContext(Dispatchers.IO) { auth.inquiryRepository.getInquiries() }) {
                    is ApiResult.Success -> inquiries = result.value
                    is ApiResult.Failure -> {
                        inquiriesError = result.error.message.ifBlank { "문의 내역을 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                inquiriesLoading = false
            }

            InquiryHistoryScreen(
                onBack = navController::navigateUp,
                onTabSelected = ::navigateMain,
                remoteInquiries = inquiries,
                isLoading = inquiriesLoading,
                errorMessage = inquiriesError,
                selectedInquiry = selectedInquiry,
                detailLoading = inquiryDetailLoading,
                detailError = inquiryDetailError,
                submitLoading = inquirySubmitLoading,
                submitError = inquirySubmitError,
                submissionRevision = inquirySubmissionRevision,
                onRetry = { inquiriesRevision++ },
                onInquiryClick = { questionId ->
                    inquiryDetailLoading = true
                    inquiryDetailError = null
                    selectedInquiry = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.inquiryRepository.getInquiry(questionId)
                        }) {
                            is ApiResult.Success -> selectedInquiry = result.value
                            is ApiResult.Failure -> inquiryDetailError = result.error.message.ifBlank { "문의 상세를 불러오지 못했어요." }
                        }
                        inquiryDetailLoading = false
                    }
                },
                onDetailDismiss = {
                    selectedInquiry = null
                    inquiryDetailLoading = false
                    inquiryDetailError = null
                },
                onSubmit = { title, content ->
                    inquirySubmitLoading = true
                    inquirySubmitError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.inquiryRepository.createInquiry(title, content)
                        }) {
                            is ApiResult.Success -> {
                                inquirySubmissionRevision++
                                inquiriesRevision++
                            }
                            is ApiResult.Failure -> inquirySubmitError = when (result.error.code) {
                                "INVALID_QUESTION" -> "제목과 문의 내용을 확인해주세요."
                                else -> result.error.message.ifBlank { "문의를 등록하지 못했어요." }
                            }
                        }
                        inquirySubmitLoading = false
                    }
                }
            )
        }
        composable(Screen.ReportHistory.route) {
            var reports by remember { mutableStateOf<List<ReportSummary>?>(null) }
            var reportsLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
            var reportsError by remember { mutableStateOf<String?>(null) }
            var reportsRevision by remember { mutableStateOf(0) }
            LaunchedEffect(reportsRevision) {
                if (!auth.networkConfig.isRestConfigured) return@LaunchedEffect
                reportsLoading = true
                reportsError = null
                when (val result = withContext(Dispatchers.IO) { auth.reportRepository.getMyReports() }) {
                    is ApiResult.Success -> reports = result.value
                    is ApiResult.Failure -> {
                        reportsError = result.error.message.ifBlank { "신고 내역을 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                reportsLoading = false
            }
            ReportHistoryScreen(
                onBack = navController::navigateUp,
                reports = reports,
                isLoading = reportsLoading,
                errorMessage = reportsError,
                onRetry = { reportsRevision++ }
            )
        }
        composable(Screen.Withdrawal.route) {
            var withdrawalLoading by remember { mutableStateOf(false) }
            var withdrawalError by remember { mutableStateOf<String?>(null) }
            var withdrawalBlockingMessage by remember { mutableStateOf<String?>(null) }
            var withdrawalCompleted by remember { mutableStateOf(false) }
            WithdrawalScreen(
                isSubmitting = withdrawalLoading,
                errorMessage = withdrawalError,
                blockingMessage = withdrawalBlockingMessage,
                completed = withdrawalCompleted,
                onSubmit = {
                    withdrawalLoading = true
                    withdrawalError = null
                    withdrawalBlockingMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.memberRepository.requestWithdrawal() }) {
                            is ApiResult.Success -> withdrawalCompleted = true
                            is ApiResult.Failure -> {
                                when (result.error.code) {
                                    "ACTIVE_ORDER_EXISTS" -> withdrawalBlockingMessage = "진행 중인 주문을 모두 완료한 뒤 다시 시도해주세요."
                                    "ACTIVE_AUCTION_EXISTS" -> withdrawalBlockingMessage = "진행 중인 경매를 모두 종료한 뒤 다시 시도해주세요."
                                    else -> withdrawalError = result.error.message.ifBlank { "탈퇴를 신청하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        withdrawalLoading = false
                    }
                },
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
            val imageUrls = navController.previousBackStackEntry?.savedStateHandle
                ?.get<ArrayList<String>>("productImageUrls").orEmpty()
            ProductImageViewerScreen(
                productId = backStackEntry.arguments?.getString("productId").orEmpty(),
                initialPage = backStackEntry.arguments?.getInt("initialPage") ?: 0,
                imageUrls = imageUrls,
                onClose = navController::navigateUp
            )
        }
        composable(
            route = Screen.SellerProfile.route,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId").orEmpty()
            SellerProfileScreen(
                onBack = navController::navigateUp,
                onReviewsClick = { navController.navigate(Screen.SellerReviews.createRoute(memberId)) },
                onListingsClick = { navController.navigate(Screen.SellerListings.createRoute(memberId)) },
                onReportClick = {
                    if (signedIn == true) navController.navigate(Screen.SellerReport.createRoute(memberId))
                    else navController.navigate(Screen.Login.route)
                }
            )
        }
        composable(
            route = Screen.SellerReviews.route,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) {
            SellerReviewsScreen(onBack = navController::navigateUp)
        }
        composable(
            route = Screen.SellerListings.route,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) {
            SellerListingsScreen(
                onBack = navController::navigateUp,
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) }
            )
        }
        composable(
            route = Screen.SellerReport.route,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId").orEmpty()
            var submitted by remember { mutableStateOf(false) }
            var submitting by remember { mutableStateOf(false) }
            var reportError by remember { mutableStateOf<String?>(null) }
            SellerReportScreen(
                onBack = navController::navigateUp,
                submitted = submitted,
                isSubmitting = submitting,
                errorMessage = reportError,
                onSubmit = { content ->
                    submitting = true
                    reportError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.reportRepository.reportMember(memberId, content)
                        }) {
                            is ApiResult.Success -> submitted = true
                            is ApiResult.Failure -> reportError = reportSubmissionMessage(result.error)
                        }
                        submitting = false
                    }
                },
                onSubmitted = { navController.navigateUp() }
            )
        }
        composable(
            route = Screen.ProductReport.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val auctionId = backStackEntry.arguments?.getString("productId").orEmpty()
            var submitted by remember { mutableStateOf(false) }
            var submitting by remember { mutableStateOf(false) }
            var reportError by remember { mutableStateOf<String?>(null) }
            ProductReportScreen(
                onBack = navController::navigateUp,
                submitted = submitted,
                isSubmitting = submitting,
                errorMessage = reportError,
                onSubmit = { content ->
                    submitting = true
                    reportError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.reportRepository.reportAuction(auctionId, content)
                        }) {
                            is ApiResult.Success -> submitted = true
                            is ApiResult.Failure -> reportError = reportSubmissionMessage(result.error)
                        }
                        submitting = false
                    }
                },
                onSubmitted = { navController.navigateUp() }
            )
        }
        composable(
            route = Screen.BidDepositPayment.route,
            arguments = listOf(
                navArgument("auctionId") { type = NavType.StringType },
                navArgument("bidAmount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val auctionId = backStackEntry.arguments?.getString("auctionId").orEmpty()
            val bidAmount = backStackEntry.arguments?.getInt("bidAmount") ?: 0
            var preparedDeposit by remember { mutableStateOf<com.ssafy.dib.domain.auction.BidDeposit?>(null) }
            var depositProcessing by remember { mutableStateOf(false) }
            var depositError by remember { mutableStateOf<String?>(null) }
            var prepareKey by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }

            fun prepareDeposit(paymentMethod: String) {
                depositProcessing = true
                depositError = null
                coroutineScope.launch {
                    when (val result = withContext(Dispatchers.IO) {
                        auth.bidDepositRepository.prepare(
                            auctionId = auctionId,
                            firstBidAmount = bidAmount.toLong(),
                            paymentMethod = paymentMethod,
                            idempotencyKey = prepareKey
                        )
                    }) {
                        is ApiResult.Success -> preparedDeposit = result.value
                        is ApiResult.Failure -> {
                            depositError = result.error.message.ifBlank { "보증금 결제를 준비하지 못했어요." }
                            if (result.error.requiresLogin) signedIn = false
                        }
                    }
                    depositProcessing = false
                }
            }

            fun checkDepositStatus() {
                depositProcessing = true
                depositError = null
                coroutineScope.launch {
                    when (val result = withContext(Dispatchers.IO) { auth.bidDepositRepository.getMine(auctionId) }) {
                        is ApiResult.Success -> preparedDeposit = result.value
                        is ApiResult.Failure -> depositError = result.error.message.ifBlank { "결제 상태를 확인하지 못했어요." }
                    }
                    depositProcessing = false
                }
            }

            BidDepositPaymentScreen(
                auctionId = auctionId,
                bidAmount = bidAmount,
                preparedDeposit = preparedDeposit,
                isProcessing = depositProcessing,
                errorMessage = depositError,
                onPrepare = ::prepareDeposit,
                onCheckStatus = ::checkDepositStatus,
                onReset = {
                    preparedDeposit = null
                    depositError = null
                    prepareKey = java.util.UUID.randomUUID().toString()
                },
                onBack = navController::navigateUp,
                onReturnToAuction = {
                    val updatedPaidProducts = depositPaidProductIds + auctionId
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

private fun auctionCommandError(error: ApiFailure): String = when (error.code) {
    "NOT_MY_PRODUCT" -> "본인이 등록한 상품만 경매에 올릴 수 있어요."
    "PRODUCT_PENDING" -> "상품 검수가 끝난 뒤 경매를 등록할 수 있어요."
    "PRODUCT_ON_AUCTION" -> "이미 경매에 등록된 상품이에요."
    "PRODUCT_ALREADY_SOLD" -> "판매가 완료된 상품이에요."
    "PRODUCT_ALREADY_DELETED" -> "삭제된 상품이에요."
    "AUCTION_NOT_EDITABLE" -> "예정 상태의 경매만 변경하거나 시작할 수 있어요."
    else -> error.message.ifBlank { "경매 요청을 처리하지 못했어요." }
}

private fun liveControlError(error: ApiFailure): String = when (error.code) {
    "STREAM_UNAVAILABLE" -> "송출 연결을 준비하지 못했어요. 잠시 후 다시 시도해주세요."
    "LIVE_ITEMS_EMPTY" -> "상품을 한 개 이상 편성한 뒤 방송을 시작해주세요."
    "LIVE_END_BLOCKED_BY_AUCTION" -> "진행 중인 경매가 끝난 뒤 방송을 종료할 수 있어요."
    "LIVE_INVALID_STATUS" -> "현재 방송 상태에서는 이 작업을 할 수 없어요."
    "AUCTION_NOT_ACTIVE" -> "선택한 경매를 시작할 수 있는 상태가 아니에요."
    "NOT_BROADCASTER" -> "이 방송을 관리할 권한이 없어요."
    else -> error.message.ifBlank { "Live 요청을 처리하지 못했어요." }
}

private fun reportSubmissionMessage(error: ApiFailure): String = when (error.code) {
    ApiErrorCodes.CLIENT_NOT_CONFIGURED -> "개발 서버 주소가 설정되지 않았어요."
    "SELF_REPORT_NOT_ALLOWED" -> "본인은 신고할 수 없어요."
    "DUPLICATE_REPORT" -> "이미 접수된 신고가 있어요."
    "AUCTION_NOT_FOUND" -> "신고할 경매를 찾을 수 없어요."
    "MEMBER_NOT_FOUND" -> "신고할 회원을 찾을 수 없어요."
    else -> error.message.ifBlank { "신고를 접수하지 못했어요. 잠시 후 다시 시도해주세요." }
}

private fun productSubmissionMessage(error: ApiFailure): String = when (error.code) {
    ApiErrorCodes.CLIENT_NOT_CONFIGURED -> "개발 서버 주소가 설정되지 않았어요."
    "IMAGE_REQUIRED" -> "상품 사진을 한 장 이상 선택해주세요."
    "INVALID_CONTENT_TYPE" -> "지원하지 않는 사진 형식이 포함돼 있어요."
    "FILE_TOO_LARGE" -> "용량이 너무 큰 사진이 포함돼 있어요."
    "FILE_COUNT_EXCEEDED" -> "상품 사진은 최대 10장까지 등록할 수 있어요."
    "S3_UPLOAD_FAILED" -> "사진 업로드에 실패했어요. 다시 시도해주세요."
    "DUPLICATE_REQUEST" -> "이미 처리된 상품 등록 요청이에요."
    else -> error.message.ifBlank { "상품을 등록하지 못했어요. 잠시 후 다시 시도해주세요." }
}
