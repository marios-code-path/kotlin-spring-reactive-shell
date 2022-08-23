package example.rsocket.security

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TreeService {
    fun shakeForLeaf() : Mono<String>
    fun rakeForLeaves(): Flux<String>
}