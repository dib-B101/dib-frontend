package com.ssafy.dib.data

import android.content.Context
import com.ssafy.dib.core.network.AccessTokenProvider
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.core.network.GuestSessionProvider
import com.ssafy.dib.core.network.NetworkConfig
import com.ssafy.dib.data.local.auth.DeviceIdentityStore
import com.ssafy.dib.data.local.auth.SecureAuthSessionStore
import com.ssafy.dib.data.remote.auth.AuthRemoteDataSource
import com.ssafy.dib.data.remote.auction.AuctionRemoteDataSource
import com.ssafy.dib.data.remote.auction.BidDepositRemoteDataSource
import com.ssafy.dib.data.remote.order.OrderRemoteDataSource
import com.ssafy.dib.data.remote.support.InquiryRemoteDataSource
import com.ssafy.dib.data.remote.report.ReportRemoteDataSource
import com.ssafy.dib.data.remote.product.ProductRemoteDataSource
import com.ssafy.dib.data.remote.payment.PaymentRemoteDataSource
import com.ssafy.dib.data.remote.member.MemberRemoteDataSource
import com.ssafy.dib.data.remote.member.AddressRemoteDataSource
import com.ssafy.dib.data.remote.live.LiveRemoteDataSource
import com.ssafy.dib.data.remote.settlement.SettlementAccountRemoteDataSource
import com.ssafy.dib.data.remote.settlement.SettlementRemoteDataSource
import com.ssafy.dib.data.remote.socket.AuctionRealtimeConnection
import com.ssafy.dib.data.remote.socket.DibWebSocketClient
import com.ssafy.dib.data.remote.socket.OrderChatConnection
import com.ssafy.dib.data.remote.socket.LiveChatConnection
import com.ssafy.dib.data.remote.socket.DomainNotificationConnection
import com.ssafy.dib.data.repository.AuctionRepositoryImpl
import com.ssafy.dib.data.repository.BidDepositRepositoryImpl
import com.ssafy.dib.data.repository.AuthRepositoryImpl
import com.ssafy.dib.data.repository.OrderRepositoryImpl
import com.ssafy.dib.data.repository.InquiryRepositoryImpl
import com.ssafy.dib.data.repository.ReportRepositoryImpl
import com.ssafy.dib.data.repository.ProductRepositoryImpl
import com.ssafy.dib.data.repository.PaymentRepositoryImpl
import com.ssafy.dib.data.repository.MemberRepositoryImpl
import com.ssafy.dib.data.repository.AddressRepositoryImpl
import com.ssafy.dib.data.repository.LiveRepositoryImpl
import com.ssafy.dib.data.repository.SettlementAccountRepositoryImpl
import com.ssafy.dib.data.repository.SettlementRepositoryImpl
import com.ssafy.dib.data.repository.SessionTokenRefresher
import com.ssafy.dib.domain.auction.AuctionRepository
import com.ssafy.dib.domain.auction.BidDepositRepository
import com.ssafy.dib.domain.auth.AuthRepository
import com.ssafy.dib.domain.order.OrderRepository
import com.ssafy.dib.domain.support.InquiryRepository
import com.ssafy.dib.domain.report.ReportRepository
import com.ssafy.dib.domain.product.ProductRepository
import com.ssafy.dib.domain.payment.PaymentRepository
import com.ssafy.dib.domain.member.MemberRepository
import com.ssafy.dib.domain.member.AddressRepository
import com.ssafy.dib.domain.live.LiveRepository
import com.ssafy.dib.domain.settlement.SettlementAccountRepository
import com.ssafy.dib.domain.settlement.SettlementRepository
import java.util.UUID

class AuthDependencies(context: Context) {
    private val appContext = context.applicationContext
    private val sessionStore = SecureAuthSessionStore(appContext)
    private val deviceStore = DeviceIdentityStore(appContext)
    private val guestPreferences = appContext.getSharedPreferences("dib_guest", Context.MODE_PRIVATE)
    private val paymentPreferences = appContext.getSharedPreferences("dib_payment", Context.MODE_PRIVATE)

