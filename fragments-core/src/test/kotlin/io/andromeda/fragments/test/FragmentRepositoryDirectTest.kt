package io.andromeda.fragments.test
 
import io.andromeda.fragments.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import kotlin.io.path.*
 
class FragmentRepositoryDirectTest {
 
    @Test
    fun simpleGetAllWorks() = runBlocking {
        val repository = InMemoryFragmentRepository()
 
        val fragment1 = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 10, 10, 0),
            publishDate = null,
            frontMatter = mapOf("tags" to listOf("test")),
            visible = true,
            template = "blog_post",
            authorIds = emptyList()
        )
 
        repository.addFragment(fragment1)
 
        val result = repository.getAll()
 
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("test-post", result[0].slug)
        assertEquals("Test Post", result[0].title)
    }
 
    @Test
    fun simpleGetBySlugWorks() = runBlocking {
        val repository = InMemoryFragmentRepository()
 
        val fragment1 = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 10, 10, 0),
            publishDate = null,
            frontMatter = mapOf("tags" to listOf("test")),
            visible = true,
            template = "blog_post",
            authorIds = emptyList()
        )
 
        repository.addFragment(fragment1)
 
        val result = repository.getBySlug("test-post")
 
        assertNotNull(result)
        assertEquals("test-post", result?.slug)
        assertEquals("Test Post", result?.title)
    }
 
    @Test
    fun simpleGetByTagWorks() = runBlocking {
        val repository = InMemoryFragmentRepository()
 
        val fragment1 = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 7, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
            tags = listOf("test"),
            authorIds = emptyList()
        )
 
        repository.addFragment(fragment1)
 
        val result = repository.getByTag("test")
 
        assertEquals(1, result.size)
        assertEquals("test-post", result[0].slug)
        assertEquals("Test Post", result[0].title)
    }
 
    @Test
    fun simpleGetByCategoryWorks() = runBlocking {
        val repository = InMemoryFragmentRepository()
 
        val fragment1 = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 9, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
            categories = listOf("blog"),
            authorIds = emptyList()
        )
 
        repository.addFragment(fragment1)
 
        val result = repository.getByCategory("blog")
 
        assertEquals(1, result.size)
        assertEquals("test-post", result[0].slug)
    }
 
    @Test
    fun simpleReloadWorks() = runBlocking {
        val repository = InMemoryFragmentRepository()
 
        val fragment1 = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Test content",
            preview = "No backticks",
            date = LocalDateTime.of(2024, 3, 12, 10, 0),
            publishDate = null,
            frontMatter = mapOf("tags" to listOf("simple")),
            visible = true,
            template = "blog_post",
            authorIds = emptyList()
        )
 
        repository.addFragment(fragment1)
 
        assertEquals(1, repository.getAll().size)
 
        repository.reload()
 
        assertEquals(0, repository.getAll().size)
    }
}
