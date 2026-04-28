package io.github.rygel.fragments.adapter

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.static.StaticPageEngine
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.LocalDateTime

class FragmentsEngineSeoTest {
    private lateinit var engine: FragmentsEngine
    private lateinit var searchEngine: LuceneSearchEngine

    @BeforeEach
    fun setup() {
        val repo = mockk<FragmentRepository>(relaxed = true)
        coEvery { repo.getAll() } returns emptyList()
        val tempIndexPath = Files.createTempDirectory("lucene-seo-test")
        searchEngine = runBlocking { LuceneSearchEngine(repo, tempIndexPath) }
        engine =
            FragmentsEngine(
                staticEngine = StaticPageEngine(repo),
                blogEngine = BlogEngine(repo),
                searchEngine = searchEngine,
                siteTitle = "Test Site",
                siteDescription = "A test site",
                siteUrl = "https://example.com",
            )
    }

    @Test
    fun generateSeoMetadataForBlogPost() =
        runBlocking {
            val fragment =
                Fragment(
                    title = "Hello World",
                    slug = "hello-world",
                    date = LocalDateTime.of(2026, 4, 15, 10, 0),
                    publishDate = LocalDateTime.of(2026, 4, 15, 10, 0),
                    preview = "Some content here.",
                    content = "# Hello\n\nSome content here.",
                    template = "blog",
                    visible = true,
                    tags = listOf("kotlin", "testing"),
                    image = "/images/hello.jpg",
                    frontMatter = mutableMapOf("title" to "Hello World", "slug" to "hello-world"),
                )

            val seo = engine.generateSeoMetadata(fragment, pagePath = "blog/2026/04/hello-world")

            assertEquals("Hello World", seo.title)
            assertEquals("https://example.com/blog/2026/04/hello-world", seo.canonicalUrl)
            assertEquals("Hello World", seo.ogTitle)
            assertEquals("article", seo.ogType)
            assertEquals("Test Site", seo.ogSiteName)
            assertEquals("https://example.com/images/hello.jpg", seo.ogImage)
            assertEquals(listOf("kotlin", "testing"), seo.keywords)
            assertNotNull(seo.description)
            assertTrue(seo.description.isNotEmpty())
        }

    @Test
    fun generateSeoMetadataForStaticPage() =
        runBlocking {
            val fragment =
                Fragment(
                    title = "About Us",
                    slug = "about",
                    date = null,
                    publishDate = null,
                    preview = "About page content.",
                    content = "# About\n\nAbout page content.",
                    template = "page",
                    visible = true,
                    frontMatter = mutableMapOf("title" to "About Us", "slug" to "about"),
                )

            val seo = engine.generateSeoMetadata(fragment, pagePath = "page/about")

            assertEquals("About Us", seo.title)
            assertEquals("https://example.com/page/about", seo.canonicalUrl)
            assertEquals("website", seo.ogType)
            assertNull(seo.ogImage)
        }

    @Test
    fun generateSeoMetadataForPage() =
        runBlocking {
            val seo =
                engine.generateSeoMetadataForPage(
                    title = "Blog",
                    description = "All blog posts",
                    pagePath = "blog",
                )

            assertEquals("Blog", seo.title)
            assertEquals("All blog posts", seo.description)
            assertEquals("https://example.com/blog", seo.canonicalUrl)
            assertEquals("website", seo.ogType)
            assertEquals("Test Site", seo.ogSiteName)
        }

    @Test
    fun seoMetadataGeneratesOpenGraphTags() =
        runBlocking {
            val fragment =
                Fragment(
                    title = "Test Post",
                    slug = "test",
                    date = null,
                    publishDate = null,
                    preview = "Test preview text.",
                    content = "Content",
                    template = "blog",
                    visible = true,
                    tags = listOf("test"),
                    frontMatter = mutableMapOf(),
                )

            val seo = engine.generateSeoMetadata(fragment)
            val ogTags = seo.generateOpenGraphTags()

            assertTrue(ogTags.any { it.contains("og:title") && it.contains("Test Post") })
            assertTrue(ogTags.any { it.contains("og:type") && it.contains("article") })
            assertTrue(ogTags.any { it.contains("og:url") && it.contains("example.com") })
        }

    @Test
    fun seoMetadataGeneratesStandardMetaTags() =
        runBlocking {
            val fragment =
                Fragment(
                    title = "Test Post",
                    slug = "test",
                    date = null,
                    publishDate = null,
                    preview = "Test preview text.",
                    content = "Content",
                    template = "blog",
                    visible = true,
                    tags = listOf("test"),
                    frontMatter = mutableMapOf(),
                )

            val seo = engine.generateSeoMetadata(fragment)
            val metaTags = seo.generateStandardMetaTags()

            assertTrue(metaTags.any { it.contains("description") })
            assertTrue(metaTags.any { it.contains("canonical") && it.contains("example.com") })
            assertTrue(metaTags.any { it.contains("robots") })
        }
}
