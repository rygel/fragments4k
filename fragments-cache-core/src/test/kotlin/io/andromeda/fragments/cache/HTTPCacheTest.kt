package io.andromeda.fragments.cache

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Duration
import java.time.Instant

class HTTPCacheHeadersTest {
    
    @Test
    fun toCacheControlHeaderGeneratesPublic() {
        val headers = HTTPCacheHeaders.public(
            eTag = "\"abc123\"",
            lastModified = Instant.now(),
            maxAge = Duration.ofMinutes(5)
        )
        
        val cacheControl = headers.toCacheControlHeader()
        
        assertTrue(cacheControl.contains("public"))
        assertTrue(cacheControl.contains("max-age=300"))
        assertFalse(cacheControl.contains("private"))
        assertFalse(cacheControl.contains("no-cache"))
    }
    
    @Test
    fun toCacheControlHeaderGeneratesPrivate() {
        val headers = HTTPCacheHeaders.private(
            eTag = "\"abc123\"",
            lastModified = Instant.now(),
            maxAge = Duration.ofMinutes(10)
        )
        
        val cacheControl = headers.toCacheControlHeader()
        
        assertTrue(cacheControl.contains("private"))
        assertTrue(cacheControl.contains("max-age=600"))
        assertFalse(cacheControl.contains("public"))
    }
    
    @Test
    fun toCacheControlHeaderGeneratesNoCache() {
        val headers = HTTPCacheHeaders.noCache()
        
        val cacheControl = headers.toCacheControlHeader()
        
        assertTrue(cacheControl.contains("no-cache"))
        assertTrue(cacheControl.contains("no-store"))
        assertTrue(cacheControl.contains("max-age=0"))
    }
    
    @Test
    fun toCacheControlHeaderIncludesMustRevalidate() {
        val headers = HTTPCacheHeaders(
            eTag = "\"abc123\"",
            lastModified = Instant.now(),
            maxAge = Duration.ofMinutes(5),
            mustRevalidate = true
        )
        
        val cacheControl = headers.toCacheControlHeader()
        
        assertTrue(cacheControl.contains("must-revalidate"))
    }
    
    @Test
    fun toCacheControlHeaderIncludesSMAXAge() {
        val headers = HTTPCacheHeaders(
            eTag = "\"abc123\"",
            lastModified = Instant.now(),
            maxAge = Duration.ofMinutes(5),
            sMaxAge = Duration.ofHours(1)
        )
        
        val cacheControl = headers.toCacheControlHeader()
        
        assertTrue(cacheControl.contains("s-maxage=3600"))
    }
}

class ETagGeneratorTest {
    
    @Test
    fun generateForStringCreatesETag() {
        val content = "Hello, World!"
        
        val eTag = ETagGenerator.generateForString(content)
        
        assertNotNull(eTag)
        assertTrue(eTag.startsWith("\""))
        assertTrue(eTag.endsWith("\""))
        val hash = eTag.substring(1, eTag.length - 1)
        assertEquals(64, hash.length)
    }
    
    @Test
    fun generateForContentCreatesETag() {
        val content = "Test content".toByteArray()
        
        val eTag = ETagGenerator.generateForContent(content)
        
        assertNotNull(eTag)
        assertTrue(eTag.startsWith("\""))
        assertTrue(eTag.endsWith("\""))
        assertEquals(64, eTag.length - 2)
    }
    
    @Test
    fun generateForContentWithLastModifiedIncludesTimestamp() {
        val content = "Test content".toByteArray()
        val lastModified = Instant.parse("2024-03-06T12:00:00Z")
        
        val eTag = ETagGenerator.generateForContent(content, lastModified)
        
        assertTrue(eTag.contains(lastModified.toEpochMilli().toString()))
    }
    
    @Test
    fun generateWeakETag() {
        val content = "Test content".toByteArray()
        
        val weakETag = ETagGenerator.generateWeak(content)
        
        assertTrue(weakETag.startsWith("W/"))
        val normalETag = ETagGenerator.generateForContent(content)
        assertEquals(weakETag, "W/$normalETag")
    }
    
    @Test
    fun generateConsistentETagForSameContent() {
        val content = "Test content"
        
        val eTag1 = ETagGenerator.generateForString(content)
        val eTag2 = ETagGenerator.generateForString(content)
        
        assertEquals(eTag1, eTag2)
    }
    
