package example.rsocket.security

import example.rsocket.clientconfig.RequesterFactory
import example.rsocket.service.TreeService
import io.rsocket.exceptions.RejectedSetupException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.connectTcpAndAwait
import org.springframework.messaging.rsocket.retrieveMono
import org.springframework.test.context.TestPropertySource
import reactor.test.StepVerifier

@SpringBootTest
@TestPropertySource(properties = ["spring.shell.interactive.enabled=false"])
class RequesterTests {

    @Test
    fun contextLoads() {
    }

    @Test
    fun `unauthenticated request gets ACCESS DENIED`(@Autowired builder: RSocketRequester.Builder) {
        val requester = builder
                .connectTcp("localhost", 8199)
                .block()!!

        val request = requester.route("shake").retrieveMono<String>()

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)
    }

    @Test
    fun `incorrect password request gets Invalid Credentials`(@Autowired requesterFactory: RequesterFactory) {
        val request = requesterFactory.requester("plumber", "nopassword")
                .route("shake")
                .retrieveMono<String>()

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)
    }

    @Test
    fun `authenticated request for shake`(@Autowired requesterFactory: RequesterFactory) {
        val request = requesterFactory.requester("plumber", "supermario")
                .route("shake")
                .retrieveMono<String>()

        StepVerifier
                .create(request)
                .assertNext {
                    Assertions
                            .assertThat(it)
                            .isNotNull()
                            .containsAnyOf(*TreeService.LEAF_COLORS.toTypedArray())
                    println("IT = $it")
                }
                .verifyComplete()
    }
}