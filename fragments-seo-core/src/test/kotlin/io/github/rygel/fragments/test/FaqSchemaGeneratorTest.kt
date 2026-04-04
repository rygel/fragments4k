package io.github.rygel.fragments.test

import io.github.rygel.fragments.FaqEntry
import io.github.rygel.fragments.FaqSchemaGenerator
import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FaqSchemaGeneratorTest {

    @Test
    fun generateProducesValidFaqPageJsonLd() {
        val entries = listOf(
            FaqEntry("What is fragments4k?", "A content engine for Kotlin."),
            FaqEntry("What frameworks?", "HTTP4k, Spring Boot, Javalin, Quarkus, Micronaut.")
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
        val entries = listOf(
            FaqEntry(
                """How do I use "quotes"?""",
                "Use a backslash \\ before the quote.\nNew line here."
            )
        )

        val jsonLd = FaqSchemaGenerator.generate(entries)

        assertTrue(jsonLd.contains("""How do I use \"quotes\"?"""))
        assertTrue(jsonLd.contains("""Use a backslash \\ before the quote.\nNew line here."""))
        // Verify the output does not contain unescaped quotes that would break JSON
        assertFalse(jsonLd.contains(""""How do I use "quotes"?""""))
    }

    @Test
    fun generateHandlesSingleEntry() {
        val entries = listOf(
            FaqEntry("Only question?", "Only answer.")
        )

        val jsonLd = FaqSchemaGenerator.generate(entries)

        assertTrue(jsonLd.contains(""""@type": "FAQPage""""))
        assertTrue(jsonLd.contains(""""name": "Only question?""""))
        assertTrue(jsonLd.contains(""""text": "Only answer.""""))
        // Single entry should not have a trailing comma after the closing brace
        assertFalse(jsonLd.contains("},\n  ]"))
    }

    @Test
    fun fromFragmentExtractsFaqEntries() {
        val fragment = Fragment(
            title = "FAQ Page",
            slug = "faq-page",
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.of(2024, 1, 15, 10, 0),
            publishDate = null,
            preview = "FAQ content",
            content = "<p>FAQ content</p>",
            frontMatter = emptyMap(),
            faq = listOf(
                FaqEntry("Question 1?", "Answer 1."),
                FaqEntry("Question 2?", "Answer 2.")
            )
        )

        val jsonLd = FaqSchemaGenerator.fromFragment(fragment)

        assertTrue(jsonLd.contains(""""@type": "FAQPage""""))
        assertTrue(jsonLd.contains(""""name": "Question 1?""""))
        assertTrue(jsonLd.contains(""""text": "Answer 1.""""))
        assertTrue(jsonLd.contains(""""name": "Question 2?""""))
        assertTrue(jsonLd.contains(""""text": "Answer 2.""""))
    }

    @Test
    fun fromFragmentReturnsEmptyStringWhenNoFaq() {
        val fragment = Fragment(
            title = "No FAQ",
            slug = "no-faq",
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.of(2024, 1, 15, 10, 0),
            publishDate = null,
            preview = "No FAQ content",
            content = "<p>No FAQ</p>",
            frontMatter = emptyMap()
        )

        val jsonLd = FaqSchemaGenerator.fromFragment(fragment)
        assertEquals("", jsonLd)
    }
}
