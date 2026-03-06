package io.andromeda.fragments.lucene

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentStatus
import io.andromeda.fragments.cache.FragmentCache
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Duration
import java.time.LocalDateTime

class CachedLuceneSearchEngineTest {
    
    private lateinit var delegate: LuceneSearchEngine
    private lateinit var fragmentCache: FragmentCache
    private lateinit var cachedEngine: CachedLuceneSearchEngine
    
    @BeforeEach
    fun setUp() {
        delegate = mockk()
        fragmentCache = FragmentCache(
            fragmentTtl = Duration.ofMinutes(5),
            listTtl = Duration.ofMinutes(2),
            relationshipTtl = Duration.ofMinutes(10),
            parsedContentTtl = Duration.ofMinutes(30),
            maxSize = 100L
        )
        cachedEngine = CachedLuceneSearchEngine(delegate, fragmentCache)
    }
    
    @Test
    fun searchCachesResults() = runBlocking {
        val fragment1 = createTestFragment("slug1", "Test Title 1")
        val fragment2 = createTestFragment("slug2", "Test Title 2")
        
        val searchResults = listOf(
            SearchResult(fragment1, 0.9f),
            SearchResult(fragment2, 0.8f)
        )
        
        coEvery { delegate.search("test", 10) } returns searchResults
        
        val result1 = cachedEngine.search("test", 10)
        
        assertNotNull(result1)
        assertEquals(2, result1.size)
        assertEquals("slug1", result1[0].fragment.slug)
        
        val result2 = cachedEngine.search("test", 10)
        
        assertEquals(2, result2.size)
        assertEquals("slug1", result2[0].fragment.slug)
        
        coVerify(exactly = 1) { delegate.search("test", 10) }
    }
    
    @Test
    fun searchWithOptionsCachesResults() = runBlocking {
        val fragment = createTestFragment("slug1", "Test Title")
        
        val searchResults = listOf(SearchResult(fragment, 0.9f))
        
        val options = SearchOptions(
            query = "test",
            maxResults = 10,
            phraseSearch = true,
            fuzzySearch = false
        )
        
        coEvery { delegate.search(options) } returns searchResults
        
        val result1 = cachedEngine.search(options)
        
        assertNotNull(result1)
        assertEquals(1, result1.size)
        assertEquals("slug1", result1[0].fragment.slug)
        
        val result2 = cachedEngine.search(options)
        
        assertEquals(1, result2.size)
        
        coVerify(exactly = 1) { delegate.search(options) }
    }
    
    @Test
    fun searchByTagCachesResults() = runBlocking {
        val fragment = createTestFragment("slug1", tags = listOf("kotlin"))
        
        val results = listOf(fragment)
        
        coEvery { delegate.searchByTag("kotlin") } returns results
        
        val result1 = cachedEngine.searchByTag("kotlin")
        
        assertNotNull(result1)
        assertEquals(1, result1.size)
        assertEquals("slug1", result1[0].slug)
        
        val result2 = cachedEngine.searchByTag("kotlin")
        
        assertEquals(1, result2.size)
        
        coVerify(exactly = 1) { delegate.searchByTag("kotlin") }
    }
    
    @Test
    fun searchByCategoryCachesResults() = runBlocking {
        val fragment = createTestFragment("slug1", categories = listOf("technology"))
        
        val results = listOf(fragment)
        
        coEvery { delegate.searchByCategory("technology") } returns results
        
        val result1 = cachedEngine.searchByCategory("technology")
        
        assertNotNull(result1)
        assertEquals(1, result1.size)
        assertEquals("slug1", result1[0].slug)
        
        val result2 = cachedEngine.searchByCategory("technology")
        
        assertEquals(1, result2.size)
        
        coVerify(exactly = 1) { delegate.searchByCategory("technology") }
    }
    
