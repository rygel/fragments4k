package io.github.rygel.fragments.cache

import io.github.rygel.fragments.ContentRelationships
import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentRevision
import io.github.rygel.fragments.FragmentStatus
import io.github.rygel.fragments.RelationshipConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class CachedFragmentRepositoryTest {
    private lateinit var delegate: FragmentRepository
    private lateinit var fragmentCache: FragmentCache
    private lateinit var cachedRepo: CachedFragmentRepository

    @BeforeEach
    fun setUp() {
        delegate = mockk(relaxed = true)
        fragmentCache =
            FragmentCache(
                fragmentTtl = Duration.ofMinutes(5),
                listTtl = Duration.ofMinutes(2),
                relationshipTtl = Duration.ofMinutes(10),
                parsedContentTtl = Duration.ofMinutes(30),
                maxSize = 100L,
            )
        cachedRepo = CachedFragmentRepository(delegate, fragmentCache)
    }

    @Test
    fun getAllReturnsFromDelegateOnMiss() =
        runBlocking {
            val fragments = listOf(createTestFragment("a"), createTestFragment("b"))
            coEvery { delegate.getAll() } returns fragments

            val result = cachedRepo.getAll()

            assertEquals(2, result.size)
            coVerify(exactly = 1) { delegate.getAll() }
        }

    @Test
    fun getAllReturnsFromCacheOnHit() =
        runBlocking {
            val fragments = listOf(createTestFragment("a"))
            coEvery { delegate.getAll() } returns fragments

            cachedRepo.getAll()
            val result = cachedRepo.getAll()

            assertEquals(1, result.size)
            coVerify(exactly = 1) { delegate.getAll() }
        }

    @Test
    fun getAllCachesIndividualFragments() =
        runBlocking {
            val fragments = listOf(createTestFragment("slug-1"), createTestFragment("slug-2"))
            coEvery { delegate.getAll() } returns fragments

            cachedRepo.getAll()

            val cached = fragmentCache.getFragment("slug-1")
            assertNotNull(cached)
            assertEquals("slug-1", cached!!.slug)
        }

    @Test
    fun getAllVisibleReturnsFromDelegateOnMiss() =
        runBlocking {
            val fragments = listOf(createTestFragment("a"))
            coEvery { delegate.getAllVisible() } returns fragments

            val result = cachedRepo.getAllVisible()

            assertEquals(1, result.size)
            coVerify(exactly = 1) { delegate.getAllVisible() }
        }

    @Test
    fun getAllVisibleReturnsFromCacheOnHit() =
        runBlocking {
            val fragments = listOf(createTestFragment("a"))
            coEvery { delegate.getAllVisible() } returns fragments

            cachedRepo.getAllVisible()
            val result = cachedRepo.getAllVisible()

            assertEquals(1, result.size)
            coVerify(exactly = 1) { delegate.getAllVisible() }
        }

    @Test
    fun getAllVisibleCachesIndividualFragments() =
        runBlocking {
            val fragments = listOf(createTestFragment("slug-x"))
            coEvery { delegate.getAllVisible() } returns fragments

            cachedRepo.getAllVisible()

            val cached = fragmentCache.getFragment("slug-x")
            assertNotNull(cached)
        }

    @Test
    fun getBySlugReturnsFromDelegateOnMiss() =
        runBlocking {
            val fragment = createTestFragment("test-slug")
            coEvery { delegate.getBySlug("test-slug") } returns fragment

            val result = cachedRepo.getBySlug("test-slug")

            assertNotNull(result)
            assertEquals("test-slug", result!!.slug)
            coVerify(exactly = 1) { delegate.getBySlug("test-slug") }
        }

    @Test
    fun getBySlugReturnsFromCacheOnHit() =
        runBlocking {
            val fragment = createTestFragment("test-slug")
            coEvery { delegate.getBySlug("test-slug") } returns fragment

            cachedRepo.getBySlug("test-slug")
            val result = cachedRepo.getBySlug("test-slug")

            assertNotNull(result)
            coVerify(exactly = 1) { delegate.getBySlug("test-slug") }
        }

    @Test
    fun getBySlugCachesPublishedFragment() =
        runBlocking {
            val fragment = createTestFragment("pub-slug", status = FragmentStatus.PUBLISHED)
            coEvery { delegate.getBySlug("pub-slug") } returns fragment

            cachedRepo.getBySlug("pub-slug")

            val cached = fragmentCache.getFragment("pub-slug")
            assertNotNull(cached)
        }

    @Test
    fun getBySlugDoesNotCacheDraftFragment() =
        runBlocking {
            val fragment = createTestFragment("draft-slug", status = FragmentStatus.DRAFT)
            coEvery { delegate.getBySlug("draft-slug") } returns fragment

            cachedRepo.getBySlug("draft-slug")

            val cached = fragmentCache.getFragment("draft-slug")
            assertNull(cached)
        }

    @Test
    fun getBySlugReturnsNullForMissingSlug() =
        runBlocking {
            coEvery { delegate.getBySlug("missing") } returns null

            val result = cachedRepo.getBySlug("missing")

            assertNull(result)
        }

    @Test
    fun getByYearMonthAndSlugDelegatesDirectly() =
        runBlocking {
            val fragment = createTestFragment("test")
            coEvery { delegate.getByYearMonthAndSlug("2024", "01", "test") } returns fragment

            val result = cachedRepo.getByYearMonthAndSlug("2024", "01", "test")

            assertNotNull(result)
            coVerify(exactly = 1) { delegate.getByYearMonthAndSlug("2024", "01", "test") }
        }

    @Test
    fun getByTagReturnsFromDelegateOnMiss() =
        runBlocking {
            val fragments = listOf(createTestFragment("a", tags = listOf("kotlin")))
            coEvery { delegate.getByTag("kotlin") } returns fragments

            val result = cachedRepo.getByTag("kotlin")

            assertEquals(1, result.size)
            coVerify(exactly = 1) { delegate.getByTag("kotlin") }
        }

    @Test
    fun getByTagReturnsFromCacheOnHit() =
        runBlocking {
            val fragments = listOf(createTestFragment("a", tags = listOf("kotlin")))
            coEvery { delegate.getByTag("kotlin") } returns fragments

            cachedRepo.getByTag("kotlin")
            val result = cachedRepo.getByTag("kotlin")

            assertEquals(1, result.size)
            coVerify(exactly = 1) { delegate.getByTag("kotlin") }
        }

    @Test
    fun getByCategoryReturnsFromDelegateOnMiss() =
        runBlocking {
            val fragments = listOf(createTestFragment("a", categories = listOf("tech")))
            coEvery { delegate.getByCategory("tech") } returns fragments

            val result = cachedRepo.getByCategory("tech")

            assertEquals(1, result.size)
            coVerify(exactly = 1) { delegate.getByCategory("tech") }
        }

    @Test
    fun getByCategoryReturnsFromCacheOnHit() =
        runBlocking {
            val fragments = listOf(createTestFragment("a", categories = listOf("tech")))
            coEvery { delegate.getByCategory("tech") } returns fragments

            cachedRepo.getByCategory("tech")
            val result = cachedRepo.getByCategory("tech")

            assertEquals(1, result.size)
            coVerify(exactly = 1) { delegate.getByCategory("tech") }
        }

    @Test
    fun getByStatusDelegatesDirectly() =
        runBlocking {
            val fragments = listOf(createTestFragment("a", status = FragmentStatus.DRAFT))
            coEvery { delegate.getByStatus(FragmentStatus.DRAFT) } returns fragments

            val result = cachedRepo.getByStatus(FragmentStatus.DRAFT)

            assertEquals(1, result.size)
            coVerify(exactly = 1) { delegate.getByStatus(FragmentStatus.DRAFT) }
        }

    @Test
    fun getByStatusDoesNotCache() =
        runBlocking {
            val fragments = listOf(createTestFragment("a", status = FragmentStatus.DRAFT))
            coEvery { delegate.getByStatus(FragmentStatus.DRAFT) } returns fragments

            cachedRepo.getByStatus(FragmentStatus.DRAFT)
            cachedRepo.getByStatus(FragmentStatus.DRAFT)

            coVerify(exactly = 2) { delegate.getByStatus(FragmentStatus.DRAFT) }
        }

    @Test
    fun getByAuthorReturnsFromDelegateOnMiss() =
        runBlocking {
            val fragments = listOf(createTestFragment("a", authorIds = listOf("author1")))
            coEvery { delegate.getByAuthor("author1") } returns fragments

            val result = cachedRepo.getByAuthor("author1")

            assertEquals(1, result.size)
            coVerify(exactly = 1) { delegate.getByAuthor("author1") }
        }

    @Test
    fun getByAuthorReturnsFromCacheOnHit() =
        runBlocking {
            val fragments = listOf(createTestFragment("a", authorIds = listOf("author1")))
            coEvery { delegate.getByAuthor("author1") } returns fragments

            cachedRepo.getByAuthor("author1")
            val result = cachedRepo.getByAuthor("author1")

            assertEquals(1, result.size)
            coVerify(exactly = 1) { delegate.getByAuthor("author1") }
        }

    @Test
    fun getByAuthorsDelegatesDirectly() =
        runBlocking {
            coEvery { delegate.getByAuthors(any()) } returns emptyList()

            cachedRepo.getByAuthors(listOf("a1", "a2"))

            coVerify(exactly = 1) { delegate.getByAuthors(listOf("a1", "a2")) }
        }

    @Test
    fun getByAuthorsDoesNotCache() =
        runBlocking {
            coEvery { delegate.getByAuthors(any()) } returns emptyList()

            cachedRepo.getByAuthors(listOf("a1"))
            cachedRepo.getByAuthors(listOf("a1"))

            coVerify(exactly = 2) { delegate.getByAuthors(listOf("a1")) }
        }

    @Test
    fun updateFragmentStatusInvalidatesCacheOnSuccess() =
        runBlocking {
            val fragment = createTestFragment("update-me")
            coEvery { delegate.getAll() } returns listOf(fragment)
            coEvery { delegate.updateFragmentStatus("update-me", FragmentStatus.DRAFT, false, null, null) } returns
                Result.success(fragment.copy(status = FragmentStatus.DRAFT))

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("update-me"))

            cachedRepo.updateFragmentStatus("update-me", FragmentStatus.DRAFT, false, null, null)

            assertNull(fragmentCache.getFragment("update-me"))
        }

    @Test
    fun updateFragmentStatusDoesNotInvalidateOnFailure() =
        runBlocking {
            val fragment = createTestFragment("keep-me")
            coEvery { delegate.getAll() } returns listOf(fragment)
            coEvery { delegate.updateFragmentStatus("keep-me", FragmentStatus.DRAFT, false, null, null) } returns
                Result.failure(IllegalArgumentException("nope"))

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("keep-me"))

            cachedRepo.updateFragmentStatus("keep-me", FragmentStatus.DRAFT, false, null, null)

            assertNotNull(fragmentCache.getFragment("keep-me"))
        }

    @Test
    fun updateMultipleFragmentsStatusInvalidatesOnSuccess() =
        runBlocking {
            val f1 = createTestFragment("a")
            coEvery { delegate.getAll() } returns listOf(f1)
            coEvery {
                delegate.updateMultipleFragmentsStatus(listOf("a"), FragmentStatus.ARCHIVED, false, null, null)
            } returns listOf(Result.success(f1))

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("a"))

            cachedRepo.updateMultipleFragmentsStatus(listOf("a"), FragmentStatus.ARCHIVED, false, null, null)

            assertNull(fragmentCache.getFragment("a"))
        }

    @Test
    fun publishMultipleInvalidatesCacheOnSuccess() =
        runBlocking {
            val f1 = createTestFragment("pub-a")
            coEvery { delegate.getAll() } returns listOf(f1)
            coEvery { delegate.publishMultiple(listOf("pub-a"), null, null) } returns listOf(Result.success(f1))

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("pub-a"))

            cachedRepo.publishMultiple(listOf("pub-a"), null, null)

            assertNull(fragmentCache.getFragment("pub-a"))
        }

    @Test
    fun unpublishMultipleInvalidatesCacheOnSuccess() =
        runBlocking {
            val f1 = createTestFragment("unpub-a")
            coEvery { delegate.getAll() } returns listOf(f1)
            coEvery { delegate.unpublishMultiple(listOf("unpub-a"), null, null) } returns listOf(Result.success(f1))

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("unpub-a"))

            cachedRepo.unpublishMultiple(listOf("unpub-a"), null, null)

            assertNull(fragmentCache.getFragment("unpub-a"))
        }

    @Test
    fun archiveMultipleInvalidatesCacheOnSuccess() =
        runBlocking {
            val f1 = createTestFragment("arch-a")
            coEvery { delegate.getAll() } returns listOf(f1)
            coEvery { delegate.archiveMultiple(listOf("arch-a"), null, null) } returns listOf(Result.success(f1))

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("arch-a"))

            cachedRepo.archiveMultiple(listOf("arch-a"), null, null)

            assertNull(fragmentCache.getFragment("arch-a"))
        }

    @Test
    fun publishScheduledFragmentsInvalidatesCacheOnSuccess() =
        runBlocking {
            val f1 = createTestFragment("sched-a")
            val threshold = LocalDateTime.now()
            coEvery { delegate.getAll() } returns listOf(f1)
            coEvery { delegate.publishScheduledFragments(threshold) } returns listOf(Result.success(f1))

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("sched-a"))

            cachedRepo.publishScheduledFragments(threshold)

            assertNull(fragmentCache.getFragment("sched-a"))
        }

    @Test
    fun scheduleMultipleInvalidatesCacheOnSuccess() =
        runBlocking {
            val f1 = createTestFragment("sch-a")
            val date = LocalDateTime.now().plusDays(1)
            coEvery { delegate.getAll() } returns listOf(f1)
            coEvery { delegate.scheduleMultiple(listOf("sch-a"), date, null, null) } returns listOf(Result.success(f1))

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("sch-a"))

            cachedRepo.scheduleMultiple(listOf("sch-a"), date, null, null)

            assertNull(fragmentCache.getFragment("sch-a"))
        }

    @Test
    fun expireFragmentsInvalidatesCacheOnSuccess() =
        runBlocking {
            val f1 = createTestFragment("exp-a")
            val threshold = LocalDateTime.now()
            coEvery { delegate.getAll() } returns listOf(f1)
            coEvery { delegate.expireFragments(threshold) } returns listOf(Result.success(f1))

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("exp-a"))

            cachedRepo.expireFragments(threshold)

            assertNull(fragmentCache.getFragment("exp-a"))
        }

    @Test
    fun getRelationshipsReturnsFromDelegateOnMiss() =
        runBlocking {
            val rels =
                ContentRelationships(
                    previous = createTestFragment("prev"),
                    next = null,
                    relatedByTag = emptyList(),
                    relatedByCategory = emptyList(),
                    relatedByContent = emptyList(),
                    translations = emptyMap(),
                )
            coEvery { delegate.getRelationships("test", any()) } returns rels

            val result = cachedRepo.getRelationships("test", RelationshipConfig())

            assertNotNull(result)
            assertEquals("prev", result!!.previous?.slug)
            coVerify(exactly = 1) { delegate.getRelationships("test", any()) }
        }

    @Test
    fun getRelationshipsReturnsFromCacheOnHit() =
        runBlocking {
            val rels =
                ContentRelationships(
                    previous = createTestFragment("prev"),
                    next = null,
                    relatedByTag = emptyList(),
                    relatedByCategory = emptyList(),
                    relatedByContent = emptyList(),
                    translations = emptyMap(),
                )
            coEvery { delegate.getRelationships("test", any()) } returns rels

            cachedRepo.getRelationships("test", RelationshipConfig())
            val result = cachedRepo.getRelationships("test", RelationshipConfig())

            assertNotNull(result)
            coVerify(exactly = 1) { delegate.getRelationships("test", any()) }
        }

    @Test
    fun getRelationshipsDoesNotCacheNullResult() =
        runBlocking {
            coEvery { delegate.getRelationships("missing", any()) } returns null

            cachedRepo.getRelationships("missing", RelationshipConfig())

            assertNull(fragmentCache.getRelationships("missing"))
        }

    @Test
    fun createRevisionDelegatesDirectly() =
        runBlocking {
            val revision =
                FragmentRevision(
                    id = "rev-1",
                    fragmentSlug = "test",
                    version = 1,
                    title = "Test",
                    content = "# Hello",
                    preview = "Hello",
                    frontMatter = emptyMap(),
                    changedBy = "user",
                    changedAt = LocalDateTime.now(),
                    changeReason = "init",
                )
            coEvery { delegate.createRevision("test", null, null) } returns Result.success(revision)

            val result = cachedRepo.createRevision("test", null, null)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { delegate.createRevision("test", null, null) }
        }

    @Test
    fun getFragmentRevisionsDelegatesDirectly() =
        runBlocking {
            coEvery { delegate.getFragmentRevisions("test") } returns emptyList()

            cachedRepo.getFragmentRevisions("test")

            coVerify(exactly = 1) { delegate.getFragmentRevisions("test") }
        }

    @Test
    fun revertToRevisionInvalidatesCacheOnSuccess() =
        runBlocking {
            val fragment = createTestFragment("rev-slug")
            coEvery { delegate.getAll() } returns listOf(fragment)
            coEvery { delegate.revertToRevision("rev-slug", "rev-1", null, null) } returns Result.success(fragment)

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("rev-slug"))

            cachedRepo.revertToRevision("rev-slug", "rev-1", null, null)

            assertNull(fragmentCache.getFragment("rev-slug"))
        }

    @Test
    fun revertToRevisionDoesNotInvalidateOnFailure() =
        runBlocking {
            val fragment = createTestFragment("rev-slug")
            coEvery { delegate.getAll() } returns listOf(fragment)
            coEvery { delegate.revertToRevision("rev-slug", "rev-1", null, null) } returns
                Result.failure(IllegalArgumentException("not found"))

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("rev-slug"))

            cachedRepo.revertToRevision("rev-slug", "rev-1", null, null)

            assertNotNull(fragmentCache.getFragment("rev-slug"))
        }

    @Test
    fun reloadClearsCacheAndDelegates() =
        runBlocking {
            val fragment = createTestFragment("reload-me")
            coEvery { delegate.getAll() } returns listOf(fragment)

            cachedRepo.getAll()
            assertNotNull(fragmentCache.getFragment("reload-me"))

            cachedRepo.reload()

            assertNull(fragmentCache.getFragment("reload-me"))
            coVerify { delegate.reload() }
        }

    @Test
    fun updateFragmentStatusInvalidatesListCaches() =
        runBlocking {
            val fragment = createTestFragment("list-inval")
            coEvery { delegate.getAllVisible() } returns listOf(fragment)
            coEvery { delegate.updateFragmentStatus("list-inval", FragmentStatus.DRAFT, false, null, null) } returns
                Result.success(fragment.copy(status = FragmentStatus.DRAFT))

            cachedRepo.getAllVisible()
            assertNotNull(fragmentCache.getVisibleFragments())

            cachedRepo.updateFragmentStatus("list-inval", FragmentStatus.DRAFT, false, null, null)

            assertNull(fragmentCache.getVisibleFragments())
        }

    @Test
    fun getScheduledFragmentsDueForPublicationDelegatesDirectly() =
        runBlocking {
            val threshold = LocalDateTime.now()
            coEvery { delegate.getScheduledFragmentsDueForPublication(threshold) } returns emptyList()

            cachedRepo.getScheduledFragmentsDueForPublication(threshold)

            coVerify(exactly = 1) { delegate.getScheduledFragmentsDueForPublication(threshold) }
        }

    @Test
    fun getFragmentsExpiringSoonDelegatesDirectly() =
        runBlocking {
            val threshold = LocalDateTime.now()
            coEvery { delegate.getFragmentsExpiringSoon(threshold) } returns emptyList()

            cachedRepo.getFragmentsExpiringSoon(threshold)

            coVerify(exactly = 1) { delegate.getFragmentsExpiringSoon(threshold) }
        }

    private fun createTestFragment(
        slug: String,
        title: String = "Test Title",
        status: FragmentStatus = FragmentStatus.PUBLISHED,
        tags: List<String> = emptyList(),
        categories: List<String> = emptyList(),
        authorIds: List<String> = emptyList(),
        seriesSlug: String? = null,
    ): Fragment =
        Fragment(
            title = title,
            slug = slug,
            date = LocalDateTime.now(),
            status = status,
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
