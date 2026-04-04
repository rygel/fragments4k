package io.github.rygel.fragments.test

import io.github.rygel.fragments.Breadcrumb
import io.github.rygel.fragments.BreadcrumbGenerator
import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentStatus
import io.github.rygel.fragments.SeoMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BreadcrumbGeneratorTest {

    @Test
    fun testManualCrumbListGeneration() {
        val crumbs = listOf(
            Breadcrumb("Home", "https://example.com/"),
            Breadcrumb("Blog", "https://example.com/blog"),
            Breadcrumb("Hello World", "https://example.com/blog/2026/03/hello-world")
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
    fun testFromFragmentWithBlogPostUrl() {
        val fragment = Fragment(
            title = "Hello World",
            slug = "hello-world",
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.of(2026, 3, 15, 10, 0),
            publishDate = null,
            preview = "A blog post",
            content = "<p>Content</p>",
            frontMatter = emptyMap(),
            resolvedUrl = "/blog/2026/03/hello-world"
        )

        val jsonLd = BreadcrumbGenerator.fromFragment(fragment, "https://example.com")

        assertTrue(jsonLd.contains("\"name\":\"Home\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/\""))
        assertTrue(jsonLd.contains("\"name\":\"Blog\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/blog\""))
        assertTrue(jsonLd.contains("\"name\":\"Hello World\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/blog/2026/03/hello-world\""))
        // Numeric segments (2026, 03) should be skipped as intermediate crumbs
        assertTrue(!jsonLd.contains("\"name\":\"2026\""))
        assertTrue(!jsonLd.contains("\"name\":\"03\""))
        // Should have exactly 3 positions: Home, Blog, Hello World
        assertTrue(jsonLd.contains("\"position\":3"))
        assertTrue(!jsonLd.contains("\"position\":4"))
    }

    @Test
    fun testFromFragmentWithSimplePageUrl() {
        val fragment = Fragment(
            title = "About Us",
            slug = "about",
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.of(2026, 1, 1, 0, 0),
            publishDate = null,
            preview = "About page",
            content = "<p>About</p>",
            frontMatter = emptyMap()
        )

        val jsonLd = BreadcrumbGenerator.fromFragment(fragment, "https://example.com")

        assertTrue(jsonLd.contains("\"name\":\"Home\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/\""))
        assertTrue(jsonLd.contains("\"name\":\"About Us\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/about\""))
        // Should have exactly 2 positions: Home, About Us
        assertTrue(jsonLd.contains("\"position\":2"))
        assertTrue(!jsonLd.contains("\"position\":3"))
    }

    @Test
    fun testFromFragmentWithNestedUrl() {
        val fragment = Fragment(
            title = "My Project",
            slug = "my-project",
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.of(2026, 1, 1, 0, 0),
            publishDate = null,
            preview = "Project description",
            content = "<p>Project</p>",
            frontMatter = emptyMap(),
            resolvedUrl = "/projects/my-project"
        )

        val jsonLd = BreadcrumbGenerator.fromFragment(fragment, "https://example.com")

        assertTrue(jsonLd.contains("\"name\":\"Home\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/\""))
        assertTrue(jsonLd.contains("\"name\":\"Projects\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/projects\""))
        assertTrue(jsonLd.contains("\"name\":\"My Project\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/projects/my-project\""))
        assertTrue(jsonLd.contains("\"position\":3"))
        assertTrue(!jsonLd.contains("\"position\":4"))
    }

    @Test
    fun testJsonEscapingOfSpecialCharactersInTitles() {
        val crumbs = listOf(
            Breadcrumb("Home", "https://example.com/"),
            Breadcrumb("He said \"hello\" & left", "https://example.com/test")
        )

        val jsonLd = BreadcrumbGenerator.generate("https://example.com", crumbs)

        assertTrue(jsonLd.contains("He said \\\"hello\\\" & left"))
        assertTrue(!jsonLd.contains("He said \"hello\""))
    }

    @Test
    fun testSeoMetadataConvenienceMethod() {
        val seoMetadata = SeoMetadata(
            title = "Test",
            description = "Test description",
            canonicalUrl = "https://example.com/test"
        )

        val crumbs = listOf(
            Breadcrumb("Home", "https://example.com/"),
            Breadcrumb("Test", "https://example.com/test")
        )

        val jsonLd = seoMetadata.generateBreadcrumbJsonLd("https://example.com", crumbs)

        assertTrue(jsonLd.contains("\"@type\":\"BreadcrumbList\""))
        assertTrue(jsonLd.contains("\"name\":\"Home\""))
        assertTrue(jsonLd.contains("\"name\":\"Test\""))
    }

    @Test
    fun testTrailingSlashOnSiteUrlIsNormalized() {
        val fragment = Fragment(
            title = "About",
            slug = "about",
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.of(2026, 1, 1, 0, 0),
            publishDate = null,
            preview = "About",
            content = "<p>About</p>",
            frontMatter = emptyMap()
        )

        val jsonLd = BreadcrumbGenerator.fromFragment(fragment, "https://example.com/")

        // Should not produce double slashes
        assertTrue(!jsonLd.contains("https://example.com//"))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/\""))
        assertTrue(jsonLd.contains("\"item\":\"https://example.com/about\""))
    }
}
