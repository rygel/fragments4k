package io.andromeda.fragments.cache

import io.andromeda.fragments.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Duration
import java.time.LocalDateTime

class FragmentCacheTest {
    
    private lateinit var fragmentCache: FragmentCache
    
    @BeforeEach
    fun setUp() {
        fragmentCache = FragmentCache(
            fragmentTtl = Duration.ofMinutes(5),
            listTtl = Duration.ofMinutes(2),
            relationshipTtl = Duration.ofMinutes(10),
            parsedContentTtl = Duration.ofMinutes(30),
            maxSize = 100L
        )
    }
    
    @Test
    fun putAndGetFragment() = runBlocking {
        val fragment = createTestFragment("test-slug")
        
        fragmentCache.putFragment(fragment)
        val retrieved = fragmentCache.getFragment("test-slug")
        
        assertNotNull(retrieved)
        assertEquals("test-slug", retrieved?.slug)
        assertEquals("Test Title", retrieved?.title)
    }
    
    @Test
    fun getOrComputeFragmentComputesOnMiss() = runBlocking {
        var computeCalls = 0
        
        val fragment = fragmentCache.getOrComputeFragment("test-slug") {
            computeCalls++
            createTestFragment("test-slug")
        }
        
        assertEquals("test-slug", fragment.slug)
        assertEquals(1, computeCalls)
        
        val fragment2 = fragmentCache.getOrComputeFragment("test-slug") {
            computeCalls++
            createTestFragment("test-slug")
        }
        
        assertEquals(1, computeCalls)
    }
    
    @Test
    fun putAndGetVisibleFragments() = runBlocking {
        val fragments = listOf(
            createTestFragment("slug1"),
            createTestFragment("slug2"),
            createTestFragment("slug3")
        )
        
        fragmentCache.putVisibleFragments(fragments)
        val retrieved = fragmentCache.getVisibleFragments()
        
        assertNotNull(retrieved)
        assertEquals(3, retrieved.size)
    }
    
    @Test
    fun putAndGetFragmentsByTag() = runBlocking {
        val fragments = listOf(
            createTestFragment("slug1", tags = listOf("kotlin")),
            createTestFragment("slug2", tags = listOf("kotlin"))
        )
        
        fragmentCache.putFragmentsByTag("kotlin", fragments)
        val retrieved = fragmentCache.getFragmentsByTag("kotlin")
        
        assertNotNull(retrieved)
        assertEquals(2, retrieved.size)
    }
    
    @Test
    fun putAndGetFragmentsByCategory() = runBlocking {
        val fragments = listOf(
            createTestFragment("slug1", categories = listOf("technology")),
            createTestFragment("slug2", categories = listOf("technology"))
        )
        
        fragmentCache.putFragmentsByCategory("technology", fragments)
        val retrieved = fragmentCache.getFragmentsByCategory("technology")
        
        assertNotNull(retrieved)
        assertEquals(2, retrieved.size)
    }
    
    @Test
    fun putAndGetFragmentsByAuthor() = runBlocking {
        val fragments = listOf(
            createTestFragment("slug1", authorIds = listOf("author1")),
            createTestFragment("slug2", authorIds = listOf("author1"))
        )
        
        fragmentCache.putFragmentsByAuthor("author1", fragments)
        val retrieved = fragmentCache.getFragmentsByAuthor("author1")
        
        assertNotNull(retrieved)
        assertEquals(2, retrieved.size)
    }
    
    @Test
    fun putAndGetFragmentsBySeries() = runBlocking {
        val fragments = listOf(
            createTestFragment("slug1", seriesSlug = "test-series"),
            createTestFragment("slug2", seriesSlug = "test-series")
        )
        
        fragmentCache.putFragmentsBySeries("test-series", fragments)
        val retrieved = fragmentCache.getFragmentsBySeries("test-series")
        
        assertNotNull(retrieved)
        assertEquals(2, retrieved.size)
    }
    
