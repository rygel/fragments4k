package io.andromeda.fragments.spring

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class FragmentsSpringControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun homeEndpointReturns200() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
    }

    @Test
    fun nonExistentPageReturns404() {
        mockMvc.perform(get("/page/non-existent"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun blogOverviewReturns200() {
        mockMvc.perform(get("/blog"))
            .andExpect(status().isOk)
    }

    @Test
    fun blogPostWithValidPathReturns404WhenNotFound() {
        mockMvc.perform(get("/blog/2024/01/test-post"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun blogOverviewWithPaginationReturns200() {
        mockMvc.perform(get("/blog?page=1"))
            .andExpect(status().isOk)
    }

    @Test
    fun blogTagRouteReturns200() {
        mockMvc.perform(get("/blog/tag/kotlin"))
            .andExpect(status().isOk)
    }

    @Test
    fun blogCategoryRouteReturns200() {
        mockMvc.perform(get("/blog/category/programming"))
            .andExpect(status().isOk)
    }
}

@SpringBootApplication
open class TestApplication {
    @Bean
    open fun fragmentRepository(): FragmentRepository = InMemoryFragmentRepository()

    @Bean
    open fun staticPageEngine(repository: FragmentRepository): StaticPageEngine =
        StaticPageEngine(repository)

    @Bean
    open fun blogEngine(repository: FragmentRepository): BlogEngine =
        BlogEngine(repository)

    @Bean
    open fun fragmentsController(repository: FragmentRepository, blogEngine: BlogEngine): FragmentsSpringController =
        FragmentsSpringController(repository, blogEngine)
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
