package io.andromeda.fragments.micronaut

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test

@MicronautTest
class FragmentsMicronautControllerTest {

    @Inject
    lateinit var controller: FragmentsMicronautController

    @Test
    fun `controller is instantiated`() {
        assert(controller is FragmentsMicronautController)
    }

    @Test
    fun `static engine is injected`() {
        assert(controller.staticEngine is StaticPageEngine)
    }

    @Test
    fun `blog engine is injected`() {
        assert(controller.blogEngine is BlogEngine)
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
