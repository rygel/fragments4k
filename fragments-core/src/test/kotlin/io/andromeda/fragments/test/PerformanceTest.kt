package io.andromeda.fragments.test

import io.andromeda.fragments.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class PerformanceTest {

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun fragmentCreationPerformance() {
        val iterations = 1000
        val start = System.currentTimeMillis()
        
        repeat(iterations) {
            Fragment(
                slug = "test-slug",
                title = "Test Title",
                date = LocalDateTime.now(),
                publishDate = LocalDateTime.now(),
                preview = "Test preview content",
                content = "# Test Content\n\nThis is test content with markdown formatting.",
                template = "blog",
                visible = true,
                tags = listOf("kotlin", "performance"),
                categories = listOf("testing"),
                frontMatter = mapOf(
                    "author" to "Test Author",
                    "language" to "en"
                ),
                authorIds = emptyList()
            )
        }
        
        val duration = System.currentTimeMillis() - start
        val avgTime = duration.toDouble() / iterations
        println("Fragment creation: ${iterations} iterations in ${duration}ms (avg: ${avgTime}ms per fragment)")
        
        // Should create at least 1000 fragments per second
        assert(avgTime < 1.0) { "Fragment creation should be faster than 1ms average" }
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun repositoryPerformance() {
        val iterations = 100
        val start = System.currentTimeMillis()
        
        runBlocking {
            val repo = InMemoryFragmentRepository()
            
            repeat(iterations) {
                val fragment = Fragment(
                    slug = "perf-test-$it",
                    title = "Performance Test $it",
                    date = LocalDateTime.now(),
                    publishDate = LocalDateTime.now(),
                    preview = "Preview content",
                    content = "Content for performance testing",
                    template = "blog",
                    visible = true,
                    tags = emptyList(),
                    categories = emptyList(),
                    frontMatter = emptyMap(),
                    authorIds = emptyList()
                )
                repo.addFragment(fragment)
            }
            
            repeat(iterations) {
                repo.getAll()
                repo.getBySlug("perf-test-${iterations / 2}")
                repo.getAllVisible()
            }
        }
        
        val duration = System.currentTimeMillis() - start
        val avgTime = duration.toDouble() / iterations
        println("Repository operations: ${iterations} iterations in ${duration}ms (avg: ${avgTime}ms per iteration)")
        
        // Repository operations should be fast
        assert(avgTime < 10.0) { "Repository operations should be faster than 10ms average" }
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun markdownRenderingPerformance() {
        val iterations = 1000
        val markdown = """
            # Large Markdown Content
            
            This is a performance test for markdown rendering. We're testing how fast
            the library can parse and render markdown content.
            
            ## Features to Test
            
            - Headers (H1-H6)
            - Paragraphs
            - Lists (ordered and unordered)
            - Links
            - Images
            - Code blocks
            - Tables
            - Bold and italic
            - Blockquotes
            
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
        """.trimIndent()
        
        val start = System.currentTimeMillis()
        
        repeat(iterations) {
            val fragment = Fragment(
                slug = "markdown-perf",
                title = "Markdown Performance Test",
                date = LocalDateTime.now(),
                publishDate = LocalDateTime.now(),
                preview = markdown.substring(0, 100),
                content = markdown,
                template = "blog",
                visible = true,
                tags = emptyList(),
                categories = emptyList(),
                frontMatter = emptyMap(),
                authorIds = emptyList()
            )
        }
        
        val duration = System.currentTimeMillis() - start
        val avgTime = duration.toDouble() / iterations
        println("Markdown rendering: ${iterations} iterations in ${duration}ms (avg: ${avgTime}ms per fragment)")
        
        // Fragment creation with markdown should be reasonable
        assert(avgTime < 5.0) { "Markdown fragment creation should be faster than 5ms average" }
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun concurrentFragmentAccess() {
        val threadCount = 10
        val fragmentsPerThread = 100
        val start = System.currentTimeMillis()
        
        runBlocking {
            val repo = InMemoryFragmentRepository()
            
            val deferred = List(threadCount) { threadId ->
                async {
                    repeat(fragmentsPerThread) { i ->
                        val fragment = Fragment(
                            slug = "concurrent-$threadId-$i",
                            title = "Concurrent Test $threadId-$i",
                            date = LocalDateTime.now(),
                            publishDate = LocalDateTime.now(),
                            preview = "Preview",
                            content = "Content",
                            template = "blog",
                            visible = true,
                            tags = emptyList(),
                            categories = emptyList(),
                            frontMatter = emptyMap(),
                            authorIds = emptyList()
                        )
                        repo.addFragment(fragment)
                        repo.getBySlug("concurrent-$threadId-$i")
                    }
                }
            }
            
            deferred.awaitAll()
        }
        
        val duration = System.currentTimeMillis() - start
        val totalOperations = threadCount * fragmentsPerThread * 2  // add + get
        val avgTime = duration.toDouble() / totalOperations
        println("Concurrent access: ${totalOperations} operations in ${duration}ms (avg: ${avgTime}ms per operation)")
        
        // Concurrent operations should still be reasonably fast
        assert(avgTime < 5.0) { "Concurrent operations should be faster than 5ms average" }
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun frontMatterParsingPerformance() {
        val iterations = 1000
        val start = System.currentTimeMillis()
        
        repeat(iterations) {
            val frontMatter = """
                ---
                title: Test Post
                slug: test-post
                date: 2024-03-05
                author: Test Author
                tags: [kotlin, performance, testing]
                categories: [development, benchmarks]
                description: This is a test description
                language: en
                published: true
                ---
                
                # Content starts here
                
                This is the actual content of the blog post.
            """.trimIndent()
            
            val lines = frontMatter.split("---\n")
            // Skip empty lines and find the actual front matter content
            val frontMatterContent = lines.getOrNull(1) ?: ""
        }
        
        val duration = System.currentTimeMillis() - start
        val avgTime = duration.toDouble() / iterations
        println("Front matter parsing: ${iterations} iterations in ${duration}ms (avg: ${avgTime}ms per iteration)")
        
        // Front matter parsing should be very fast
        assert(avgTime < 1.0) { "Front matter parsing should be faster than 1ms average" }
    }
}
