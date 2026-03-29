package io.github.rygel.fragments.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Wrapper for cached values with expiration tracking
 */
data class CacheEntry<T>(
    val value: T,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant? = null
) {
    /**
     * Check if the cache entry has expired
     */
    fun isExpired(): Boolean {
        return expiresAt != null && Instant.now().isAfter(expiresAt)
    }
    
    /**
     * Get the age of the cache entry
     */
    fun getAge(): Duration {
        return Duration.between(createdAt, Instant.now())
    }
    
    /**
     * Get remaining time until expiration
     */
    fun getTimeToExpiry(): Duration? {
        return if (expiresAt != null) {
            Duration.between(Instant.now(), expiresAt).takeIf { it.isPositive() }
        } else {
            null
        }
    }
}

/**
 * Cache configuration options
 */
data class CacheConfiguration(
    val ttl: Duration? = null,
    val maxSize: Long? = null,
    val enableStatistics: Boolean = true,
    val recordStats: Boolean = true
) {
    companion object {
        val DEFAULT = CacheConfiguration()
        
        val SHORT_LIVED = CacheConfiguration(ttl = Duration.ofMinutes(1))
        
        val MEDIUM_LIVED = CacheConfiguration(ttl = Duration.ofMinutes(5))
        
        val LONG_LIVED = CacheConfiguration(ttl = Duration.ofMinutes(30))
    }
}

/**
 * Cache statistics tracking
 */
data class CacheStatistics(
    val hitCount: Long = 0,
    val missCount: Long = 0,
    val loadCount: Long = 0,
    val loadFailureCount: Long = 0,
    val totalLoadTime: Long = 0, // in nanoseconds
    val evictionCount: Long = 0
) {
    val requestCount: Long
        get() = hitCount + missCount
    
    val hitRate: Double
        get() = if (requestCount > 0) hitCount.toDouble() / requestCount else 0.0
    
    val missRate: Double
        get() = if (requestCount > 0) missCount.toDouble() / requestCount else 0.0
    
    val averageLoadPenalty: Double
        get() = if (loadCount > 0) totalLoadTime.toDouble() / loadCount else 0.0
    
    fun hit(): CacheStatistics = copy(hitCount = hitCount + 1)
    
    fun miss(): CacheStatistics = copy(missCount = missCount + 1)
    
    fun load(loadTimeNanos: Long): CacheStatistics = 
        copy(loadCount = loadCount + 1, totalLoadTime = totalLoadTime + loadTimeNanos)
    
    fun loadFailure(): CacheStatistics = copy(loadFailureCount = loadFailureCount + 1)
    
    fun eviction(): CacheStatistics = copy(evictionCount = evictionCount + 1)
    
    fun reset(): CacheStatistics = CacheStatistics()
}

/**
 * Generic cache interface
 */
interface Cache<K, V> {
    /**
     * Get a value from the cache, returning null if not present or expired
     */
    suspend fun get(key: K): V?
    
    /**
     * Get a value from the cache, or compute it if not present
     */
    suspend fun getOrCompute(key: K, compute: suspend () -> V): V
    
    /**
     * Put a value into the cache
     */
    suspend fun put(key: K, value: V)
    
    /**
     * Put multiple values into the cache
     */
    suspend fun putAll(entries: Map<K, V>)
    
    /**
     * Remove a value from the cache
     */
    suspend fun invalidate(key: K)
    
    /**
     * Remove multiple values from the cache
     */
    suspend fun invalidateAll(keys: Collection<K>)
    
    /**
     * Clear all values from the cache
     */
    suspend fun clear()
    
    /**
     * Get the current cache statistics
     */
    fun getStatistics(): CacheStatistics
    
    /**
     * Reset cache statistics
     */
    fun resetStatistics()
    
    /**
     * Get the approximate size of the cache
     */
    fun size(): Long
    
    /**
     * Get all keys currently in the cache
     */
    fun getKeys(): Set<K>
    
    /**
     * Check if a key exists in the cache
     */
    suspend fun containsKey(key: K): Boolean
}

/**
 * Thread-safe in-memory cache implementation
 */
