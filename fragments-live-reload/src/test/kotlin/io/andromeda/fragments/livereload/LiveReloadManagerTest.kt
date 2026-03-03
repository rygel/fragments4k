package io.andromeda.fragments.livereload

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository
import io.andromeda.fragments.Visibility
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    fun `should start watching`() = runBlocking {
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
    fun `should detect file creation`() = runBlocking {
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
    fun `should detect file modification`() = runBlocking {
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
    fun `should stop watching`() = runBlocking {
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
            modifiedDate = LocalDateTime.now(),
            visible = Visibility.PUBLIC,
            template = "blog_post"
        )
    }
}

class InMemoryFragmentRepository : FragmentRepository {
    private val fragments = mutableListOf<Fragment>()
    
    override suspend fun getAll(): List<Fragment> = fragments.toList()
    
    override suspend fun getAllVisible(): List<Fragment> = 
        fragments.filter { it.visible == Visibility.PUBLIC }
    
    override suspend fun getBySlug(slug: String): Fragment? = 
        fragments.find { it.slug == slug }
    
    override suspend fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
    }
    
    override suspend fun updateFragment(fragment: Fragment) {
        val index = fragments.indexOfFirst { it.slug == fragment.slug }
        if (index >= 0) {
            fragments[index] = fragment
        }
    }
    
    override suspend fun deleteFragment(slug: String) {
        fragments.removeIf { it.slug == slug }
    }
}
