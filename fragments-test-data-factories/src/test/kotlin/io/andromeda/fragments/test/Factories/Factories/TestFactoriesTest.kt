package io.github.rygel.fragments.test

import io.github.rygel.fragments.FragmentStatus
import io.github.rygel.fragments.SeriesStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class FragmentFactoryTest {
    
    @Test
    fun createBasicFragment() {
        val fragment = FragmentFactory.create()
        
        assertNotNull(fragment)
        assertEquals("Test Fragment", fragment.title)
        assertEquals("test-fragment", fragment.slug)
        assertEquals(FragmentStatus.PUBLISHED, fragment.status)
        assertTrue(fragment.visible)
    }
    
    @Test
    fun createPublishedFragment() {
        val fragment = FragmentFactory.published()
        
        assertEquals(FragmentStatus.PUBLISHED, fragment.status)
        assertTrue(fragment.visible)
    }
    
    @Test
    fun createDraftFragment() {
        val fragment = FragmentFactory.draft()
        
        assertEquals(FragmentStatus.DRAFT, fragment.status)
        assertFalse(fragment.visible)
    }
    
    @Test
    fun createFragmentWithCategories() {
        val fragment = FragmentFactory.withCategories("kotlin", "java")
        
        assertEquals(2, fragment.categories.size)
        assertTrue(fragment.categories.contains("kotlin"))
        assertTrue(fragment.categories.contains("java"))
    }
    
    @Test
    fun createFragmentWithTags() {
        val fragment = FragmentFactory.withTags("test", "example")
        
        assertEquals(2, fragment.tags.size)
        assertTrue(fragment.tags.contains("test"))
        assertTrue(fragment.tags.contains("example"))
    }
    
    @Test
    fun createFragmentWithSeries() {
        val fragment = FragmentFactory.withSeries("test-series", 1, "Test Series")
        
        assertEquals("test-series", fragment.seriesSlug)
        assertEquals(1, fragment.seriesPart)
        assertEquals("Test Series", fragment.seriesTitle)
    }
    
    @Test
    fun createScheduledFragment() {
        val publishDate = java.time.LocalDateTime.now().plusDays(1)
        val fragment = FragmentFactory.scheduled(publishDate)
        
        assertEquals(FragmentStatus.SCHEDULED, fragment.status)
        assertEquals(publishDate, fragment.publishDate)
    }
    
    @Test
    fun createExpiringFragment() {
        val expiryDate = java.time.LocalDateTime.now().plusDays(7)
        val fragment = FragmentFactory.expiring(expiryDate)
        
        assertEquals(FragmentStatus.PUBLISHED, fragment.status)
        assertEquals(expiryDate, fragment.expiryDate)
    }
    
    @Test
    fun createCompleteFragment() {
        val fragment = FragmentFactory.complete()
        
        assertEquals("Complete Test Fragment", fragment.title)
        assertEquals("complete-test-fragment", fragment.slug)
        assertTrue(fragment.categories.isNotEmpty())
        assertTrue(fragment.tags.isNotEmpty())
        assertTrue(fragment.authorIds.isNotEmpty())
        assertNotNull(fragment.seriesSlug)
    }
    
    @Test
    fun createMultipleFragments() {
        val fragments = FragmentFactory.createMany(5)
        
        assertEquals(5, fragments.size)
        assertEquals("Test Fragment 1", fragments[0].title)
        assertEquals("Test Fragment 5", fragments[4].title)
    }
    
    @Test
    fun builderCreatesCustomFragment() {
        val fragment = FragmentFactory.Builder()
            .title("Custom Title")
            .slug("custom-slug")
            .content("<p>Custom content</p>")
            .categories(listOf("custom"))
            .build()
        
        assertEquals("Custom Title", fragment.title)
        assertEquals("custom-slug", fragment.slug)
        assertEquals(1, fragment.categories.size)
    }
    
    @Test
    fun builderChaining() {
        val fragment = FragmentFactory.Builder()
            .title("Title 1")
            .slug("slug-1")
            .content("<p>Content 1</p>")
            .tags(listOf("tag1", "tag2"))
            .author("Author 1")
            .status(FragmentStatus.PUBLISHED)
            .visible(true)
            .build()
        
        assertEquals("Title 1", fragment.title)
        assertEquals(2, fragment.tags.size)
        assertEquals("Author 1", fragment.author)
    }
}

