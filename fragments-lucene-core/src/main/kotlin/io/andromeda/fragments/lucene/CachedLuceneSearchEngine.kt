package io.github.rygel.fragments.lucene

import io.github.rygel.fragments.cache.FragmentCache
import io.github.rygel.fragments.cache.InMemoryCache
import io.github.rygel.fragments.cache.CacheConfiguration
import io.github.rygel.fragments.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.Duration

/**
 * Cached search engine decorator that adds caching to LuceneSearchEngine operations
 */
class CachedLuceneSearchEngine(
    private val delegate: LuceneSearchEngine,
    private val fragmentCache: FragmentCache
) : SearchEngine {
    
    private val logger = LoggerFactory.getLogger(CachedLuceneSearchEngine::class.java)
    
    private val suggestionCache = InMemoryCache<String, List<SearchSuggestion>>(
        CacheConfiguration(ttl = Duration.ofMinutes(10), maxSize = 300, recordStats = true)
    )
    
    private val searchResultCache = InMemoryCache<String, List<Fragment>>(
        CacheConfiguration(ttl = Duration.ofMinutes(5), maxSize = 500, recordStats = true)
    )
    
    override suspend fun search(queryString: String, maxResults: Int): List<SearchResult> {
        logger.debug("Searching for: $queryString (limit: $maxResults)")
        
        val cacheKey = generateSearchCacheKey(queryString, maxResults, phraseSearch = false, fuzzySearch = false)
        
        val cachedResults = searchResultCache.get(cacheKey)
        if (cachedResults != null) {
            logger.debug("Cache hit for search: $cacheKey")
            return cachedResults.map { fragment -> SearchResult(fragment = fragment, score = 1.0f) }
        }
        
        logger.debug("Cache miss for search: $cacheKey, executing search")
        val results = delegate.search(queryString, maxResults)
        
        searchResultCache.put(cacheKey, results.map { it.fragment })
        
        return results
    }
    
    override suspend fun search(options: SearchOptions): List<SearchResult> {
        logger.debug("Searching with options: ${options.query}")
        
        val cacheKey = generateSearchCacheKey(
            options.query,
            options.maxResults,
            options.phraseSearch,
            options.fuzzySearch
        )
        
        val cachedResults = searchResultCache.get(cacheKey)
        if (cachedResults != null) {
            logger.debug("Cache hit for search with options: $cacheKey")
            return cachedResults.map { fragment -> SearchResult(fragment = fragment, score = 1.0f) }
        }
        
        logger.debug("Cache miss for search with options: $cacheKey, executing search")
        val results = delegate.search(options)
        
        searchResultCache.put(cacheKey, results.map { it.fragment })
        
        return results
    }
    
    override suspend fun searchByTag(tag: String): List<Fragment> {
        logger.debug("Searching by tag: $tag")
        
        val cacheKey = generateTagSearchCacheKey(tag)
        
        val cachedResults = searchResultCache.get(cacheKey)
        if (cachedResults != null) {
            logger.debug("Cache hit for tag search: $cacheKey")
            return cachedResults
        }
        
        logger.debug("Cache miss for tag search: $cacheKey, executing search")
        val results = delegate.searchByTag(tag)
        
        searchResultCache.put(cacheKey, results)
        
        return results
    }
    
    override suspend fun searchByCategory(category: String): List<Fragment> {
        logger.debug("Searching by category: $category")
        
        val cacheKey = generateCategorySearchCacheKey(category)
        
        val cachedResults = searchResultCache.get(cacheKey)
        if (cachedResults != null) {
            logger.debug("Cache hit for category search: $cacheKey")
            return cachedResults
        }
        
        logger.debug("Cache miss for category search: $cacheKey, executing search")
        val results = delegate.searchByCategory(category)
        
        searchResultCache.put(cacheKey, results)
        
        return results
    }
    
    override suspend fun autocomplete(query: String, limit: Int): List<SearchSuggestion> {
        logger.debug("Autocomplete for: $query (limit: $limit)")
        
        val cacheKey = generateSuggestionCacheKey(query, limit)
        
        val cachedSuggestions = suggestionCache.get(cacheKey)
        if (cachedSuggestions != null) {
            logger.debug("Cache hit for autocomplete: $cacheKey")
            return cachedSuggestions
        }
        
        logger.debug("Cache miss for autocomplete: $cacheKey, executing search")
        val suggestions = delegate.autocomplete(query, limit)
        
        suggestionCache.put(cacheKey, suggestions)
        
        return suggestions
    }
    
    override suspend fun getSuggestions(query: String, limit: Int): List<SearchSuggestion> {
        logger.debug("Getting suggestions for: $query (limit: $limit)")
        
        val cacheKey = generateSuggestionCacheKey(query, limit)
        
        val cachedSuggestions = suggestionCache.get(cacheKey)
        if (cachedSuggestions != null) {
            logger.debug("Cache hit for suggestions: $cacheKey")
            return cachedSuggestions
        }
        
        logger.debug("Cache miss for suggestions: $cacheKey, executing search")
        val suggestions = delegate.getSuggestions(query, limit)
        
        suggestionCache.put(cacheKey, suggestions)
        
        return suggestions
    }
    
    /**
     * Invalidate all search-related caches
     */
    override suspend fun invalidateSearchCache() {
        logger.debug("Invalidating search cache")
        searchResultCache.clear()
        suggestionCache.clear()
    }
    
    /**
     * Invalidate search results for a specific fragment (e.g., when fragment is updated)
     */
    override suspend fun invalidateFragmentSearchResults(fragmentSlug: String) {
        logger.debug("Invalidating search results containing fragment: $fragmentSlug")
        
        val cacheKeys = searchResultCache.getKeys().filter { it.startsWith("search:") }
        
        cacheKeys.forEach { key ->
            val results = searchResultCache.get(key)
            if (results != null && results.any { it.slug == fragmentSlug }) {
                fragmentCache.invalidateFragment(fragmentSlug)
            }
        }
    }
    
    /**
     * Invalidate search results containing specific tags
     */
    override suspend fun invalidateTagSearchResults(tag: String) {
        logger.debug("Invalidating tag search results for tag: $tag")
        
        val cacheKey = generateTagSearchCacheKey(tag)
        searchResultCache.invalidate(cacheKey)
    }
    
    /**
     * Invalidate search results containing specific categories
     */
    override suspend fun invalidateCategorySearchResults(category: String) {
        logger.debug("Invalidating category search results for category: $category")
        
        val cacheKey = generateCategorySearchCacheKey(category)
        searchResultCache.invalidate(cacheKey)
    }
    /**
     * Generate cache key for search queries
     */
    private fun generateSearchCacheKey(
        query: String,
        maxResults: Int,
        phraseSearch: Boolean,
        fuzzySearch: Boolean
    ): String {
        val components = listOf(
            "q:$query",
            "limit:$maxResults",
            "phrase:$phraseSearch",
            "fuzzy:$fuzzySearch"
        ).joinToString("|")
        
        return hashString(components)
    }
    
    /**
     * Generate cache key for tag searches
     */
    private fun generateTagSearchCacheKey(tag: String): String {
        return "tag:${hashString(tag)}"
    }
    
    /**
     * Generate cache key for category searches
     */
    private fun generateCategorySearchCacheKey(category: String): String {
        return "category:${hashString(category)}"
    }
    
    /**
     * Generate cache key for suggestions
     */
    private fun generateSuggestionCacheKey(query: String, limit: Int): String {
        return "suggestion:${hashString("$query:$limit")}"
    }
    
    /**
     * Hash a string for use as cache key
     */
    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Search engine interface (common interface for search implementations)
 */
interface SearchEngine {
    suspend fun search(queryString: String, maxResults: Int = 10): List<SearchResult>
    suspend fun search(options: SearchOptions): List<SearchResult>
    suspend fun searchByTag(tag: String): List<Fragment>
    suspend fun searchByCategory(category: String): List<Fragment>
    suspend fun autocomplete(query: String, limit: Int = 10): List<SearchSuggestion>
    suspend fun getSuggestions(query: String, limit: Int = 10): List<SearchSuggestion>
    
    suspend fun invalidateSearchCache()
    suspend fun invalidateFragmentSearchResults(fragmentSlug: String)
    suspend fun invalidateTagSearchResults(tag: String)
    suspend fun invalidateCategorySearchResults(category: String)
}
