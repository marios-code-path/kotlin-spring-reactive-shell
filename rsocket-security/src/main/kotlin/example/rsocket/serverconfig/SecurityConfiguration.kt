package example.rsocket.serverconfig

import org.springframework.context.annotation.Bean
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User

open class SecurityConfiguration {

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

    @Bean
    open fun securityAuthorizer(security: RSocketSecurity) = security
                .authorizePayload { spec ->
                    spec
                            .anyExchange()
                            .authenticated()
                }
                .simpleAuthentication(Customizer.withDefaults())
                .build()
}