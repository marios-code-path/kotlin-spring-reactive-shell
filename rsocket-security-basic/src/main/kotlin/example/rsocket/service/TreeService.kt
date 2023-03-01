package example.rsocket.service

import example.rsocket.domain.TreeSpecies
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TreeService {
    fun shakeForLeaf(): Mono<String>
    fun rakeForLeaves(): Flux<String>
    fun variety(species: TreeSpecies): Mono<TreeSpecies>

    companion object {
        val LEAF_COLORS = listOf("Green", "Yellow", "Orange", "Brown", "Red")
    }
}
