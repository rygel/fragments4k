package io.github.rygel.fragments.sitemap

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import javax.xml.parsers.DocumentBuilderFactory

class SitemapGeneratorTest {
    private val repository = mockk<FragmentRepository>()

    @Test
    fun `generated sitemap is valid XML`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns listOf(fragment("hello-world", "Hello World"))
            val generator = SitemapGenerator(repository, "https://example.com")

            val xml = generator.generateSitemap()
            assertValidXml(xml)
        }

    @Test
    fun `sitemap contains root url and fragment urls`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("post-one", "Post One"),
                    fragment("post-two", "Post Two"),
                )
            val generator = SitemapGenerator(repository, "https://example.com")

            val xml = generator.generateSitemap()
            assertValidXml(xml)

            assertTrue(xml.contains("https://example.com"), "should contain root URL")
            assertTrue(xml.contains("/post-one"), "should contain post-one URL")
            assertTrue(xml.contains("/post-two"), "should contain post-two URL")
        }

    @Test
    fun `sitemap properly escapes XML special characters in titles`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment(
                        slug = "tom-and-jerry",
                        title = "Tom & Jerry <Adventures> \"Fun\"",
                        imageUrl = "https://example.com/img/tom&jerry.jpg",
                    ),
                )
            val generator = SitemapGenerator(repository, "https://example.com")

            val xml = generator.generateSitemap()
            assertValidXml(xml)

            assertFalse(xml.contains("Tom & Jerry"), "raw & must be escaped")
            assertFalse(xml.contains("<Adventures>"), "raw < > must be escaped")
            assertTrue(xml.contains("Tom &amp; Jerry"), "& should be escaped to &amp;")
        }

    @Test
    fun `sitemap properly escapes ampersands in URLs`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment(
                        slug = "search",
                        title = "Search",
                        url = "/search?q=test&page=1",
                    ),
                )
            val generator = SitemapGenerator(repository, "https://example.com")

            val xml = generator.generateSitemap()
            assertValidXml(xml)

            assertTrue(
                xml.contains("https://example.com/search?q=test&amp;page=1"),
                "& in URLs must be escaped to &amp;",
            )
        }

    @Test
    fun `sitemap with no fragments produces valid XML with only root url`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns emptyList()
            val generator = SitemapGenerator(repository, "https://example.com")

            val xml = generator.generateSitemap()
            assertValidXml(xml)

            assertTrue(xml.contains("https://example.com"), "should contain root URL")
        }

    @Test
    fun `sitemap includes image elements when fragment has image`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("with-image", "Post With Image", imageUrl = "https://example.com/img/photo.jpg"),
                )
            val generator = SitemapGenerator(repository, "https://example.com")

            val xml = generator.generateSitemap()
            assertValidXml(xml)

            assertTrue(xml.contains("https://example.com/img/photo.jpg"), "should contain image URL")
        }

    @Test
    fun `sitemap declares correct namespaces`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns emptyList()
            val generator = SitemapGenerator(repository, "https://example.com")

            val xml = generator.generateSitemap()
            assertValidXml(xml)

            assertTrue(xml.contains("http://www.sitemaps.org/schemas/sitemap/0.9"), "should declare sitemap namespace")
            assertTrue(xml.contains("http://www.google.com/schemas/sitemap-image/1.1"), "should declare image namespace")
        }

    @Test
    fun `sitemap contains valid priority values`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns listOf(fragment("post", "A Post"))
            val generator = SitemapGenerator(repository, "https://example.com")

            val xml = generator.generateSitemap()
            assertValidXml(xml)

            val priorityPattern = Regex("<priority>(\\d\\.\\d)</priority>")
            val priorities = priorityPattern.findAll(xml).map { it.groupValues[1].toDouble() }.toList()
            assertTrue(priorities.isNotEmpty(), "should contain priority elements")
            priorities.forEach { priority ->
                assertTrue(priority in 0.0..1.0, "priority $priority must be between 0.0 and 1.0")
            }
        }

    @Test
    fun `sitemap includes fragments from multiple repositories`() =
        runBlocking {
            val staticRepo = mockk<FragmentRepository>()
            val blogRepo = mockk<FragmentRepository>()
            coEvery { staticRepo.getAllVisible() } returns listOf(fragment("about", "About Us"))
            coEvery { blogRepo.getAllVisible() } returns
                listOf(
                    fragment("hello-world", "Hello World", url = "/blog/hello-world"),
                    fragment("second-post", "Second Post", url = "/blog/second-post"),
                )

            val generator =
                SitemapGenerator(
                    repositories = listOf(staticRepo, blogRepo),
                    siteUrl = "https://example.com",
                )

            val xml = generator.generateSitemap()
            assertValidXml(xml)

            assertTrue(xml.contains("/about"), "should contain static page")
            assertTrue(xml.contains("/blog/hello-world"), "should contain blog post")
            assertTrue(xml.contains("/blog/second-post"), "should contain second blog post")
        }

    @Test
    fun `sitemap deduplicates fragments across repositories by slug`() =
        runBlocking {
            val repo1 = mockk<FragmentRepository>()
            val repo2 = mockk<FragmentRepository>()
            coEvery { repo1.getAllVisible() } returns listOf(fragment("about", "About Us"))
            coEvery { repo2.getAllVisible() } returns listOf(fragment("about", "About Us Duplicate"))

            val generator =
                SitemapGenerator(
                    repositories = listOf(repo1, repo2),
                    siteUrl = "https://example.com",
                )

            val xml = generator.generateSitemap()
            assertValidXml(xml)

            val aboutCount = Regex("/about</loc>").findAll(xml).count()
            assertTrue(aboutCount == 1, "duplicate slug should appear only once, found $aboutCount")
        }

    @Test
    fun `root URL always has priority 1_0`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns listOf(fragment("post", "A Post"))
            val generator = SitemapGenerator(repository, "https://example.com")

            val xml = generator.generateSitemap()
            assertValidXml(xml)

            assertTrue(xml.contains("<priority>1.0</priority>"), "root URL should have priority 1.0")
        }

    private fun assertValidXml(xml: String) {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
        val builder = factory.newDocumentBuilder()
        builder.parse(xml.byteInputStream())
    }

    private fun fragment(
        slug: String,
        title: String,
        url: String = "/$slug",
        imageUrl: String? = null,
    ): Fragment {
        val frontMatter = mutableMapOf<String, Any>("title" to title, "slug" to slug)
        if (imageUrl != null) {
            frontMatter["image"] = imageUrl
        }
        return Fragment(
            title = title,
            slug = slug,
            content = "Test content",
            preview = "Test preview",
            publishDate = null,
            frontMatter = frontMatter,
            date = LocalDateTime.of(2026, 1, 15, 10, 0),
            status = FragmentStatus.PUBLISHED,
            visible = true,
            resolvedUrl = url,
        )
    }
}
