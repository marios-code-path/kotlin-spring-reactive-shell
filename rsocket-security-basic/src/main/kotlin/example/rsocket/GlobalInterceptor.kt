package example.rsocket

import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.metadata.WellKnownMimeType
import io.rsocket.plugins.RSocketInterceptor
import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.MediaType
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Mono


class GlobalInterceptor(private val strategies: RSocketStrategies) : RSocketInterceptor, Ordered {

    fun getRoute(payload: Payload): String {
        val metadataMimetype = MimeTypeUtils
                .parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string)
        val metadata = strategies.metadataExtractor().extract(payload, metadataMimetype)
        val route = metadata["route"]
        if (metadata.containsKey("route")) return route.toString()

        return ""
    }

    fun getMapOfPayloadData(payload: Payload): Map<String, Any> =
            try {
                val data = DefaultDataBufferFactory().wrap(payload.data)
                val decoder = strategies.decoder<Map<String, Any>>(ResolvableType.forClass(Map::class.java), MediaType.APPLICATION_CBOR)
                decoder.decode(data, ResolvableType.forClass(Map::class.java), MediaType.APPLICATION_CBOR, emptyMap()) as Map<String, Any>
            } catch (e: Exception) {
                mapOf()
            }


    override fun apply(rSocket: RSocket): RSocket {
        return object : RSocket {
            override fun requestResponse(payload: Payload): Mono<Payload> {

                val ctxAdd = mutableMapOf<String, String>()

                if (payload.data.hasRemaining()) {
                    val decoded = getMapOfPayloadData(payload)

                    decoded.keys.forEach() { dataType ->
                        when (dataType) {
                            "TreeSpecies" -> {
                                val thisData = decoded[dataType] as Map<*, *>
                                ctxAdd["id"] = thisData["id"].toString()
                                if (thisData["leaf"].toString().lowercase() == "oak") {
                                    throw Exception("Oak is not allowed")
                                }
                            }

                        }
                    }
                }
                return rSocket
                        .requestResponse(payload)
                        .contextWrite { ctx ->
                            ctx.putAllMap(ctxAdd)
                        }
            }
        }
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

}