    val networkConfig: NetworkConfig = NetworkConfig.fromBuildConfig()
    val deviceId: String = deviceStore.getOrCreate()
    val repository: AuthRepository
    val auctionRepository: AuctionRepository
    val bidDepositRepository: BidDepositRepository
    val orderRepository: OrderRepository
    val inquiryRepository: InquiryRepository
    val reportRepository: ReportRepository
    val productRepository: ProductRepository
    val paymentRepository: PaymentRepository
    val memberRepository: MemberRepository
    val addressRepository: AddressRepository
    val liveRepository: LiveRepository
    val settlementAccountRepository: SettlementAccountRepository
    val settlementRepository: SettlementRepository

    init {
        val refreshRemote = AuthRemoteDataSource(
            DibHttpClient(
                config = networkConfig,
                accessTokenProvider = AccessTokenProvider { null },
                guestSessionProvider = GuestSessionProvider { null }
            )
        )
        val tokenRefresher = SessionTokenRefresher(
            sessionStore = sessionStore,
            deviceId = deviceId,
            refreshRequest = refreshRemote::refresh
        )
        val client = DibHttpClient(
            config = networkConfig,
            accessTokenProvider = AccessTokenProvider { sessionStore.read()?.accessToken },
            guestSessionProvider = GuestSessionProvider { guestSessionId() },
            tokenRefresher = tokenRefresher
        )
        repository = AuthRepositoryImpl(AuthRemoteDataSource(client), sessionStore)
        auctionRepository = AuctionRepositoryImpl(AuctionRemoteDataSource(client))
        bidDepositRepository = BidDepositRepositoryImpl(BidDepositRemoteDataSource(client))
        orderRepository = OrderRepositoryImpl(OrderRemoteDataSource(client))
        inquiryRepository = InquiryRepositoryImpl(InquiryRemoteDataSource(client))
        reportRepository = ReportRepositoryImpl(ReportRemoteDataSource(client))
        productRepository = ProductRepositoryImpl(ProductRemoteDataSource(client))
        paymentRepository = PaymentRepositoryImpl(PaymentRemoteDataSource(client))
        memberRepository = MemberRepositoryImpl(MemberRemoteDataSource(client))
        addressRepository = AddressRepositoryImpl(AddressRemoteDataSource(client))
        liveRepository = LiveRepositoryImpl(LiveRemoteDataSource(client))
        settlementAccountRepository = SettlementAccountRepositoryImpl(SettlementAccountRemoteDataSource(client))
        settlementRepository = SettlementRepositoryImpl(SettlementRemoteDataSource(client))
    }

    private fun guestSessionId(): String {
        guestPreferences.getString(KEY_GUEST_SESSION_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also { generated ->
            guestPreferences.edit().putString(KEY_GUEST_SESSION_ID, generated).apply()
        }
    }

    fun paymentCustomerKey(): String {
        val memberId = sessionStore.read()?.memberId ?: return ""
        val preferenceKey = "customer_key_$memberId"
        paymentPreferences.getString(preferenceKey, null)?.let { return it }
        return UUID.randomUUID().toString().also { generated ->
            paymentPreferences.edit().putString(preferenceKey, generated).apply()
        }
    }

    fun createAuctionRealtimeConnection() = AuctionRealtimeConnection(
        DibWebSocketClient(
            config = networkConfig,
            accessTokenProvider = AccessTokenProvider { sessionStore.read()?.accessToken },
            guestSessionProvider = GuestSessionProvider { guestSessionId() }
        )
    )

    fun createOrderChatConnection() = OrderChatConnection(
        DibWebSocketClient(
            config = networkConfig,
            accessTokenProvider = AccessTokenProvider { sessionStore.read()?.accessToken },
            guestSessionProvider = GuestSessionProvider { null }
        )
    )

    fun createLiveChatConnection() = LiveChatConnection(
        DibWebSocketClient(
            config = networkConfig,
            accessTokenProvider = AccessTokenProvider { sessionStore.read()?.accessToken },
            guestSessionProvider = GuestSessionProvider { guestSessionId() }
        )
    )

    fun createDomainNotificationConnection() = DomainNotificationConnection(
        DibWebSocketClient(
            config = networkConfig,
            accessTokenProvider = AccessTokenProvider { sessionStore.read()?.accessToken },
            guestSessionProvider = GuestSessionProvider { null }
        )
    )

    private companion object {
        const val KEY_GUEST_SESSION_ID = "guest_session_id"
    }
}
