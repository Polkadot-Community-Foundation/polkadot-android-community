package io.paritytech.polkadotapp.app.internal

import io.paritytech.polkadotapp.feature_usernames_api.presentation.MIN_USERNAME_LENGTH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class InternalUsernameSuggestionTest {

    @Test
    fun `suggestions are two listed words, lowercase, with no separator`() {
        val random = Random(seed = 42)

        repeat(1_000) {
            val name = InternalUsernameSuggestion.next(random)

            assertTrue("'$name' has a character outside a-z", name.all { it in 'a'..'z' })
            assertTrue(
                "'$name' is not an adjective followed by a noun",
                InternalUsernameSuggestion.adjectives.any { adjective ->
                    name.startsWith(adjective) && name.removePrefix(adjective) in InternalUsernameSuggestion.nouns
                }
            )
        }
    }

    @Test
    fun `every word pair is a usable username`() {
        InternalUsernameSuggestion.adjectives.forEach { adjective ->
            InternalUsernameSuggestion.nouns.forEach { noun ->
                val name = adjective + noun
                assertTrue("'$name' is shorter than $MIN_USERNAME_LENGTH", name.length >= MIN_USERNAME_LENGTH)
                assertTrue("'$name' is rejected", InternalUsernameSuggestion.isAcceptable(name))
            }
        }
    }

    @Test
    fun `names containing an obvious term are rejected`() {
        listOf("testoasis", "zestytest", "polkadotfern", "adminowl", "pcfriver").forEach { name ->
            assertFalse("'$name' should be rejected", InternalUsernameSuggestion.isAcceptable(name))
        }
    }

    @Test
    fun `word lists have no duplicates`() {
        assertEquals(InternalUsernameSuggestion.adjectives.size, InternalUsernameSuggestion.adjectives.toSet().size)
        assertEquals(InternalUsernameSuggestion.nouns.size, InternalUsernameSuggestion.nouns.toSet().size)
    }
}
