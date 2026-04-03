package io.github.rygel.fragments.javalin

import io.github.rygel.fragments.*
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.static.StaticPageEngine
import io.javalin.Javalin
import io.javalin.testtools.JavalinTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.LocalDateTime

@Tag("integration")
class FragmentsJavalinAdapterTest {

    private lateinit var repo: InMemoryFragmentRepository
    private lateinit var searchEngine: LuceneSearchEngine

    @BeforeEach
    fun setup() = runBlocking {
        repo = InMemoryFragmentRepository()
        val tempIndexPath = Files.createTempDirectory("lucene-test")
        searchEngine = LuceneSearchEngine(repo, tempIndexPath)
        searchEngine.index()
    }

    private fun createTestApp(staticEngine: StaticPageEngine, blogEngine: BlogEngine): Javalin =
        Javalin.create { config ->
            config.routes.fragmentsRoutes(
                staticEngine = staticEngine,
                blogEngine = blogEngine,
                renderer = MockTemplateRenderer(),
                searchEngine = searchEngine
            )
        }

    @Test
    fun homeEndpointReturns200() {
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            assertEquals(200, client.get("/").code)
        }
    }

    @Test
    fun homeEndpointWithFragmentsReturns200() {
        repo.addFragment(createFragment("home-test", "Home Test"))
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            assertEquals(200, client.get("/").code)
        }
    }

    @Test
    fun nonExistentPageReturns404() {
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            assertEquals(404, client.get("/page/non-existent").code)
        }
    }

    @Test
    fun existingPageReturns200() {
        repo.addFragment(createFragment("test-page", "Test Page", isBlog = false))
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            assertEquals(200, client.get("/page/test-page").code)
        }
    }

    @Test
    fun blogOverviewReturns200() {
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            assertEquals(200, client.get("/blog").code)
        }
    }

    @Test
    fun blogOverviewWithPostsReturns200() {
        repo.addFragment(createFragment("post-1", "Post 1", isBlog = true))
        repo.addFragment(createFragment("post-2", "Post 2", isBlog = true))
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            assertEquals(200, client.get("/blog").code)
        }
    }

    @Test
    fun blogPostWithValidPathReturns404WhenMissing() {
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            assertEquals(404, client.get("/blog/2024/01/test-post").code)
        }
    }

    @Test
    fun blogPostWithValidPathReturns200WhenExists() {
        val date = LocalDateTime.of(2024, 1, 15, 10, 0)
        repo.addFragment(createFragment("test-post", "Test Post", isBlog = true, date = date))
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            assertEquals(200, client.get("/blog/2024/01/test-post").code)
        }
    }

    @Test
    fun blogPaginationReturns200() {
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            assertEquals(200, client.get("/blog/page/2").code)
        }
    }

    @Test
    fun blogTagReturns200() {
        repo.addFragment(createFragment("tagged-post", "Tagged Post", isBlog = true, tags = listOf("kotlin")))
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            assertEquals(200, client.get("/blog/tag/kotlin").code)
        }
    }

    @Test
    fun blogCategoryReturns200() {
        repo.addFragment(createFragment("categorized-post", "Categorized Post", isBlog = true, categories = listOf("tech")))
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            assertEquals(200, client.get("/blog/category/tech").code)
        }
    }

    @Test
    fun rssFeedReturns200() {
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            val response = client.get("/rss.xml")
            assertEquals(200, response.code)
            assertEquals("application/rss+xml", response.headers().get("Content-Type")?.first())
        }
    }

    @Test
    fun sitemapReturns200() {
        val app = createTestApp(StaticPageEngine(repo), BlogEngine(repo))
        JavalinTest.test(app) { _, client ->
            val response = client.get("/sitemap.xml")
            assertEquals(200, response.code)
            assertEquals("application/xml", response.headers().get("Content-Type")?.first())
        }
    }
}

class InMemoryFragmentRepository : FragmentRepository {
    private val fragments = mutableListOf<Fragment>()

    fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
    }

    override suspend fun getAll(): List<Fragment> = fragments
    override suspend fun getAllVisible(): List<Fragment> = fragments.filter { it.visible }
    override suspend fun getBySlug(slug: String): Fragment? = fragments.find { it.slug == slug }
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
    override suspend fun reload() {}
    override suspend fun getByStatus(status: io.github.rygel.fragments.FragmentStatus): List<Fragment> = fragments.filter { it.status == status }
    override suspend fun getByAuthor(authorId: String): List<Fragment> = fragments.filter { it.author == authorId || it.authorIds.contains(authorId) }
    override suspend fun getByAuthors(authorIds: List<String>): List<Fragment> = fragments.filter { fragment -> authorIds.any { fragment.author == it || fragment.authorIds.contains(it) } }
    override suspend fun updateFragmentStatus(slug: String, status: io.github.rygel.fragments.FragmentStatus, force: Boolean, changedBy: String?, reason: String?): Result<Fragment> {
        val f = fragments.find { it.slug == slug }
        return if (f != null) Result.success(f) else Result.failure(IllegalArgumentException("Fragment not found"))
    }
    override suspend fun updateMultipleFragmentsStatus(slugs: List<String>, status: io.github.rygel.fragments.FragmentStatus, force: Boolean, changedBy: String?, reason: String?): List<Result<Fragment>> = slugs.map { updateFragmentStatus(it, status, force, changedBy, reason) }
    override suspend fun publishMultiple(slugs: List<String>, changedBy: String?, reason: String?): List<Result<Fragment>> = slugs.map { updateFragmentStatus(it, io.github.rygel.fragments.FragmentStatus.PUBLISHED, false, changedBy, reason) }
    override suspend fun unpublishMultiple(slugs: List<String>, changedBy: String?, reason: String?): List<Result<Fragment>> = slugs.map { updateFragmentStatus(it, io.github.rygel.fragments.FragmentStatus.DRAFT, false, changedBy, reason) }
    override suspend fun archiveMultiple(slugs: List<String>, changedBy: String?, reason: String?): List<Result<Fragment>> = slugs.map { updateFragmentStatus(it, io.github.rygel.fragments.FragmentStatus.ARCHIVED, false, changedBy, reason) }
    override suspend fun getScheduledFragmentsDueForPublication(threshold: LocalDateTime): List<Fragment> = emptyList()
    override suspend fun publishScheduledFragments(threshold: LocalDateTime): List<Result<Fragment>> = emptyList()
    override suspend fun scheduleMultiple(slugs: List<String>, publishDate: LocalDateTime, changedBy: String?, reason: String?): List<Result<Fragment>> = emptyList()
    override suspend fun expireFragments(threshold: LocalDateTime): List<Result<Fragment>> = emptyList()
    override suspend fun getFragmentsExpiringSoon(threshold: LocalDateTime): List<Fragment> = emptyList()
    override suspend fun getRelationships(slug: String, config: io.github.rygel.fragments.RelationshipConfig): io.github.rygel.fragments.ContentRelationships? = null
    override suspend fun createRevision(slug: String, changedBy: String?, reason: String?): Result<io.github.rygel.fragments.FragmentRevision> = Result.failure(UnsupportedOperationException())
    override suspend fun getFragmentRevisions(slug: String): List<io.github.rygel.fragments.FragmentRevision> = emptyList()
    override suspend fun revertToRevision(slug: String, revisionId: String, changedBy: String?, reason: String?): Result<Fragment> = Result.failure(UnsupportedOperationException())
}

class MockTemplateRenderer : TemplateRenderer {
    override fun render(template: String, viewModel: Any): String {
        return "<html><body>Mock rendered content for $template</body></html>"
    }
}

private fun createFragment(
    slug: String,
    title: String,
    isBlog: Boolean = false,
    date: LocalDateTime? = null,
    tags: List<String> = emptyList(),
    categories: List<String> = emptyList()
): Fragment {
    return Fragment(
        slug = slug,
        title = title,
        content = "# Test Content\n\nThis is test content.",
        date = date ?: if (isBlog) LocalDateTime.now() else null,
        publishDate = date ?: if (isBlog) LocalDateTime.now() else null,
        preview = "This is test content.",
        template = if (isBlog) "blog_post" else "page",
        visible = true,
        tags = tags,
        categories = categories,
        frontMatter = mutableMapOf(
            "title" to title,
            "slug" to slug
        )
    )
}
