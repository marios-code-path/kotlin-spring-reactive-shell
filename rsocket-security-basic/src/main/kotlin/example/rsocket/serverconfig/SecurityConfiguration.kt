package example.rsocket.serverconfig

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor

open class SecurityConfiguration {

    @Bean
    open fun userDetailService(): ReactiveUserDetailsService =
            MapReactiveUserDetailsService(
                    User.builder()
                            .username("shaker")
                            .password("{noop}nopassword")
                            .roles("SHAKE")
                            .build(),
                    User.builder()
                            .username("raker")
                            .password("{noop}nopassword")
                            .roles("RAKE", "LOGIN")
                            .build()
            )

    @Bean
    @Profile("LOGINONLY")
    open fun simpleSecurityAuthentication(security: RSocketSecurity)
            : PayloadSocketAcceptorInterceptor = security
            .simpleAuthentication(Customizer.withDefaults())
            .authorizePayload { authorize ->
                authorize
                        .setup().authenticated()
                        .anyRequest().authenticated()
            }
            .build()
}