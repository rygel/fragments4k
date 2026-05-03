package io.github.rygel.fragments.livereload

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

@Tag("integration")
class LiveReloadManagerTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var contentDir: Path
    private lateinit var testRepository: InMemoryFragmentRepository
    private lateinit var liveReloadManager: LiveReloadManager

    @BeforeEach
    fun setup() {
        contentDir = tempDir.resolve("content")
        Files.createDirectories(contentDir)

        testRepository = InMemoryFragmentRepository()
        liveReloadManager = LiveReloadManager(testRepository, contentDir)
    }

    @AfterEach
    fun tearDown() =
        runBlocking {
            if (liveReloadManager.isWatching()) {
                liveReloadManager.stopWatching()
            }
        }

    @Test
    fun shouldStartWatching() =
        runBlocking {
            // Create initial content
            val file = contentDir.resolve("test.md")
            Files.writeString(file, createMarkdown("Test"))
            testRepository.addFragment(createFragment("test", "Test"))

            // Start watching
            liveReloadManager.startWatching()

            // Verify it's watching
            assertTrue(liveReloadManager.isWatching())
        }

    @Test
    fun shouldDetectFileCreation() =
        runBlocking {
            liveReloadManager.startWatching()

            // Wait for watch to start
            delay(500)

            val events =
                collectEventsDuring(liveReloadManager) {
                    val file = contentDir.resolve("new-post.md")
                    Files.writeString(file, createMarkdown("New Post"))
                    testRepository.addFragment(createFragment("new-post", "New Post"))
                }

            assertTrue(events.isNotEmpty())
            assertEquals(ReloadType.CONTENT, events.first().type)
        }

    @Test
    fun shouldDetectFileModification() =
        runBlocking {
            val file = contentDir.resolve("test.md")
            Files.writeString(file, createMarkdown("Initial"))
            testRepository.addFragment(createFragment("test", "Initial"))

            liveReloadManager.startWatching()

            // Wait for watch to start
            delay(500)

            val events =
                collectEventsDuring(liveReloadManager) {
                    Files.writeString(file, createMarkdown("Modified"))
                    testRepository.updateFragment(createFragment("test", "Modified"))
                }

            assertTrue(events.isNotEmpty())
            assertEquals(ReloadType.CONTENT, events.first().type)
        }

    @Test
    fun shouldStopWatching() =
        runBlocking {
            liveReloadManager.startWatching()

            // Wait for watch to start
            delay(500)

            assertTrue(liveReloadManager.isWatching())

            // Stop watching
            liveReloadManager.stopWatching()

            // Verify it's not watching
            delay(100)
            assertFalse(liveReloadManager.isWatching())
        }

    @Test
    fun shouldDetectFileDeletion() =
        runBlocking {
            val file = contentDir.resolve("test.md")
            Files.writeString(file, createMarkdown("Test"))
            testRepository.addFragment(createFragment("test", "Test"))

            liveReloadManager.startWatching()

            // Wait for watch to start
            delay(500)

            val events =
                collectEventsDuring(liveReloadManager) {
                    Files.delete(file)
                    testRepository.deleteFragment("test")
                }

            assertTrue(events.isNotEmpty())
            assertEquals(ReloadType.CONTENT, events.first().type)
        }

    @Test
    fun shouldDetectChangesInNestedDirectories() =
        runBlocking {
            val nestedDir = contentDir.resolve("blog").resolve("2024").resolve("03")
            Files.createDirectories(nestedDir)

            val file = nestedDir.resolve("test-post.md")
            Files.writeString(file, createMarkdown("Test Post"))
            testRepository.addFragment(createFragment("test-post", "Test Post"))

            liveReloadManager.startWatching()

            // Wait for watch to start
            delay(500)

            val events =
                collectEventsDuring(liveReloadManager) {
                    Files.writeString(file, createMarkdown("Modified Post"))
                    testRepository.updateFragment(createFragment("test-post", "Modified Post"))
                }

            assertTrue(events.isNotEmpty())
            assertEquals(ReloadType.CONTENT, events.first().type)
            assertTrue(events.first().changedFiles.isNotEmpty())
        }

    @Test
    fun shouldHandleMultipleRapidChanges() =
        runBlocking {
            liveReloadManager.startWatching()

            // Wait for watch to start
            delay(500)

            val events =
                collectEventsDuring(liveReloadManager, settleTimeMillis = 2500) {
                    val file1 = contentDir.resolve("post1.md")
                    val file2 = contentDir.resolve("post2.md")
                    val file3 = contentDir.resolve("post3.md")

                    Files.writeString(file1, createMarkdown("Post 1"))
                    testRepository.addFragment(createFragment("post1", "Post 1"))
                    delay(100)

                    Files.writeString(file2, createMarkdown("Post 2"))
                    testRepository.addFragment(createFragment("post2", "Post 2"))
                    delay(100)

                    Files.writeString(file3, createMarkdown("Post 3"))
                    testRepository.addFragment(createFragment("post3", "Post 3"))
                }

            assertTrue(events.isNotEmpty())
            assertEquals(ReloadType.CONTENT, events.first().type)
        }

    @Test
    fun shouldEmitErrorEventOnReloadFailure() =
        runBlocking {
            val errorRepository = ErrorFragmentRepository()
            val errorLiveReloadManager = LiveReloadManager(errorRepository, contentDir)

            errorLiveReloadManager.startWatching()

            // Wait for watch to start
            delay(500)

            try {
                val events =
                    collectEventsDuring(errorLiveReloadManager) {
                        val file = contentDir.resolve("test.md")
                        Files.writeString(file, createMarkdown("Test"))
                    }

                assertTrue(events.isNotEmpty())
                assertEquals(ReloadType.ERROR, events.first().type)
                assertNotNull(events.first().error)
            } finally {
                errorLiveReloadManager.stopWatching()
            }
        }

    @Test
    fun shouldNotWatchNonExistentDirectory() =
        runBlocking {
            val nonExistentDir = tempDir.resolve("nonexistent")
            val liveReloadManager = LiveReloadManager(testRepository, nonExistentDir)

            // Should not throw exception
            liveReloadManager.startWatching()

            // Verify it's not watching
            delay(500)
            assertFalse(liveReloadManager.isWatching())
        }

    @Test
    fun shouldCollectAllEventsSequentially() =
        runBlocking {
            liveReloadManager.startWatching()

            // Wait for watch to start
            delay(500)

            val events =
                collectEventsDuring(liveReloadManager, settleTimeMillis = 1500) {
                    val file = contentDir.resolve("test.md")

                    Files.writeString(file, createMarkdown("Initial"))
                    testRepository.addFragment(createFragment("test", "Initial"))
                    delay(1200)

                    Files.writeString(file, createMarkdown("Modified"))
                    testRepository.updateFragment(createFragment("test", "Modified"))
                    delay(1200)

                    Files.delete(file)
                    testRepository.deleteFragment("test")
                }

            assertTrue(events.size >= 3)
            events.forEach { assertEquals(ReloadType.CONTENT, it.type) }
        }

    private suspend fun collectEventsDuring(
        manager: LiveReloadManager,
        settleTimeMillis: Long = 1500,
        action: suspend () -> Unit,
    ): List<ReloadEvent> =
        coroutineScope {
            val events = Channel<ReloadEvent>(capacity = Channel.UNLIMITED)
            val collectJob =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    manager.reloadEvents.collect { event ->
                        events.send(event)
                    }
                }

            try {
                action()
                delay(settleTimeMillis)
                buildList {
                    while (true) {
                        val event = events.tryReceive().getOrNull() ?: break
                        add(event)
                    }
                }
            } finally {
                collectJob.cancelAndJoin()
            }
        }

    private fun createMarkdown(title: String): String =
        """---
title: "$title"
date: 2024-03-04
slug: ${title.lowercase().replace(" ", "-")}
---

# $title

This is test content.
"""

    private fun createFragment(
        slug: String,
        title: String,
    ): Fragment =
        Fragment(
            slug = slug,
            title = title,
            content = "Content for $title",
            preview = "Preview for $title",
            date = LocalDateTime.now(),
            publishDate = LocalDateTime.now(),
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
        )
}

