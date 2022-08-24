package example.rsocket.service

import org.springframework.messaging.handler.annotation.MessageMapping
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TreeControllerMapping : TreeService {
    @MessageMapping("shake")
    override fun shakeForLeaf(): Mono<String>

    @MessageMapping("rake")
    override fun rakeForLeaves(): Flux<String>
}
