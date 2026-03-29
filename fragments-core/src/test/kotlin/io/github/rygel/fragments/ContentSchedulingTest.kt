package io.github.rygel.fragments

import io.github.rygel.fragments.test.InMemoryFragmentRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ScheduledPublicationJobTest {

    private lateinit var repository: InMemoryFragmentRepository
    private lateinit var job: DefaultScheduledPublicationJob
    private val now = LocalDateTime.of(2024, 1, 15, 10, 0)

    @BeforeEach
    fun setup() = runBlocking {
        repository = InMemoryFragmentRepository()
        repository.addFragment(
            Fragment(
                title = "Scheduled Post 1",
                slug = "scheduled-post-1",
                status = FragmentStatus.SCHEDULED,
                date = now,
                publishDate = now.minusHours(1),
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap()
            )
        )
        repository.addFragment(
            Fragment(
                title = "Scheduled Post 2",
                slug = "scheduled-post-2",
                status = FragmentStatus.SCHEDULED,
                date = now,
                publishDate = now.minusHours(2),
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap()
            )
        )
        repository.addFragment(
            Fragment(
                title = "Future Scheduled Post",
                slug = "future-scheduled-post",
                status = FragmentStatus.SCHEDULED,
                date = now,
                publishDate = now.plusHours(1),
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap()
            )
        )
        repository.addFragment(
            Fragment(
                title = "Published Post",
                slug = "published-post",
                status = FragmentStatus.PUBLISHED,
                date = now,
                publishDate = now,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap()
            )
        )
        job = DefaultScheduledPublicationJob(repository)
    }

    @Test
    fun testExecutePublishesDueFragments() = runBlocking {
        val result = job.execute(now)

        assertEquals(2, result.published)
        assertEquals(0, result.failed)
        assertTrue(result.success)

        val fragment1 = repository.getBySlug("scheduled-post-1")
        val fragment2 = repository.getBySlug("scheduled-post-2")
        val futureFragment = repository.getBySlug("future-scheduled-post")

        assertEquals(FragmentStatus.PUBLISHED, fragment1?.status)
        assertEquals(FragmentStatus.PUBLISHED, fragment2?.status)
        assertEquals(FragmentStatus.SCHEDULED, futureFragment?.status)
    }

    @Test
    fun testExecuteWithCustomThreshold() = runBlocking {
        val futureThreshold = now.plusHours(2)
        val result = job.execute(futureThreshold)

        assertEquals(3, result.published)
        assertEquals(0, result.failed)
        assertTrue(result.success)
    }

    @Test
    fun testExecuteWithNoDueFragments() = runBlocking {
        val pastThreshold = now.minusHours(24)
        val result = job.execute(pastThreshold)

        assertEquals(0, result.published)
        assertTrue(result.success)
    }

    @Test
    fun testScheduledPublicationResultCalculations() = runBlocking {
        val result = ScheduledPublicationResult(
            published = 5,
            failed = 2,
            skipped = 1,
            executionTime = 100,
            errors = listOf("Error 1", "Error 2")
        )

        assertEquals(8, result.total)
        assertFalse(result.success)
    }

    @Test
    fun testScheduledPublicationListenerBeforePublications() = runBlocking {
        var beforeCalled = false
        var fragmentsBefore: List<Fragment>? = null

        val listener = object : ScheduledPublicationListener {
            override suspend fun onBeforePublications(fragments: List<Fragment>) {
                beforeCalled = true
                fragmentsBefore = fragments
            }

            override suspend fun onAfterPublications(result: ScheduledPublicationResult) {}
            override suspend fun onPublicationError(fragment: Fragment, error: Throwable) {}
        }

        val jobWithListener = DefaultScheduledPublicationJob(repository, listOf(listener))
        jobWithListener.execute(now)

        assertTrue(beforeCalled)
        assertNotNull(fragmentsBefore)
        assertEquals(2, fragmentsBefore?.size)
    }

    @Test
    fun testScheduledPublicationListenerAfterPublications() = runBlocking {
        var afterCalled = false
        var resultAfter: ScheduledPublicationResult? = null

        val listener = object : ScheduledPublicationListener {
            override suspend fun onBeforePublications(fragments: List<Fragment>) {}
            override suspend fun onAfterPublications(result: ScheduledPublicationResult) {
                afterCalled = true
                resultAfter = result
            }
            override suspend fun onPublicationError(fragment: Fragment, error: Throwable) {}
        }

        val jobWithListener = DefaultScheduledPublicationJob(repository, listOf(listener))
        jobWithListener.execute(now)

        assertTrue(afterCalled)
        assertNotNull(resultAfter)
        assertEquals(2, resultAfter?.published)
    }

    @Test
    fun testGetScheduledFragmentsDueForPublication() = runBlocking {
        val dueFragments = repository.getScheduledFragmentsDueForPublication(now)

        assertEquals(2, dueFragments.size)
        assertEquals("scheduled-post-1", dueFragments[0].slug)
        assertEquals("scheduled-post-2", dueFragments[1].slug)
    }

    @Test
    fun testPublishScheduledFragments() = runBlocking {
        val results = repository.publishScheduledFragments(now)

        assertEquals(2, results.size)
        assertTrue(results.all { it.isSuccess })

        val fragment1 = repository.getBySlug("scheduled-post-1")
        assertEquals(FragmentStatus.PUBLISHED, fragment1?.status)
        assertTrue(fragment1?.statusChangeHistory?.any { it.reason == "Scheduled publication" } == true)
    }
}

