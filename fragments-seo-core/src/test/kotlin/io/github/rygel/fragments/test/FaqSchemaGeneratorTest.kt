package io.github.rygel.fragments.test

import io.github.rygel.fragments.FaqItem
import io.github.rygel.fragments.FaqSchemaGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FaqSchemaGeneratorTest {
    @Test
    fun generateProducesValidFaqPageJsonLd() {
        val entries =
            listOf(
                FaqItem("What is fragments4k?", "A content engine for Kotlin."),
                FaqItem("What frameworks?", "HTTP4k, Spring Boot, Javalin, Quarkus, Micronaut."),
            )

        val jsonLd = FaqSchemaGenerator.generate(entries)

        assertTrue(jsonLd.contains(""""@context": "https://schema.org""""))
        assertTrue(jsonLd.contains(""""@type": "FAQPage""""))
        assertTrue(jsonLd.contains(""""mainEntity": ["""))
        assertTrue(jsonLd.contains(""""@type": "Question""""))
        assertTrue(jsonLd.contains(""""name": "What is fragments4k?""""))
        assertTrue(jsonLd.contains(""""acceptedAnswer": {"""))
        assertTrue(jsonLd.contains(""""@type": "Answer""""))
        assertTrue(jsonLd.contains(""""text": "A content engine for Kotlin.""""))
        assertTrue(jsonLd.contains(""""name": "What frameworks?""""))
        assertTrue(jsonLd.contains(""""text": "HTTP4k, Spring Boot, Javalin, Quarkus, Micronaut.""""))
    }

    @Test
    fun generateReturnsEmptyStringForEmptyList() {
        val jsonLd = FaqSchemaGenerator.generate(emptyList())
        assertEquals("", jsonLd)
    }

    @Test
    fun generateEscapesSpecialCharactersInJson() {
        val entries =
            listOf(
                FaqItem(
                    """How do I use "quotes"?""",
                    "Use a backslash \\ before the quote.\nNew line here.",
                ),
            )

        val jsonLd = FaqSchemaGenerator.generate(entries)

        assertTrue(jsonLd.contains("""How do I use \"quotes\"?"""))
        assertTrue(jsonLd.contains("""Use a backslash \\ before the quote.\nNew line here."""))
        assertFalse(jsonLd.contains(""""How do I use "quotes"?""""))
    }

    @Test
    fun generateHandlesSingleEntry() {
        val entries =
            listOf(
                FaqItem("Only question?", "Only answer."),
            )

        val jsonLd = FaqSchemaGenerator.generate(entries)

        assertTrue(jsonLd.contains(""""@type": "FAQPage""""))
        assertTrue(jsonLd.contains(""""name": "Only question?""""))
        assertTrue(jsonLd.contains(""""text": "Only answer.""""))
        assertFalse(jsonLd.contains("},\n  ]"))
    }
}
