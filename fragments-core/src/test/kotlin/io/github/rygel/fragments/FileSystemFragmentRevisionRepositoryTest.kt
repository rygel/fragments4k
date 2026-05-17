package io.github.rygel.fragments

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.time.LocalDateTime

class FileSystemFragmentRevisionRepositoryTest {
    private lateinit var tempDir: File
    private lateinit var repository: FileSystemFragmentRevisionRepository

    @BeforeEach
    fun setUp() {
        tempDir = createTempDir("revision-test")
        repository = FileSystemFragmentRevisionRepository(tempDir.absolutePath)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createFragment(
        slug: String = "test-post",
        title: String = "Test Post",
        htmlContent: String = "<p>Hello world</p>",
        preview: String = "Hello world",
    ): Fragment =
        Fragment(
            title = title,
            slug = slug,
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.now(),
            publishDate = null,
            preview = preview,
            htmlContent = htmlContent,
            frontMatter = emptyMap(),
        )

    @Test
    fun saveRevisionCreatesFileAndReturnsRevision() =
        runBlocking {
            val fragment = createFragment()
            val revision = repository.saveRevision(fragment, "author", "initial version")

            assertNotNull(revision)
            assertEquals("test-post", revision.fragmentSlug)
            assertEquals(1, revision.version)
            assertEquals("Test Post", revision.title)
            assertEquals("<p>Hello world</p>", revision.content)
            assertEquals("author", revision.changedBy)
            assertEquals("initial version", revision.changeReason)

            val revisionFile = File(tempDir, ".revisions/${revision.id}.json")
            assertTrue(revisionFile.exists())
        }

    @Test
    fun saveRevisionCreatesRevisionsDirectory() =
        runBlocking {
            val fragment = createFragment()
            repository.saveRevision(fragment)

            val revisionsDir = File(tempDir, ".revisions")
            assertTrue(revisionsDir.exists())
            assertTrue(revisionsDir.isDirectory)
        }

    @Test
    fun saveRevisionUpdatesIndexFile() =
        runBlocking {
            val fragment = createFragment()
            repository.saveRevision(fragment)

            val indexFile = File(tempDir, ".revisions/index.json")
            assertTrue(indexFile.exists())
            val content = indexFile.readText()
            assertTrue(content.contains("test-post"))
        }

    @Test
    fun saveRevisionFirstVersionHasNoPreviousRevisionId() =
        runBlocking {
            val fragment = createFragment()
            val revision = repository.saveRevision(fragment)

            assertNull(revision.previousRevisionId)
            assertTrue(revision.isInitial)
        }

    @Test
    fun deleteRevisionsReturnsZeroForUnknownSlug() =
        runBlocking {
            val count = repository.deleteRevisions("unknown")

            assertEquals(0, count)
        }

    @Test
    fun revertToRevisionFailsForUnknownRevision() =
        runBlocking {
            val result = repository.revertToRevision("test-post", "nonexistent")

            assertTrue(result.isFailure)
        }

    @Test
    fun getRevisionCountReturnsZeroForUnknownSlug() =
        runBlocking {
            assertEquals(0, repository.getRevisionCount("unknown"))
        }

    @Test
    fun getRevisionReturnsNullForUnknownId() =
        runBlocking {
            val result = repository.getRevision("nonexistent")

            assertNull(result)
        }

    @Test
    fun getLatestRevisionReturnsNullForUnknownSlug() =
        runBlocking {
            val result = repository.getLatestRevision("unknown")

            assertNull(result)
        }

    @Test
    fun safeYamlRejectsUnsafeTagsInRevisionFrontMatter() {
        val yaml = org.yaml.snakeyaml.Yaml(org.yaml.snakeyaml.constructor.SafeConstructor(org.yaml.snakeyaml.LoaderOptions()))
        val result =
            runCatching {
                yaml.load<Any>("!!javax.script.ScriptEngineManager [[]]")
            }
        assertTrue(
            result.isFailure || result.getOrNull() is String,
            "SafeConstructor should reject or neuter unsafe YAML tags",
        )
    }

    @Test
    fun saveRevisionRejectsPathTraversalSlug() {
        runBlocking {
            val fragment = createFragment(slug = "../../etc/passwd")
            assertThrows<IllegalArgumentException> {
                repository.saveRevision(fragment)
            }
        }
    }

    @Test
    fun saveRevisionRejectsBlankSlug() {
        runBlocking {
            val fragment = createFragment(slug = "")
            assertThrows<IllegalArgumentException> {
                repository.saveRevision(fragment)
            }
        }
    }

    @Test
    fun getRevisionsRejectsPathTraversalSlug() {
        runBlocking {
            assertThrows<IllegalArgumentException> {
                repository.getRevisions("../../etc")
            }
        }
    }

    @Test
    fun deleteRevisionsRejectsPathTraversalSlug() {
        runBlocking {
            assertThrows<IllegalArgumentException> {
                repository.deleteRevisions("../secret")
            }
        }
    }

    @Test
    fun getRevisionCountRejectsPathTraversalSlug() {
        runBlocking {
            assertThrows<IllegalArgumentException> {
                repository.getRevisionCount("../../etc")
            }
        }
    }
}
