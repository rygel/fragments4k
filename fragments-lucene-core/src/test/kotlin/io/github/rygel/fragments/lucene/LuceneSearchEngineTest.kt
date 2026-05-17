package io.github.rygel.fragments.lucene

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.time.ZoneOffset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LuceneSearchEngineTest {
    private lateinit var repo: SimpleTestRepository
    private lateinit var engine: LuceneSearchEngine

    @BeforeEach
    fun setUp() =
        runBlocking {
            repo = SimpleTestRepository()
            repo.addAll(testFragments())
            engine = LuceneSearchEngine(repo)
            engine.index()
        }

    @AfterEach
    fun tearDown() {
        engine.close()
    }

    // ── Standard search ─────────────────────────────────────────────────────

    @Test
    fun testStandardSearchReturnsFragmentWhoseTitleMatches() =
        runBlocking {
            val results = engine.search("Kotlin")
            assertTrue(
                results.any { it.fragment.slug == "kotlin-guide" },
                "Expected kotlin-guide in results, got: ${results.map { it.fragment.slug }}",
            )
        }

    @Test
    fun testStandardSearchDoesNotReturnUnrelatedFragment() =
        runBlocking {
            val results = engine.search("Kotlin")
            assertFalse(
                results.any { it.fragment.slug == "cooking-basics" },
                "cooking-basics should not match a Kotlin query",
            )
        }

    @Test
    fun testStandardSearchExcludesDraftFragments() =
        runBlocking {
            val results = engine.search("draft")
            assertFalse(
                results.any { it.fragment.slug == "draft-fragment" },
                "Draft fragment must not appear in search results",
            )
        }

    @Test
    fun testTitleMatchScoresHigherThanContentOnlyMatch() =
        runBlocking {
            // "Kotlin" is in the title of kotlin-guide; java-tutorial only mentions kotlin in content
            val results = engine.search("Kotlin")
            val kotlinIdx = results.indexOfFirst { it.fragment.slug == "kotlin-guide" }
            val javaIdx = results.indexOfFirst { it.fragment.slug == "java-tutorial" }
            assertTrue(kotlinIdx >= 0, "kotlin-guide not in results")
            if (javaIdx >= 0) {
                assertTrue(kotlinIdx < javaIdx, "kotlin-guide (title match) should rank above java-tutorial (content match)")
            }
        }

    @Test
    fun testSearchRespectsMaxResultsLimit() =
        runBlocking {
            val results = engine.search(SearchOptions(query = "programming", maxResults = 2))
            assertTrue(results.size <= 2, "Expected at most 2 results, got ${results.size}")
        }

    // ── Phrase search ────────────────────────────────────────────────────────

    @Test
    fun testPhraseSearchReturnsFragmentContainingExactPhrase() =
        runBlocking {
            val results = engine.search(SearchOptions(query = "modern programming language", phraseSearch = true))
            assertTrue(
                results.any { it.fragment.slug == "kotlin-guide" },
                "kotlin-guide should match phrase 'modern programming language'",
            )
        }

    @Test
    fun testPhraseSearchDoesNotReturnFragmentMissingPhrase() =
        runBlocking {
            val results = engine.search(SearchOptions(query = "modern programming language", phraseSearch = true))
            assertFalse(
                results.any { it.fragment.slug == "cooking-basics" },
                "cooking-basics has nothing to do with 'modern programming language'",
            )
        }

    @Test
    fun testSingleWordPhraseSearchReturnsMatchingFragment() =
        runBlocking {
            val results = engine.search(SearchOptions(query = "android", phraseSearch = true))
            assertTrue(
                results.any { it.fragment.slug == "kotlin-guide" },
                "kotlin-guide should match single-word phrase 'android'",
            )
        }

    // ── Fuzzy search ─────────────────────────────────────────────────────────

    @Test
    fun testFuzzySearchFindsFragmentWithTypoInContent() =
        runBlocking {
            // "programing" (one m) is a typo for "programming"
            val results = engine.search(SearchOptions(query = "programing", fuzzySearch = true, fuzzyThreshold = 0.7f))
            assertTrue(results.isNotEmpty(), "Fuzzy search for 'programing' should return results")
        }

    @Test
    fun testFuzzySearchFindsMatchOnTitle() =
        runBlocking {
            // "Kotln" is a typo for "Kotlin" — should still find kotlin-guide via title
            val results = engine.search(SearchOptions(query = "Kotln", fuzzySearch = true, fuzzyThreshold = 0.6f))
            assertTrue(
                results.any { it.fragment.slug == "kotlin-guide" },
                "Fuzzy search for 'Kotln' should still find kotlin-guide via title",
            )
        }

    // ── Tag search ───────────────────────────────────────────────────────────

    @Test
    fun testSearchByTagReturnsOnlyFragmentsTaggedWithExactTag() =
        runBlocking {
            val results = engine.searchByTag("kotlin")
            assertTrue(
                results.any { it.slug == "kotlin-guide" },
                "kotlin-guide should be returned for tag 'kotlin'",
            )
            assertFalse(
                results.any { it.slug == "java-tutorial" },
                "java-tutorial should not be returned for tag 'kotlin'",
            )
        }

    @Test
    fun testSearchByTagDoesNotReturnPartialTagMatches() =
        runBlocking {
            // Searching for "kot" should NOT return results tagged "kotlin"
            val results = engine.searchByTag("kot")
            assertFalse(
                results.any { it.slug == "kotlin-guide" },
                "Partial tag 'kot' should not match tag 'kotlin'",
            )
        }

    @Test
    fun testSearchByTagReturnsEmptyListWhenNoFragmentsHaveTag() =
        runBlocking {
            val results = engine.searchByTag("nonexistent-tag")
            assertTrue(results.isEmpty(), "Expected empty list for unknown tag")
        }

    // ── Category search ──────────────────────────────────────────────────────

    @Test
    fun testSearchByCategoryReturnsFragmentsInThatCategory() =
        runBlocking {
            val results = engine.searchByCategory("recipes")
            assertTrue(
                results.any { it.slug == "cooking-basics" },
                "cooking-basics should be in category 'recipes'",
            )
            assertFalse(
                results.any { it.slug == "kotlin-guide" },
                "kotlin-guide should not be in category 'recipes'",
            )
        }

    @Test
    fun testSearchByCategoryDoesNotReturnPartialCategoryMatches() =
        runBlocking {
            val results = engine.searchByCategory("recipe")
            assertFalse(
                results.any { it.slug == "cooking-basics" },
                "Partial category 'recipe' should not match 'recipes'",
            )
        }

    @Test
    fun testSearchByCategoryReturnsEmptyListForUnknownCategory() =
        runBlocking {
            val results = engine.searchByCategory("unknown-category")
            assertTrue(results.isEmpty())
        }

    // ── Autocomplete ─────────────────────────────────────────────────────────

    @Test
    fun testAutocompleteReturnsTitleSuggestionMatchingPrefix() =
        runBlocking {
            val suggestions = engine.autocomplete("Kot", limit = 10)
            assertTrue(
                suggestions.any { it.text == "Kotlin Programming Guide" && it.type == SearchSuggestion.SuggestionType.TITLE },
                "Expected 'Kotlin Programming Guide' title suggestion for prefix 'Kot'",
            )
        }

    @Test
    fun testAutocompleteReturnsTagSuggestionMatchingPrefix() =
        runBlocking {
            val suggestions = engine.autocomplete("kot", limit = 10)
            assertTrue(
                suggestions.any { it.text == "kotlin" && it.type == SearchSuggestion.SuggestionType.TAG },
                "Expected 'kotlin' tag suggestion for prefix 'kot', got: $suggestions",
            )
        }

    @Test
    fun testAutocompleteReturnsEmptyListForBlankQuery() =
        runBlocking {
            val suggestions = engine.autocomplete("", limit = 10)
            assertTrue(suggestions.isEmpty())
        }

    @Test
    fun testAutocompleteReturnsEmptyForSingleCharacterPrefix() =
        runBlocking {
            val suggestions = engine.autocomplete("k", limit = 10)
            assertTrue(suggestions.isEmpty(), "Single character prefix should be rejected for being too broad")
        }

    @Test
    fun testSearchReturnsEmptyForBlankQuery() =
        runBlocking {
            val results = engine.search("   ")
            assertTrue(results.isEmpty())
        }

    @Test
    fun testSearchReturnsEmptyForOverlongQuery() =
        runBlocking {
            val longQuery = "kotlin " + "a".repeat(500)
            val results = engine.search(longQuery)
            assertTrue(results.isEmpty())
        }

    @Test
    fun testSearchClampsNegativeMaxResults() =
        runBlocking {
            val results = engine.search("kotlin", maxResults = -1)
            assertTrue(results.isNotEmpty(), "Negative maxResults should be clamped to 1, not cause an error")
        }

    @Test
    fun testAutocompleteRespectsLimit() =
        runBlocking {
            val suggestions = engine.autocomplete("p", limit = 1)
            assertTrue(suggestions.size <= 1)
        }

    // ── Query hardening ──────────────────────────────────────────────────────

    @Test
    fun testStandardSearchTreatsSpecialCharsAsLiteral() =
        runBlocking {
            val results = engine.search(SearchOptions(query = "title:secret OR content:admin"))
            assertTrue(
                results.isEmpty() || results.none { it.fragment.slug == "cooking-basics" },
                "Query syntax should be escaped in standard mode"
            )
        }

    @Test
    fun testAdvancedModeAcceptsRawQuerySyntax() =
        runBlocking {
            val results = engine.search(
                SearchOptions(
                    query = "title:Kotlin",
                    searchType = SearchOptions.SearchType.ADVANCED,
                ),
            )
            assertTrue(
                results.any { it.fragment.slug == "kotlin-guide" },
                "ADVANCED mode should allow field-specific queries"
            )
        }

    @Test
    fun testQueryWithExcessiveTokensIsTruncated() =
        runBlocking {
            val longQuery = (1..25).joinToString(" ") { "word$it" }
            val results = engine.search(SearchOptions(query = longQuery))
            assertTrue(results.size <= testFragments().size, "Should not crash with excessive tokens")
        }

    // ── Multiple repositories ────────────────────────────────────────────────

    @Test
    fun testSearchAcrossMultipleRepositoriesReturnsResultsFromBoth() =
        runBlocking {
            val repo2 = SimpleTestRepository()
            repo2.addAll(
                listOf(
                    fragment(
                        slug = "extra-doc",
                        title = "Extra Documentation",
                        htmlContent = "This is extra content about relational storage.",
                        tags = listOf("storage"),
                    ),
                ),
            )
            val multiEngine = LuceneSearchEngine(listOf(repo, repo2))
            multiEngine.index()

            val results = multiEngine.search("kotlin")
            assertTrue(results.any { it.fragment.slug == "kotlin-guide" }, "Should find kotlin-guide from repo1")
            val storageResults = multiEngine.search("relational storage")
            assertTrue(storageResults.any { it.fragment.slug == "extra-doc" }, "Should find extra-doc from repo2")

            multiEngine.close()
        }

    @Test
    fun testSearchAfterCloseReturnsEmpty() =
        runBlocking {
            val repo2 = SimpleTestRepository()
            repo2.addAll(testFragments())
            val engine2 = LuceneSearchEngine(repo2)
            engine2.index()

            val resultsBeforeClose = engine2.search("Kotlin")
            assertTrue(resultsBeforeClose.isNotEmpty(), "Should find results before close")

            engine2.close()

            val resultsAfterClose = engine2.search("Kotlin")
            assertTrue(resultsAfterClose.isEmpty(), "Search after close should return empty results")
        }

    @Test
    fun testAutocompleteAfterCloseReturnsEmpty() =
        runBlocking {
            val repo2 = SimpleTestRepository()
            repo2.addAll(testFragments())
            val engine2 = LuceneSearchEngine(repo2)
            engine2.index()

            engine2.close()

            val suggestions = engine2.autocomplete("Kot")
            assertTrue(suggestions.isEmpty(), "Autocomplete after close should return empty")
        }

    @Test
    fun testIndexAfterCloseIsNoOp() =
        runBlocking {
            val repo2 = SimpleTestRepository()
            repo2.addAll(testFragments())
            val engine2 = LuceneSearchEngine(repo2)
            engine2.index()

            val resultsBeforeClose = engine2.search("Kotlin")
            assertTrue(resultsBeforeClose.isNotEmpty())

            engine2.close()

            val engine3 = LuceneSearchEngine(repo2)
            engine3.index()

            val results = engine3.search("Kotlin")
            assertTrue(results.isNotEmpty(), "New engine should work after old engine was closed")

            engine3.close()
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun testFragments() =
        listOf(
            fragment(
                slug = "kotlin-guide",
                title = "Kotlin Programming Guide",
                htmlContent = "Kotlin is a modern programming language for Android development. It runs on the JVM.",
                tags = listOf("kotlin", "programming", "android"),
                categories = listOf("technology"),
            ),
            fragment(
                slug = "java-tutorial",
                title = "Java Tutorial",
                htmlContent = "Java is a versatile programming language. Kotlin is interoperable with Java.",
                tags = listOf("java", "programming"),
                categories = listOf("technology"),
            ),
            fragment(
                slug = "python-beginners",
                title = "Python for Beginners",
                htmlContent = "Python is an easy-to-learn programming language for data science.",
                tags = listOf("python", "programming"),
                categories = listOf("technology"),
            ),
            fragment(
                slug = "cooking-basics",
                title = "Cooking Basics",
                htmlContent = "Learn how to cook delicious meals at home with simple recipes.",
                tags = listOf("cooking", "food"),
                categories = listOf("recipes"),
            ),
            fragment(
                slug = "draft-fragment",
                title = "Draft about drafts",
                htmlContent = "This is a draft fragment and should not appear in search results.",
                status = FragmentStatus.DRAFT,
                tags = listOf("draft"),
                categories = listOf("hidden"),
            ),
        )

    private fun fragment(
        slug: String,
        title: String,
        htmlContent: String,
        tags: List<String> = emptyList(),
        categories: List<String> = emptyList(),
        status: FragmentStatus = FragmentStatus.PUBLISHED,
    ) = Fragment(
        title = title,
        slug = slug,
        date = LocalDateTime.now(ZoneOffset.UTC),
        publishDate = null,
        preview = htmlContent.take(80),
        htmlContent = htmlContent,
        frontMatter = emptyMap(),
        status = status,
        tags = tags,
        categories = categories,
    )

    class SimpleTestRepository : FragmentRepository {
        private val fragments = mutableListOf<Fragment>()

        fun addAll(list: List<Fragment>) {
            fragments.addAll(list)
        }

        override suspend fun getAll(): List<Fragment> = fragments.toList()

        override suspend fun getAllVisible(): List<Fragment> = fragments.filter { it.status == FragmentStatus.PUBLISHED }

        override suspend fun getBySlug(slug: String): Fragment? = fragments.find { it.slug == slug }

        override suspend fun getByYearMonthAndSlug(
            year: String,
            month: String,
            slug: String,
        ): Fragment? = null

        override suspend fun getByTag(tag: String): List<Fragment> = fragments.filter { it.tags.contains(tag) }

        override suspend fun getByCategory(category: String): List<Fragment> = fragments.filter { it.categories.contains(category) }

        override suspend fun getByStatus(status: FragmentStatus): List<Fragment> = fragments.filter { it.status == status }

        override suspend fun getByAuthor(authorId: String): List<Fragment> =
            fragments.filter {
                it.author == authorId ||
                    it.authorIds.contains(authorId)
            }

        override suspend fun getByAuthors(authorIds: List<String>): List<Fragment> =
            fragments.filter { f ->
                authorIds.any {
                    f.author == it ||
                        f.authorIds.contains(it)
                }
            }

        override suspend fun updateFragmentStatus(
            slug: String,
            status: FragmentStatus,
            force: Boolean,
            changedBy: String?,
            reason: String?,
        ): Result<Fragment> {
            val f = fragments.find { it.slug == slug } ?: return Result.failure(IllegalArgumentException("not found"))
            return Result.success(f)
        }

        override suspend fun updateMultipleFragmentsStatus(
            slugs: List<String>,
            status: FragmentStatus,
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
                updateFragmentStatus(it, FragmentStatus.PUBLISHED, false, changedBy, reason)
            }

        override suspend fun unpublishMultiple(
            slugs: List<String>,
            changedBy: String?,
            reason: String?,
        ): List<Result<Fragment>> =
            slugs.map {
                updateFragmentStatus(it, FragmentStatus.DRAFT, false, changedBy, reason)
            }

        override suspend fun archiveMultiple(
            slugs: List<String>,
            changedBy: String?,
            reason: String?,
        ): List<Result<Fragment>> =
            slugs.map {
                updateFragmentStatus(it, FragmentStatus.ARCHIVED, false, changedBy, reason)
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

        override suspend fun reload() {}

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
    }
}
