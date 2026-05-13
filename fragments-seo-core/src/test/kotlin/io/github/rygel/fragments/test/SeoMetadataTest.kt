package io.github.rygel.fragments.test

import io.github.rygel.fragments.SeoMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeoMetadataTest {
    @Test
    fun testGenerateStandardMetaTags() {
        val seoMetadata =
            SeoMetadata(
                title = "Test Page",
                description = "Test description",
                canonicalUrl = "https://example.com/test",
                keywords = listOf("test", "example"),
                author = "Test Author",
            )

        val metaTags = seoMetadata.generateStandardMetaTags()

        assertTrue(metaTags.any { it.contains("""<meta name="description" content="Test description">""") })
        assertTrue(metaTags.any { it.contains("""<meta name="keywords" content="test, example">""") })
        assertTrue(metaTags.any { it.contains("""<meta name="author" content="Test Author">""") })
        assertTrue(metaTags.any { it.contains("""<link rel="canonical" href="https://example.com/test">""") })
    }

    @Test
    fun testGenerateOpenGraphTags() {
        val seoMetadata =
            SeoMetadata(
                title = "Test Article",
                description = "Test description",
                canonicalUrl = "https://example.com/article",
                ogType = "article",
                ogSiteName = "My Site",
                ogImage = "https://example.com/image.jpg",
                author = "Test Author",
                publishedDate = "2024-01-15T10:00:00",
                keywords = listOf("test", "article"),
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
        val seoMetadata =
            SeoMetadata(
                title = "Test Tweet",
                description = "Test tweet description",
                canonicalUrl = "https://example.com/tweet",
                twitterCard = "summary_large_image",
                twitterImage = "https://example.com/tweet-image.jpg",
            )

        val twitterTags = seoMetadata.generateTwitterCardTags()

        assertTrue(twitterTags.any { it.contains("""<meta name="twitter:card" content="summary_large_image">""") })
        assertTrue(twitterTags.any { it.contains("""<meta name="twitter:title" content="Test Tweet">""") })
        assertTrue(twitterTags.any { it.contains("""<meta name="twitter:description" content="Test tweet description">""") })
        assertTrue(twitterTags.any { it.contains("""<meta name="twitter:image" content="https://example.com/tweet-image.jpg">""") })
    }

    @Test
    fun testGenerateJsonLd() {
        val seoMetadata =
            SeoMetadata(
                title = "Test JSON-LD",
                description = "Test JSON-LD description",
                canonicalUrl = "https://example.com/json-ld",
                ogType = "article",
                author = "JSON Author",
                publishedDate = "2024-01-15T10:00:00",
                keywords = listOf("json", "ld"),
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
        val seoMetadata =
            SeoMetadata(
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
                twitterImage = "https://example.com/twitter-image.jpg",
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
        val seoMetadata =
            SeoMetadata.forPage(
                title = "About Us",
                description = "About our company",
                siteUrl = "https://example.com",
                pagePath = "about",
                siteName = "Example Company",
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
        val seoMetadata =
            SeoMetadata(
                title = "Test <script>alert('xss')</script>",
                description = "Test & \" ' < > description",
                canonicalUrl = "https://example.com/test",
                keywords = listOf("<script>", "&quot;", "'>"),
            )

        val metaTags = seoMetadata.generateStandardMetaTags()

        assertFalse(metaTags.any { it.contains("<script>") })
        assertTrue(metaTags.any { it.contains("&lt;script&gt;") })
        assertTrue(metaTags.any { it.contains("&amp;") })
        assertTrue(metaTags.any { it.contains("&quot;") })
    }

    @Test
    fun testJsonLdPersonSchemaWithoutSocialLinks() {
        val seo =
            SeoMetadata(
                title = "Test Post",
                description = "Test description",
                canonicalUrl = "https://example.com/test",
                ogType = "article",
                author = "Jane Doe",
                authorUrl = "https://example.com/blog/author/jane-doe",
            )

        val jsonLd = seo.generateJsonLd()
        assertTrue(jsonLd.contains(""""url": "https://example.com/blog/author/jane-doe""""))
        assertFalse(jsonLd.contains("sameAs"))
    }

    @Test
    fun testJsonLdPersonSchemaWithoutAuthorUrl() {
        val seo =
            SeoMetadata(
                title = "Test Post",
                description = "Test description",
                canonicalUrl = "https://example.com/test",
                ogType = "article",
                author = "Jane Doe",
                authorSocialLinks = listOf("https://github.com/janedoe"),
            )

        val jsonLd = seo.generateJsonLd()
        assertTrue(jsonLd.contains(""""name": "Jane Doe""""))
        val personBlock = jsonLd.substringAfter(""""author":""").substringBefore("}")
        assertFalse(personBlock.contains(""""url":"""))
        assertTrue(jsonLd.contains(""""sameAs": ["https://github.com/janedoe"]"""))
    }

    @Test
    fun testTwitterImageFallsBackToOgImage() {
        val seo =
            SeoMetadata(
                title = "Test",
                description = "Test description",
                canonicalUrl = "https://example.com/test",
                ogImage = "https://example.com/og-image.jpg",
                twitterImage = null,
            )

        val twitterTags = seo.generateTwitterCardTags()

        assertTrue(twitterTags.any { it.contains("""<meta name="twitter:image" content="https://example.com/og-image.jpg">""") })
    }

    @Test
    fun testTwitterImageExplicitValueTakesPrecedenceOverOgImage() {
        val seo =
            SeoMetadata(
                title = "Test",
                description = "Test description",
                canonicalUrl = "https://example.com/test",
                ogImage = "https://example.com/og-image.jpg",
                twitterImage = "https://example.com/twitter-image.jpg",
            )

        val twitterTags = seo.generateTwitterCardTags()

        assertTrue(twitterTags.any { it.contains("""<meta name="twitter:image" content="https://example.com/twitter-image.jpg">""") })
        assertFalse(twitterTags.any { it.contains("og-image.jpg") })
    }

    @Test
    fun testForPageWithCanonicalUrl() {
        val seo =
            SeoMetadata.forPageWithUrl(
                title = "About Us",
                description = "About our company",
                canonicalUrl = "https://example.com/en/about",
                siteName = "Example Company",
            )

        assertEquals("About Us", seo.title)
        assertEquals("About our company", seo.description)
        assertEquals("https://example.com/en/about", seo.canonicalUrl)
        assertEquals("website", seo.ogType)
        assertEquals("Example Company", seo.ogSiteName)
    }

    @Test
    fun testForPageWithCanonicalUrlAndLocale() {
        val seo =
            SeoMetadata.forPageWithUrl(
                title = "Uber uns",
                description = "Uber unser Unternehmen",
                canonicalUrl = "https://example.com/de/ueber-uns",
                locale = "de_DE",
                siteName = "Example Company",
            )

        assertEquals("de_DE", seo.locale)
        assertEquals("https://example.com/de/ueber-uns", seo.canonicalUrl)
    }

    @Test
    fun testForPageWithCanonicalUrlIncludesOgAndTwitterTags() {
        val seo =
            SeoMetadata.forPageWithUrl(
                title = "Contact",
                description = "Get in touch",
                canonicalUrl = "https://example.com/contact",
                imageUrl = "https://example.com/og.jpg",
            )

        val allTags = seo.generateAllMetaTags()
        assertTrue(allTags.contains("og:title"))
        assertTrue(allTags.contains("twitter:title"))
        assertTrue(allTags.contains("og:image"))
    }

    @Test
    fun testTwitterImageAndOgImageBothNull() {
        val seo =
            SeoMetadata(
                title = "Test",
                description = "Test description",
                canonicalUrl = "https://example.com/test",
            )

        val twitterTags = seo.generateTwitterCardTags()

        assertFalse(twitterTags.any { it.contains("twitter:image") })
    }

    @Test
    fun testAdditionalMetaTagsAreAppended() {
        val seo =
            SeoMetadata(
                title = "Test",
                description = "Test description",
                canonicalUrl = "https://example.com/test",
                additionalMetaTags =
                    listOf(
                        """<meta property="fb:app_id" content="123456789">""",
                        """<meta property="article:tag" content="kotlin">""",
                    ),
            )

        val allTags = seo.generateAllMetaTags()

        assertTrue(allTags.contains("""<meta property="fb:app_id" content="123456789">"""))
        assertTrue(allTags.contains("""<meta property="article:tag" content="kotlin">"""))
    }

    @Test
    fun testAdditionalJsonLdIsAppended() {
        val seo =
            SeoMetadata(
                title = "Test",
                description = "Test description",
                canonicalUrl = "https://example.com/test",
                additionalJsonLd =
                    """
                    {
                        "@context": "https://schema.org",
                        "@type": "WebSite",
                        "url": "https://example.com",
                        "potentialAction": {
                            "@type": "SearchAction",
                            "target": "https://example.com/search?q={search_term_string}",
                            "query-input": "required name=search_term_string"
                        }
                    }
                    """.trimIndent(),
            )

        val allTags = seo.generateAllMetaTags()

        assertTrue(allTags.contains("WebSite"))
        assertTrue(allTags.contains("SearchAction"))
        assertTrue(allTags.contains("</script>"))
        val scriptBlocks = Regex("""<script type="application/ld\+json">""").findAll(allTags).toList()
        assertEquals(2, scriptBlocks.size)
    }

    @Test
    fun testNoAdditionalFieldsProduceNoExtraOutput() {
        val seo =
            SeoMetadata(
                title = "Test",
                description = "Test description",
                canonicalUrl = "https://example.com/test",
            )

        val allTags = seo.generateAllMetaTags()
        val scriptBlocks = Regex("""<script type="application/ld\+json">""").findAll(allTags).toList()
        assertEquals(1, scriptBlocks.size)
        assertFalse(allTags.contains("fb:app_id"))
    }

    @Test
    fun testJsonEscaping() {
        val seoMetadata =
            SeoMetadata(
                title = "Test with \"quotes\"",
                description = "Test with \\backslash\\ and \\n newline",
                canonicalUrl = "https://example.com/test",
            )

        val jsonLd = seoMetadata.generateJsonLd()

        assertTrue(jsonLd.contains("\\\""))
        assertTrue(jsonLd.contains("\\\\"))
        assertTrue(jsonLd.contains("\\n"))
    }
}
