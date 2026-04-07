package io.github.rygel.fragments.sitemap

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.time.LocalDateTime
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * Validates SitemapGenerator output against the sitemap protocol:
 * https://www.sitemaps.org/protocol.html
 */
class SitemapGeneratorTest {
    private val repository = mockk<FragmentRepository>()

    // -------------------------------------------------------------------------
    // XML structure
    // -------------------------------------------------------------------------

    @Test
    fun `generated sitemap is valid XML`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns listOf(fragment("hello-world", "Hello World"))
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
            assertValidXml(xml)
        }

    @Test
    fun `sitemap root element is urlset`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns emptyList()
            val doc = parseXml(SitemapGenerator(repository, "https://example.com").generateSitemap())
            assertEquals("urlset", doc.documentElement.localName, "root element must be 'urlset'")
        }

    @Test
    fun `sitemap declares sitemap namespace`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns emptyList()
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
            assertTrue(xml.contains("http://www.sitemaps.org/schemas/sitemap/0.9"), "must declare sitemap namespace")
        }

    @Test
    fun `sitemap declares image namespace`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns emptyList()
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
            assertTrue(xml.contains("http://www.google.com/schemas/sitemap-image/1.1"), "must declare image namespace")
        }

    // -------------------------------------------------------------------------
    // Root URL
    // -------------------------------------------------------------------------

    @Test
    fun `root URL is always present`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns emptyList()
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
            assertTrue(xml.contains("<loc>https://example.com</loc>"), "root URL must always be present")
        }

    @Test
    fun `root URL has priority 1_0`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns emptyList()
            val doc = parseXml(SitemapGenerator(repository, "https://example.com").generateSitemap())
            val priorities = xpath(doc, "//sm:url[sm:loc='https://example.com']/sm:priority")
            assertEquals(1, priorities.length, "root URL must have a priority element")
            assertEquals("1.0", priorities.item(0).textContent)
        }

    @Test
    fun `empty sitemap produces valid XML with only root url`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns emptyList()
            val doc = parseXml(SitemapGenerator(repository, "https://example.com").generateSitemap())
            val urls = xpath(doc, "//sm:url")
            assertEquals(1, urls.length, "empty sitemap should have exactly one url (root)")
        }

    // -------------------------------------------------------------------------
    // W3C Datetime format (sitemap protocol requirement)
    // -------------------------------------------------------------------------

    @Test
    fun `lastmod dates use W3C Datetime format (ISO 8601 date)`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(fragment("post", "Post", date = LocalDateTime.of(2026, 3, 15, 10, 30)))
            val doc = parseXml(SitemapGenerator(repository, "https://example.com").generateSitemap())
            val lastmods = xpath(doc, "//sm:url/sm:lastmod")
            assertTrue(lastmods.length > 0, "must contain lastmod elements")
            lastmods.forEachText { value ->
                assertTrue(
                    value.matches(Regex("\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}(:\\d{2})?([+-]\\d{2}:\\d{2}|Z))?")),
                    "lastmod '$value' must be W3C Datetime (ISO 8601)",
                )
            }
        }

    @Test
    fun `lastmod date for a known fragment is correct`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(fragment("post", "Post", date = LocalDateTime.of(2026, 3, 15, 10, 30)))
            val doc = parseXml(SitemapGenerator(repository, "https://example.com").generateSitemap())
            val lastmods = xpath(doc, "//sm:url[contains(sm:loc,'post')]/sm:lastmod")
            assertEquals(1, lastmods.length)
            assertEquals("2026-03-15", lastmods.item(0).textContent)
        }

    @Test
    fun `lastmod does not contain bare time without timezone`() =
        runBlocking {
            // ISO_LOCAL_DATE_TIME produces "2026-03-15T10:30:00" — no timezone, invalid per protocol
            coEvery { repository.getAllVisible() } returns
                listOf(fragment("post", "Post", date = LocalDateTime.of(2026, 3, 15, 10, 30)))
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
            assertFalse(
                Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}").containsMatchIn(xml),
                "lastmod must not contain a time without timezone — use date-only or include offset",
            )
        }

    // -------------------------------------------------------------------------
    // Priority (must be between 0.0 and 1.0, one decimal place)
    // -------------------------------------------------------------------------

    @Test
    fun `all priority values are in range 0_0 to 1_0`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("post-a", "Post A"),
                    fragment("post-b", "Post B"),
                )
            val doc = parseXml(SitemapGenerator(repository, "https://example.com").generateSitemap())
            val priorities = xpath(doc, "//sm:url/sm:priority")
            assertTrue(priorities.length > 0)
            priorities.forEachText { value ->
                val p = value.toDouble()
                assertTrue(p in 0.0..1.0, "priority $p must be between 0.0 and 1.0")
            }
        }

    @Test
    fun `priority values use locale-independent decimal formatting`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns listOf(fragment("post", "Post"))
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
            assertFalse(xml.contains(","), "priority must use '.' as decimal separator, not ','")
        }

    // -------------------------------------------------------------------------
    // changefreq valid values
    // -------------------------------------------------------------------------

    @Test
    fun `all changefreq values are valid protocol values`() =
        runBlocking {
            val valid = setOf("always", "hourly", "daily", "weekly", "monthly", "yearly", "never")
            coEvery { repository.getAllVisible() } returns listOf(fragment("post", "Post"))
            val doc = parseXml(SitemapGenerator(repository, "https://example.com").generateSitemap())
            val freqs = xpath(doc, "//sm:url/sm:changefreq")
            freqs.forEachText { value ->
                assertTrue(value in valid, "changefreq '$value' is not a valid protocol value")
            }
        }

    // -------------------------------------------------------------------------
    // URL content
    // -------------------------------------------------------------------------

    @Test
    fun `fragment resolvedUrl is used for loc`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(fragment("hello-world", "Hello World", resolvedUrl = "/blog/2026/03/hello-world"))
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
            assertTrue(xml.contains("https://example.com/blog/2026/03/hello-world"), "resolved URL must be used")
            assertFalse(
                xml.contains("<loc>https://example.com/hello-world</loc>"),
                "bare slug URL must not appear when resolvedUrl is set",
            )
        }

    @Test
    fun `XML special characters in URLs are escaped`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(fragment("search", "Search", resolvedUrl = "/search?q=test&page=1"))
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
            assertValidXml(xml)
            assertTrue(xml.contains("&amp;"), "& in URLs must be escaped to &amp;")
            assertFalse(xml.contains("q=test&page"), "raw & must not appear in XML")
        }

    @Test
    fun `XML special characters in titles are escaped`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("tom-jerry", "Tom & Jerry <Adventures>", imageUrl = "https://example.com/img/photo.jpg"),
                )
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
            assertValidXml(xml)
            assertTrue(xml.contains("Tom &amp; Jerry"), "& must be escaped in image caption")
        }

    // -------------------------------------------------------------------------
    // Image sitemap extension
    // -------------------------------------------------------------------------

    @Test
    fun `image extension element is included when fragment has image`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(fragment("post", "Post", imageUrl = "https://example.com/img/photo.jpg"))
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
            assertValidXml(xml)
            assertTrue(xml.contains("https://example.com/img/photo.jpg"), "image URL must appear")
        }

    @Test
    fun `image element is omitted when fragment has no image`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns listOf(fragment("post", "Post"))
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
            assertValidXml(xml)
            assertFalse(xml.contains("image:image"), "no image element when fragment has no image")
        }

    // -------------------------------------------------------------------------
    // Multiple repositories and deduplication
    // -------------------------------------------------------------------------

    @Test
    fun `fragments from multiple repositories are all included`() =
        runBlocking {
            val repo1 = mockk<FragmentRepository>()
            val repo2 = mockk<FragmentRepository>()
            coEvery { repo1.getAllVisible() } returns listOf(fragment("about", "About"))
            coEvery { repo2.getAllVisible() } returns
                listOf(fragment("post", "Post", resolvedUrl = "/blog/2026/03/post"))
            val xml = SitemapGenerator(listOf(repo1, repo2), "https://example.com").generateSitemap()
            assertValidXml(xml)
            assertTrue(xml.contains("/about"))
            assertTrue(xml.contains("/blog/2026/03/post"))
        }

    @Test
    fun `duplicate slugs across repositories appear only once`() =
        runBlocking {
            val repo1 = mockk<FragmentRepository>()
            val repo2 = mockk<FragmentRepository>()
            coEvery { repo1.getAllVisible() } returns listOf(fragment("about", "About"))
            coEvery { repo2.getAllVisible() } returns listOf(fragment("about", "About Duplicate"))
            val doc = parseXml(SitemapGenerator(listOf(repo1, repo2), "https://example.com").generateSitemap())
            val aboutLocs = xpath(doc, "//sm:url[contains(sm:loc,'/about')]")
            assertEquals(1, aboutLocs.length, "duplicate slug must appear only once")
        }

    // -------------------------------------------------------------------------
    // Template exclusion
    // -------------------------------------------------------------------------

    @Test
    fun `fragments with excluded templates are not included`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("public-page", "Public"),
                    fragment("email-tpl", "Email").copy(template = "email"),
                )
            val xml =
                SitemapGenerator(
                    repository,
                    "https://example.com",
                    excludedTemplates = setOf("email"),
                ).generateSitemap()
            assertValidXml(xml)
            assertTrue(xml.contains("/public-page"))
            assertFalse(xml.contains("/email-tpl"))
        }

    // -------------------------------------------------------------------------
    // Pre-resolved fragments override
    // -------------------------------------------------------------------------

    @Test
    fun `resolvedFragments parameter bypasses repository call`() =
        runBlocking {
            val preResolved = listOf(fragment("resolved-post", "Resolved", resolvedUrl = "/blog/2026/03/resolved-post"))
            // repository should not be called — we pass fragments directly
            val xml = SitemapGenerator(repository, "https://example.com").generateSitemap(preResolved)
            assertValidXml(xml)
            assertTrue(xml.contains("/blog/2026/03/resolved-post"))
        }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun assertValidXml(xml: String) {
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(xml.byteInputStream())
    }

    private fun parseXml(xml: String): Document =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(xml.byteInputStream())

    /**
     * Evaluates an XPath expression against a namespace-aware sitemap document.
     * Use prefix `sm:` for sitemap elements and `image:` for image extension elements.
     * Example: `//sm:url/sm:priority`, `//sm:url[sm:loc='https://example.com']/sm:priority`
     */
    private fun xpath(
        doc: Document,
        expression: String,
    ): NodeList {
        val xp = XPathFactory.newInstance().newXPath()
        xp.namespaceContext =
            object : NamespaceContext {
                override fun getNamespaceURI(prefix: String): String =
                    when (prefix) {
                        "sm" -> "http://www.sitemaps.org/schemas/sitemap/0.9"
                        "image" -> "http://www.google.com/schemas/sitemap-image/1.1"
                        else -> XMLConstants.NULL_NS_URI
                    }

                override fun getPrefix(namespaceURI: String): String? = null

                override fun getPrefixes(namespaceURI: String): Iterator<String> = emptyList<String>().iterator()
            }
        return xp.evaluate(expression, doc, XPathConstants.NODESET) as NodeList
    }

    private fun NodeList.forEachText(block: (String) -> Unit) {
        for (i in 0 until length) block(item(i).textContent)
    }

    private fun fragment(
        slug: String,
        title: String,
        resolvedUrl: String = "/$slug",
        imageUrl: String? = null,
        date: LocalDateTime = LocalDateTime.of(2026, 1, 15, 10, 0),
        template: String = "default",
    ): Fragment {
        val frontMatter = mutableMapOf<String, Any>("title" to title, "slug" to slug)
        if (imageUrl != null) frontMatter["image"] = imageUrl
        return Fragment(
            title = title,
            slug = slug,
            content = "Test content",
            preview = "Test preview",
            publishDate = null,
            frontMatter = frontMatter,
            date = date,
            status = FragmentStatus.PUBLISHED,
            visible = true,
            resolvedUrl = resolvedUrl,
            template = template,
        )
    }
}
