package io.andromeda.fragments.javalin

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.lucene.LuceneSearchEngine
import io.andromeda.fragments.static.StaticPageEngine
import io.javalin.testtools.JavalinTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.LocalDateTime

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

    @Test
    fun homeEndpointReturns200() = JavalinTest.test { app, client ->
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/")
        assertEquals(200, response.code)
    }

    @Test
    fun homeEndpointWithFragmentsReturns200() = JavalinTest.test { app, client ->
        repo.addFragment(createFragment("home-test", "Home Test"))
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/")
        assertEquals(200, response.code)
    }

    @Test
    fun nonExistentPageReturns404() = JavalinTest.test { app, client ->
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/page/non-existent")
        assertEquals(404, response.code)
    }

    @Test
    fun existingPageReturns200() = JavalinTest.test { app, client ->
        repo.addFragment(createFragment("test-page", "Test Page", isBlog = false))
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/page/test-page")
        assertEquals(200, response.code)
    }

    @Test
    fun blogOverviewReturns200() = JavalinTest.test { app, client ->
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/blog")
        assertEquals(200, response.code)
    }

    @Test
    fun blogOverviewWithPostsReturns200() = JavalinTest.test { app, client ->
        repo.addFragment(createFragment("post-1", "Post 1", isBlog = true))
        repo.addFragment(createFragment("post-2", "Post 2", isBlog = true))
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/blog")
        assertEquals(200, response.code)
    }

    @Test
    fun blogPostWithValidPathReturns404WhenMissing() = JavalinTest.test { app, client ->
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/blog/2024/01/test-post")
        assertEquals(404, response.code)
    }

    @Test
    fun blogPostWithValidPathReturns200WhenExists() = JavalinTest.test { app, client ->
        val date = LocalDateTime.of(2024, 1, 15, 10, 0)
        repo.addFragment(createFragment("test-post", "Test Post", isBlog = true, date = date))
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/blog/2024/01/test-post")
        assertEquals(200, response.code)
    }

    @Test
    fun blogPaginationReturns200() = JavalinTest.test { app, client ->
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/blog/page/2")
        assertEquals(200, response.code)
    }

    @Test
    fun blogTagReturns200() = JavalinTest.test { app, client ->
        repo.addFragment(createFragment("tagged-post", "Tagged Post", isBlog = true, tags = listOf("kotlin")))
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/blog/tag/kotlin")
        assertEquals(200, response.code)
    }

    @Test
    fun blogCategoryReturns200() = JavalinTest.test { app, client ->
        repo.addFragment(createFragment("categorized-post", "Categorized Post", isBlog = true, categories = listOf("tech")))
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/blog/category/tech")
        assertEquals(200, response.code)
    }

    @Test
    fun rssFeedReturns200() = JavalinTest.test { app, client ->
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/rss.xml")
        assertEquals(200, response.code)
        assertEquals("application/rss+xml", response.header("Content-Type"))
    }

    @Test
    fun sitemapReturns200() = JavalinTest.test { app, client ->
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        app.fragmentsRoutes(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = MockTemplateRenderer(),
            searchEngine = searchEngine
        )

        val response = client.get("/sitemap.xml")
        assertEquals(200, response.code)
        assertEquals("application/xml", response.header("Content-Type"))
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
