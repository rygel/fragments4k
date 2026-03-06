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
        slug: String = "test-series",
        description: String = "A test content series"
    ): ContentSeries {
        return ContentSeries(
            title = title,
            slug = slug,
            description = description,
            status = SeriesStatus.ACTIVE,
            order = 0,
            tags = emptyList(),
            categories = emptyList(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            metadata = emptyMap()
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
        private var order: Int = 0
        private var tags: List<String> = emptyList()
        private var categories: List<String> = emptyList()
        private var createdAt: LocalDateTime = LocalDateTime.now()
        private var updatedAt: LocalDateTime = LocalDateTime.now()
        private var metadata: Map<String, Any> = emptyMap()
        
        fun title(title: String) = apply { this.title = title }

        fun slug(slug: String) = apply { this.slug = slug }

        fun description(description: String) = apply { this.description = description }

        fun status(status: SeriesStatus) = apply { this.status = status }

        fun order(order: Int) = apply { this.order = order }

        fun tags(tags: List<String>) = apply { this.tags = tags }

        fun categories(categories: List<String>) = apply { this.categories = categories }

        fun createdAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

        fun updatedAt(updatedAt: LocalDateTime) = apply { this.updatedAt = updatedAt }

        fun metadata(metadata: Map<String, Any>) = apply { this.metadata = metadata }
        
        fun build(): ContentSeries {
            return ContentSeries(
                title = title,
                slug = slug,
                description = description,
                status = status,
                order = order,
                tags = tags,
                categories = categories,
                createdAt = createdAt,
                updatedAt = updatedAt,
                metadata = metadata
            )
        }
    }
    
    /**
     * Create an active series
     */
    fun active(): ContentSeries {
        return Builder().status(SeriesStatus.ACTIVE).build()
    }

    /**
     * Create an inactive series
     */
    fun inactive(): ContentSeries {
        return Builder().status(SeriesStatus.INACTIVE).build()
    }

    /**
     * Create a draft series
     */
    fun draft(): ContentSeries {
        return Builder().status(SeriesStatus.DRAFT).build()
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