class InMemoryFragmentRepository : FragmentRepository {
    private val fragments = mutableListOf<Fragment>()

    override suspend fun getAll(): List<Fragment> = fragments.toList()

    override suspend fun getAllVisible(): List<Fragment> = fragments.filter { it.visible }

    override suspend fun getBySlug(slug: String): Fragment? = fragments.find { it.slug == slug }

    override suspend fun getByYearMonthAndSlug(
        year: String,
        month: String,
        slug: String,
    ): Fragment? =
        fragments.find {
            it.slug == slug &&
                it.date?.year == year.toIntOrNull() &&
                it.date?.monthValue == month.toIntOrNull()
        }

    override suspend fun getByTag(tag: String): List<Fragment> = fragments.filter { it.tags.contains(tag) }

    override suspend fun getByCategory(category: String): List<Fragment> = fragments.filter { it.categories.contains(category) }

    override suspend fun getByStatus(status: io.github.rygel.fragments.FragmentStatus): List<Fragment> =
        fragments.filter {
            it.status ==
                status
        }

    override suspend fun getByAuthor(authorId: String): List<Fragment> =
        fragments.filter {
            it.author == authorId ||
                it.authorIds.contains(authorId)
        }

    override suspend fun getByAuthors(authorIds: List<String>): List<Fragment> =
        fragments.filter { fragment ->
            authorIds.any {
                fragment.author ==
                    it ||
                    fragment.authorIds.contains(it)
            }
        }