class InMemoryCache<K, V>(
    private val configuration: CacheConfiguration = CacheConfiguration.DEFAULT
) : Cache<K, V> {
    
    private val logger = LoggerFactory.getLogger(InMemoryCache::class.java)
    private val store = ConcurrentHashMap<K, CacheEntry<V>>()
    private val mutex = Mutex()
    private var statistics = CacheStatistics()
    
    override suspend fun get(key: K): V? {
        return mutex.withLock {
            val entry = store[key]
            if (entry != null && !entry.isExpired()) {
                if (configuration.recordStats) {
                    statistics = statistics.hit()
                    logger.debug("Cache hit for key: $key")
                }
                entry.value
            } else {
                if (entry != null && entry.isExpired()) {
                    logger.debug("Cache entry expired for key: $key")
                    store.remove(key)
                }
                if (configuration.recordStats) {
                    statistics = statistics.miss()
                    logger.debug("Cache miss for key: $key")
                }
                null
            }
        }
    }
    
    override suspend fun getOrCompute(key: K, compute: suspend () -> V): V {
        val cached = get(key)
        if (cached != null) {
            return cached
        }
        
        return mutex.withLock {
            val entry = store[key]
            if (entry != null && !entry.isExpired()) {
                if (configuration.recordStats) {
                    statistics = statistics.hit()
                }
                entry.value
            } else {
                if (entry != null && entry.isExpired()) {
                    store.remove(key)
                }
                
                val startTime = System.nanoTime()
                try {
                    val value = compute()
                    val endTime = System.nanoTime()
                    
                    putEntry(key, value)
                    
                    if (configuration.recordStats) {
                        statistics = statistics.load(endTime - startTime)
                        logger.debug("Cache compute and load for key: $key in ${endTime - startTime}ns")
                    }
                    
                    value
                } catch (e: Exception) {
                    if (configuration.recordStats) {
                        statistics = statistics.loadFailure()
                        logger.error("Cache load failure for key: $key", e)
                    }
                    throw e
                }
            }
        }
    }
    
    override suspend fun put(key: K, value: V) {
        mutex.withLock {
            putEntry(key, value)
            logger.debug("Cache put for key: $key")
        }
    }
    
    override suspend fun putAll(entries: Map<K, V>) {
        mutex.withLock {
            entries.forEach { (key, value) ->
                putEntry(key, value)
            }
            logger.debug("Cache putAll for ${entries.size} entries")
        }
    }
    
    override suspend fun invalidate(key: K) {
        mutex.withLock {
            store.remove(key)
            logger.debug("Cache invalidate for key: $key")
        }
    }
    
    override suspend fun invalidateAll(keys: Collection<K>) {
        mutex.withLock {
            keys.forEach { store.remove(it) }
            logger.debug("Cache invalidateAll for ${keys.size} keys")
        }
    }
    
    override suspend fun clear() {
        mutex.withLock {
            store.clear()
            logger.debug("Cache cleared")
        }
    }
    
    override fun getStatistics(): CacheStatistics = statistics
    
    override fun resetStatistics() {
        statistics = statistics.reset()
        logger.debug("Cache statistics reset")
    }
    
    override fun size(): Long = store.size.toLong()
    
    override fun getKeys(): Set<K> = store.keys
    
    override suspend fun containsKey(key: K): Boolean {
        return mutex.withLock {
            val entry = store[key]
            entry != null && !entry.isExpired()
        }
    }
    
    private fun putEntry(key: K, value: V) {
        val expiresAt = configuration.ttl?.let { ttl ->
            Instant.now().plus(ttl)
        }
        
        store[key] = CacheEntry(value = value, expiresAt = expiresAt)
        
        enforceMaxSize()
    }
    
    private fun enforceMaxSize() {
        val maxSize = configuration.maxSize ?: return
        
        while (store.size > maxSize) {
            val oldestEntry = store.values
                .minByOrNull { it.createdAt }
            
            if (oldestEntry != null) {
                val keyToRemove = store.entries.find { it.value === oldestEntry }?.key
                if (keyToRemove != null) {
                    store.remove(keyToRemove)
                    if (configuration.recordStats) {
                        statistics = statistics.eviction()
                    }
                    logger.debug("Cache eviction for key: $keyToRemove (max size reached)")
                }
            } else {
                break
            }
        }
    }
}
