package io.andromeda.fragments.test

import io.andromeda.fragments.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import kotlin.io.path.*

class BlogEngineFullCycleTest {

    @TempDir
    lateinit var tempDir: Path
    lateinit var contentDir: Path

    @BeforeEach
    fun setup() {
        tempDir = Files.createTempDirectory("blog-cycle-test")
        contentDir = tempDir.resolve("content")

        Files.createDirectories(contentDir.resolve("posts"))
    }

    @AfterEach
    fun cleanup() {
        Files.walk(contentDir)
            .filter { it.toFile().isFile }
            .forEach { Files.deleteIfExists(it) }

        Files.deleteIfExists(contentDir)
        Files.deleteIfExists(tempDir)
    }

    @Test
    fun `blog engine handles full request cycle`() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "This is test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 10),
            frontMatter = mapOf("tags" to listOf("blog")),
            visible = true,
            template = "blog_post"
        )

        val fragment2 = Fragment(
            slug = "related-post",
            title = "Related Post",
            content = "This is related content",
            preview = "Related preview",
            date = LocalDateTime.of(2024, 3, 15),
            frontMatter = mapOf("tags" to listOf("blog")),
            visible = true,
            template = "blog_post"
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val blogEngine = BlogEngine(repository)

        val overview = blogEngine.getOverview(page = 1, perPage = 2)

        assertNotNull(overview)
        assertEquals(1, overview.totalPages)
        assertEquals(2, overview.items.size)
        assertEquals("test-post", overview.items[0].slug)
        assertEquals("Related Post", overview.items[1].title)

        val post = blogEngine.getBySlug("test-post")
        assertNotNull(post)
        assertEquals("test-post", post?.slug)
        assertEquals("This is test content", post?.content)
        assertEquals("Test preview", post?.preview)
    }

    @Test
    fun `blog engine handles tag filtering`() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "kotlin-tag-post",
            title = "Kotlin Tag Post",
            content = "Content about Kotlin",
            preview = "Kotlin preview",
            date = LocalDateTime.of(2024, 3, 7),
            frontMatter = mapOf("tags" to listOf("kotlin", "blog")),
            visible = true,
            template = "blog_post"
        )

        val fragment2 = Fragment(
            slug = "java-tag-post",
            title = "Java Tag Post",
            content = "Content about Java",
            preview = "Java preview",
            date = LocalDateTime.of(2024, 3, 8),
            frontMatter = mapOf("tags" to listOf("java", "blog")),
            visible = true,
            template = "blog_post"
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val blogEngine = BlogEngine(repository)

        val kotlinPosts = blogEngine.getByTag("kotlin")
        val javaPosts = blogEngine.getByTag("java")

        assertEquals(1, kotlinPosts.size)
        assertEquals("kotlin-tag-post", kotlinPosts[0].slug)
        assertEquals("Kotlin Post", kotlinPosts[0].title)
    }

    @Test
    fun `blog engine handles category filtering`() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "blog-category-post",
            title = "Blog Category Post",
            content = "Content for blog",
            preview = "Blog preview",
            date = LocalDateTime.of(2024, 3, 9),
            frontMatter = mapOf("categories" to listOf("blog")),
            visible = true,
            template = "blog_post"
        )

        repository.addFragment(fragment1)

        val blogEngine = BlogEngine(repository)

        val blogPosts = blogEngine.getByCategory("blog")

        assertEquals(1, blogPosts.size)
        assertEquals("blog-category-post", blogPosts[0].slug)
        assertEquals("Blog Category Post", blogPosts[0].title)
    }
}
