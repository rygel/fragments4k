package io.andromeda.fragments.http4k

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.lucene.LuceneSearchEngine
import io.andromeda.fragments.static.StaticPageEngine
import kotlinx.coroutines.runBlocking
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.template.PebbleTemplates
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

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
