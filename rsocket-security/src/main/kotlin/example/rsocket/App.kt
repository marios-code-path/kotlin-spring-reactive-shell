package example.rsocket

import example.rsocket.clientconfig.RequesterFactory
import example.rsocket.serverconfig.SecurityConfiguration
import example.rsocket.serverconfig.SecurityMessageHandlerCustomizer
import example.rsocket.service.TreeControllerMapping
import example.rsocket.service.TreeService
import example.rsocket.service.TreeServiceImpl
import example.rsocket.service.TreeServiceSecurity
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
import org.springframework.stereotype.Controller

@EnableReactiveMethodSecurity
@EnableRSocketSecurity
@SpringBootApplication
class App {

    @Configuration
    class ServerSecurityConfiguration : SecurityConfiguration()

    @Controller
    class TreeController(impl: TreeServiceImpl) : TreeControllerMapping, TreeServiceSecurity, TreeService by impl

    @Bean
    fun treeServiceBean(): TreeServiceImpl = TreeServiceImpl()

    @Bean
    fun messageHandlerCustomizerBean() = SecurityMessageHandlerCustomizer()

    @Bean
    fun requesterFactoryBean(@Value("\${spring.rsocket.server.port}") port: String,
                             builder: RSocketRequester.Builder
    ): RequesterFactory = RequesterFactory(port, builder)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<App>(*args)
        }
    }
}