    @Test
    fun putAndGetRelationships() = runBlocking {
        val relationships = ContentRelationships(
            previous = null,
            next = null,
            relatedByTag = emptyList<Fragment>(),
            relatedByCategory = emptyList<Fragment>(),
            relatedByContent = emptyList<Fragment>(),
            translations = emptyList<Fragment>()
        )
        
        fragmentCache.putRelationships("test-slug", relationships)
        val retrieved = fragmentCache.getRelationships("test-slug")
        
        assertNotNull(retrieved)
        assertEquals("previous", retrieved?.previous?.slug)
        assertEquals("next", retrieved?.next?.slug)
        assertEquals(1, retrieved?.relatedByTag?.size)
    }
    
    @Test
    fun putAndGetParsedContent() = runBlocking {
        val parsedContent = ParsedContent(
            frontMatter = emptyMap<String, Any>(),
            rawContent = "# Test Content",
            htmlContent = "<h1>Test Content</h1>",
            fileHash = "abc123"
        )
        
        fragmentCache.putParsedContent("test-slug", parsedContent)
        val retrieved = fragmentCache.getParsedContent("test-slug")
        
        assertNotNull(retrieved)
        assertEquals("Test", retrieved?.frontMatter?.get("title"))
        assertEquals("# Test Content", retrieved?.rawContent)
        assertEquals("<h1>Test Content</h1>", retrieved?.htmlContent)
    }
    
    @Test
    fun invalidateFragmentRemovesFragment() = runBlocking {
        val fragment = createTestFragment("test-slug")
        fragmentCache.putFragment(fragment)
        
        assertNotNull(fragmentCache.getFragment("test-slug"))
        
        fragmentCache.invalidateFragment("test-slug")
        
        assertNull(fragmentCache.getFragment("test-slug"))
    }
    
    @Test
    fun invalidateFragmentRemovesRelationships() = runBlocking {
        val fragment = createTestFragment("test-slug")
        val relationships = ContentRelationships(
            previous = null,
            next = null,
            relatedByTag = emptyList(),
            relatedByCategory = emptyList(),
            relatedByContent = emptyList(),
            translations = emptyList()
        )
        
        fragmentCache.putFragment(fragment)
        fragmentCache.putRelationships("test-slug", relationships)
        
        assertNotNull(fragmentCache.getRelationships("test-slug"))
        
        fragmentCache.invalidateFragment("test-slug")
        
        assertNull(fragmentCache.getRelationships("test-slug"))
    }
    
    @Test
    fun invalidateFragmentListsClearsListCaches() = runBlocking {
        val fragments = listOf(createTestFragment("slug1"))
        
        fragmentCache.putVisibleFragments(fragments)
        fragmentCache.putFragmentsByTag("kotlin", fragments)
        fragmentCache.putFragmentsByCategory("tech", fragments)
        fragmentCache.putFragmentsByAuthor("author1", fragments)
        fragmentCache.putFragmentsBySeries("series1", fragments)
        
        assertNotNull(fragmentCache.getVisibleFragments())
        assertNotNull(fragmentCache.getFragmentsByTag("kotlin"))
        assertNotNull(fragmentCache.getFragmentsByCategory("tech"))
        assertNotNull(fragmentCache.getFragmentsByAuthor("author1"))
        assertNotNull(fragmentCache.getFragmentsBySeries("series1"))
        
        fragmentCache.invalidateFragmentLists()
        
        assertNull(fragmentCache.getVisibleFragments())
        assertNull(fragmentCache.getFragmentsByTag("kotlin"))
        assertNull(fragmentCache.getFragmentsByCategory("tech"))
        assertNull(fragmentCache.getFragmentsByAuthor("author1"))
        assertNull(fragmentCache.getFragmentsBySeries("series1"))
    }
    
    @Test
    fun invalidateRelationshipsRemovesOnlyRelationships() = runBlocking {
        val fragment = createTestFragment("test-slug")
        val relationships = ContentRelationships(
            previous = null,
            next = null,
            relatedByTag = emptyList(),
            relatedByCategory = emptyList(),
            relatedByContent = emptyList(),
            translations = emptyList()
        )
        
        fragmentCache.putFragment(fragment)
        fragmentCache.putRelationships("test-slug", relationships)
        
        assertNotNull(fragmentCache.getFragment("test-slug"))
        assertNotNull(fragmentCache.getRelationships("test-slug"))
        
        fragmentCache.invalidateRelationships("test-slug")
        
        assertNotNull(fragmentCache.getFragment("test-slug"))
        assertNull(fragmentCache.getRelationships("test-slug"))
    }
    
