package com.example.telephone.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User

@Configuration
class TelephoneConfiguration {

    @Bean
    fun userDetailService() = MapReactiveUserDetailsService(
            User.withUsername("mario")
                    .password("12345")
                    .authorities(SimpleGrantedAuthority("CALL")).build(),
            User.withUsername("tux")
                    .password("12345")
                    .authorities(SimpleGrantedAuthority("CALL")).build())

    @Bean
    fun authManager(userDetailService: MapReactiveUserDetailsService) = ReactiveAuthentication(userDetailService)
}