package io.andromeda.fragments.cache

import io.andromeda.fragments.ContentRelationships
import io.andromeda.fragments.Fragment
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Multi-level cache for Fragments CMS
 * 
 * Provides caching for:
 * - Individual fragments by slug
 * - Fragment lists (visible, by tag, by category, etc.)
 * - Parsed content (markdown + HTML)
 * - Relationships (previous, next, related fragments)
 */
class FragmentCache(
    private val fragmentTtl: Duration = Duration.ofMinutes(5),
    private val listTtl: Duration = Duration.ofMinutes(2),
    private val relationshipTtl: Duration = Duration.ofMinutes(10),
    private val parsedContentTtl: Duration = Duration.ofMinutes(30),
    private val maxSize: Long = 1000L
) {
    
    private val logger = LoggerFactory.getLogger(FragmentCache::class.java)
    
    // Fragment cache by slug
    private val fragmentCache = InMemoryCache<String, Fragment>(
        CacheConfiguration(
            ttl = fragmentTtl,
            maxSize = maxSize,
            recordStats = true
        )
    )
    
    // Fragment list caches
    private val visibleFragmentsCache = InMemoryCache<String, List<Fragment>>(
        CacheConfiguration(ttl = listTtl, maxSize = 100, recordStats = true)
    )
    
    private val fragmentsByTagCache = InMemoryCache<String, List<Fragment>>(
        CacheConfiguration(ttl = listTtl, maxSize = 500, recordStats = true)
    )
    
    private val fragmentsByCategoryCache = InMemoryCache<String, List<Fragment>>(
        CacheConfiguration(ttl = listTtl, maxSize = 500, recordStats = true)
    )
    
    private val fragmentsByAuthorCache = InMemoryCache<String, List<Fragment>>(
        CacheConfiguration(ttl = listTtl, maxSize = 200, recordStats = true)
    )
    
    private val fragmentsBySeriesCache = InMemoryCache<String, List<Fragment>>(
        CacheConfiguration(ttl = listTtl, maxSize = 100, recordStats = true)
    )
    
    // Relationship cache
    private val relationshipCache = InMemoryCache<String, ContentRelationships>(
        CacheConfiguration(ttl = relationshipTtl, maxSize = 500, recordStats = true)
    )
    
    // Parsed content cache (front matter + raw content + HTML)
    private val parsedContentCache = InMemoryCache<String, ParsedContent>(
        CacheConfiguration(ttl = parsedContentTtl, maxSize = maxSize, recordStats = true)
    )
    
    /**
     * Get a fragment by slug from cache
     */
    suspend fun getFragment(slug: String): Fragment? {
        return fragmentCache.get("fragment:$slug")
    }
    
    /**
     * Get a fragment by slug, or compute if not cached
     */
    suspend fun getOrComputeFragment(slug: String, compute: suspend () -> Fragment): Fragment {
        return fragmentCache.getOrCompute("fragment:$slug", compute)
    }
    
    /**
     * Cache a fragment
     */
    suspend fun putFragment(fragment: Fragment) {
        fragmentCache.put("fragment:${fragment.slug}", fragment)
    }
    
    /**
     * Invalidate a specific fragment
     */
    suspend fun invalidateFragment(slug: String) {
        logger.debug("Invalidating fragment cache for: $slug")
        fragmentCache.invalidate("fragment:$slug")
        relationshipCache.invalidate("relationships:$slug")
        parsedContentCache.invalidate("parsed:$slug")
    }
    
    /**
     * Get all visible fragments from cache
     */
    suspend fun getVisibleFragments(): List<Fragment>? {
        return visibleFragmentsCache.get("visible:all")
    }
    
    /**
     * Cache all visible fragments
     */
    suspend fun putVisibleFragments(fragments: List<Fragment>) {
        visibleFragmentsCache.put("visible:all", fragments)
    }
    
    /**
     * Invalidate fragment list caches
     */
    suspend fun invalidateFragmentLists() {
        logger.debug("Invalidating all fragment list caches")
        visibleFragmentsCache.clear()
        fragmentsByTagCache.clear()
        fragmentsByCategoryCache.clear()
        fragmentsByAuthorCache.clear()
        fragmentsBySeriesCache.clear()
    }
    
    /**
     * Get fragments by tag from cache
     */
    suspend fun getFragmentsByTag(tag: String): List<Fragment>? {
        return fragmentsByTagCache.get("tag:$tag")
    }
    
    /**
     * Cache fragments by tag
     */
    suspend fun putFragmentsByTag(tag: String, fragments: List<Fragment>) {
        fragmentsByTagCache.put("tag:$tag", fragments)
    }
    
    /**
     * Get fragments by category from cache
     */
    suspend fun getFragmentsByCategory(category: String): List<Fragment>? {
        return fragmentsByCategoryCache.get("category:$category")
    }
    
    /**
     * Cache fragments by category
     */
    suspend fun putFragmentsByCategory(category: String, fragments: List<Fragment>) {
        fragmentsByCategoryCache.put("category:$category", fragments)
    }
    
    /**
     * Get fragments by author from cache
     */
    suspend fun getFragmentsByAuthor(authorId: String): List<Fragment>? {
        return fragmentsByAuthorCache.get("author:$authorId")
    }
    
    /**
     * Cache fragments by author
     */
    suspend fun putFragmentsByAuthor(authorId: String, fragments: List<Fragment>) {
        fragmentsByAuthorCache.put("author:$authorId", fragments)
    }
    
    /**
     * Get fragments by series from cache
     */
    suspend fun getFragmentsBySeries(seriesSlug: String): List<Fragment>? {
        return fragmentsBySeriesCache.get("series:$seriesSlug")
    }
    
    /**
     * Cache fragments by series
     */
    suspend fun putFragmentsBySeries(seriesSlug: String, fragments: List<Fragment>) {
        fragmentsBySeriesCache.put("series:$seriesSlug", fragments)
    }
    
    /**
     * Get relationships for a fragment from cache
     */
    suspend fun getRelationships(slug: String): ContentRelationships? {
        return relationshipCache.get("relationships:$slug")
    }
    
    /**
     * Cache relationships for a fragment
     */
    suspend fun putRelationships(slug: String, relationships: ContentRelationships) {
        relationshipCache.put("relationships:$slug", relationships)
    }
    
    /**
     * Invalidate relationships for a specific fragment
     */
    suspend fun invalidateRelationships(slug: String) {
        logger.debug("Invalidating relationship cache for: $slug")
        relationshipCache.invalidate("relationships:$slug")
    }
    
    /**
     * Get parsed content from cache
     */
    suspend fun getParsedContent(slug: String): ParsedContent? {
        return parsedContentCache.get("parsed:$slug")
    }
    
    /**
     * Cache parsed content
     */
    suspend fun putParsedContent(slug: String, parsedContent: ParsedContent) {
        parsedContentCache.put("parsed:$slug", parsedContent)
    }
    
    /**
     * Clear all caches
     */
    suspend fun clearAll() {
        logger.debug("Clearing all caches")
        fragmentCache.clear()
        visibleFragmentsCache.clear()
        fragmentsByTagCache.clear()
        fragmentsByCategoryCache.clear()
        fragmentsByAuthorCache.clear()
        fragmentsBySeriesCache.clear()
        relationshipCache.clear()
        parsedContentCache.clear()
    }
    
    /**
     * Get combined cache statistics
     */
    fun getStatistics(): CacheStatisticsReport {
        return CacheStatisticsReport(
            fragmentStats = fragmentCache.getStatistics(),
            listStats = CacheStatistics(
                hitCount = visibleFragmentsCache.getStatistics().hitCount +
                          fragmentsByTagCache.getStatistics().hitCount +
                          fragmentsByCategoryCache.getStatistics().hitCount +
                          fragmentsByAuthorCache.getStatistics().hitCount +
                          fragmentsBySeriesCache.getStatistics().hitCount,
                missCount = visibleFragmentsCache.getStatistics().missCount +
                          fragmentsByTagCache.getStatistics().missCount +
                          fragmentsByCategoryCache.getStatistics().missCount +
                          fragmentsByAuthorCache.getStatistics().missCount +
                          fragmentsBySeriesCache.getStatistics().missCount,
                loadCount = visibleFragmentsCache.getStatistics().loadCount +
                          fragmentsByTagCache.getStatistics().loadCount +
                          fragmentsByCategoryCache.getStatistics().loadCount +
                          fragmentsByAuthorCache.getStatistics().loadCount +
                          fragmentsBySeriesCache.getStatistics().loadCount,
                loadFailureCount = visibleFragmentsCache.getStatistics().loadFailureCount +
                          fragmentsByTagCache.getStatistics().loadFailureCount +
                          fragmentsByCategoryCache.getStatistics().loadFailureCount +
                          fragmentsByAuthorCache.getStatistics().loadFailureCount +
                          fragmentsBySeriesCache.getStatistics().loadFailureCount,
                totalLoadTime = visibleFragmentsCache.getStatistics().totalLoadTime +
                          fragmentsByTagCache.getStatistics().totalLoadTime +
                          fragmentsByCategoryCache.getStatistics().totalLoadTime +
                          fragmentsByAuthorCache.getStatistics().totalLoadTime +
                          fragmentsBySeriesCache.getStatistics().totalLoadTime,
                evictionCount = visibleFragmentsCache.getStatistics().evictionCount +
                          fragmentsByTagCache.getStatistics().evictionCount +
                          fragmentsByCategoryCache.getStatistics().evictionCount +
                          fragmentsByAuthorCache.getStatistics().evictionCount +
                          fragmentsBySeriesCache.getStatistics().evictionCount
            ),
            relationshipStats = relationshipCache.getStatistics(),
            parsedContentStats = parsedContentCache.getStatistics(),
            totalSize = fragmentCache.size() +
                        visibleFragmentsCache.size() +
                        fragmentsByTagCache.size() +
                        fragmentsByCategoryCache.size() +
                        fragmentsByAuthorCache.size() +
                        fragmentsBySeriesCache.size() +
                        relationshipCache.size() +
                        parsedContentCache.size()
        )
    }
    
    /**
     * Reset all cache statistics
     */
    fun resetStatistics() {
        fragmentCache.resetStatistics()
        visibleFragmentsCache.resetStatistics()
        fragmentsByTagCache.resetStatistics()
        fragmentsByCategoryCache.resetStatistics()
        fragmentsByAuthorCache.resetStatistics()
        fragmentsBySeriesCache.resetStatistics()
        relationshipCache.resetStatistics()
        parsedContentCache.resetStatistics()
    }
}

/**
 * Parsed content with front matter and rendered HTML
 */
data class ParsedContent(
    val frontMatter: Map<String, Any>,
    val rawContent: String,
    val htmlContent: String,
    val fileHash: String
)

/**
 * Combined cache statistics report
 */
data class CacheStatisticsReport(
    val fragmentStats: CacheStatistics,
    val listStats: CacheStatistics,
    val relationshipStats: CacheStatistics,
    val parsedContentStats: CacheStatistics,
    val totalSize: Long
) {
    val overallHitRate: Double
        get() {
            val totalRequests = fragmentStats.requestCount + listStats.requestCount + 
                             relationshipStats.requestCount + parsedContentStats.requestCount
            val totalHits = fragmentStats.hitCount + listStats.hitCount + 
                           relationshipStats.hitCount + parsedContentStats.hitCount
            return if (totalRequests > 0) totalHits.toDouble() / totalRequests else 0.0
        }
}
