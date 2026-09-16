package com.ssafy.dib.data.remote.socket

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// 기존 SocketCommands 봉투(SUBSCRIBE_AUCTION, PLACE_BID …)를 STOMP destination 으로 옮긴다.
// 덕분에 AuctionRealtimeConnection / OrderChatConnection / LiveChatConnection 은 그대로 두고 클라이언트만 STOMP 로 바뀐다.
//
//  백엔드 destination (Spring STOMP, prefix /app 전송 · /topic 브로드캐스트 · /user/queue 개인)
//   경매   구독 /topic/auctions/{id}  /user/queue/auction  /app/auctions/{id}/snapshot(1회 응답)   전송 /app/auctions/{id}/bids {commandId, amount}
//   주문   구독 /topic/orders/{id}    /user/queue/orders   /app/orders/{id}/snapshot                전송 /app/orders/{id}/messages {commandId, content}
//   알림   구독 /user/queue/notifications
//   라이브 구독 /topic/live/{id}      /user/queue/live     /app/live/{id}/snapshot                  전송 /app/live/{id}/messages   (라이브 담당 백엔드 확정 전 — 이름만 예약)
sealed class StompAction {
    data class Subscribe(val destinations: List<String>) : StompAction()
    data class Unsubscribe(val destinations: List<String>) : StompAction()
    data class Send(val destination: String, val body: JsonObject) : StompAction()
    object None : StompAction()
}

object StompRouting {
    const val USER_QUEUE_AUCTION = "/user/queue/auction"
    const val USER_QUEUE_ORDERS = "/user/queue/orders"
    const val USER_QUEUE_NOTIFICATIONS = "/user/queue/notifications"
    const val USER_QUEUE_LIVE = "/user/queue/live"

    fun auctionTopic(id: String) = "/topic/auctions/$id"
    fun auctionSnapshot(id: String) = "/app/auctions/$id/snapshot"
    fun auctionBids(id: String) = "/app/auctions/$id/bids"
    fun orderTopic(id: String) = "/topic/orders/$id"
    fun orderSnapshot(id: String) = "/app/orders/$id/snapshot"
    fun orderMessages(id: String) = "/app/orders/$id/messages"
    fun liveTopic(id: String) = "/topic/live/$id"
    fun liveSnapshot(id: String) = "/app/live/$id/snapshot"
    fun liveMessages(id: String) = "/app/live/$id/messages"

    fun route(envelope: SocketEnvelope): StompAction {
        val p = envelope.payload
        fun id(name: String): String? = (p[name] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
        return when (envelope.eventType) {
            SocketEventTypes.SUBSCRIBE_AUCTION -> id("auctionId")?.let {
                StompAction.Subscribe(listOf(auctionTopic(it), USER_QUEUE_AUCTION, auctionSnapshot(it)))
            } ?: StompAction.None
            SocketEventTypes.SYNC_AUCTION -> id("auctionId")?.let {
                StompAction.Subscribe(listOf(auctionSnapshot(it)))   // 스냅샷은 SUBSCRIBE 마다 1회 응답 → 재구독으로 재동기화
            } ?: StompAction.None
            SocketEventTypes.UNSUBSCRIBE_AUCTION -> id("auctionId")?.let {
                StompAction.Unsubscribe(listOf(auctionTopic(it), auctionSnapshot(it)))
            } ?: StompAction.None
            SocketEventTypes.PLACE_BID -> id("auctionId")?.let {
                StompAction.Send(auctionBids(it), pick(p, "commandId", "amount"))
            } ?: StompAction.None
            SocketEventTypes.SUBSCRIBE_ORDER -> id("orderId")?.let {
                StompAction.Subscribe(listOf(orderTopic(it), USER_QUEUE_ORDERS, orderSnapshot(it)))
            } ?: StompAction.None
            SocketEventTypes.SEND_CHAT_MESSAGE -> id("orderId")?.let {
                StompAction.Send(orderMessages(it), pick(p, "commandId", "content"))
            } ?: StompAction.None
            SocketEventTypes.SUBSCRIBE_NOTIFICATIONS -> StompAction.Subscribe(listOf(USER_QUEUE_NOTIFICATIONS))
            SocketEventTypes.SUBSCRIBE_LIVE -> id("liveBroadcastId")?.let {
                StompAction.Subscribe(listOf(liveTopic(it), USER_QUEUE_LIVE, liveSnapshot(it)))
            } ?: StompAction.None
            SocketEventTypes.UNSUBSCRIBE_LIVE -> id("liveBroadcastId")?.let {
                StompAction.Unsubscribe(listOf(liveTopic(it), liveSnapshot(it)))
            } ?: StompAction.None
            SocketEventTypes.SEND_LIVE_CHAT -> id("liveBroadcastId")?.let {
                StompAction.Send(liveMessages(it), pick(p, "commandId", "content"))
            } ?: StompAction.None
            SocketEventTypes.PING -> StompAction.None   // STOMP heart-beat 가 대신한다
            else -> StompAction.None
        }
    }

    private fun pick(p: JsonObject, vararg keys: String): JsonObject {
        val m = LinkedHashMap<String, JsonElement>()
        keys.forEach { k -> p[k]?.let { m[k] = it } }
        return JsonObject(m)
    }
}
