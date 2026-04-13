package io.github.rygel.fragments.rss

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

class RssGeneratorTest {
    private val repository = mockk<FragmentRepository>()

    @Test
    fun `generated RSS feed is valid XML`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns listOf(fragment("post", "A Post"))
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed()
            assertValidXml(xml)
        }

    @Test
    fun `feed contains channel metadata`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns emptyList()
            val generator = RssGenerator(repository)

            val xml =
                generator.generateFeed(
                    siteTitle = "Test Blog",
                    siteDescription = "A test blog",
                    siteUrl = "https://example.com",
                )
            assertValidXml(xml)

            assertTrue(xml.contains("Test Blog"), "should contain site title")
            assertTrue(xml.contains("A test blog"), "should contain site description")
            assertTrue(xml.contains("https://example.com"), "should contain site URL")
        }

    @Test
    fun `feed contains item elements with correct fields`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("hello-world", "Hello World"),
                )
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed(siteUrl = "https://example.com")
            assertValidXml(xml)

            assertTrue(xml.contains("Hello World"), "should contain item title")
            assertTrue(xml.contains("https://example.com/hello-world"), "should contain item link")
            assertTrue(xml.contains("<guid>"), "should contain guid element")
        }

    @Test
    fun `feed properly escapes XML special characters`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("post", "Tom & Jerry <Adventures>"),
                )
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed()
            assertValidXml(xml)

            assertFalse(xml.contains("Tom & Jerry"), "raw & must be escaped")
            assertTrue(xml.contains("Tom &amp; Jerry"), "& should be escaped to &amp;")
        }

    @Test
    fun `feed includes blog posts from multiple repositories`() =
        runBlocking {
            val staticRepo = mockk<FragmentRepository>()
            val blogRepo = mockk<FragmentRepository>()
            coEvery { staticRepo.getAllVisible() } returns listOf(fragment("about", "About", template = "static"))
            coEvery { blogRepo.getAllVisible() } returns listOf(fragment("post", "Blog Post", template = "blog"))

            val generator = RssGenerator(listOf(staticRepo, blogRepo))

            val xml = generator.generateFeed(siteUrl = "https://example.com")
            assertValidXml(xml)

            assertFalse(xml.contains("/about"), "static pages must not appear in RSS feed")
            assertTrue(xml.contains("/post"), "blog posts must appear in RSS feed")
        }

    @Test
    fun `feed excludes non-blog templates`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("blog-post", "Blog Post", template = "blog"),
                    fragment("about-page", "About", template = "static"),
                    fragment("contact-page", "Contact", template = "default"),
                )
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed(siteUrl = "https://example.com")
            assertValidXml(xml)

            assertTrue(xml.contains("/blog-post"), "blog posts must appear in feed")
            assertFalse(xml.contains("/about-page"), "static pages must not appear in feed")
            assertFalse(xml.contains("/contact-page"), "default-template pages must not appear in feed")
        }

    @Test
    fun `feed deduplicates fragments by slug`() =
        runBlocking {
            val repo1 = mockk<FragmentRepository>()
            val repo2 = mockk<FragmentRepository>()
            coEvery { repo1.getAllVisible() } returns listOf(fragment("about", "About"))
            coEvery { repo2.getAllVisible() } returns listOf(fragment("about", "About Duplicate"))

            val generator = RssGenerator(listOf(repo1, repo2))

            val xml = generator.generateFeed(siteUrl = "https://example.com")
            assertValidXml(xml)

            val itemCount = Regex("<item>").findAll(xml).count()
            assertTrue(itemCount == 1, "duplicate slug should appear only once, found $itemCount")
        }

    @Test
    fun `feed includes categories and tags`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("post", "A Post", categories = listOf("tech"), tags = listOf("kotlin")),
                )
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed()
            assertValidXml(xml)

            assertTrue(xml.contains("tech"), "should contain category")
            assertTrue(xml.contains("kotlin"), "should contain tag")
        }

    @Test
    fun `feed declares Atom namespace for self link`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns emptyList()
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed(feedUrl = "https://example.com/rss.xml")
            assertValidXml(xml)

            assertTrue(xml.contains("http://www.w3.org/2005/Atom"), "should declare Atom namespace")
            assertTrue(xml.contains("https://example.com/rss.xml"), "should contain feed URL")
        }

    @Test
    fun `feed with empty fragments produces valid XML with no items`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns emptyList()
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed()
            assertValidXml(xml)

            assertFalse(xml.contains("<item>"), "empty feed should have no items")
        }

    @Test
    fun `feed limits output to 20 items`() =
        runBlocking {
            val fragments = (1..30).map { fragment("post-$it", "Post $it") }
            coEvery { repository.getAllVisible() } returns fragments
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed()
            assertValidXml(xml)

            val itemCount = Regex("<item>").findAll(xml).count()
            assertTrue(itemCount == 20, "should limit to 20 items, found $itemCount")
        }

    @Test
    fun `feed escapes ampersands in URLs`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("search", "Search", url = "/search?q=test&page=1"),
                )
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed(siteUrl = "https://example.com")
            assertValidXml(xml)

            assertTrue(
                xml.contains("https://example.com/search?q=test&amp;page=1"),
                "& in URLs must be escaped",
            )
        }

    @Test
    fun `feed handles fragment with null date`() =
        runBlocking {
            val frag =
                Fragment(
                    title = "No Date",
                    slug = "no-date",
                    content = "Content",
                    preview = "Preview",
                    publishDate = null,
                    frontMatter = emptyMap(),
                    date = null,
                    status = FragmentStatus.PUBLISHED,
                    visible = true,
                    resolvedUrl = "/no-date",
                    template = "blog",
                )
            coEvery { repository.getAllVisible() } returns listOf(frag)
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed()
            assertValidXml(xml)

            assertTrue(xml.contains("No Date"), "should still contain the item")
        }

    @Test
    fun `feed escapes all XML special characters in descriptions`() =
        runBlocking {
            val frag =
                Fragment(
                    title = "Special",
                    slug = "special",
                    content = "<p>Code: if (a < b && c > d) use \"quotes\" and 'apostrophes'</p>",
                    preview = "Code: if (a < b && c > d) use \"quotes\" and 'apostrophes'",
                    publishDate = null,
                    frontMatter = emptyMap(),
                    date = LocalDateTime.of(2026, 1, 15, 10, 0),
                    status = FragmentStatus.PUBLISHED,
                    visible = true,
                    resolvedUrl = "/special",
                    template = "blog",
                )
            coEvery { repository.getAllVisible() } returns listOf(frag)
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed()
            assertValidXml(xml)
        }

    @Test
    fun `feed sorts items by date descending`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("old", "Old Post", date = LocalDateTime.of(2025, 1, 1, 10, 0)),
                    fragment("new", "New Post", date = LocalDateTime.of(2026, 6, 1, 10, 0)),
                    fragment("mid", "Mid Post", date = LocalDateTime.of(2026, 3, 1, 10, 0)),
                )
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed()
            assertValidXml(xml)

            val newPos = xml.indexOf("New Post")
            val midPos = xml.indexOf("Mid Post")
            val oldPos = xml.indexOf("Old Post")
            assertTrue(newPos < midPos && midPos < oldPos, "items should be sorted newest first")
        }

    @Test
    fun `feed uses locale-independent date formatting`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("post", "A Post", date = LocalDateTime.of(2026, 3, 15, 10, 30, 0)),
                )
            val generator = RssGenerator(repository)

            val xml = generator.generateFeed()
            assertValidXml(xml)

            assertTrue(xml.contains("Sun, 15 Mar 2026"), "date should use English locale day/month names")
        }

    @Test
    fun `fragments without resolvedUrl are excluded from feed`() =
        runBlocking {
            coEvery { repository.getAllVisible() } returns
                listOf(
                    fragment("resolved-post", "Resolved Post"),
                    Fragment(
                        title = "Unresolved Post",
                        slug = "unresolved-post",
                        content = "Content",
                        preview = "Preview",
                        publishDate = null,
                        frontMatter = emptyMap(),
                        date = LocalDateTime.of(2026, 1, 15, 10, 0),
                        status = FragmentStatus.PUBLISHED,
                        visible = true,
                        template = "blog",
                    ),
                )
            val xml = RssGenerator(repository).generateFeed(siteUrl = "https://example.com")
            assertValidXml(xml)
            assertTrue(xml.contains("Resolved Post"), "resolved fragment must be present")
            assertFalse(xml.contains("Unresolved Post"), "unresolved fragment must be excluded")
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
        categories: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        url: String = "/$slug",
        date: LocalDateTime = LocalDateTime.of(2026, 1, 15, 10, 0),
        template: String = "blog",
    ): Fragment =
        Fragment(
            title = title,
            slug = slug,
            content = "Test content",
            preview = "Test preview",
            publishDate = null,
            frontMatter = mapOf("title" to title, "slug" to slug),
            date = date,
            status = FragmentStatus.PUBLISHED,
            visible = true,
            resolvedUrl = url,
            categories = categories,
            tags = tags,
            template = template,
        )
}
