package io.github.rygel.fragments.test

import io.github.rygel.fragments.Author
import io.github.rygel.fragments.Fragment
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AuthorRepositoryTest {

    @Test
    fun testAddAndGetAllAuthors() = runBlocking {
        val repository = InMemoryAuthorRepository()

        val author1 = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com",
            bio = "Software developer and writer"
        )

        val author2 = Author(
            id = "author-2",
            name = "Jane Smith",
            slug = "jane-smith",
            email = "jane@example.com"
        )

        repository.addAuthor(author1)
        repository.addAuthor(author2)

        val result = repository.getAll()

        assertEquals(2, result.size)
        assertEquals("John Doe", result[0].name)
        assertEquals("Jane Smith", result[1].name)
    }

    @Test
    fun testGetAuthorById() = runBlocking {
        val repository = InMemoryAuthorRepository()

        val author1 = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com"
        )

        repository.addAuthor(author1)

        val result = repository.getById("author-1")

        assertNotNull(result)
        assertEquals("author-1", result?.id)
        assertEquals("John Doe", result?.name)
        assertEquals("john-doe", result?.slug)
    }

    @Test
    fun testGetAuthorBySlug() = runBlocking {
        val repository = InMemoryAuthorRepository()

        val author1 = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com"
        )

        repository.addAuthor(author1)

        val result = repository.getBySlug("john-doe")

        assertNotNull(result)
        assertEquals("author-1", result?.id)
        assertEquals("John Doe", result?.name)
    }

    @Test
    fun testGetAuthorBySlugOrId() = runBlocking {
        val repository = InMemoryAuthorRepository()

        val author1 = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com"
        )

        repository.addAuthor(author1)

        val resultById = repository.getBySlugOrId("author-1")
        val resultBySlug = repository.getBySlugOrId("john-doe")

        assertNotNull(resultById)
        assertNotNull(resultBySlug)
        assertEquals(author1, resultById)
        assertEquals(author1, resultBySlug)
    }

    @Test
    fun testRegisterNewAuthor() = runBlocking {
        val repository = InMemoryAuthorRepository()

        val author1 = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com"
        )

        repository.register(author1)

        assertEquals(1, repository.getAll().size)
    }

    @Test
    fun testUpdateExistingAuthor() = runBlocking {
        val repository = InMemoryAuthorRepository()

        val author1 = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com"
        )

        repository.addAuthor(author1)

        val updatedAuthor = author1.copy(
            name = "John D.",
            bio = "Updated biography"
        )

        repository.register(updatedAuthor)

        val retrieved = repository.getById("author-1")
        assertEquals("John D.", retrieved?.name)
        assertEquals("Updated biography", retrieved?.bio)
    }

    @Test
    fun testRemoveAuthor() = runBlocking {
        val repository = InMemoryAuthorRepository()

        val author1 = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com"
        )

        repository.addAuthor(author1)

        assertEquals(1, repository.getAll().size)

        val result = repository.remove("author-1")

        assertTrue(result)
        assertEquals(0, repository.getAll().size)
    }

    @Test
    fun testCountAuthors() = runBlocking {
        val repository = InMemoryAuthorRepository()

        val author1 = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com"
        )

        val author2 = Author(
            id = "author-2",
            name = "Jane Smith",
            slug = "jane-smith",
            email = "jane@example.com"
        )

        repository.register(author1)
        repository.register(author2)

        assertEquals(2, repository.count())
    }

    @Test
    fun testClearRepository() = runBlocking {
        val repository = InMemoryAuthorRepository()

        val author1 = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com"
        )

        repository.addAuthor(author1)

        assertEquals(1, repository.getAll().size)

        repository.clear()

        assertEquals(0, repository.getAll().size)
    }

    @Test
    fun testGetByName() = runBlocking {
        val repository = InMemoryAuthorRepository()

        val author1 = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com"
        )

        repository.addAuthor(author1)

        val result = repository.getByName("John Doe")

        assertNotNull(result)
        assertEquals("author-1", result?.id)
        assertEquals("John Doe", result?.name)
    }
}

class FragmentAuthorFilteringTest {

