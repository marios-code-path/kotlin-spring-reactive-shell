package example.rsocket.service

import org.springframework.security.access.annotation.Secured
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TreeServiceSecurity : TreeService {

    @Secured("ROLE_SHAKE")
    override fun shakeForLeaf(): Mono<String>

    @Secured("ROLE_RAKE")
    override fun rakeForLeaves(): Flux<String>
}