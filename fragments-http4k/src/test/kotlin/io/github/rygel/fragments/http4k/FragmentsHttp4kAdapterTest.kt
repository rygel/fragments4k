package io.github.rygel.fragments.http4k

import io.github.rygel.fragments.*
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.static.StaticPageEngine
import kotlinx.coroutines.runBlocking
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Tag("integration")
class FragmentsHttp4kAdapterTest {

    private lateinit var server: Http4kServer
    private lateinit var repo: InMemoryFragmentRepository
    private lateinit var searchEngine: LuceneSearchEngine

    @BeforeEach
    fun setup() {
        repo = InMemoryFragmentRepository()
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        val renderer = StubTemplateRenderer()
        val tempIndexPath = Files.createTempDirectory("lucene-test")
        
        runBlocking {
            searchEngine = LuceneSearchEngine(repo, tempIndexPath)
            searchEngine.index()

            val adapter = FragmentsHttp4kAdapter(
                staticEngine = staticEngine,
                blogEngine = blogEngine,
                renderer = renderer,
                searchEngine = searchEngine,
                siteTitle = "Test Blog",
                siteDescription = "Test Description",
                siteUrl = "http://localhost:8080"
            )

            server = adapter.createRoutes().asServer(SunHttp(0))
            server.start()
        }
    }

    @AfterEach
    fun tearDown() {
        server.stop()
        searchEngine.close()
    }

    class StubTemplateRenderer : (Any) -> String {
        override fun invoke(viewModel: Any): String {
            return "Stub template"
        }
    }

    @Test
    fun homeEndpointReturns200() {
        val response = Request(Method.GET, "http://localhost:${server.port()}/")
            .execute(server)
        assertEquals(Status.OK, response.status)
    }

    @Test
    fun homeEndpointWithFragmentsReturns200() {
        repo.addFragment(createFragment("home-test", "Home Test"))
        
        val response = Request(Method.GET, "http://localhost:${server.port()}/")
            .execute(server)
        assertEquals(Status.OK, response.status)
    }

    @Test
    fun nonExistentPageReturns404() {
        val response = Request(Method.GET, "http://localhost:${server.port()}/page/non-existent")
            .execute(server)
        assertEquals(Status.NOT_FOUND, response.status)
    }

    @Test
    fun existingPageReturns200() {
        repo.addFragment(createFragment("test-page", "Test Page", isBlog = false))
        
        val response = Request(Method.GET, "http://localhost:${server.port()}/page/test-page")
            .execute(server)
        assertEquals(Status.OK, response.status)
    }

    @Test
    fun blogOverviewReturns200() {
        val response = Request(Method.GET, "http://localhost:${server.port()}/blog")
            .execute(server)
        assertEquals(Status.OK, response.status)
    }

    @Test
    fun blogOverviewWithPostsReturns200() {
        repo.addFragment(createFragment("post-1", "Post 1", isBlog = true))
        repo.addFragment(createFragment("post-2", "Post 2", isBlog = true))
        
        val response = Request(Method.GET, "http://localhost:${server.port()}/blog")
            .execute(server)
        assertEquals(Status.OK, response.status)
    }

    @Test
    fun blogPostWithValidPathReturns404WhenMissing() {
        val response = Request(Method.GET, "http://localhost:${server.port()}/blog/2024/01/test-post")
            .execute(server)
        assertEquals(Status.NOT_FOUND, response.status)
    }

    @Test
    fun blogPostWithValidPathReturns200WhenExists() {
        val date = LocalDateTime.of(2024, 1, 15, 10, 0)
        repo.addFragment(createFragment("test-post", "Test Post", isBlog = true, date = date))
        
        val response = Request(Method.GET, "http://localhost:${server.port()}/blog/2024/01/test-post")
            .execute(server)
        assertEquals(Status.OK, response.status)
    }

    @Test
    fun blogPaginationReturns200() {
        val response = Request(Method.GET, "http://localhost:${server.port()}/blog/page/2")
            .execute(server)
        assertEquals(Status.OK, response.status)
    }

    @Test
    fun blogTagReturns200() {
        repo.addFragment(createFragment("tagged-post", "Tagged Post", isBlog = true, tags = listOf("kotlin")))
        
        val response = Request(Method.GET, "http://localhost:${server.port()}/blog/tag/kotlin")
            .execute(server)
        assertEquals(Status.OK, response.status)
    }

    @Test
    fun blogCategoryReturns200() {
        repo.addFragment(createFragment("categorized-post", "Categorized Post", isBlog = true, categories = listOf("tech")))
        
        val response = Request(Method.GET, "http://localhost:${server.port()}/blog/category/tech")
            .execute(server)
        assertEquals(Status.OK, response.status)
    }

    @Test
    fun rssFeedReturns200() {
        val response = Request(Method.GET, "http://localhost:${server.port()}/rss.xml")
            .execute(server)
        assertEquals(Status.OK, response.status)
        assertEquals("application/rss+xml; charset=utf-8", response.header("Content-Type"))
    }

    @Test
    fun sitemapReturns200() {
        val response = Request(Method.GET, "http://localhost:${server.port()}/sitemap.xml")
            .execute(server)
        assertEquals(Status.OK, response.status)
        assertEquals("application/xml; charset=utf-8", response.header("Content-Type"))
    }

    @Test
    fun htmxRequestReturnsPartialRender() {
        repo.addFragment(createFragment("test-page", "Test Page", isBlog = false))
        
        val response = Request(Method.GET, "http://localhost:${server.port()}/page/test-page")
            .header("HX-Request", "true")
            .execute(server)
        assertEquals(Status.OK, response.status)
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

private fun Request.execute(server: Http4kServer): Response {
    return org.http4k.client.OkHttp()(this)
}
