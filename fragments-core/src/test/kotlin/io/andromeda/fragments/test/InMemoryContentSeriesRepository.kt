package io.github.rygel.fragments.test

import io.github.rygel.fragments.*

class InMemoryContentSeriesRepository : ContentSeriesRepository {
    private val seriesList = mutableListOf<ContentSeries>()

    override suspend fun getAll(): List<ContentSeries> = seriesList.toList()

    override suspend fun getBySlug(slug: String): ContentSeries? =
        seriesList.find { it.slug == slug }

    override suspend fun getActiveSeries(): List<ContentSeries> =
        seriesList.filter { it.status == SeriesStatus.ACTIVE }

    override suspend fun getSeriesByTag(tag: String): List<ContentSeries> =
        seriesList.filter { it.tags.contains(tag.lowercase()) }

    override suspend fun getSeriesByCategory(category: String): List<ContentSeries> =
        seriesList.filter { it.categories.contains(category.lowercase()) }

    override suspend fun createSeries(series: ContentSeries): Result<ContentSeries> {
        if (seriesList.any { it.slug == series.slug }) {
            return Result.failure(IllegalArgumentException("Series already exists: ${series.slug}"))
        }

        val newSeries = series.copy(
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )
        seriesList.add(newSeries)
        return Result.success(newSeries)
    }

    override suspend fun updateSeries(series: ContentSeries): Result<ContentSeries> {
        val index = seriesList.indexOfFirst { it.slug == series.slug }
        if (index < 0) {
            return Result.failure(IllegalArgumentException("Series not found: ${series.slug}"))
        }

        val updatedSeries = series.copy(
            updatedAt = java.time.LocalDateTime.now()
        )
        seriesList[index] = updatedSeries
        return Result.success(updatedSeries)
    }

    override suspend fun deleteSeries(slug: String): Result<Boolean> {
        val removed = seriesList.removeIf { it.slug == slug }
        return Result.success(removed)
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
    ): SeriesNavigation? {
        val series = getBySlug(seriesSlug) ?: return null
        val currentFragment = fragmentRepository.getBySlug(fragmentSlug) ?: return null

        if (currentFragment.seriesSlug != seriesSlug) {
            return null
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

        return SeriesNavigation(
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
    ): Fragment? {
        return fragmentRepository.getAll()
            .filter { it.seriesSlug == seriesSlug }
            .filter { it.seriesPart != null }
            .sortedBy { it.seriesPart }
            .firstOrNull { it.seriesPart == currentPart + 1 }
    }

    override suspend fun getPreviousPartInSeries(
        seriesSlug: String,
        currentPart: Int,
        fragmentRepository: FragmentRepository
    ): Fragment? {
        if (currentPart <= 1) return null

        return fragmentRepository.getAll()
            .filter { it.seriesSlug == seriesSlug }
            .filter { it.seriesPart != null }
            .sortedBy { it.seriesPart }
            .firstOrNull { it.seriesPart == currentPart - 1 }
    }

    override suspend fun reload() {
    }
}
