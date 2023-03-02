package example.rsocket.service

import example.rsocket.domain.TreeSpecies
import example.rsocket.service.TreeService.Companion.LEAF_COLORS
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.stream.Stream
import kotlin.random.Random

class TreeServiceImpl : TreeService {

    override fun shakeForLeaf(): Mono<String> = Mono.just(LEAF_COLORS[Random.nextInt(LEAF_COLORS.size)])

    override fun rakeForLeaves(): Flux<String> = Flux
            .fromStream(
                    Stream.generate { Random.nextInt(LEAF_COLORS.size) }
                            .limit(10)
            ).map { LEAF_COLORS[it] }

    override fun variety(species: TreeSpecies): Mono<TreeSpecies> = Mono.just(TreeSpecies(species.id!!, species.leaf!!.uppercase()))
}