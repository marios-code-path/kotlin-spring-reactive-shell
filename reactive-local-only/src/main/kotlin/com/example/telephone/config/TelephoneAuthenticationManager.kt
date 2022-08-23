package com.example.telephone.config

import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import reactor.core.publisher.Mono

class TelephoneAuthenticationManager(private val userDetailService: MapReactiveUserDetailsService,
                                     private val encoder: PasswordEncoder) : ReactiveAuthenticationManager {
    override fun authenticate(authen: Authentication): Mono<Authentication> {
        val credential = authen.credentials.toString()

        return userDetailService
                .findByUsername(authen.name)
                .switchIfEmpty(Mono.error(Exception("User Not Found")))
                .map { user ->
                    if (!encoder.matches(credential, user.password))
                        throw Exception("User Authentication Error.")
                    UsernamePasswordAuthenticationToken.authenticated(user, user.password, user.authorities)
                }
    }
}
