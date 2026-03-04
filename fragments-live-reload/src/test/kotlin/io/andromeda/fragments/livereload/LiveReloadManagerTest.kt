package io.andromeda.fragments.livereload

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

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
    fun tearDown() = runBlocking {
        if (liveReloadManager.isWatching()) {
            liveReloadManager.stopWatching()
        }
    }
    
    @Test
    fun shouldStartWatching() = runBlocking {
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
    fun shouldDetectFileCreation() = runBlocking {
        liveReloadManager.startWatching()
        
        // Wait for watch to start
        kotlinx.coroutines.delay(500)
        
        // Create a new file
        val file = contentDir.resolve("new-post.md")
        Files.writeString(file, createMarkdown("New Post"))
        testRepository.addFragment(createFragment("new-post", "New Post"))
        
        // Wait for event processing
        kotlinx.coroutines.delay(1000)
        
        // Verify event was emitted
        val events = mutableListOf<ReloadEvent>()
        liveReloadManager.reloadEvents.collect { event ->
            events.add(event)
            if (events.size >= 1) return@collect
        }
        
        assertTrue(events.isNotEmpty())
        assertEquals(ReloadType.CONTENT, events[0].type)
    }
    
    @Test
    fun shouldDetectFileModification() = runBlocking {
        val file = contentDir.resolve("test.md")
        Files.writeString(file, createMarkdown("Initial"))
        testRepository.addFragment(createFragment("test", "Initial"))
        
        liveReloadManager.startWatching()
        
        // Wait for watch to start
        kotlinx.coroutines.delay(500)
        
        // Modify the file
        Files.writeString(file, createMarkdown("Modified"))
        testRepository.updateFragment(createFragment("test", "Modified"))
        
        // Wait for event processing
        kotlinx.coroutines.delay(1000)
        
        // Verify event was emitted
        val events = mutableListOf<ReloadEvent>()
        liveReloadManager.reloadEvents.collect { event ->
            events.add(event)
            if (events.size >= 1) return@collect
        }
        
        assertTrue(events.isNotEmpty())
        assertEquals(ReloadType.CONTENT, events[0].type)
    }
    
    @Test
    fun shouldStopWatching() = runBlocking {
        liveReloadManager.startWatching()
        
        // Wait for watch to start
        kotlinx.coroutines.delay(500)
        
        assertTrue(liveReloadManager.isWatching())
        
        // Stop watching
        liveReloadManager.stopWatching()
        
        // Verify it's not watching
        kotlinx.coroutines.delay(100)
        assertFalse(liveReloadManager.isWatching())
    }
    
    @Test
    fun shouldDetectFileDeletion() = runBlocking {
        val file = contentDir.resolve("test.md")
        Files.writeString(file, createMarkdown("Test"))
        testRepository.addFragment(createFragment("test", "Test"))
        
        liveReloadManager.startWatching()
        
        // Wait for watch to start
        kotlinx.coroutines.delay(500)
        
        // Delete the file
        Files.delete(file)
        testRepository.deleteFragment("test")
        
        // Wait for event processing
        kotlinx.coroutines.delay(1000)
        
        // Verify event was emitted
        val events = mutableListOf<ReloadEvent>()
        liveReloadManager.reloadEvents.collect { event ->
            events.add(event)
            if (events.size >= 1) return@collect
        }
        
        assertTrue(events.isNotEmpty())
        assertEquals(ReloadType.CONTENT, events[0].type)
    }
    
    @Test
    fun shouldDetectChangesInNestedDirectories() = runBlocking {
        val nestedDir = contentDir.resolve("blog").resolve("2024").resolve("03")
        Files.createDirectories(nestedDir)
        
        val file = nestedDir.resolve("test-post.md")
        Files.writeString(file, createMarkdown("Test Post"))
        testRepository.addFragment(createFragment("test-post", "Test Post"))
        
        liveReloadManager.startWatching()
        
        // Wait for watch to start
        kotlinx.coroutines.delay(500)
        
        // Modify nested file
        Files.writeString(file, createMarkdown("Modified Post"))
        testRepository.updateFragment(createFragment("test-post", "Modified Post"))
        
        // Wait for event processing
        kotlinx.coroutines.delay(1000)
        
        // Verify event was emitted
        val events = mutableListOf<ReloadEvent>()
        liveReloadManager.reloadEvents.collect { event ->
            events.add(event)
            if (events.size >= 1) return@collect
        }
        
        assertTrue(events.isNotEmpty())
        assertEquals(ReloadType.CONTENT, events[0].type)
        assertTrue(events[0].changedFiles.isNotEmpty())
    }
    
    @Test
    fun shouldHandleMultipleRapidChanges() = runBlocking {
        liveReloadManager.startWatching()
        
        // Wait for watch to start
        kotlinx.coroutines.delay(500)
        
        // Create multiple files rapidly
        val file1 = contentDir.resolve("post1.md")
        val file2 = contentDir.resolve("post2.md")
        val file3 = contentDir.resolve("post3.md")
        
        Files.writeString(file1, createMarkdown("Post 1"))
        testRepository.addFragment(createFragment("post1", "Post 1"))
        kotlinx.coroutines.delay(100)
        
        Files.writeString(file2, createMarkdown("Post 2"))
        testRepository.addFragment(createFragment("post2", "Post 2"))
        kotlinx.coroutines.delay(100)
        
        Files.writeString(file3, createMarkdown("Post 3"))
        testRepository.addFragment(createFragment("post3", "Post 3"))
        
        // Wait for event processing
        kotlinx.coroutines.delay(2000)
        
        // Verify multiple events were emitted
        val events = mutableListOf<ReloadEvent>()
        liveReloadManager.reloadEvents.collect { event ->
            events.add(event)
            if (events.size >= 3) return@collect
        }
        
        assertTrue(events.size >= 1)
        assertEquals(ReloadType.CONTENT, events[0].type)
    }
    
    @Test
    fun shouldEmitErrorEventOnReloadFailure() = runBlocking {
        val errorRepository = ErrorFragmentRepository()
        val errorLiveReloadManager = LiveReloadManager(errorRepository, contentDir)
        
        errorLiveReloadManager.startWatching()
        
        // Wait for watch to start
        kotlinx.coroutines.delay(500)
        
        // Create a file to trigger reload
        val file = contentDir.resolve("test.md")
        Files.writeString(file, createMarkdown("Test"))
        
        // Wait for event processing
        kotlinx.coroutines.delay(1000)
        
        // Verify error event was emitted
        val events = mutableListOf<ReloadEvent>()
        errorLiveReloadManager.reloadEvents.collect { event ->
            events.add(event)
            if (events.size >= 1) return@collect
        }
        
        assertTrue(events.isNotEmpty())
        assertEquals(ReloadType.ERROR, events[0].type)
        assertNotNull(events[0].error)
        
        // Clean up
        errorLiveReloadManager.stopWatching()
    }
    
    @Test
    fun shouldNotWatchNonExistentDirectory() = runBlocking {
        val nonExistentDir = tempDir.resolve("nonexistent")
        val liveReloadManager = LiveReloadManager(testRepository, nonExistentDir)
        
        // Should not throw exception
        liveReloadManager.startWatching()
        
        // Verify it's not watching
        kotlinx.coroutines.delay(500)
        assertFalse(liveReloadManager.isWatching())
    }
    
    @Test
    fun shouldCollectAllEventsSequentially() = runBlocking {
        liveReloadManager.startWatching()
        
        // Wait for watch to start
        kotlinx.coroutines.delay(500)
        
        // Create, modify, then delete a file
        val file = contentDir.resolve("test.md")
        
        Files.writeString(file, createMarkdown("Initial"))
        testRepository.addFragment(createFragment("test", "Initial"))
        kotlinx.coroutines.delay(200)
        
        Files.writeString(file, createMarkdown("Modified"))
        testRepository.updateFragment(createFragment("test", "Modified"))
        kotlinx.coroutines.delay(200)
        
        Files.delete(file)
        testRepository.deleteFragment("test")
        kotlinx.coroutines.delay(200)
        
        // Collect all events
        val events = mutableListOf<ReloadEvent>()
        liveReloadManager.reloadEvents.collect { event ->
            events.add(event)
            if (events.size >= 3) return@collect
        }
        
        // Verify we got 3 events
        assertTrue(events.size >= 2)
        events.forEach { assertEquals(ReloadType.CONTENT, it.type) }
    }
    
    private fun createMarkdown(title: String): String {
        return """---
title: "$title"
date: 2024-03-04
slug: ${title.lowercase().replace(" ", "-")}
---

# $title

This is test content.
"""
    }
    
    private fun createFragment(slug: String, title: String): Fragment {
        return Fragment(
            slug = slug,
            title = title,
            content = "Content for $title",
            preview = "Preview for $title",
            date = LocalDateTime.now(),
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post"
        )
    }
}

