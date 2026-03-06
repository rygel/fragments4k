package io.andromeda.fragments.test

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentStatus
import io.andromeda.fragments.StatusChangeHistory
import java.time.LocalDateTime

/**
 * Factory for creating test Fragment objects
 */
object FragmentFactory {
    
    /**
     * Create a basic fragment with required fields
     */
    fun create(
        title: String = "Test Fragment",
        slug: String = "test-fragment",
        content: String = "<p>Test content</p>",
        categories: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        seriesSlug: String? = null,
        seriesPart: Int? = null,
        seriesTitle: String? = null
    ): Fragment {
        return Fragment(
            title = title,
            slug = slug,
            date = LocalDateTime.now(),
            status = FragmentStatus.PUBLISHED,
            publishDate = null,
            expiryDate = null,
            preview = "Test preview",
            content = content,
            frontMatter = emptyMap(),
            visible = true,
            template = "default",
            categories = categories,
            tags = tags,
            order = 1,
            language = "en",
            languages = emptyMap(),
            author = "Test Author",
            authorIds = emptyList(),
            seriesSlug = seriesSlug,
            seriesPart = seriesPart,
            seriesTitle = seriesTitle,
            statusChangeHistory = emptyList()
        )
    }
    
    /**
     * Builder for creating fragments with custom configuration
     */
    class Builder {
        private var title: String = "Test Fragment"
        private var slug: String = "test-fragment"
        private var date: LocalDateTime = LocalDateTime.now()
        private var status: FragmentStatus = FragmentStatus.PUBLISHED
        private var publishDate: LocalDateTime? = null
        private var expiryDate: LocalDateTime? = null
        private var preview: String = "Test preview"
        private var content: String = "<p>Test content</p>"
        private var frontMatter: Map<String, Any> = emptyMap()
        private var visible: Boolean = true
        private var template: String = "default"
        private var categories: List<String> = emptyList()
        private var tags: List<String> = emptyList()
        private var order: Int = 1
        private var language: String = "en"
        private var languages: Map<String, String> = emptyMap()
        private var author: String = "Test Author"
        private var authorIds: List<String> = emptyList()
        private var seriesSlug: String? = null
        private var seriesPart: Int? = null
        private var seriesTitle: String? = null
        private var statusChangeHistory: List<StatusChangeHistory> = emptyList()
        
        fun title(title: String) = apply { this.title = title }
        
        fun slug(slug: String) = apply { this.slug = slug }
        
        fun date(date: LocalDateTime) = apply { this.date = date }
        
        fun status(status: FragmentStatus) = apply { this.status = status }
        
        fun publishDate(publishDate: LocalDateTime) = apply { this.publishDate = publishDate }
        
        fun expiryDate(expiryDate: LocalDateTime) = apply { this.expiryDate = expiryDate }
        
        fun preview(preview: String) = apply { this.preview = preview }
        
        fun content(content: String) = apply { this.content = content }
        
        fun frontMatter(frontMatter: Map<String, Any>) = apply { this.frontMatter = frontMatter }
        
        fun visible(visible: Boolean) = apply { this.visible = visible }
        
        fun template(template: String) = apply { this.template = template }
        
        fun categories(categories: List<String>) = apply { this.categories = categories }
        
        fun tags(tags: List<String>) = apply { this.tags = tags }
        
        fun order(order: Int) = apply { this.order = order }
        
        fun language(language: String) = apply { this.language = language }
        
        fun languages(languages: Map<String, String>) = apply { this.languages = languages }
        
        fun author(author: String) = apply { this.author = author }
        
        fun authorIds(authorIds: List<String>) = apply { this.authorIds = authorIds }
        
        fun seriesSlug(seriesSlug: String) = apply { this.seriesSlug = seriesSlug }
        
        fun seriesPart(seriesPart: Int?) = apply { this.seriesPart = seriesPart }

        fun seriesTitle(seriesTitle: String?) = apply { this.seriesTitle = seriesTitle }

        fun statusChangeHistory(statusChangeHistory: List<StatusChangeHistory>) = apply { this.statusChangeHistory = statusChangeHistory }
        
        fun build(): Fragment {
            return Fragment(
                title = title,
                slug = slug,
                date = date,
                status = status,
                publishDate = publishDate,
                expiryDate = expiryDate,
                preview = preview,
                content = content,
                frontMatter = frontMatter,
                visible = visible,
                template = template,
                categories = categories,
                tags = tags,
                order = order,
                language = language,
                languages = languages,
                author = author,
                authorIds = authorIds,
                seriesSlug = seriesSlug,
                seriesPart = seriesPart,
                seriesTitle = seriesTitle,
                statusChangeHistory = statusChangeHistory
            )
        }
    }
    
    /**
     * Create a published fragment
     */
    fun published(): Fragment {
        return Builder().status(FragmentStatus.PUBLISHED).build()
    }

    /**
     * Create a draft fragment
     */
    fun draft(): Fragment {
        return Builder()
            .status(FragmentStatus.DRAFT)
            .visible(false)
            .build()
    }

    /**
     * Create an archived fragment
     */
    fun archived(): Fragment {
        return Builder().status(FragmentStatus.ARCHIVED).build()
    }

    /**
     * Create a scheduled fragment
     */
    fun scheduled(publishDate: LocalDateTime): Fragment {
        return Builder()
            .status(FragmentStatus.SCHEDULED)
            .publishDate(publishDate)
            .build()
    }

    /**
     * Create a fragment with expiration
     */
    fun expiring(expiryDate: LocalDateTime): Fragment {
        return Builder()
            .status(FragmentStatus.PUBLISHED)
            .expiryDate(expiryDate)
            .build()
    }
    
    /**
     * Create a fragment with categories
     */
    fun withCategories(vararg categories: String): Fragment {
        return create(categories = categories.toList())
    }
    
    /**
     * Create a fragment with tags
     */
    fun withTags(vararg tags: String): Fragment {
        return create(tags = tags.toList())
    }
    
    /**
     * Create a fragment with series
     */
    fun withSeries(
        seriesSlug: String,
        seriesPart: Int? = null,
        seriesTitle: String? = null
    ): Fragment {
        return create(
            seriesSlug = seriesSlug,
            seriesPart = seriesPart,
            seriesTitle = seriesTitle
        )
    }
    
    /**
     * Create multiple fragments
     */
    fun createMany(count: Int): List<Fragment> {
        return (1..count).map { i ->
            create(
                title = "Test Fragment $i",
                slug = "test-fragment-$i"
            )
        }
    }
    
    /**
     * Create a fragment with all fields populated
     */
    fun complete(): Fragment {
        return Builder()
            .title("Complete Test Fragment")
            .slug("complete-test-fragment")
            .content("<p>This is a complete test fragment with all fields populated.</p>")
            .categories(listOf("technology", "kotlin"))
            .tags(listOf("test", "example"))
            .author("Complete Test Author")
            .authorIds(listOf("author1", "author2"))
            .seriesSlug("test-series")
            .seriesPart(1)
            .seriesTitle("Test Series")
            .build()
    }
}

/**
 * Fragment status change for testing
 */
data class FragmentStatusChange(
    val status: FragmentStatus,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val changedBy: String? = null,
    val reason: String? = null
) {
    fun toStatusChangeHistory(): StatusChangeHistory {
        return StatusChangeHistory(
            fromStatus = FragmentStatus.DRAFT,
            toStatus = status,
            changedAt = timestamp,
            changedBy = changedBy,
            reason = reason
        )
    }
}
