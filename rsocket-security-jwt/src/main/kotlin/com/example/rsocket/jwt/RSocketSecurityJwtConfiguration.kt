package com.example.rsocket.jwt

import org.springframework.context.annotation.Bean
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity.AuthorizePayloadsSpec
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor
import org.springframework.web.bind.annotation.GetMapping


open class RSocketSecurityJwtConfiguration {

    @Bean
    open fun openRSocketInterceptor(rsocket: RSocketSecurity): PayloadSocketAcceptorInterceptor {
        rsocket
                .authorizePayload { authorize: AuthorizePayloadsSpec ->
                    authorize
                            .anyRequest().authenticated()
                            .anyExchange().permitAll()
                }
                .jwt(Customizer.withDefaults())
        return rsocket.build()
    }

    @Bean
    open fun jwtDecoder(): ReactiveJwtDecoder {
        return ReactiveJwtDecoders
                .fromIssuerLocation("http://localhost:9000")
    }

    @Bean
    open fun userDetailService(): ReactiveUserDetailsService = MapReactiveUserDetailsService(
            User.builder()
                    .username("plumber")
                    .password("{noop}supermario")
                    .roles("SHAKE")
                    .build(),
            User.builder()
                    .username("gardner")
                    .password("{noop}superuser")
                    .roles("RAKE")
                    .build()
    )

    @GetMapping("/")
    fun index(@AuthenticationPrincipal jwt: Jwt) =
            "Hello ${jwt.subject}"
}