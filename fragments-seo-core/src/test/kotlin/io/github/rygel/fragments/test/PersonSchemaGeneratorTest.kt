package io.github.rygel.fragments.test

import io.github.rygel.fragments.Author
import io.github.rygel.fragments.PersonSchemaGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PersonSchemaGeneratorTest {

    @Test
    fun testGenerateMinimalPerson() {
        val jsonLd = PersonSchemaGenerator.generate(
            name = "John Doe",
            siteUrl = "https://example.com"
        )

        assertTrue(jsonLd.contains(""""@context": "https://schema.org""""))
        assertTrue(jsonLd.contains(""""@type": "Person""""))
        assertTrue(jsonLd.contains(""""name": "John Doe""""))
        assertFalse(jsonLd.contains("@id"))
        assertFalse(jsonLd.contains("description"))
        assertFalse(jsonLd.contains("image"))
        assertFalse(jsonLd.contains("url"))
        assertFalse(jsonLd.contains("sameAs"))
    }

    @Test
    fun testGenerateFullPerson() {
        val jsonLd = PersonSchemaGenerator.generate(
            name = "Alexander Brandt",
            siteUrl = "https://example.com",
            authorSlug = "alexander-brandt",
            bio = "Software developer and writer",
            image = "/images/avatar.jpg",
            url = "https://alexanderbrandt.dev",
            socialLinks = listOf(
                "https://github.com/rygel",
                "https://x.com/username",
                "https://linkedin.com/in/alexanderbrandt"
            )
        )

        assertTrue(jsonLd.contains(""""@id": "https://example.com/blog/author/alexander-brandt#person""""))
        assertTrue(jsonLd.contains(""""name": "Alexander Brandt""""))
        assertTrue(jsonLd.contains(""""description": "Software developer and writer""""))
        assertTrue(jsonLd.contains(""""image": "https://example.com/images/avatar.jpg""""))
        assertTrue(jsonLd.contains(""""url": "https://alexanderbrandt.dev""""))
        assertTrue(jsonLd.contains(""""sameAs": ["""))
        assertTrue(jsonLd.contains(""""https://github.com/rygel""""))
        assertTrue(jsonLd.contains(""""https://x.com/username""""))
        assertTrue(jsonLd.contains(""""https://linkedin.com/in/alexanderbrandt""""))
    }

    @Test
    fun testGenerateWithAbsoluteImageUrl() {
        val jsonLd = PersonSchemaGenerator.generate(
            name = "Jane Doe",
            siteUrl = "https://example.com",
            image = "https://cdn.example.com/avatar.jpg"
        )

        assertTrue(jsonLd.contains(""""image": "https://cdn.example.com/avatar.jpg""""))
    }

    @Test
    fun testGenerateWithRelativeImageUrl() {
        val jsonLd = PersonSchemaGenerator.generate(
            name = "Jane Doe",
            siteUrl = "https://example.com",
            image = "/static/avatar.jpg"
        )

        assertTrue(jsonLd.contains(""""image": "https://example.com/static/avatar.jpg""""))
    }

    @Test
    fun testGenerateDefaultUrlFromSlug() {
        val jsonLd = PersonSchemaGenerator.generate(
            name = "Jane Doe",
            siteUrl = "https://example.com",
            authorSlug = "jane-doe"
        )

        assertTrue(jsonLd.contains(""""url": "https://example.com/blog/author/jane-doe""""))
    }

    @Test
    fun testGenerateExplicitUrlOverridesDefault() {
        val jsonLd = PersonSchemaGenerator.generate(
            name = "Jane Doe",
            siteUrl = "https://example.com",
            authorSlug = "jane-doe",
            url = "https://janedoe.dev"
        )

        assertTrue(jsonLd.contains(""""url": "https://janedoe.dev""""))
        // @id still uses the slug-based URL for identity, but url is explicit
        assertTrue(jsonLd.contains(""""@id": "https://example.com/blog/author/jane-doe#person""""))
    }

    @Test
    fun testFromAuthor() {
        val author = Author(
            id = "alex",
            name = "Alexander Brandt",
            slug = "alexander-brandt",
            bio = "Kotlin developer",
            avatar = "/images/alex.jpg",
            website = "https://alexanderbrandt.dev",
            twitter = "alexbrandt",
            github = "rygel",
            linkedin = "alexanderbrandt",
            joinedDate = LocalDateTime.of(2024, 1, 1, 0, 0)
        )

        val jsonLd = PersonSchemaGenerator.fromAuthor(author, "https://example.com")

        assertTrue(jsonLd.contains(""""@type": "Person""""))
        assertTrue(jsonLd.contains(""""name": "Alexander Brandt""""))
        assertTrue(jsonLd.contains(""""description": "Kotlin developer""""))
        assertTrue(jsonLd.contains(""""image": "https://example.com/images/alex.jpg""""))
        assertTrue(jsonLd.contains(""""url": "https://alexanderbrandt.dev""""))
        assertTrue(jsonLd.contains(""""sameAs": ["""))
        assertTrue(jsonLd.contains("twitter.com"))
        assertTrue(jsonLd.contains("github.com/rygel"))
        assertTrue(jsonLd.contains("linkedin.com/in/alexanderbrandt"))
    }

    @Test
    fun testFromAuthorMinimal() {
        val author = Author(
            id = "minimal",
            name = "Minimal Author",
            slug = "minimal-author",
            joinedDate = LocalDateTime.of(2024, 1, 1, 0, 0)
        )

        val jsonLd = PersonSchemaGenerator.fromAuthor(author, "https://example.com")

        assertTrue(jsonLd.contains(""""name": "Minimal Author""""))
        assertTrue(jsonLd.contains(""""@id": "https://example.com/blog/author/minimal-author#person""""))
        assertFalse(jsonLd.contains("description"))
        assertFalse(jsonLd.contains("image"))
        assertFalse(jsonLd.contains("sameAs"))
    }

    @Test
    fun testJsonEscapingInPersonSchema() {
        val jsonLd = PersonSchemaGenerator.generate(
            name = "John \"JD\" Doe",
            siteUrl = "https://example.com",
            bio = "Writes about\nthings & stuff"
        )

        // Quotes in name should be escaped
        assertTrue(jsonLd.contains("""John \"JD\" Doe"""))
        // Newline in bio should be escaped as \n in JSON
        assertTrue(jsonLd.contains("""Writes about\nthings & stuff"""))
    }
}
