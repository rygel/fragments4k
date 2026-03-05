package io.andromeda.fragments.blog.test

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class BlogEngineFullCycleTest {

    @Test
    fun blogEngineHandlesFullRequestCycle() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "This is test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 10, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            tags = listOf("blog")
        )

        val fragment2 = Fragment(
            slug = "related-post",
            title = "Related Post",
            content = "This is related content",
            preview = "Related preview",
            date = LocalDateTime.of(2024, 3, 15, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            tags = listOf("blog")
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val blogEngine = BlogEngine(repository, pageSize = 2)

        val overview = blogEngine.getOverview(page = 1)

        assertNotNull(overview)
        assertEquals(1, overview.totalPages)
        assertEquals(2, overview.items.size)
        assertEquals("related-post", overview.items[0].slug)
        assertEquals("Related Post", overview.items[0].title)

        val post = blogEngine.getPost("2024", "3", "test-post")
        assertNotNull(post)
        assertEquals("test-post", post?.slug)
        assertEquals("This is test content", post?.content)
        assertEquals("Test preview", post?.preview)
    }

    @Test
    fun blogEngineHandlesTagFiltering() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "kotlin-tag-post",
            title = "Kotlin Tag Post",
            content = "Content about Kotlin",
            preview = "Kotlin preview",
            date = LocalDateTime.of(2024, 3, 7, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            tags = listOf("kotlin", "blog")
        )

        val fragment2 = Fragment(
            slug = "java-tag-post",
            title = "Java Tag Post",
            content = "Content about Java",
            preview = "Java preview",
            date = LocalDateTime.of(2024, 3, 8, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            tags = listOf("java", "blog")
        )

        val fragment3 = Fragment(
            slug = "another-kotlin-post",
            title = "Another Kotlin Post",
            content = "More content about Kotlin",
            preview = "Another Kotlin preview",
            date = LocalDateTime.of(2024, 3, 9, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            tags = listOf("kotlin", "blog")
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)
        repository.addFragment(fragment3)

        val blogEngine = BlogEngine(repository)

        val kotlinPosts = blogEngine.getByTag("kotlin", page = 1)

        assertEquals(2, kotlinPosts.totalItems)
        assertEquals(2, kotlinPosts.items.size)
        assertEquals("another-kotlin-post", kotlinPosts.items[0].slug)
        assertEquals("Another Kotlin Post", kotlinPosts.items[0].title)
        assertEquals("kotlin-tag-post", kotlinPosts.items[1].slug)
        assertEquals("Kotlin Tag Post", kotlinPosts.items[1].title)
    }

    @Test
    fun blogEngineHandlesCategoryFiltering() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "blog-category-post",
            title = "Blog Category Post",
            content = "Content for blog",
            preview = "Blog preview",
            date = LocalDateTime.of(2024, 3, 9, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            categories = listOf("blog")
        )

        val fragment2 = Fragment(
            slug = "another-blog-post",
            title = "Another Blog Post",
            content = "More content for blog",
            preview = "Another blog preview",
            date = LocalDateTime.of(2024, 3, 10, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            categories = listOf("blog")
        )

        val fragment3 = Fragment(
            slug = "tutorial-post",
            title = "Tutorial Post",
            content = "Tutorial content",
            preview = "Tutorial preview",
            date = LocalDateTime.of(2024, 3, 11, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            categories = listOf("tutorial")
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)
        repository.addFragment(fragment3)

        val blogEngine = BlogEngine(repository)

        val blogPosts = blogEngine.getByCategory("blog", page = 1)

        assertEquals(2, blogPosts.totalItems)
        assertEquals(2, blogPosts.items.size)
        assertEquals("another-blog-post", blogPosts.items[0].slug)
        assertEquals("Another Blog Post", blogPosts.items[0].title)
        assertEquals("blog-category-post", blogPosts.items[1].slug)
        assertEquals("Blog Category Post", blogPosts.items[1].title)
    }
}
