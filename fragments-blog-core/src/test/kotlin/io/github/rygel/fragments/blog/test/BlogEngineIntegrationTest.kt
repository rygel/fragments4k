package io.github.rygel.fragments.blog.test

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.test.InMemoryFragmentRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BlogEngineIntegrationTest {
    @Test
    fun blogEngineReturnsPaginatedPosts() =
        runBlocking {
            val repository = InMemoryFragmentRepository()

            val fragment1 =
                Fragment(
                    slug = "first-post",
                    title = "First Post",
                    htmlContent = "This is my first post with markdown content",
                    preview = "First post preview",
                    date = LocalDateTime.of(2024, 3, 10, 10, 0),
                    publishDate = null,
                    frontMatter = emptyMap(),
                    visible = true,
                    template = "blog",
                    tags = listOf("blog"),
                )

            val fragment2 =
                Fragment(
                    slug = "second-post",
                    title = "Second Post",
                    htmlContent = "This is my second post with more markdown content",
                    preview = "Second post preview",
                    date = LocalDateTime.of(2024, 3, 15, 10, 0),
                    publishDate = null,
                    frontMatter = emptyMap(),
                    visible = true,
                    template = "blog",
                    tags = listOf("blog"),
                )

            val fragment3 =
                Fragment(
                    slug = "third-post",
                    title = "Third Post",
                    htmlContent = "This is my third post",
                    preview = "Third post preview",
                    date = LocalDateTime.of(2024, 3, 20, 10, 0),
                    publishDate = null,
                    frontMatter = emptyMap(),
                    visible = true,
                    template = "blog",
                    tags = listOf("blog"),
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
    fun blogEngineFiltersByTagCorrectly() =
        runBlocking {
            val repository = InMemoryFragmentRepository()

            val fragment1 =
                Fragment(
                    slug = "kotlin-post",
                    title = "Kotlin Post",
                    htmlContent = "Content about Kotlin",
                    preview = "Kotlin preview",
                    date = LocalDateTime.of(2024, 3, 7, 10, 0),
                    publishDate = null,
                    frontMatter = emptyMap(),
                    visible = true,
                    template = "blog",
                    tags = listOf("kotlin", "programming"),
                )

            val fragment2 =
                Fragment(
                    slug = "java-post",
                    title = "Java Post",
                    htmlContent = "Content about Java",
                    preview = "Java preview",
                    date = LocalDateTime.of(2024, 3, 8, 10, 0),
                    publishDate = null,
                    frontMatter = emptyMap(),
                    visible = true,
                    template = "blog",
                    tags = listOf("java", "maven"),
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
    fun `getAllPosts returns all blog posts with resolved date-based URLs`() =
        runBlocking {
            val repository = InMemoryFragmentRepository()
            repository.addFragment(
                Fragment(
                    slug = "post-with-date",
                    title = "Post With Date",
                    htmlContent = "Content",
                    preview = "Preview",
                    date = LocalDateTime.of(2024, 3, 10, 10, 0),
                    publishDate = null,
                    frontMatter = emptyMap(),
                    visible = true,
                    template = "blog",
                ),
            )
            repository.addFragment(
                Fragment(
                    slug = "static-page",
                    title = "Static Page",
                    htmlContent = "Content",
                    preview = "Preview",
                    date = LocalDateTime.of(2024, 3, 11, 10, 0),
                    publishDate = null,
                    frontMatter = emptyMap(),
                    visible = true,
                    template = "static",
                ),
            )

            val blogEngine = BlogEngine(repository, blogUrlPrefix = "/blog")
            val posts = blogEngine.getAllPosts()

            assertEquals(1, posts.size, "should only include blog-template fragments")
            assertEquals("post-with-date", posts[0].slug)
            assertEquals("/blog/2024/03/post-with-date", posts[0].resolvedUrl, "URL should use date-based prefix")
        }

    @Test
    fun `getAllPosts excludes drafts by default`() =
        runBlocking {
            val repository = InMemoryFragmentRepository()
            repository.addFragment(
                Fragment(
                    slug = "published-post",
                    title = "Published",
                    htmlContent = "",
                    preview = "",
                    date = LocalDateTime.of(2024, 1, 1, 0, 0),
                    publishDate = null,
                    frontMatter = emptyMap(),
                    visible = true,
                    template = "blog",
                    status = io.github.rygel.fragments.FragmentStatus.PUBLISHED,
                ),
            )
            repository.addFragment(
                Fragment(
                    slug = "draft-post",
                    title = "Draft",
                    htmlContent = "",
                    preview = "",
                    date = LocalDateTime.of(2024, 1, 2, 0, 0),
                    publishDate = null,
                    frontMatter = emptyMap(),
                    visible = false,
                    template = "blog",
                    status = io.github.rygel.fragments.FragmentStatus.DRAFT,
                ),
            )

            val blogEngine = BlogEngine(repository)
            assertEquals(1, blogEngine.getAllPosts().size, "drafts excluded by default")
            assertEquals(2, blogEngine.getAllPosts(includeDrafts = true).size, "drafts included when requested")
        }

    @Test
    fun blogEngineFiltersByCategoryCorrectly() =
        runBlocking {
            val repository = InMemoryFragmentRepository()

            val fragment1 =
                Fragment(
                    slug = "tutorial-post",
                    title = "Tutorial Post",
                    htmlContent = "Content about tutorials",
                    preview = "Tutorial preview",
                    date = LocalDateTime.of(2024, 3, 5, 10, 0),
                    publishDate = null,
                    frontMatter = emptyMap(),
                    visible = true,
                    template = "blog",
                    categories = listOf("tutorial"),
                )

            val fragment2 =
                Fragment(
                    slug = "blog-post",
                    title = "Blog Post",
                    htmlContent = "Content for blog",
                    preview = "Blog preview",
                    date = LocalDateTime.of(2024, 3, 6, 10, 0),
                    publishDate = null,
                    frontMatter = emptyMap(),
                    visible = true,
                    template = "blog",
                    categories = listOf("blog"),
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
