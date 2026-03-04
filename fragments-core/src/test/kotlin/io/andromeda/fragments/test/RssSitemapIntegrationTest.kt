package io.andromeda.fragments.test

import io.andromeda.fragments.*
import io.andromeda.fragments.rss.RssGenerator
import io.andromeda.fragments.sitemap.SitemapGenerator
import io.andromeda.fragments.blog.BlogEngine
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.io.TempDir
import kotlin.io.path.*

class RssSitemapIntegrationTest {

    @TempDir
    lateinit var tempDir: Path
    lateinit var contentDir: Path

    @BeforeEach
    fun setup() {
        tempDir = Files.createTempDirectory("rss-sitemap-test")
        contentDir = tempDir.resolve("content")

        Files.createDirectories(contentDir.resolve("posts"))
    }

    @AfterEach
    fun cleanup() {
        Files.walk(contentDir)
            .filter { it.toFile().isFile }
            .forEach { Files.deleteIfExists(it) }

        Files.deleteIfExists(contentDir)
        Files.deleteIfExists(tempDir)
    }

    @Test
    fun `RSS generation produces valid XML`() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "first-post",
            title = "First Post",
            content = "This is my first post with markdown content",
            preview = "First post preview",
            date = LocalDateTime.of(2024, 3, 10),
            frontMatter = mapOf("tags" to listOf("blog", "rss")),
            visible = true,
            template = "blog_post"
        )

        val fragment2 = Fragment(
            slug = "second-post",
            title = "Second Post",
            content = "This is my second post with more markdown",
            preview = "Second post preview",
            date = LocalDateTime.of(2024, 3, 15),
            frontMatter = mapOf("tags" to listOf("blog", "feed")),
            visible = true,
            template = "blog_post"
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val rssGenerator = RssGenerator(
            repository = repository,
            siteTitle = "Test Blog",
            siteDescription = "Test Description",
            siteUrl = "https://example.com",
            feedUrl = "https://example.com/feed.xml"
            lastModified = LocalDateTime.of(2024, 3, 20)
        )

        val rssXml = rssGenerator.generateRss()

        assertNotNull(rssXml)
        assertTrue(rssXml.contains("<?xml version=\"1.0\""))
        assertTrue(rssXml.contains("<rss version=\"2.0\""))
        assertTrue(rssXml.contains("<channel>"))
        assertTrue(rssXml.contains("<title>Test Blog</title>"))
    }

    @Test
    fun `Sitemap generation produces valid XML`() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "first-post",
            title = "First Post",
            content = "This is my first post with markdown content",
            preview = "First post preview",
            date = LocalDateTime.of(2024, 3, 10),
            frontMatter = mapOf("tags" to listOf("blog", "sitemap")),
            visible = true,
            template = "blog_post"
        )

        val fragment2 = Fragment(
            slug = "second-post",
            title = "Second Post",
            content = "This is my second post with more markdown",
            preview = "Second post preview",
            date = LocalDateTime.of(2024, 3, 15),
            frontMatter = mapOf("tags" to listOf("blog", "sitemap")),
            visible = true,
            template = "blog_post"
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val sitemapGenerator = SitemapGenerator(
            repository = repository,
            siteUrl = "https://example.com",
            lastModified = LocalDateTime.of(2024, 3, 20)
        )

        val sitemapXml = sitemapGenerator.generateSitemap()

        assertNotNull(sitemapXml)
        assertTrue(sitemapXml.contains("<?xml version=\"1.0\""))
        assertTrue(sitemapXml.contains("<urlset>"))
        assertTrue(sitemapXml.contains("https://example.com/"))
    }
}
