package io.github.rygel.fragments

import java.time.LocalDateTime

interface ContentSeriesRepository {
    suspend fun getAll(): List<ContentSeries>
    suspend fun getBySlug(slug: String): ContentSeries?
    suspend fun getActiveSeries(): List<ContentSeries>
    suspend fun getSeriesByTag(tag: String): List<ContentSeries>
    suspend fun getSeriesByCategory(category: String): List<ContentSeries>
    suspend fun createSeries(series: ContentSeries): Result<ContentSeries>
    suspend fun updateSeries(series: ContentSeries): Result<ContentSeries>
    suspend fun deleteSeries(slug: String): Result<Boolean>
    suspend fun getFragmentsInSeries(seriesSlug: String, fragmentRepository: FragmentRepository): List<Fragment>
    suspend fun getSeriesNavigation(seriesSlug: String, fragmentSlug: String, fragmentRepository: FragmentRepository): SeriesNavigation?
    suspend fun getNextPartInSeries(seriesSlug: String, currentPart: Int, fragmentRepository: FragmentRepository): Fragment?
    suspend fun getPreviousPartInSeries(seriesSlug: String, currentPart: Int, fragmentRepository: FragmentRepository): Fragment?
    suspend fun reload()
}
