package example.rsocket.service

import org.springframework.security.access.prepost.PreAuthorize
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TreeServiceSecurity : TreeService {

    @PreAuthorize("hasRole('SHAKE')")
    override fun shakeForLeaf(): Mono<String>

    @PreAuthorize("hasRole('RAKE')")
    override fun rakeForLeaves(): Flux<String>
}