    @Test
    fun generateDifferentETagForDifferentContent() {
        val content1 = "Test content 1"
        val content2 = "Test content 2"
        
        val eTag1 = ETagGenerator.generateForString(content1)
        val eTag2 = ETagGenerator.generateForString(content2)
        
        assertNotEquals(eTag1, eTag2)
    }
}

class CacheControlPolicyTest {
    
    private lateinit var policy: CacheControlPolicy
    
    @BeforeEach
    fun setUp() {
        policy = CacheControlPolicy.builder()
    }
    
    @Test
    fun builderCreatesDefaultPolicy() {
        val built = policy.build("etag", Instant.now())
        
        assertTrue(built.public)
        assertFalse(built.private)
        assertFalse(built.noCache)
        assertEquals(Duration.ofMinutes(5), built.maxAge)
    }
    
    @Test
    fun maxAgeSetsDuration() {
        val built = policy.maxAge(Duration.ofHours(1)).build("etag", Instant.now())
        
        assertEquals(Duration.ofHours(1), built.maxAge)
        assertTrue(built.toCacheControlHeader().contains("max-age=3600"))
    }
    
    @Test
    fun maxAgeSecondsSetsDuration() {
        val built = policy.maxAgeSeconds(3600).build("etag", Instant.now())
        
        assertEquals(Duration.ofHours(1), built.maxAge)
        assertTrue(built.toCacheControlHeader().contains("max-age=3600"))
    }
    
    @Test
    fun mustRevalidateEnablesFlag() {
        val built = policy.mustRevalidate().build("etag", Instant.now())
        
        assertTrue(built.mustRevalidate)
        assertTrue(built.toCacheControlHeader().contains("must-revalidate"))
    }
    
    @Test
    fun noCacheDisablesCaching() {
        val built = policy.noCache().build("etag", Instant.now())
        
        assertTrue(built.noCache)
        assertTrue(built.noStore)
        assertTrue(built.toCacheControlHeader().contains("no-cache"))
        assertTrue(built.toCacheControlHeader().contains("no-store"))
    }
    
    @Test
    fun publicSetsPublic() {
        val built = policy.public().build("etag", Instant.now())
        
        assertTrue(built.public)
        assertFalse(built.private)
        assertTrue(built.toCacheControlHeader().contains("public"))
    }
    
    @Test
    fun privateSetsPrivate() {
        val built = policy.private().build("etag", Instant.now())
        
        assertTrue(built.private)
        assertFalse(built.public)
        assertTrue(built.toCacheControlHeader().contains("private"))
    }
    
    @Test
    fun noTransformSetsFlag() {
        val built = policy.noTransform().build("etag", Instant.now())
        
        assertTrue(built.noTransform)
        assertTrue(built.toCacheControlHeader().contains("no-transform"))
    }
    
    @Test
    fun sMaxAgeSetsDirective() {
        val built = policy.sMaxAge(Duration.ofHours(2)).build("etag", Instant.now())
        
        assertEquals(Duration.ofHours(2), built.sMaxAge)
        assertTrue(built.toCacheControlHeader().contains("s-maxage=7200"))
    }
    
    @Test
    fun staleWhileRevalidateSetsDirective() {
        val built = policy.staleWhileRevalidate(Duration.ofMinutes(10)).build("etag", Instant.now())
        
        assertEquals(Duration.ofMinutes(10), built.staleWhileRevalidate)
        assertTrue(built.toCacheControlHeader().contains("stale-while-revalidate=600"))
    }
    
    @Test
    fun staleIfErrorSetsDirective() {
        val built = policy.staleIfError(600).build("etag", Instant.now())
        
        assertEquals(600, built.staleIfError)
        assertTrue(built.toCacheControlHeader().contains("stale-if-error=600"))
    }
    
    @Test
    fun proxyRevalidateSetsDirective() {
        val built = policy.proxyRevalidate(300).build("etag", Instant.now())
        
        assertEquals(300, built.proxyRevalidate)
        assertTrue(built.toCacheControlHeader().contains("proxy-revalidate=300"))
    }
    
    @Test
    fun chainingMultipleMethods() {
        val built = CacheControlPolicy.builder()
            .maxAgeSeconds(3600)
            .mustRevalidate()
            .public()
            .build("etag", Instant.now())
        
        assertTrue(built.public)
        assertTrue(built.mustRevalidate)
        assertEquals(Duration.ofHours(1), built.maxAge)
    }
}

