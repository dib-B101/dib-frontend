package com.ssafy.dib.core.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibCreateMenuSheet
import com.ssafy.dib.core.ui.AssistantNotice
import com.ssafy.dib.core.ui.DibFloatingAssistant
import com.ssafy.dib.core.ui.ReviewRatingDialog
import com.ssafy.dib.BuildConfig
import com.ssafy.dib.feature.auction.ProductDetailScreen
import com.ssafy.dib.feature.auction.RealtimeBidFeedback
import com.ssafy.dib.feature.auction.AuctionRegisterScreen
import com.ssafy.dib.feature.auction.ProductImageViewerScreen
import com.ssafy.dib.feature.auction.ProductOverviewScreen
import com.ssafy.dib.feature.auction.ProductReportScreen
import com.ssafy.dib.feature.auction.SellerProfileScreen
import com.ssafy.dib.feature.auction.SellerListingsScreen
import com.ssafy.dib.feature.auction.SellerListing
import com.ssafy.dib.feature.auction.SellerReportScreen
import com.ssafy.dib.feature.auction.SellerReviewsScreen
import com.ssafy.dib.feature.auth.LoginScreen
import com.ssafy.dib.feature.auth.FindEmailScreen
import com.ssafy.dib.feature.auth.PasswordResetLinkScreen
import com.ssafy.dib.feature.auth.PasswordResetScreen
import com.ssafy.dib.feature.auth.SignupScreen
import com.ssafy.dib.feature.auth.SignupUiState
import com.ssafy.dib.feature.auth.SignupMode
import com.ssafy.dib.feature.auth.SplashScreen
import com.ssafy.dib.feature.auth.WelcomeScreen
import com.ssafy.dib.feature.feed.LiveFeedScreen
import com.ssafy.dib.feature.live.LiveManagementScreen
import com.ssafy.dib.feature.live.LiveBroadcastConsoleScreen
import com.ssafy.dib.feature.live.LiveWatchScreen
import com.ssafy.dib.feature.live.LiveBidNotice
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
import com.ssafy.dib.feature.main.MyAuctionManagementScreen
import com.ssafy.dib.feature.main.MyTradesScreen
import com.ssafy.dib.feature.main.NotificationSettingsScreen
import com.ssafy.dib.feature.main.PaymentMethodsScreen
import com.ssafy.dib.feature.main.ProfileEditScreen
import com.ssafy.dib.feature.main.ProductRegisterScreen
import com.ssafy.dib.feature.main.ProductEditScreen
import com.ssafy.dib.feature.main.ProductRegistrationForm
import com.ssafy.dib.feature.main.resolveProductImageMediaType
import com.ssafy.dib.feature.main.RegisteredProductsScreen
import com.ssafy.dib.feature.main.ReportHistoryScreen
import com.ssafy.dib.feature.main.SettlementAccountsScreen
import com.ssafy.dib.feature.main.SettlementHistoryScreen
import com.ssafy.dib.feature.main.SettlementDetailScreen
import com.ssafy.dib.feature.main.ORDER_HOLD_BLOCK_MESSAGE
import com.ssafy.dib.feature.main.TransactionScreen
import com.ssafy.dib.feature.main.OrderChatScreen
import com.ssafy.dib.feature.main.WithdrawalScreen
import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.RetriableCommandKeys
import com.ssafy.dib.data.AuthDependencies
import com.ssafy.dib.domain.auth.SignUpCommand
import com.ssafy.dib.domain.auth.KakaoAuthenticationResult
import com.ssafy.dib.domain.auth.KakaoSignupCommand
import com.ssafy.dib.core.auth.KakaoOAuthCallback
import com.ssafy.dib.domain.order.OrderRole
import com.ssafy.dib.domain.auction.SellerAuction
import com.ssafy.dib.domain.order.OrderSummary
import com.ssafy.dib.domain.auction.SaleHistoryItem
import com.ssafy.dib.domain.support.InquiryDetail
import com.ssafy.dib.domain.support.InquirySummary
import com.ssafy.dib.domain.report.ReportSummary
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.domain.product.ProductImageUpload
import com.ssafy.dib.domain.product.ProductRegistration
import com.ssafy.dib.domain.product.ProductRegistrationResult
import com.ssafy.dib.domain.product.ProductSearchFilter
import com.ssafy.dib.domain.product.RegisteredProduct
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import com.ssafy.dib.data.remote.socket.AuctionRealtimeConnection
import com.ssafy.dib.data.remote.socket.SocketEventTypes
import com.ssafy.dib.domain.notification.DomainNotification
import com.ssafy.dib.domain.notification.NotificationCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun AppNavHost(
    oauthCallbackUri: Uri? = null,
    onOAuthCallbackConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val auth = remember(context) { AuthDependencies(context) }
    val coroutineScope = rememberCoroutineScope()
    val notificationSnackbar = remember { SnackbarHostState() }
    val session = remember(context) { context.getSharedPreferences("dib_session", 0) }
    val navigationPreferences = remember(context) { context.getSharedPreferences("dib_navigation", 0) }
    val notificationPreferences = remember(context) { context.getSharedPreferences("dib_notification_preferences", 0) }
    val commandKeys = remember { RetriableCommandKeys() }
    var signedIn by remember { mutableStateOf<Boolean?>(null) }
    var previewMode by rememberSaveable { mutableStateOf(false) }
    val hasAppAccess = signedIn == true || previewMode
    var showCreateMenu by rememberSaveable { mutableStateOf(false) }
    var productSelectionPurpose by rememberSaveable { mutableStateOf<String?>(null) }
    var browseAllAuctions by rememberSaveable { mutableStateOf(false) }
    var memberProfile by remember { mutableStateOf<com.ssafy.dib.domain.member.MemberProfile?>(null) }
    var loginLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var signupState by remember { mutableStateOf(SignupUiState()) }
    var phoneVerificationToken by remember { mutableStateOf<String?>(null) }
    var kakaoSignupToken by remember { mutableStateOf<String?>(null) }
    var kakaoNickname by remember { mutableStateOf<String?>(null) }
    var remoteAuctions by remember { mutableStateOf<List<HomeAuction>?>(null) }
    var remoteHomeLives by remember { mutableStateOf<List<com.ssafy.dib.domain.auction.RecommendedLive>?>(null) }
    var auctionsLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
    var auctionsError by remember { mutableStateOf<String?>(null) }
    var auctionsRevision by remember { mutableStateOf(0) }
    var purchaseOrders by remember { mutableStateOf<List<OrderSummary>?>(null) }
    var saleOrders by remember { mutableStateOf<List<SaleHistoryItem>?>(null) }
    var purchaseOrdersCursor by remember { mutableStateOf<String?>(null) }
    var saleOrdersCursor by remember { mutableStateOf<String?>(null) }
    var purchaseOrdersHasNext by remember { mutableStateOf(false) }
    var saleOrdersHasNext by remember { mutableStateOf(false) }
    var ordersLoadingMoreRole by remember { mutableStateOf<OrderRole?>(null) }
    var purchaseOrdersLoadMoreError by remember { mutableStateOf<String?>(null) }
    var saleOrdersLoadMoreError by remember { mutableStateOf<String?>(null) }
    var ordersLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
    var ordersError by remember { mutableStateOf<String?>(null) }
    var ordersRevision by remember { mutableStateOf(0) }
    var bidHistory by remember { mutableStateOf<List<com.ssafy.dib.domain.auction.BidHistoryItem>?>(null) }
    var bidHistoryLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured) }
    var bidHistoryError by remember { mutableStateOf<String?>(null) }
    var bidHistoryRevision by remember { mutableStateOf(0) }
    var bidHistoryCursor by remember { mutableStateOf<String?>(null) }
    var bidHistoryHasNext by remember { mutableStateOf(false) }
    var bidHistoryLoadingMore by remember { mutableStateOf(false) }
    var bidHistoryLoadMoreError by remember { mutableStateOf<String?>(null) }
    var domainNotifications by remember { mutableStateOf<List<DomainNotification>>(emptyList()) }
    var notificationConnectionState by remember { mutableStateOf<RealtimeConnectionState?>(null) }
    var unreadNotificationCount by remember { mutableStateOf(0) }
    var notificationsLoading by remember { mutableStateOf(false) }
    var notificationsError by remember { mutableStateOf<String?>(null) }
    var notificationsCursor by remember { mutableStateOf<String?>(null) }
    var notificationsHasNext by remember { mutableStateOf(false) }
    var notificationsLoadingMore by remember { mutableStateOf(false) }
    var notificationsLoadMoreError by remember { mutableStateOf<String?>(null) }
    var notificationActionId by remember { mutableStateOf<String?>(null) }
    var notificationActionError by remember { mutableStateOf<String?>(null) }
    var notificationsRevision by remember { mutableStateOf(0) }
    var tradeNotificationsEnabled by remember {
        mutableStateOf(notificationPreferences.getBoolean("trade_enabled", true))
    }
    var liveNotificationsEnabled by remember {
        mutableStateOf(notificationPreferences.getBoolean("live_enabled", true))
    }
    var wishlistNotificationsEnabled by remember {
        mutableStateOf(notificationPreferences.getBoolean("wishlist_enabled", false))
    }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    // 띱이 말풍선에 떠 있는 알림. 사용자가 처리하거나 닫을 때까지 남는다
    var assistantNotice by remember { mutableStateOf<AssistantNotice?>(null) }

    // 내가 켠 방송의 id. 화면을 강제로 바꾸는 데 쓰지 않고 우상단 표시에만 쓴다.
    // 라이브 피드 화면 안에도 같은 뜻으로 읽히는 activeLiveBroadcastId 가 따로 있는데
    // 그건 "지금 보고 있는 남의 방송" 이라 반대 개념이다. 섀도잉으로 헷갈리지 않게 이름을 갈라 둔다
    var myLiveBroadcastId by remember { mutableStateOf<String?>(null) }

    suspend fun fetchMyLiveBroadcastId(): String? {
        if (!auth.networkConfig.isRestConfigured) return null
        return when (val result = withContext(Dispatchers.IO) {
            auth.liveRepository.getMine(status = "LIVE", size = 10)
        }) {
            is ApiResult.Success -> result.value.items
                .firstOrNull { it.status.equals("LIVE", ignoreCase = true) }
                ?.liveBroadcastId
                ?.takeIf(String::isNotBlank)
            is ApiResult.Failure -> null
        }
    }

    suspend fun refreshMyLiveBroadcast() {
        myLiveBroadcastId = fetchMyLiveBroadcastId()
    }

    // 방송 중이어도 콘솔로 끌고 오지 않는다. 방송하면서 다른 화면을 보는 건 정상적인 사용이고,
    // 놓치지 않게 하는 건 우상단 LIVE 표시(LiveBroadcastIndicator)가 맡는다
    suspend fun postSignInRoute(): String {
        refreshMyLiveBroadcast()
        return navigationPreferences.getString(LAST_AUTHENTICATED_ROUTE_KEY, null)
            ?.takeIf(::isRestorableAuthenticatedRoute)
            ?: Screen.Home.route
    }

    // 마지막으로 보던 화면으로 복귀시킬 때, 그 화면 하나만 백스택에 넣으면
    // 뒤로가기 버튼이 갈 곳이 없어 아무 반응도 안 한다(navigateUp 이 false 를 돌려준다).
    // 상품 등록처럼 깊은 화면으로 복귀하면 그 화면에 갇히는 셈이라, 홈을 밑에 깔아 준다.
    fun navigateAfterSignIn(destination: String, clearRoute: String) {
        navController.navigate(Screen.Home.route) {
            popUpTo(clearRoute) { inclusive = true }
        }
        if (destination != Screen.Home.route) {
            navController.navigate(destination)
        }
    }

    LaunchedEffect(currentBackStackEntry, signedIn) {
        if (signedIn == true) {
            currentBackStackEntry?.persistedRoute()?.let { route ->
                navigationPreferences.edit().putString(LAST_AUTHENTICATED_ROUTE_KEY, route).apply()
            }
        } else if (signedIn == false) {
            navigationPreferences.edit().remove(LAST_AUTHENTICATED_ROUTE_KEY).apply()
        }
    }

    // 우상단 LIVE 표시의 근거 상태. 로그아웃하면 남아 있으면 안 된다
    LaunchedEffect(signedIn) {
        if (signedIn == true) {
            refreshMyLiveBroadcast()
        } else {
            myLiveBroadcastId = null
            assistantNotice = null
        }
    }

    fun navigateMain(tab: DibMainTab) {
        if (!hasAppAccess && tab in setOf(DibMainTab.Register, DibMainTab.Trades, DibMainTab.My)) {
            navController.navigate(Screen.Login.route)
            return
        }
        if (tab == DibMainTab.Register) {
            showCreateMenu = true
            return
        }
        productSelectionPurpose = null
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

    LaunchedEffect(oauthCallbackUri) {
        val callbackUri = oauthCallbackUri ?: return@LaunchedEffect
        onOAuthCallbackConsumed()
        when (val callback = auth.kakaoOAuthConfig.parseCallback(callbackUri)) {
            is KakaoOAuthCallback.Success -> {
                if (!auth.kakaoOAuthStateStore.consume(callback.state)) {
                    loginError = "카카오 로그인 요청이 만료됐거나 올바르지 않습니다. 다시 시도해주세요."
                    return@LaunchedEffect
                }
                loginLoading = true
                loginError = null
                when (val result = withContext(Dispatchers.IO) {
                    auth.repository.authenticateWithKakao(
                        callback.authorizationCode,
                        auth.kakaoOAuthConfig.redirectUri,
                        auth.deviceId
                    )
                }) {
                    is ApiResult.Success -> when (val value = result.value) {
                        is KakaoAuthenticationResult.LoggedIn -> {
                            signedIn = true
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                        is KakaoAuthenticationResult.SignupRequired -> {
                            kakaoSignupToken = value.signupToken
                            kakaoNickname = value.nickname
                            signupState = SignupUiState()
                            phoneVerificationToken = null
                            navController.navigate(Screen.SignUp.route)
                        }
                    }
                    is ApiResult.Failure -> loginError = when (result.error.code) {
                        "KAKAO_AUTH_FAILED" -> "카카오 인증에 실패했습니다. 다시 시도해주세요."
                        "ACCOUNT_SUSPENDED", "ACCOUNT_BLOCKED" -> result.error.message
                        else -> result.error.message.ifBlank { "카카오 로그인에 실패했습니다." }
                    }
                }
                loginLoading = false
            }
            is KakaoOAuthCallback.Failure -> {
                auth.kakaoOAuthStateStore.consume(callback.state)
                loginError = callback.message
            }
            is KakaoOAuthCallback.Invalid -> {
                auth.kakaoOAuthStateStore.consume(null)
                loginError = callback.message
            }
        }
    }

    fun canOpenNotification(notification: DomainNotification): Boolean =
        notification.resourceType.uppercase() in setOf(
            "LIVE", "LIVE_BROADCAST", "AUCTION", "ORDER", "PAYMENT", "SHIPMENT",
            "DELIVERY", "SETTLEMENT", "TRANSACTION", "PRODUCT"
        ) && notification.resourceId.isNotBlank()

    // 띱이 말풍선은 "지금 당장 뭘 해야 하는" 알림만 띄운다. 나머지는 알림 목록과 뱃지로 충분하다.
    //   OUTBID         — 몇 초 안에 다시 안 지르면 놓친다
    //   AUCTION_WON    — 낙찰. 카드가 결제되고 배송지를 넣어야 한다 (차순위 낙찰 제안도 같은 타입)
    //   REVIEW_REQUEST — 구매확정 직후가 평가를 받을 수 있는 유일한 순간이다. 알림함에 묻히면 아무도 안 쓴다
    // LIVE_STARTED / BOOKMARK_STARTED / SYSTEM 은 알려주면 좋지만 놓쳐도 되는 것들이라 뺐다
    fun deservesAssistant(notification: DomainNotification): Boolean =
        notification.type.uppercase() in setOf("OUTBID", "AUCTION_WON", "REVIEW_REQUEST")

    fun mergeNotifications(first: List<DomainNotification>, second: List<DomainNotification>): List<DomainNotification> =
        (first + second).distinctBy(DomainNotification::eventId).sortedByDescending(DomainNotification::occurredAt).take(100)

    fun openNotification(notification: DomainNotification) {
        when (notification.resourceType.uppercase()) {
            "LIVE", "LIVE_BROADCAST" -> navController.navigate(Screen.Feed.route)
            "AUCTION" -> navController.navigate(Screen.ProductDetail.createRoute(notification.resourceId))
            "PRODUCT" -> navController.navigate(Screen.RegisteredProducts.route)
            "SETTLEMENT" -> navController.navigate(Screen.SettlementDetail.createRoute(notification.resourceId))
            // 주문 알림은 이제 orderId 를 들고 온다. 거래 탭 목록이 아니라 그 주문으로 바로 간다.
            // (평가 요청 알림이 여기로 온다 — 목록에 떨어뜨리면 어느 거래인지 다시 찾아야 한다)
            "ORDER" -> navController.navigate(Screen.Transaction.createRoute("buyer", notification.resourceId))
            "PAYMENT", "SHIPMENT", "DELIVERY", "TRANSACTION" ->
                navigateMain(DibMainTab.Trades)
        }
    }

    fun isNotificationEnabled(notification: DomainNotification): Boolean = when (notification.category) {
        NotificationCategory.Trade -> tradeNotificationsEnabled
        NotificationCategory.Live -> liveNotificationsEnabled
        NotificationCategory.Bookmark -> wishlistNotificationsEnabled
        NotificationCategory.Other -> true
    }

    fun updateBookmark(
        auctionId: String,
        bookmarked: Boolean,
        onResult: ((bookmarked: Boolean, errorMessage: String?) -> Unit)? = null
    ) {
        if (previewMode) {
            remoteAuctions = remoteAuctions?.map { auction ->
                if (auction.id == auctionId) auction.copy(bookmarked = bookmarked) else auction
            }
            onResult?.invoke(bookmarked, null)
            return
        }
        if (signedIn != true) return
        val bookmarkProductId = remoteAuctions?.firstOrNull { it.id == auctionId }?.productId ?: auctionId
        val command = "bookmark:$bookmarkProductId:$bookmarked"
        val idempotencyKey = commandKeys.keyFor(command)
        remoteAuctions = remoteAuctions?.map { auction ->
            if (auction.id == auctionId) auction.copy(bookmarked = bookmarked) else auction
        }
        coroutineScope.launch {
            when (val result = withContext(Dispatchers.IO) {
                auth.auctionRepository.setBookmark(bookmarkProductId, bookmarked, idempotencyKey)
            }) {
                is ApiResult.Success -> {
                    commandKeys.complete(command)
                    remoteAuctions = remoteAuctions?.map { auction ->
                        if (auction.id == auctionId) auction.copy(bookmarked = result.value) else auction
                    }
                    onResult?.invoke(result.value, null)
                }
                is ApiResult.Failure -> {
                    remoteAuctions = remoteAuctions?.map { auction ->
                        if (auction.id == auctionId) auction.copy(bookmarked = !bookmarked) else auction
                    }
                    val message = result.error.message.ifBlank { "찜 상태를 변경하지 못했어요." }
                    auctionsError = message
                    onResult?.invoke(!bookmarked, message)
                    if (result.error.requiresLogin) signedIn = false
                }
            }
        }
    }

    LaunchedEffect(signedIn, auth.networkConfig.isRestConfigured) {
        if (signedIn == true && auth.networkConfig.isRestConfigured) {
            when (val result = withContext(Dispatchers.IO) { auth.memberRepository.getMe() }) {
                is ApiResult.Success -> memberProfile = result.value
                is ApiResult.Failure -> if (result.error.requiresLogin) signedIn = false
            }
        }
    }

    DisposableEffect(signedIn, lifecycleOwner, currentBackStackEntry) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && signedIn == true) {
                // 앱 복귀 시 방송 표시 상태만 갱신하며, 사용자 비활동을 이유로 세션을 종료하지 않는다.
                coroutineScope.launch { refreshMyLiveBroadcast() }
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    LaunchedEffect(signedIn) {
        if (signedIn != true) {
            memberProfile = null
            domainNotifications = emptyList()
            unreadNotificationCount = 0
            notificationsCursor = null
            notificationsHasNext = false
            notificationsError = null
            notificationsLoadMoreError = null
            notificationActionError = null
            purchaseOrders = null
            saleOrders = null
            purchaseOrdersCursor = null
            saleOrdersCursor = null
            purchaseOrdersHasNext = false
            saleOrdersHasNext = false
            purchaseOrdersLoadMoreError = null
            saleOrdersLoadMoreError = null
            ordersLoadingMoreRole = null
            ordersError = null
            bidHistory = null
            bidHistoryCursor = null
            bidHistoryHasNext = false
            bidHistoryLoadMoreError = null
            bidHistoryLoadingMore = false
            bidHistoryError = null
        }
        while (signedIn == true) {
            val current = withContext(Dispatchers.IO) { auth.repository.currentSession() }
            if (current == null) {
                signedIn = false
                break
            }
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

    DisposableEffect(signedIn, auth.networkConfig.isWebSocketConfigured) {
        val connection = if (signedIn == true && auth.networkConfig.isWebSocketConfigured) {
            auth.createDomainNotificationConnection().also { realtime ->
                realtime.start(
                    onNotification = { notification ->
                        coroutineScope.launch {
                            if (notification.resourceType.equals("ORDER", ignoreCase = true)) {
                                ordersRevision++
                            }
                            if (isNotificationEnabled(notification) && domainNotifications.none { it.eventId == notification.eventId }) {
                                domainNotifications = mergeNotifications(listOf(notification), domainNotifications)
                                unreadNotificationCount = if (unreadNotificationCount < Int.MAX_VALUE) {
                                    unreadNotificationCount + 1
                                } else {
                                    Int.MAX_VALUE
                                }
                                // 스낵바로 띄우던 걸 띱이 말풍선으로 옮겼다. 스낵바는 몇 초 뒤 사라져서
                                // 다른 걸 보고 있었으면 놓치면 끝이었다. 말풍선은 처리하거나 닫을 때까지 남는다.
                                // 뒤 알림이 앞 알림을 덮어쓰는데, 목록에 다 남으니 여기선 최신 한 건만 보여도 된다
                                if (deservesAssistant(notification)) {
                                    assistantNotice = AssistantNotice(
                                        id = notification.eventId,
                                        title = notification.title.ifBlank { "새 알림이 도착했어요" },
                                        body = notification.body,
                                        actionable = canOpenNotification(notification)
                                    )
                                }
                            }
                        }
                    },
                    onState = { state -> coroutineScope.launch { notificationConnectionState = state } }
                )
            }
        } else {
            notificationConnectionState = null
            null
        }
        onDispose {
            connection?.close()
            notificationConnectionState = null
        }
    }

    LaunchedEffect(notificationsRevision, signedIn) {
        if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
        notificationsLoading = true
        notificationsError = null
        notificationsLoadMoreError = null
        val result = withContext(Dispatchers.IO) { auth.notificationRepository.getNotifications() }
        val unreadResult = withContext(Dispatchers.IO) { auth.notificationRepository.getUnreadCount() }
        if (unreadResult is ApiResult.Success) unreadNotificationCount = unreadResult.value
        when (result) {
            is ApiResult.Success -> {
                domainNotifications = mergeNotifications(result.value.items, domainNotifications)
                notificationsCursor = result.value.nextCursor
                notificationsHasNext = result.value.hasNext && !result.value.nextCursor.isNullOrBlank()
                if (unreadResult is ApiResult.Failure) unreadNotificationCount = domainNotifications.count { !it.isRead }
            }
            is ApiResult.Failure -> {
                notificationsError = result.error.message.ifBlank { "알림을 불러오지 못했어요." }
                if (result.error.requiresLogin) signedIn = false
            }
        }
        notificationsLoading = false
    }

    LaunchedEffect(auctionsRevision, signedIn, previewMode) {
        if (previewMode || !auth.networkConfig.isRestConfigured) {
            remoteAuctions = null
            remoteHomeLives = null
            auctionsLoading = false
            auctionsError = null
            return@LaunchedEffect
        }
        auctionsLoading = true
        auctionsError = null
        when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getRecommendations() }) {
            is ApiResult.Success -> {
                remoteAuctions = result.value.generalItems.map { it.toHomeAuction() }
                remoteHomeLives = result.value.liveItems
            }
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
        purchaseOrdersLoadMoreError = null
        saleOrdersLoadMoreError = null
        val (buyerResult, sellerResult) = withContext(Dispatchers.IO) {
            auth.orderRepository.getOrders(OrderRole.BUYER) to
                auth.auctionRepository.getMySales()
        }
        when (buyerResult) {
            is ApiResult.Success -> {
                purchaseOrders = buyerResult.value.items
                purchaseOrdersCursor = buyerResult.value.nextCursor
                purchaseOrdersHasNext = buyerResult.value.hasNext && !buyerResult.value.nextCursor.isNullOrBlank()
            }
            is ApiResult.Failure -> ordersError = buyerResult.error.message.ifBlank { "구매 내역을 불러오지 못했어요." }
        }
        when (sellerResult) {
            is ApiResult.Success -> {
                saleOrders = sellerResult.value.items
                saleOrdersCursor = sellerResult.value.nextCursor
                saleOrdersHasNext = sellerResult.value.hasNext && !sellerResult.value.nextCursor.isNullOrBlank()
            }
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
        bidHistoryLoadMoreError = null
        when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getMyBids() }) {
            is ApiResult.Success -> {
                bidHistory = result.value.items
                bidHistoryCursor = result.value.nextCursor
                bidHistoryHasNext = result.value.hasNext && !result.value.nextCursor.isNullOrBlank()
            }
            is ApiResult.Failure -> {
                bidHistoryError = result.error.message.ifBlank { "입찰 내역을 불러오지 못했어요." }
                if (result.error.requiresLogin) signedIn = false
            }
        }
        bidHistoryLoading = false
    }

    Box(Modifier.fillMaxSize()) {
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
                    val destination = if (restored) postSignInRoute() else Screen.Welcome.route
                    signedIn = restored
                    if (restored) {
                        navigateAfterSignIn(destination, Screen.Splash.route)
                    } else {
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            })
        }
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onEmailSignup = { navController.navigate(Screen.SignUp.route) },
                onLogin = { navController.navigate(Screen.Login.route) },
                onBrowse = {
                    previewMode = false
                    signedIn = false
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Welcome.route) { inclusive = true } }
                },
                showDeveloperPreview = BuildConfig.DEBUG,
                onDeveloperPreview = {
                    previewMode = true
                    signedIn = false
                    remoteAuctions = null
                    remoteHomeLives = null
                    auctionsLoading = false
                    auctionsError = null
                    ordersLoading = false
                    ordersError = null
                    bidHistoryLoading = false
                    bidHistoryError = null
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Welcome.route) { inclusive = true } }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onBack = navController::navigateUp,
                onSignUp = { navController.navigate(Screen.SignUp.route) },
                onFindEmail = { navController.navigate(Screen.FindEmail.route) },
                onPasswordReset = { navController.navigate(Screen.PasswordResetLink.route) },
                isLoading = loginLoading,
                errorMessage = loginError,
                onKakaoLogin = {
                    if (!auth.kakaoOAuthConfig.isConfigured) {
                        loginError = "Kakao REST API 키와 Redirect URI 설정을 확인해주세요."
                    } else {
                        loginError = null
                        val state = auth.kakaoOAuthStateStore.create()
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, auth.kakaoOAuthConfig.authorizationUri(state)))
                        }.onFailure {
                            auth.kakaoOAuthStateStore.consume(state)
                            loginError = "카카오 로그인 화면을 열 수 없습니다. 브라우저 설정을 확인해주세요."
                        }
                    }
                },
                onLogin = { email, password ->
                    loginLoading = true
                    loginError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.repository.login(email, password, auth.deviceId)
                        }) {
                            is ApiResult.Success -> {
                                val destination = postSignInRoute()
                                signedIn = true
                                navigateAfterSignIn(destination, Screen.Welcome.route)
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
        composable(Screen.FindEmail.route) {
            var verificationId by remember { mutableStateOf<String?>(null) }
            var findEmailLoading by remember { mutableStateOf(false) }
            var findEmailError by remember { mutableStateOf<String?>(null) }
            var maskedEmail by remember { mutableStateOf<String?>(null) }
            var findEmailPhoneNumber by remember { mutableStateOf<String?>(null) }

            FindEmailScreen(
                verificationRequested = verificationId != null,
                isLoading = findEmailLoading,
                errorMessage = findEmailError,
                maskedEmail = maskedEmail,
                onRequestVerification = { phoneNumber ->
                    findEmailPhoneNumber = phoneNumber
                    findEmailLoading = true
                    findEmailError = null
                    maskedEmail = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.repository.requestFindEmailPhoneVerification(phoneNumber) }) {
                            is ApiResult.Success -> verificationId = result.value.verificationId
                            is ApiResult.Failure -> findEmailError = signupErrorMessage(result.error)
                        }
                        findEmailLoading = false
                    }
                },
                onConfirmVerification = { code ->
                    val challengeId = verificationId ?: return@FindEmailScreen
                    findEmailLoading = true
                    findEmailError = null
                    coroutineScope.launch {
                        when (val confirmation = withContext(Dispatchers.IO) { auth.repository.confirmPhoneVerification(challengeId, code) }) {
                            is ApiResult.Success -> when (val result = withContext(Dispatchers.IO) {
                                auth.repository.findEmail(
                                    confirmation.value.verificationToken,
                                    findEmailPhoneNumber.orEmpty()
                                )
                            }) {
                                is ApiResult.Success -> maskedEmail = result.value
                                is ApiResult.Failure -> findEmailError = when (result.error.code) {
                                    "MEMBER_NOT_FOUND" -> "해당 휴대전화 번호로 가입한 계정을 찾을 수 없어요."
                                    else -> signupErrorMessage(result.error)
                                }
                            }
                            is ApiResult.Failure -> findEmailError = signupErrorMessage(confirmation.error)
                        }
                        findEmailLoading = false
                    }
                },
                onBack = navController::navigateUp,
                onLogin = { navController.popBackStack(Screen.Login.route, inclusive = false) }
            )
        }
        composable(Screen.PasswordResetLink.route) {
            var verificationId by remember { mutableStateOf<String?>(null) }
            var resetLoading by remember { mutableStateOf(false) }
            var resetError by remember { mutableStateOf<String?>(null) }
            var linkSent by remember { mutableStateOf(false) }
            var resetPhoneNumber by remember { mutableStateOf<String?>(null) }

            PasswordResetLinkScreen(
                verificationRequested = verificationId != null,
                isLoading = resetLoading,
                errorMessage = resetError,
                linkSent = linkSent,
                onRequestVerification = { phoneNumber ->
                    resetPhoneNumber = phoneNumber
                    resetLoading = true
                    resetError = null
                    linkSent = false
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.repository.requestPasswordResetPhoneVerification(phoneNumber) }) {
                            is ApiResult.Success -> verificationId = result.value.verificationId
                            is ApiResult.Failure -> resetError = signupErrorMessage(result.error)
                        }
                        resetLoading = false
                    }
                },
                onRequestResetLink = { email, code ->
                    val challengeId = verificationId ?: return@PasswordResetLinkScreen
                    resetLoading = true
                    resetError = null
                    coroutineScope.launch {
                        when (val confirmation = withContext(Dispatchers.IO) { auth.repository.confirmPhoneVerification(challengeId, code) }) {
                            is ApiResult.Success -> when (val result = withContext(Dispatchers.IO) {
                                auth.repository.requestPasswordResetLink(
                                    email,
                                    resetPhoneNumber.orEmpty(),
                                    confirmation.value.verificationToken
                                )
                            }) {
                                is ApiResult.Success -> linkSent = true
                                is ApiResult.Failure -> resetError = signupErrorMessage(result.error)
                            }
                            is ApiResult.Failure -> resetError = signupErrorMessage(confirmation.error)
                        }
                        resetLoading = false
                    }
                },
                onBack = navController::navigateUp,
                onLogin = { navController.popBackStack(Screen.Login.route, inclusive = false) }
            )
        }
        composable(
            route = Screen.PasswordReset.route,
            arguments = listOf(navArgument("resetToken") { type = NavType.StringType })
        ) { backStackEntry ->
            val resetToken = backStackEntry.arguments?.getString("resetToken").orEmpty()
            var resetLoading by remember { mutableStateOf(false) }
            var resetComplete by remember { mutableStateOf(false) }
            var resetError by remember { mutableStateOf<String?>(null) }

            PasswordResetScreen(
                isLoading = resetLoading,
                isComplete = resetComplete,
                errorMessage = resetError,
                onSubmit = { newPassword ->
                    resetLoading = true
                    resetError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.repository.resetPassword(resetToken, newPassword)
                        }) {
                            is ApiResult.Success -> resetComplete = true
                            is ApiResult.Failure -> resetError = when (result.error.code) {
                                "INVALID_RESET_TOKEN" -> "재설정 링크가 만료됐거나 이미 사용됐어요. 링크를 다시 요청해주세요."
                                "INVALID_PASSWORD" -> "비밀번호 조건을 확인해주세요."
                                else -> signupErrorMessage(result.error)
                            }
                        }
                        resetLoading = false
                    }
                },
                onBack = navController::navigateUp,
                onLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.PasswordReset.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.SignUp.route) {
            SignupScreen(
                state = signupState,
                mode = if (kakaoSignupToken == null) SignupMode.EMAIL else SignupMode.KAKAO,
                initialNickname = kakaoNickname.orEmpty(),
                onBack = {
                    kakaoSignupToken = null
                    kakaoNickname = null
                    signupState = SignupUiState()
                    phoneVerificationToken = null
                    navController.navigateUp()
                },
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
                            val socialToken = kakaoSignupToken
                            if (socialToken == null) auth.repository.signUp(
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
                            ) else auth.repository.signUpWithKakao(
                                KakaoSignupCommand(
                                    signupToken = socialToken,
                                    email = form.email,
                                    name = form.name,
                                    nickname = form.nickname,
                                    gender = form.gender,
                                    birthDate = form.birthDate,
                                    phoneNumber = form.phoneNumber,
                                    phoneVerificationToken = token,
                                    deviceId = auth.deviceId
                                )
                            )
                        }) {
                            is ApiResult.Success -> {
                                signupState = SignupUiState()
                                phoneVerificationToken = null
                                kakaoSignupToken = null
                                kakaoNickname = null
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
                isAuthenticated = hasAppAccess,
                remoteAuctions = remoteAuctions,
                remoteLives = remoteHomeLives,
                showSampleContent = previewMode || !auth.networkConfig.isRestConfigured,
                remoteLoading = auctionsLoading,
                remoteError = auctionsError,
                unreadNotificationCount = unreadNotificationCount,
                onRetry = { auctionsRevision++ },
                onBookmarkChange = ::updateBookmark,
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onLiveClick = { navController.navigate(Screen.Feed.route) },
                onSearchClick = {
                    browseAllAuctions = false
                    navController.navigate(Screen.Search.route)
                },
                onViewAllAuctions = {
                    browseAllAuctions = true
                    navController.navigate(Screen.Search.route)
                },
                onNotificationsClick = {
                    if (hasAppAccess) {
                        unreadNotificationCount = 0
                        navController.navigate(Screen.Notifications.route)
                    }
                    else navController.navigate(Screen.Login.route)
                },
                onCategoryClick = { navController.navigate(Screen.Categories.route) },
                onLoginRequired = { navController.navigate(Screen.Login.route) },
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.Categories.route) {
            // null 이면 CategoryScreen 이 기본 카테고리로 그린다 — 응답 전/실패에는 빈 화면 대신 대체 목록이 보여야 한다
            var categoryList by remember { mutableStateOf<List<ProductCategory>?>(null) }
            var categoryAuctions by remember { mutableStateOf<List<HomeAuction>?>(null) }
            var categoryLoading by remember { mutableStateOf(false) }
            var categoryError by remember { mutableStateOf<String?>(null) }
            var selectedCategoryId by remember { mutableStateOf<String?>(null) }
            var categoryCursor by remember { mutableStateOf<String?>(null) }
            var categoryHasNext by remember { mutableStateOf(false) }
            var categoryLoadingMore by remember { mutableStateOf(false) }
            var categoryLoadMoreError by remember { mutableStateOf<String?>(null) }

            fun loadCategory(categoryId: String, cursor: String? = null, append: Boolean = false) {
                selectedCategoryId = categoryId
                if (previewMode || !auth.networkConfig.isRestConfigured) {
                    categoryAuctions = null
                    categoryError = null
                    return
                }
                if (append) categoryLoadingMore = true else categoryLoading = true
                if (append) categoryLoadMoreError = null else categoryError = null
                coroutineScope.launch {
                    when (val result = withContext(Dispatchers.IO) {
                        auth.auctionRepository.getActiveGeneralAuctions(categoryId = categoryId, cursor = cursor)
                    }) {
                        is ApiResult.Success -> {
                            val mapped = result.value.items.map { it.toHomeAuction() }
                            categoryAuctions = if (append) (categoryAuctions.orEmpty() + mapped).distinctBy { it.id } else mapped
                            categoryCursor = result.value.nextCursor
                            categoryHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                        }
                        is ApiResult.Failure -> {
                            if (append && result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                categoryLoadingMore = false
                                loadCategory(categoryId)
                            } else {
                                val message = result.error.message.ifBlank { "경매 목록을 불러오지 못했어요." }
                                if (append) categoryLoadMoreError = message else categoryError = message
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                    }
                    if (append) categoryLoadingMore = false else categoryLoading = false
                }
            }

            // 카테고리는 로그인 없이 열리는 API — 비로그인에서도 불러야 화면이 하드코딩 목록에 머물지 않는다
            LaunchedEffect(signedIn) {
                if (!auth.networkConfig.isRestConfigured) return@LaunchedEffect
                when (val result = withContext(Dispatchers.IO) { auth.productRepository.getCategories() }) {
                    is ApiResult.Success -> categoryList = result.value
                    is ApiResult.Failure -> Unit
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
                hasNext = categoryHasNext,
                isLoadingMore = categoryLoadingMore,
                loadMoreError = categoryLoadMoreError,
                onCategorySelected = { categoryId ->
                    categoryCursor = null
                    categoryHasNext = false
                    categoryLoadMoreError = null
                    loadCategory(categoryId)
                },
                onRetry = { selectedCategoryId?.let { loadCategory(it) } },
                onLoadMore = {
                    val categoryId = selectedCategoryId
                    val cursor = categoryCursor
                    if (categoryId != null && cursor != null && categoryHasNext && !categoryLoadingMore) {
                        loadCategory(categoryId, cursor, append = true)
                    }
                }
            )
        }
        composable(Screen.Search.route) {
            var searchCategories by remember {
                mutableStateOf<List<ProductCategory>?>(if (auth.networkConfig.isRestConfigured && !previewMode) emptyList() else null)
            }
            var searchAuctions by remember { mutableStateOf<List<HomeAuction>?>(null) }
            var searchLoading by remember { mutableStateOf(false) }
            var searchError by remember { mutableStateOf<String?>(null) }
            var lastSearchFilters by remember { mutableStateOf<AuctionSearchFilters?>(null) }
            var searchCursor by remember { mutableStateOf<String?>(null) }
            var searchHasNext by remember { mutableStateOf(false) }
            var searchLoadingMore by remember { mutableStateOf(false) }
            var searchLoadMoreError by remember { mutableStateOf<String?>(null) }
            var matchingProductIds by remember { mutableStateOf<Set<String>?>(null) }
            var matchingProductFilterKey by remember { mutableStateOf<String?>(null) }

            suspend fun loadMatchingProductIds(filters: AuctionSearchFilters): Set<String>? {
                val ids = linkedSetOf<String>()
                val visitedCursors = mutableSetOf<String>()
                var cursor: String? = null
                var cursorRestarted = false
                do {
                    when (val products = withContext(Dispatchers.IO) {
                        // 두 경로(상품 검색 → 경매 조회)가 같은 조건을 봐야 결과가 어긋나지 않는다.
                        // 예전엔 여기서 keyword 만 보내고 카테고리를 앱에서 걸러서, 페이지 밖의 결과가 사라졌다
                        auth.productRepository.searchProducts(
                            query = filters.query,
                            filter = ProductSearchFilter(
                                categoryId = filters.categoryId,
                                minPrice = filters.minPrice,
                                maxPrice = filters.maxPrice,
                                onAuctionOnly = filters.status == "ACTIVE"
                            ),
                            cursor = cursor
                        )
                    }) {
                        is ApiResult.Success -> {
                            ids += products.value.items.map { it.productId }
                            val nextCursor = products.value.nextCursor
                            if (!products.value.hasNext || nextCursor.isNullOrBlank() || !visitedCursors.add(nextCursor)) {
                                cursor = null
                            } else {
                                cursor = nextCursor
                            }
                        }
                        is ApiResult.Failure -> {
                            if (products.error.code == ApiErrorCodes.INVALID_CURSOR && cursor != null && !cursorRestarted) {
                                ids.clear()
                                visitedCursors.clear()
                                cursor = null
                                cursorRestarted = true
                                continue
                            }
                            if (products.error.requiresLogin) signedIn = false
                            return null
                        }
                    }
                } while (cursor != null)
                return ids
            }

            fun search(filters: AuctionSearchFilters, cursor: String? = null, append: Boolean = false) {
                lastSearchFilters = filters
                if (previewMode || !auth.networkConfig.isRestConfigured) {
                    searchAuctions = null
                    searchError = null
                    return
                }
                if (append) searchLoadingMore = true else searchLoading = true
                if (append) searchLoadMoreError = null else searchError = null
                coroutineScope.launch {
                    val productFilterKey = "${filters.query.trim()}|${filters.categoryId.orEmpty()}"
                    val productIds = when {
                        filters.query.isBlank() -> null
                        signedIn != true -> null
                        append && matchingProductFilterKey == productFilterKey -> matchingProductIds
                        else -> loadMatchingProductIds(filters).also {
                            matchingProductIds = it
                            matchingProductFilterKey = productFilterKey
                        }
                    }
                    val result = withContext(Dispatchers.IO) {
                        auth.auctionRepository.getGeneralAuctions(
                            size = 20,
                            categoryId = filters.categoryId,
                            status = filters.status,
                            minPrice = filters.minPrice,
                            maxPrice = filters.maxPrice,
                            sort = filters.sort,
                            cursor = cursor
                        )
                    }
                    when (result) {
                        is ApiResult.Success -> {
                            val filtered = if (filters.query.isBlank()) {
                                result.value.items
                            } else if (productIds != null) {
                                result.value.items.filter { it.productId in productIds }
                            } else {
                                result.value.items.filter { it.title.contains(filters.query, ignoreCase = true) }
                            }
                            val mapped = filtered.map { it.toHomeAuction() }
                            searchAuctions = if (append) (searchAuctions.orEmpty() + mapped).distinctBy { it.id } else mapped
                            val nextCursor = result.value.nextCursor
                            searchCursor = nextCursor
                            searchHasNext = hasUsableNextCursor(result.value.hasNext, nextCursor, cursor)
                        }
                        is ApiResult.Failure -> {
                            if (append && result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                searchLoadingMore = false
                                search(filters)
                            } else {
                                val message = result.error.message.ifBlank { "검색 결과를 불러오지 못했어요." }
                                if (append) searchLoadMoreError = message else searchError = message
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                    }
                    if (append) searchLoadingMore = false else searchLoading = false
                }
            }

            // 카테고리는 로그인 없이 열리는 API — 비로그인에서도 불러야 화면이 하드코딩 목록에 머물지 않는다
            LaunchedEffect(signedIn) {
                if (!auth.networkConfig.isRestConfigured) return@LaunchedEffect
                when (val result = withContext(Dispatchers.IO) { auth.productRepository.getCategories() }) {
                    is ApiResult.Success -> searchCategories = result.value
                    is ApiResult.Failure -> Unit
                }
            }
            AuctionSearchScreen(
                browseOnOpen = browseAllAuctions,
                onBack = navController::navigateUp,
                onProductClick = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                onTabSelected = ::navigateMain,
                remoteCategories = searchCategories,
                remoteAuctions = searchAuctions,
                isLoading = searchLoading,
                errorMessage = searchError,
                hasNext = searchHasNext,
                isLoadingMore = searchLoadingMore,
                loadMoreError = searchLoadMoreError,
                onSearch = { filters ->
                    searchCursor = null
                    searchHasNext = false
                    searchLoadMoreError = null
                    search(filters)
                },
                onRetry = { lastSearchFilters?.let { search(it) } },
                onLoadMore = {
                    val filters = lastSearchFilters
                    val cursor = searchCursor
                    if (filters != null && cursor != null && searchHasNext && !searchLoadingMore) {
                        search(filters, cursor, append = true)
                    }
                }
            )
        }
        composable(Screen.Notifications.route) {
            NotificationCenterScreen(
                notifications = domainNotifications,
                connectionState = notificationConnectionState,
                isLoading = notificationsLoading,
                errorMessage = notificationsError,
                hasNext = notificationsHasNext,
                isLoadingMore = notificationsLoadingMore,
                loadMoreError = notificationsLoadMoreError,
                actionNotificationId = notificationActionId,
                actionError = notificationActionError,
                isNotificationActionable = ::canOpenNotification,
                onNotificationClick = ::openNotification,
                onMarkRead = { notification ->
                    if (!notification.isRead) {
                        domainNotifications = domainNotifications.map { item ->
                            if (item.eventId == notification.eventId) item.copy(isRead = true) else item
                        }
                        unreadNotificationCount = (unreadNotificationCount - 1).coerceAtLeast(0)
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) { auth.notificationRepository.markRead(notification.eventId) }) {
                                is ApiResult.Success -> Unit
                                is ApiResult.Failure -> {
                                    notificationActionError = result.error.message.ifBlank { "알림을 읽음 처리하지 못했어요." }
                                    notificationsRevision++
                                    if (result.error.requiresLogin) signedIn = false
                                }
                            }
                        }
                    }
                },
                onMarkAllRead = {
                    notificationActionId = "read-all"
                    notificationActionError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.notificationRepository.markAllRead() }) {
                            is ApiResult.Success -> {
                                domainNotifications = domainNotifications.map { it.copy(isRead = true) }
                                unreadNotificationCount = 0
                            }
                            is ApiResult.Failure -> {
                                notificationActionError = result.error.message.ifBlank { "알림을 모두 읽음 처리하지 못했어요." }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        notificationActionId = null
                    }
                },
                onAcceptOffer = { notification ->
                    notificationActionId = notification.eventId
                    notificationActionError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.orderRepository.acceptRunnerUpOffer(notification.resourceId)
                        }) {
                            is ApiResult.Success -> {
                                ordersRevision++
                                val paymentFailed = result.value.paymentResult != "PAID"
                                if (paymentFailed) {
                                    notificationSnackbar.showSnackbar("구매 제안을 수락했지만 자동결제에 실패했어요. 거래 상세에서 다시 결제해주세요.")
                                }
                                navController.navigate(Screen.Transaction.createRoute("buyer", result.value.order.orderId))
                            }
                            is ApiResult.Failure -> {
                                notificationActionError = when (result.error.code) {
                                    "OFFER_EXPIRED" -> "구매 제안의 24시간 응답 기한이 지났어요."
                                    "FORBIDDEN" -> "현재 수락할 수 있는 구매 제안이 아니에요."
                                    else -> result.error.message.ifBlank { "구매 제안을 수락하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        notificationActionId = null
                    }
                },
                onRetry = { notificationsRevision++ },
                onLoadMore = {
                    val cursor = notificationsCursor
                    if (cursor != null && notificationsHasNext && !notificationsLoadingMore) {
                        notificationsLoadingMore = true
                        notificationsLoadMoreError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.notificationRepository.getNotifications(cursor)
                            }) {
                                is ApiResult.Success -> {
                                    domainNotifications = mergeNotifications(domainNotifications, result.value.items)
                                    notificationsCursor = result.value.nextCursor
                                    notificationsHasNext = result.value.hasNext && !result.value.nextCursor.isNullOrBlank() && result.value.nextCursor != cursor
                                }
                                is ApiResult.Failure -> {
                                    notificationsLoadMoreError = result.error.message.ifBlank { "다음 알림을 불러오지 못했어요." }
                                    if (result.error.requiresLogin) signedIn = false
                                }
                            }
                            notificationsLoadingMore = false
                        }
                    }
                },
                onSettingsClick = { navController.navigate(Screen.NotificationSettings.route) },
                onBack = navController::navigateUp,
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.Feed.route) {
            var liveFeedItems by remember { mutableStateOf<List<com.ssafy.dib.domain.live.LiveFeedItem>?>(null) }
            var liveFeedLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var liveFeedError by remember { mutableStateOf<String?>(null) }
            var liveFeedRevision by remember { mutableStateOf(0) }
            var liveFeedNextCursor by remember { mutableStateOf<String?>(null) }
            var liveFeedHasNext by remember { mutableStateOf(false) }
            var liveFeedLoadingMore by remember { mutableStateOf(false) }
            var liveFeedLoadMoreError by remember { mutableStateOf<String?>(null) }
            var activeLiveBroadcastId by remember { mutableStateOf<String?>(null) }
            var liveAuctionLists by remember { mutableStateOf<Map<String, List<com.ssafy.dib.domain.auction.AuctionSummary>>>(emptyMap()) }
            var liveDetailLoading by remember { mutableStateOf(false) }
            var liveDetailError by remember { mutableStateOf<String?>(null) }
            var liveComments by remember { mutableStateOf<List<com.ssafy.dib.domain.live.LiveChatMessage>>(emptyList()) }
            var liveChatHasMore by remember { mutableStateOf(false) }
            var liveChatLoadingEarlier by remember { mutableStateOf(false) }
            var liveChatLoadEarlierError by remember { mutableStateOf<String?>(null) }
            var liveChatError by remember { mutableStateOf<String?>(null) }
            var liveChatState by remember { mutableStateOf<RealtimeConnectionState?>(null) }
            var liveChatConnection by remember { mutableStateOf<com.ssafy.dib.data.remote.socket.LiveChatConnection?>(null) }
            var pendingLiveBidCommandId by remember { mutableStateOf<String?>(null) }
            var liveBidFeedback by remember { mutableStateOf<RealtimeBidFeedback?>(null) }
            var liveReportSubmitting by remember { mutableStateOf(false) }
            var liveReportError by remember { mutableStateOf<String?>(null) }
            var liveReportCompleted by remember { mutableStateOf(false) }
            var liveFavoriteError by remember { mutableStateOf<String?>(null) }
            var liveFavoriteUpdatingAuctionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
            LaunchedEffect(liveFeedRevision, signedIn, previewMode) {
                if (previewMode || !auth.networkConfig.isRestConfigured) {
                    liveFeedItems = null
                    liveFeedLoading = false
                    liveFeedError = null
                    return@LaunchedEffect
                }
                liveFeedLoading = true
                liveFeedError = null
                liveFeedLoadMoreError = null
                liveFeedNextCursor = null
                liveFeedHasNext = false
                liveFeedLoadingMore = false
                when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getFeed() }) {
                    is ApiResult.Success -> {
                        liveFeedItems = result.value.items
                        liveFeedNextCursor = result.value.nextCursor
                        liveFeedHasNext = result.value.hasNext && !result.value.nextCursor.isNullOrBlank()
                    }
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
                    liveChatHasMore = false
                    return@LaunchedEffect
                }
                liveChatError = null
                liveChatLoadEarlierError = null
                when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getMessages(liveId) }) {
                    is ApiResult.Success -> {
                        liveComments = result.value.items
                        liveChatHasMore = result.value.hasMore
                    }
                    is ApiResult.Failure -> {
                        liveChatError = result.error.message.ifBlank { "Live 댓글을 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
            }
            LaunchedEffect(activeLiveBroadcastId, liveFeedRevision, previewMode) {
                val liveId = activeLiveBroadcastId ?: return@LaunchedEffect
                if (previewMode || !auth.networkConfig.isRestConfigured) {
                    liveDetailLoading = false
                    liveDetailError = null
                    return@LaunchedEffect
                }
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
            DisposableEffect(activeLiveBroadcastId, signedIn, previewMode, auth.networkConfig.isWebSocketConfigured) {
                val liveId = activeLiveBroadcastId
                val connection = if (!previewMode && liveId != null && auth.networkConfig.isWebSocketConfigured) {
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
                                if (update.eventType == SocketEventTypes.LIVE_ENDED) {
                                    liveFeedItems = liveFeedItems?.filterNot {
                                        it.liveBroadcastId == targetLiveId
                                    }
                                    liveAuctionLists = liveAuctionLists - targetLiveId
                                    if (activeLiveBroadcastId == targetLiveId) {
                                        activeLiveBroadcastId = null
                                        pendingLiveBidCommandId = null
                                        liveBidFeedback = null
                                    }
                                    liveFeedRevision++
                                    return@launch
                                }
                                liveFeedItems = liveFeedItems?.map { item ->
                                    if (item.liveBroadcastId != targetLiveId) return@map item
                                    val current = item.currentAuction
                                    val updatedAuction = when {
                                        current != null && update.auctionId == current.auctionId -> current.copy(
                                            title = update.title ?: current.title,
                                            currentPrice = update.currentPrice ?: current.currentPrice,
                                            bidCount = update.bidCount ?: current.bidCount,
                                            remainingSeconds = update.remainingSeconds ?: current.remainingSeconds,
                                            status = update.status ?: current.status,
                                            imageUrls = update.thumbnailUrl?.takeIf(String::isNotBlank)
                                                ?.let(::listOf)
                                                ?: current.imageUrls,
                                            isHighestBidder = when {
                                                update.isHighestBidder != null -> update.isHighestBidder
                                                update.eventType == SocketEventTypes.HIGHEST_BID_UPDATED &&
                                                    update.currentPrice != null && update.currentPrice != current.currentPrice -> false
                                                else -> current.isHighestBidder
                                            }
                                        )
                                        update.eventType in setOf(SocketEventTypes.LIVE_SNAPSHOT, SocketEventTypes.LIVE_AUCTION_OPENED) && update.auctionId != null ->
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
                                                isHighestBidder = update.isHighestBidder,
                                                imageUrls = listOfNotNull(update.thumbnailUrl)
                                            )
                                        else -> current
                                    }
                                    item.copy(
                                        title = update.liveTitle ?: item.title,
                                        streamUrl = update.streamUrl ?: item.streamUrl,
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
                                                status = update.status ?: auction.status,
                                                isHighestBidder = when {
                                                    update.isHighestBidder != null -> update.isHighestBidder
                                                    update.eventType == SocketEventTypes.HIGHEST_BID_UPDATED &&
                                                        update.currentPrice != null && update.currentPrice != auction.currentPrice -> false
                                                    else -> auction.isHighestBidder
                                                }
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
            fun applyLiveBookmark(auctionId: String, bookmarked: Boolean) {
                liveFeedItems = liveFeedItems?.map { item ->
                    item.copy(
                        currentAuction = item.currentAuction?.let { auction ->
                            if (auction.auctionId == auctionId) auction.copy(bookmarked = bookmarked) else auction
                        }
                    )
                }
                liveAuctionLists = liveAuctionLists.mapValues { (_, auctions) ->
                    auctions.map { auction ->
                        if (auction.auctionId == auctionId) auction.copy(bookmarked = bookmarked) else auction
                    }
                }
            }
            val feedStreamTokenProvider: (suspend (String) -> Result<com.ssafy.dib.domain.live.LiveStreamSession>)? =
                remember(previewMode, auth.networkConfig.isRestConfigured) {
                    if (previewMode || !auth.networkConfig.isRestConfigured) null
                    else { liveId ->
                        when (val result = withContext(Dispatchers.IO) {
                            auth.liveRepository.prepareStream(liveId, java.util.UUID.randomUUID().toString())
                        }) {
                            is ApiResult.Success -> Result.success(result.value)
                            is ApiResult.Failure -> Result.failure(
                                IllegalStateException(result.error.message.ifBlank { "영상 연결 정보를 받지 못했어요." })
                            )
                        }
                    }
                }
            LiveFeedScreen(
                remoteItems = liveFeedItems,
                isLoading = liveFeedLoading,
                errorMessage = liveFeedError,
                onRetry = { liveFeedRevision++ },
                hasNextPage = liveFeedHasNext,
                isLoadingMore = liveFeedLoadingMore,
                loadMoreError = liveFeedLoadMoreError,
                onLoadMore = {
                    val cursor = liveFeedNextCursor
                    if (!liveFeedLoadingMore && liveFeedHasNext && cursor != null) {
                        liveFeedLoadingMore = true
                        liveFeedLoadMoreError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getFeed(cursor) }) {
                                is ApiResult.Success -> {
                                    liveFeedItems = (liveFeedItems.orEmpty() + result.value.items)
                                        .distinctBy { it.liveBroadcastId }
                                    liveFeedNextCursor = result.value.nextCursor
                                    liveFeedHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                                }
                                is ApiResult.Failure -> {
                                    if (result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                        liveFeedRevision++
                                    } else {
                                        liveFeedLoadMoreError = result.error.message.ifBlank { "다음 Live를 불러오지 못했어요." }
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            }
                            liveFeedLoadingMore = false
                        }
                    }
                },
                activeLiveBroadcastId = activeLiveBroadcastId,
                streamTokenProvider = feedStreamTokenProvider,
                liveComments = liveComments,
                chatHasMore = liveChatHasMore,
                chatLoadingEarlier = liveChatLoadingEarlier,
                chatLoadEarlierError = liveChatLoadEarlierError,
                liveAuctionsByBroadcast = liveAuctionLists,
                productListLoading = liveDetailLoading,
                productListError = liveDetailError,
                reportSubmitting = liveReportSubmitting,
                reportError = liveReportError,
                reportCompleted = liveReportCompleted,
                favoriteError = liveFavoriteError,
                favoriteUpdatingAuctionIds = liveFavoriteUpdatingAuctionIds,
                chatError = liveChatError,
                chatConnectionState = liveChatState,
                onLiveVisible = { liveId ->
                    if (activeLiveBroadcastId != liveId) {
                        activeLiveBroadcastId = liveId
                        liveComments = emptyList()
                        liveChatHasMore = false
                        liveChatLoadingEarlier = false
                        liveChatLoadEarlierError = null
                        liveChatError = null
                    }
                },
                onLoadEarlierComments = {
                    val liveId = activeLiveBroadcastId
                    val cursor = liveComments.minByOrNull { it.time }?.liveChattingId
                    if (liveId != null && cursor != null && liveChatHasMore && !liveChatLoadingEarlier) {
                        liveChatLoadingEarlier = true
                        liveChatLoadEarlierError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.liveRepository.getMessages(liveId, cursor)
                            }) {
                                is ApiResult.Success -> {
                                    liveComments = (result.value.items + liveComments)
                                        .distinctBy { it.liveChattingId }
                                    liveChatHasMore = result.value.hasMore
                                }
                                is ApiResult.Failure -> {
                                    liveChatLoadEarlierError = result.error.message.ifBlank {
                                        "이전 댓글을 불러오지 못했어요."
                                    }
                                    if (result.error.requiresLogin) signedIn = false
                                }
                            }
                            liveChatLoadingEarlier = false
                        }
                    }
                },
                onSendComment = { content ->
                    if (previewMode) true else {
                        liveChatConnection?.updateCurrentMemberId(memberProfile?.memberId)
                        liveChatConnection?.send(content) == true
                    }
                },
                isAuthenticated = hasAppAccess,
                currentMemberId = memberProfile?.memberId,
                realtimeBiddingEnabled = previewMode || auth.networkConfig.isWebSocketConfigured,
                realtimeBidFeedback = liveBidFeedback,
                onRealtimeBid = { auctionId, amount ->
                    if (previewMode) {
                        liveBidFeedback = RealtimeBidFeedback(
                            accepted = true,
                            message = "개발 미리보기 입찰이 반영됐어요.",
                            currentPrice = amount,
                            minAllowedAmount = null,
                            errorCode = null,
                            eventKey = "preview:$auctionId:$amount"
                        )
                        true
                    } else liveChatConnection?.placeBid(auctionId, amount)?.let { commandId ->
                        pendingLiveBidCommandId = commandId
                        true
                    } ?: false
                },
                onClose = { navController.navigateUp() },
                onProductClick = { auctionId -> navController.navigate(Screen.ProductDetail.createRoute(auctionId)) },
                onFavoriteChange = { auctionId, bookmarked ->
                    liveFavoriteError = null
                    liveFavoriteUpdatingAuctionIds = liveFavoriteUpdatingAuctionIds + auctionId
                    applyLiveBookmark(auctionId, bookmarked)
                    updateBookmark(auctionId, bookmarked) { resolvedBookmark, errorMessage ->
                        applyLiveBookmark(auctionId, resolvedBookmark)
                        liveFavoriteUpdatingAuctionIds = liveFavoriteUpdatingAuctionIds - auctionId
                        liveFavoriteError = errorMessage
                    }
                },
                onDismissFavoriteError = { liveFavoriteError = null },
                onLoginRequired = { navController.navigate(Screen.Login.route) },
                onReportAuction = { auctionId, content ->
                    if (signedIn != true) {
                        navController.navigate(Screen.Login.route)
                    } else {
                        val command = "auction-report:$auctionId:$content"
                        val idempotencyKey = commandKeys.keyFor(command)
                        liveReportSubmitting = true
                        liveReportError = null
                        liveReportCompleted = false
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.reportRepository.reportAuction(auctionId, content, idempotencyKey)
                            }) {
                                is ApiResult.Success -> {
                                    commandKeys.complete(command)
                                    liveReportCompleted = true
                                }
                                is ApiResult.Failure -> {
                                    liveReportError = reportSubmissionMessage(result.error)
                                    if (result.error.requiresLogin) signedIn = false
                                }
                            }
                            liveReportSubmitting = false
                        }
                    }
                },
                onReportParticipant = { liveBroadcastId, memberId, content ->
                    if (signedIn != true) {
                        navController.navigate(Screen.Login.route)
                    } else if (memberId == memberProfile?.memberId) {
                        liveReportError = "본인은 신고할 수 없어요."
                    } else {
                        val command = "live-report:$liveBroadcastId:$memberId:$content"
                        val idempotencyKey = commandKeys.keyFor(command)
                        liveReportSubmitting = true
                        liveReportError = null
                        liveReportCompleted = false
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.reportRepository.reportLiveParticipant(liveBroadcastId, memberId, content, idempotencyKey)
                            }) {
                                is ApiResult.Success -> {
                                    commandKeys.complete(command)
                                    liveReportCompleted = true
                                }
                                is ApiResult.Failure -> {
                                    liveReportError = reportSubmissionMessage(result.error)
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
                }
            )
        }
        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("auctionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("auctionId").orEmpty()
            var remoteDetail by remember(productId) {
                mutableStateOf(remoteAuctions?.firstOrNull { it.id == productId })
            }
            var remoteProduct by remember(productId) { mutableStateOf<com.ssafy.dib.domain.product.ProductDetail?>(null) }
            var detailLoading by remember(productId) { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var detailError by remember(productId) { mutableStateOf<String?>(null) }
            var detailRevision by remember(productId) { mutableStateOf(0) }
            var realtimeState by remember(productId) { mutableStateOf<RealtimeConnectionState?>(null) }
            var realtimeNotice by remember(productId) { mutableStateOf<String?>(null) }
            var realtimeConnection by remember(productId) { mutableStateOf<AuctionRealtimeConnection?>(null) }
            var pendingBidCommandId by remember(productId) { mutableStateOf<String?>(null) }
            var realtimeBidFeedback by remember(productId) { mutableStateOf<RealtimeBidFeedback?>(null) }
            var wonOrderId by remember(productId) { mutableStateOf<String?>(null) }
            var bookmarkLoading by remember(productId) { mutableStateOf(false) }
            var bookmarkError by remember(productId) { mutableStateOf<String?>(null) }
            var auctionBidHistory by remember(productId) { mutableStateOf<List<com.ssafy.dib.domain.auction.AuctionBidHistoryItem>?>(null) }
            var auctionBidHistoryLoading by remember(productId) { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var auctionBidHistoryError by remember(productId) { mutableStateOf<String?>(null) }
            var auctionBidHistoryCursor by remember(productId) { mutableStateOf<String?>(null) }
            var auctionBidHistoryHasNext by remember(productId) { mutableStateOf(false) }
            var auctionBidHistoryRevision by remember(productId) { mutableStateOf(0) }
            var similarProducts by remember(productId) { mutableStateOf<List<com.ssafy.dib.domain.product.RegisteredProduct>?>(null) }
            var similarProductsLoading by remember(productId) { mutableStateOf(false) }
            var similarProductsError by remember(productId) { mutableStateOf<String?>(null) }
            var similarProductsRevision by remember(productId) { mutableStateOf(0) }
            LaunchedEffect(productId, detailRevision, signedIn, previewMode) {
                if (previewMode || !auth.networkConfig.isRestConfigured) {
                    detailLoading = false
                    detailError = null
                    return@LaunchedEffect
                }
                detailLoading = true
                detailError = null
                when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getAuction(productId) }) {
                    is ApiResult.Success -> {
                        remoteDetail = result.value.toHomeAuction()
                        // 낙찰자 본인이면 서버가 myOrderId 를 준다. 소켓 AUCTION_ENDED 를 못 받고 들어와도 거래 상세로 갈 수 있게 여기서 채운다
                        result.value.myOrderId?.takeIf(String::isNotBlank)?.let { wonOrderId = it }
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
            LaunchedEffect(remoteProduct?.productId, signedIn, similarProductsRevision) {
                val sourceProductId = remoteProduct?.productId ?: return@LaunchedEffect
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                similarProductsLoading = true
                similarProductsError = null
                when (val result = withContext(Dispatchers.IO) { auth.productRepository.getSimilarProducts(sourceProductId) }) {
                    is ApiResult.Success -> similarProducts = result.value.filterNot { it.productId == sourceProductId }
                    is ApiResult.Failure -> {
                        similarProductsError = result.error.message.ifBlank { "비슷한 상품을 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                similarProductsLoading = false
            }
            LaunchedEffect(productId, previewMode, realtimeState == RealtimeConnectionState.Connected) {
                if (previewMode || !auth.networkConfig.isRestConfigured || realtimeState == RealtimeConnectionState.Connected) return@LaunchedEffect
                while (true) {
                    when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getBidSnapshot(productId) }) {
                        is ApiResult.Success -> {
                            val snapshot = result.value
                            val base = remoteDetail ?: remoteAuctions?.firstOrNull { it.id == productId }
                            if (base != null) {
                                remoteDetail = base.copy(
                                    price = snapshot.currentPrice,
                                    bidCount = snapshot.bidCount,
                                    remainingSeconds = snapshot.remainingSeconds,
                                    isHighestBidder = snapshot.isHighestBidder
                                )
                            }
                        }
                        is ApiResult.Failure -> if (remoteDetail == null) {
                            realtimeNotice = result.error.message.ifBlank { "최신 입찰 정보를 불러오지 못했어요." }
                        }
                    }
                    delay(15_000L)
                }
            }
            LaunchedEffect(productId, auctionBidHistoryRevision, previewMode) {
                if (previewMode || !auth.networkConfig.isRestConfigured) {
                    auctionBidHistoryLoading = false
                    return@LaunchedEffect
                }
                auctionBidHistoryLoading = true
                auctionBidHistoryError = null
                when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getBidHistory(productId) }) {
                    is ApiResult.Success -> {
                        auctionBidHistory = result.value.items
                        auctionBidHistoryCursor = result.value.nextCursor
                        auctionBidHistoryHasNext = result.value.hasNext && !result.value.nextCursor.isNullOrBlank()
                    }
                    is ApiResult.Failure -> auctionBidHistoryError = result.error.message.ifBlank { "입찰 이력을 불러오지 못했어요." }
                }
                auctionBidHistoryLoading = false
            }
            DisposableEffect(productId, previewMode, auth.networkConfig.isWebSocketConfigured) {
                val connection = if (!previewMode && auth.networkConfig.isWebSocketConfigured) {
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
                                    if (update.eventType == SocketEventTypes.BID_ACCEPTED) auctionBidHistoryRevision++
                                    if (update.eventType == SocketEventTypes.AUCTION_ENDED) {
                                        wonOrderId = update.orderId?.takeIf(String::isNotBlank)
                                    }
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
                showSampleContent = previewMode || !auth.networkConfig.isRestConfigured,
                remoteLoading = detailLoading,
                remoteError = detailError,
                onRetry = { detailRevision++ },
                bidHistory = auctionBidHistory,
                bidHistoryLoading = auctionBidHistoryLoading,
                bidHistoryError = auctionBidHistoryError,
                bidHistoryHasNext = auctionBidHistoryHasNext,
                onBidHistoryRetry = { auctionBidHistoryRevision++ },
                onBidHistoryLoadMore = {
                    val cursor = auctionBidHistoryCursor
                    if (cursor != null && !auctionBidHistoryLoading && auctionBidHistoryHasNext) {
                        auctionBidHistoryLoading = true
                        auctionBidHistoryError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.auctionRepository.getBidHistory(productId, cursor)
                            }) {
                                is ApiResult.Success -> {
                                    auctionBidHistory = (auctionBidHistory.orEmpty() + result.value.items).distinctBy { it.bidId }
                                    auctionBidHistoryCursor = result.value.nextCursor
                                    auctionBidHistoryHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                                }
                                is ApiResult.Failure -> auctionBidHistoryError = result.error.message.ifBlank { "입찰 이력을 더 불러오지 못했어요." }
                            }
                            auctionBidHistoryLoading = false
                        }
                    }
                },
                bookmarkLoading = bookmarkLoading,
                bookmarkError = bookmarkError,
                onBookmarkChange = { selected ->
                    val command = "bookmark:$productId:$selected"
                    val idempotencyKey = commandKeys.keyFor(command)
                    bookmarkLoading = true
                    bookmarkError = null
                    remoteDetail = remoteDetail?.copy(bookmarked = selected)
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.auctionRepository.setBookmark(remoteDetail?.productId ?: productId, selected, idempotencyKey)
                        }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
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
                realtimeBiddingEnabled = previewMode || auth.networkConfig.isWebSocketConfigured,
                realtimeConnected = previewMode || realtimeState == RealtimeConnectionState.Connected,
                realtimeBidFeedback = realtimeBidFeedback,
                onRealtimeBid = { amount ->
                    if (previewMode) {
                        realtimeBidFeedback = RealtimeBidFeedback(
                            accepted = true,
                            message = "개발 미리보기 입찰이 반영됐어요.",
                            currentPrice = amount,
                            minAllowedAmount = null,
                            errorCode = null,
                            eventKey = "preview:$productId:$amount"
                        )
                        true
                    } else realtimeConnection?.placeBid(amount)?.let { commandId ->
                        pendingBidCommandId = commandId
                        true
                    } ?: false
                },
                isAuthenticated = hasAppAccess,
                isOwnAuction = signedIn == true && memberProfile?.memberId?.let { memberId ->
                    memberId == remoteDetail?.sellerMemberId
                } == true,
                onBack = navController::navigateUp,
                onImageClick = { page ->
                    backStackEntry.savedStateHandle["productImageUrls"] = ArrayList(remoteProduct?.imageUrls?.takeIf { it.isNotEmpty() } ?: remoteDetail?.imageUrls.orEmpty())
                    navController.navigate(
                        Screen.ProductImages.createRoute(
                            backStackEntry.arguments?.getString("auctionId").orEmpty(),
                            page
                        )
                    )
                },
                onSellerClick = { sellerMemberId ->
                    if (sellerMemberId.isNotBlank()) {
                        backStackEntry.savedStateHandle["sellerNickname"] = remoteProduct?.sellerNickname ?: remoteDetail?.sellerNickname
                        backStackEntry.savedStateHandle["sellerRating"] = remoteProduct?.sellerRating ?: remoteDetail?.sellerRating
                        backStackEntry.savedStateHandle["sellerReviewCount"] = remoteProduct?.sellerReviewCount ?: remoteDetail?.sellerReviewCount
                        backStackEntry.savedStateHandle["sellerTradeCount"] = remoteProduct?.sellerTradeCount ?: remoteDetail?.sellerTradeCount
                        navController.navigate(Screen.SellerProfile.createRoute(sellerMemberId))
                    } else {
                        realtimeNotice = "판매자 정보를 확인하지 못했어요."
                    }
                },
                onReportClick = {
                    if (hasAppAccess) {
                        navController.navigate(
                            Screen.ProductReport.createRoute(
                                backStackEntry.arguments?.getString("auctionId").orEmpty()
                            )
                        )
                    } else navController.navigate(Screen.Login.route)
                },
                onTransactionClick = {
                    if (signedIn != true) {
                        navController.navigate(Screen.Login.route)
                    } else {
                        val orderId = wonOrderId
                        if (orderId != null) navController.navigate(Screen.Transaction.createRoute("buyer", orderId))
                        else navigateMain(DibMainTab.Trades)
                    }
                },
                onLoginRequired = { navController.navigate(Screen.Login.route) },
                similarProducts = similarProducts,
                similarProductsLoading = similarProductsLoading,
                similarProductsError = similarProductsError,
                onSimilarProductsRetry = { similarProductsRevision++ },
                onSimilarProductClick = { similarProductId -> navController.navigate(Screen.ProductOverview.createRoute(similarProductId)) }
            )
        }
        composable(Screen.Register.route) {
            var productCategories by remember {
                mutableStateOf(
                    if (auth.networkConfig.isRestConfigured && !previewMode) emptyList() else listOf(
                        ProductCategory("1", "디지털"),
                        ProductCategory("2", "패션"),
                        ProductCategory("3", "라이프")
                    )
                )
            }
            var categoriesLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var categoriesError by remember { mutableStateOf<String?>(null) }
            var categoriesRevision by remember { mutableStateOf(0) }
            var productSubmitLoading by remember { mutableStateOf(false) }
            var productSubmitError by remember { mutableStateOf<String?>(null) }
            var productResult by remember { mutableStateOf<ProductRegistrationResult?>(null) }
            // 검수는 비동기이고 완료 알림이 없어서 사용자가 직접 상태를 다시 조회해야 한다
            var productLatestStatus by remember { mutableStateOf<String?>(null) }
            var productStatusRefreshing by remember { mutableStateOf(false) }

            LaunchedEffect(categoriesRevision, previewMode) {
                if (previewMode || !auth.networkConfig.isRestConfigured) {
                    categoriesLoading = false
                    categoriesError = null
                    return@LaunchedEffect
                }
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
                onSubmit = submitProduct@ { form: ProductRegistrationForm ->
                    if (previewMode) {
                        productResult = ProductRegistrationResult(
                            productId = "PREVIEW-001",
                            status = "REGISTERED",
                            thumbnailUrl = null,
                            createdAt = java.time.Instant.now().toString()
                        )
                        return@submitProduct
                    }
                    val command = listOf(
                        "product-create",
                        form.title,
                        form.description,
                        form.categoryId,
                        form.condition,
                        form.modelName.orEmpty(),
                        form.releaseYear?.toString().orEmpty(),
                        form.images.joinToString { "${it.uri}" }
                    ).joinToString("\u001f")
                    val idempotencyKey = commandKeys.keyFor(command)
                    productSubmitLoading = true
                    productSubmitError = null
                    coroutineScope.launch {
                        val uploads = withContext(Dispatchers.IO) {
                            runCatching {
                                form.images.mapIndexed { index, image ->
                                    val uri = image.uri
                                    ProductImageUpload(
                                        fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "product-$index.jpg",
                                        mediaType = resolveProductImageMediaType(context.contentResolver, uri) ?: "application/octet-stream",
                                        bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                            ?: error("선택한 사진을 읽을 수 없습니다.")
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
                                            modelName = form.modelName,
                                            releaseYear = form.releaseYear,
                                            images = images
                                        ),
                                        idempotencyKey
                                    )
                                }) {
                                    is ApiResult.Success -> {
                                        commandKeys.complete(command)
                                        productResult = result.value
                                    }
                                    is ApiResult.Failure -> {
                                        productSubmitError = productSubmissionMessage(result.error)
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            },
                            onFailure = { productSubmitError = it.message ?: "선택한 사진을 읽지 못했어요." }
                        )
                        productSubmitLoading = false
                    }
                },
                onComplete = {
                    navController.navigate(Screen.RegisteredProducts.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onBack = navController::navigateUp,
                latestStatus = productLatestStatus,
                statusRefreshing = productStatusRefreshing,
                onRefreshStatus = {
                    val productId = productResult?.productId
                    if (!productId.isNullOrBlank() && !productStatusRefreshing) {
                        productStatusRefreshing = true
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) { auth.productRepository.getProduct(productId) }) {
                                is ApiResult.Success -> productLatestStatus = result.value.status
                                is ApiResult.Failure -> if (result.error.requiresLogin) signedIn = false
                            }
                            productStatusRefreshing = false
                        }
                    }
                }
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
                showSampleContent = previewMode || !auth.networkConfig.isRestConfigured,
                remoteLoading = ordersLoading,
                remoteError = ordersError,
                bidsLoading = bidHistoryLoading,
                bidsError = bidHistoryError,
                bidsHasNext = bidHistoryHasNext,
                bidsLoadingMore = bidHistoryLoadingMore,
                bidsLoadMoreError = bidHistoryLoadMoreError,
                purchaseHasNext = purchaseOrdersHasNext,
                saleHasNext = saleOrdersHasNext,
                loadingMoreRole = ordersLoadingMoreRole,
                purchaseLoadMoreError = purchaseOrdersLoadMoreError,
                saleLoadMoreError = saleOrdersLoadMoreError,
                onRetry = { ordersRevision++ },
                onBidsRetry = { bidHistoryRevision++ },
                onLoadMoreBids = {
                    val cursor = bidHistoryCursor
                    if (cursor != null && bidHistoryHasNext && !bidHistoryLoadingMore) {
                        bidHistoryLoadingMore = true
                        bidHistoryLoadMoreError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.auctionRepository.getMyBids(cursor)
                            }) {
                                is ApiResult.Success -> {
                                    bidHistory = (bidHistory.orEmpty() + result.value.items).distinctBy { it.bidId }
                                    bidHistoryCursor = result.value.nextCursor
                                    bidHistoryHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                                }
                                is ApiResult.Failure -> {
                                    if (result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                        bidHistoryRevision++
                                    } else {
                                        bidHistoryLoadMoreError = result.error.message.ifBlank { "다음 입찰 내역을 불러오지 못했어요." }
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            }
                            bidHistoryLoadingMore = false
                        }
                    }
                },
                onLoadMoreOrders = { role ->
                    val cursor = if (role == OrderRole.BUYER) purchaseOrdersCursor else saleOrdersCursor
                    val hasNext = if (role == OrderRole.BUYER) purchaseOrdersHasNext else saleOrdersHasNext
                    if (cursor != null && hasNext && ordersLoadingMoreRole == null) {
                        ordersLoadingMoreRole = role
                        if (role == OrderRole.BUYER) purchaseOrdersLoadMoreError = null else saleOrdersLoadMoreError = null
                        coroutineScope.launch {
                            if (role == OrderRole.BUYER) {
                                when (val result = withContext(Dispatchers.IO) {
                                    auth.orderRepository.getOrders(role, cursor)
                                }) {
                                    is ApiResult.Success -> {
                                        purchaseOrders = (purchaseOrders.orEmpty() + result.value.items).distinctBy { it.orderId }
                                        purchaseOrdersCursor = result.value.nextCursor
                                        purchaseOrdersHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                                    }
                                    is ApiResult.Failure -> {
                                        purchaseOrdersLoadMoreError = result.error.message.ifBlank { "다음 구매 내역을 불러오지 못했어요." }
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            } else {
                                when (val result = withContext(Dispatchers.IO) {
                                    auth.auctionRepository.getMySales(cursor = cursor)
                                }) {
                                    is ApiResult.Success -> {
                                        saleOrders = (saleOrders.orEmpty() + result.value.items).distinctBy { it.auction.auctionId }
                                        saleOrdersCursor = result.value.nextCursor
                                        saleOrdersHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                                    }
                                    is ApiResult.Failure -> {
                                        saleOrdersLoadMoreError = result.error.message.ifBlank { "다음 판매 내역을 불러오지 못했어요." }
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            }
                            ordersLoadingMoreRole = null
                        }
                    }
                }
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
            var showReviewDialog by remember(orderId) { mutableStateOf(false) }
            var reviewSubmitting by remember(orderId) { mutableStateOf(false) }
            var reviewError by remember(orderId) { mutableStateOf<String?>(null) }
            var paymentLoading by remember(orderId) { mutableStateOf(false) }
            var paymentError by remember(orderId) { mutableStateOf<String?>(null) }
            var completedPayment by remember(orderId) { mutableStateOf<com.ssafy.dib.domain.payment.Payment?>(null) }
            var completedPaymentLoading by remember(orderId) { mutableStateOf(false) }
            var completedPaymentError by remember(orderId) { mutableStateOf<String?>(null) }
            var shipment by remember(orderId) { mutableStateOf<com.ssafy.dib.domain.order.OrderShipment?>(null) }
            var shipmentLoading by remember(orderId) { mutableStateOf(false) }
            var shipmentError by remember(orderId) { mutableStateOf<String?>(null) }
            var shippingAddress by remember(orderId) { mutableStateOf<com.ssafy.dib.domain.order.OrderShippingAddress?>(null) }
            var shippingAddressLoading by remember(orderId) { mutableStateOf(false) }
            var shippingAddressError by remember(orderId) { mutableStateOf<String?>(null) }
            var addressSubmitting by remember(orderId) { mutableStateOf(false) }
            var addressSubmitError by remember(orderId) { mutableStateOf<String?>(null) }
            var savedAddresses by remember(orderId) { mutableStateOf<List<com.ssafy.dib.domain.member.MemberAddress>>(emptyList()) }
            var shippingCarriers by remember(orderId) { mutableStateOf<List<com.ssafy.dib.domain.order.ShippingCarrier>?>(null) }
            var shippingCarriersLoading by remember(orderId) { mutableStateOf(false) }
            var shippingCarriersError by remember(orderId) { mutableStateOf<String?>(null) }
            var shippingCarriersRevision by remember(orderId) { mutableStateOf(0) }
            var orderReportSubmitting by remember(orderId) { mutableStateOf(false) }
            var orderReportError by remember(orderId) { mutableStateOf<String?>(null) }
            var orderReportCompleted by remember(orderId) { mutableStateOf(false) }

            LaunchedEffect(orderId, orderDetailRevision, ordersRevision, previewMode) {
                if (previewMode || orderId == "sample") {
                    orderDetailLoading = false
                    return@LaunchedEffect
                }
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
                        completedPayment = null
                        completedPaymentError = null
                        result.value.paymentId?.takeIf(String::isNotBlank)?.let { paymentId ->
                            completedPaymentLoading = true
                            when (val paymentResult = withContext(Dispatchers.IO) { auth.paymentRepository.getPayment(paymentId) }) {
                                is ApiResult.Success -> completedPayment = paymentResult.value
                                is ApiResult.Failure -> {
                                    completedPaymentError = paymentResult.error.message.ifBlank { "결제 상세를 불러오지 못했어요." }
                                    if (paymentResult.error.requiresLogin) signedIn = false
                                }
                            }
                            completedPaymentLoading = false
                        }
                        if (result.value.status.uppercase() in setOf("SHIPPED", "DELIEVERED", "DELIVERED")) {
                            shipmentLoading = true
                            when (val shipmentResult = withContext(Dispatchers.IO) { auth.orderRepository.getShipment(orderId) }) {
                                is ApiResult.Success -> shipment = shipmentResult.value
                                is ApiResult.Failure -> {
                                    shipmentError = shipmentResult.error.message.ifBlank { "배송 정보를 불러오지 못했어요." }
                                    if (shipmentResult.error.requiresLogin) signedIn = false
                                }
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

            LaunchedEffect(orderId, role, remoteOrder?.status, orderDetailRevision) {
                if (orderId == "sample" || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                val sellerCanView = role == "seller" && remoteOrder?.status?.uppercase() !in setOf(null, "PENDING")
                if (role != "buyer" && !sellerCanView) return@LaunchedEffect
                shippingAddressLoading = true
                shippingAddressError = null
                when (val result = withContext(Dispatchers.IO) { auth.orderRepository.getShippingAddress(orderId) }) {
                    is ApiResult.Success -> shippingAddress = result.value
                    is ApiResult.Failure -> {
                        shippingAddressError = when (result.error.code) {
                            "PAYMENT_REQUIRED" -> "결제 완료 후 배송지를 확인할 수 있어요."
                            "FORBIDDEN" -> "배송지를 확인할 권한이 없어요."
                            else -> result.error.message.ifBlank { "배송지를 불러오지 못했어요." }
                        }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                shippingAddressLoading = false
            }

            LaunchedEffect(orderId, role, signedIn) {
                // 배송지 입력 다이얼로그에서 고를 수 있게 마이페이지에 등록된 배송지를 미리 받아둔다
                if (orderId == "sample" || !auth.networkConfig.isRestConfigured || role != "buyer" || signedIn != true) return@LaunchedEffect
                when (val result = withContext(Dispatchers.IO) { auth.addressRepository.getAddresses() }) {
                    is ApiResult.Success -> savedAddresses = result.value
                    is ApiResult.Failure -> if (result.error.requiresLogin) signedIn = false
                }
            }

            LaunchedEffect(orderId, role, remoteOrder?.status, shippingCarriersRevision) {
                if (
                    orderId == "sample" ||
                    !auth.networkConfig.isRestConfigured ||
                    role != "seller" ||
                    remoteOrder?.status?.uppercase() !in setOf("PAID", "PREPARING")
                ) return@LaunchedEffect
                shippingCarriersLoading = true
                shippingCarriersError = null
                when (val result = withContext(Dispatchers.IO) { auth.orderRepository.getShippingCarriers() }) {
                    is ApiResult.Success -> shippingCarriers = result.value
                    is ApiResult.Failure -> {
                        shippingCarriersError = result.error.message.ifBlank { "택배사 목록을 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                shippingCarriersLoading = false
            }

            TransactionScreen(
                role = role,
                remoteOrder = remoteOrder,
                isLoading = orderDetailLoading,
                errorMessage = orderDetailError,
                confirmationLoading = confirmationLoading,
                confirmationError = confirmationError,
                paymentLoading = paymentLoading,
                paymentError = paymentError,
                completedPayment = completedPayment,
                completedPaymentLoading = completedPaymentLoading,
                completedPaymentError = completedPaymentError,
                shipment = shipment,
                shipmentLoading = shipmentLoading,
                shipmentError = shipmentError,
                shippingAddress = shippingAddress,
                shippingAddressLoading = shippingAddressLoading,
                shippingAddressError = shippingAddressError,
                addressSubmitting = addressSubmitting,
                addressSubmitError = addressSubmitError,
                savedAddresses = savedAddresses,
                onSubmitShippingAddress = { input ->
                    addressSubmitting = true
                    addressSubmitError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.orderRepository.updateShippingAddress(orderId, input)
                        }) {
                            is ApiResult.Success -> {
                                shippingAddress = result.value
                                shippingAddressError = null
                            }
                            is ApiResult.Failure -> {
                                addressSubmitError = when (result.error.code) {
                                    "ADDRESS_NOT_EDITABLE" -> "결제 완료 후 발송 전까지만 배송지를 등록할 수 있어요."
                                    "FORBIDDEN" -> "주문한 본인만 배송지를 등록할 수 있어요."
                                    else -> result.error.message.ifBlank { "배송지를 등록하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        addressSubmitting = false
                    }
                },
                shippingCarriers = shippingCarriers,
                shippingCarriersLoading = shippingCarriersLoading,
                shippingCarriersError = shippingCarriersError,
                onRetryPayment = {
                    paymentLoading = true
                    paymentError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.paymentRepository.retryPayment(orderId)
                        }) {
                            is ApiResult.Success -> {
                                completedPayment = result.value
                                orderDetailRevision++
                                ordersRevision++
                            }
                            is ApiResult.Failure -> {
                                paymentError = when (result.error.code) {
                                    "PAYMENT_METHOD_NOT_FOUND" -> "등록된 결제 카드가 없어요. 결제수단을 먼저 등록해주세요."
                                    else -> result.error.message.ifBlank { "재결제를 완료하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        paymentLoading = false
                    }
                },
                onManagePaymentMethod = { navController.navigate(Screen.PaymentMethods.route) },
                onRegisterShipment = { carrier, trackingNumber ->
                    val command = "shipment:$orderId:$carrier:$trackingNumber"
                    val idempotencyKey = commandKeys.keyFor(command)
                    shipmentLoading = true
                    shipmentError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.orderRepository.registerShipment(orderId, carrier, trackingNumber, idempotencyKey)
                        }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                shipment = result.value
                                remoteOrder = remoteOrder?.copy(status = result.value.status)
                                ordersRevision++
                            }
                            is ApiResult.Failure -> {
                                shipmentError = when (result.error.code) {
                                    "ORDER_ON_HOLD" -> ORDER_HOLD_BLOCK_MESSAGE
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
                onShippingCarriersRetry = { shippingCarriersRevision++ },
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
                                    "ORDER_ON_HOLD" -> ORDER_HOLD_BLOCK_MESSAGE
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
                onOpenReview = { showReviewDialog = true },
                onOpenChat = { navController.navigate(Screen.OrderChat.createRoute(orderId)) },
                reportSubmitting = orderReportSubmitting,
                reportError = orderReportError,
                reportCompleted = orderReportCompleted,
                onReportOrder = { content, type ->
                    val command = "order-report:" + orderId + ":" + type + ":" + content
                    val idempotencyKey = commandKeys.keyFor(command)
                    orderReportSubmitting = true
                    orderReportError = null
                    orderReportCompleted = false
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.reportRepository.reportOrder(orderId, content, type, idempotencyKey)
                        }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                orderReportCompleted = true
                                // 접수되면 서버가 주문에 보류를 걸므로 heldAt 을 다시 받아 보류 배지를 띄운다
                                orderDetailRevision++
                                ordersRevision++
                            }
                            is ApiResult.Failure -> {
                                orderReportError = reportSubmissionMessage(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        orderReportSubmitting = false
                    }
                },
                onDismissReport = {
                    orderReportError = null
                    orderReportCompleted = false
                },
                onBack = navController::navigateUp
            )

            if (showReviewDialog) ReviewRatingDialog(
                productTitle = remoteOrder?.title.orEmpty().ifBlank { "거래한 상품" },
                submitting = reviewSubmitting,
                errorMessage = reviewError,
                onDismiss = {
                    showReviewDialog = false
                    reviewError = null
                },
                onSubmit = { rating ->
                    reviewSubmitting = true
                    reviewError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.orderRepository.writeReview(orderId, rating)
                        }) {
                            is ApiResult.Success -> {
                                // 서버를 다시 부르지 않고 화면부터 바꾼다. 응답에 주문 전체가 오지 않는다
                                remoteOrder = remoteOrder?.copy(myRating = rating)
                                showReviewDialog = false
                            }
                            is ApiResult.Failure -> {
                                reviewError = when (result.error.code) {
                                    "REVIEW_ALREADY_WRITTEN" -> "이미 평가한 거래예요."
                                    "REVIEW_NOT_ALLOWED" -> "구매 확정된 거래만 평가할 수 있어요."
                                    "REVIEW_NOT_BUYER" -> "구매자만 평가할 수 있어요."
                                    "REVIEW_RATING_INVALID" -> "별점은 0~5 사이여야 해요."
                                    else -> result.error.message.ifBlank { "평가를 보내지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        reviewSubmitting = false
                    }
                }
            )
        }
        composable(
            route = Screen.OrderChat.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            var chatMessages by remember(orderId) { mutableStateOf<List<com.ssafy.dib.domain.order.OrderMessage>>(emptyList()) }
            var chatLoading by remember(orderId) { mutableStateOf(true) }
            var chatHasMore by remember(orderId) { mutableStateOf(false) }
            var chatLoadingEarlier by remember(orderId) { mutableStateOf(false) }
            var chatLoadEarlierError by remember(orderId) { mutableStateOf<String?>(null) }
            var chatError by remember(orderId) { mutableStateOf<String?>(null) }
            var chatRevision by remember(orderId) { mutableStateOf(0) }
            var currentMemberId by remember(orderId) { mutableStateOf("") }
            var chatWritable by remember(orderId) { mutableStateOf(false) }
            var chatConnectionState by remember(orderId) { mutableStateOf<RealtimeConnectionState?>(null) }
            var chatConnection by remember(orderId) { mutableStateOf<com.ssafy.dib.data.remote.socket.OrderChatConnection?>(null) }
            var chatReportSubmitting by remember(orderId) { mutableStateOf(false) }
            var chatReportError by remember(orderId) { mutableStateOf<String?>(null) }
            var chatReportCompleted by remember(orderId) { mutableStateOf(false) }

            LaunchedEffect(orderId, chatRevision, previewMode) {
                if (previewMode) {
                    chatLoading = false
                    chatError = null
                    return@LaunchedEffect
                }
                if (!auth.networkConfig.isRestConfigured) {
                    chatLoading = false
                    chatError = "개발 서버 주소가 설정되지 않았어요."
                    return@LaunchedEffect
                }
                chatLoading = true
                chatError = null
                val messagesResult = withContext(Dispatchers.IO) { auth.orderRepository.getMessages(orderId) }
                val memberResult = withContext(Dispatchers.IO) { auth.memberRepository.getMe() }
                val orderResult = withContext(Dispatchers.IO) { auth.orderRepository.getOrder(orderId) }
                when (messagesResult) {
                    is ApiResult.Success -> {
                        chatMessages = messagesResult.value.items
                        chatHasMore = messagesResult.value.hasMore
                        chatConnection?.updateLastChattingId(chatMessages.maxByOrNull { it.time }?.chattingId)
                    }
                    is ApiResult.Failure -> chatError = messagesResult.error.message.ifBlank { "채팅 내역을 불러오지 못했어요." }
                }
                if (memberResult is ApiResult.Success) currentMemberId = memberResult.value.memberId
                if (orderResult is ApiResult.Success) {
                    chatWritable = com.ssafy.dib.domain.order.isOrderChatWritable(
                        orderResult.value.status,
                        orderResult.value.chattingReadOnly
                    )
                } else if (orderResult is ApiResult.Failure && chatError == null) {
                    chatError = orderResult.error.message.ifBlank { "거래 상태를 확인하지 못했어요." }
                }
                if (
                    (messagesResult is ApiResult.Failure && messagesResult.error.requiresLogin) ||
                    (memberResult is ApiResult.Failure && memberResult.error.requiresLogin) ||
                    (orderResult is ApiResult.Failure && orderResult.error.requiresLogin)
                ) signedIn = false
                chatLoading = false
            }

            DisposableEffect(orderId, previewMode, auth.networkConfig.isWebSocketConfigured) {
                val connection = if (!previewMode && auth.networkConfig.isWebSocketConfigured) {
                    auth.createOrderChatConnection().also { created ->
                        chatConnection = created
                        created.start(
                            orderId = orderId,
                            lastChattingId = chatMessages.maxByOrNull { it.time }?.chattingId,
                            onMessage = { message -> coroutineScope.launch {
                                chatMessages = (chatMessages + message).distinctBy { it.chattingId }
                            } },
                            onHistoryGap = { coroutineScope.launch { chatRevision++ } },
                            onError = { message -> coroutineScope.launch { chatError = message } },
                            onWritableChanged = { writable -> coroutineScope.launch { chatWritable = writable } },
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
                hasMore = chatHasMore,
                isLoadingEarlier = chatLoadingEarlier,
                loadEarlierError = chatLoadEarlierError,
                errorMessage = chatError,
                connectionState = chatConnectionState,
                canSend = chatWritable && currentMemberId.isNotBlank(),
                reportSubmitting = chatReportSubmitting,
                reportError = chatReportError,
                reportCompleted = chatReportCompleted,
                onRetry = { chatRevision++ },
                onLoadEarlier = {
                    if (!chatLoadingEarlier && chatHasMore) {
                        coroutineScope.launch {
                            val beforeChattingId = chatMessages.minByOrNull { it.time }?.chattingId
                                ?: return@launch
                            chatLoadingEarlier = true
                            chatLoadEarlierError = null
                            when (val result = withContext(Dispatchers.IO) {
                                auth.orderRepository.getMessages(orderId, beforeChattingId)
                            }) {
                                is ApiResult.Success -> {
                                    chatMessages = (result.value.items + chatMessages)
                                        .distinctBy { it.chattingId }
                                    chatHasMore = result.value.hasMore
                                }
                                is ApiResult.Failure -> {
                                    chatLoadEarlierError = result.error.message.ifBlank {
                                        "이전 메시지를 불러오지 못했어요."
                                    }
                                    if (result.error.requiresLogin) signedIn = false
                                }
                            }
                            chatLoadingEarlier = false
                        }
                    }
                },
                onSend = { content ->
                    chatConnection?.updateCurrentMemberId(currentMemberId)
                    chatConnection?.send(content) == true
                },
                onReportParticipant = { memberId, content ->
                    val command = "order-chat-report:$orderId:$memberId:$content"
                    val idempotencyKey = commandKeys.keyFor(command)
                    chatReportSubmitting = true
                    chatReportError = null
                    chatReportCompleted = false
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.reportRepository.reportMember(memberId, content, idempotencyKey)
                        }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                chatReportCompleted = true
                            }
                            is ApiResult.Failure -> {
                                chatReportError = reportSubmissionMessage(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        chatReportSubmitting = false
                    }
                },
                onDismissReport = {
                    chatReportError = null
                    chatReportCompleted = false
                },
                onBack = navController::navigateUp
            )
        }
        composable(Screen.My.route) {
            var myProfileLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var myProfileError by remember { mutableStateOf<String?>(null) }
            var myProfileRevision by remember { mutableStateOf(0) }
            LaunchedEffect(myProfileRevision, signedIn, previewMode) {
                if (previewMode || signedIn != true || !auth.networkConfig.isRestConfigured) {
                    myProfileLoading = false
                    myProfileError = null
                    return@LaunchedEffect
                }
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
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onInquiriesClick = { navController.navigate(Screen.Inquiries.route) },
                onAddressesClick = { navController.navigate(Screen.Addresses.route) },
                onPaymentMethodsClick = { navController.navigate(Screen.PaymentMethods.route) },
                onAccountsClick = { navController.navigate(Screen.SettlementAccounts.route) },
                onSettlementsClick = { navController.navigate(Screen.Settlements.route) },
                onNotificationSettingsClick = { navController.navigate(Screen.NotificationSettings.route) },
                onReportsClick = { navController.navigate(Screen.ReportHistory.route) },
                onWithdrawalClick = { navController.navigate(Screen.Withdrawal.route) },
                onLogout = {
                    previewMode = false
                    signedIn = false
                    coroutineScope.launch(Dispatchers.IO) { auth.repository.logout(auth.deviceId) }
                    navController.navigate(Screen.Welcome.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                }
            )
        }
        composable(Screen.PaymentMethods.route) {
            var paymentMethod by remember { mutableStateOf<com.ssafy.dib.domain.payment.PaymentMethod?>(null) }
            var paymentMethodLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var paymentMethodError by remember { mutableStateOf<String?>(null) }
            var paymentMethodActionLoading by remember { mutableStateOf(false) }
            var paymentMethodActionMessage by remember { mutableStateOf<String?>(null) }
            var paymentMethodActionError by remember { mutableStateOf<String?>(null) }
            var paymentMethodRevision by remember { mutableStateOf(0) }

            LaunchedEffect(paymentMethodRevision, signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) {
                    paymentMethodLoading = false
                    if (!auth.networkConfig.isRestConfigured) paymentMethodError = "개발 서버 주소가 설정되지 않았어요."
                    return@LaunchedEffect
                }
                paymentMethodLoading = true
                paymentMethodError = null
                when (val result = withContext(Dispatchers.IO) { auth.paymentRepository.getPaymentMethod() }) {
                    is ApiResult.Success -> paymentMethod = result.value
                    is ApiResult.Failure -> {
                        if (result.error.code == "PAYMENT_METHOD_NOT_FOUND" || result.error.status == 404) {
                            paymentMethod = null
                        } else {
                            paymentMethodError = result.error.message.ifBlank { "결제수단을 불러오지 못했어요." }
                        }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                paymentMethodLoading = false
            }

            PaymentMethodsScreen(
                paymentMethod = paymentMethod,
                isLoading = paymentMethodLoading,
                errorMessage = paymentMethodError,
                actionLoading = paymentMethodActionLoading,
                actionMessage = paymentMethodActionMessage,
                actionError = paymentMethodActionError,
                tossClientKey = com.ssafy.dib.BuildConfig.TOSS_CLIENT_KEY,
                customerKey = auth.paymentCustomerKey(),
                onRetry = { paymentMethodRevision++ },
                onRegister = { authKey, customerKey ->
                    paymentMethodActionLoading = true
                    paymentMethodActionMessage = null
                    paymentMethodActionError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.paymentRepository.registerPaymentMethod(authKey, customerKey)
                        }) {
                            is ApiResult.Success -> {
                                paymentMethod = result.value
                                paymentMethodActionMessage = "자동결제 카드가 등록됐어요."
                            }
                            is ApiResult.Failure -> {
                                paymentMethodActionError = paymentMethodRegistrationErrorMessage(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        paymentMethodActionLoading = false
                    }
                },
                onDelete = {
                    paymentMethodActionLoading = true
                    paymentMethodActionMessage = null
                    paymentMethodActionError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.paymentRepository.deletePaymentMethod() }) {
                            is ApiResult.Success -> {
                                paymentMethod = null
                                paymentMethodActionMessage = "등록 카드가 삭제됐어요."
                            }
                            is ApiResult.Failure -> {
                                paymentMethodActionError = result.error.message.ifBlank { "등록 카드를 삭제하지 못했어요." }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        paymentMethodActionLoading = false
                    }
                },
                onBack = navController::navigateUp,
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.MyAuctions.route) {
            var sales by remember { mutableStateOf<List<com.ssafy.dib.domain.auction.SaleHistoryItem>?>(null) }
            var salesLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var salesError by remember { mutableStateOf<String?>(null) }
            var salesRevision by remember { mutableStateOf(0) }
            var salesStatus by remember { mutableStateOf<String?>(null) }
            var salesCursor by remember { mutableStateOf<String?>(null) }
            var salesHasNext by remember { mutableStateOf(false) }
            var salesLoadingMore by remember { mutableStateOf(false) }
            var salesLoadMoreError by remember { mutableStateOf<String?>(null) }
            var actionAuctionId by remember { mutableStateOf<String?>(null) }
            var actionMessage by remember { mutableStateOf<String?>(null) }
            var actionError by remember { mutableStateOf<String?>(null) }
            val startKeys = remember { mutableMapOf<String, String>() }
            val cancelKeys = remember { mutableMapOf<String, String>() }
            val relistKeys = remember { mutableMapOf<String, String>() }
            var relistedAuctionId by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(salesRevision, salesStatus, signedIn, previewMode) {
                if (previewMode || !auth.networkConfig.isRestConfigured) {
                    sales = emptyList()
                    salesLoading = false
                    salesError = null
                    return@LaunchedEffect
                }
                if (signedIn != true) {
                    salesLoading = false
                    salesError = "로그인 후 내 경매를 확인할 수 있어요."
                    return@LaunchedEffect
                }
                salesLoading = true
                salesError = null
                salesLoadMoreError = null
                salesCursor = null
                salesHasNext = false
                salesLoadingMore = false
                when (val result = withContext(Dispatchers.IO) {
                    auth.auctionRepository.getMySales(auctionStatus = salesStatus)
                }) {
                    is ApiResult.Success -> {
                        sales = result.value.items
                        salesCursor = result.value.nextCursor
                        salesHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, null)
                    }
                    is ApiResult.Failure -> {
                        salesError = result.error.message.ifBlank { "내 경매를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                salesLoading = false
            }

            MyAuctionManagementScreen(
                sales = sales,
                isLoading = salesLoading,
                errorMessage = salesError,
                hasNext = salesHasNext,
                isLoadingMore = salesLoadingMore,
                loadMoreError = salesLoadMoreError,
                selectedStatus = salesStatus,
                actionAuctionId = actionAuctionId,
                actionMessage = actionMessage,
                actionError = actionError,
                onRetry = { salesRevision++ },
                onStatusSelected = { status ->
                    if (status != salesStatus) {
                        salesStatus = status
                        actionMessage = null
                        actionError = null
                    }
                },
                onLoadMore = {
                    val cursor = salesCursor
                    val requestedStatus = salesStatus
                    if (cursor != null && salesHasNext && !salesLoadingMore) {
                        salesLoadingMore = true
                        salesLoadMoreError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.auctionRepository.getMySales(auctionStatus = requestedStatus, cursor = cursor)
                            }) {
                                is ApiResult.Success -> {
                                    if (salesStatus == requestedStatus) {
                                        sales = (sales.orEmpty() + result.value.items)
                                            .distinctBy { it.auction.auctionId }
                                        salesCursor = result.value.nextCursor
                                        salesHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                                    }
                                }
                                is ApiResult.Failure -> {
                                    if (salesStatus != requestedStatus) {
                                        Unit
                                    } else if (result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                        salesRevision++
                                    } else {
                                        salesLoadMoreError = result.error.message.ifBlank { "다음 경매를 불러오지 못했어요." }
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            }
                            salesLoadingMore = false
                        }
                    }
                },
                onOpenAuction = { auctionId -> navController.navigate(Screen.ProductDetail.createRoute(auctionId)) },
                onUpdate = { auctionId, startPrice, auctionTime ->
                    actionAuctionId = auctionId
                    actionMessage = null
                    actionError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.auctionRepository.updateAuction(auctionId, startPrice, auctionTime)
                        }) {
                            is ApiResult.Success -> {
                                actionMessage = result.value.message.ifBlank { "경매 조건을 수정했어요." }
                                salesRevision++
                                auctionsRevision++
                            }
                            is ApiResult.Failure -> {
                                actionError = auctionCommandError(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        actionAuctionId = null
                    }
                },
                onStart = { auctionId, startPrice, auctionTime ->
                    actionAuctionId = auctionId
                    actionMessage = null
                    actionError = null
                    // 시작가/경매 시간이 바뀌면 같은 멱등키를 재사용하면 안 된다
                    val key = startKeys.getOrPut("$auctionId:$startPrice:$auctionTime") { java.util.UUID.randomUUID().toString() }
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.auctionRepository.startAuction(auctionId, key, startPrice, auctionTime)
                        }) {
                            is ApiResult.Success -> {
                                startKeys.remove("$auctionId:$startPrice:$auctionTime")
                                actionMessage = result.value.ifBlank { "경매를 시작했어요." }
                                salesRevision++
                                auctionsRevision++
                            }
                            is ApiResult.Failure -> {
                                actionError = auctionCommandError(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        actionAuctionId = null
                    }
                },
                onCancel = { auctionId ->
                    actionAuctionId = auctionId
                    actionMessage = null
                    actionError = null
                    val key = cancelKeys.getOrPut(auctionId) { java.util.UUID.randomUUID().toString() }
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.auctionRepository.cancelAuction(auctionId, key)
                        }) {
                            is ApiResult.Success -> {
                                cancelKeys.remove(auctionId)
                                actionMessage = "경매를 취소했어요."
                                salesRevision++
                                auctionsRevision++
                            }
                            is ApiResult.Failure -> {
                                actionError = auctionCommandError(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        actionAuctionId = null
                    }
                },
                onRelist = { auctionId ->
                    actionAuctionId = auctionId
                    actionMessage = null
                    actionError = null
                    val key = relistKeys.getOrPut(auctionId) { java.util.UUID.randomUUID().toString() }
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.auctionRepository.relistAuction(auctionId, key)
                        }) {
                            is ApiResult.Success -> {
                                relistKeys.remove(auctionId)
                                actionMessage = result.value.message.ifBlank { "경매를 다시 올렸어요. 시작가와 경매 시간을 정해주세요." }
                                relistedAuctionId = result.value.auctionId.ifBlank { auctionId }
                                salesRevision++
                                auctionsRevision++
                            }
                            is ApiResult.Failure -> {
                                actionError = auctionCommandError(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        actionAuctionId = null
                    }
                },
                relistedAuctionId = relistedAuctionId,
                onRelistConsumed = { relistedAuctionId = null },
                onBack = navController::navigateUp,
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.LiveManagement.route) {
            val previewLiveAuctions = remember { previewLiveAuctions() }
            var broadcasts by remember(previewMode) {
                mutableStateOf<List<com.ssafy.dib.domain.live.LiveBroadcastSummary>?>(
                    if (previewMode) listOf(previewLiveBroadcast()) else null
                )
            }
            var assignedAuctions by remember(previewMode) {
                mutableStateOf<Map<String, List<com.ssafy.dib.domain.auction.AuctionSummary>>>(
                    if (previewMode) mapOf("preview-live" to previewLiveAuctions.take(1)) else emptyMap()
                )
            }
            var availableLiveAuctions by remember(previewMode) {
                mutableStateOf<List<com.ssafy.dib.domain.auction.AuctionSummary>>(
                    if (previewMode) previewLiveAuctions else emptyList()
                )
            }
            var availableLiveAuctionsCursor by remember { mutableStateOf<String?>(null) }
            var availableLiveAuctionsHasNext by remember { mutableStateOf(false) }
            var availableLiveAuctionsLoadingMore by remember { mutableStateOf(false) }
            var availableLiveAuctionsLoadMoreError by remember { mutableStateOf<String?>(null) }
            var liveManagementLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var liveManagementError by remember { mutableStateOf<String?>(null) }
            var liveManagementRevision by remember { mutableStateOf(0) }
            var liveManagementCursor by remember { mutableStateOf<String?>(null) }
            var liveManagementHasNext by remember { mutableStateOf(false) }
            var liveManagementLoadingMore by remember { mutableStateOf(false) }
            var liveManagementLoadMoreError by remember { mutableStateOf<String?>(null) }
            var liveActionLoading by remember { mutableStateOf(false) }
            var liveActionError by remember { mutableStateOf<String?>(null) }
            var liveActionMessage by remember { mutableStateOf<String?>(null) }
            var liveActionRevision by remember { mutableStateOf(0) }

            LaunchedEffect(liveManagementRevision, signedIn, previewMode) {
                if (previewMode) {
                    liveManagementLoading = false
                    liveManagementError = null
                    return@LaunchedEffect
                }
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                liveManagementLoading = true
                liveManagementError = null
                liveManagementLoadMoreError = null
                liveManagementCursor = null
                liveManagementHasNext = false
                liveManagementLoadingMore = false
                availableLiveAuctionsCursor = null
                availableLiveAuctionsHasNext = false
                availableLiveAuctionsLoadingMore = false
                availableLiveAuctionsLoadMoreError = null
                val profile = memberProfile ?: when (val result = withContext(Dispatchers.IO) { auth.memberRepository.getMe() }) {
                    is ApiResult.Success -> result.value.also { memberProfile = it }
                    is ApiResult.Failure -> {
                        if (result.error.requiresLogin) signedIn = false
                        null
                    }
                }
                when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getMine() }) {
                    is ApiResult.Success -> {
                        broadcasts = result.value.items
                        liveManagementCursor = result.value.nextCursor
                        liveManagementHasNext = result.value.hasNext && !result.value.nextCursor.isNullOrBlank()
                        val loadedAssignments = mutableMapOf<String, List<com.ssafy.dib.domain.auction.AuctionSummary>>()
                        result.value.items.filter { it.status == "SCHEDULED" || it.status == "LIVE" }.forEach { live ->
                            when (val detail = withContext(Dispatchers.IO) { auth.liveRepository.getDetail(live.liveBroadcastId) }) {
                                is ApiResult.Success -> loadedAssignments[live.liveBroadcastId] = detail.value.auctions
                                is ApiResult.Failure -> if (detail.error.requiresLogin) signedIn = false
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
                        result.value.items.filter { auction -> auction.sellerMemberId == memberId }
                    }.orEmpty().also {
                        availableLiveAuctionsCursor = result.value.nextCursor
                        availableLiveAuctionsHasNext = result.value.hasNext && !result.value.nextCursor.isNullOrBlank()
                    }
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
                hasNext = liveManagementHasNext,
                isLoadingMore = liveManagementLoadingMore,
                loadMoreError = liveManagementLoadMoreError,
                availableAuctionsHasNext = availableLiveAuctionsHasNext,
                availableAuctionsLoadingMore = availableLiveAuctionsLoadingMore,
                availableAuctionsLoadMoreError = availableLiveAuctionsLoadMoreError,
                onRetry = { liveManagementRevision++ },
                onLoadMore = {
                    val cursor = liveManagementCursor
                    if (cursor != null && liveManagementHasNext && !liveManagementLoadingMore) {
                        liveManagementLoadingMore = true
                        liveManagementLoadMoreError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.liveRepository.getMine(cursor = cursor)
                            }) {
                                is ApiResult.Success -> {
                                    val nextItems = result.value.items
                                    broadcasts = (broadcasts.orEmpty() + nextItems)
                                        .distinctBy { it.liveBroadcastId }
                                    val loadedAssignments = assignedAuctions.toMutableMap()
                                    nextItems.filter { it.status == "SCHEDULED" || it.status == "LIVE" }.forEach { live ->
                                        when (val detail = withContext(Dispatchers.IO) {
                                            auth.liveRepository.getDetail(live.liveBroadcastId)
                                        }) {
                                            is ApiResult.Success -> loadedAssignments[live.liveBroadcastId] = detail.value.auctions
                                            is ApiResult.Failure -> if (detail.error.requiresLogin) signedIn = false
                                        }
                                    }
                                    assignedAuctions = loadedAssignments
                                    liveManagementCursor = result.value.nextCursor
                                    liveManagementHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                                }
                                is ApiResult.Failure -> {
                                    if (result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                        liveManagementRevision++
                                    } else {
                                        liveManagementLoadMoreError = result.error.message.ifBlank { "다음 방송을 불러오지 못했어요." }
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            }
                            liveManagementLoadingMore = false
                        }
                    }
                },
                onLoadMoreAvailableAuctions = {
                    val cursor = availableLiveAuctionsCursor
                    if (cursor != null && availableLiveAuctionsHasNext && !availableLiveAuctionsLoadingMore) {
                        availableLiveAuctionsLoadingMore = true
                        availableLiveAuctionsLoadMoreError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.auctionRepository.getAuctions("GENERAL", "SCHEDULED", cursor = cursor)
                            }) {
                                is ApiResult.Success -> {
                                    val memberId = memberProfile?.memberId
                                    val nextItems = result.value.items.filter { it.sellerMemberId == memberId }
                                    availableLiveAuctions = (availableLiveAuctions + nextItems)
                                        .distinctBy { it.auctionId }
                                    availableLiveAuctionsCursor = result.value.nextCursor
                                    availableLiveAuctionsHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                                }
                                is ApiResult.Failure -> {
                                    if (result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                        liveManagementRevision++
                                    } else {
                                        availableLiveAuctionsLoadMoreError = result.error.message.ifBlank { "다음 예약 경매를 불러오지 못했어요." }
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            }
                            availableLiveAuctionsLoadingMore = false
                        }
                    }
                },
                onCreate = createLive@ { title, description, scheduledAt, streamUrl ->
                    if (previewMode) {
                        val liveId = "preview-live-${broadcasts.orEmpty().size + 1}"
                        broadcasts = broadcasts.orEmpty() + com.ssafy.dib.domain.live.LiveBroadcastSummary(
                            liveBroadcastId = liveId,
                            title = title,
                            description = description,
                            status = "SCHEDULED",
                            streamUrl = streamUrl ?: "preview://stream",
                            scheduledAt = scheduledAt,
                            viewCount = 0
                        )
                        assignedAuctions = assignedAuctions + (liveId to emptyList<com.ssafy.dib.domain.auction.AuctionSummary>())
                        liveActionMessage = "개발 미리보기 방송을 예약했어요."
                        liveActionRevision++
                        return@createLive
                    }
                    val command = listOf("live-create", title, description.orEmpty(), scheduledAt, streamUrl.orEmpty())
                        .joinToString("\u001f")
                    val idempotencyKey = commandKeys.keyFor(command)
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.create(title, description, scheduledAt, streamUrl, idempotencyKey) }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                liveActionRevision++
                                liveManagementRevision++
                            }
                            is ApiResult.Failure -> {
                                liveActionError = result.error.message.ifBlank { "Live 방송을 예약하지 못했어요." }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        liveActionLoading = false
                    }
                },
                onUpdate = updateLive@ { liveId, title, description, scheduledAt, streamUrl ->
                    if (previewMode) {
                        broadcasts = broadcasts?.map { live ->
                            if (live.liveBroadcastId == liveId) live.copy(title = title, description = description, scheduledAt = scheduledAt, streamUrl = streamUrl ?: live.streamUrl) else live
                        }
                        liveActionMessage = "개발 미리보기 방송 정보를 수정했어요."
                        liveActionRevision++
                        return@updateLive
                    }
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
                onSetItems = setLiveItems@ { liveId, items ->
                    if (previewMode) {
                        val auctionIds = items.map { it.auctionId }
                        assignedAuctions = assignedAuctions + (liveId to previewLiveAuctions.filter { it.auctionId in auctionIds })
                        liveActionMessage = "개발 미리보기 상품 편성을 저장했어요."
                        liveActionRevision++
                        return@setLiveItems
                    }
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.setItems(liveId, items) }) {
                            is ApiResult.Success -> {
                                assignedAuctions = assignedAuctions + (liveId to result.value)
                                liveActionMessage = "Live 상품 편성을 저장했어요."
                                liveActionRevision++
                                liveManagementRevision++
                            }
                            is ApiResult.Failure -> {
                                liveActionError = liveControlError(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        liveActionLoading = false
                    }
                },
                onPrepareStream = prepareLive@ { liveId ->
                    if (previewMode) {
                        broadcasts = broadcasts?.map { live -> if (live.liveBroadcastId == liveId) live.copy(streamUrl = "preview://stream/$liveId") else live }
                        liveActionMessage = "개발 미리보기 송출 준비가 완료됐어요."
                        return@prepareLive
                    }
                    val command = "live-prepare-stream:$liveId"
                    val idempotencyKey = commandKeys.keyFor(command)
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.prepareStream(liveId, idempotencyKey) }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                broadcasts = broadcasts?.map { live -> if (live.liveBroadcastId == liveId) live.copy(streamUrl = result.value.roomName) else live }
                                liveActionMessage = "송출 연결 정보를 준비했어요."
                                liveManagementRevision++
                            }
                            is ApiResult.Failure -> { liveActionError = liveControlError(result.error); if (result.error.requiresLogin) signedIn = false }
                        }
                        liveActionLoading = false
                    }
                },
                onStartLive = startLive@ { liveId ->
                    if (previewMode) {
                        broadcasts = broadcasts?.map { live -> if (live.liveBroadcastId == liveId) live.copy(status = "LIVE", viewCount = 128) else live }
                        liveActionMessage = "개발 미리보기 Live를 시작했어요."
                        return@startLive
                    }
                    val command = "live-start:$liveId"
                    val idempotencyKey = commandKeys.keyFor(command)
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.start(liveId, idempotencyKey) }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                liveActionMessage = "Live 방송을 시작했어요."
                                liveManagementRevision++
                                myLiveBroadcastId = liveId   // 우상단 LIVE 표시 켜기
                                navController.navigate(Screen.LiveBroadcastConsole.createRoute(liveId))
                            }
                            is ApiResult.Failure -> { liveActionError = liveControlError(result.error); if (result.error.requiresLogin) signedIn = false }
                        }
                        liveActionLoading = false
                    }
                },
                onStartAuction = startLiveAuction@ { liveId, auctionId ->
                    if (previewMode) {
                        assignedAuctions = assignedAuctions + (liveId to assignedAuctions[liveId].orEmpty().map { auction ->
                            if (auction.auctionId == auctionId) auction.copy(status = "ACTIVE", remainingSeconds = 300) else auction
                        })
                        liveActionMessage = "개발 미리보기 상품 경매를 시작했어요."
                        return@startLiveAuction
                    }
                    val command = "live-start-auction:$liveId:$auctionId"
                    val idempotencyKey = commandKeys.keyFor(command)
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.startAuction(liveId, auctionId, idempotencyKey) }) {
                            is ApiResult.Success -> { commandKeys.complete(command); liveActionMessage = "Live 상품 경매를 시작했어요."; liveManagementRevision++ }
                            is ApiResult.Failure -> { liveActionError = liveControlError(result.error); if (result.error.requiresLogin) signedIn = false }
                        }
                        liveActionLoading = false
                    }
                },
                onEndLive = endLive@ { liveId ->
                    if (previewMode) {
                        broadcasts = broadcasts?.map { live -> if (live.liveBroadcastId == liveId) live.copy(status = "ENDED") else live }
                        liveActionMessage = "개발 미리보기 Live를 종료했어요."
                        return@endLive
                    }
                    val command = "live-end:$liveId"
                    val idempotencyKey = commandKeys.keyFor(command)
                    liveActionLoading = true
                    liveActionError = null
                    liveActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.end(liveId, idempotencyKey) }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                liveActionMessage = "Live 방송을 종료했어요."
                                liveManagementRevision++
                                if (myLiveBroadcastId == liveId) myLiveBroadcastId = null   // 우상단 LIVE 표시 끄기
                            }
                            is ApiResult.Failure -> { liveActionError = liveControlError(result.error); if (result.error.requiresLogin) signedIn = false }
                        }
                        liveActionLoading = false
                    }
                },
                onBack = navController::navigateUp,
                onOpenConsole = { liveId -> navController.navigate(Screen.LiveBroadcastConsole.createRoute(liveId)) }
            )
        }
        composable(
            route = Screen.LiveBroadcastConsole.route,
            arguments = listOf(navArgument("liveBroadcastId") { type = NavType.StringType })
        ) { backStackEntry ->
            val liveId = backStackEntry.arguments?.getString("liveBroadcastId").orEmpty()
            var consoleTitle by remember(liveId) { mutableStateOf("Live 방송") }
            var consoleStatus by remember(liveId) { mutableStateOf("LIVE") }
            var consoleRoomName by remember(liveId) { mutableStateOf<String?>(null) }
            var consoleViewerCount by remember(liveId) { mutableStateOf(0) }
            var consoleAuctions by remember(liveId) { mutableStateOf<List<com.ssafy.dib.domain.auction.AuctionSummary>>(emptyList()) }
            var consoleMessages by remember(liveId) { mutableStateOf<List<com.ssafy.dib.domain.live.LiveChatMessage>>(emptyList()) }
            var consoleBidNotices by remember(liveId) { mutableStateOf<List<LiveBidNotice>>(emptyList()) }
            var consoleNoticeSeq by remember(liveId) { mutableStateOf(0) }
            var consoleLoading by remember(liveId) { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var consoleError by remember(liveId) { mutableStateOf<String?>(null) }
            var consoleRevision by remember(liveId) { mutableStateOf(0) }
            var consoleChatError by remember(liveId) { mutableStateOf<String?>(null) }
            var consoleState by remember(liveId) { mutableStateOf<RealtimeConnectionState?>(null) }
            var consoleEnded by remember(liveId) { mutableStateOf(false) }
            var consoleActionLoading by remember(liveId) { mutableStateOf(false) }
            var consoleActionError by remember(liveId) { mutableStateOf<String?>(null) }
            var consoleActionErrorCode by remember(liveId) { mutableStateOf<String?>(null) }
            var consoleActionMessage by remember(liveId) { mutableStateOf<String?>(null) }
            var consoleConnection by remember(liveId) { mutableStateOf<com.ssafy.dib.data.remote.socket.LiveChatConnection?>(null) }
            val consoleActiveAuctionId = consoleAuctions.firstOrNull { it.status.equals("ACTIVE", ignoreCase = true) }?.auctionId

            LaunchedEffect(liveId, consoleRevision, signedIn, previewMode) {
                if (previewMode || !auth.networkConfig.isRestConfigured || liveId.isBlank()) {
                    consoleLoading = false
                    return@LaunchedEffect
                }
                // 경매가 열리고 닫힐 때마다 목록을 다시 받되, 첫 진입에서만 전체 로딩을 보여준다
                consoleLoading = consoleAuctions.isEmpty()
                consoleError = null
                when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getDetail(liveId) }) {
                    is ApiResult.Success -> {
                        val detail = result.value
                        consoleTitle = detail.title
                        consoleStatus = detail.status
                        consoleRoomName = detail.streamUrl
                        consoleViewerCount = detail.viewCount
                        consoleAuctions = detail.auctions
                        consoleEnded = detail.status.equals("ENDED", ignoreCase = true)
                    }
                    is ApiResult.Failure -> {
                        consoleError = liveControlError(result.error)
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                if (consoleMessages.isEmpty()) {
                    when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getMessages(liveId) }) {
                        is ApiResult.Success -> consoleMessages = result.value.items
                        is ApiResult.Failure -> {
                            consoleChatError = result.error.message.ifBlank { "이전 채팅을 불러오지 못했어요." }
                            if (result.error.requiresLogin) signedIn = false
                        }
                    }
                }
                consoleLoading = false
            }
            LaunchedEffect(consoleActionMessage) {
                if (consoleActionMessage != null) {
                    delay(2_000L)
                    consoleActionMessage = null
                }
            }
            // LiveChatConnection이 LIVE_AUCTION_OPENED/CLOSED를 받아 경매 토픽만 바꾸므로
            // 상품 시작 때 전체 소켓을 재생성하지 않는다. 재연결은 영상과 상품 UI를 깜빡이게 했다.
            DisposableEffect(liveId, signedIn, previewMode, auth.networkConfig.isWebSocketConfigured) {
                val connection = if (!previewMode && liveId.isNotBlank() && auth.networkConfig.isWebSocketConfigured) {
                    auth.createLiveChatConnection().also { created ->
                        consoleConnection = created
                        created.updateCurrentMemberId(memberProfile?.memberId)
                        created.start(
                            liveBroadcastId = liveId,
                            activeAuctionId = consoleActiveAuctionId,
                            onMessage = { message -> coroutineScope.launch {
                                consoleMessages = (consoleMessages + message).distinctBy { it.liveChattingId }
                            } },
                            onUpdate = { update -> coroutineScope.launch {
                                update.viewerCount?.let { consoleViewerCount = it }
                                update.liveTitle?.let { consoleTitle = it }
                                when (update.eventType) {
                                    SocketEventTypes.LIVE_ENDED -> {
                                        consoleEnded = true
                                        consoleStatus = "ENDED"
                                    }
                                    SocketEventTypes.LIVE_STARTED -> consoleStatus = "LIVE"
                                    SocketEventTypes.LIVE_AUCTION_OPENED, SocketEventTypes.LIVE_AUCTION_CLOSED -> consoleRevision++
                                }
                                update.auctionId?.let { auctionId ->
                                    consoleAuctions = consoleAuctions.map { auction ->
                                        if (auction.auctionId != auctionId) auction else auction.copy(
                                            currentPrice = update.currentPrice ?: auction.currentPrice,
                                            bidCount = update.bidCount ?: auction.bidCount,
                                            remainingSeconds = update.remainingSeconds ?: auction.remainingSeconds,
                                            status = update.status ?: auction.status
                                        )
                                    }
                                }
                                if (update.eventType == SocketEventTypes.HIGHEST_BID_UPDATED) {
                                    update.currentPrice?.let { amount ->
                                        consoleNoticeSeq++
                                        consoleBidNotices = (consoleBidNotices + LiveBidNotice(
                                            noticeId = "${update.auctionId.orEmpty()}:$amount:$consoleNoticeSeq",
                                            nickname = null,
                                            amount = amount,
                                            time = update.occurredAt ?: java.time.Instant.now().toString()
                                        )).takeLast(100)
                                    }
                                }
                            } },
                            onError = { message -> coroutineScope.launch { consoleChatError = message } },
                            onState = { state -> coroutineScope.launch { consoleState = state } }
                        )
                    }
                } else null
                onDispose {
                    consoleConnection = null
                    connection?.close()
                }
            }
            LaunchedEffect(consoleConnection, consoleActiveAuctionId) {
                consoleConnection?.updateActiveAuction(consoleActiveAuctionId)
            }

            val consoleStreamTokenProvider: (suspend () -> Result<com.ssafy.dib.domain.live.LiveStreamSession>)? =
                remember(liveId, previewMode, auth.networkConfig.isRestConfigured) {
                    if (previewMode || liveId.isBlank() || !auth.networkConfig.isRestConfigured) null
                    else suspend {
                        // 방 접속 토큰은 재사용하면 안 돼서 시도마다 새 멱등 키를 쓴다
                        when (val result = withContext(Dispatchers.IO) {
                            auth.liveRepository.prepareStream(liveId, java.util.UUID.randomUUID().toString())
                        }) {
                            is ApiResult.Success -> Result.success(result.value)
                            is ApiResult.Failure -> Result.failure(IllegalStateException(liveControlError(result.error)))
                        }
                    }
                }

            LiveBroadcastConsoleScreen(
                liveTitle = consoleTitle,
                liveStatus = consoleStatus,
                viewerCount = consoleViewerCount,
                roomName = consoleRoomName,
                streamTokenProvider = consoleStreamTokenProvider,
                auctions = consoleAuctions,
                chatMessages = consoleMessages,
                bidNotices = consoleBidNotices,
                connectionState = consoleState,
                isLoading = consoleLoading,
                errorMessage = consoleError,
                actionLoading = consoleActionLoading,
                actionError = consoleActionError,
                actionErrorCode = consoleActionErrorCode,
                actionMessage = consoleActionMessage,
                chatError = consoleChatError,
                currentMemberId = memberProfile?.memberId,
                liveEnded = consoleEnded,
                onRetry = { consoleRevision++ },
                onStartAuction = startConsoleAuction@ { auctionId ->
                    if (previewMode) {
                        consoleAuctions = consoleAuctions.map { auction ->
                            if (auction.auctionId == auctionId) auction.copy(status = "ACTIVE", remainingSeconds = 300) else auction
                        }
                        consoleActionMessage = "개발 미리보기 상품 경매를 시작했어요."
                        return@startConsoleAuction
                    }
                    val command = "live-start-auction:$liveId:$auctionId"
                    val idempotencyKey = commandKeys.keyFor(command)
                    consoleActionLoading = true
                    consoleActionError = null
                    consoleActionErrorCode = null
                    consoleActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.liveRepository.startAuction(liveId, auctionId, idempotencyKey)
                        }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                consoleAuctions = consoleAuctions.map { auction ->
                                    if (auction.auctionId == auctionId) auction.copy(status = "ACTIVE") else auction
                                }
                                consoleActionMessage = "상품 경매를 시작했어요."
                            }
                            is ApiResult.Failure -> {
                                consoleActionError = liveControlError(result.error)
                                consoleActionErrorCode = result.error.code
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        consoleActionLoading = false
                    }
                },
                onEndLive = endConsoleLive@ {
                    if (previewMode) {
                        consoleEnded = true
                        consoleStatus = "ENDED"
                        return@endConsoleLive
                    }
                    val command = "live-end:$liveId"
                    val idempotencyKey = commandKeys.keyFor(command)
                    consoleActionLoading = true
                    consoleActionError = null
                    consoleActionErrorCode = null
                    consoleActionMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.liveRepository.end(liveId, idempotencyKey) }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                consoleEnded = true
                                consoleStatus = "ENDED"
                                if (myLiveBroadcastId == liveId) myLiveBroadcastId = null   // 우상단 LIVE 표시 끄기
                                navController.navigateUp()
                            }
                            is ApiResult.Failure -> {
                                consoleActionError = liveControlError(result.error)
                                consoleActionErrorCode = result.error.code
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        consoleActionLoading = false
                    }
                },
                onSendChat = { content ->
                    if (previewMode) true else {
                        consoleConnection?.updateCurrentMemberId(memberProfile?.memberId)
                        consoleConnection?.send(content) == true
                    }
                },
                onOpenItemPlan = { navController.navigateUp() },
                onDismissActionError = {
                    consoleActionError = null
                    consoleActionErrorCode = null
                },
                // 방송 콘솔에서 뒤로 가면 홈으로 보낸다. navigateUp 이면 방송 편성 화면 같은
                // "방송 준비" 맥락으로 돌아가는데, 방송은 이미 켜져 있어서 맞지 않는다.
                // 대신 방송이 켜진 채라면 띱이가 그 사실을 말풍선으로 붙들고 있는다
                onBack = {
                    if (!consoleEnded && myLiveBroadcastId != null) {
                        assistantNotice = AssistantNotice(
                            id = "$HOSTING_NOTICE_PREFIX$liveId",
                            title = "회원님이 호스팅 중인 라이브가 있어요!",
                            body = "방송은 계속 켜져 있어요. 눌러서 방송 화면으로 돌아갈 수 있어요.",
                            actionable = true
                        )
                    }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = Screen.LiveWatch.route,
            arguments = listOf(navArgument("liveBroadcastId") { type = NavType.StringType })
        ) { backStackEntry ->
            val liveId = backStackEntry.arguments?.getString("liveBroadcastId").orEmpty()
            var watchTitle by remember(liveId) { mutableStateOf("Live 방송") }
            var watchSellerNickname by remember(liveId) { mutableStateOf<String?>(null) }
            var watchSellerMemberId by remember(liveId) { mutableStateOf<String?>(null) }
            var watchViewerCount by remember(liveId) { mutableStateOf(0) }
            var watchAuction by remember(liveId) { mutableStateOf<com.ssafy.dib.domain.auction.AuctionSummary?>(null) }
            var watchMessages by remember(liveId) { mutableStateOf<List<com.ssafy.dib.domain.live.LiveChatMessage>>(emptyList()) }
            var watchBidNotices by remember(liveId) { mutableStateOf<List<LiveBidNotice>>(emptyList()) }
            var watchNoticeSeq by remember(liveId) { mutableStateOf(0) }
            var watchLoading by remember(liveId) { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var watchError by remember(liveId) { mutableStateOf<String?>(null) }
            var watchRevision by remember(liveId) { mutableStateOf(0) }
            var watchChatError by remember(liveId) { mutableStateOf<String?>(null) }
            var watchState by remember(liveId) { mutableStateOf<RealtimeConnectionState?>(null) }
            var watchEnded by remember(liveId) { mutableStateOf(false) }
            var watchConnection by remember(liveId) { mutableStateOf<com.ssafy.dib.data.remote.socket.LiveChatConnection?>(null) }
            var watchPendingBidCommandId by remember(liveId) { mutableStateOf<String?>(null) }
            var watchBidFeedback by remember(liveId) { mutableStateOf<RealtimeBidFeedback?>(null) }
            val watchActiveAuctionId = watchAuction?.takeIf { it.status.equals("ACTIVE", ignoreCase = true) }?.auctionId

            LaunchedEffect(liveId, watchRevision, signedIn, previewMode) {
                if (previewMode || !auth.networkConfig.isRestConfigured || liveId.isBlank()) {
                    watchLoading = false
                    return@LaunchedEffect
                }
                watchLoading = watchAuction == null && watchMessages.isEmpty()
                watchError = null
                when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getDetail(liveId) }) {
                    is ApiResult.Success -> {
                        val detail = result.value
                        watchTitle = detail.title
                        watchViewerCount = detail.viewCount
                        watchAuction = detail.currentAuction
                            ?: detail.auctions.firstOrNull { it.status.equals("ACTIVE", ignoreCase = true) }
                        watchSellerNickname = (listOfNotNull(detail.currentAuction) + detail.auctions)
                            .firstNotNullOfOrNull { it.sellerNickname?.takeIf(String::isNotBlank) }
                        watchSellerMemberId = detail.memberId.takeIf(String::isNotBlank)
                        watchEnded = detail.status.equals("ENDED", ignoreCase = true)
                    }
                    is ApiResult.Failure -> {
                        watchError = result.error.message.ifBlank { "방송 정보를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                if (watchMessages.isEmpty()) {
                    when (val result = withContext(Dispatchers.IO) { auth.liveRepository.getMessages(liveId) }) {
                        is ApiResult.Success -> watchMessages = result.value.items
                        is ApiResult.Failure -> {
                            watchChatError = result.error.message.ifBlank { "이전 채팅을 불러오지 못했어요." }
                            if (result.error.requiresLogin) signedIn = false
                        }
                    }
                }
                watchLoading = false
            }
            // 열린 경매 구독은 기존 Live 소켓 안에서 전환한다. 경매 변경마다 소켓을 닫지 않는다.
            DisposableEffect(liveId, signedIn, previewMode, auth.networkConfig.isWebSocketConfigured) {
                val connection = if (!previewMode && liveId.isNotBlank() && auth.networkConfig.isWebSocketConfigured) {
                    auth.createLiveChatConnection().also { created ->
                        watchConnection = created
                        created.updateCurrentMemberId(memberProfile?.memberId)
                        created.start(
                            liveBroadcastId = liveId,
                            activeAuctionId = watchActiveAuctionId,
                            onMessage = { message -> coroutineScope.launch {
                                watchMessages = (watchMessages + message).distinctBy { it.liveChattingId }
                            } },
                            onUpdate = { update -> coroutineScope.launch {
                                update.viewerCount?.let { watchViewerCount = it }
                                update.liveTitle?.let { watchTitle = it }
                                if (update.eventType == SocketEventTypes.LIVE_ENDED) {
                                    watchEnded = true
                                    watchPendingBidCommandId = null
                                    return@launch
                                }
                                if (update.eventType == SocketEventTypes.LIVE_AUCTION_OPENED && update.auctionId != null) {
                                    watchRevision++
                                }
                                val current = watchAuction
                                if (current != null && update.auctionId == current.auctionId) {
                                    watchAuction = current.copy(
                                        currentPrice = update.currentPrice ?: current.currentPrice,
                                        bidCount = update.bidCount ?: current.bidCount,
                                        remainingSeconds = update.remainingSeconds ?: current.remainingSeconds,
                                        status = update.status ?: current.status,
                                        isHighestBidder = when {
                                            update.isHighestBidder != null -> update.isHighestBidder
                                            update.eventType == SocketEventTypes.HIGHEST_BID_UPDATED &&
                                                update.currentPrice != null && update.currentPrice != current.currentPrice -> false
                                            else -> current.isHighestBidder
                                        }
                                    )
                                }
                                if (update.eventType == SocketEventTypes.HIGHEST_BID_UPDATED) {
                                    update.currentPrice?.let { amount ->
                                        watchNoticeSeq++
                                        watchBidNotices = (watchBidNotices + LiveBidNotice(
                                            noticeId = "${update.auctionId.orEmpty()}:$amount:$watchNoticeSeq",
                                            nickname = null,
                                            amount = amount,
                                            time = update.occurredAt ?: java.time.Instant.now().toString()
                                        )).takeLast(100)
                                    }
                                }
                                if (update.bidAccepted != null && update.commandId == watchPendingBidCommandId) {
                                    watchBidFeedback = RealtimeBidFeedback(
                                        accepted = update.bidAccepted,
                                        message = update.message.orEmpty(),
                                        currentPrice = update.currentPrice,
                                        minAllowedAmount = update.minAllowedAmount,
                                        errorCode = update.errorCode,
                                        eventKey = "${update.eventType}:${update.commandId}:${update.occurredAt.orEmpty()}"
                                    )
                                    watchPendingBidCommandId = null
                                }
                            } },
                            onError = { message -> coroutineScope.launch { watchChatError = message } },
                            onState = { state -> coroutineScope.launch { watchState = state } }
                        )
                    }
                } else null
                onDispose {
                    watchConnection = null
                    watchPendingBidCommandId = null
                    connection?.close()
                }
            }
            LaunchedEffect(watchConnection, watchActiveAuctionId) {
                watchConnection?.updateActiveAuction(watchActiveAuctionId)
            }

            val watchStreamTokenProvider: (suspend () -> Result<com.ssafy.dib.domain.live.LiveStreamSession>)? =
                remember(liveId, previewMode, auth.networkConfig.isRestConfigured) {
                    if (previewMode || liveId.isBlank() || !auth.networkConfig.isRestConfigured) null
                    else suspend {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.liveRepository.prepareStream(liveId, java.util.UUID.randomUUID().toString())
                        }) {
                            is ApiResult.Success -> Result.success(result.value)
                            is ApiResult.Failure -> Result.failure(IllegalStateException(liveControlError(result.error)))
                        }
                    }
                }

            LiveWatchScreen(
                liveTitle = watchTitle,
                sellerNickname = watchSellerNickname,
                viewerCount = watchViewerCount,
                streamTokenProvider = watchStreamTokenProvider,
                activeAuction = watchAuction,
                chatMessages = watchMessages,
                bidNotices = watchBidNotices,
                connectionState = watchState,
                isLoading = watchLoading,
                errorMessage = watchError,
                chatError = watchChatError,
                bidFeedback = watchBidFeedback,
                bidEnabled = previewMode || auth.networkConfig.isWebSocketConfigured,
                isAuthenticated = hasAppAccess,
                currentMemberId = memberProfile?.memberId,
                sellerMemberId = watchSellerMemberId,
                liveEnded = watchEnded,
                onRetry = { watchRevision++ },
                onBid = { auctionId, amount ->
                    if (previewMode) {
                        watchBidFeedback = RealtimeBidFeedback(
                            accepted = true,
                            message = "개발 미리보기 입찰이 반영됐어요.",
                            currentPrice = amount,
                            minAllowedAmount = null,
                            errorCode = null,
                            eventKey = "preview:$auctionId:$amount"
                        )
                        true
                    } else watchConnection?.placeBid(auctionId, amount)?.let { commandId ->
                        watchPendingBidCommandId = commandId
                        true
                    } ?: false
                },
                onSendChat = { content ->
                    if (previewMode) true else {
                        watchConnection?.updateCurrentMemberId(memberProfile?.memberId)
                        watchConnection?.send(content) == true
                    }
                },
                onLoginRequired = { navController.navigate(Screen.Login.route) },
                onBack = navController::navigateUp
            )
        }
        composable(Screen.Addresses.route) {
            var addresses by remember { mutableStateOf<List<com.ssafy.dib.domain.member.MemberAddress>?>(null) }
            var addressesLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var addressesError by remember { mutableStateOf<String?>(null) }
            var addressesRevision by remember { mutableStateOf(0) }
            var addressActionLoading by remember { mutableStateOf(false) }
            var addressActionError by remember { mutableStateOf<String?>(null) }
            var addressActionMessage by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(addressesRevision, signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                addressesLoading = true
                addressesError = null
                when (val result = withContext(Dispatchers.IO) { auth.addressRepository.getAddresses() }) {
                    is ApiResult.Success -> addresses = result.value
                    is ApiResult.Failure -> {
                        addressesError = result.error.message.ifBlank { "배송지를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                addressesLoading = false
            }
            LaunchedEffect(addressActionMessage) {
                if (addressActionMessage != null) {
                    delay(1_800L)
                    addressActionMessage = null
                }
            }

            AddressManagementScreen(
                addresses = addresses,
                isLoading = addressesLoading,
                errorMessage = addressesError,
                actionLoading = addressActionLoading,
                actionError = addressActionError,
                actionMessage = addressActionMessage,
                onRetry = { addressesRevision++ },
                onUpdate = { address ->
                    addressActionLoading = true
                    addressActionError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.addressRepository.updateAddress(address) }) {
                            is ApiResult.Success -> {
                                addresses = addresses?.map { if (it.addressId == result.value.addressId) result.value else it }
                                addressActionMessage = "배송지를 수정했어요."
                            }
                            is ApiResult.Failure -> {
                                addressActionError = when (result.error.code) {
                                    "ADDRESS_NOT_FOUND" -> "배송지를 찾을 수 없어요."
                                    "FORBIDDEN" -> "이 배송지를 수정할 권한이 없어요."
                                    else -> result.error.message.ifBlank { "배송지를 수정하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        addressActionLoading = false
                    }
                },
                onCreate = { newAddress ->
                    addressActionLoading = true
                    addressActionError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.addressRepository.createAddress(newAddress) }) {
                            is ApiResult.Success -> {
                                addresses = (addresses.orEmpty() + result.value)
                                addressActionMessage = "배송지를 등록했어요."
                            }
                            is ApiResult.Failure -> {
                                addressActionError = result.error.message.ifBlank { "배송지를 등록하지 못했어요." }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        addressActionLoading = false
                    }
                },
                onDelete = { addressId ->
                    addressActionLoading = true
                    addressActionError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.addressRepository.deleteAddress(addressId) }) {
                            is ApiResult.Success -> {
                                addresses = addresses?.filterNot { it.addressId == addressId }
                                addressActionMessage = "배송지를 삭제했어요."
                            }
                            is ApiResult.Failure -> {
                                addressActionError = when (result.error.code) {
                                    "ADDRESS_IN_USE" -> "진행 중인 입찰이나 주문에서 사용하는 배송지는 삭제할 수 없어요."
                                    "ADDRESS_NOT_FOUND" -> "이미 삭제됐거나 찾을 수 없는 배송지예요."
                                    else -> result.error.message.ifBlank { "배송지를 삭제하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        addressActionLoading = false
                    }
                },
                onBack = navController::navigateUp,
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.SettlementAccounts.route) {
            var settlementAccount by remember { mutableStateOf<com.ssafy.dib.domain.settlement.SettlementAccount?>(null) }
            var settlementLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
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
                            is ApiResult.Failure -> {
                                settlementActionError = signupErrorMessage(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
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
                            is ApiResult.Failure -> {
                                settlementActionError = signupErrorMessage(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
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
            var settlementsLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var settlementsError by remember { mutableStateOf<String?>(null) }
            var settlementsCursor by remember { mutableStateOf<String?>(null) }
            var settlementsHasNext by remember { mutableStateOf(false) }
            var settlementsLoadingMore by remember { mutableStateOf(false) }
            var settlementsLoadMoreError by remember { mutableStateOf<String?>(null) }
            var settlementsRevision by remember { mutableStateOf(0) }

            fun loadSettlements(cursor: String?, append: Boolean) {
                if (append) {
                    settlementsLoadingMore = true
                    settlementsLoadMoreError = null
                } else {
                    settlementsLoading = true
                    settlementsError = null
                    settlementsLoadMoreError = null
                    settlementsLoadingMore = false
                    settlementsCursor = null
                    settlementsHasNext = false
                }
                coroutineScope.launch {
                    when (val result = withContext(Dispatchers.IO) { auth.settlementRepository.getSettlements(cursor) }) {
                        is ApiResult.Success -> {
                            settlements = if (append) {
                                (settlements.orEmpty() + result.value.items).distinctBy { it.settlementId }
                            } else result.value.items
                            settlementsCursor = result.value.nextCursor
                            settlementsHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                        }
                        is ApiResult.Failure -> {
                            if (append && result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                settlementsLoadingMore = false
                                loadSettlements(cursor = null, append = false)
                            } else {
                                val message = result.error.message.ifBlank { "정산 내역을 불러오지 못했어요." }
                                if (append) settlementsLoadMoreError = message else settlementsError = message
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                    }
                    if (append) settlementsLoadingMore = false else settlementsLoading = false
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
                isLoadingMore = settlementsLoadingMore,
                loadMoreError = settlementsLoadMoreError,
                onRetry = { settlementsRevision++ },
                onLoadMore = {
                    if (!settlementsLoadingMore && settlementsCursor != null) {
                        loadSettlements(settlementsCursor, append = true)
                    }
                },
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
            var settlementLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
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
            NotificationSettingsScreen(
                tradeEnabled = tradeNotificationsEnabled,
                liveEnabled = liveNotificationsEnabled,
                wishlistEnabled = wishlistNotificationsEnabled,
                onTradeEnabledChange = { enabled ->
                    tradeNotificationsEnabled = enabled
                    notificationPreferences.edit().putBoolean("trade_enabled", enabled).apply()
                },
                onLiveEnabledChange = { enabled ->
                    liveNotificationsEnabled = enabled
                    notificationPreferences.edit().putBoolean("live_enabled", enabled).apply()
                },
                onWishlistEnabledChange = { enabled ->
                    wishlistNotificationsEnabled = enabled
                    notificationPreferences.edit().putBoolean("wishlist_enabled", enabled).apply()
                },
                onBack = navController::navigateUp,
                onTabSelected = ::navigateMain
            )
        }
        composable(Screen.ProfileEdit.route) {
            var profileLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
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
            var favoritesLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var favoritesError by remember { mutableStateOf<String?>(null) }
            var favoritesRevision by remember { mutableStateOf(0) }
            var removingAuctionId by remember { mutableStateOf<String?>(null) }
            var favoritesCursor by remember { mutableStateOf<String?>(null) }
            var favoritesHasNext by remember { mutableStateOf(false) }
            var favoritesLoadingMore by remember { mutableStateOf(false) }
            var favoritesLoadMoreError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(favoritesRevision, signedIn) {
                if (signedIn != true || !auth.networkConfig.isRestConfigured) return@LaunchedEffect
                favoritesLoading = true
                favoritesError = null
                favoritesLoadMoreError = null
                when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getBookmarks() }) {
                    is ApiResult.Success -> {
                        favorites = result.value.items.map { it.toHomeAuction() }
                        favoritesCursor = result.value.nextCursor
                        favoritesHasNext = result.value.hasNext && !result.value.nextCursor.isNullOrBlank()
                    }
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
                showSampleContent = previewMode || !auth.networkConfig.isRestConfigured,
                isLoading = favoritesLoading,
                errorMessage = favoritesError,
                removingAuctionId = removingAuctionId,
                hasNext = favoritesHasNext,
                isLoadingMore = favoritesLoadingMore,
                loadMoreError = favoritesLoadMoreError,
                onRetry = { favoritesRevision++ },
                onLoadMore = {
                    val cursor = favoritesCursor
                    if (cursor != null && favoritesHasNext && !favoritesLoadingMore) {
                        favoritesLoadingMore = true
                        favoritesLoadMoreError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.auctionRepository.getBookmarks(cursor = cursor)
                            }) {
                                is ApiResult.Success -> {
                                    favorites = (favorites.orEmpty() + result.value.items.map { it.toHomeAuction() })
                                        .distinctBy(HomeAuction::id)
                                    favoritesCursor = result.value.nextCursor
                                    favoritesHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                                }
                                is ApiResult.Failure -> {
                                    if (result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                        favoritesRevision++
                                    } else {
                                        favoritesLoadMoreError = result.error.message.ifBlank { "다음 찜 목록을 불러오지 못했어요." }
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            }
                            favoritesLoadingMore = false
                        }
                    }
                },
                onRemove = { auctionId ->
                    val bookmarkProductId = favorites?.firstOrNull { it.id == auctionId }?.productId ?: auctionId
                    val command = "bookmark:$bookmarkProductId:false"
                    val idempotencyKey = commandKeys.keyFor(command)
                    removingAuctionId = auctionId
                    favoritesError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.auctionRepository.setBookmark(bookmarkProductId, false, idempotencyKey)
                        }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
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
            var registeredProductsLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var registeredProductsError by remember { mutableStateOf<String?>(null) }
            var registeredProductsRevision by remember { mutableStateOf(0) }
            var registeredProductsCursor by remember { mutableStateOf<String?>(null) }
            var registeredProductsHasNext by remember { mutableStateOf(false) }
            var registeredProductsLoadingMore by remember { mutableStateOf(false) }
            var registeredProductsLoadMoreError by remember { mutableStateOf<String?>(null) }
            var deletingProductId by remember { mutableStateOf<String?>(null) }
            var productDeleteError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(registeredProductsRevision, productsRefresh, previewMode) {
                if (previewMode || !auth.networkConfig.isRestConfigured) {
                    registeredProductsLoading = false
                    registeredProductsError = null
                    return@LaunchedEffect
                }
                registeredProductsLoading = true
                registeredProductsError = null
                registeredProductsLoadMoreError = null
                when (val result = withContext(Dispatchers.IO) { auth.productRepository.getMyProducts() }) {
                    is ApiResult.Success -> {
                        registeredProducts = result.value.items
                        registeredProductsCursor = result.value.nextCursor
                        registeredProductsHasNext = result.value.hasNext && !result.value.nextCursor.isNullOrBlank()
                    }
                    is ApiResult.Failure -> {
                        registeredProductsError = result.error.message.ifBlank { "등록 상품을 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                registeredProductsLoading = false
            }
            RegisteredProductsScreen(
                selectionPurpose = productSelectionPurpose,
                onBack = {
                    productSelectionPurpose = null
                    navController.navigateUp()
                },
                onRegister = { navController.navigate(Screen.Register.route) },
                onTabSelected = ::navigateMain,
                remoteProducts = registeredProducts,
                showSampleContent = previewMode || !auth.networkConfig.isRestConfigured,
                isLoading = registeredProductsLoading,
                errorMessage = registeredProductsError,
                deleteError = productDeleteError,
                deletingProductId = deletingProductId,
                hasNext = registeredProductsHasNext,
                isLoadingMore = registeredProductsLoadingMore,
                loadMoreError = registeredProductsLoadMoreError,
                onRetry = { registeredProductsRevision++ },
                onLoadMore = {
                    val cursor = registeredProductsCursor
                    if (cursor != null && registeredProductsHasNext && !registeredProductsLoadingMore) {
                        registeredProductsLoadingMore = true
                        registeredProductsLoadMoreError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.productRepository.getMyProducts(cursor = cursor)
                            }) {
                                is ApiResult.Success -> {
                                    registeredProducts = (registeredProducts.orEmpty() + result.value.items)
                                        .distinctBy { it.productId }
                                    registeredProductsCursor = result.value.nextCursor
                                    registeredProductsHasNext = hasUsableNextCursor(result.value.hasNext, result.value.nextCursor, cursor)
                                }
                                is ApiResult.Failure -> {
                                    if (result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                        registeredProductsRevision++
                                    } else {
                                        registeredProductsLoadMoreError = result.error.message.ifBlank { "다음 상품을 불러오지 못했어요." }
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            }
                            registeredProductsLoadingMore = false
                        }
                    }
                },
                onAuctionStart = { productId ->
                    productSelectionPurpose = null
                    navController.navigate(Screen.AuctionRegister.createRoute(productId))
                },
                onEditProduct = { productId -> navController.navigate(Screen.ProductEdit.createRoute(productId)) },
                onDeleteProduct = { productId ->
                    val command = "product-delete:$productId"
                    val idempotencyKey = commandKeys.keyFor(command)
                    deletingProductId = productId
                    productDeleteError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.productRepository.deleteProduct(productId, idempotencyKey) }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                registeredProducts = registeredProducts?.filterNot { it.productId == productId }
                            }
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
            // 상품 상세에는 경매 정보가 없어 판매자 상품 목록에서 이 상품의 경매 상태/시작가/시간을 가져온다
            var editAuction by remember(productId) { mutableStateOf<RegisteredProduct?>(null) }

            LaunchedEffect(productId, editRevision) {
                editLoading = true
                editError = null
                val detailResult = withContext(Dispatchers.IO) { auth.productRepository.getProduct(productId) }
                val categoriesResult = withContext(Dispatchers.IO) { auth.productRepository.getCategories() }
                if (detailResult is ApiResult.Success && categoriesResult is ApiResult.Success) {
                    editDetail = detailResult.value
                    editCategories = categoriesResult.value
                    val sellerProducts = withContext(Dispatchers.IO) {
                        auth.productRepository.getSellerProducts(detailResult.value.memberId)
                    }
                    editAuction = (sellerProducts as? ApiResult.Success)?.value?.firstOrNull { it.productId == productId }
                } else {
                    val failure = (detailResult as? ApiResult.Failure) ?: (categoriesResult as? ApiResult.Failure)
                    editError = failure?.error?.message?.ifBlank { "상품 정보를 불러오지 못했어요." }
                }
                editLoading = false
            }

            ProductEditScreen(
                detail = editDetail,
                categories = editCategories,
                auctionStatus = editAuction?.auctionStatus,
                auctionStartPrice = editAuction?.startPrice,
                auctionTimeSeconds = editAuction?.auctionTimeSeconds,
                productStatus = editDetail?.status,
                isLoading = editLoading,
                errorMessage = editError,
                submitLoading = editSubmitLoading,
                submitError = editSubmitError,
                result = editResult,
                onRetry = { editRevision++ },
                onSubmit = { update ->
                    editSubmitLoading = true
                    editSubmitError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.productRepository.updateProduct(productId, update) }) {
                            is ApiResult.Success -> editResult = result.value
                            is ApiResult.Failure -> {
                                editSubmitError = when (result.error.code) {
                                    "PRODUCT_NOT_EDITABLE" -> "진행 중인 경매나 거래가 있어 수정할 수 없어요."
                                    "PRODUCT_NOT_FOUND" -> "상품을 찾을 수 없어요."
                                    "FORBIDDEN", "NOT_MY_PRODUCT" -> "본인이 등록한 상품만 수정할 수 있어요."
                                    // 검수 전 상품에는 경매 행이 없어서 가격·경매시간을 보내면 404 가 난다
                                    "AUCTION_NOT_FOUND" -> "검수 통과 후에 가격과 경매 시간을 정할 수 있어요."
                                    "PRODUCT_PENDING" -> "검수 중인 상품이에요."
                                    "PRODUCT_NOT_APPROVED" -> "검수가 끝난 상품만 경매를 시작할 수 있어요."
                                    else -> result.error.message.ifBlank { "상품을 수정하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
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
            var auctionProduct by remember(productId) { mutableStateOf<RegisteredProduct?>(null) }
            var productLoading by remember(productId) { mutableStateOf(true) }
            var productLoadError by remember(productId) { mutableStateOf<String?>(null) }
            var productRevision by remember(productId) { mutableStateOf(0) }
            var auctionStarted by remember(productId) { mutableStateOf(false) }
            var commandLoading by remember(productId) { mutableStateOf(false) }
            var commandError by remember(productId) { mutableStateOf<String?>(null) }

            LaunchedEffect(productId, productRevision, previewMode) {
                if (previewMode) {
                    auctionProduct = RegisteredProduct(
                        productId = productId,
                        title = "개발 미리보기 상품",
                        condition = "GOOD",
                        status = "REGISTERED",
                        thumbnailUrl = null,
                        auctionId = "preview-auction-$productId",
                        startPrice = 30_000,
                        auctionTimeSeconds = 300,
                        auctionStatus = "SCHEDULED"
                    )
                    productLoading = false
                    productLoadError = null
                    return@LaunchedEffect
                }
                productLoading = true
                productLoadError = null
                when (val result = withContext(Dispatchers.IO) { auth.productRepository.getMyProducts(size = 100) }) {
                    is ApiResult.Success -> {
                        auctionProduct = result.value.items.firstOrNull { it.productId == productId }
                        if (auctionProduct == null) productLoadError = "상품 또는 예정 경매 정보를 찾지 못했어요."
                    }
                    is ApiResult.Failure -> {
                        productLoadError = result.error.message.ifBlank { "경매 준비 정보를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                productLoading = false
            }

            AuctionRegisterScreen(
                productId = productId,
                product = auctionProduct,
                started = auctionStarted,
                isLoading = productLoading || commandLoading,
                errorMessage = commandError ?: productLoadError,
                onRetry = {
                    commandError = null
                    productRevision++
                },
                onStart = { startPrice, auctionTime ->
                    val auctionId = auctionProduct?.auctionId ?: return@AuctionRegisterScreen
                    if (previewMode) {
                        auctionStarted = true
                        return@AuctionRegisterScreen
                    }
                    val command = "auction-start:$auctionId:$startPrice:$auctionTime"
                    val idempotencyKey = commandKeys.keyFor(command)
                    commandLoading = true
                    commandError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.startAuction(auctionId, idempotencyKey, startPrice, auctionTime) }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                auctionStarted = true
                                auctionsRevision++
                                navController.previousBackStackEntry?.savedStateHandle?.set("refreshProducts", System.currentTimeMillis())
                            }
                            is ApiResult.Failure -> {
                                commandError = auctionCommandError(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        commandLoading = false
                    }
                },
                onOpenAuction = { auctionProduct?.auctionId?.let { navController.navigate(Screen.ProductDetail.createRoute(it)) } },
                onBack = navController::navigateUp
            )
        }
        composable(Screen.Inquiries.route) {
            var inquiries by remember { mutableStateOf<List<InquirySummary>?>(null) }
            var inquiriesLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var inquiriesError by remember { mutableStateOf<String?>(null) }
            var inquiriesRevision by remember { mutableStateOf(0) }
            var inquiriesCursor by remember { mutableStateOf<String?>(null) }
            var inquiriesHasNext by remember { mutableStateOf(false) }
            var inquiriesLoadingMore by remember { mutableStateOf(false) }
            var inquiriesLoadMoreError by remember { mutableStateOf<String?>(null) }
            var selectedInquiry by remember { mutableStateOf<InquiryDetail?>(null) }
            var inquiryDetailLoading by remember { mutableStateOf(false) }
            var inquiryDetailError by remember { mutableStateOf<String?>(null) }
            var inquirySubmitLoading by remember { mutableStateOf(false) }
            var inquirySubmitError by remember { mutableStateOf<String?>(null) }
            var inquirySubmissionRevision by remember { mutableStateOf(0) }

            LaunchedEffect(inquiriesRevision, previewMode) {
                if (previewMode || !auth.networkConfig.isRestConfigured) {
                    inquiriesLoading = false
                    inquiriesError = null
                    return@LaunchedEffect
                }
                inquiriesLoading = true
                inquiriesError = null
                inquiriesLoadMoreError = null
                when (val result = withContext(Dispatchers.IO) { auth.inquiryRepository.getInquiries() }) {
                    is ApiResult.Success -> {
                        inquiries = result.value.items
                        inquiriesCursor = result.value.nextCursor
                        inquiriesHasNext = result.value.hasNext && !result.value.nextCursor.isNullOrBlank()
                    }
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
                showSampleContent = previewMode || !auth.networkConfig.isRestConfigured,
                isLoading = inquiriesLoading,
                errorMessage = inquiriesError,
                hasNext = inquiriesHasNext,
                isLoadingMore = inquiriesLoadingMore,
                loadMoreError = inquiriesLoadMoreError,
                selectedInquiry = selectedInquiry,
                detailLoading = inquiryDetailLoading,
                detailError = inquiryDetailError,
                submitLoading = inquirySubmitLoading,
                submitError = inquirySubmitError,
                submissionRevision = inquirySubmissionRevision,
                onRetry = { inquiriesRevision++ },
                onLoadMore = {
                    val cursor = inquiriesCursor
                    if (cursor != null && !inquiriesLoadingMore) {
                        inquiriesLoadingMore = true
                        inquiriesLoadMoreError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.inquiryRepository.getInquiries(cursor)
                            }) {
                                is ApiResult.Success -> {
                                    inquiries = (inquiries.orEmpty() + result.value.items).distinctBy { it.questionId }
                                    inquiriesCursor = result.value.nextCursor.takeIf {
                                        hasUsableNextCursor(result.value.hasNext, it, cursor)
                                    }
                                    inquiriesHasNext = !inquiriesCursor.isNullOrBlank()
                                }
                                is ApiResult.Failure -> {
                                    if (result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                        inquiriesRevision++
                                    } else {
                                        inquiriesLoadMoreError = result.error.message.ifBlank { "다음 문의 내역을 불러오지 못했어요." }
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            }
                            inquiriesLoadingMore = false
                        }
                    }
                },
                onInquiryClick = { questionId ->
                    inquiryDetailLoading = true
                    inquiryDetailError = null
                    selectedInquiry = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.inquiryRepository.getInquiry(questionId)
                        }) {
                            is ApiResult.Success -> selectedInquiry = result.value
                            is ApiResult.Failure -> {
                                inquiryDetailError = result.error.message.ifBlank { "문의 상세를 불러오지 못했어요." }
                                if (result.error.requiresLogin) signedIn = false
                            }
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
                            is ApiResult.Failure -> {
                                inquirySubmitError = when (result.error.code) {
                                    "INVALID_QUESTION" -> "제목과 문의 내용을 확인해주세요."
                                    else -> result.error.message.ifBlank { "문의를 등록하지 못했어요." }
                                }
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        inquirySubmitLoading = false
                    }
                }
            )
        }
        composable(Screen.ReportHistory.route) {
            var reports by remember { mutableStateOf<List<ReportSummary>?>(null) }
            var reportsLoading by remember { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var reportsError by remember { mutableStateOf<String?>(null) }
            var reportsRevision by remember { mutableStateOf(0) }
            var reportsCursor by remember { mutableStateOf<String?>(null) }
            var reportsHasNext by remember { mutableStateOf(false) }
            var reportsLoadingMore by remember { mutableStateOf(false) }
            var reportsLoadMoreError by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(reportsRevision, previewMode) {
                if (previewMode || !auth.networkConfig.isRestConfigured) {
                    reportsLoading = false
                    reportsError = null
                    return@LaunchedEffect
                }
                reportsLoading = true
                reportsError = null
                reportsLoadMoreError = null
                when (val result = withContext(Dispatchers.IO) { auth.reportRepository.getMyReports() }) {
                    is ApiResult.Success -> {
                        reports = result.value.items
                        reportsCursor = result.value.nextCursor
                        reportsHasNext = result.value.hasNext && !result.value.nextCursor.isNullOrBlank()
                    }
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
                showSampleContent = previewMode || !auth.networkConfig.isRestConfigured,
                isLoading = reportsLoading,
                errorMessage = reportsError,
                hasNext = reportsHasNext,
                isLoadingMore = reportsLoadingMore,
                loadMoreError = reportsLoadMoreError,
                onRetry = { reportsRevision++ },
                onLoadMore = {
                    val cursor = reportsCursor
                    if (cursor != null && !reportsLoadingMore) {
                        reportsLoadingMore = true
                        reportsLoadMoreError = null
                        coroutineScope.launch {
                            when (val result = withContext(Dispatchers.IO) {
                                auth.reportRepository.getMyReports(cursor)
                            }) {
                                is ApiResult.Success -> {
                                    reports = (reports.orEmpty() + result.value.items).distinctBy { it.reportId }
                                    reportsCursor = result.value.nextCursor.takeIf {
                                        hasUsableNextCursor(result.value.hasNext, it, cursor)
                                    }
                                    reportsHasNext = !reportsCursor.isNullOrBlank()
                                }
                                is ApiResult.Failure -> {
                                    if (result.error.code == ApiErrorCodes.INVALID_CURSOR) {
                                        reportsRevision++
                                    } else {
                                        reportsLoadMoreError = result.error.message.ifBlank { "다음 신고 내역을 불러오지 못했어요." }
                                        if (result.error.requiresLogin) signedIn = false
                                    }
                                }
                            }
                            reportsLoadingMore = false
                        }
                    }
                }
            )
        }
        composable(Screen.Withdrawal.route) {
            var withdrawalLoading by remember { mutableStateOf(false) }
            var withdrawalError by remember { mutableStateOf<String?>(null) }
            var withdrawalBlockingMessage by remember { mutableStateOf<String?>(null) }
            var withdrawalResult by remember { mutableStateOf<com.ssafy.dib.domain.member.MemberWithdrawal?>(null) }
            WithdrawalScreen(
                isSubmitting = withdrawalLoading,
                errorMessage = withdrawalError,
                blockingMessage = withdrawalBlockingMessage,
                withdrawal = withdrawalResult,
                onSubmit = {
                    withdrawalLoading = true
                    withdrawalError = null
                    withdrawalBlockingMessage = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) { auth.memberRepository.requestWithdrawal() }) {
                            is ApiResult.Success -> withdrawalResult = result.value
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
            route = Screen.ProductOverview.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            var product by remember(productId) { mutableStateOf<com.ssafy.dib.domain.product.ProductDetail?>(null) }
            var loading by remember(productId) { mutableStateOf(auth.networkConfig.isRestConfigured && !previewMode) }
            var errorMessage by remember(productId) { mutableStateOf<String?>(null) }
            var revision by remember(productId) { mutableStateOf(0) }
            LaunchedEffect(productId, revision, signedIn) {
                if (!auth.networkConfig.isRestConfigured || signedIn != true) {
                    loading = false
                    if (signedIn == false) errorMessage = "로그인 후 상품 정보를 확인할 수 있어요."
                    return@LaunchedEffect
                }
                loading = true
                errorMessage = null
                when (val result = withContext(Dispatchers.IO) { auth.productRepository.getProduct(productId) }) {
                    is ApiResult.Success -> product = result.value
                    is ApiResult.Failure -> {
                        errorMessage = result.error.message.ifBlank { "상품 정보를 불러오지 못했어요." }
                        if (result.error.requiresLogin) signedIn = false
                    }
                }
                loading = false
            }
            ProductOverviewScreen(
                product = product,
                loading = loading,
                errorMessage = errorMessage,
                isMyProduct = product?.memberId?.isNotBlank() == true && product?.memberId == memberProfile?.memberId,
                onEditProduct = { editId -> navController.navigate(Screen.ProductEdit.createRoute(editId)) },
                onRetry = { revision++ },
                onBack = navController::navigateUp,
                onSellerClick = { memberId ->
                    product?.let { detail ->
                        backStackEntry.savedStateHandle["sellerNickname"] = detail.sellerNickname
                        detail.sellerRating?.let { backStackEntry.savedStateHandle["sellerRating"] = it }
                        detail.sellerReviewCount?.let { backStackEntry.savedStateHandle["sellerReviewCount"] = it }
                        detail.sellerTradeCount?.let { backStackEntry.savedStateHandle["sellerTradeCount"] = it }
                    }
                    navController.navigate(Screen.SellerProfile.createRoute(memberId))
                },
                onImageClick = { page ->
                    backStackEntry.savedStateHandle["productOverviewImageUrls"] = ArrayList(product?.imageUrls?.takeIf { it.isNotEmpty() } ?: listOfNotNull(product?.thumbnailUrl))
                    navController.navigate(Screen.ProductOverviewImages.createRoute(productId, page))
                }
            )
        }
        composable(
            route = Screen.ProductOverviewImages.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("initialPage") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val imageUrls = navController.previousBackStackEntry?.savedStateHandle
                ?.get<ArrayList<String>>("productOverviewImageUrls").orEmpty()
            ProductImageViewerScreen(
                productId = backStackEntry.arguments?.getString("productId").orEmpty(),
                initialPage = backStackEntry.arguments?.getInt("initialPage") ?: 0,
                imageUrls = imageUrls,
                showSampleContent = false,
                onClose = navController::navigateUp
            )
        }
        composable(
            route = Screen.ProductImages.route,
            arguments = listOf(
                navArgument("auctionId") { type = NavType.StringType },
                navArgument("initialPage") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val imageUrls = navController.previousBackStackEntry?.savedStateHandle
                ?.get<ArrayList<String>>("productImageUrls").orEmpty()
            ProductImageViewerScreen(
                productId = backStackEntry.arguments?.getString("auctionId").orEmpty(),
                initialPage = backStackEntry.arguments?.getInt("initialPage") ?: 0,
                imageUrls = imageUrls,
                showSampleContent = previewMode || !auth.networkConfig.isRestConfigured,
                onClose = navController::navigateUp
            )
        }
        composable(
            route = Screen.SellerProfile.route,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId").orEmpty()
            val sourceState = navController.previousBackStackEntry?.savedStateHandle
            var sellerAuctions by remember(memberId) { mutableStateOf<List<SellerAuction>?>(null) }

            LaunchedEffect(memberId) {
                if (previewMode || !auth.networkConfig.isRestConfigured || memberId.isBlank()) return@LaunchedEffect
                when (val result = withContext(Dispatchers.IO) { auth.auctionRepository.getSellerAuctions(memberId) }) {
                    is ApiResult.Success -> sellerAuctions = result.value
                    is ApiResult.Failure -> if (result.error.requiresLogin) signedIn = false
                }
            }

            SellerProfileScreen(
                sellerNickname = sourceState?.get<String>("sellerNickname"),
                sellerRating = sourceState?.get<Double>("sellerRating"),
                sellerReviewCount = sourceState?.get<Int>("sellerReviewCount"),
                sellerTradeCount = sourceState?.get<Int>("sellerTradeCount"),
                activeCount = sellerAuctions?.count { it.status == "ACTIVE" },
                endedCount = sellerAuctions?.count { it.status == "ENDED" },
                showSampleContent = previewMode || !auth.networkConfig.isRestConfigured,
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
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId").orEmpty()
            var sellerListings by remember(memberId) { mutableStateOf<List<SellerListing>?>(null) }
            var listingsLoading by remember(memberId) { mutableStateOf(false) }
            var listingsError by remember(memberId) { mutableStateOf<String?>(null) }
            var listingsRevision by remember(memberId) { mutableStateOf(0) }

            LaunchedEffect(memberId, listingsRevision) {
                if (previewMode || !auth.networkConfig.isRestConfigured || memberId.isBlank()) return@LaunchedEffect
                listingsLoading = true
                listingsError = null
                val auctions = withContext(Dispatchers.IO) { auth.auctionRepository.getSellerAuctions(memberId) }
                val products = withContext(Dispatchers.IO) { auth.productRepository.getSellerProducts(memberId) }
                if (auctions is ApiResult.Success) {
                    // 판매자 경매 API 응답에는 상품 제목/썸네일이 없어 판매자 상품 목록과 productId 로 맞춘다
                    val byProductId = (products as? ApiResult.Success)?.value?.associateBy { it.productId }.orEmpty()
                    sellerListings = auctions.value.filter { it.productId.isNotBlank() }.map { auction ->
                        val product = byProductId[auction.productId]
                        SellerListing(
                            productId = auction.productId,
                            title = product?.title ?: "판매 상품",
                            thumbnailUrl = product?.thumbnailUrl,
                            currentPrice = auction.currentPrice,
                            bidCount = auction.bidCount,
                            status = auction.status
                        )
                    }
                } else {
                    val failure = (auctions as? ApiResult.Failure)?.error
                    listingsError = failure?.message?.ifBlank { "판매 내역을 불러오지 못했어요." } ?: "판매 내역을 불러오지 못했어요."
                    if (failure?.requiresLogin == true) signedIn = false
                }
                listingsLoading = false
            }

            SellerListingsScreen(
                listings = sellerListings,
                isLoading = listingsLoading,
                errorMessage = listingsError,
                onRetry = { listingsRevision++ },
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
                    val command = "member-report:$memberId:$content"
                    val idempotencyKey = commandKeys.keyFor(command)
                    submitting = true
                    reportError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.reportRepository.reportMember(memberId, content, idempotencyKey)
                        }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                submitted = true
                            }
                            is ApiResult.Failure -> {
                                reportError = reportSubmissionMessage(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        submitting = false
                    }
                },
                onSubmitted = { navController.navigateUp() }
            )
        }
        composable(
            route = Screen.ProductReport.route,
            arguments = listOf(navArgument("auctionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val auctionId = backStackEntry.arguments?.getString("auctionId").orEmpty()
            var submitted by remember { mutableStateOf(false) }
            var submitting by remember { mutableStateOf(false) }
            var reportError by remember { mutableStateOf<String?>(null) }
            ProductReportScreen(
                onBack = navController::navigateUp,
                submitted = submitted,
                isSubmitting = submitting,
                errorMessage = reportError,
                onSubmit = { content ->
                    val command = "auction-report:$auctionId:$content"
                    val idempotencyKey = commandKeys.keyFor(command)
                    submitting = true
                    reportError = null
                    coroutineScope.launch {
                        when (val result = withContext(Dispatchers.IO) {
                            auth.reportRepository.reportAuction(auctionId, content, idempotencyKey)
                        }) {
                            is ApiResult.Success -> {
                                commandKeys.complete(command)
                                submitted = true
                            }
                            is ApiResult.Failure -> {
                                reportError = reportSubmissionMessage(result.error)
                                if (result.error.requiresLogin) signedIn = false
                            }
                        }
                        submitting = false
                    }
                },
                onSubmitted = { navController.navigateUp() }
            )
        }
        }
        SnackbarHost(
            hostState = notificationSnackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 76.dp)
        )
        if (showCreateMenu) {
            DibCreateMenuSheet(
                onDismiss = { showCreateMenu = false },
                onProductRegister = {
                    showCreateMenu = false
                    navController.navigate(Screen.Register.route)
                },
                onAuctionRegister = {
                    showCreateMenu = false
                    productSelectionPurpose = "auction"
                    navController.navigate(Screen.RegisteredProducts.route)
                },
                onLivePrepare = {
                    showCreateMenu = false
                    navController.navigate(Screen.LiveManagement.route)
                }
            )
        }

        // 떠 있는 띱이. 방송 중이라는 표시와 새 알림을 둘 다 맡는다.
        // 콘솔 화면에서는 방송 중 표시가 중복이라 알림이 있을 때만 나온다
        DibFloatingAssistant(
            live = signedIn == true
                && myLiveBroadcastId != null
                && currentBackStackEntry?.destination?.route != Screen.LiveBroadcastConsole.route,
            notice = assistantNotice.takeIf { signedIn == true },
            onOpenLive = {
                myLiveBroadcastId?.let { liveId ->
                    navController.navigate(Screen.LiveBroadcastConsole.createRoute(liveId)) {
                        launchSingleTop = true
                    }
                }
            },
            onOpenNotice = { notice ->
                assistantNotice = null
                if (notice.id.startsWith(HOSTING_NOTICE_PREFIX)) {
                    // 서버에서 온 알림이 아니라 "방송 켜 둔 채 나왔다" 를 붙들어 두는 자체 안내다
                    notice.id.removePrefix(HOSTING_NOTICE_PREFIX).takeIf(String::isNotBlank)?.let { liveId ->
                        navController.navigate(Screen.LiveBroadcastConsole.createRoute(liveId)) {
                            launchSingleTop = true
                        }
                    }
                } else {
                    domainNotifications.firstOrNull { it.eventId == notice.id }
                        ?.takeIf(::canOpenNotification)
                        ?.let(::openNotification)
                }
            },
            onDismissNotice = { assistantNotice = null },
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

// 서버 알림이 아니라 "방송 켜 둔 채 콘솔에서 나왔다" 를 띱이가 붙들어 두는 자체 안내의 id 접두사
private const val HOSTING_NOTICE_PREFIX = "live-hosting:"
private const val LAST_AUTHENTICATED_ROUTE_KEY = "last_authenticated_route"
private val ROUTE_ARGUMENT_PATTERN = Regex("\\{([^}]+)\\}")
private val NON_RESTORABLE_AUTHENTICATED_ROUTES = setOf(
    Screen.Splash.route,
    Screen.Welcome.route,
    Screen.SignUp.route,
    Screen.Login.route,
    Screen.FindEmail.route,
    Screen.PasswordResetLink.route,
    Screen.PasswordReset.route
)

private fun NavBackStackEntry.persistedRoute(): String? {
    var route = destination.route ?: return null
    if (route in NON_RESTORABLE_AUTHENTICATED_ROUTES) return null
    ROUTE_ARGUMENT_PATTERN.findAll(route).toList().forEach { match ->
        val key = match.groupValues[1]
        val args = arguments ?: return null
        if (!args.containsKey(key)) return null
        @Suppress("DEPRECATION")
        val value = args.get(key) ?: return null
        route = route.replace(match.value, Uri.encode(value.toString()))
    }
    return route.takeIf(::isRestorableAuthenticatedRoute)
}

private fun isRestorableAuthenticatedRoute(route: String): Boolean =
    route.isNotBlank() &&
        '{' !in route &&
        route.substringBefore('?') !in NON_RESTORABLE_AUTHENTICATED_ROUTES.map { it.substringBefore('?') }.toSet()

private fun previewLiveBroadcast() = com.ssafy.dib.domain.live.LiveBroadcastSummary(
    liveBroadcastId = "preview-live",
    title = "오늘의 빈티지 컬렉션",
    description = "개발 미리보기에서 송출 준비와 Live 시작을 확인할 수 있어요.",
    status = "SCHEDULED",
    streamUrl = "preview://stream",
    scheduledAt = java.time.Instant.now().plusSeconds(3_600).toString(),
    viewCount = 0
)

private fun previewLiveAuctions() = listOf(
    com.ssafy.dib.domain.auction.AuctionSummary(
        auctionId = "preview-live-auction-1",
        sellerMemberId = "preview-member",
        productId = "preview-product-1",
        title = "빈티지 필름 카메라",
        categoryName = "디지털",
        currentPrice = 34_500,
        startPrice = 30_000,
        bidCount = 4,
        auctionTimeSeconds = 300,
        remainingSeconds = 300,
        status = "SCHEDULED",
        bookmarked = false
    ),
    com.ssafy.dib.domain.auction.AuctionSummary(
        auctionId = "preview-live-auction-2",
        sellerMemberId = "preview-member",
        productId = "preview-product-2",
        title = "수제 가죽 크로스백",
        categoryName = "패션",
        currentPrice = 28_000,
        startPrice = 28_000,
        bidCount = 0,
        auctionTimeSeconds = 600,
        remainingSeconds = 600,
        status = "SCHEDULED",
        bookmarked = false
    )
)

internal fun signupErrorMessage(error: ApiFailure): String = when (error.code) {
    ApiErrorCodes.CLIENT_NOT_CONFIGURED -> "개발 서버 주소가 설정되지 않았어요. 연결 설정을 확인해주세요."
    "INVALID_PHONE" -> "휴대폰 번호 형식을 확인해주세요."
    "RATE_LIMITED" -> "요청이 너무 많아요. 잠시 후 다시 시도해주세요."
    "INVALID_CODE" -> "인증번호가 올바르지 않아요."
    "VERIFICATION_EXPIRED" -> "인증 시간이 만료됐어요. 인증번호를 다시 요청해주세요."
    "ATTEMPTS_EXCEEDED" -> "인증 시도 횟수를 초과했어요. 인증번호를 다시 요청해주세요."
    "INVALID_EMAIL" -> "이메일 형식을 확인해주세요."
    "EMAIL_DUPLICATED" -> "이미 가입된 이메일이에요."
    "PHONE_DUPLICATED" -> "이미 가입된 휴대폰 번호예요."
    "NICKNAME_DUPLICATED" -> "이미 사용 중인 닉네임이에요. 다른 닉네임을 입력해주세요."
    "INVALID_PASSWORD" -> "비밀번호 조건을 확인해주세요."
    "INVALID_VERIFICATION" -> "휴대폰 인증이 만료됐어요. 다시 인증해주세요."
    "INVALID_VERIFICATION_ID" -> "인증 요청 정보가 올바르지 않아요. 인증번호를 다시 요청해주세요."
    "INVALID_RESET_TOKEN" -> "비밀번호 재설정 링크가 만료됐거나 이미 사용됐어요. 링크를 다시 요청해주세요."
    "ACCOUNT_NOT_FOUND", "MEMBER_NOT_FOUND" -> "입력한 정보와 일치하는 계정을 찾을 수 없어요."
    else -> error.message.ifBlank { "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요." }
}

internal fun auctionCommandError(error: ApiFailure): String = when (error.code) {
    "PRODUCT_NOT_OWNED", "NOT_MY_PRODUCT" -> "본인이 등록한 상품만 경매에 올릴 수 있어요."
    "PRODUCT_NOT_APPROVED" -> "검수가 끝난 상품만 경매를 시작할 수 있어요."
    "PRODUCT_PENDING" -> "검수 중인 상품이에요."
    "PRODUCT_ALREADY_LISTED", "PRODUCT_ON_AUCTION" -> "이미 다른 경매에 등록되거나 Live에 편성된 상품이에요."
    "PRODUCT_ALREADY_SOLD" -> "판매가 완료된 상품이에요."
    "PRODUCT_ALREADY_DELETED" -> "삭제된 상품이에요."
    "AUCTION_STARTED", "AUCTION_NOT_EDITABLE" -> "시작된 경매는 변경하거나 취소할 수 없어요."
    "AUCTION_NOT_FOUND" -> "경매를 찾을 수 없어요. 목록에서 다시 확인해주세요."
    "INVALID_AUCTION", "LIVE_RULES_INVALID" -> "시작가와 경매 시간을 다시 확인해주세요."
    "AUCTION_PRICE_REQUIRED" -> "시작가와 경매 시간을 먼저 정해주세요."
    "AUCTION_PRICE_INVALID" -> "시작가는 1,000원 이상이어야 해요."
    "AUCTION_SCHEDULE_INVALID" -> "경매 시간은 5분 이상이어야 해요. (라이브 편성 상품은 30초~5분)"
    "AUCTION_NOT_RELISTABLE" -> "유찰된 경매만 다시 올릴 수 있어요."
    else -> error.message.ifBlank { "경매 요청을 처리하지 못했어요." }
}

internal fun liveControlError(error: ApiFailure): String = when (error.code) {
    "STREAM_UNAVAILABLE" -> "송출 연결을 준비하지 못했어요. 잠시 후 다시 시도해주세요."
    "LIVE_ITEMS_EMPTY" -> "상품을 한 개 이상 편성한 뒤 방송을 시작해주세요."
    "LIVE_END_BLOCKED_BY_AUCTION" -> "진행 중인 경매가 끝난 뒤 방송을 종료할 수 있어요."
    "LIVE_INVALID_STATUS" -> "현재 방송 상태에서는 이 작업을 할 수 없어요."
    "LIVE_NOT_EDITABLE" -> "시작된 방송의 설정이나 상품 편성은 변경할 수 없어요."
    "LIVE_ITEMS_LIMIT_EXCEEDED" -> "Live에는 상품을 최대 10개까지 편성할 수 있어요."
    "PRODUCT_NOT_OWNED" -> "본인이 등록한 상품만 Live에 편성할 수 있어요."
    "PRODUCT_NOT_APPROVED" -> "검수가 승인된 상품만 Live에 편성할 수 있어요."
    "PRODUCT_ALREADY_LISTED" -> "이미 다른 경매에 등록되거나 Live에 편성된 상품이에요."
    "LIVE_AUCTION_ALREADY_ACTIVE" -> "현재 경매가 끝난 뒤 다음 상품 경매를 시작해주세요."
    "LIVE_AUCTION_ALREADY_PROCESSED" -> "이미 시작되었거나 종료된 경매예요. 다른 예정 경매를 선택해주세요."
    "LIVE_RULES_INVALID" -> "Live 경매의 시작가와 진행 시간을 확인해주세요."
    "AUCTION_PRICE_REQUIRED" -> "시작가와 경매 시간을 먼저 정해주세요."
    "AUCTION_PRICE_INVALID" -> "시작가는 1,000원 이상이어야 해요."
    "AUCTION_SCHEDULE_INVALID" -> "라이브 경매 시간은 30초~5분이어야 해요."
    "AUCTION_NOT_ACTIVE" -> "선택한 경매를 시작할 수 있는 상태가 아니에요."
    "NOT_BROADCASTER" -> "이 방송을 관리할 권한이 없어요."
    else -> error.message.ifBlank { "Live 요청을 처리하지 못했어요." }
}

internal fun reportSubmissionMessage(error: ApiFailure): String = when (error.code) {
    ApiErrorCodes.CLIENT_NOT_CONFIGURED -> "개발 서버 주소가 설정되지 않았어요."
    "SELF_REPORT_NOT_ALLOWED" -> "본인은 신고할 수 없어요."
    "DUPLICATE_REPORT" -> "이미 접수된 신고가 있어요."
    "AUCTION_NOT_FOUND" -> "신고할 경매를 찾을 수 없어요."
    "MEMBER_NOT_FOUND" -> "신고할 회원을 찾을 수 없어요."
    "LIVE_NOT_FOUND" -> "신고할 Live 방송을 찾을 수 없어요."
    "ORDER_NOT_FOUND" -> "신고할 거래를 찾을 수 없어요."
    "ORDER_ON_HOLD" -> ORDER_HOLD_BLOCK_MESSAGE
    else -> error.message.ifBlank { "신고를 접수하지 못했어요. 잠시 후 다시 시도해주세요." }
}

internal fun productSubmissionMessage(error: ApiFailure): String = when (error.code) {
    ApiErrorCodes.CLIENT_NOT_CONFIGURED -> "개발 서버 주소가 설정되지 않았어요."
    "IMAGE_REQUIRED" -> "상품 사진을 한 장 이상 선택해주세요."
    "INVALID_CONTENT_TYPE" -> "지원하지 않는 사진 형식이 포함돼 있어요."
    "FILE_TOO_LARGE" -> "용량이 너무 큰 사진이 포함돼 있어요."
    "FILE_COUNT_EXCEEDED" -> "상품 사진은 최대 10장까지 등록할 수 있어요."
    "S3_UPLOAD_FAILED" -> "사진 업로드에 실패했어요. 다시 시도해주세요."
    "DUPLICATE_REQUEST" -> "이미 처리된 상품 등록 요청이에요."
    else -> error.message.ifBlank { "상품을 등록하지 못했어요. 잠시 후 다시 시도해주세요." }
}

internal fun paymentMethodRegistrationErrorMessage(error: ApiFailure): String = when (error.code) {
    "BILLING_KEY_ISSUE_FAILED" -> "카드 인증 정보를 확인하지 못했어요. 잠시 후 다시 등록해주세요."
    "PAYMENT_METHOD_ALREADY_EXISTS" -> "이미 등록된 카드가 있어요. 기존 카드를 삭제한 뒤 다시 시도해주세요."
    ApiErrorCodes.NETWORK_UNAVAILABLE -> "네트워크 연결을 확인한 뒤 다시 시도해주세요."
    ApiErrorCodes.CLIENT_NOT_CONFIGURED -> "개발 서버 주소가 설정되지 않았어요."
    else -> error.message.ifBlank { "카드를 등록하지 못했어요. 다시 시도해주세요." }
}