    @Test
    fun clearAllRemovesAllCaches() = runBlocking {
        val fragment = createTestFragment("test-slug")
        val fragments = listOf(fragment)
        val relationships = ContentRelationships(
            previous = null,
            next = null,
            relatedByTag = emptyList(),
            relatedByCategory = emptyList(),
            relatedByContent = emptyList(),
            translations = emptyList()
        )
        val parsedContent = ParsedContent(
            frontMatter = emptyMap(),
            rawContent = "",
            htmlContent = "",
            fileHash = ""
        )
        
        fragmentCache.putFragment(fragment)
        fragmentCache.putVisibleFragments(fragments)
        fragmentCache.putRelationships("test-slug", relationships)
        fragmentCache.putParsedContent("test-slug", parsedContent)
        
        fragmentCache.clearAll()
        
        assertNull(fragmentCache.getFragment("test-slug"))
        assertNull(fragmentCache.getVisibleFragments())
        assertNull(fragmentCache.getRelationships("test-slug"))
        assertNull(fragmentCache.getParsedContent("test-slug"))
    }
    
    @Test
    fun getStatisticsReturnsReport() = runBlocking {
        val fragment = createTestFragment("test-slug")
        
        fragmentCache.putFragment(fragment)
        fragmentCache.getFragment("test-slug")
        fragmentCache.getFragment("nonexistent")
        
        val stats = fragmentCache.getStatistics()
        
        assertNotNull(stats)
        assertNotNull(stats.fragmentStats)
        assertNotNull(stats.listStats)
        assertNotNull(stats.relationshipStats)
        assertNotNull(stats.parsedContentStats)
        assertTrue(stats.totalSize >= 1L)
    }
    
    @Test
    fun resetStatisticsClearsAllStats() = runBlocking {
        val fragment = createTestFragment("test-slug")
        
        fragmentCache.putFragment(fragment)
        fragmentCache.getFragment("test-slug")
        fragmentCache.getFragment("nonexistent")
        
        fragmentCache.resetStatistics()
        
        val stats = fragmentCache.getStatistics()
        
        assertEquals(0L, stats.fragmentStats.hitCount)
        assertEquals(0L, stats.fragmentStats.missCount)
        assertEquals(0L, stats.fragmentStats.loadCount)
    }
    
    @Test
    fun overallHitRateCalculatesCorrectly() = runBlocking {
        val fragment = createTestFragment("test-slug")
        val fragments = listOf(fragment)
        
        fragmentCache.putFragment(fragment)
        fragmentCache.putVisibleFragments(fragments)
        
        fragmentCache.getFragment("test-slug") // hit
        fragmentCache.getFragment("test-slug") // hit
        fragmentCache.getFragment("nonexistent") // miss
        fragmentCache.getVisibleFragments() // hit
        fragmentCache.getVisibleFragments() // hit
        
        val stats = fragmentCache.getStatistics()
        
        assertEquals(0.8, stats.overallHitRate, 0.001)
    }
    
    private fun createTestFragment(
        slug: String,
        title: String = "Test Title",
        tags: List<String> = emptyList(),
        categories: List<String> = emptyList(),
        authorIds: List<String> = emptyList(),
        seriesSlug: String? = null
    ): Fragment {
        return Fragment(
            title = title,
            slug = slug,
            date = LocalDateTime.now(),
            status = FragmentStatus.PUBLISHED,
            publishDate = null,
            expiryDate = null,
            preview = "Preview text",
            content = "<p>Content</p>",
            frontMatter = emptyMap<String, Any>(),
            visible = true,
            template = "default",
            categories = categories,
            tags = tags,
            order = 1,
            language = "en",
            languages = emptyMap(),
            author = "Test Author",
            authorIds = authorIds,
            seriesSlug = seriesSlug,
            seriesPart = null,
            seriesTitle = null,
            statusChangeHistory = emptyList()
        )
    }
}
