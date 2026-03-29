package io.andromeda.fragments.lucene

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository
import io.andromeda.fragments.FragmentStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LuceneSearchEngineTest {

    private val repository = SimpleTestRepository()
    private lateinit var searchEngine: LuceneSearchEngine

    @BeforeAll
    fun setUp() = runBlocking {
        searchEngine = LuceneSearchEngine(repository)
        createTestFragments().forEach { repository.addFragment(it) }
        searchEngine.index()
    }

    @AfterAll
    fun tearDown() {
        searchEngine.close()
    }

    @Test
    fun standardSearchFindsMatches() = runBlocking {
        val results = searchEngine.search("kotlin")

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.fragment.title.contains("kotlin", ignoreCase = true) })
    }

    @Test
    fun phraseSearchFindsExactMatches() = runBlocking {
        val results = searchEngine.search(
            SearchOptions(query = "kotlin programming", phraseSearch = true)
        )

        assertTrue(results.isNotEmpty())
    }

    @Test
    fun fuzzySearchFindsApproximateMatches() = runBlocking {
        val results = searchEngine.search(
            SearchOptions(
                query = "kotlin programing",
                fuzzySearch = true,
                fuzzyThreshold = 0.7f,
            )
        )

        assertTrue(results.isNotEmpty())
    }

    @Test
    fun autocompleteReturnsSuggestions() = runBlocking {
        val suggestions = searchEngine.autocomplete("kot", limit = 5)

        assertTrue(suggestions.any { it.text.contains("kotlin", ignoreCase = true) })
    }

    @Test
    fun getSuggestionsWorks() = runBlocking {
        val suggestions = searchEngine.getSuggestions("kot", limit = 5)

        assertTrue(suggestions.isNotEmpty())
    }

    private fun createTestFragments(): List<Fragment> = listOf(
        Fragment(
            title = "Kotlin Programming Guide",
            slug = "kotlin-guide",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Learn Kotlin programming",
            content = "Kotlin is a modern programming language for Android development and server-side applications.",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED,
            tags = listOf("kotlin", "programming", "android"),
            categories = listOf("technology"),
        ),
        Fragment(
            title = "Java Tutorial",
            slug = "java-tutorial",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Learn Java",
            content = "Java is a versatile programming language used for enterprise applications.",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED,
            tags = listOf("java", "programming", "enterprise"),
            categories = listOf("technology"),
        ),
        Fragment(
            title = "Python for Beginners",
            slug = "python-beginners",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Python basics",
            content = "Python is an easy-to-learn programming language for data science and web development.",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED,
            tags = listOf("python", "programming", "data-science"),
            categories = listOf("technology"),
        ),
    )

    class SimpleTestRepository : FragmentRepository {
        private val fragments = mutableListOf<Fragment>()

        suspend fun addFragment(fragment: Fragment) {
            fragments.add(fragment)
        }

        override suspend fun getAll(): List<Fragment> = fragments.toList()
        override suspend fun getAllVisible(): List<Fragment> = fragments.filter { it.status == FragmentStatus.PUBLISHED }
        override suspend fun getBySlug(slug: String): Fragment? = fragments.find { it.slug == slug }
        override suspend fun getByYearMonthAndSlug(year: String, month: String, slug: String): Fragment? = null
        override suspend fun getByTag(tag: String): List<Fragment> = fragments.filter { it.tags.contains(tag) }
        override suspend fun getByCategory(category: String): List<Fragment> = fragments.filter { it.categories.contains(category) }
        override suspend fun getByStatus(status: FragmentStatus): List<Fragment> = fragments.filter { it.status == status }
        override suspend fun getByAuthor(authorId: String): List<Fragment> = fragments.filter { it.author == authorId || it.authorIds.contains(authorId) }
        override suspend fun getByAuthors(authorIds: List<String>): List<Fragment> = fragments.filter { fragment -> authorIds.any { fragment.author == it || fragment.authorIds.contains(it) } }
        override suspend fun updateFragmentStatus(slug: String, status: FragmentStatus, force: Boolean, changedBy: String?, reason: String?): Result<Fragment> {
            val f = fragments.find { it.slug == slug }
            return if (f != null) Result.success(f) else Result.failure(IllegalArgumentException("Fragment not found"))
        }
        override suspend fun updateMultipleFragmentsStatus(slugs: List<String>, status: FragmentStatus, force: Boolean, changedBy: String?, reason: String?): List<Result<Fragment>> = slugs.map { updateFragmentStatus(it, status, force, changedBy, reason) }
        override suspend fun publishMultiple(slugs: List<String>, changedBy: String?, reason: String?): List<Result<Fragment>> = slugs.map { updateFragmentStatus(it, FragmentStatus.PUBLISHED, false, changedBy, reason) }
        override suspend fun unpublishMultiple(slugs: List<String>, changedBy: String?, reason: String?): List<Result<Fragment>> = slugs.map { updateFragmentStatus(it, FragmentStatus.DRAFT, false, changedBy, reason) }
        override suspend fun archiveMultiple(slugs: List<String>, changedBy: String?, reason: String?): List<Result<Fragment>> = slugs.map { updateFragmentStatus(it, FragmentStatus.ARCHIVED, false, changedBy, reason) }
        override suspend fun getScheduledFragmentsDueForPublication(threshold: LocalDateTime): List<Fragment> = emptyList()
        override suspend fun publishScheduledFragments(threshold: LocalDateTime): List<Result<Fragment>> = emptyList()
        override suspend fun scheduleMultiple(slugs: List<String>, publishDate: LocalDateTime, changedBy: String?, reason: String?): List<Result<Fragment>> = emptyList()
        override suspend fun expireFragments(threshold: LocalDateTime): List<Result<Fragment>> = emptyList()
        override suspend fun getFragmentsExpiringSoon(threshold: LocalDateTime): List<Fragment> = emptyList()
        override suspend fun reload() {}
        override suspend fun getRelationships(slug: String, config: io.andromeda.fragments.RelationshipConfig): io.andromeda.fragments.ContentRelationships? = null
        override suspend fun createRevision(slug: String, changedBy: String?, reason: String?): Result<io.andromeda.fragments.FragmentRevision> = Result.failure(UnsupportedOperationException())
        override suspend fun getFragmentRevisions(slug: String): List<io.andromeda.fragments.FragmentRevision> = emptyList()
        override suspend fun revertToRevision(slug: String, revisionId: String, changedBy: String?, reason: String?): Result<Fragment> = Result.failure(UnsupportedOperationException())
    }
}