    override suspend fun updateFragmentStatus(
        slug: String,
        status: io.github.rygel.fragments.FragmentStatus,
        force: Boolean,
        changedBy: String?,
        reason: String?,
    ): Result<Fragment> {
        val f = fragments.find { it.slug == slug }
        return if (f != null) Result.success(f) else Result.failure(IllegalArgumentException("Fragment not found"))
    }

    override suspend fun updateMultipleFragmentsStatus(
        slugs: List<String>,
        status: io.github.rygel.fragments.FragmentStatus,
        force: Boolean,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> =
        slugs.map {
            updateFragmentStatus(it, status, force, changedBy, reason)
        }

    override suspend fun publishMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> =
        slugs.map {
            updateFragmentStatus(it, io.github.rygel.fragments.FragmentStatus.PUBLISHED, false, changedBy, reason)
        }

    override suspend fun unpublishMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> =
        slugs.map {
            updateFragmentStatus(it, io.github.rygel.fragments.FragmentStatus.DRAFT, false, changedBy, reason)
        }

    override suspend fun archiveMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> =
        slugs.map {
            updateFragmentStatus(it, io.github.rygel.fragments.FragmentStatus.ARCHIVED, false, changedBy, reason)
        }

    override suspend fun getScheduledFragmentsDueForPublication(threshold: LocalDateTime): List<Fragment> = emptyList()

    override suspend fun publishScheduledFragments(threshold: LocalDateTime): List<Result<Fragment>> = emptyList()

    override suspend fun scheduleMultiple(
        slugs: List<String>,
        publishDate: LocalDateTime,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> = emptyList()

    override suspend fun expireFragments(threshold: LocalDateTime): List<Result<Fragment>> = emptyList()

    override suspend fun getFragmentsExpiringSoon(threshold: LocalDateTime): List<Fragment> = emptyList()

    override suspend fun getRelationships(
        slug: String,
        config: io.github.rygel.fragments.RelationshipConfig,
    ): io.github.rygel.fragments.ContentRelationships? = null

    override suspend fun createRevision(
        slug: String,
        changedBy: String?,
        reason: String?,
    ): Result<io.github.rygel.fragments.FragmentRevision> = Result.failure(UnsupportedOperationException())

    override suspend fun getFragmentRevisions(slug: String): List<io.github.rygel.fragments.FragmentRevision> = emptyList()

    override suspend fun revertToRevision(
        slug: String,
        revisionId: String,
        changedBy: String?,
        reason: String?,
    ): Result<Fragment> = Result.failure(UnsupportedOperationException())

    override suspend fun reload() {
        // Do nothing for in-memory repository
    }

    suspend fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
    }

    suspend fun updateFragment(fragment: Fragment) {
        val index = fragments.indexOfFirst { it.slug == fragment.slug }
        if (index >= 0) {
            fragments[index] = fragment
        }
    }

    suspend fun deleteFragment(slug: String) {
        fragments.removeIf { it.slug == slug }
    }
}

class ErrorFragmentRepository : FragmentRepository {
    override suspend fun getAll(): List<Fragment> = throw IOException("Simulated repository error")

