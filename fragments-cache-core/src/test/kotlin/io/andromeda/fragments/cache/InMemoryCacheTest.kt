package io.andromeda.fragments.cache

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class InMemoryCacheTest {
    
    private lateinit var cache: InMemoryCache<String, String>
    
    @BeforeEach
    fun setUp() {
        cache = InMemoryCache(
            CacheConfiguration(
                ttl = Duration.ofMinutes(1),
                maxSize = 10L,
                recordStats = true
            )
        )
    }
    
    @Test
    fun putAndGet() = runBlocking {
        cache.put("key1", "value1")
        
        val result = cache.get("key1")
        assertEquals("value1", result)
    }
    
    @Test
    fun getReturnsNullForNonExistentKey() = runBlocking {
        val result = cache.get("nonexistent")
        assertNull(result)
    }
    
    @Test
    fun putAndGetMultiple() = runBlocking {
        val entries = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3"
        )
        
        cache.putAll(entries)
        
        assertEquals("value1", cache.get("key1"))
        assertEquals("value2", cache.get("key2"))
        assertEquals("value3", cache.get("key3"))
    }
    
    @Test
    fun invalidateRemovesKey() = runBlocking {
        cache.put("key1", "value1")
        assertEquals("value1", cache.get("key1"))
        
        cache.invalidate("key1")
        assertNull(cache.get("key1"))
    }
    
    @Test
    fun invalidateAllRemovesMultipleKeys() = runBlocking {
        cache.putAll(mapOf("key1" to "value1", "key2" to "value2", "key3" to "value3"))
        
        cache.invalidateAll(listOf("key1", "key2"))
        
        assertNull(cache.get("key1"))
        assertNull(cache.get("key2"))
        assertEquals("value3", cache.get("key3"))
    }
    
    @Test
    fun clearRemovesAllKeys() = runBlocking {
        cache.putAll(mapOf("key1" to "value1", "key2" to "value2", "key3" to "value3"))
        assertEquals(3L, cache.size())
        
        cache.clear()
        
        assertEquals(0L, cache.size())
        assertNull(cache.get("key1"))
        assertNull(cache.get("key2"))
        assertNull(cache.get("key3"))
    }
    
    @Test
    fun containsKeyReturnsTrueForExistingKey() = runBlocking {
        cache.put("key1", "value1")
        
        assertTrue(cache.containsKey("key1"))
        assertFalse(cache.containsKey("nonexistent"))
    }
    
    @Test
    fun getKeysReturnsAllKeys() = runBlocking {
        cache.putAll(mapOf("key1" to "value1", "key2" to "value2", "key3" to "value3"))
        
        val keys = cache.getKeys()
        assertEquals(3, keys.size)
        assertTrue(keys.contains("key1"))
        assertTrue(keys.contains("key2"))
        assertTrue(keys.contains("key3"))
    }
    
    @Test
    fun sizeReturnsCorrectCount() = runBlocking {
        assertEquals(0L, cache.size())
        
        cache.put("key1", "value1")
        assertEquals(1L, cache.size())
        
        cache.put("key2", "value2")
        assertEquals(2L, cache.size())
    }
    
    @Test
    fun getOrComputeReturnsCachedValue() = runBlocking {
        cache.put("key1", "cached")
        
        var computeCalls = 0
        val result = cache.getOrCompute("key1") {
            computeCalls++
            "computed"
        }
        
        assertEquals("cached", result)
        assertEquals(0, computeCalls)
    }
    
    @Test
    fun getOrComputeComputesAndCaches() = runBlocking {
        var computeCalls = 0
        val result1 = cache.getOrCompute("key1") {
            computeCalls++
            "computed"
        }
        
        assertEquals("computed", result1)
        assertEquals(1, computeCalls)
        
        val result2 = cache.getOrCompute("key1") {
            computeCalls++
            "computed2"
        }
        
        assertEquals("computed", result2)
        assertEquals(1, computeCalls)
    }
    
    @Test
    fun getStatisticsTracksHitsAndMisses() = runBlocking {
        cache.put("key1", "value1")
        
        cache.get("key1") // hit
        cache.get("key1") // hit
        cache.get("nonexistent") // miss
        
        val stats = cache.getStatistics()
        assertEquals(2L, stats.hitCount)
        assertEquals(1L, stats.missCount)
        assertEquals(3L, stats.requestCount)
        assertEquals(2.0 / 3.0, stats.hitRate, 0.001)
    }
    
    @Test
    fun getStatisticsTracksLoads() = runBlocking {
        var computeCalls = 0
        cache.getOrCompute("key1") {
            computeCalls++
            "computed"
        }
        
        val stats = cache.getStatistics()
        assertEquals(1L, stats.loadCount)
        assertTrue(stats.averageLoadPenalty >= 0.0)
    }
    
    @Test
    fun getStatisticsTracksEvictions() = runBlocking {
        val smallCache = InMemoryCache<String, String>(
            CacheConfiguration(maxSize = 2L, recordStats = true)
        )
        
        smallCache.put("key1", "value1")
        smallCache.put("key2", "value2")
        smallCache.put("key3", "value3") // should evict one
        
        val stats = smallCache.getStatistics()
        assertTrue(stats.evictionCount > 0)
    }
    
    @Test
    fun resetStatisticsClearsAllStats() = runBlocking {
        cache.put("key1", "value1")
        cache.get("key1")
        cache.get("nonexistent")
        
        var computeCalls = 0
        cache.getOrCompute("key2") {
            computeCalls++
            "computed"
        }
        
        cache.resetStatistics()
        
        val stats = cache.getStatistics()
        assertEquals(0L, stats.hitCount)
        assertEquals(0L, stats.missCount)
        assertEquals(0L, stats.loadCount)
        assertEquals(0L, stats.evictionCount)
    }
}

