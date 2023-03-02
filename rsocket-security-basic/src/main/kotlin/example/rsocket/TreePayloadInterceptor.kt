package example.rsocket

import org.springframework.core.Ordered
import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.rsocket.api.PayloadExchange
import org.springframework.security.rsocket.api.PayloadInterceptor
import org.springframework.security.rsocket.api.PayloadInterceptorChain
import reactor.core.publisher.Mono

class TreePayloadInterceptor : PayloadInterceptor, Ordered {
    override fun intercept(payload: PayloadExchange, chain: PayloadInterceptorChain): Mono<Void> {
        return ReactiveSecurityContextHolder
                .getContext()
                .doOnNext {
                    println("User: ${it.authentication.principal}")
                    println("Authorities: ${it.authentication.authorities}")
                }
                .then(chain.next(payload))
    }

    override fun getOrder(): Int = HIGHEST_PRECEDENCE
}
