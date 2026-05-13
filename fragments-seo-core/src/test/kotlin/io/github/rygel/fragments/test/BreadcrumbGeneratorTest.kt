package io.github.rygel.fragments.test

import io.github.rygel.fragments.Breadcrumb
import io.github.rygel.fragments.BreadcrumbGenerator
import io.github.rygel.fragments.SeoMetadata
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BreadcrumbGeneratorTest {
    @Test
    fun testManualCrumbListGeneration() {
        val crumbs =
            listOf(
                Breadcrumb("Home", "https://example.com/"),
                Breadcrumb("Blog", "https://example.com/blog"),
                Breadcrumb("Hello World", "https://example.com/blog/2026/03/hello-world"),
            )

        val jsonLd = BreadcrumbGenerator.generate("https://example.com", crumbs)

        assertTrue(jsonLd.contains("\"@context\":\"https://schema.org\""))
        assertTrue(jsonLd.contains("\"@type\":\"BreadcrumbList\""))
        assertTrue(jsonLd.contains("\"position\":1"))
        assertTrue(jsonLd.contains("\"name\":\"Home\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/\""))
        assertTrue(jsonLd.contains("\"position\":2"))
        assertTrue(jsonLd.contains("\"name\":\"Blog\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/blog\""))
        assertTrue(jsonLd.contains("\"position\":3"))
        assertTrue(jsonLd.contains("\"name\":\"Hello World\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/blog/2026/03/hello-world\""))
    }

    @Test
    fun testJsonEscapingOfSpecialCharactersInTitles() {
        val crumbs =
            listOf(
                Breadcrumb("Home", "https://example.com/"),
                Breadcrumb("He said \"hello\" & left", "https://example.com/test"),
            )

        val jsonLd = BreadcrumbGenerator.generate("https://example.com", crumbs)

        assertTrue(jsonLd.contains("He said \\\"hello\\\" & left"))
        assertTrue(!jsonLd.contains("He said \"hello\""))
    }

    @Test
    fun testSeoMetadataConvenienceMethod() {
        val seoMetadata =
            SeoMetadata(
                title = "Test",
                description = "Test description",
                canonicalUrl = "https://example.com/test",
            )

        val crumbs =
            listOf(
                Breadcrumb("Home", "https://example.com/"),
                Breadcrumb("Test", "https://example.com/test"),
            )

        val jsonLd = seoMetadata.generateBreadcrumbJsonLd("https://example.com", crumbs)

        assertTrue(jsonLd.contains("\"@type\":\"BreadcrumbList\""))
        assertTrue(jsonLd.contains("\"name\":\"Home\""))
        assertTrue(jsonLd.contains("\"name\":\"Test\""))
    }
}
