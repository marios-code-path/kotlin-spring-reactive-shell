package example.rsocket.security

import com.fasterxml.jackson.databind.ObjectMapper
import example.rsocket.clientconfig.RequesterFactory
import example.rsocket.service.TreeService
import example.rsocket.domain.TreeSpecies
import io.rsocket.exceptions.ApplicationErrorException
import io.rsocket.exceptions.RejectedSetupException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.rsocket.retrieveMono
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata
import org.springframework.test.annotation.DirtiesContext
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@SpringBootTest
class ClientIntegrationTests {


    @Test
    fun contextLoads() {
    }

    @Test
    fun `can decode TreeSpecies`(@Autowired objectMapper: ObjectMapper) {
        println("Hello World! " + objectMapper.writeValueAsString(TreeSpecies(34L, "LONG")))
        val newTree: TreeSpecies = objectMapper.readValue("{\"TreeSpecies\":{\"id\":34,\"leaf\":\"LONG\"}}", TreeSpecies::class.java)

        println(newTree.id)
    }

    @Test
    fun `sends TreeSpecies Oak is not accepted`(@Autowired requesterFactory: RequesterFactory) {
        Hooks.onOperatorDebug()

        val requester = requesterFactory.requester("shaker", "nopassword")

        val request = requester
                .route("species")
                .data(Mono.just(TreeSpecies(1L, "Oak")), TreeSpecies::class.java)
                .retrieveMono<TreeSpecies>()

        StepVerifier
                .create(request)
                .verifyError(ApplicationErrorException::class.java)

        val second = requester
                .route("species")
                .data(TreeSpecies(2L, "Birch"))
                .retrieveMono<TreeSpecies>()

        StepVerifier
                .create(second)
                .assertNext { species ->
                    Assertions.assertThat(species.leaf).isEqualTo("BIRCH")
                }
                .verifyComplete()
    }

    @Test
    // TODO: FIX The GlobalInterceptor is throwing an error
    fun `no setup metadata request is REJECTEDSETUP`(@Autowired requesterFactory: RequesterFactory) {
        val requester = requesterFactory.requester()

        val request = requester
                .route("status.123").retrieveMono<String>()

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)
    }

    @Test
    fun `sends credential metadata in request is REJECTEDSETUP`(@Autowired requesterFactory: RequesterFactory) {
        val requester = requesterFactory.requester()

        val request = requester
                .route("status.345")
                .metadata(UsernamePasswordMetadata("shaker", "nopassword"), RequesterFactory.SIMPLE_AUTH)
                .retrieveMono<String>()

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)
    }

    @Test
    fun `incorrect password request is REJECTEDSETUP Invalid Credentials`(@Autowired requesterFactory: RequesterFactory) {
        Hooks.onOperatorDebug()

        val request = requesterFactory
                .requester("shaker", "wnopassword")
                .route("shake")
                .retrieveMono<String>()

        StepVerifier
                .create(request)
                .verifyError(RejectedSetupException::class.java)
    }

    @Test
    fun `authenticated request for shake is resolved`(@Autowired requesterFactory: RequesterFactory) {
        val requester = requesterFactory
                .requester("shaker", "nopassword")

        val request = requester
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
        requester.dispose()
    }

    @Test
    fun `authenticated request for status is resolved`(@Autowired requesterFactory: RequesterFactory) {
        val requester = requesterFactory
                .requester("shaker", "nopassword")

        val request = requester
                .route("status.1234563")
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

        requester.dispose()
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