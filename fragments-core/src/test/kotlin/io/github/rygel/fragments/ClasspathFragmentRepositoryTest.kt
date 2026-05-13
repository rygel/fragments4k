package io.github.rygel.fragments

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ClasspathFragmentRepositoryTest {
    private lateinit var repository: ClasspathFragmentRepository

    @BeforeEach
    fun setUp() {
        repository =
            ClasspathFragmentRepository(
                basePath = "test-classpath-fragments",
                baseUrl = "/blog",
            )
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    @Test
    fun testLoadsAllFragmentsListedInIndex() =
        runBlocking {
            val all = repository.getAll()
            assertEquals(8, all.size)
        }

    @Test
    fun testReturnsEmptyListWhenIndexFileIsMissing() =
        runBlocking {
            val repo = ClasspathFragmentRepository(basePath = "nonexistent-directory")
            val all = repo.getAll()
            assertTrue(all.isEmpty())
        }

    @Test
    fun testSkipsCommentedAndBlankLinesInIndex() =
        runBlocking {
            // The index for this test has 8 real entries; comments/blanks would cause extras
            val all = repository.getAll()
            assertEquals(8, all.size)
        }

    // ── getAllVisible ─────────────────────────────────────────────────────────

    @Test
    fun testGetAllVisibleReturnsOnlyPublishedNonExpiredFragments() =
        runBlocking {
            val visible = repository.getAllVisible()
            val slugs = visible.map { it.slug }
            assertTrue(slugs.contains("published-post"), "published-post should be visible")
            assertTrue(slugs.contains("another-post"), "another-post should be visible")
            assertTrue(slugs.contains("tagged-post"), "tagged-post should be visible")
        }

    @Test
    fun testGetAllVisibleExcludesDraftFragments() =
        runBlocking {
            val visible = repository.getAllVisible()
            assertTrue(visible.none { it.slug == "draft-post" }, "draft-post must not be visible")
        }

    @Test
    fun testGetAllVisibleExcludesFragmentsWithPastExpiryDate() =
        runBlocking {
            val visible = repository.getAllVisible()
            assertTrue(visible.none { it.slug == "expired-post" }, "expired-post must not be visible")
        }

    @Test
    fun testGetAllVisibleExcludesScheduledFragmentsWithFuturePublishDate() =
        runBlocking {
            val visible = repository.getAllVisible()
            assertTrue(visible.none { it.slug == "scheduled-post" }, "scheduled-post must not be visible")
        }

    @Test
    fun testGetAllVisibleReturnsFragmentsSortedByDateDescending() =
        runBlocking {
            val visible = repository.getAllVisible().filter { it.date != null }
            val dates = visible.map { it.date!! }
            assertEquals(dates.sortedDescending(), dates, "fragments must be sorted newest-first")
        }

    // ── getBySlug ─────────────────────────────────────────────────────────────

    @Test
    fun testGetBySlugReturnsCorrectFragment() =
        runBlocking {
            val fragment = repository.getBySlug("published-post")
            assertNotNull(fragment)
            assertEquals("Published Post", fragment!!.title)
        }

    @Test
    fun testGetBySlugReturnsNullForUnknownSlug() =
        runBlocking {
            val fragment = repository.getBySlug("does-not-exist")
            assertNull(fragment)
        }

    // ── getByTag ──────────────────────────────────────────────────────────────

    @Test
    fun testGetByTagReturnsFragmentsWithMatchingTag() =
        runBlocking {
            val results = repository.getByTag("kotlin")
            val slugs = results.map { it.slug }
            assertTrue(slugs.contains("published-post"))
            assertTrue(slugs.contains("another-post"))
            assertTrue(slugs.none { it == "tagged-post" }, "tagged-post has no kotlin tag")
        }

    @Test
    fun testGetByTagIsCaseInsensitive() =
        runBlocking {
            val lower = repository.getByTag("kotlin")
            val upper = repository.getByTag("KOTLIN")
            assertEquals(lower.map { it.slug }.sorted(), upper.map { it.slug }.sorted())
        }

    @Test
    fun testGetByTagReturnsEmptyListForUnknownTag() =
        runBlocking {
            val results = repository.getByTag("nonexistent-tag")
            assertTrue(results.isEmpty())
        }

    // ── getByCategory ─────────────────────────────────────────────────────────

    @Test
    fun testGetByCategoryReturnsFragmentsInMatchingCategory() =
        runBlocking {
            val results = repository.getByCategory("kotlin")
            val slugs = results.map { it.slug }
            assertTrue(slugs.contains("published-post"))
            assertTrue(slugs.contains("another-post"))
        }

    @Test
    fun testGetByCategoryIsCaseInsensitive() =
        runBlocking {
            val lower = repository.getByCategory("education")
            val upper = repository.getByCategory("EDUCATION")
            assertEquals(lower.map { it.slug }.sorted(), upper.map { it.slug }.sorted())
        }

    // ── getByStatus ───────────────────────────────────────────────────────────

    @Test
    fun testGetByStatusReturnsOnlyFragmentsWithMatchingStatus() =
        runBlocking {
            val drafts = repository.getByStatus(FragmentStatus.DRAFT)
            assertEquals(1, drafts.size)
            assertEquals("draft-post", drafts.first().slug)
        }

    @Test
    fun testGetByStatusReturnsScheduledFragments() =
        runBlocking {
            val scheduled = repository.getByStatus(FragmentStatus.SCHEDULED)
            assertEquals(1, scheduled.size)
            assertEquals("scheduled-post", scheduled.first().slug)
        }

    // ── getByAuthor ───────────────────────────────────────────────────────────

    @Test
    fun testGetByAuthorReturnsFragmentsMatchingAuthorId() =
        runBlocking {
            val results = repository.getByAuthor("alex")
            assertTrue(results.any { it.slug == "published-post" })
        }

    @Test
    fun testGetByAuthorReturnsEmptyForUnknownAuthor() =
        runBlocking {
            val results = repository.getByAuthor("unknown-author")
            assertTrue(results.isEmpty())
        }

    // ── Front matter ──────────────────────────────────────────────────────────

    @Test
    fun testFragmentHasCorrectTitleFromFrontMatter() =
        runBlocking {
            val fragment = repository.getBySlug("published-post")!!
            assertEquals("Published Post", fragment.title)
        }

    @Test
    fun testFragmentHasCorrectTemplateFromFrontMatter() =
        runBlocking {
            val fragment = repository.getBySlug("published-post")!!
            assertEquals("blog", fragment.template)
        }

    @Test
    fun testFragmentHasCorrectTagsFromFrontMatter() =
        runBlocking {
            val fragment = repository.getBySlug("published-post")!!
            assertTrue(fragment.tags.contains("kotlin"))
            assertTrue(fragment.tags.contains("jvm"))
        }

    @Test
    fun testFragmentHasCorrectCategoriesFromFrontMatter() =
        runBlocking {
            val fragment = repository.getBySlug("published-post")!!
            assertTrue(fragment.categories.contains("tech"))
            assertTrue(fragment.categories.contains("kotlin"))
        }

    @Test
    fun testFragmentHtmlContentRenderedFromMarkdown() =
        runBlocking {
            val fragment = repository.getBySlug("published-post")!!
            assertTrue(fragment.htmlContent.contains("<h1>"), "content should be rendered HTML")
            assertTrue(fragment.htmlContent.contains("Published Post"))
        }

    @Test
    fun testFragmentPreviewExtractedUpToMoreTag() =
        runBlocking {
            val fragment = repository.getBySlug("published-post")!!
            assertTrue(fragment.preview.contains("published post"), "preview should contain text before more tag")
            assertTrue(!fragment.preview.contains("Full content"), "preview must not include content after more tag")
        }

    @Test
    fun testGetAllReturnsFragmentsSortedByOrder() =
        runBlocking {
            val all = repository.getAll()
            val withOrder = all.filter { it.slug in listOf("ordered-first", "ordered-last") }
            assertEquals("ordered-first", withOrder.first().slug)
            assertEquals("ordered-last", withOrder.last().slug)
        }

    // ── URL building ──────────────────────────────────────────────────────────

    @Test
    fun testDefaultUrlUsesBaseUrlAndSlug() =
        runBlocking {
            val fragment = repository.getBySlug("published-post")!!
            assertEquals("/blog/published-post", fragment.url)
        }

    @Test
    fun testUrlBuilderOverridesDefaultUrl() =
        runBlocking {
            val repo =
                ClasspathFragmentRepository(
                    basePath = "test-classpath-fragments",
                    urlBuilder = { "/custom/${it.slug}" },
                )
            val fragment = repo.getBySlug("published-post")!!
            assertEquals("/custom/published-post", fragment.url)
            assertEquals("/custom/published-post", fragment.resolvedUrl)
        }

    // ── getByYearMonthAndSlug ─────────────────────────────────────────────────

    @Test
    fun testGetByYearMonthAndSlugReturnsMatchingFragment() =
        runBlocking {
            val fragment = repository.getByYearMonthAndSlug("2024", "3", "published-post")
            assertNotNull(fragment)
            assertEquals("Published Post", fragment!!.title)
        }

    @Test
    fun testGetByYearMonthAndSlugReturnsNullForWrongMonth() =
        runBlocking {
            val fragment = repository.getByYearMonthAndSlug("2024", "12", "published-post")
            assertNull(fragment)
        }

    // ── Scheduling ────────────────────────────────────────────────────────────

    @Test
    fun testGetScheduledFragmentsDueReturnsNothingForFarFutureThreshold() =
        runBlocking {
            val due =
                repository.getScheduledFragmentsDueForPublication(
                    java.time.LocalDateTime.now(),
                )
            assertTrue(due.isEmpty(), "scheduled-post is in 2099 and should not be due now")
        }

    // ── reload ────────────────────────────────────────────────────────────────

    @Test
    fun testReloadReReadsFragmentsFromClasspath() =
        runBlocking {
            repository.getAll() // prime cache
            repository.reload()
            val all = repository.getAll()
            assertEquals(8, all.size)
        }

    // ── Write operations ──────────────────────────────────────────────────────

    @Test
    fun testUpdateFragmentStatusReturnsFailure() =
        runBlocking<Unit> {
            val result = repository.updateFragmentStatus("published-post", FragmentStatus.ARCHIVED)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
        }

    @Test
    fun testCreateRevisionReturnsFailure() =
        runBlocking {
            val result = repository.createRevision("published-post")
            assertTrue(result.isFailure)
        }

    @Test
    fun testGetFragmentRevisionsReturnsEmptyList() =
        runBlocking {
            val revisions = repository.getFragmentRevisions("published-post")
            assertTrue(revisions.isEmpty())
        }

    @Test
    fun testRevertToRevisionReturnsFailure() =
        runBlocking {
            val result = repository.revertToRevision("published-post", "any-revision-id")
            assertTrue(result.isFailure)
        }
}
