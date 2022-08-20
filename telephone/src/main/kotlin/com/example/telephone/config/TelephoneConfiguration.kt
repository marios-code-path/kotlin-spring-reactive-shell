package com.example.telephone.config

import io.netty.util.internal.NoOpTypeParameterMatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class TelephoneConfiguration {

    @Bean
    fun passwordEncoder(): PasswordEncoder = DelegatingPasswordEncoder("noop",
            mapOf(Pair("noop", NoOpPasswordEncoder.getInstance())))

    @Bean
    fun userDetailService(encoder: PasswordEncoder) = MapReactiveUserDetailsService(
            User.withUsername("spring")
                    .password(encoder.encode("12345"))
                    .authorities(
                            SimpleGrantedAuthority("ROLE_CALL"),
                            SimpleGrantedAuthority("ROLE_CREATE")
                    ).build(),
            User.withUsername("tux")
                    .password(encoder.encode("12345"))
                    .authorities(SimpleGrantedAuthority("ROLE_RECEIVE")).build())

    @Bean
    fun authenticationManager(userDetailService: MapReactiveUserDetailsService,
                              passwordEncoder: PasswordEncoder) =
            UserDetailsRepositoryReactiveAuthenticationManager(userDetailService).apply {
                setPasswordEncoder(passwordEncoder)
            }


//    @Bean
//    fun authenticationManager(userDetailService: MapReactiveUserDetailsService) =
//            ReactiveAuth(userDetailService)
}