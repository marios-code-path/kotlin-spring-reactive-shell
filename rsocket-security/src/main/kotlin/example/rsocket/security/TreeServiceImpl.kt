package example.rsocket.security

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.stream.Stream
import kotlin.random.Random

class TreeServiceImpl : TreeService {

    val colors = listOf<String>("Green", "Yellow", "Orange", "Brown", "Red")
    private val randomizer = Random

    override fun shakeForLeaf(): Mono<String> = Mono.just(colors.get(Random.nextInt(colors.size)))

    override fun rakeForLeaves(): Flux<String> = Flux.fromStream(
            Stream.generate { Random.nextInt(colors.size) }
                    .limit(10)
    )
            .map { colors[it] }
}