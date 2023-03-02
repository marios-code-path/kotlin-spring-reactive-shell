package example.rsocket.clientconfig

import io.rsocket.metadata.WellKnownMimeType
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata
import org.springframework.util.MimeTypeUtils

// OK > do not user the same builder to create multiple requesters
// the builder keeps setupmetadata and setupdata and passes it to each requester
// so the first requester will have the setupdata and setupmetadata of the second requester
// even if the second requester does not have any setupdata or setupmetadata
open class RequesterFactory(
        private val builder: RSocketRequester.Builder,
        private val port: String) {
    companion object {
        val SIMPLE_AUTH = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string)
    }

    open fun requester(): RSocketRequester =
            builder
                    .tcp("localhost", port.toInt())


    open fun requester(username: String, password: String): RSocketRequester =
            builder
                    .setupMetadata(UsernamePasswordMetadata(username, password), SIMPLE_AUTH)
                    .tcp("localhost", port.toInt())
}