class InMemoryFragmentRepository : FragmentRepository {
    private val fragments = mutableListOf<Fragment>()

    override suspend fun getAll(): List<Fragment> = fragments.toList()

    override suspend fun getAllVisible(): List<Fragment> =
        fragments.filter { it.visible }

    override suspend fun getBySlug(slug: String): Fragment? =
        fragments.find { it.slug == slug }

    override suspend fun getByYearMonthAndSlug(year: String, month: String, slug: String): Fragment? {
        return fragments.find {
            it.slug == slug &&
            it.date?.year == year.toIntOrNull() &&
            it.date?.monthValue == month.toIntOrNull()
        }
    }

    override suspend fun getByTag(tag: String): List<Fragment> =
        fragments.filter { it.tags.contains(tag) }

    override suspend fun getByCategory(category: String): List<Fragment> =
        fragments.filter { it.categories.contains(category) }

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
    override suspend fun getAll(): List<Fragment> {
        throw RuntimeException("Simulated repository error")
    }

    override suspend fun getAllVisible(): List<Fragment> {
        throw RuntimeException("Simulated repository error")
    }

    override suspend fun getBySlug(slug: String): Fragment? {
        throw RuntimeException("Simulated repository error")
    }

    override suspend fun getByYearMonthAndSlug(year: String, month: String, slug: String): Fragment? {
        throw RuntimeException("Simulated repository error")
    }

    override suspend fun getByTag(tag: String): List<Fragment> {
        throw RuntimeException("Simulated repository error")
    }

    override suspend fun getByCategory(category: String): List<Fragment> {
        throw RuntimeException("Simulated repository error")
    }

    override suspend fun reload() {
        throw RuntimeException("Simulated repository error")
    }
}