    override suspend fun getAllVisible(): List<Fragment> = throw IOException("Simulated repository error")

    override suspend fun getBySlug(slug: String): Fragment? = throw IOException("Simulated repository error")

    override suspend fun getByYearMonthAndSlug(
        year: String,
        month: String,
        slug: String,
    ): Fragment? = throw IOException("Simulated repository error")

    override suspend fun getByTag(tag: String): List<Fragment> = throw IOException("Simulated repository error")

    override suspend fun getByCategory(category: String): List<Fragment> = throw IOException("Simulated repository error")

    override suspend fun reload(): Unit = throw IOException("Simulated repository error")

    override suspend fun getByStatus(status: io.github.rygel.fragments.FragmentStatus): List<Fragment> =
        throw IOException("Simulated repository error")

    override suspend fun getByAuthor(authorId: String): List<Fragment> = throw IOException("Simulated repository error")

    override suspend fun getByAuthors(authorIds: List<String>): List<Fragment> = throw IOException("Simulated repository error")

    override suspend fun updateFragmentStatus(
        slug: String,
        status: io.github.rygel.fragments.FragmentStatus,
        force: Boolean,
        changedBy: String?,
        reason: String?,
    ): Result<Fragment> = throw IOException("Simulated repository error")

    override suspend fun updateMultipleFragmentsStatus(
        slugs: List<String>,
        status: io.github.rygel.fragments.FragmentStatus,
        force: Boolean,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> = throw IOException("Simulated repository error")

    override suspend fun publishMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> = throw IOException("Simulated repository error")

    override suspend fun unpublishMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> = throw IOException("Simulated repository error")

    override suspend fun archiveMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> = throw IOException("Simulated repository error")

    override suspend fun getScheduledFragmentsDueForPublication(threshold: LocalDateTime): List<Fragment> =
        throw IOException("Simulated repository error")

    override suspend fun publishScheduledFragments(threshold: LocalDateTime): List<Result<Fragment>> =
        throw IOException("Simulated repository error")

    override suspend fun scheduleMultiple(
        slugs: List<String>,
        publishDate: LocalDateTime,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> = throw IOException("Simulated repository error")

    override suspend fun expireFragments(threshold: LocalDateTime): List<Result<Fragment>> = throw IOException("Simulated repository error")

    override suspend fun getFragmentsExpiringSoon(threshold: LocalDateTime): List<Fragment> =
        throw IOException("Simulated repository error")

    override suspend fun getRelationships(
        slug: String,
        config: io.github.rygel.fragments.RelationshipConfig,
    ): io.github.rygel.fragments.ContentRelationships? = throw IOException("Simulated repository error")

    override suspend fun createRevision(
        slug: String,
        changedBy: String?,
        reason: String?,
    ): Result<io.github.rygel.fragments.FragmentRevision> = throw IOException("Simulated repository error")

    override suspend fun getFragmentRevisions(slug: String): List<io.github.rygel.fragments.FragmentRevision> =
        throw IOException("Simulated repository error")

    override suspend fun revertToRevision(
        slug: String,
        revisionId: String,
        changedBy: String?,
        reason: String?,
    ): Result<Fragment> = throw IOException("Simulated repository error")
}
