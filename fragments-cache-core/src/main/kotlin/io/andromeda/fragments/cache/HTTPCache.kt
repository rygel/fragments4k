package io.github.rygel.fragments.cache

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * HTTP cache headers for caching responses
 */
data class HTTPCacheHeaders(
    val eTag: String,
    val lastModified: Instant,
    val maxAge: Duration,
    val mustRevalidate: Boolean = false,
    val noCache: Boolean = false,
    val noStore: Boolean = false,
    val noTransform: Boolean = false,
    val public: Boolean = true,
    val private: Boolean = false,
    val sMaxAge: Duration? = null,
    val staleWhileRevalidate: Duration? = null,
    val staleIfError: Int? = null,
    val staleIfErrorSeconds: Int? = null,
    val proxyRevalidate: Int? = null
) {
    companion object {
        /**
         * Create cache headers for public resources
         */
        fun public(
            eTag: String,
            lastModified: Instant,
            maxAge: Duration
        ): HTTPCacheHeaders {
            return HTTPCacheHeaders(
                eTag = eTag,
                lastModified = lastModified,
                maxAge = maxAge,
                public = true
            )
        }
        
        /**
         * Create cache headers for private resources
         */
        fun private(
            eTag: String,
            lastModified: Instant,
            maxAge: Duration
        ): HTTPCacheHeaders {
            return HTTPCacheHeaders(
                eTag = eTag,
                lastModified = lastModified,
                maxAge = maxAge,
                public = false,
                private = true
            )
        }
        
        /**
         * Create no-cache headers
         */
        fun noCache(): HTTPCacheHeaders {
            return HTTPCacheHeaders(
                eTag = "",
                lastModified = Instant.now(),
                maxAge = Duration.ZERO,
                noCache = true,
                noStore = true
            )
        }
    }
    
    /**
     * Generate Cache-Control header value
     */
    fun toCacheControlHeader(): String {
        val directives = mutableListOf<String>()
        
        if (public) {
            directives.add("public")
        }
        
        if (private) {
            directives.add("private")
        }
        
        if (noCache) {
            directives.add("no-cache")
        }
        
        if (noStore) {
            directives.add("no-store")
        }
        
        if (noTransform) {
            directives.add("no-transform")
        }
        
        if (mustRevalidate) {
            directives.add("must-revalidate")
        }
        
        directives.add("max-age=${maxAge.seconds}")
        
        sMaxAge?.let {
            directives.add("s-maxage=${it.seconds}")
        }
        
        staleWhileRevalidate?.let {
            directives.add("stale-while-revalidate=${it.seconds}")
        }
        
        staleIfError?.let {
            directives.add("stale-if-error=$it")
        }
        
        staleIfErrorSeconds?.let {
            directives.add("stale-if-error=$it")
        }
        
        proxyRevalidate?.let {
            directives.add("proxy-revalidate=$it")
        }
        
        return directives.joinToString(", ")
    }
}

/**
 * ETag generator for HTTP caching
 */
class ETagGenerator {
    companion object {
        /**
         * Generate ETag for content based on its hash
         */
        fun generateForContent(content: ByteArray): String {
            return "\"${generateHash(content)}\""
        }
        
        /**
         * Generate ETag for content based on its hash and last modified time
         */
        fun generateForContent(content: ByteArray, lastModified: Instant): String {
            val hash = generateHash(content)
            val timestamp = lastModified.toEpochMilli()
            return "\"$hash-$timestamp\""
        }
        
        /**
         * Generate ETag for a string content
         */
        fun generateForString(content: String): String {
            return generateForContent(content.toByteArray())
        }
        
        /**
         * Generate weak ETag
         */
        fun generateWeak(content: ByteArray): String {
            return "W/\"${generateForContent(content).removeSurrounding("\"")}\""
        }
        
        /**
         * Generate hash for content
         */
        private fun generateHash(content: ByteArray): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(content)
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}

/**
 * Cache control policy builder
 */
class CacheControlPolicy {
    private var maxAge: Duration = Duration.ofMinutes(5)
    private var mustRevalidate: Boolean = false
    private var noCache: Boolean = false
    private var noStore: Boolean = false
    private var noTransform: Boolean = false
    private var publicCache: Boolean = true
    private var privateCache: Boolean = false
    private var sMaxAge: Duration? = null
    private var staleWhileRevalidate: Duration? = null
    private var staleIfError: Int? = null
    private var proxyRevalidate: Int? = null
    
