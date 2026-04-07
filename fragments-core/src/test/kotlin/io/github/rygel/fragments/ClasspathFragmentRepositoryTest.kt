package io.github.rygel.fragments

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ClasspathFragmentRepositoryTest {

    private lateinit var repository: ClasspathFragmentRepository

    @BeforeEach
    fun setUp() {
        repository = ClasspathFragmentRepository(
            basePath = "test-classpath-fragments",
            baseUrl = "/blog",
        )
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    @Test
    fun `loads all fragments listed in index`() = runBlocking {
        val all = repository.getAll()
        assertEquals(8, all.size)
    }

    @Test
    fun `returns empty list when index file is missing`() = runBlocking {
        val repo = ClasspathFragmentRepository(basePath = "nonexistent-directory")
        val all = repo.getAll()
        assertTrue(all.isEmpty())
    }

    @Test
    fun `skips commented and blank lines in index`() = runBlocking {
        // The index for this test has 8 real entries; comments/blanks would cause extras
        val all = repository.getAll()
        assertEquals(8, all.size)
    }

    // ── getAllVisible ─────────────────────────────────────────────────────────

    @Test
    fun `getAllVisible returns only published non-expired fragments`() = runBlocking {
        val visible = repository.getAllVisible()
        val slugs = visible.map { it.slug }
        assertTrue(slugs.contains("published-post"), "published-post should be visible")
        assertTrue(slugs.contains("another-post"), "another-post should be visible")
        assertTrue(slugs.contains("tagged-post"), "tagged-post should be visible")
    }

    @Test
    fun `getAllVisible excludes draft fragments`() = runBlocking {
        val visible = repository.getAllVisible()
        assertTrue(visible.none { it.slug == "draft-post" }, "draft-post must not be visible")
    }

    @Test
    fun `getAllVisible excludes fragments with past expiry date`() = runBlocking {
        val visible = repository.getAllVisible()
        assertTrue(visible.none { it.slug == "expired-post" }, "expired-post must not be visible")
    }

    @Test
    fun `getAllVisible excludes scheduled fragments with future publish date`() = runBlocking {
        val visible = repository.getAllVisible()
        assertTrue(visible.none { it.slug == "scheduled-post" }, "scheduled-post must not be visible")
    }

    @Test
    fun `getAllVisible returns fragments sorted by date descending`() = runBlocking {
        val visible = repository.getAllVisible().filter { it.date != null }
        val dates = visible.map { it.date!! }
        assertEquals(dates.sortedDescending(), dates, "fragments must be sorted newest-first")
    }

    // ── getBySlug ─────────────────────────────────────────────────────────────

    @Test
    fun `getBySlug returns correct fragment`() = runBlocking {
        val fragment = repository.getBySlug("published-post")
        assertNotNull(fragment)
        assertEquals("Published Post", fragment!!.title)
    }

    @Test
    fun `getBySlug returns null for unknown slug`() = runBlocking {
        val fragment = repository.getBySlug("does-not-exist")
        assertNull(fragment)
    }

    // ── getByTag ──────────────────────────────────────────────────────────────

    @Test
    fun `getByTag returns fragments with matching tag`() = runBlocking {
        val results = repository.getByTag("kotlin")
        val slugs = results.map { it.slug }
        assertTrue(slugs.contains("published-post"))
        assertTrue(slugs.contains("another-post"))
        assertTrue(slugs.none { it == "tagged-post" }, "tagged-post has no kotlin tag")
    }

    @Test
    fun `getByTag is case-insensitive`() = runBlocking {
        val lower = repository.getByTag("kotlin")
        val upper = repository.getByTag("KOTLIN")
        assertEquals(lower.map { it.slug }.sorted(), upper.map { it.slug }.sorted())
    }

    @Test
    fun `getByTag returns empty list for unknown tag`() = runBlocking {
        val results = repository.getByTag("nonexistent-tag")
        assertTrue(results.isEmpty())
    }

    // ── getByCategory ─────────────────────────────────────────────────────────

    @Test
    fun `getByCategory returns fragments in matching category`() = runBlocking {
        val results = repository.getByCategory("kotlin")
        val slugs = results.map { it.slug }
        assertTrue(slugs.contains("published-post"))
        assertTrue(slugs.contains("another-post"))
    }

    @Test
    fun `getByCategory is case-insensitive`() = runBlocking {
        val lower = repository.getByCategory("education")
        val upper = repository.getByCategory("EDUCATION")
        assertEquals(lower.map { it.slug }.sorted(), upper.map { it.slug }.sorted())
    }

    // ── getByStatus ───────────────────────────────────────────────────────────

    @Test
    fun `getByStatus returns only fragments with matching status`() = runBlocking {
        val drafts = repository.getByStatus(FragmentStatus.DRAFT)
        assertEquals(1, drafts.size)
        assertEquals("draft-post", drafts.first().slug)
    }

    @Test
    fun `getByStatus returns scheduled fragments`() = runBlocking {
        val scheduled = repository.getByStatus(FragmentStatus.SCHEDULED)
        assertEquals(1, scheduled.size)
        assertEquals("scheduled-post", scheduled.first().slug)
    }

    // ── getByAuthor ───────────────────────────────────────────────────────────

    @Test
    fun `getByAuthor returns fragments matching author id`() = runBlocking {
        val results = repository.getByAuthor("alex")
        assertTrue(results.any { it.slug == "published-post" })
    }

    @Test
    fun `getByAuthor returns empty for unknown author`() = runBlocking {
        val results = repository.getByAuthor("unknown-author")
        assertTrue(results.isEmpty())
    }

    // ── Front matter ──────────────────────────────────────────────────────────

    @Test
    fun `fragment has correct title from front matter`() = runBlocking {
        val fragment = repository.getBySlug("published-post")!!
        assertEquals("Published Post", fragment.title)
    }

    @Test
    fun `fragment has correct template from front matter`() = runBlocking {
        val fragment = repository.getBySlug("published-post")!!
        assertEquals("blog", fragment.template)
    }

    @Test
    fun `fragment has correct tags from front matter`() = runBlocking {
        val fragment = repository.getBySlug("published-post")!!
        assertTrue(fragment.tags.contains("kotlin"))
        assertTrue(fragment.tags.contains("jvm"))
    }

    @Test
    fun `fragment has correct categories from front matter`() = runBlocking {
        val fragment = repository.getBySlug("published-post")!!
        assertTrue(fragment.categories.contains("tech"))
        assertTrue(fragment.categories.contains("kotlin"))
    }

    @Test
    fun `fragment HTML content is rendered from markdown`() = runBlocking {
        val fragment = repository.getBySlug("published-post")!!
        assertTrue(fragment.content.contains("<h1>"), "content should be rendered HTML")
        assertTrue(fragment.content.contains("Published Post"))
    }

    @Test
    fun `fragment preview is extracted up to more tag`() = runBlocking {
        val fragment = repository.getBySlug("published-post")!!
        assertTrue(fragment.preview.contains("published post"), "preview should contain text before more tag")
        assertTrue(!fragment.preview.contains("Full content"), "preview must not include content after more tag")
    }

    @Test
    fun `getAll returns fragments sorted by order`() = runBlocking {
        val all = repository.getAll()
        val withOrder = all.filter { it.slug in listOf("ordered-first", "ordered-last") }
        assertEquals("ordered-first", withOrder.first().slug)
        assertEquals("ordered-last", withOrder.last().slug)
    }

    // ── URL building ──────────────────────────────────────────────────────────

    @Test
    fun `default url uses baseUrl and slug`() = runBlocking {
        val fragment = repository.getBySlug("published-post")!!
        assertEquals("/blog/published-post", fragment.url)
    }

    @Test
    fun `urlBuilder overrides default url`() = runBlocking {
        val repo = ClasspathFragmentRepository(
            basePath = "test-classpath-fragments",
            urlBuilder = { "/custom/${it.slug}" },
        )
        val fragment = repo.getBySlug("published-post")!!
        assertEquals("/custom/published-post", fragment.url)
        assertEquals("/custom/published-post", fragment.resolvedUrl)
    }

    // ── getByYearMonthAndSlug ─────────────────────────────────────────────────

    @Test
    fun `getByYearMonthAndSlug returns fragment matching date and slug`() = runBlocking {
        val fragment = repository.getByYearMonthAndSlug("2024", "3", "published-post")
        assertNotNull(fragment)
        assertEquals("Published Post", fragment!!.title)
    }

    @Test
    fun `getByYearMonthAndSlug returns null for wrong month`() = runBlocking {
        val fragment = repository.getByYearMonthAndSlug("2024", "12", "published-post")
        assertNull(fragment)
    }

    // ── Scheduling ────────────────────────────────────────────────────────────

    @Test
    fun `getScheduledFragmentsDueForPublication returns nothing for far-future threshold`() = runBlocking {
        val due = repository.getScheduledFragmentsDueForPublication(
            java.time.LocalDateTime.now()
        )
        assertTrue(due.isEmpty(), "scheduled-post is in 2099 and should not be due now")
    }

    // ── reload ────────────────────────────────────────────────────────────────

    @Test
    fun `reload re-reads fragments from classpath`() = runBlocking {
        repository.getAll() // prime cache
        repository.reload()
        val all = repository.getAll()
        assertEquals(8, all.size)
    }

    // ── Write operations ──────────────────────────────────────────────────────

    @Test
    fun `updateFragmentStatus returns failure`() = runBlocking<Unit> {
        val result = repository.updateFragmentStatus("published-post", FragmentStatus.ARCHIVED)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun `createRevision returns failure`() = runBlocking {
        val result = repository.createRevision("published-post")
        assertTrue(result.isFailure)
    }

    @Test
    fun `getFragmentRevisions returns empty list`() = runBlocking {
        val revisions = repository.getFragmentRevisions("published-post")
        assertTrue(revisions.isEmpty())
    }

    @Test
    fun `revertToRevision returns failure`() = runBlocking {
        val result = repository.revertToRevision("published-post", "any-revision-id")
        assertTrue(result.isFailure)
    }
}
