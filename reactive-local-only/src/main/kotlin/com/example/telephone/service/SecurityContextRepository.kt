package com.example.telephone.service

import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import reactor.core.publisher.Mono


class SecurityContextRepository {
    private var securityContext: SecurityContext? = null

    fun save(secCtx: SecurityContext): Mono<SecurityContext> = Mono.create { sink ->
        securityContext = secCtx
        sink.success(securityContext)
    }

    fun load(): Mono<SecurityContext> = Mono
            .just(securityContext!!)
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext!!)))
}