class AuthorFactoryTest {
    
    @Test
    fun createBasicAuthor() {
        val author = AuthorFactory.create()
        
        assertNotNull(author)
        assertEquals("Test Author", author.name)
        assertEquals("test-author", author.slug)
        assertEquals("test@example.com", author.email)
    }
    
    @Test
    fun createFullProfileAuthor() {
        val author = AuthorFactory.fullProfile()
        
        assertEquals("Full Test Author", author.name)
        assertEquals("full@example.com", author.email)
        assertNotNull(author.bio)
        assertNotNull(author.avatar)
        assertNotNull(author.location)
        assertNotNull(author.company)
        assertNotNull(author.role)
        assertTrue(author.socialLinks.isNotEmpty())
    }
    
    @Test
    fun createAuthorWithSocialLinks() {
        val author = AuthorFactory.withSocialLinks(
            "twitter" to "@testauthor",
            "github" to "testauthor"
        )
        
        assertEquals(2, author.socialLinks.size)
        assertEquals("@testauthor", author.socialLinks["twitter"])
        assertEquals("testauthor", author.socialLinks["github"])
    }
    
    @Test
    fun createMultipleAuthors() {
        val authors = AuthorFactory.createMany(5)
        
        assertEquals(5, authors.size)
        assertEquals("Test Author 1", authors[0].name)
        assertEquals("Test Author 5", authors[4].name)
    }
    
    @Test
    fun builderCreatesCustomAuthor() {
        val author = AuthorFactory.Builder()
            .name("Custom Author")
            .slug("custom-slug")
            .email("custom@example.com")
            .bio("Custom bio")
            .location("Custom Location")
            .build()
        
        assertEquals("Custom Author", author.name)
        assertEquals("custom-slug", author.slug)
        assertEquals("Custom Location", author.location)
    }
}

class ContentSeriesFactoryTest {
    
    @Test
    fun createBasicSeries() {
        val series = ContentSeriesFactory.create()
        
        assertNotNull(series)
        assertEquals("Test Series", series.title)
        assertEquals("test-series", series.slug)
        assertEquals(SeriesStatus.ACTIVE, series.status)
    }
    
    @Test
    fun createActiveSeries() {
        val series = ContentSeriesFactory.active()
        
        assertEquals(SeriesStatus.ACTIVE, series.status)
    }
    
    @Test
    fun createInactiveSeries() {
        val series = ContentSeriesFactory.inactive()
        
        assertEquals(SeriesStatus.INACTIVE, series.status)
    }
    
    @Test
    fun createDraftSeries() {
        val series = ContentSeriesFactory.draft()
        
        assertEquals(SeriesStatus.DRAFT, series.status)
    }
    
    @Test
    fun createMultipleSeries() {
        val seriesList = ContentSeriesFactory.createMany(5)
        
        assertEquals(5, seriesList.size)
        assertEquals("Test Series 1", seriesList[0].title)
        assertEquals("Test Series 5", seriesList[4].title)
    }
    
    @Test
    fun builderCreatesCustomSeries() {
        val series = ContentSeriesFactory.Builder()
            .title("Custom Series")
            .slug("custom-series")
            .description("Custom description")
            .status(SeriesStatus.INACTIVE)
            .build()
        
        assertEquals("Custom Series", series.title)
        assertEquals("custom-series", series.slug)
        assertEquals(SeriesStatus.INACTIVE, series.status)
    }
}

class RandomDataGeneratorTest {
    
    private lateinit var generator: RandomDataGenerator
    
    @BeforeEach
    fun setUp() {
        generator = RandomDataGenerator()
    }
    
