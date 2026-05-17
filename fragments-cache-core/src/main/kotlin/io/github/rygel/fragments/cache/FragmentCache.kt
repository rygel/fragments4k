package io.github.rygel.fragments.cache

import io.github.rygel.fragments.ContentRelationships
import io.github.rygel.fragments.Fragment
import org.slf4j.LoggerFactory
import java.time.Duration

class FragmentCache(
    private val fragmentTtl: Duration = Duration.ofMinutes(5),
    private val listTtl: Duration = Duration.ofMinutes(2),
    private val relationshipTtl: Duration = Duration.ofMinutes(10),
    private val parsedContentTtl: Duration = Duration.ofMinutes(30),
    private val maxSize: Long = 1000L,
) {
    private val logger = LoggerFactory.getLogger(FragmentCache::class.java)

    private val fragmentCache: Cache<String, Fragment> =
        InMemoryCache(CacheConfiguration(ttl = fragmentTtl, maxSize = maxSize, recordStats = true))

    private val allFragmentsCache: Cache<String, List<Fragment>> =
        InMemoryCache(CacheConfiguration(ttl = listTtl, maxSize = 100, recordStats = true))

    private val visibleFragmentsCache: Cache<String, List<Fragment>> =
        InMemoryCache(CacheConfiguration(ttl = listTtl, maxSize = 100, recordStats = true))

    private val fragmentsByTagCache: Cache<String, List<Fragment>> =
        InMemoryCache(CacheConfiguration(ttl = listTtl, maxSize = 500, recordStats = true))

    private val fragmentsByCategoryCache: Cache<String, List<Fragment>> =
        InMemoryCache(CacheConfiguration(ttl = listTtl, maxSize = 500, recordStats = true))

    private val fragmentsByAuthorCache: Cache<String, List<Fragment>> =
        InMemoryCache(CacheConfiguration(ttl = listTtl, maxSize = 200, recordStats = true))

    private val relationshipCache: Cache<String, ContentRelationships> =
        InMemoryCache(CacheConfiguration(ttl = relationshipTtl, maxSize = 500, recordStats = true))

    private val parsedContentCache: Cache<String, ParsedContent> =
        InMemoryCache(CacheConfiguration(ttl = parsedContentTtl, maxSize = maxSize, recordStats = true))

    private val searchResultCache: Cache<String, List<Fragment>> =
        InMemoryCache(CacheConfiguration(ttl = Duration.ofMinutes(5), maxSize = 500, recordStats = true))

    suspend fun getOrComputeFragment(slug: String, compute: suspend () -> Fragment): Fragment =
        fragmentCache.getOrCompute("fragment:$slug", compute)

    suspend fun getFragment(slug: String): Fragment? = fragmentCache.get("fragment:$slug")

    suspend fun invalidateFragment(slug: String) {
        logger.debug("Invalidating fragment cache for: $slug")
        fragmentCache.invalidate("fragment:$slug")
        relationshipCache.invalidate("relationships:$slug")
        parsedContentCache.invalidate("parsed:$slug")
    }

    suspend fun getOrComputeAllFragments(compute: suspend () -> List<Fragment>): List<Fragment> =
        allFragmentsCache.getOrCompute("all:fragments", compute)

    suspend fun getOrComputeVisibleFragments(compute: suspend () -> List<Fragment>): List<Fragment> =
        visibleFragmentsCache.getOrCompute("visible:all", compute)

    suspend fun getVisibleFragments(): List<Fragment>? = visibleFragmentsCache.get("visible:all")

    suspend fun invalidateFragmentLists() {
        logger.debug("Invalidating all fragment list caches")
        allFragmentsCache.clear()
        visibleFragmentsCache.clear()
        fragmentsByTagCache.clear()
        fragmentsByCategoryCache.clear()
        fragmentsByAuthorCache.clear()
    }

    suspend fun getOrComputeByTag(tag: String, compute: suspend () -> List<Fragment>): List<Fragment> =
        fragmentsByTagCache.getOrCompute("tag:$tag", compute)

    suspend fun getOrComputeByCategory(category: String, compute: suspend () -> List<Fragment>): List<Fragment> =
        fragmentsByCategoryCache.getOrCompute("category:$category", compute)

    suspend fun getOrComputeByAuthor(authorId: String, compute: suspend () -> List<Fragment>): List<Fragment> =
        fragmentsByAuthorCache.getOrCompute("author:$authorId", compute)

    suspend fun getOrComputeRelationships(slug: String, compute: suspend () -> ContentRelationships): ContentRelationships =
        relationshipCache.getOrCompute("relationships:$slug", compute)

    suspend fun getRelationships(slug: String): ContentRelationships? = relationshipCache.get("relationships:$slug")

    suspend fun clearAll() {
        logger.debug("Clearing all caches")
        fragmentCache.clear()
        allFragmentsCache.clear()
        visibleFragmentsCache.clear()
        fragmentsByTagCache.clear()
        fragmentsByCategoryCache.clear()
        fragmentsByAuthorCache.clear()
        relationshipCache.clear()
        parsedContentCache.clear()
        searchResultCache.clear()
    }

    fun getStatistics(): CacheStatisticsReport {
        val listStats = listOf(
            allFragmentsCache.getStatistics(),
            visibleFragmentsCache.getStatistics(),
            fragmentsByTagCache.getStatistics(),
            fragmentsByCategoryCache.getStatistics(),
            fragmentsByAuthorCache.getStatistics(),
        )
        return CacheStatisticsReport(
            fragmentStats = fragmentCache.getStatistics(),
            listStats = CacheStatistics(
                hitCount = listStats.sumOf { it.hitCount },
                missCount = listStats.sumOf { it.missCount },
                loadCount = listStats.sumOf { it.loadCount },
                loadFailureCount = listStats.sumOf { it.loadFailureCount },
                totalLoadTime = listStats.sumOf { it.totalLoadTime },
                evictionCount = listStats.sumOf { it.evictionCount },
            ),
            relationshipStats = relationshipCache.getStatistics(),
            parsedContentStats = parsedContentCache.getStatistics(),
            searchStats = searchResultCache.getStatistics(),
            totalSize = listOf(
                fragmentCache.size(), allFragmentsCache.size(), visibleFragmentsCache.size(),
                fragmentsByTagCache.size(), fragmentsByCategoryCache.size(), fragmentsByAuthorCache.size(),
                relationshipCache.size(), parsedContentCache.size(), searchResultCache.size(),
            ).sum(),
        )
    }

    fun resetStatistics() {
        fragmentCache.resetStatistics()
        allFragmentsCache.resetStatistics()
        visibleFragmentsCache.resetStatistics()
        fragmentsByTagCache.resetStatistics()
        fragmentsByCategoryCache.resetStatistics()
        fragmentsByAuthorCache.resetStatistics()
        relationshipCache.resetStatistics()
        parsedContentCache.resetStatistics()
        searchResultCache.resetStatistics()
    }
}
