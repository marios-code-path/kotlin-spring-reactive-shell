package example.rsocket.security

import example.rsocket.clientconfig.RequesterFactory
import example.rsocket.service.TreeService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.rsocket.retrieveMono
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier

@SpringBootTest
@ActiveProfiles("LOGINONLY")
class ClientSetupIntegrationTests {
    @Test
    fun `connection setup user then application user should pass`(@Autowired requesterFactory: RequesterFactory) {
        val request = requesterFactory.requester("connector", "foobarnopassword")
                .route("shake")
                .metadata(UsernamePasswordMetadata("shaker", "foobarnopassword"), RequesterFactory.SIMPLE_AUTH)
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
}