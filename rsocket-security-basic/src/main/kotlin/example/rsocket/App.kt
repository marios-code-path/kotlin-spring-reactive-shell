package example.rsocket

import example.rsocket.clientconfig.RequesterFactory
import example.rsocket.serverconfig.SecurityMessageHandlerCustomizer
import example.rsocket.service.*
import io.rsocket.core.RSocketServer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer
import org.springframework.boot.rsocket.server.RSocketServerCustomizer
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder
import org.springframework.stereotype.Controller

@EnableReactiveMethodSecurity
@EnableRSocketSecurity
@SpringBootApplication
@Import(
        JacksonAutoConfiguration::class,
        RSocketStrategiesAutoConfiguration::class,
        RSocketMessagingAutoConfiguration::class,
)
class App {

    @Configuration
    class StrategiesCustomizer : RSocketStrategiesCustomizer {
        override fun customize(strategies: RSocketStrategies.Builder?) {
            strategies!!.encoder(SimpleAuthenticationEncoder())
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
                            .roles("SHAKE", "SPECIES")
                            .build(),
                    User.builder()
                            .username("raker")
                            .password("{noop}nopassword")
                            .roles("RAKE")
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