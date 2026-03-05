package io.andromeda.fragments

import io.andromeda.fragments.test.InMemoryFragmentRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ContentExpiryTest {

    private lateinit var repository: InMemoryFragmentRepository
    private val now = LocalDateTime.of(2024, 1, 15, 10, 0)

    @BeforeEach
    fun setup() = runBlocking {
        repository = InMemoryFragmentRepository()
    }

    @Test
    fun testFragmentExpiryDateProperty() = runBlocking {
        val fragment = Fragment(
            title = "Test",
            slug = "test",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            expiryDate = now.plusDays(30),
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(fragment)

        val retrieved = repository.getBySlug("test")
        assertNotNull(retrieved)
        assertEquals(now.plusDays(30), retrieved?.expiryDate)
    }

    @Test
    fun testGetAllVisibleExcludesExpiredFragments() = runBlocking {
        val expiredFragment = Fragment(
            title = "Expired Post",
            slug = "expired-post",
            status = FragmentStatus.PUBLISHED,
            date = now.minusDays(10),
            publishDate = now.minusDays(10),
            expiryDate = LocalDateTime.now().minusDays(1),
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        val activeFragment = Fragment(
            title = "Active Post",
            slug = "active-post",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            expiryDate = LocalDateTime.now().plusDays(30),
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        val noExpiryFragment = Fragment(
            title = "No Expiry Post",
            slug = "no-expiry-post",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(expiredFragment)
        repository.addFragment(activeFragment)
        repository.addFragment(noExpiryFragment)

        val visible = repository.getAllVisible()
        assertEquals(2, visible.size)
        assertFalse(visible.any { it.slug == "expired-post" })
        assertTrue(visible.any { it.slug == "active-post" })
        assertTrue(visible.any { it.slug == "no-expiry-post" })
    }

    @Test
    fun testGetFragmentsExpiringSoon() = runBlocking {
        val expiringTomorrow = Fragment(
            title = "Expiring Tomorrow",
            slug = "expiring-tomorrow",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            expiryDate = now.plusDays(1),
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        val expiringNextWeek = Fragment(
            title = "Expiring Next Week",
            slug = "expiring-next-week",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            expiryDate = now.plusDays(6),
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        val expiringNextMonth = Fragment(
            title = "Expiring Next Month",
            slug = "expiring-next-month",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            expiryDate = now.plusDays(30),
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(expiringTomorrow)
        repository.addFragment(expiringNextWeek)
        repository.addFragment(expiringNextMonth)

        val expiringSoon = repository.getFragmentsExpiringSoon(now.plusDays(7))
        assertEquals(2, expiringSoon.size)
        assertTrue(expiringSoon.any { it.slug == "expiring-tomorrow" })
        assertTrue(expiringSoon.any { it.slug == "expiring-next-week" })
        assertFalse(expiringSoon.any { it.slug == "expiring-next-month" })
    }

    @Test
    fun testExpireFragmentsTransitionsToExpiredStatus() = runBlocking {
        val fragment = Fragment(
            title = "Old Post",
            slug = "old-post",
            status = FragmentStatus.PUBLISHED,
            date = now.minusDays(10),
            publishDate = now.minusDays(10),
            expiryDate = null,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(fragment)

        val results = repository.expireFragments(now)
        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)

        val expiredFragment = repository.getBySlug("old-post")
        assertEquals(FragmentStatus.EXPIRED, expiredFragment?.status)
        assertTrue(expiredFragment?.statusChangeHistory?.any { it.reason == "Content expired" } == true)
    }

    @Test
    fun testExpireFragmentsDoesNotAffectNewContent() = runBlocking {
        val newFragment = Fragment(
            title = "New Post",
            slug = "new-post",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            expiryDate = null,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(newFragment)

        val results = repository.expireFragments(now)
        assertEquals(0, results.size)

        val fragment = repository.getBySlug("new-post")
        assertEquals(FragmentStatus.PUBLISHED, fragment?.status)
    }

    @Test
    fun testUnpublishMultipleTransitionsToDraft() = runBlocking {
        val fragment1 = Fragment(
            title = "Post 1",
            slug = "post-1",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        val fragment2 = Fragment(
            title = "Post 2",
            slug = "post-2",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val results = repository.unpublishMultiple(
            slugs = listOf("post-1", "post-2"),
            changedBy = "admin",
            reason = "Content review needed"
        )

        assertEquals(2, results.size)
        assertTrue(results.all { it.isSuccess })

        val unpublished1 = repository.getBySlug("post-1")
        val unpublished2 = repository.getBySlug("post-2")
        assertEquals(FragmentStatus.DRAFT, unpublished1?.status)
        assertEquals(FragmentStatus.DRAFT, unpublished2?.status)
        assertTrue(unpublished1?.statusChangeHistory?.any { it.changedBy == "admin" } == true)
        assertTrue(unpublished2?.statusChangeHistory?.any { it.reason == "Content review needed" } == true)
    }

    @Test
    fun testUnpublishMultipleWithPartialFailure() = runBlocking {
        val fragment1 = Fragment(
            title = "Post 1",
            slug = "post-1",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(fragment1)

        val results = repository.unpublishMultiple(
            slugs = listOf("post-1", "non-existent-post"),
            changedBy = "admin"
        )

        assertEquals(2, results.size)
        assertTrue(results[0].isSuccess)
        assertFalse(results[1].isSuccess)

        val unpublished1 = repository.getBySlug("post-1")
        assertEquals(FragmentStatus.DRAFT, unpublished1?.status)
    }

    @Test
    fun testStatusTransitionFromPublishedToDraft() {
        val canTransition = FragmentStatus.canTransition(FragmentStatus.PUBLISHED, FragmentStatus.DRAFT)
        assertTrue(canTransition)

        val validTransitions = FragmentStatus.getValidTransitions(FragmentStatus.PUBLISHED)
        assertTrue(validTransitions.contains(FragmentStatus.DRAFT))
    }

    @Test
    fun testExpiredFragmentsAreNotVisible() = runBlocking {
        val expiredFragment = Fragment(
            title = "Expired",
            slug = "expired",
            status = FragmentStatus.EXPIRED,
            date = now.minusDays(10),
            publishDate = now.minusDays(10),
            expiryDate = now.minusDays(1),
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(expiredFragment)

        val visible = repository.getAllVisible()
        assertFalse(visible.any { it.slug == "expired" })
    }

    @Test
    fun testDraftFragmentsAreNotVisible() = runBlocking {
        val draftFragment = Fragment(
            title = "Draft",
            slug = "draft",
            status = FragmentStatus.DRAFT,
            date = now,
            publishDate = null,
            expiryDate = null,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(draftFragment)

        val visible = repository.getAllVisible()
        assertFalse(visible.any { it.slug == "draft" })
    }

    @Test
    fun testExpiryDateNullDoesNotCauseExpiryDateExpiration() = runBlocking {
        val noExpiryFragment = Fragment(
            title = "No Expiry",
            slug = "no-expiry",
            status = FragmentStatus.PUBLISHED,
            date = now.minusDays(30),
            publishDate = now.minusDays(30),
            expiryDate = null,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(noExpiryFragment)

        val expiringSoon = repository.getFragmentsExpiringSoon(now.plusDays(7))
        assertFalse(expiringSoon.any { it.slug == "no-expiry" })

        val results = repository.expireFragments(now.minusDays(1))
        assertEquals(1, results.size)
        assertEquals(FragmentStatus.EXPIRED, repository.getBySlug("no-expiry")?.status)
    }

    @Test
    fun testGetFragmentsExpiringSoonWithDefaultThreshold() = runBlocking {
        val expiringIn5Days = Fragment(
            title = "Expiring in 5 Days",
            slug = "expiring-5-days",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            expiryDate = now.plusDays(5),
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(expiringIn5Days)

        val expiringSoon = repository.getFragmentsExpiringSoon()
        assertTrue(expiringSoon.any { it.slug == "expiring-5-days" })
    }

    @Test
    fun testGetFragmentsExpiringSoonOnlyPublished() = runBlocking {
        val draftExpiring = Fragment(
            title = "Draft Expiring",
            slug = "draft-expiring",
            status = FragmentStatus.DRAFT,
            date = now,
            publishDate = null,
            expiryDate = now.plusDays(3),
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        val publishedExpiring = Fragment(
            title = "Published Expiring",
            slug = "published-expiring",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            expiryDate = now.plusDays(3),
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )

        repository.addFragment(draftExpiring)
        repository.addFragment(publishedExpiring)

        val expiringSoon = repository.getFragmentsExpiringSoon(now.plusDays(7))
        assertEquals(1, expiringSoon.size)
        assertEquals("published-expiring", expiringSoon[0].slug)
    }
}
