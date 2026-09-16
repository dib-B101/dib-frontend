package com.ssafy.dib.data.remote.socket

// STOMP 1.2 프레임. 백엔드가 Spring STOMP(SockJS 아님, 순수 WebSocket) 이라 프레임을 직접 만든다.
// 형식: COMMAND\nheader:value\n...\n\nbody\u0000   /  heart-beat 는 "\n" 한 글자
data class StompFrame(
    val command: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
) {
    fun header(name: String): String? = headers[name]

    fun encode(): String = buildString {
        append(command).append('\n')
        headers.forEach { (k, v) -> append(escape(k)).append(':').append(escape(v)).append('\n') }
        if (body.isNotEmpty() && !headers.containsKey("content-length")) {
            append("content-length:").append(body.toByteArray(Charsets.UTF_8).size).append('\n')
        }
        append('\n').append(body).append(NULL)
    }

    companion object {
        const val NULL = '\u0000'
        const val HEARTBEAT = "\n"

        const val CONNECT = "CONNECT"
        const val CONNECTED = "CONNECTED"
        const val SUBSCRIBE = "SUBSCRIBE"
        const val UNSUBSCRIBE = "UNSUBSCRIBE"
        const val SEND = "SEND"
        const val MESSAGE = "MESSAGE"
        const val ERROR = "ERROR"
        const val RECEIPT = "RECEIPT"
        const val DISCONNECT = "DISCONNECT"

        fun isHeartbeat(raw: String): Boolean = raw.isEmpty() || raw == "\n" || raw == "\r\n"

        // 한 텍스트 메시지 = 프레임 하나. 헤더 끝은 빈 줄, 본문 끝은 NULL
        fun decode(raw: String): StompFrame {
            val text = raw.trimEnd(NULL)
            val headerEnd = text.indexOf("\n\n").let { if (it >= 0) it else text.indexOf("\r\n\r\n") }
            val headerBlock = if (headerEnd >= 0) text.substring(0, headerEnd) else text
            val body = if (headerEnd >= 0) text.substring(headerEnd).trimStart('\r', '\n') else ""
            val lines = headerBlock.split('\n').map { it.trimEnd('\r') }
            val command = lines.firstOrNull()?.trim().orEmpty()
            val headers = LinkedHashMap<String, String>()
            lines.drop(1).forEach { line ->
                val idx = line.indexOf(':')
                if (idx > 0) {
                    val key = unescape(line.substring(0, idx))
                    if (!headers.containsKey(key)) headers[key] = unescape(line.substring(idx + 1))   // 같은 키는 첫 값이 유효 (STOMP 규격)
                }
            }
            return StompFrame(command, headers, body)
        }

        private fun escape(v: String): String =
            v.replace("\\", "\\\\").replace("\n", "\\n").replace(":", "\\c").replace("\r", "\\r")

        private fun unescape(v: String): String {
            val sb = StringBuilder()
            var i = 0
            while (i < v.length) {
                val c = v[i]
                if (c == '\\' && i + 1 < v.length) {
                    when (v[i + 1]) {
                        'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 'c' -> sb.append(':'); '\\' -> sb.append('\\')
                        else -> sb.append(c).append(v[i + 1])
                    }
                    i += 2
                } else {
                    sb.append(c); i++
                }
            }
            return sb.toString()
        }
    }
}