    companion object {
        /**
         * Create a new policy builder
         */
        fun builder() = CacheControlPolicy()
    }
    
    /**
     * Set max age
     */
    fun maxAge(duration: Duration): CacheControlPolicy {
        this.maxAge = duration
        return this
    }
    
    /**
     * Set max age in seconds
     */
    fun maxAgeSeconds(seconds: Long): CacheControlPolicy {
        this.maxAge = Duration.ofSeconds(seconds)
        return this
    }
    
    /**
     * Require revalidation
     */
    fun mustRevalidate(): CacheControlPolicy {
        this.mustRevalidate = true
        return this
    }
    
    /**
     * Disable caching
     */
    fun noCache(): CacheControlPolicy {
        this.noCache = true
        this.noStore = true
        return this
    }
    
    /**
     * Public cache
     */
    fun public(): CacheControlPolicy {
        this.publicCache = true
        this.privateCache = false
        return this
    }
    
    /**
     * Private cache
     */
    fun private(): CacheControlPolicy {
        this.publicCache = false
        this.privateCache = true
        return this
    }
    
    /**
     * No transform
     */
    fun noTransform(): CacheControlPolicy {
        this.noTransform = true
        return this
    }
    
    /**
     * Set s-maxage
     */
    fun sMaxAge(duration: Duration): CacheControlPolicy {
        this.sMaxAge = duration
        return this
    }
    
    /**
     * Set stale-while-revalidate
     */
    fun staleWhileRevalidate(duration: Duration): CacheControlPolicy {
        this.staleWhileRevalidate = duration
        return this
    }
    
    /**
     * Set stale-if-error
     */
    fun staleIfError(seconds: Int): CacheControlPolicy {
        this.staleIfError = seconds
        return this
    }
    
    /**
     * Set proxy-revalidate
     */
    fun proxyRevalidate(seconds: Int): CacheControlPolicy {
        this.proxyRevalidate = seconds
        return this
    }
    
    /**
     * Build HTTP cache headers
     */
    fun build(eTag: String, lastModified: Instant = Instant.now()): HTTPCacheHeaders {
        return HTTPCacheHeaders(
            eTag = eTag,
            lastModified = lastModified,
            maxAge = maxAge,
            mustRevalidate = mustRevalidate,
            noCache = noCache,
            noStore = noStore,
            noTransform = noTransform,
            public = publicCache,
            private = privateCache,
            sMaxAge = sMaxAge,
            staleWhileRevalidate = staleWhileRevalidate,
            staleIfError = staleIfError,
            proxyRevalidate = proxyRevalidate
        )
    }
}

/**
 * HTTP response cache for storing cached responses
 */
class HTTPResponseCache(
    private val maxEntries: Int = 1000,
    private val defaultTtl: Duration = Duration.ofMinutes(5)
) {
    private val cache = ConcurrentHashMap<String, CachedResponse>()
    
    /**
     * Cached response with metadata
     */
    data class CachedResponse(
        val body: String,
        val headers: HTTPCacheHeaders,
        val cachedAt: Instant = Instant.now()
    )
    
    /**
     * Get cached response for a key
     */
    fun get(key: String): CachedResponse? {
        val cached = cache[key]
        
        if (cached == null) {
            return null
        }
        
        val age = Duration.between(cached.cachedAt, Instant.now())
        val maxAge = cached.headers.maxAge
        
        return if (age > maxAge) {
            null
        } else {
            cached
        }
    }
    
    /**
     * Put response into cache
     */
    fun put(key: String, response: CachedResponse) {
        cache[key] = response
    }
    
    /**
     * Invalidate cached response
     */
    fun invalidate(key: String) {
        cache.remove(key)
    }
    
    /**
     * Clear all cached responses
     */
    fun clear() {
        cache.clear()
    }
    
    /**
     * Get cache size
     */
    fun size(): Int {
        return cache.size
    }
    
    /**
     * Get all keys
     */
    fun getKeys(): Set<String> {
        return cache.keys
    }
}
