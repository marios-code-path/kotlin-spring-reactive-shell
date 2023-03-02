package example.rsocket.serverconfig

import example.rsocket.TreePayloadInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor

@Configuration
class SecurityConfiguration (){

    @Bean
    fun simpleSecurityAuthentication(security: RSocketSecurity)
            : PayloadSocketAcceptorInterceptor = security
     //       .addPayloadInterceptor(TreePayloadInterceptor())
            .simpleAuthentication(Customizer.withDefaults())
            .authorizePayload { authorize ->
                authorize
                        .setup()
                        .authenticated()
                        .anyRequest()
                        .authenticated()
            }
            .build()
}