    @Test
    fun autocompleteCachesResults() = runBlocking {
        val suggestions = listOf(
            SearchSuggestion("Kotlin Programming", 5, SearchSuggestion.SuggestionType.TITLE),
            SearchSuggestion("kotlin", 10, SearchSuggestion.SuggestionType.TAG)
        )
        
        coEvery { delegate.autocomplete("kot", 10) } returns suggestions
        
        val result1 = cachedEngine.autocomplete("kot", 10)
        
        assertNotNull(result1)
        assertEquals(2, result1.size)
        assertEquals("Kotlin Programming", result1[0].text)
        
        val result2 = cachedEngine.autocomplete("kot", 10)
        
        assertEquals(2, result2.size)
        
        coVerify(exactly = 1) { delegate.autocomplete("kot", 10) }
    }
    
    @Test
    fun getSuggestionsCachesResults() = runBlocking {
        val suggestions = listOf(
            SearchSuggestion("Kotlin Programming", 5, SearchSuggestion.SuggestionType.TITLE),
            SearchSuggestion("kotlin", 10, SearchSuggestion.SuggestionType.TAG)
        )
        
        coEvery { delegate.getSuggestions("kot", 10) } returns suggestions
        
        val result1 = cachedEngine.getSuggestions("kot", 10)
        
        assertNotNull(result1)
        assertEquals(2, result1.size)
        assertEquals("Kotlin Programming", result1[0].text)
        
        val result2 = cachedEngine.getSuggestions("kot", 10)
        
        assertEquals(2, result2.size)
        
        coVerify(exactly = 1) { delegate.getSuggestions("kot", 10) }
    }
    
    @Test
    fun searchUsesDifferentCacheKeysForDifferentQueries() = runBlocking {
        val fragment1 = createTestFragment("slug1")
        val fragment2 = createTestFragment("slug2")
        
        val results1 = listOf(SearchResult(fragment1, 0.9f))
        val results2 = listOf(SearchResult(fragment2, 0.8f))
        
        coEvery { delegate.search("query1", 10) } returns results1
        coEvery { delegate.search("query2", 10) } returns results2
        
        cachedEngine.search("query1", 10)
        cachedEngine.search("query2", 10)
        
        val result1 = cachedEngine.search("query1", 10)
        val result2 = cachedEngine.search("query2", 10)
        
        assertEquals("slug1", result1[0].fragment.slug)
        assertEquals("slug2", result2[0].fragment.slug)
    }
    
    @Test
    fun searchWithOptionsUsesDifferentCacheKeys() = runBlocking {
        val fragment = createTestFragment("slug1")
        
        val results = listOf(SearchResult(fragment, 0.9f))
        
        val options1 = SearchOptions(query = "test", phraseSearch = true, fuzzySearch = false)
        val options2 = SearchOptions(query = "test", phraseSearch = false, fuzzySearch = true)
        
        coEvery { delegate.search(options1) } returns results
        coEvery { delegate.search(options2) } returns results
        
        cachedEngine.search(options1)
        cachedEngine.search(options2)
        
        val result1 = cachedEngine.search(options1)
        val result2 = cachedEngine.search(options2)
        
        assertEquals(1, result1.size)
        assertEquals(1, result2.size)
    }
    
    @Test
    fun invalidateSearchCacheClearsAllSearchResults() = runBlocking {
        val fragment = createTestFragment("slug1")
        
        val results = listOf(SearchResult(fragment, 0.9f))
        
        coEvery { delegate.search("test", 10) } returns results
        
        cachedEngine.search("test", 10)
        cachedEngine.invalidateSearchCache()
        
        coEvery { delegate.search("test", 10) } returns results
        
        val result = cachedEngine.search("test", 10)
        
        assertEquals(1, result.size)
        coVerify(atLeast = 2) { delegate.search("test", 10) }
    }
    
    private fun createTestFragment(
        slug: String,
        title: String = "Test Title",
        tags: List<String> = emptyList(),
        categories: List<String> = emptyList()
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
            frontMatter = emptyMap(),
            visible = true,
            template = "default",
            categories = categories,
            tags = tags,
            order = 1,
            language = "en",
            languages = emptyMap(),
            author = "Test Author",
            authorIds = emptyList(),
            seriesSlug = null,
            seriesPart = null,
            seriesTitle = null,
            statusChangeHistory = emptyList()
        )
    }
}