    @Test
    fun testGetByAuthorId() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "test-post-1",
            title = "Test Post 1",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 10, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
            authorIds = listOf("author-1", "author-2")
        )

        val fragment2 = Fragment(
            slug = "test-post-2",
            title = "Test Post 2",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 11, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
            authorIds = listOf("author-3")
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val result = repository.getByAuthor("author-1")

        assertEquals(1, result.size)
        assertEquals("test-post-1", result[0].slug)
    }

    @Test
    fun testGetByLegacyAuthorField() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "test-post-1",
            title = "Test Post 1",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 10, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
            author = "john-doe",
            authorIds = emptyList()
        )

        repository.addFragment(fragment1)

        val result = repository.getByAuthor("john-doe")

        assertEquals(1, result.size)
        assertEquals("test-post-1", result[0].slug)
    }

    @Test
    fun testGetByMultipleAuthors() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            slug = "test-post-1",
            title = "Test Post 1",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 10, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
            authorIds = listOf("author-1")
        )

        val fragment2 = Fragment(
            slug = "test-post-2",
            title = "Test Post 2",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 11, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
            authorIds = listOf("author-2")
        )

        val fragment3 = Fragment(
            slug = "test-post-3",
            title = "Test Post 3",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 12, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
            authorIds = listOf("author-1", "author-2")
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)
        repository.addFragment(fragment3)

        val result = repository.getByAuthors(listOf("author-1", "author-2"))

        assertEquals(3, result.size)
    }

    @Test
    fun testPrimaryAuthorReturnsFirstAuthorId() = runBlocking {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 10, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
            authorIds = listOf("author-1", "author-2", "author-3")
        )

        assertEquals("author-1", fragment.primaryAuthor)
    }

    @Test
    fun testPrimaryAuthorFallsBackToLegacyAuthor() = runBlocking {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 10, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
            author = "john-doe",
            authorIds = emptyList()
        )

        assertEquals("john-doe", fragment.primaryAuthor)
    }

    @Test
    fun testPrimaryAuthorReturnsNullWhenNoAuthors() = runBlocking {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Test content",
            preview = "Test preview",
            date = LocalDateTime.of(2024, 3, 10, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            visible = true,
            template = "blog_post",
            authorIds = emptyList()
        )

        assertNull(fragment.primaryAuthor)
    }
}

class AuthorDataClassTest {

    @Test
    fun testAuthorCreationWithAllFields() {
        val author = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com",
            bio = "Software developer and writer",
            avatar = "https://example.com/avatar.jpg",
            website = "https://johndoe.com",
            twitter = "johndoe",
            github = "johndoe",
            linkedin = "johndoe",
            location = "New York",
            company = "Tech Co",
            role = "admin",
            socialLinks = mapOf("Discord" to "https://discord.gg/johndoe"),
            joinedDate = LocalDateTime.of(2024, 1, 1, 0, 0)
        )

        assertEquals("author-1", author.id)
        assertEquals("John Doe", author.name)
        assertEquals("john-doe", author.slug)
        assertEquals("john@example.com", author.email)
        assertEquals("Software developer and writer", author.bio)
        assertEquals("https://example.com/avatar.jpg", author.avatar)
        assertEquals("https://johndoe.com", author.website)
        assertEquals("johndoe", author.twitter)
        assertEquals("johndoe", author.github)
        assertEquals("johndoe", author.linkedin)
        assertEquals("New York", author.location)
        assertEquals("Tech Co", author.company)
        assertEquals("admin", author.role)
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), author.joinedDate)
    }

    @Test
    fun testAuthorCreationWithMinimalFields() {
        val author = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe"
        )

        assertEquals("author-1", author.id)
        assertEquals("John Doe", author.name)
        assertEquals("john-doe", author.slug)
        assertNull(author.email)
        assertNull(author.bio)
        assertNull(author.avatar)
        assertNull(author.website)
        assertNull(author.twitter)
        assertNull(author.github)
        assertNull(author.linkedin)
        assertNull(author.location)
        assertNull(author.company)
        assertNull(author.role)
        assertNotNull(author.joinedDate)
    }

    @Test
    fun testAuthorCopy() {
        val author1 = Author(
            id = "author-1",
            name = "John Doe",
            slug = "john-doe",
            email = "john@example.com"
        )

        val author2 = author1.copy(
            name = "John D.",
            bio = "Updated biography"
        )

        assertEquals("author-1", author2.id)
        assertEquals("john-doe", author2.slug)
        assertEquals("John D.", author2.name)
        assertEquals("john@example.com", author2.email)
        assertEquals("Updated biography", author2.bio)
    }
}
