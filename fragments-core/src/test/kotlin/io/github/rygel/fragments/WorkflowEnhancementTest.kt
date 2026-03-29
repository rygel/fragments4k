package io.github.rygel.fragments

import io.github.rygel.fragments.test.InMemoryFragmentRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class WorkflowEnhancementTest {

    private lateinit var repository: InMemoryFragmentRepository

    @BeforeEach
    fun setUp() {
        repository = InMemoryFragmentRepository()
    }

    @Test
    fun fragmentStatusIncludesReviewAndApproved() {
        val reviewStatus = FragmentStatus.REVIEW
        val approvedStatus = FragmentStatus.APPROVED

        assertEquals("REVIEW", reviewStatus.name)
        assertEquals("APPROVED", approvedStatus.name)
    }

    @Test
    fun fragmentReviewAndApprovedTransitions() {
        assertTrue(FragmentStatus.canTransition(FragmentStatus.DRAFT, FragmentStatus.REVIEW))
        assertTrue(FragmentStatus.canTransition(FragmentStatus.DRAFT, FragmentStatus.APPROVED))
        assertTrue(FragmentStatus.canTransition(FragmentStatus.REVIEW, FragmentStatus.APPROVED))
        assertTrue(FragmentStatus.canTransition(FragmentStatus.REVIEW, FragmentStatus.DRAFT))
        assertTrue(FragmentStatus.canTransition(FragmentStatus.APPROVED, FragmentStatus.PUBLISHED))
        assertTrue(FragmentStatus.canTransition(FragmentStatus.APPROVED, FragmentStatus.DRAFT))
    }

    @Test
    fun fragmentHasIsReviewAndIsApprovedProperties() = runBlocking {
        val fragment = Fragment(
            title = "Test",
            slug = "test",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.REVIEW
        )

        assertTrue(fragment.isReview)
        assertFalse(fragment.isApproved)

        val approvedFragment = fragment.copy(status = FragmentStatus.APPROVED)
        assertTrue(approvedFragment.isApproved)
        assertFalse(approvedFragment.isReview)
    }

    @Test
    fun statusChangeHistoryRecordsTransitions() = runBlocking {
        val fragment = Fragment(
            title = "Test",
            slug = "test",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.DRAFT
        )

        repository.addFragment(fragment)

        repository.updateFragmentStatus("test", FragmentStatus.REVIEW, force = false, changedBy = "user1", reason = "Ready for review")

        val updated = repository.getBySlug("test")
        assertNotNull(updated)
        assertEquals(FragmentStatus.REVIEW, updated!!.status)
        assertEquals(1, updated.statusChangeHistory.size)
        assertEquals(FragmentStatus.DRAFT, updated.statusChangeHistory[0].fromStatus)
        assertEquals(FragmentStatus.REVIEW, updated.statusChangeHistory[0].toStatus)
        assertEquals("user1", updated.statusChangeHistory[0].changedBy)
        assertEquals("Ready for review", updated.statusChangeHistory[0].reason)
    }

    @Test
    fun statusChangeHistoryAccumulatesTransitions() = runBlocking {
        val fragment = Fragment(
            title = "Test",
            slug = "test",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.DRAFT
        )

        repository.addFragment(fragment)

        repository.updateFragmentStatus("test", FragmentStatus.REVIEW, changedBy = "user1", reason = "Ready for review")
        repository.updateFragmentStatus("test", FragmentStatus.APPROVED, changedBy = "user2", reason = "Approved")
        repository.updateFragmentStatus("test", FragmentStatus.PUBLISHED, changedBy = "user2", reason = "Published")

        val updated = repository.getBySlug("test")
        assertNotNull(updated)
        assertEquals(3, updated!!.statusChangeHistory.size)
        assertEquals(FragmentStatus.DRAFT, updated.statusChangeHistory[0].fromStatus)
        assertEquals(FragmentStatus.REVIEW, updated.statusChangeHistory[0].toStatus)
        assertEquals(FragmentStatus.REVIEW, updated.statusChangeHistory[1].fromStatus)
        assertEquals(FragmentStatus.APPROVED, updated.statusChangeHistory[1].toStatus)
        assertEquals(FragmentStatus.APPROVED, updated.statusChangeHistory[2].fromStatus)
        assertEquals(FragmentStatus.PUBLISHED, updated.statusChangeHistory[2].toStatus)
    }

    @Test
    fun getAllVisibleExcludesReviewAndApproved() = runBlocking {
        val draft = Fragment(
            title = "Draft",
            slug = "draft",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.DRAFT
        )

        val review = Fragment(
            title = "Review",
            slug = "review",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.REVIEW
        )

        val approved = Fragment(
            title = "Approved",
            slug = "approved",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.APPROVED
        )

        val published = Fragment(
            title = "Published",
            slug = "published",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(draft)
        repository.addFragment(review)
        repository.addFragment(approved)
        repository.addFragment(published)

        val visible = repository.getAllVisible()
        assertEquals(1, visible.size)
        assertEquals("published", visible[0].slug)
    }

    @Test
    fun publishMultipleUpdatesMultipleFragments() = runBlocking {
        val draft1 = Fragment(
            title = "Draft 1",
            slug = "draft1",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.APPROVED
        )

        val draft2 = Fragment(
            title = "Draft 2",
            slug = "draft2",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.APPROVED
        )

        repository.addFragment(draft1)
        repository.addFragment(draft2)

        val results = repository.publishMultiple(listOf("draft1", "draft2"), changedBy = "admin", reason = "Batch publish")

        assertEquals(2, results.size)
        assertTrue(results.all { it.isSuccess })

        val published1 = repository.getBySlug("draft1")
        val published2 = repository.getBySlug("draft2")

        assertEquals(FragmentStatus.PUBLISHED, published1!!.status)
        assertEquals(FragmentStatus.PUBLISHED, published2!!.status)
        assertEquals("admin", published1.statusChangeHistory.last().changedBy)
        assertEquals("admin", published2.statusChangeHistory.last().changedBy)
    }

    @Test
    fun archiveMultipleUpdatesMultipleFragments() = runBlocking {
        val published1 = Fragment(
            title = "Published 1",
            slug = "published1",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        val published2 = Fragment(
            title = "Published 2",
            slug = "published2",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(published1)
        repository.addFragment(published2)

        val results = repository.archiveMultiple(listOf("published1", "published2"), changedBy = "admin", reason = "Old content")

        assertEquals(2, results.size)
        assertTrue(results.all { it.isSuccess })

        val archived1 = repository.getBySlug("published1")
        val archived2 = repository.getBySlug("published2")

        assertEquals(FragmentStatus.ARCHIVED, archived1!!.status)
        assertEquals(FragmentStatus.ARCHIVED, archived2!!.status)
        assertEquals("Old content", archived1.statusChangeHistory.last().reason)
    }

    @Test
    fun expiryDateFieldIsParsedAndStored() = runBlocking {
        val fragment = Fragment(
            title = "Test",
            slug = "test",
            date = LocalDateTime.now(),
            publishDate = LocalDateTime.now(),
            expiryDate = LocalDateTime.now().plusDays(30),
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment)

        val retrieved = repository.getBySlug("test")
        assertNotNull(retrieved)
        assertNotNull(retrieved!!.expiryDate)
    }

    @Test
    fun getAllVisibleRespectsExpiryDate() = runBlocking {
        val now = LocalDateTime.now()

        val notExpired = Fragment(
            title = "Not Expired",
            slug = "not-expired",
            date = now.minusDays(1),
            publishDate = now.minusDays(1),
            expiryDate = now.plusDays(30),
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        val expired = Fragment(
            title = "Expired",
            slug = "expired",
            date = now.minusDays(10),
            publishDate = now.minusDays(10),
            expiryDate = now.minusDays(1),
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        val noExpiry = Fragment(
            title = "No Expiry",
            slug = "no-expiry",
            date = now.minusDays(1),
            publishDate = now.minusDays(1),
            expiryDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(notExpired)
        repository.addFragment(expired)
        repository.addFragment(noExpiry)

        val visible = repository.getAllVisible()
        val slugs = visible.map { it.slug }
        
        assertEquals(2, visible.size)
        assertTrue(slugs.contains("no-expiry"))
        assertTrue(slugs.contains("not-expired"))
        assertFalse(slugs.contains("expired"))
    }

    @Test
    fun getFragmentsExpiringSoonReturnsCorrectFragments() = runBlocking {
        val now = LocalDateTime.now()

        val expiringSoon = Fragment(
            title = "Expiring Soon",
            slug = "expiring-soon",
            date = now,
            publishDate = now.minusDays(10),
            expiryDate = now.plusDays(3),
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        val notExpiringSoon = Fragment(
            title = "Not Expiring Soon",
            slug = "not-expiring-soon",
            date = now,
            publishDate = now.minusDays(10),
            expiryDate = now.plusDays(20),
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        val noExpiryDate = Fragment(
            title = "No Expiry Date",
            slug = "no-expiry-date",
            date = now,
            publishDate = now.minusDays(10),
            expiryDate = null,
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(expiringSoon)
        repository.addFragment(notExpiringSoon)
        repository.addFragment(noExpiryDate)

        val expiringSoonFragments = repository.getFragmentsExpiringSoon(now.plusDays(7))
        assertEquals(1, expiringSoonFragments.size)
        assertEquals("expiring-soon", expiringSoonFragments[0].slug)
    }

    @Test
    fun statusChangeHistoryRecordsScheduledPublication() = runBlocking {
        val now = LocalDateTime.now()

        val scheduled = Fragment(
            title = "Scheduled",
            slug = "scheduled",
            date = now,
            publishDate = now.plusHours(1),
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.SCHEDULED
        )

        repository.addFragment(scheduled)

        repository.publishScheduledFragments(now.plusHours(2))

        val published = repository.getBySlug("scheduled")
        assertNotNull(published)
        assertEquals(FragmentStatus.PUBLISHED, published!!.status)
        assertEquals(1, published.statusChangeHistory.size)
        assertEquals("system", published.statusChangeHistory[0].changedBy)
        assertEquals("Scheduled publication", published.statusChangeHistory[0].reason)
    }

    @Test
    fun statusChangeHistoryRecordsExpiry() = runBlocking {
        val now = LocalDateTime.now()

        val expired = Fragment(
            title = "Expired",
            slug = "expired",
            date = now.minusDays(30),
            publishDate = now.minusDays(30),
            expiryDate = now.minusDays(1),
            preview = "preview",
            content = "content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(expired)

        repository.expireFragments(now)

        val expiredFragment = repository.getBySlug("expired")
        assertNotNull(expiredFragment)
        assertEquals(FragmentStatus.EXPIRED, expiredFragment!!.status)
        assertEquals(1, expiredFragment.statusChangeHistory.size)
        assertEquals("system", expiredFragment.statusChangeHistory[0].changedBy)
        assertEquals("Content expired", expiredFragment.statusChangeHistory[0].reason)
    }
}