class ContentSchedulingTest {

    private lateinit var repository: InMemoryFragmentRepository
    private val now = LocalDateTime.of(2024, 1, 15, 10, 0)

    @BeforeEach
    fun setup() = runBlocking {
        repository = InMemoryFragmentRepository()
        repository.addFragment(
            Fragment(
                title = "Draft Post 1",
                slug = "draft-post-1",
                status = FragmentStatus.DRAFT,
                date = now,
                publishDate = null,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap()
            )
        )
        repository.addFragment(
            Fragment(
                title = "Draft Post 2",
                slug = "draft-post-2",
                status = FragmentStatus.DRAFT,
                date = now,
                publishDate = null,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap()
            )
        )
        repository.addFragment(
            Fragment(
                title = "Draft Post 3",
                slug = "draft-post-3",
                status = FragmentStatus.DRAFT,
                date = now,
                publishDate = null,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap()
            )
        )
    }

    @Test
    fun testScheduleMultipleFragments() = runBlocking {
        val publishDate = now.plusDays(1)
        val results = repository.scheduleMultiple(
            slugs = listOf("draft-post-1", "draft-post-2", "draft-post-3"),
            publishDate = publishDate,
            changedBy = "admin",
            reason = "Batch scheduling"
        )

        assertEquals(3, results.size)
        assertTrue(results.all { it.isSuccess })

        results.forEach { result ->
            val fragment = result.getOrNull()
            assertEquals(FragmentStatus.SCHEDULED, fragment?.status)
            assertEquals(publishDate, fragment?.publishDate)
            assertTrue(fragment?.statusChangeHistory?.any { it.changedBy == "admin" } == true)
            assertTrue(fragment?.statusChangeHistory?.any { it.reason == "Batch scheduling" } == true)
        }
    }

    @Test
    fun testScheduleMultipleWithPartialFailure() = runBlocking {
        val publishDate = now.plusDays(1)
        val results = repository.scheduleMultiple(
            slugs = listOf("draft-post-1", "non-existent-post", "draft-post-2"),
            publishDate = publishDate
        )

        assertEquals(3, results.size)
        assertTrue(results[0].isSuccess)
        assertFalse(results[1].isSuccess)
        assertTrue(results[2].isSuccess)

        val fragment1 = repository.getBySlug("draft-post-1")
        assertEquals(FragmentStatus.SCHEDULED, fragment1?.status)

        val fragment2 = repository.getBySlug("draft-post-2")
        assertEquals(FragmentStatus.SCHEDULED, fragment2?.status)
    }

    @Test
    fun testScheduleFromDraft() = runBlocking {
        val publishDate = now.plusDays(1)
        val results = repository.scheduleMultiple(
            slugs = listOf("draft-post-1"),
            publishDate = publishDate
        )

        val fragment = results[0].getOrNull()
        assertNotNull(fragment)
        assertEquals(FragmentStatus.SCHEDULED, fragment?.status)
        assertEquals(publishDate, fragment?.publishDate)
        assertTrue(fragment?.statusChangeHistory?.any { it.fromStatus == FragmentStatus.DRAFT } == true)
    }

    @Test
    fun testFragmentIsScheduledProperty() = runBlocking {
        val draftFragment = repository.getBySlug("draft-post-1")
        assertFalse(draftFragment?.isScheduled == true)

        val scheduledFragment = Fragment(
            title = "Test",
            slug = "test",
            status = FragmentStatus.SCHEDULED,
            date = now,
            publishDate = now,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap()
        )
        assertTrue(scheduledFragment.isScheduled)
    }

