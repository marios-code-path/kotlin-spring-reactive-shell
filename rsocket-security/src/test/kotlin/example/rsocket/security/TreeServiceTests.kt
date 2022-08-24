package example.rsocket.security

import example.rsocket.service.TreeService.Companion.LEAF_COLORS
import example.rsocket.service.TreeServiceImpl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class TreeServiceTests {
    @Test
    fun shakesForLeaves() {
        val tree = TreeServiceImpl()

        StepVerifier
                .create(tree.rakeForLeaves())
                .thenConsumeWhile { leafColor ->
                    Assertions
                            .assertThat(leafColor)
                            .containsAnyOf(*LEAF_COLORS.toTypedArray())

                    true
                }
                .verifyComplete()
    }

    @Test
    fun shakesForLeaf() {
        val tree = TreeServiceImpl()

        StepVerifier
                .create(tree.shakeForLeaf())
                .assertNext { color ->
                    Assertions
                            .assertThat(color)
                            .containsAnyOf(*LEAF_COLORS.toTypedArray())
                }
                .verifyComplete()
    }
}