class HTTPResponseCacheTest {
    
    private lateinit var cache: HTTPResponseCache
    
    @BeforeEach
    fun setUp() {
        cache = HTTPResponseCache(maxEntries = 100, defaultTtl = Duration.ofMinutes(5))
    }
    
    @Test
    fun getReturnsNullForNonExistentKey() {
        val result = cache.get("nonexistent")
        
        assertNull(result)
    }
    
    @Test
    fun putAndGetReturnsCachedResponse() = runBlocking {
        val headers = HTTPCacheHeaders.public(
            eTag = "\"abc123\"",
            lastModified = Instant.now(),
            maxAge = Duration.ofMinutes(5)
        )
        
        val response = HTTPResponseCache.CachedResponse(
            body = "Hello, World!",
            headers = headers
        )
        
        cache.put("test-key", response)
        
        val retrieved = cache.get("test-key")
        
        assertNotNull(retrieved)
        assertEquals("Hello, World!", retrieved?.body)
        assertEquals("\"abc123\"", retrieved?.headers?.eTag)
    }
    
    @Test
    fun getReturnsNullAfterExpiration() = runBlocking {
        val headers = HTTPCacheHeaders.public(
            eTag = "\"abc123\"",
            lastModified = Instant.now(),
            maxAge = Duration.ofSeconds(1)
        )
        
        val response = HTTPResponseCache.CachedResponse(
            body = "Hello, World!",
            headers = headers
        )
        
        cache.put("test-key", response)
        
        kotlinx.coroutines.delay(2000) // Wait for expiration
        
        val retrieved = cache.get("test-key")
        
        assertNull(retrieved)
    }
    
    @Test
    fun invalidateRemovesCachedResponse() = runBlocking {
        val headers = HTTPCacheHeaders.public(
            eTag = "\"abc123\"",
            lastModified = Instant.now(),
            maxAge = Duration.ofMinutes(5)
        )
        
        val response = HTTPResponseCache.CachedResponse(
            body = "Hello, World!",
            headers = headers
        )
        
        cache.put("test-key", response)
        
        assertNotNull(cache.get("test-key"))
        
        cache.invalidate("test-key")
        
        assertNull(cache.get("test-key"))
    }
    
    @Test
    fun clearRemovesAllResponses() = runBlocking {
        val headers = HTTPCacheHeaders.public(
            eTag = "\"abc123\"",
            lastModified = Instant.now(),
            maxAge = Duration.ofMinutes(5)
        )
        
        val response = HTTPResponseCache.CachedResponse(
            body = "Hello, World!",
            headers = headers
        )
        
        cache.put("key1", response)
        cache.put("key2", response)
        cache.put("key3", response)
        
        assertEquals(3, cache.size())
        
        cache.clear()
        
        assertEquals(0, cache.size())
    }
    
    @Test
    fun getKeysReturnsAllKeys() = runBlocking {
        val headers = HTTPCacheHeaders.public(
            eTag = "\"abc123\"",
            lastModified = Instant.now(),
            maxAge = Duration.ofMinutes(5)
        )
        
        val response = HTTPResponseCache.CachedResponse(
            body = "Hello, World!",
            headers = headers
        )
        
        cache.put("key1", response)
        cache.put("key2", response)
        cache.put("key3", response)
        
        val keys = cache.getKeys()
        
        assertEquals(3, keys.size)
        assertTrue(keys.contains("key1"))
        assertTrue(keys.contains("key2"))
        assertTrue(keys.contains("key3"))
    }
    
    @Test
    fun sizeReturnsCacheSize() = runBlocking {
        val headers = HTTPCacheHeaders.public(
            eTag = "\"abc123\"",
            lastModified = Instant.now(),
            maxAge = Duration.ofMinutes(5)
        )
        
        val response = HTTPResponseCache.CachedResponse(
            body = "Hello, World!",
            headers = headers
        )
        
        assertEquals(0, cache.size())
        
        cache.put("key1", response)
        assertEquals(1, cache.size())
        
        cache.put("key2", response)
        assertEquals(2, cache.size())
        
        cache.put("key3", response)
        assertEquals(3, cache.size())
    }
}
