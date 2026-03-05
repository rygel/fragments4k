package io.andromeda.fragments

/**
 * Represents relationships between content fragments
 */
data class ContentRelationships(
    val previous: Fragment? = null,
    val next: Fragment? = null,
    val relatedByTag: List<Fragment> = emptyList(),
    val relatedByCategory: List<Fragment> = emptyList(),
    val relatedByContent: List<Fragment> = emptyList(),
    val translations: Map<String, Fragment> = emptyMap()
) {
    /**
     * Get all related fragments combined and deduplicated
     */
    val allRelated: List<Fragment>
        get() = (relatedByTag + relatedByCategory + relatedByContent)
            .distinctBy { it.slug }
            .filter { it.slug != (previous?.slug) }
            .filter { it.slug != (next?.slug) }

    /**
     * Check if there are any relationships
     */
    val hasRelationships: Boolean
        get() = previous != null || next != null ||
                  allRelated.isNotEmpty() || translations.isNotEmpty()

    /**
     * Check if there is navigation (previous/next)
     */
    val hasNavigation: Boolean
        get() = previous != null || next != null

    /**
     * Check if there are related posts
     */
    val hasRelatedPosts: Boolean
        get() = allRelated.isNotEmpty()

    /**
     * Check if there are translations
     */
    val hasTranslations: Boolean
        get() = translations.isNotEmpty()
}

/**
 * Types of content relationships
 */
enum class RelationshipType {
    PREVIOUS,
    NEXT,
    RELATED_BY_TAG,
    RELATED_BY_CATEGORY,
    RELATED_BY_CONTENT,
    TRANSLATION
}

/**
 * Configuration for content relationship generation
 */
data class RelationshipConfig(
    val maxRelatedByTag: Int = 5,
    val maxRelatedByCategory: Int = 3,
    val maxRelatedByContent: Int = 5,
    val excludeCurrentLanguage: Boolean = false,
    val excludeCurrentAuthor: Boolean = false,
    val minSharedTags: Int = 2,
    val minSharedCategories: Int = 2
)
