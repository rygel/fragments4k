package io.github.rygel.fragments

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime

@Tag("integration")
class FileSystemFragmentRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var repository: FileSystemFragmentRepository

    @BeforeEach
    fun setUp() {
        repository = FileSystemFragmentRepository(tempDir.toString())
    }

    @Test
    fun getAllReturnsEmptyListWhenNoFilesExist() = runBlocking {
        val result = repository.getAll()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getAllVisibleExcludesInvisibleFragments() = runBlocking {
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
    fun getBySlugReturnsCorrectFragment() = runBlocking {
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
    fun getBySlugReturnsNullForNonExistentSlug() = runBlocking {
        val result = repository.getBySlug("non-existent")
        assertNull(result)
    }

    @Test
    fun parseFrontMatterExtractsAllFieldsCorrectly() = runBlocking {
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
    fun getByTagReturnsFragmentsWithMatchingTag() = runBlocking {
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
    fun getByCategoryReturnsFragmentsWithMatchingCategory() = runBlocking {
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
    fun contentIsConvertedToHtml() = runBlocking {
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
    fun previewIsExtractedFromMoreTag() = runBlocking {
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

    @Disabled("TODO: Investigate Result.isSuccess assertion issue")
    @Test
    fun updateFragmentStatusUpdatesFileAndCache() = runBlocking {
        createTestFile("draft-post.md", """
            ---
            title: Draft Post
            slug: draft-post
            status: DRAFT
            visible: true
            ---
            Draft content here.
        """.trimIndent())

        repository.reload()

        val initialResult = repository.getBySlug("draft-post")
        assertNotNull(initialResult)
        assertEquals(FragmentStatus.DRAFT, initialResult?.status)

        val updateResult = repository.updateFragmentStatus("draft-post", FragmentStatus.PUBLISHED)

        println("UpdateResult class: ${updateResult.javaClass.name}")
        println("UpdateResult isSuccess: ${updateResult.isSuccess}")
        println("UpdateResult isFailure: ${updateResult.isFailure}")
        println("UpdateResult exception: ${updateResult.exceptionOrNull()}")
        println("UpdateResult value: ${updateResult.getOrNull()}")

        if (updateResult.isFailure) {
            println("Update failed with exception: ${updateResult.exceptionOrNull()}")
        } else {
            println("Update succeeded with status: ${updateResult.getOrNull()?.status}")
        }

        assertEquals(true, updateResult.isSuccess)

        val updatedFragment = updateResult.getOrNull()
        assertNotNull(updatedFragment)
        assertEquals(FragmentStatus.PUBLISHED, updatedFragment?.status)

        val reloadedFragment = repository.getBySlug("draft-post")
        assertEquals(FragmentStatus.PUBLISHED, reloadedFragment?.status)

        val fileContent = File(tempDir.toFile(), "draft-post.md").readText()
        assertTrue(fileContent.contains("status: PUBLISHED"))
    }

    @Test
    fun getAllVisibleExcludesDraftAndArchived() = runBlocking {
        createTestFile("draft.md", """
            ---
            title: Draft
            slug: draft
            status: DRAFT
            visible: true
            ---
            Draft
        """.trimIndent())

        createTestFile("published.md", """
            ---
            title: Published
            slug: published
            status: PUBLISHED
            visible: true
            ---
            Published
        """.trimIndent())

        repository.reload()

        val visibleFragments = repository.getAllVisible()
        assertEquals(1, visibleFragments.size)
        assertEquals("published", visibleFragments[0].slug)
    }

    @Disabled
    @Test
    fun getAllVisibleIncludesScheduledFragmentsPastPublishDate() = runBlocking {
        val pastPublishDate = LocalDateTime.now().minusDays(1)

        createTestFile("draft.md", """
            ---
            title: Draft
            slug: draft
            status: DRAFT
            visible: true
            ---
            Draft
        """.trimIndent())

        createTestFile("published.md", """
            ---
            title: Published
            slug: published
            status: PUBLISHED
            visible: true
            ---
            Published
        """.trimIndent())

        createTestFile("scheduled-past.md", """
            ---
            title: Scheduled Past
            slug: scheduled-past
            status: SCHEDULED
            publishDate: $pastPublishDate
            visible: true
            ---
            Scheduled Past
        """.trimIndent())

        createTestFile("scheduled-future.md", """
            ---
            title: Scheduled Future
            slug: scheduled-future
            status: SCHEDULED
            publishDate: ${LocalDateTime.now().plusDays(1)}
            visible: true
            ---
            Scheduled Future
        """.trimIndent())

        repository.reload()

        val visibleFragments = repository.getAllVisible()
        assertEquals(2, visibleFragments.size)
        val slugs = visibleFragments.map { it.slug }
        assertTrue(slugs.contains("published"))
        assertTrue(slugs.contains("scheduled-past"))
        assertFalse(slugs.contains("scheduled-future"))
        assertFalse(slugs.contains("draft"))
    }

    @Test
    fun updateFragmentStatusReturnsFailureForNonExistentSlug() = runBlocking {
        val updateResult = repository.updateFragmentStatus("non-existent", FragmentStatus.PUBLISHED)
        assertTrue(updateResult.isFailure)
        assertTrue(updateResult.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun updateFragmentStatusValidatesTransitions() = runBlocking {
        createTestFile("test-post.md", """
            ---
            title: Test Post
            slug: test-post
            status: DRAFT
            visible: true
            ---
            Test content.
        """.trimIndent())

        repository.reload()

        val draftToPublished = repository.updateFragmentStatus("test-post", FragmentStatus.PUBLISHED)
        assertTrue(draftToPublished.isSuccess)
        assertEquals(FragmentStatus.PUBLISHED, draftToPublished.getOrNull()?.status)

        val publishedToDraft = repository.updateFragmentStatus("test-post", FragmentStatus.DRAFT)
        assertTrue(publishedToDraft.isSuccess)
        assertEquals(FragmentStatus.DRAFT, publishedToDraft.getOrNull()?.status)

        val draftToScheduled = repository.updateFragmentStatus("test-post", FragmentStatus.SCHEDULED)
        assertTrue(draftToScheduled.isSuccess)
        assertEquals(FragmentStatus.SCHEDULED, draftToScheduled.getOrNull()?.status)
    }

    @Test
    fun updateFragmentStatusRejectsInvalidTransition() = runBlocking {
        createTestFile("archived-post.md", """
            ---
            title: Archived Post
            slug: archived-post
            status: ARCHIVED
            visible: true
            ---
            Archived content.
        """.trimIndent())

        repository.reload()

        val archivedToScheduled = repository.updateFragmentStatus("archived-post", FragmentStatus.SCHEDULED)
        assertTrue(archivedToScheduled.isFailure)
        assertTrue(archivedToScheduled.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun updateFragmentStatusForcesInvalidTransition() = runBlocking {
        createTestFile("archived-post.md", """
            ---
            title: Archived Post
            slug: archived-post
            status: ARCHIVED
            visible: true
            ---
            Archived content.
        """.trimIndent())

        repository.reload()

        val forceTransition = repository.updateFragmentStatus("archived-post", FragmentStatus.SCHEDULED, force = true)
        assertTrue(forceTransition.isSuccess)
        assertEquals(FragmentStatus.SCHEDULED, forceTransition.getOrNull()?.status)
    }

    @Test
    fun fragmentStatusValidTransitions() {
        assertTrue(FragmentStatus.canTransition(FragmentStatus.DRAFT, FragmentStatus.PUBLISHED))
        assertTrue(FragmentStatus.canTransition(FragmentStatus.DRAFT, FragmentStatus.SCHEDULED))
        assertTrue(FragmentStatus.canTransition(FragmentStatus.SCHEDULED, FragmentStatus.PUBLISHED))
        assertTrue(FragmentStatus.canTransition(FragmentStatus.PUBLISHED, FragmentStatus.ARCHIVED))
        assertTrue(FragmentStatus.canTransition(FragmentStatus.PUBLISHED, FragmentStatus.EXPIRED))
        assertTrue(FragmentStatus.canTransition(FragmentStatus.EXPIRED, FragmentStatus.ARCHIVED))

        assertFalse(FragmentStatus.canTransition(FragmentStatus.ARCHIVED, FragmentStatus.SCHEDULED))
        assertFalse(FragmentStatus.canTransition(FragmentStatus.SCHEDULED, FragmentStatus.EXPIRED))
    }

    private fun createTestFile(filename: String, content: String) {
        val file = File(tempDir.toFile(), filename)
        file.writeText(content)
    }
}
