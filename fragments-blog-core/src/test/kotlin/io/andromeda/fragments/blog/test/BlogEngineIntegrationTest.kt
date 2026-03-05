package io.andromeda.fragments.blog.test

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class BlogEngineIntegrationTest {

    @Test
    fun blogEngineReturnsPaginatedPosts() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "first-post",
            title = "First Post",
            content = "This is my first post with markdown content",
            preview = "First post preview",
            date = LocalDateTime.of(2024, 3, 10, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            tags = listOf("blog")
        )

        val fragment2 = Fragment(
            slug = "second-post",
            title = "Second Post",
            content = "This is my second post with more markdown content",
            preview = "Second post preview",
            date = LocalDateTime.of(2024, 3, 15, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            tags = listOf("blog")
        )

        val fragment3 = Fragment(
            slug = "third-post",
            title = "Third Post",
            content = "This is my third post",
            preview = "Third post preview",
            date = LocalDateTime.of(2024, 3, 20, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            tags = listOf("blog")
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)
        repository.addFragment(fragment3)

        val blogEngine = BlogEngine(repository, pageSize = 10)

        val result = blogEngine.getOverview(page = 1)
        assertNotNull(result)
        assertEquals(1, result.totalPages)
        assertEquals(3, result.items.size)
        assertEquals("third-post", result.items[0].slug)
        assertEquals("Third Post", result.items[0].title)
    }

    @Test
    fun blogEngineFiltersByTagCorrectly() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "kotlin-post",
            title = "Kotlin Post",
            content = "Content about Kotlin",
            preview = "Kotlin preview",
            date = LocalDateTime.of(2024, 3, 7, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            tags = listOf("kotlin", "programming")
        )

        val fragment2 = Fragment(
            slug = "java-post",
            title = "Java Post",
            content = "Content about Java",
            preview = "Java preview",
            date = LocalDateTime.of(2024, 3, 8, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            tags = listOf("java", "maven")
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val blogEngine = BlogEngine(repository)

        val result = blogEngine.getByTag("kotlin", page = 1)

        assertEquals(1, result.totalItems)
        assertEquals("kotlin-post", result.items[0].slug)
        assertEquals("Kotlin Post", result.items[0].title)
    }

    @Test
    fun blogEngineFiltersByCategoryCorrectly() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "tutorial-post",
            title = "Tutorial Post",
            content = "Content about tutorials",
            preview = "Tutorial preview",
            date = LocalDateTime.of(2024, 3, 5, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            categories = listOf("tutorial")
        )

        val fragment2 = Fragment(
            slug = "blog-post",
            title = "Blog Post",
            content = "Content for blog",
            preview = "Blog preview",
            date = LocalDateTime.of(2024, 3, 6, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            categories = listOf("blog")
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val blogEngine = BlogEngine(repository)

        val result = blogEngine.getByCategory("blog", page = 1)

        assertEquals(1, result.totalItems)
        assertEquals("blog-post", result.items[0].slug)
        assertEquals("Blog Post", result.items[0].title)
    }
}
