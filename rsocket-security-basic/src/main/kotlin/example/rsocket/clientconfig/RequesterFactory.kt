package example.rsocket.clientconfig

import io.rsocket.metadata.WellKnownMimeType
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata
import org.springframework.util.MimeTypeUtils

open class RequesterFactory(
        private val builder: RSocketRequester.Builder,
        private val port: String) {
    companion object {
        val SIMPLE_AUTH = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string)
    }

    open fun requester(): RSocketRequester =
            builder
                    .connectTcp("localhost", port.toInt())
                    .block()!!

    open fun requester(username: String, password: String): RSocketRequester =
            builder

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