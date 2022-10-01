package example.rsocket.security

import example.rsocket.clientconfig.RequesterFactory
import example.rsocket.service.TreeService
import io.rsocket.exceptions.ApplicationErrorException
import io.rsocket.exceptions.RejectedSetupException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.connectTcpAndAwait
import org.springframework.messaging.rsocket.retrieveMono
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata
import org.springframework.test.context.TestPropertySource
import reactor.test.StepVerifier

@SpringBootTest
class RequesterFactoryTests {

    @Test
    fun contextLoads() {
    }

    @Test
    fun `no setup metadata request is REJECTEDSETUP`(@Autowired requesterFactory: RequesterFactory) {
        val requester = requesterFactory.requester()

        val request = requester
                .route("status").retrieveMono<String>()

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)
    }

    @Test
    fun `sends credential metadata in request is REJECTEDSETUP`(@Autowired requesterFactory: RequesterFactory) {
        val requester = requesterFactory.requester()

        val request = requester
                .route("status")
                .metadata(UsernamePasswordMetadata("shaker", "nopassword"), RequesterFactory.SIMPLE_AUTH)
                .retrieveMono<String>()

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)
    }

    @Test
    fun `incorrect password request is REJECTEDSETUP Invalid Credentials`(@Autowired requesterFactory: RequesterFactory) {
        val request = requesterFactory
                .requester("shaker", "wrongpassword")
                .route("shake")
                .retrieveMono<String>()

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)
    }

    @Test
    fun `authenticated request for shake is resolved`(@Autowired requesterFactory: RequesterFactory) {
        val request = requesterFactory
                .requester("shaker", "nopassword")
                .route("shake")
                .retrieveMono<String>()

        StepVerifier
                .create(request)
                .assertNext {
                    Assertions
                            .assertThat(it)
                            .isNotNull
                            .containsAnyOf(*TreeService.LEAF_COLORS.toTypedArray())
                }
                .verifyComplete()
    }

    @Test
    fun `authenticated request for status is resolved`(@Autowired requesterFactory: RequesterFactory) {
        val request = requesterFactory
                .requester("shaker", "nopassword")
                .route("status")
                .retrieveMono<String>()

        StepVerifier
                .create(request)
                .assertNext {
                    Assertions
                            .assertThat(it)
                            .isNotNull
                            .isEqualTo(true.toString())
                }
                .verifyComplete()
    }

    @Test
    fun `underprivileged shake request is APPLICATIONERROR Denied`(@Autowired requesterFactory: RequesterFactory) {
        val request = requesterFactory.requester("raker", "nopassword")
                .route("shake")
                .retrieveMono<String>()

        StepVerifier
                .create(request)
                .verifyError(ApplicationErrorException::class.java)
    }
}