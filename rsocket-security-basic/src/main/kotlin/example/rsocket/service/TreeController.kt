package example.rsocket.service

import example.rsocket.domain.TreeSpecies
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TreeControllerMapping : TreeService {
    @MessageMapping("shake")
    override fun shakeForLeaf(): Mono<String>

    @MessageMapping("rake")
    override fun rakeForLeaves(): Flux<String>

    @MessageMapping("species")
    override fun variety(species: TreeSpecies): Mono<TreeSpecies>

    @MessageMapping("status/{id}")
    fun status(@AuthenticationPrincipal user: Mono<UserDetails>, @DestinationVariable id: String): Mono<String> =
            user.hasElement().map(Boolean::toString).apply { println("ID = $id")}
}