    @Test
    fun testStatusTransitionToScheduled() {
        val canTransition = FragmentStatus.canTransition(FragmentStatus.DRAFT, FragmentStatus.SCHEDULED)
        assertTrue(canTransition)

        val canTransitionFromPublished = FragmentStatus.canTransition(FragmentStatus.PUBLISHED, FragmentStatus.SCHEDULED)
        assertFalse(canTransitionFromPublished)
    }

    @Test
    fun testScheduleMultipleWithEmptyList() = runBlocking {
        val publishDate = now.plusDays(1)
        val results = repository.scheduleMultiple(
            slugs = emptyList(),
            publishDate = publishDate
        )

        assertEquals(0, results.size)
    }

    @Test
    fun testScheduleMultipleWithSamePublishDate() = runBlocking {
        val publishDate = now.plusHours(6)
        val results = repository.scheduleMultiple(
            slugs = listOf("draft-post-1", "draft-post-2", "draft-post-3"),
            publishDate = publishDate
        )

        assertEquals(3, results.size)
        results.forEach { result ->
            assertEquals(publishDate, result.getOrNull()?.publishDate)
        }
    }

    @Test
    fun testGetAllScheduledFragments() = runBlocking {
        val publishDate = now.plusDays(1)
        repository.scheduleMultiple(
            slugs = listOf("draft-post-1", "draft-post-2"),
            publishDate = publishDate
        )

        val scheduledFragments = repository.getByStatus(FragmentStatus.SCHEDULED)

        assertEquals(2, scheduledFragments.size)
        assertTrue(scheduledFragments.all { it.status == FragmentStatus.SCHEDULED })
    }
}

class PublicationNotificationServiceTest {

    private val now = LocalDateTime.of(2024, 1, 15, 10, 0)
    private val fragment = Fragment(
        title = "Test Fragment",
        slug = "test-fragment",
        status = FragmentStatus.SCHEDULED,
        date = now,
        publishDate = now.plusDays(1),
        preview = "Preview",
        content = "Content",
        frontMatter = emptyMap(),
        author = "John Doe"
    )

    @Test
    fun testNoOpNotificationServiceDoesNothing() = runBlocking {
        val service = NoOpPublicationNotificationService()

        service.notifyScheduledPublication(fragment, now.plusDays(1))
        service.notifyPublicationReminder(fragment, 24)
        service.notifyPublicationSuccess(fragment)
        service.notifyPublicationFailure(fragment, Exception("Test error"))

        assertTrue(true)
    }

    @Test
    fun testLoggingNotificationServiceCreatesNotifications() = runBlocking {
        val notifications = mutableListOf<PublicationNotification>()
        val listener = object : PublicationNotificationListener {
            override suspend fun onNotification(notification: PublicationNotification) {
                notifications.add(notification)
            }
        }

        val service = LoggingPublicationNotificationService(listOf(listener))

        service.notifyScheduledPublication(fragment, now.plusDays(1))
        assertEquals(1, notifications.size)
        assertEquals(NotificationType.SCHEDULED, notifications[0].notificationType)
        assertEquals("Test Fragment", notifications[0].fragmentTitle)

        service.notifyPublicationReminder(fragment, 24)
        assertEquals(2, notifications.size)
        assertEquals(NotificationType.REMINDER, notifications[1].notificationType)

        service.notifyPublicationSuccess(fragment)
        assertEquals(3, notifications.size)
        assertEquals(NotificationType.SUCCESS, notifications[2].notificationType)

        service.notifyPublicationFailure(fragment, Exception("Test error"))
        assertEquals(4, notifications.size)
        assertEquals(NotificationType.FAILURE, notifications[3].notificationType)
    }

    @Test
    fun testPublicationNotificationData() {
        val notification = PublicationNotification(
            fragmentSlug = "test-fragment",
            fragmentTitle = "Test Fragment",
            scheduledDate = now.plusDays(1),
            author = "John Doe",
            notificationType = NotificationType.SCHEDULED,
            message = "Test message"
        )

        assertEquals("test-fragment", notification.fragmentSlug)
        assertEquals("Test Fragment", notification.fragmentTitle)
        assertEquals("John Doe", notification.author)
        assertEquals(NotificationType.SCHEDULED, notification.notificationType)
        assertEquals("Test message", notification.message)
    }
}
