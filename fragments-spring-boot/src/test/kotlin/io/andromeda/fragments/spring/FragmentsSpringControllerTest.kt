package io.andromeda.fragments.spring

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(FragmentsSpringController::class)
@Import(TestConfig::class)
class FragmentsSpringControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `home endpoint returns 200`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
    }

    @Test
    fun `non-existent page returns 404`() {
        mockMvc.perform(get("/page/non-existent"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `blog overview returns 200`() {
        mockMvc.perform(get("/blog"))
            .andExpect(status().isOk)
    }

    @Test
    fun `blog post with valid path returns 404 when not found`() {
        mockMvc.perform(get("/blog/2024/01/test-post"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `blog overview with pagination returns 200`() {
        mockMvc.perform(get("/blog?page=1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `blog tag route returns 200`() {
        mockMvc.perform(get("/blog/tag/kotlin"))
            .andExpect(status().isOk)
    }

    @Test
    fun `blog category route returns 200`() {
        mockMvc.perform(get("/blog/category/programming"))
            .andExpect(status().isOk)
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun fragmentRepository(): FragmentRepository = InMemoryFragmentRepository()

        @Bean
        fun staticPageEngine(repository: FragmentRepository): StaticPageEngine = 
            StaticPageEngine(repository)

        @Bean
        fun blogEngine(repository: FragmentRepository): BlogEngine = 
            BlogEngine(repository)

        @Bean
        fun fragmentsController(staticEngine: StaticPageEngine, blogEngine: BlogEngine): FragmentsSpringController =
            FragmentsSpringController(staticEngine, blogEngine)
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
