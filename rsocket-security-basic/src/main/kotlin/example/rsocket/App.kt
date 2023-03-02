package example.rsocket

import example.rsocket.clientconfig.RequesterFactory
import example.rsocket.serverconfig.SecurityMessageHandlerCustomizer
import example.rsocket.service.*
import io.rsocket.core.RSocketServer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer
import org.springframework.boot.rsocket.server.RSocketServerCustomizer
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder
import org.springframework.stereotype.Controller
import org.springframework.web.util.pattern.PathPatternRouteMatcher


@EnableRSocketSecurity
@EnableReactiveMethodSecurity
@SpringBootApplication
class App {

    @Configuration
    class StrategiesCustomizer : RSocketStrategiesCustomizer {
        override fun customize(strategies: RSocketStrategies.Builder) {
            strategies.apply {
                encoder(SimpleAuthenticationEncoder())
                routeMatcher(PathPatternRouteMatcher())
            }
        }
    }

    @Configuration
    class ServerCustomize(private val strategies: RSocketStrategies) : RSocketServerCustomizer {
        override fun customize(rSocketServer: RSocketServer) {
            rSocketServer.interceptors { it.forResponder(GlobalInterceptor(strategies)) }
        }
    }

    @Controller
    class ServerTreeController : TreeControllerMapping,
            TreeServiceSecurity, TreeService by TreeServiceImpl()

    @Bean
    @Scope("prototype")
    fun requesterFactoryBean(@Value("\${spring.rsocket.server.port}") port: String, builder: RSocketRequester.Builder): RequesterFactory =
            RequesterFactory(builder, port)

    @Bean
    fun messageHandlerCustomizerBean() = SecurityMessageHandlerCustomizer()

    @Bean
    fun userDetailService(): ReactiveUserDetailsService =
            MapReactiveUserDetailsService(
                    User.builder()
                            .username("shaker")
                            .password("{noop}nopassword")
                            .roles("SHAKE", "SPECIES", "LOGIN")
                            .build(),
                    User.builder()
                            .username("raker")
                            .password("{noop}nopassword")
                            .roles("RAKE", "LOGIN")
                            .build(),
                    User.builder()
                            .username("connector")
                            .password("{noop}nopassword")
                            .roles("LOGIN")
                            .build()
            )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<App>(*args)
        }
    }
}