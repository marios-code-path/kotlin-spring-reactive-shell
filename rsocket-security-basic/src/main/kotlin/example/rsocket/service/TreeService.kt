package example.rsocket.service

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TreeService {
    fun shakeForLeaf(): Mono<String>
    fun rakeForLeaves(): Flux<String>

    companion object {
        val LEAF_COLORS = listOf("Green", "Yellow", "Orange", "Brown", "Red")
    }
}
