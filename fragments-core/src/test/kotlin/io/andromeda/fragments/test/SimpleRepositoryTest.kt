package io.andromeda.fragments.test

import io.andromeda.fragments.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import kotlin.io.path.*

class InMemoryFragmentRepository : FragmentRepository {
    private val fragments = mutableListOf<Fragment>()

    override suspend fun getAll(): List<Fragment> {
        return fragments.toList()
    }

    override suspend fun getAllVisible(): List<Fragment> {
        return fragments.filter { it.visible }.toList()
    }

    override suspend fun getBySlug(slug: String): Fragment? {
        return fragments.find { it.slug == slug }
    }

    override suspend fun getByYearMonthAndSlug(year: String, month: String, slug: String): Fragment? {
        return fragments.find {
            it.date?.monthValue?.toString() == month &&
            it.slug == slug
        }
    }

    override suspend fun getByTag(tag: String): List<Fragment> {
        return fragments.filter { it.tags.contains(tag) }.toList()
    }

    override suspend fun getByCategory(category: String): List<Fragment> {
        return fragments.filter { it.categories.contains(category) }.toList()
    }

    override suspend fun reload() {
        fragments.clear()
    }

    fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
    }
}

class SimpleRepositoryTest {

    @Test
    fun testGetAllFragments() {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "first-post",
            title = "First Post",
            content = "This is my first post",
            preview = "First post preview",
            date = LocalDateTime.of(2024, 3, 4),
            frontMatter = mapOf("tags" to listOf("kotlin", "testing")),
            visible = true,
            template = "blog_post"
        )

        val fragment2 = Fragment(
            slug = "second-post",
            title = "Second Post",
            content = "This is my second post",
            preview = "Second post preview",
            date = LocalDateTime.of(2024, 3, 5),
            frontMatter = mapOf("tags" to listOf("java", "maven")),
            visible = true,
            template = "blog_post"
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val result = runBlocking {
            repository.getAll()
        }

        assertEquals(2, result.size)
        assertEquals("first-post", result[0].slug)
        assertEquals("Second Post", result[1].title)
    }
}
