package io.github.rygel.fragments

import io.github.rygel.fragments.test.InMemoryFragmentRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LlmsTxtGeneratorTest {

    @Test
    fun generateWithBlogPostsAndPages() = runBlocking {
        val repo = InMemoryFragmentRepository()
        repo.addFragment(createFragment("hello-world", "Hello World", template = "blog_post",
            date = LocalDateTime.of(2024, 3, 15, 10, 0),
            preview = "<p>Welcome to my first blog post about Kotlin.</p>"))
        repo.addFragment(createFragment("about", "About Me", template = "page",
            preview = "<p>This is the about page.</p>"))

        val result = LlmsTxtGenerator.generate(
            siteTitle = "Test Site",
            siteDescription = "A test site for unit testing",
            siteUrl = "https://example.com",
            repositories = listOf(repo)
        )

        assertTrue(result.startsWith("# Test Site"))
        assertTrue(result.contains("> A test site for unit testing"))
        assertTrue(result.contains("## Blog Posts"))
        assertTrue(result.contains("## Pages"))
        assertTrue(result.contains("[Hello World](https://example.com/hello-world)"))
        assertTrue(result.contains("[About Me](https://example.com/about)"))
        assertTrue(result.contains("Welcome to my first blog post about Kotlin."))
        assertTrue(result.contains("This is the about page."))
    }

    @Test
    fun generateWithOnlyBlogPosts() = runBlocking {
        val repo = InMemoryFragmentRepository()
        repo.addFragment(createFragment("post-1", "First Post", template = "blog",
            date = LocalDateTime.of(2024, 6, 1, 12, 0),
            preview = "<p>First post content</p>"))

        val result = LlmsTxtGenerator.generate(
            siteTitle = "Blog Only",
            siteDescription = "Only blog posts here",
            siteUrl = "https://example.com",
            repositories = listOf(repo)
        )

        assertTrue(result.contains("## Blog Posts"))
        assertFalse(result.contains("## Pages"))
    }

    @Test
    fun generateWithOnlyPages() = runBlocking {
        val repo = InMemoryFragmentRepository()
        repo.addFragment(createFragment("contact", "Contact", template = "static",
            preview = "<p>Contact information</p>"))

        val result = LlmsTxtGenerator.generate(
            siteTitle = "Pages Only",
            siteDescription = "Only pages here",
            siteUrl = "https://example.com",
            repositories = listOf(repo)
        )

        assertFalse(result.contains("## Blog Posts"))
        assertTrue(result.contains("## Pages"))
    }

    @Test
    fun generateWithEmptyRepository() = runBlocking {
        val repo = InMemoryFragmentRepository()

        val result = LlmsTxtGenerator.generate(
            siteTitle = "Empty Site",
            siteDescription = "Nothing here yet",
            siteUrl = "https://example.com",
            repositories = listOf(repo)
        )

        assertTrue(result.contains("# Empty Site"))
        assertTrue(result.contains("> Nothing here yet"))
        assertFalse(result.contains("## Blog Posts"))
        assertFalse(result.contains("## Pages"))
    }

    @Test
    fun previewTextIsTruncatedAt160Characters() = runBlocking {
        val longPreview = "<p>${"A".repeat(200)}</p>"
        val repo = InMemoryFragmentRepository()
        repo.addFragment(createFragment("long", "Long Preview", template = "page",
            preview = longPreview))

        val result = LlmsTxtGenerator.generate(
            siteTitle = "Test",
            siteDescription = "Test",
            siteUrl = "https://example.com",
            repositories = listOf(repo)
        )

        // The plain text from the preview (200 A's) should be truncated to 160
        val line = result.lines().first { it.startsWith("- [Long Preview]") }
        val afterColon = line.substringAfter(": ")
        assertEquals(160, afterColon.length)
    }

    @Test
    fun generateUsesAbsoluteUrls() = runBlocking {
        val repo = InMemoryFragmentRepository()
        repo.addFragment(createFragment("test", "Test Page", template = "default",
            preview = "<p>Test</p>"))

        val result = LlmsTxtGenerator.generate(
            siteTitle = "Test",
            siteDescription = "Test",
            siteUrl = "https://example.com",
            repositories = listOf(repo)
        )

        assertTrue(result.contains("https://example.com/test"))
    }

    @Test
    fun generateStripsTrailingSlashFromSiteUrl() = runBlocking {
        val repo = InMemoryFragmentRepository()
        repo.addFragment(createFragment("test", "Test Page", template = "default",
            preview = "<p>Test</p>"))

        val result = LlmsTxtGenerator.generate(
            siteTitle = "Test",
            siteDescription = "Test",
            siteUrl = "https://example.com/",
            repositories = listOf(repo)
        )

        assertTrue(result.contains("https://example.com/test"))
        assertFalse(result.contains("https://example.com//test"))
    }

    @Test
    fun blogPostsAreSortedByDateDescending() = runBlocking {
        val repo = InMemoryFragmentRepository()
        repo.addFragment(createFragment("older", "Older Post", template = "blog_post",
            date = LocalDateTime.of(2024, 1, 1, 10, 0),
            preview = "<p>Older</p>"))
        repo.addFragment(createFragment("newer", "Newer Post", template = "blog_post",
            date = LocalDateTime.of(2024, 6, 1, 10, 0),
            preview = "<p>Newer</p>"))

        val result = LlmsTxtGenerator.generate(
            siteTitle = "Test",
            siteDescription = "Test",
            siteUrl = "https://example.com",
            repositories = listOf(repo)
        )

        val newerIndex = result.indexOf("Newer Post")
        val olderIndex = result.indexOf("Older Post")
        assertTrue(newerIndex < olderIndex, "Newer post should appear before older post")
    }

    private fun createFragment(
        slug: String,
        title: String,
        template: String = "default",
        date: LocalDateTime? = null,
        preview: String = "<p>Preview text</p>"
    ): Fragment {
        return Fragment(
            slug = slug,
            title = title,
            content = "<p>Content for $title</p>",
            date = date,
            publishDate = date,
            preview = preview,
            template = template,
            visible = true,
            frontMatter = mapOf("title" to title, "slug" to slug)
        )
    }
}
