package example.rsocket.clientconfig

import io.rsocket.metadata.WellKnownMimeType
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata
import org.springframework.util.MimeTypeUtils

open class RequesterFactory(private val port: String,
                            private val builder: RSocketRequester.Builder) {
    companion object {
        val SIMPLE_AUTH = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string)
    }

    open fun requester(username: String, password: String): RSocketRequester =
            builder
                    .rsocketStrategies { strategiesBuilder ->
                        strategiesBuilder.encoder(SimpleAuthenticationEncoder())
                    }
                    .setupMetadata(UsernamePasswordMetadata(username, password), SIMPLE_AUTH)
                    .connectTcp("localhost", port.toInt())
                    .block()!!
                    .apply {
                        rsocket()!!
                                .onClose()
                                .doOnError { println("CLOSED") }
                                .doFinally { println("DISCONNECTED") }
                    }
}