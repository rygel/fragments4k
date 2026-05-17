package io.github.rygel.fragments

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class FileSystemContentSeriesRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var repository: FileSystemContentSeriesRepository

    @BeforeEach
    fun setUp() {
        repository = FileSystemContentSeriesRepository(tempDir)
    }

    @AfterEach
    fun tearDown() {
        runBlocking { repository.reload() }
    }

    @Test
    fun discoversSeriesFilesWithDefaultExtension() =
        runBlocking {
            val seriesDir = tempDir.resolve("series").toFile()
            seriesDir.mkdirs()
            seriesDir.resolve("kotlin-tutorial.series.yml").writeText(
                """
                slug: kotlin-tutorial
                title: Kotlin Tutorial
                status: ACTIVE
                order: 1
                tags:
                  - kotlin
                  - tutorial
                categories:
                  - programming
                createdAt: "2024-01-15T10:00:00"
                updatedAt: "2024-01-15T10:00:00"
                """.trimIndent(),
            )
            seriesDir.resolve("other-guide.series.yml").writeText(
                """
                slug: other-guide
                title: Other Guide
                status: ACTIVE
                order: 2
                tags: []
                categories: []
                createdAt: "2024-02-01T10:00:00"
                updatedAt: "2024-02-01T10:00:00"
                """.trimIndent(),
            )
            seriesDir.resolve("readme.txt").writeText("Not a series file")

            val all = repository.getAll()

            assertEquals(2, all.size)
            assertTrue(all.any { it.slug == "kotlin-tutorial" })
            assertTrue(all.any { it.slug == "other-guide" })
        }

    @Test
    fun ignoresFilesWithoutCorrectExtension() =
        runBlocking {
            val seriesDir = tempDir.resolve("series").toFile()
            seriesDir.mkdirs()
            seriesDir.resolve("guide.yml").writeText(
                """
                slug: guide
                title: Guide
                status: ACTIVE
                order: 1
                """.trimIndent(),
            )
            seriesDir.resolve("notes.yaml").writeText("notes: true")
            seriesDir.resolve("data.json").writeText("{}")

            val all = repository.getAll()

            assertTrue(all.isEmpty())
        }

    @Test
    fun discoversSeriesWithCustomExtension() =
        runBlocking {
            val customRepo = FileSystemContentSeriesRepository(tempDir, extension = ".ser.yaml")
            val seriesDir = tempDir.resolve("series").toFile()
            seriesDir.mkdirs()
            seriesDir.resolve("custom-series.ser.yaml").writeText(
                """
                slug: custom-series
                title: Custom Series
                status: ACTIVE
                order: 1
                tags: []
                categories: []
                createdAt: "2024-01-01T00:00:00"
                updatedAt: "2024-01-01T00:00:00"
                """.trimIndent(),
            )

            val all = customRepo.getAll()

            assertEquals(1, all.size)
            assertEquals("custom-series", all[0].slug)
        }

    @Test
    fun deleteSeriesFindsFileByFullExtension() =
        runBlocking {
            val seriesDir = tempDir.resolve("series").toFile()
            seriesDir.mkdirs()
            seriesDir.resolve("removable.series.yml").writeText(
                """
                slug: removable
                title: Removable
                status: ACTIVE
                order: 1
                tags: []
                categories: []
                createdAt: "2024-01-01T00:00:00"
                updatedAt: "2024-01-01T00:00:00"
                """.trimIndent(),
            )

            val all = repository.getAll()
            assertEquals(1, all.size)

            val result = repository.deleteSeries("removable")
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull() == true)
            assertFalse(seriesDir.resolve("removable.series.yml").exists())
        }

    @Test
    fun deleteSeriesReturnsFalseForMissingSlug() =
        runBlocking {
            val result = repository.deleteSeries("nonexistent")
            assertTrue(result.isSuccess)
            assertFalse(result.getOrNull() == true)
        }

    @Test
    fun getSeriesBySlugFindsCorrectSeries() =
        runBlocking {
            val seriesDir = tempDir.resolve("series").toFile()
            seriesDir.mkdirs()
            seriesDir.resolve("find-me.series.yml").writeText(
                """
                slug: find-me
                title: Find Me
                status: ACTIVE
                order: 1
                tags: []
                categories: []
                createdAt: "2024-01-01T00:00:00"
                updatedAt: "2024-01-01T00:00:00"
                """.trimIndent(),
            )

            val found = repository.getBySlug("find-me")
            assertNotNull(found)
            assertEquals("Find Me", found?.title)

            val missing = repository.getBySlug("no-such-series")
            assertFalse(missing != null)
        }

    @Test
    fun updateSeriesFindsFileByFullExtension() =
        runBlocking {
            val seriesDir = tempDir.resolve("series").toFile()
            seriesDir.mkdirs()
            seriesDir.resolve("updatable.series.yml").writeText(
                """
                slug: updatable
                title: Original Title
                status: ACTIVE
                order: 1
                tags: []
                categories: []
                createdAt: "2024-01-01T00:00:00"
                updatedAt: "2024-01-01T00:00:00"
                """.trimIndent(),
            )

            val existing = repository.getBySlug("updatable")
            assertNotNull(existing)

            val updated = existing!!.copy(title = "Updated Title")
            val result = repository.updateSeries(updated)

            assertTrue(result.isSuccess)
            assertEquals("Updated Title", result.getOrNull()?.title)

            repository.reload()
            val reloaded = repository.getBySlug("updatable")
            assertNotNull(reloaded)
            assertEquals("Updated Title", reloaded?.title)
        }

    @Test
    fun createSeriesRejectsPathTraversalSlug() =
        runBlocking {
            val series = ContentSeries(
                slug = "../../etc/passwd",
                title = "Malicious",
                status = SeriesStatus.ACTIVE,
            )
            val result = repository.createSeries(series)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }

    @Test
    fun createSeriesRejectsBlankSlug() =
        runBlocking {
            val series = ContentSeries(
                slug = "",
                title = "Empty Slug",
                status = SeriesStatus.ACTIVE,
            )
            val result = repository.createSeries(series)
            assertTrue(result.isFailure)
        }

    @Test
    fun deleteSeriesRejectsPathTraversalSlug() =
        runBlocking {
            val result = repository.deleteSeries("../../etc/passwd")
            assertTrue(result.isFailure)
        }

    @Test
    fun getBySlugRejectsPathTraversalSlug() {
        runBlocking {
            assertThrows<IllegalArgumentException> {
                repository.getBySlug("../secret")
            }
        }
    }
}