    @Test
    fun generateRandomString() {
        val str = generator.randomString(10)
        
        assertEquals(10, str.length)
        assertTrue(str.all { it.isLetterOrDigit() })
    }
    
    @Test
    fun generateRandomEmail() {
        val email = generator.randomEmail()
        
        assertTrue(email.contains("@"))
        assertTrue(email.contains(".com"))
        assertTrue(email.endsWith(".com"))
    }
    
    @Test
    fun generateRandomDate() {
        val date = generator.randomDate()
        
        assertNotNull(date)
        assertTrue(date.isBefore(java.time.LocalDateTime.now()))
    }
    
    @Test
    fun generateRandomFutureDate() {
        val date = generator.randomFutureDate()
        
        assertNotNull(date)
        assertTrue(date.isAfter(java.time.LocalDateTime.now()))
    }
    
    @Test
    fun generateRandomParagraph() {
        val paragraph = generator.randomParagraph(3)

        assertNotNull(paragraph)
        assertTrue(paragraph.endsWith("."))
        assertEquals(3, paragraph.split(".").size - 1)
    }
    
    @Test
    fun generateRandomTags() {
        val tags = generator.randomTags(5)
        
        assertEquals(5, tags.size)
        tags.forEach { tag -> assertTrue(tag.isNotBlank()) }
    }
    
    @Test
    fun generateRandomCategories() {
        val categories = generator.randomCategories(3)
        
        assertEquals(3, categories.size)
        categories.forEach { category -> assertTrue(category.isNotBlank()) }
    }
    
    @Test
    fun generateRandomFragment() {
        val fragment = generator.randomFragment()
        
        assertNotNull(fragment)
        assertNotNull(fragment.title)
        assertNotNull(fragment.content)
        assertNotNull(fragment.slug)
    }
    
    @Test
    fun generateRandomFragments() {
        val fragments = generator.randomFragments(10)
        
        assertEquals(10, fragments.size)
        fragments.forEach { fragment ->
            assertEquals(FragmentStatus.PUBLISHED, fragment.status)
            assertTrue(fragment.visible)
        }
    }
    
    @Test
    fun generateRandomAuthor() {
        val author = generator.randomAuthor()
        
        assertNotNull(author)
        assertNotNull(author.name)
        assertNotNull(author.slug)
        assertNotNull(author.email)
    }
    
    @Test
    fun generateRandomContentSeries() {
        val series = generator.randomContentSeries()
        
        assertNotNull(series)
        assertNotNull(series.title)
        assertNotNull(series.slug)
        assertNotNull(series.description)
    }
    
    @Test
    fun generateRandomUUID() {
        val uuid = generator.randomUUID()
        
        assertNotNull(uuid)
        assertEquals(36, uuid.length)
    }
    
    @Test
    fun generateLoremIpsum() {
        val lorem = generator.loremIpsum(20)
        
        assertNotNull(lorem)
        assertTrue(lorem.isNotBlank())
        assertTrue(lorem.endsWith("."))
    }
    
    @Test
    fun generateHtmlContent() {
        val html = generator.randomHtmlContent()
        
        assertTrue(html.contains("<div"))
        assertTrue(html.contains("<h1>"))
        assertTrue(html.contains("<p>"))
    }
}

class TestScenarioTest {
    
    @Test
    fun createSimpleBlogScenario() {
        val scenario = TestScenario.simpleBlog()
            .build()
        
        assertEquals("Simple Blog", scenario.name)
        assertTrue(scenario.description.contains("published posts"))
        assertEquals(5, scenario.fragments.size)
        scenario.fragments.forEach { fragment ->
            assertEquals(FragmentStatus.PUBLISHED, fragment.status)
            assertTrue(fragment.visible)
        }
    }
    
