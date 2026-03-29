package io.github.rygel.fragments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FileSystemContentSeriesRepository(
    private val basePath: Path,
    private val extension: String = ".series.yml"
) : ContentSeriesRepository {

    private val logger = LoggerFactory.getLogger(FileSystemContentSeriesRepository::class.java)
    private val yaml = Yaml()
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private var cachedSeries: List<ContentSeries> = emptyList()
    private var lastLoaded: LocalDateTime = LocalDateTime.MIN

    override suspend fun getAll(): List<ContentSeries> = withContext(Dispatchers.IO) {
        loadSeries()
    }

    override suspend fun getBySlug(slug: String): ContentSeries? = withContext(Dispatchers.IO) {
        loadSeries().find { it.slug == slug }
    }

    override suspend fun getActiveSeries(): List<ContentSeries> = withContext(Dispatchers.IO) {
        loadSeries().filter { it.status == SeriesStatus.ACTIVE }
    }

    override suspend fun getSeriesByTag(tag: String): List<ContentSeries> = withContext(Dispatchers.IO) {
        loadSeries().filter { it.tags.contains(tag.lowercase()) }
    }

    override suspend fun getSeriesByCategory(category: String): List<ContentSeries> = withContext(Dispatchers.IO) {
        loadSeries().filter { it.categories.contains(category.lowercase()) }
    }

    override suspend fun createSeries(series: ContentSeries): Result<ContentSeries> = withContext(Dispatchers.IO) {
        if (loadSeries().any { it.slug == series.slug }) {
            logger.warn("Series already exists: ${series.slug}")
            return@withContext Result.failure(IllegalArgumentException("Series already exists: ${series.slug}"))
        }

        val newSeries = series.copy(
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        saveSeries(newSeries)
        cacheUpdatedSeries(newSeries)
        Result.success(newSeries)
    }

    override suspend fun updateSeries(series: ContentSeries): Result<ContentSeries> = withContext(Dispatchers.IO) {
        val existing = loadSeries().find { it.slug == series.slug }
        if (existing == null) {
            logger.warn("Series not found: ${series.slug}")
            return@withContext Result.failure(IllegalArgumentException("Series not found: ${series.slug}"))
        }

        val updatedSeries = series.copy(
            createdAt = existing.createdAt,
            updatedAt = LocalDateTime.now()
        )

        saveSeries(updatedSeries)
        cacheUpdatedSeries(updatedSeries)
        Result.success(updatedSeries)
    }

    override suspend fun deleteSeries(slug: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val file = getSeriesFile(slug)
        if (file == null || !file.exists()) {
            logger.warn("Series file not found: $slug")
            return@withContext Result.success(false)
        }

        try {
            Files.delete(file.toPath())
            invalidateCache()
            logger.info("Deleted series: $slug")
            Result.success(true)
        } catch (e: Exception) {
            logger.error("Failed to delete series: $slug", e)
            Result.failure(e)
        }
    }

    override suspend fun getFragmentsInSeries(seriesSlug: String, fragmentRepository: FragmentRepository): List<Fragment> {
        return fragmentRepository.getAll()
            .filter { it.seriesSlug == seriesSlug }
            .filter { it.seriesPart != null }
            .sortedBy { it.seriesPart }
    }

    override suspend fun getSeriesNavigation(
        seriesSlug: String,
        fragmentSlug: String,
        fragmentRepository: FragmentRepository
    ): SeriesNavigation? = withContext(Dispatchers.IO) {
        val series = getBySlug(seriesSlug) ?: return@withContext null
        val currentFragment = fragmentRepository.getBySlug(fragmentSlug) ?: return@withContext null

        if (currentFragment.seriesSlug != seriesSlug) {
            return@withContext null
        }

        val fragmentsInSeries = fragmentRepository.getAll()
            .filter { it.seriesSlug == seriesSlug }
            .filter { it.seriesPart != null }
            .sortedBy { it.seriesPart }

        val parts = fragmentsInSeries.mapIndexed { index, fragment ->
            SeriesPart(
                fragment = fragment,
                partNumber = fragment.seriesPart ?: (index + 1),
                partTitle = fragment.seriesTitle
            )
        }

        val currentPart = parts.find { it.fragment.slug == fragmentSlug }
        val currentPartIndex = parts.indexOfFirst { it.fragment.slug == fragmentSlug }

        SeriesNavigation(
            series = series,
            parts = parts,
            currentPart = currentPart,
            previousPart = if (currentPartIndex > 0) parts[currentPartIndex - 1] else null,
            nextPart = if (currentPartIndex < parts.size - 1) parts[currentPartIndex + 1] else null,
            totalParts = parts.size
        )
    }

    override suspend fun getNextPartInSeries(
        seriesSlug: String,
        currentPart: Int,
        fragmentRepository: FragmentRepository
    ): Fragment? = withContext(Dispatchers.IO) {
        fragmentRepository.getAll()
            .filter { it.seriesSlug == seriesSlug }
            .filter { it.seriesPart != null }
            .sortedBy { it.seriesPart }
            .firstOrNull { it.seriesPart == currentPart + 1 }
    }

    override suspend fun getPreviousPartInSeries(
        seriesSlug: String,
        currentPart: Int,
        fragmentRepository: FragmentRepository
    ): Fragment? = withContext(Dispatchers.IO) {
        if (currentPart <= 1) return@withContext null

        fragmentRepository.getAll()
            .filter { it.seriesSlug == seriesSlug }
            .filter { it.seriesPart != null }
            .sortedBy { it.seriesPart }
            .firstOrNull { it.seriesPart == currentPart - 1 }
    }

    override suspend fun reload() {
        invalidateCache()
    }

    private fun loadSeries(): List<ContentSeries> {
        if (cachedSeries.isEmpty() || lastLoaded == LocalDateTime.MIN) {
            val files = File(basePath.toFile(), "series").listFiles { file ->
                file.extension == extension.removePrefix(".").removePrefix("yml") ||
                file.extension == extension.removePrefix(".").removePrefix("yaml")
            } ?: emptyArray()

            cachedSeries = files.mapNotNull { parseSeriesFile(it) }
                .sortedBy { it.order }
            lastLoaded = LocalDateTime.now()
        }

        return cachedSeries
    }

    private fun parseSeriesFile(file: File): ContentSeries? {
        return try {
            val content = file.readText()
            val data = yaml.load<Map<String, Any>>(content)

            ContentSeries(
                slug = data["slug"] as? String ?: file.nameWithoutExtension,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String,
                status = (data["status"] as? String)?.let { SeriesStatus.valueOf(it.uppercase()) } ?: SeriesStatus.ACTIVE,
                order = (data["order"] as? Int) ?: 0,
                tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                categories = (data["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                createdAt = (data["createdAt"] as? String)?.let { LocalDateTime.parse(it, formatter) } ?: LocalDateTime.now(),
                updatedAt = (data["updatedAt"] as? String)?.let { LocalDateTime.parse(it, formatter) } ?: LocalDateTime.now(),
                metadata = (data["metadata"] as? Map<String, Any>) ?: emptyMap()
            )
        } catch (e: Exception) {
            logger.error("Failed to parse series file: ${file.name}", e)
            null
        }
    }

    private fun saveSeries(series: ContentSeries) {
        val seriesDir = File(basePath.toFile(), "series")
        if (!seriesDir.exists()) {
            seriesDir.mkdirs()
        }

        val file = File(seriesDir, "${series.slug}$extension")
        val data = mapOf(
            "slug" to series.slug,
            "title" to series.title,
            "description" to series.description,
            "status" to series.status.name.lowercase(),
            "order" to series.order,
            "tags" to series.tags,
            "categories" to series.categories,
            "createdAt" to series.createdAt.format(formatter),
            "updatedAt" to series.updatedAt.format(formatter),
            "metadata" to series.metadata
        )

        val yamlContent = yaml.dump(data)
        file.writeText(yamlContent)
        logger.info("Saved series: ${series.slug}")
    }

    private fun getSeriesFile(slug: String): File? {
        val seriesDir = File(basePath.toFile(), "series")
        if (!seriesDir.exists()) return null

        val files = seriesDir.listFiles { file ->
            file.nameWithoutExtension == slug
        }

        return files?.firstOrNull()
    }

    private fun cacheUpdatedSeries(updatedSeries: ContentSeries) {
        val index = cachedSeries.indexOfFirst { it.slug == updatedSeries.slug }
        if (index >= 0) {
            cachedSeries = cachedSeries.toMutableList().apply { this[index] = updatedSeries }
        } else {
            cachedSeries = cachedSeries + updatedSeries
        }
    }

    private fun invalidateCache() {
        cachedSeries = emptyList()
        lastLoaded = LocalDateTime.MIN
    }
}
