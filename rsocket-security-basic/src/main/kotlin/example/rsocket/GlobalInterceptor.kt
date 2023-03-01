package example.rsocket

import example.rsocket.domain.TreeSpecies
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.plugins.RSocketInterceptor
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.messaging.rsocket.RSocketStrategies
import reactor.core.publisher.Mono

class GlobalInterceptor(private val strategies: RSocketStrategies) : RSocketInterceptor {
    override fun apply(t: RSocket?): RSocket {
        val r = object : RSocket {
            override fun requestResponse(payload: Payload): Mono<Payload> {

                if(payload.data.hasRemaining()) {
                    val data = DefaultDataBufferFactory().wrap(payload.data)
                    val decoder = strategies.decoder<TreeSpecies>(ResolvableType.forClass(TreeSpecies::class.java), null)
                    val treeSpecies = decoder.decode(data, ResolvableType.forClass(TreeSpecies::class.java), null, null)

                    if (treeSpecies?.leaf == "Oak") {
                        return Mono.error(RuntimeException("Oak is not allowed"))
                    }
                }

                return t!!.requestResponse(payload)
            }
        }

        return r
    }

}