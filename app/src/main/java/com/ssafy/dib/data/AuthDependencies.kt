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
import com.ssafy.dib.data.remote.socket.AuctionRealtimeConnection
import com.ssafy.dib.data.remote.socket.DibWebSocketClient
import com.ssafy.dib.data.repository.AuctionRepositoryImpl
import com.ssafy.dib.data.repository.BidDepositRepositoryImpl
import com.ssafy.dib.data.repository.AuthRepositoryImpl
import com.ssafy.dib.data.repository.OrderRepositoryImpl
import com.ssafy.dib.data.repository.InquiryRepositoryImpl
import com.ssafy.dib.data.repository.ReportRepositoryImpl
import com.ssafy.dib.data.repository.ProductRepositoryImpl
import com.ssafy.dib.domain.auction.AuctionRepository
import com.ssafy.dib.domain.auction.BidDepositRepository
import com.ssafy.dib.domain.auth.AuthRepository
import com.ssafy.dib.domain.order.OrderRepository
import com.ssafy.dib.domain.support.InquiryRepository
import com.ssafy.dib.domain.report.ReportRepository
import com.ssafy.dib.domain.product.ProductRepository
import java.util.UUID

class AuthDependencies(context: Context) {
    private val appContext = context.applicationContext
    private val sessionStore = SecureAuthSessionStore(appContext)
    private val deviceStore = DeviceIdentityStore(appContext)
    private val guestPreferences = appContext.getSharedPreferences("dib_guest", Context.MODE_PRIVATE)

    val networkConfig: NetworkConfig = NetworkConfig.fromBuildConfig()
    val deviceId: String = deviceStore.getOrCreate()
    val repository: AuthRepository
    val auctionRepository: AuctionRepository
    val bidDepositRepository: BidDepositRepository
    val orderRepository: OrderRepository
    val inquiryRepository: InquiryRepository
    val reportRepository: ReportRepository
    val productRepository: ProductRepository

    init {
        val client = DibHttpClient(
            config = networkConfig,
            accessTokenProvider = AccessTokenProvider { sessionStore.read()?.accessToken },
            guestSessionProvider = GuestSessionProvider { guestSessionId() }
        )
        repository = AuthRepositoryImpl(AuthRemoteDataSource(client), sessionStore)
        auctionRepository = AuctionRepositoryImpl(AuctionRemoteDataSource(client))
        bidDepositRepository = BidDepositRepositoryImpl(BidDepositRemoteDataSource(client))
        orderRepository = OrderRepositoryImpl(OrderRemoteDataSource(client))
        inquiryRepository = InquiryRepositoryImpl(InquiryRemoteDataSource(client))
        reportRepository = ReportRepositoryImpl(ReportRemoteDataSource(client))
        productRepository = ProductRepositoryImpl(ProductRemoteDataSource(client))
    }

    private fun guestSessionId(): String {
        guestPreferences.getString(KEY_GUEST_SESSION_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also { generated ->
            guestPreferences.edit().putString(KEY_GUEST_SESSION_ID, generated).apply()
        }
    }

    fun createAuctionRealtimeConnection() = AuctionRealtimeConnection(
        DibWebSocketClient(
            config = networkConfig,
            accessTokenProvider = AccessTokenProvider { sessionStore.read()?.accessToken },
            guestSessionProvider = GuestSessionProvider { guestSessionId() }
        )
    )

    private companion object {
        const val KEY_GUEST_SESSION_ID = "guest_session_id"
    }
}