class CacheEntryTest {
    
    @Test
    fun isExpiredReturnsFalseForFreshEntry() {
        val entry = CacheEntry("value", createdAt = Instant.now())
        assertFalse(entry.isExpired())
    }
    
    @Test
    fun isExpiredReturnsTrueForExpiredEntry() {
        val entry = CacheEntry(
            value = "value",
            createdAt = Instant.now().minusSeconds(3600),
            expiresAt = Instant.now().minusSeconds(60)
        )
        assertTrue(entry.isExpired())
    }
    
    @Test
    fun isExpiredReturnsFalseForFutureExpiration() {
        val entry = CacheEntry(
            value = "value",
            expiresAt = Instant.now().plusSeconds(60)
        )
        assertFalse(entry.isExpired())
    }
    
    @Test
    fun isExpiredReturnsFalseWhenNoExpiration() {
        val entry = CacheEntry("value")
        assertFalse(entry.isExpired())
    }
    
    @Test
    fun getAgeReturnsCorrectDuration() {
        val createdAt = Instant.now().minusSeconds(30)
        val entry = CacheEntry("value", createdAt = createdAt)
        
        val age = entry.getAge()
        assertTrue(age.seconds >= 30)
        assertTrue(age.seconds < 31)
    }
    
    @Test
    fun getTimeToExpiryReturnsNullWhenNoExpiration() {
        val entry = CacheEntry("value")
        assertNull(entry.getTimeToExpiry())
    }
    
    @Test
    fun getTimeToExpiryReturnsNullWhenExpired() {
        val entry = CacheEntry(
            value = "value",
            createdAt = Instant.now().minusSeconds(60),
            expiresAt = Instant.now().minusSeconds(30)
        )
        assertNull(entry.getTimeToExpiry())
    }
    
    @Test
    fun getTimeToExpiryReturnsRemainingTime() {
        val entry = CacheEntry(
            value = "value",
            expiresAt = Instant.now().plusSeconds(60)
        )
        
        val timeToExpiry = entry.getTimeToExpiry()
        assertNotNull(timeToExpiry)
        assertTrue(timeToExpiry.seconds >= 59)
        assertTrue(timeToExpiry.seconds <= 60)
    }
}

class CacheStatisticsTest {
    
    @Test
    fun hitRateCalculatesCorrectly() {
        val stats = CacheStatistics(hitCount = 70, missCount = 30)
        assertEquals(0.7, stats.hitRate, 0.001)
    }
    
    @Test
    fun missRateCalculatesCorrectly() {
        val stats = CacheStatistics(hitCount = 70, missCount = 30)
        assertEquals(0.3, stats.missRate, 0.001)
    }
    
    @Test
    fun requestCountCalculatesCorrectly() {
        val stats = CacheStatistics(hitCount = 70, missCount = 30)
        assertEquals(100L, stats.requestCount)
    }
    
    @Test
    fun averageLoadPenaltyCalculatesCorrectly() {
        val stats = CacheStatistics(
            loadCount = 3,
            totalLoadTime = 1500000000 // 1.5 seconds in nanoseconds
        )
        assertEquals(500000000.0, stats.averageLoadPenalty, 0.001)
    }
    
    @Test
    fun hitIncrementsHitCount() {
        val stats = CacheStatistics(hitCount = 5)
        val newStats = stats.hit()
        assertEquals(6L, newStats.hitCount)
    }
    
    @Test
    fun missIncrementsMissCount() {
        val stats = CacheStatistics(missCount = 3)
        val newStats = stats.miss()
        assertEquals(4L, newStats.missCount)
    }
    
    @Test
    fun loadIncrementsLoadCount() {
        val stats = CacheStatistics(loadCount = 2, totalLoadTime = 1000000)
        val newStats = stats.load(500000)
        assertEquals(3L, newStats.loadCount)
        assertEquals(1500000L, newStats.totalLoadTime)
    }
    
    @Test
    fun resetClearsAllCounts() {
        val stats = CacheStatistics(
            hitCount = 10,
            missCount = 5,
            loadCount = 3,
            loadFailureCount = 1,
            totalLoadTime = 1000000,
            evictionCount = 2
        )
        
        val resetStats = stats.reset()
        
        assertEquals(0L, resetStats.hitCount)
        assertEquals(0L, resetStats.missCount)
        assertEquals(0L, resetStats.loadCount)
        assertEquals(0L, resetStats.loadFailureCount)
        assertEquals(0L, resetStats.totalLoadTime)
        assertEquals(0L, resetStats.evictionCount)
    }
}
