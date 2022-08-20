package com.example.telephone.config

import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import reactor.core.publisher.Mono

class ReactiveAuth(private val userDetailService: MapReactiveUserDetailsService) : ReactiveAuthenticationManager {
    override fun authenticate(authen: Authentication): Mono<Authentication> {
        val credential = authen.credentials.toString()

        return userDetailService
                .findByUsername(authen.name)
                .switchIfEmpty(Mono.error(Exception("User Not Found")))
                .map { user ->
                    if (!user.password.equals(credential, true))
                        throw Exception("User Authentication Error.")
                    UsernamePasswordAuthenticationToken(user, user.password, user.authorities)
                }
    }
}
