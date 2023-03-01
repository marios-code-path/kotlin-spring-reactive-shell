package example.rsocket.security

import example.rsocket.service.TreeService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.context.support.WithUserDetails
import reactor.test.StepVerifier

@SpringBootTest()
class MethodSecurityTests {

    @Test
    @WithMockUser("testuser", roles = ["SHAKE"])
    fun `mock user service test`(@Autowired svc: TreeService) {
        StepVerifier
                .create(svc.shakeForLeaf())
                .assertNext {
                    Assertions
                            .assertThat(it)
                            .isNotNull
                            .containsAnyOf(*TreeService.LEAF_COLORS.toTypedArray())
                }
    }

    @Test
    @WithUserDetails("shaker")
    fun `withUserDetails service test`(@Autowired svc: TreeService) {
        StepVerifier
                .create(svc.shakeForLeaf())
                .assertNext {
                    Assertions
                            .assertThat(it)
                            .isNotNull
                            .containsAnyOf(*TreeService.LEAF_COLORS.toTypedArray())
                }
                .verifyComplete()
    }
}