package io.andromeda.fragments

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class FileSystemFragmentRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var repository: FileSystemFragmentRepository

    @BeforeEach
    fun setUp() {
        repository = FileSystemFragmentRepository(tempDir.toString())
    }

    @Test
    fun `getAll returns empty list when no files exist`() = runBlocking {
        val result = repository.getAll()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllVisible excludes invisible fragments`() = runBlocking {
        createTestFile("visible.md", """
            ---
            title: Visible
            visible: true
            ---
            Content
        """.trimIndent())

        createTestFile("invisible.md", """
            ---
            title: Invisible
            visible: false
            ---
            Content
        """.trimIndent())

        repository.reload()

        val result = repository.getAllVisible()
        assertEquals(1, result.size)
        assertEquals("Visible", result.first().title)
    }

    @Test
    fun `getBySlug returns correct fragment`() = runBlocking {
        createTestFile("test-post.md", """
            ---
            title: Test Post
            slug: my-test-post
            date: 2024-01-15
            visible: true
            ---
            Content here
        """.trimIndent())

        repository.reload()

        val result = repository.getBySlug("my-test-post")
        assertNotNull(result)
        assertEquals("Test Post", result!!.title)
    }

    @Test
    fun `getBySlug returns null for non-existent slug`() = runBlocking {
        val result = repository.getBySlug("non-existent")
        assertNull(result)
    }

    @Test
    fun `parseFrontMatter extracts all fields correctly`() = runBlocking {
        createTestFile("full-post.md", """
            ---
            title: Full Post
            slug: full-post
            date: 2024-01-15
            visible: true
            template: custom
            categories: tech, news
            tags: kotlin, java
            order: 5
            ---
            # Content
        """.trimIndent())

        repository.reload()

        val result = repository.getAll().first()

        assertEquals("Full Post", result.title)
        assertEquals("full-post", result.slug)
        
        // Debug date
        println("Date value: ${result.date}")
        assertNotNull(result.date, "Date should not be null")
        
        result.date?.let {
            assertEquals(2024, it.year)
            assertEquals(1, it.monthValue)
            assertEquals(15, it.dayOfMonth)
        }
        
        assertTrue(result.visible)
        assertEquals("custom", result.template)
        assertEquals(listOf("tech", "news"), result.categories)
        assertEquals(listOf("kotlin", "java"), result.tags)
        assertEquals(5, result.order)
    }

    @Test
    fun `getByTag returns fragments with matching tag`() = runBlocking {
        createTestFile("kotlin-post.md", """
            ---
            title: Kotlin Post
            tags: kotlin, programming
            visible: true
            ---
            Content
        """.trimIndent())

        createTestFile("java-post.md", """
            ---
            title: Java Post
            tags: java, programming
            visible: true
            ---
            Content
        """.trimIndent())

        repository.reload()

        val result = repository.getByTag("kotlin")
        assertEquals(1, result.size)
        assertEquals("Kotlin Post", result.first().title)
    }

    @Test
    fun `getByCategory returns fragments with matching category`() = runBlocking {
        createTestFile("tech-post.md", """
            ---
            title: Tech Post
            categories: technology
            visible: true
            ---
            Content
        """.trimIndent())

        createTestFile("other-post.md", """
            ---
            title: Other Post
            categories: other
            visible: true
            ---
            Content
        """.trimIndent())

        repository.reload()

        val result = repository.getByCategory("technology")
        assertEquals(1, result.size)
        assertEquals("Tech Post", result.first().title)
    }

    @Test
    fun `content is converted to HTML`() = runBlocking {
        createTestFile("markdown.md", """
            ---
            title: Markdown
            visible: true
            ---
            # Hello World
            
            This is **bold** and *italic*.
        """.trimIndent())

        repository.reload()

        val result = repository.getAll().first()
        assertTrue(result.content.contains("<h1>"))
        assertTrue(result.content.contains("<strong>"))
    }

    @Test
    fun `preview is extracted from more tag`() = runBlocking {
        createTestFile("with-more.md", """
            ---
            title: With More Tag
            visible: true
            ---
            Preview content here.
            
            <!--more-->
            
            Full content here.
        """.trimIndent())

        repository.reload()

        val result = repository.getAll().first()
        assertTrue(result.hasMoreTag)
        assertTrue(result.preview.contains("Preview content"))
    }

    private fun createTestFile(filename: String, content: String) {
        val file = File(tempDir.toFile(), filename)
        file.writeText(content)
    }
}
