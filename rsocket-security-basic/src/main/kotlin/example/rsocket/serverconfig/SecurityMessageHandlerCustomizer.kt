package example.rsocket.serverconfig

import org.springframework.boot.autoconfigure.rsocket.RSocketMessageHandlerCustomizer
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver

// TODO This is already customized in RSocketSecurityAutoConfiguration
class SecurityMessageHandlerCustomizer : RSocketMessageHandlerCustomizer {
    override fun customize(messageHandler: RSocketMessageHandler) {
        val ar: HandlerMethodArgumentResolver = AuthenticationPrincipalArgumentResolver()
        messageHandler.argumentResolverConfigurer.addCustomResolver(ar)
    }
}