    @Test
    fun createBlogWithDraftsScenario() {
        val scenario = TestScenario.blogWithDrafts()
            .build()
        
        assertEquals("Blog with Drafts", scenario.name)
        assertTrue(scenario.description.contains("both published and draft"))
        
        val publishedFragments = scenario.fragments.filter { it.status == FragmentStatus.PUBLISHED }
        val draftFragments = scenario.fragments.filter { it.status == FragmentStatus.DRAFT }
        
        assertTrue(publishedFragments.isNotEmpty())
        assertTrue(draftFragments.isNotEmpty())
        assertTrue(publishedFragments.all { it.visible })
        assertTrue(draftFragments.all { !it.visible })
    }
    
    @Test
    fun createBlogWithScheduledPostsScenario() {
        val scenario = TestScenario.blogWithScheduledPosts(5)
            .build()
        
        val scheduledFragments = scenario.fragments.filter { it.status == FragmentStatus.SCHEDULED }

        assertTrue(scheduledFragments.isNotEmpty())
        scheduledFragments.forEach { fragment ->
            assertNotNull(fragment.publishDate)
            assertTrue(fragment.publishDate!!.isAfter(java.time.LocalDateTime.now()))
        }
    }
    
    @Test
    fun createBlogWithExpiringPostsScenario() {
        val scenario = TestScenario.blogWithExpiringPosts(5)
            .build()
        
        val expiringFragments = scenario.fragments.filter { it.expiryDate != null }
        
        assertTrue(expiringFragments.isNotEmpty())
        expiringFragments.forEach { fragment ->
            assertTrue(fragment.expiryDate!!.isAfter(java.time.LocalDateTime.now()))
            assertEquals(FragmentStatus.PUBLISHED, fragment.status)
        }
    }
    
    @Test
    fun createBlogWithSeriesScenario() {
        val scenario = TestScenario.blogWithSeries()
            .build()
        
        assertEquals("Blog with Series", scenario.name)
        assertTrue(scenario.contentSeries.isNotEmpty())
        assertEquals(2, scenario.contentSeries.size)
        
        scenario.fragments.forEach { fragment ->
            assertNotNull(fragment.seriesSlug)
            assertNotNull(fragment.seriesPart)
            assertNotNull(fragment.seriesTitle)
        }
    }
    
    @Test
    fun createMultiAuthorBlogScenario() {
        val scenario = TestScenario.multiAuthorBlog()
            .build()
        
        assertEquals("Multi-Author Blog", scenario.name)
        assertTrue(scenario.description.contains("multiple authors"))
        assertTrue(scenario.authors.isNotEmpty())
        assertEquals(3, scenario.authors.size)
        
        scenario.fragments.forEach { fragment ->
            assertTrue(fragment.authorIds.isNotEmpty())
            assertTrue(fragment.authorIds.size == 1)
        }
    }
    
    @Test
    fun createComplexScenario() {
        val scenario = TestScenario.complexBlog()
            .build()
        
        assertEquals("Complex Blog", scenario.name)
        assertTrue(scenario.description.contains("multiple features"))
        assertTrue(scenario.fragments.isNotEmpty())
        assertTrue(scenario.authors.isNotEmpty())
        assertTrue(scenario.contentSeries.isNotEmpty())
        
        val draftFragments = scenario.fragments.filter { it.status == FragmentStatus.DRAFT }
        val publishedFragments = scenario.fragments.filter { it.status == FragmentStatus.PUBLISHED }
        
        assertTrue(draftFragments.isNotEmpty())
        assertTrue(publishedFragments.isNotEmpty())
    }
    
    @Test
    fun testScenarioBuilder() {
        val scenario = TestScenarioBuilder()
            .name("Custom Scenario")
            .description("A custom test scenario")
            .fragments(FragmentFactory.createMany(3))
            .authors(AuthorFactory.createMany(2))
            .contentSeries(ContentSeriesFactory.createMany(1))
            .build()
        
        assertEquals("Custom Scenario", scenario.name)
        assertEquals(3, scenario.fragments.size)
        assertEquals(2, scenario.authors.size)
        assertEquals(1, scenario.contentSeries.size)
    }
}
