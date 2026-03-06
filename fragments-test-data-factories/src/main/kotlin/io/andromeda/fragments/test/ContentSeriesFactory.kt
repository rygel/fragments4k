package io.andromeda.fragments.test

import io.andromeda.fragments.ContentSeries
import io.andromeda.fragments.SeriesStatus
import java.time.LocalDateTime

/**
 * Factory for creating test ContentSeries objects
 */
object ContentSeriesFactory {
    
    /**
     * Create a basic series with required fields
     */
    fun create(
        title: String = "Test Series",
        slug: String = "test-series"
        description: String = "A test content series"
    ): ContentSeries {
        return ContentSeries(
            title = title,
            slug = slug,
            description = description,
            status = SeriesStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )
    }
    
    /**
     * Builder for creating series with custom configuration
     */
    class Builder {
        private var title: String = "Test Series"
        private var slug: String = "test-series"
        private var description: String = "A test content series"
        private var status: SeriesStatus = SeriesStatus.ACTIVE
        private var createdAt: LocalDateTime = LocalDateTime.now()
        private var updatedAt: LocalDateTime? = null
        
        fun title(title: String) = apply { this.title = title }
        
        fun slug(slug: String) = apply { this.slug = slug }
        
        fun description(description: String) = apply { this.description = description }
        
        fun status(status: SeriesStatus) = apply { this.status = status }
        
        fun createdAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }
        
        fun updatedAt(updatedAt: LocalDateTime?) = apply { this.updatedAt = updatedAt }
        
        fun build(): ContentSeries {
            return ContentSeries(
                title = title,
                slug = slug,
                description = description,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
    
    /**
     * Create an active series
     */
    fun active(): ContentSeries {
        return create(status = SeriesStatus.ACTIVE)
    }
    
    /**
     * Create an inactive series
     */
    fun inactive(): ContentSeries {
        return create(status = SeriesStatus.INACTIVE)
    }
    
    /**
     * Create a draft series
     */
    fun draft(): ContentSeries {
        return create(status = SeriesStatus.DRAFT)
    }
    
    /**
     * Create multiple series
     */
    fun createMany(count: Int): List<ContentSeries> {
        return (1..count).map { i ->
            create(
                title = "Test Series $i",
                slug = "test-series-$i",
                description = "A test content series number $i"
            )
        }
    }
}
