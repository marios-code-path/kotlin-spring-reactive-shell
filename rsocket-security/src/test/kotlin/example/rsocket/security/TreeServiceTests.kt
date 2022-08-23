package example.rsocket.security

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class TreeServiceTests {
    @Test
    fun shakesForLeaves() {
        val tree = TreeService()

        StepVerifier
                .create(tree.shakeForLeaves())
                .thenConsumeWhile { leafColor ->
                    Assertions
                            .assertThat(leafColor)
                            .containsAnyOf(*tree.colors.toTypedArray())

                    true
                }
                .verifyComplete()
    }

    @Test
    fun shakesForLeaf() {
        val tree = TreeService()

        StepVerifier
                .create(tree.shakeForLeaf())
                .assertNext { color ->
                    Assertions
                            .assertThat(color)
                            .containsAnyOf(*tree.colors.toTypedArray())
                }
                .verifyComplete()
    }
}