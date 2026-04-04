package io.github.rygel.fragments.test

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentStatus
import io.github.rygel.fragments.SeoMetadata
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SeoMetadataTest {
    @Test
    fun testSeoMetadataGeneration() {
        val fragment = Fragment(
            title = "Test Post",
            slug = "test-post",
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.of(2024, 1, 15, 10, 0),
            publishDate = null,
            preview = "This is a test post preview",
            content = "# Test Content\n\nThis is the content of the test post.",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            categories = listOf("Technology"),
            tags = listOf("kotlin", "testing")
        )

        val seoMetadata = SeoMetadata.fromFragment(
            fragment = fragment,
            siteUrl = "https://example.com",
            siteName = "My Blog",
            pagePath = "blog/2024/01/test-post",
            author = "John Doe"
        )

        assertEquals("Test Post", seoMetadata.title)
        assertEquals("This is a test post preview", seoMetadata.description)
        assertEquals("https://example.com/blog/2024/01/test-post", seoMetadata.canonicalUrl)
        assertEquals("Test Post", seoMetadata.ogTitle)
        assertEquals("https://example.com/blog/2024/01/test-post", seoMetadata.canonicalUrl)
        assertEquals("article", seoMetadata.ogType)
        assertEquals("My Blog", seoMetadata.ogSiteName)
        assertEquals("summary_large_image", seoMetadata.twitterCard)
        assertEquals("Test Post", seoMetadata.twitterTitle)
        assertEquals("John Doe", seoMetadata.author)
        assertEquals("2024-01-15T10:00", seoMetadata.publishedDate)
        assertEquals(listOf("kotlin", "testing"), seoMetadata.keywords)
        assertEquals("en", seoMetadata.locale)
    }

    @Test
    fun testGenerateStandardMetaTags() {
        val seoMetadata = SeoMetadata(
            title = "Test Page",
            description = "Test description",
            canonicalUrl = "https://example.com/test",
            keywords = listOf("test", "example"),
            author = "Test Author"
        )

        val metaTags = seoMetadata.generateStandardMetaTags()

        assertTrue(metaTags.any { it.contains("""<meta name="description" content="Test description">""") })
        assertTrue(metaTags.any { it.contains("""<meta name="keywords" content="test, example">""") })
        assertTrue(metaTags.any { it.contains("""<meta name="author" content="Test Author">""") })
        assertTrue(metaTags.any { it.contains("""<link rel="canonical" href="https://example.com/test">""") })
    }

    @Test
    fun testGenerateOpenGraphTags() {
        val seoMetadata = SeoMetadata(
            title = "Test Article",
            description = "Test description",
            canonicalUrl = "https://example.com/article",
            ogType = "article",
            ogSiteName = "My Site",
            ogImage = "https://example.com/image.jpg",
            author = "Test Author",
            publishedDate = "2024-01-15T10:00:00",
            keywords = listOf("test", "article")
        )

        val ogTags = seoMetadata.generateOpenGraphTags()

        assertTrue(ogTags.any { it.contains("""<meta property="og:title" content="Test Article">""") })
        assertTrue(ogTags.any { it.contains("""<meta property="og:description" content="Test description">""") })
        assertTrue(ogTags.any { it.contains("""<meta property="og:url" content="https://example.com/article">""") })
        assertTrue(ogTags.any { it.contains("""<meta property="og:type" content="article">""") })
        assertTrue(ogTags.any { it.contains("""<meta property="og:site_name" content="My Site">""") })
        assertTrue(ogTags.any { it.contains("""<meta property="og:image" content="https://example.com/image.jpg">""") })
        assertTrue(ogTags.any { it.contains("""<meta property="article:author" content="Test Author">""") })
        assertTrue(ogTags.any { it.contains("""<meta property="article:published_time" content="2024-01-15T10:00:00">""") })
        assertTrue(ogTags.any { it.contains("""<meta property="article:tag" content="test">""") })
        assertTrue(ogTags.any { it.contains("""<meta property="article:tag" content="article">""") })
    }

    @Test
    fun testGenerateTwitterCardTags() {
        val seoMetadata = SeoMetadata(
            title = "Test Tweet",
            description = "Test tweet description",
            canonicalUrl = "https://example.com/tweet",
            twitterCard = "summary_large_image",
            twitterImage = "https://example.com/tweet-image.jpg"
        )

        val twitterTags = seoMetadata.generateTwitterCardTags()

        assertTrue(twitterTags.any { it.contains("""<meta name="twitter:card" content="summary_large_image">""") })
        assertTrue(twitterTags.any { it.contains("""<meta name="twitter:title" content="Test Tweet">""") })
        assertTrue(twitterTags.any { it.contains("""<meta name="twitter:description" content="Test tweet description">""") })
        assertTrue(twitterTags.any { it.contains("""<meta name="twitter:image" content="https://example.com/tweet-image.jpg">""") })
    }

    @Test
    fun testGenerateJsonLd() {
        val seoMetadata = SeoMetadata(
            title = "Test JSON-LD",
            description = "Test JSON-LD description",
            canonicalUrl = "https://example.com/json-ld",
            ogType = "article",
            author = "JSON Author",
            publishedDate = "2024-01-15T10:00:00",
            keywords = listOf("json", "ld")
        )

        val jsonLd = seoMetadata.generateJsonLd()

        assertTrue(jsonLd.contains(""""@context": "https://schema.org""""))
        assertTrue(jsonLd.contains(""""@type": "BlogPosting""""))
        assertTrue(jsonLd.contains(""""headline": "Test JSON-LD""""))
        assertTrue(jsonLd.contains(""""description": "Test JSON-LD description""""))
        assertTrue(jsonLd.contains(""""url": "https://example.com/json-ld""""))
        assertTrue(jsonLd.contains(""","author": {""") || jsonLd.contains(""""author": {"""))
        assertTrue(jsonLd.contains(""""@type": "Person""""))
        assertTrue(jsonLd.contains(""""name": "JSON Author""""))
        assertTrue(jsonLd.contains(""""datePublished": "2024-01-15T10:00:00""""))
        assertTrue(jsonLd.contains(""""keywords": "json, ld""""))
    }

    @Test
    fun testGenerateAllMetaTags() {
        val seoMetadata = SeoMetadata(
            title = "All Tags Test",
            description = "All tags description",
            canonicalUrl = "https://example.com/all",
            ogType = "article",
            ogSiteName = "All Tags Site",
            ogImage = "https://example.com/all-image.jpg",
            author = "All Tags Author",
            publishedDate = "2024-01-15T10:00:00",
            keywords = listOf("all", "tags"),
            twitterCard = "summary_large_image",
            twitterImage = "https://example.com/twitter-image.jpg"
        )

        val allTags = seoMetadata.generateAllMetaTags()

        assertTrue(allTags.contains("<!-- Standard Meta Tags -->"))
        assertTrue(allTags.contains("<!-- Open Graph Meta Tags -->"))
        assertTrue(allTags.contains("<!-- Twitter Card Meta Tags -->"))
        assertTrue(allTags.contains("<!-- JSON-LD Structured Data -->"))
        assertTrue(allTags.contains("""<script type="application/ld+json">"""))
    }

    @Test
    fun testSeoMetadataForPage() {
        val seoMetadata = SeoMetadata.forPage(
            title = "About Us",
            description = "About our company",
            siteUrl = "https://example.com",
            pagePath = "about",
            siteName = "Example Company"
        )

        assertEquals("About Us", seoMetadata.title)
        assertEquals("About our company", seoMetadata.description)
        assertEquals("https://example.com/about", seoMetadata.canonicalUrl)
        assertEquals("website", seoMetadata.ogType)
        assertEquals("Example Company", seoMetadata.ogSiteName)
        assertEquals("summary", seoMetadata.twitterCard)
    }

    @Test
    fun testHtmlEscapingInSeoMetadata() {
        val seoMetadata = SeoMetadata(
            title = "Test <script>alert('xss')</script>",
            description = "Test & \" ' < > description",
            canonicalUrl = "https://example.com/test",
            keywords = listOf("<script>", "&quot;", "'>")
        )

        val metaTags = seoMetadata.generateStandardMetaTags()

        assertFalse(metaTags.any { it.contains("<script>") })
        assertTrue(metaTags.any { it.contains("&lt;script&gt;") })
        assertTrue(metaTags.any { it.contains("&amp;") })
        assertTrue(metaTags.any { it.contains("&quot;") })
    }

    @Test
    fun testDescriptionTruncation() {
        val longDescription = "a".repeat(200)
        val fragment = Fragment(
            title = "Test",
            slug = "test",
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.now(),
            publishDate = null,
            preview = longDescription,
            content = "Content",
            frontMatter = emptyMap(),
            visible = true
        )

        val seoMetadata = SeoMetadata.fromFragment(
            fragment = fragment,
            siteUrl = "https://example.com"
        )

        assertTrue(seoMetadata.description.length <= 163)
    }

    @Test
    fun testFromFragmentUsesFragmentImageAsFallback() {
        val fragment = Fragment(
            title = "Post with Image",
            slug = "post-with-image",
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.of(2024, 6, 1, 12, 0),
            publishDate = null,
            preview = "A post with a cover image",
            content = "<p>Content</p>",
            frontMatter = emptyMap(),
            image = "/static/images/cover.jpg"
        )

        val seo = SeoMetadata.fromFragment(
            fragment = fragment,
            siteUrl = "https://example.com"
        )

        assertEquals("https://example.com/static/images/cover.jpg", seo.ogImage)
        assertEquals("https://example.com/static/images/cover.jpg", seo.twitterImage)
    }

    @Test
    fun testFromFragmentExplicitImageUrlTakesPrecedenceOverFragmentImage() {
        val fragment = Fragment(
            title = "Post with Image",
            slug = "post-with-image",
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.of(2024, 6, 1, 12, 0),
            publishDate = null,
            preview = "A post with a cover image",
            content = "<p>Content</p>",
            frontMatter = emptyMap(),
            image = "/static/images/cover.jpg"
        )

        val seo = SeoMetadata.fromFragment(
            fragment = fragment,
            siteUrl = "https://example.com",
            imageUrl = "https://cdn.example.com/explicit.jpg"
        )

        assertEquals("https://cdn.example.com/explicit.jpg", seo.ogImage)
        assertEquals("https://cdn.example.com/explicit.jpg", seo.twitterImage)
    }

    @Test
    fun testFromFragmentNoImageAtAll() {
        val fragment = Fragment(
            title = "Post without Image",
            slug = "post-without-image",
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.of(2024, 6, 1, 12, 0),
            publishDate = null,
            preview = "A post without image",
            content = "<p>Content</p>",
            frontMatter = emptyMap()
        )

        val seo = SeoMetadata.fromFragment(
            fragment = fragment,
            siteUrl = "https://example.com"
        )

        assertNull(seo.ogImage)
        assertNull(seo.twitterImage)
    }

    @Test
    fun testJsonEscaping() {
        val seoMetadata = SeoMetadata(
            title = "Test with \"quotes\"",
            description = "Test with \\backslash\\ and \\n newline",
            canonicalUrl = "https://example.com/test"
        )

        val jsonLd = seoMetadata.generateJsonLd()

        assertTrue(jsonLd.contains("\\\""))
        assertTrue(jsonLd.contains("\\\\"))
        assertTrue(jsonLd.contains("\\n"))
    }
}
