package io.andromeda.fragments.test

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.rss.RssGenerator
import io.andromeda.fragments.sitemap.SitemapGenerator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class BlogEngineIntegrationTest {

    @Test
    fun `blog engine returns paginated posts`() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "first-post",
            title = "First Post",
            content = "This is my first post with markdown content",
            preview = "First post preview",
            date = LocalDateTime.of(2024, 3, 10),
            frontMatter = mapOf("tags" to listOf("blog")),
            visible = true,
            template = "blog_post"
        )

        val fragment2 = Fragment(
            slug = "second-post",
            title = "Second Post",
            content = "This is my second post with more markdown content",
            preview = "Second post preview",
            date = LocalDateTime.of(2024, 3, 15),
            frontMatter = mapOf("tags" to listOf("blog")),
            visible = true,
            template = "blog_post"
        )

        val fragment3 = Fragment(
            slug = "third-post",
            title = "Third Post",
            content = "This is my third post",
            preview = "Third post preview",
            date = LocalDateTime.of(2024, 3, 20),
            frontMatter = mapOf("tags" to listOf("blog")),
            visible = true,
            template = "blog_post"
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)
        repository.addFragment(fragment3)

        val blogEngine = BlogEngine(repository)

        val result = blogEngine.getOverview(page = 1, perPage = 10)
        assertNotNull(result)
        assertEquals(1, result.items.size)
        assertEquals("first-post", result.items[0].slug)
        assertEquals("First Post", result.items[0].title)
    }

    @Test
    fun `blog engine filters by tag correctly`() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "kotlin-post",
            title = "Kotlin Post",
            content = "Content about Kotlin",
            preview = "Kotlin preview",
            date = LocalDateTime.of(2024, 3, 7),
            frontMatter = mapOf("tags" to listOf("kotlin", "programming")),
            visible = true,
            template = "blog_post"
        )

        val fragment2 = Fragment(
            slug = "java-post",
            title = "Java Post",
            content = "Content about Java",
            preview = "Java preview",
            date = LocalDateTime.of(2024, 3, 8),
            frontMatter = mapOf("tags" to listOf("java", "maven")),
            visible = true,
            template = "blog_post"
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val blogEngine = BlogEngine(repository)

        val result = blogEngine.getByTag("kotlin")

        assertEquals(1, result.size)
        assertEquals("kotlin-post", result[0].slug)
        assertEquals("Kotlin Post", result[0].title)
    }

    @Test
    fun `RSS generation produces valid output`() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "rss-test-post",
            title = "RSS Test Post",
            content = "Test content for RSS",
            preview = "RSS preview",
            date = LocalDateTime.of(2024, 3, 25),
            frontMatter = mapOf("tags" to listOf("rss", "feed")),
            visible = true,
            template = "blog_post"
        )

        repository.addFragment(fragment1)

        val rssGenerator = RssGenerator(
            repository = repository,
            siteTitle = "Test Site RSS",
            siteDescription = "Test Description",
            siteUrl = "https://example.com",
            feedUrl = "https://example.com/feed.xml",
            lastModified = LocalDateTime.of(2024, 3, 25)
        )

        val rssXml = rssGenerator.generateRss()

        assertNotNull(rssXml)
        assertTrue(rssXml.contains("<?xml version"))
        assertTrue(rssXml.contains("<channel>"))
        assertTrue(rssXml.contains("<title>Test Site RSS</title>"))
    }
}
