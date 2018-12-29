package fredboat.ws

import com.google.gson.Gson
import fredboat.audio.player.GuildPlayer
import fredboat.sentinel.Guild
import fredboat.sentinel.GuildCache
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import java.util.regex.Pattern

class UserSession(
        val session: WebSocketSession,
        private val guildCache: GuildCache,
        private val gson: Gson
) : WebSocketSession by session {

    companion object {
        private val expctedPath = Pattern.compile("/playerinfo/(\\d+)/?")
    }

    @Volatile
    private lateinit var sink: FluxSink<WebSocketMessage>
    var isOpen = true
    val guildId = expctedPath.matcher(handshakeInfo.uri.path).run { find(); group(1) }.toLong()
    val guild: Guild? get() = guildCache.getIfCached(guildId)
    val player: GuildPlayer? get() = guild?.guildPlayer
    fun sendJson(payload: Any) {
        sink.next(textMessage(gson.toJson(payload)))
    }
    fun send(payload: String) {
        sink.next(textMessage(payload))
    }
    fun send(message: WebSocketMessage) {
        sink.next(message)
    }

    /** Grants us a hook to send messages with */
    fun initSendStream(): Mono<Void> {
        return session.send(Flux.create { sink = it })
                .doFinally { isOpen = false }
    }
}