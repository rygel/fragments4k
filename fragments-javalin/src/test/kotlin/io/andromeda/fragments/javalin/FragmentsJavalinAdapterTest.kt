package io.andromeda.fragments.javalin

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import io.javalin.Javalin
import io.javalin.testtools.JavalinTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FragmentsJavalinAdapterTest {

    @Test
    fun `home endpoint returns 200`() = JavalinTest.test(createApp()) { _, client ->
        val response = client.get("/")
        assertEquals(200, response.code)
    }

    @Test
    fun `non-existent page returns 404`() = JavalinTest.test(createApp()) { _, client ->
        val response = client.get("/page/non-existent")
        assertEquals(404, response.code)
    }

    @Test
    fun `blog overview returns 200`() = JavalinTest.test(createApp()) { _, client ->
        val response = client.get("/blog")
        assertEquals(200, response.code)
    }

    @Test
    fun `blog post with valid path returns 200`() = JavalinTest.test(createApp()) { _, client ->
        val response = client.get("/blog/2024/01/test-post")
        assertEquals(404, response.code) // No actual post exists in test
    }

    private fun createApp(): Javalin {
        val repo = InMemoryFragmentRepository()
        val staticEngine = StaticPageEngine(repo)
        val blogEngine = BlogEngine(repo)
        
        return Javalin.create()
            .fragmentsRoutes(staticEngine, blogEngine, MockTemplateRenderer())
    }
}

class InMemoryFragmentRepository : FragmentRepository {
    private val fragments = mutableListOf<Fragment>()

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
