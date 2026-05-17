package io.github.rygel.fragments.cache

import io.github.rygel.fragments.ContentRelationships
import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class FragmentCacheTest {
    private lateinit var fragmentCache: FragmentCache

    @BeforeEach
    fun setUp() {
        fragmentCache =
            FragmentCache(
                fragmentTtl = Duration.ofMinutes(5),
                listTtl = Duration.ofMinutes(2),
                relationshipTtl = Duration.ofMinutes(10),
                parsedContentTtl = Duration.ofMinutes(30),
                maxSize = 100L,
            )
    }

    @Test
    fun getOrComputeFragmentComputesOnMiss() =
        runBlocking {
            var computeCalls = 0

            val fragment =
                fragmentCache.getOrComputeFragment("test-slug") {
                    computeCalls++
                    createTestFragment("test-slug")
                }

            assertEquals("test-slug", fragment.slug)
            assertEquals(1, computeCalls)

            val fragment2 =
                fragmentCache.getOrComputeFragment("test-slug") {
                    computeCalls++
                    createTestFragment("test-slug")
                }

            assertEquals(1, computeCalls)
            assertEquals(fragment.slug, fragment2.slug)
        }

    @Test
    fun getOrComputeVisibleFragmentsCachesList() =
        runBlocking {
            var computeCalls = 0

            val fragments1 =
                fragmentCache.getOrComputeVisibleFragments {
                    computeCalls++
                    listOf(
                        createTestFragment("slug1"),
                        createTestFragment("slug2"),
                    )
                }

            assertEquals(2, fragments1.size)
            assertEquals(1, computeCalls)

            val fragments2 =
                fragmentCache.getOrComputeVisibleFragments {
                    computeCalls++
                    emptyList()
                }

            assertEquals(1, computeCalls)
            assertEquals(2, fragments2.size)
        }

    @Test
    fun getOrComputeByTagCachesByTag() =
        runBlocking {
            var computeCalls = 0

            val fragments1 =
                fragmentCache.getOrComputeByTag("kotlin") {
                    computeCalls++
                    listOf(
                        createTestFragment("slug1", tags = listOf("kotlin")),
                        createTestFragment("slug2", tags = listOf("kotlin")),
                    )
                }

            assertEquals(2, fragments1.size)
            assertEquals(1, computeCalls)

            val fragments2 =
                fragmentCache.getOrComputeByTag("kotlin") {
                    computeCalls++
                    emptyList()
                }

            assertEquals(1, computeCalls)
            assertEquals(2, fragments2.size)
        }

    @Test
    fun getOrComputeByCategoryCachesByCategory() =
        runBlocking {
            var computeCalls = 0

            val fragments1 =
                fragmentCache.getOrComputeByCategory("technology") {
                    computeCalls++
                    listOf(
                        createTestFragment("slug1", categories = listOf("technology")),
                        createTestFragment("slug2", categories = listOf("technology")),
                    )
                }

            assertEquals(2, fragments1.size)
            assertEquals(1, computeCalls)

            val fragments2 =
                fragmentCache.getOrComputeByCategory("technology") {
                    computeCalls++
                    emptyList()
                }

            assertEquals(1, computeCalls)
            assertEquals(2, fragments2.size)
        }

    @Test
    fun getOrComputeByAuthorCachesByAuthor() =
        runBlocking {
            var computeCalls = 0

            val fragments1 =
                fragmentCache.getOrComputeByAuthor("author1") {
                    computeCalls++
                    listOf(
                        createTestFragment("slug1", authorIds = listOf("author1")),
                        createTestFragment("slug2", authorIds = listOf("author1")),
                    )
                }

            assertEquals(2, fragments1.size)
            assertEquals(1, computeCalls)

            val fragments2 =
                fragmentCache.getOrComputeByAuthor("author1") {
                    computeCalls++
                    emptyList()
                }

            assertEquals(1, computeCalls)
            assertEquals(2, fragments2.size)
        }

    @Test
    fun getOrComputeRelationshipsCachesRelationships() =
        runBlocking {
            var computeCalls = 0
            val relationships =
                ContentRelationships(
                    previous = createTestFragment("prev"),
                    next = createTestFragment("next"),
                    relatedByTag = listOf(createTestFragment("related")),
                    relatedByCategory = emptyList(),
                    relatedByContent = emptyList(),
                    translations = emptyMap(),
                )

            val result1 =
                fragmentCache.getOrComputeRelationships("test-slug") {
                    computeCalls++
                    relationships
                }

            assertNotNull(result1)
            assertEquals("prev", result1.previous?.slug)
            assertEquals(1, computeCalls)

            val result2 =
                fragmentCache.getOrComputeRelationships("test-slug") {
                    computeCalls++
                    ContentRelationships(null, null, emptyList(), emptyList(), emptyList(), emptyMap())
                }

            assertEquals(1, computeCalls)
            assertEquals("prev", result2.previous?.slug)
        }

    @Test
    fun invalidateFragmentRemovesFragment() =
        runBlocking {
            fragmentCache.getOrComputeFragment("test-slug") { createTestFragment("test-slug") }

            var computeCalls = 0
            fragmentCache.getOrComputeFragment("test-slug") {
                computeCalls++
                createTestFragment("test-slug")
            }
            assertEquals(0, computeCalls)

            fragmentCache.invalidateFragment("test-slug")

            var computeCalls2 = 0
            fragmentCache.getOrComputeFragment("test-slug") {
                computeCalls2++
                createTestFragment("test-slug")
            }
            assertEquals(1, computeCalls2)
        }

    @Test
    fun invalidateFragmentListsClearsListCaches() =
        runBlocking {
            fragmentCache.getOrComputeVisibleFragments { listOf(createTestFragment("slug1")) }
            fragmentCache.getOrComputeByTag("kotlin") { listOf(createTestFragment("slug1")) }
            fragmentCache.getOrComputeByCategory("tech") { listOf(createTestFragment("slug1")) }
            fragmentCache.getOrComputeByAuthor("author1") { listOf(createTestFragment("slug1")) }
            fragmentCache.getOrComputeByAuthor("author1") { listOf(createTestFragment("slug1")) }

            var computeCalls = 0
            fragmentCache.getOrComputeVisibleFragments {
                computeCalls++
                listOf(createTestFragment("slug1"))
            }
            assertEquals(0, computeCalls) // all still cached

            fragmentCache.invalidateFragmentLists()

            var computeCalls2 = 0
            fragmentCache.getOrComputeVisibleFragments {
                computeCalls2++
                listOf(createTestFragment("slug1"))
            }
            assertEquals(1, computeCalls2)
        }

    @Test
    fun clearAllRemovesAllCaches() =
        runBlocking {
            fragmentCache.getOrComputeFragment("test-slug") { createTestFragment("test-slug") }
            fragmentCache.getOrComputeVisibleFragments { listOf(createTestFragment("test-slug")) }
            fragmentCache.getOrComputeRelationships("test-slug") {
                ContentRelationships(null, null, emptyList(), emptyList(), emptyList(), emptyMap())
            }

            fragmentCache.clearAll()

            var fragmentCalls = 0
            fragmentCache.getOrComputeFragment("test-slug") {
                fragmentCalls++
                createTestFragment("test-slug")
            }
            assertEquals(1, fragmentCalls) // recomputed
        }

    @Test
    fun getStatisticsReturnsReport() =
        runBlocking {
            fragmentCache.getOrComputeFragment("test-slug") { createTestFragment("test-slug") }
            fragmentCache.getOrComputeFragment("test-slug") { createTestFragment("test-slug") }
            fragmentCache.getFragment("nonexistent")

            val stats = fragmentCache.getStatistics()

            assertNotNull(stats)
            assertNotNull(stats.fragmentStats)
            assertTrue(stats.totalSize >= 1L)
        }

    @Test
    fun resetStatisticsClearsAllStats() =
        runBlocking {
            fragmentCache.getOrComputeFragment("test-slug") { createTestFragment("test-slug") }

            fragmentCache.resetStatistics()

            val stats = fragmentCache.getStatistics()
            assertEquals(0L, stats.fragmentStats.hitCount)
            assertEquals(0L, stats.fragmentStats.missCount)
        }

    @Test
    fun overallHitRateCalculatesCorrectly() =
        runBlocking {
            fragmentCache.getOrComputeFragment("test-slug") { createTestFragment("test-slug") }
            fragmentCache.getOrComputeVisibleFragments { listOf(createTestFragment("test-slug")) }

            // hits
            fragmentCache.getOrComputeFragment("test-slug") { throw IllegalStateException() }
            fragmentCache.getOrComputeFragment("test-slug") { throw IllegalStateException() }
            fragmentCache.getOrComputeVisibleFragments { throw IllegalStateException() }
            fragmentCache.getOrComputeVisibleFragments { throw IllegalStateException() }

            // miss
            fragmentCache.getOrComputeFragment("nonexistent") { createTestFragment("other") }

            val stats = fragmentCache.getStatistics()
            assertTrue(stats.overallHitRate > 0.0)
        }

    private fun createTestFragment(
        slug: String,
        title: String = "Test Title",
        tags: List<String> = emptyList(),
        categories: List<String> = emptyList(),
        authorIds: List<String> = emptyList(),
        seriesSlug: String? = null,
    ): Fragment =
        Fragment(
            title = title,
            slug = slug,
            date = LocalDateTime.now(),
            status = FragmentStatus.PUBLISHED,
            publishDate = null,
            expiryDate = null,
            preview = "Preview text",
            htmlContent = "<p>Content</p>",
            frontMatter = emptyMap(),
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
            statusChangeHistory = emptyList(),
        )
}
