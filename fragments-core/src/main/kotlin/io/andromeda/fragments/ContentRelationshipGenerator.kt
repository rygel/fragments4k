package io.github.rygel.fragments

import kotlin.math.min

/**
 * Generates content relationships between fragments
 */
object ContentRelationshipGenerator {

    /**
     * Generates all relationships for a given fragment
     */
    fun generateRelationships(
        currentFragment: Fragment,
        allFragments: List<Fragment>,
        config: RelationshipConfig = RelationshipConfig()
    ): ContentRelationships {
        val publishedFragments = allFragments
            .filter { it.status == FragmentStatus.PUBLISHED }
            .filter { it.slug != currentFragment.slug }

        val previous = findPrevious(currentFragment, publishedFragments)
        val next = findNext(currentFragment, publishedFragments)
        val relatedByTag = findRelatedByTag(currentFragment, publishedFragments, config)
        val relatedByCategory = findRelatedByCategory(currentFragment, publishedFragments, config)
        val relatedByContent = findRelatedByContent(currentFragment, publishedFragments, config)
        val translations = findTranslations(currentFragment, publishedFragments, config)

        return ContentRelationships(
            previous = previous,
            next = next,
            relatedByTag = relatedByTag,
            relatedByCategory = relatedByCategory,
            relatedByContent = relatedByContent,
            translations = translations
        )
    }

    /**
     * Finds the previous fragment by date
     */
    private fun findPrevious(
        currentFragment: Fragment,
        fragments: List<Fragment>
    ): Fragment? {
        val date = currentFragment.date ?: return null
        return fragments
            .filter { it.date != null }
            .filter { it.date!! < date }
            .maxByOrNull { it.date!! }
    }

    /**
     * Finds the next fragment by date
     */
    private fun findNext(
        currentFragment: Fragment,
        fragments: List<Fragment>
    ): Fragment? {
        val date = currentFragment.date ?: return null
        return fragments
            .filter { it.date != null }
            .filter { it.date!! > date }
            .minByOrNull { it.date!! }
    }

    /**
     * Finds fragments related by shared tags
     */
    private fun findRelatedByTag(
        currentFragment: Fragment,
        fragments: List<Fragment>,
        config: RelationshipConfig
    ): List<Fragment> {
        if (currentFragment.tags.isEmpty()) return emptyList()

        val withSharedTags = fragments.filter { fragment ->
            val sharedTags = fragment.tags.intersect(currentFragment.tags.toSet())
            sharedTags.size >= config.minSharedTags
        }

        return withSharedTags
            .filterNot { fragment -> fragment.slug in (currentFragment.frontMatter["exclude"] as? List<*> ?: emptyList<Any>()) }
            .sortedByDescending { calculateTagSimilarity(currentFragment, it) }
            .take(config.maxRelatedByTag)
    }

    /**
     * Finds fragments related by shared categories
     */
    private fun findRelatedByCategory(
        currentFragment: Fragment,
        fragments: List<Fragment>,
        config: RelationshipConfig
    ): List<Fragment> {
        if (currentFragment.categories.isEmpty()) return emptyList()

        val withSharedCategories = fragments.filter { fragment ->
            val sharedCategories = fragment.categories.intersect(currentFragment.categories.toSet())
            sharedCategories.size >= config.minSharedCategories
        }

        return withSharedCategories
            .filterNot { fragment -> fragment.slug in (currentFragment.frontMatter["exclude"] as? List<*> ?: emptyList<Any>()) }
            .sortedByDescending { calculateCategorySimilarity(currentFragment, it) }
            .take(config.maxRelatedByCategory)
    }

    /**
     * Finds fragments related by content references in the current fragment
     */
    private fun findRelatedByContent(
        currentFragment: Fragment,
        fragments: List<Fragment>,
        config: RelationshipConfig
    ): List<Fragment> {
        if (currentFragment.contentTextOnly.isBlank()) return emptyList()

        val referencedSlugs = extractContentReferences(currentFragment)
        if (referencedSlugs.isEmpty()) return emptyList()

        return fragments
            .filter { fragment -> fragment.slug in referencedSlugs }
            .filterNot { fragment -> fragment.slug == currentFragment.slug }
            .take(config.maxRelatedByContent)
    }

    /**
     * Finds translations of the current fragment
     */
    private fun findTranslations(
        currentFragment: Fragment,
        fragments: List<Fragment>,
        config: RelationshipConfig
    ): Map<String, Fragment> {
        if (currentFragment.frontMatter["translation_of"] == null) return emptyMap()

        val translationKey = currentFragment.frontMatter["translation_of"] as? String ?: return emptyMap()

        return fragments
            .filter { it.frontMatter["translation_of"] == translationKey }
            .filterNot { it.slug == currentFragment.slug }
            .filterNot { it.language == currentFragment.language }
            .associateBy { it.language }
    }

    /**
     * Extracts content references from fragment content
     * Looks for patterns like {{fragment:slug}} or {% fragment slug %}
     */
    private fun extractContentReferences(fragment: Fragment): Set<String> {
        val content = fragment.content

        val referencePatterns = listOf(
            """\{\{fragment:([^}]+)\}\}""".toRegex(),
            """\{%\s*fragment\s+([^\s%]+)\s*%\}""".toRegex(),
            """\[fragment:([^\]]+)\]""".toRegex(),
            """<fragment\s+id=["']([^"']+)["']>""".toRegex()
        )

        val references = mutableSetOf<String>()
        referencePatterns.forEach { pattern ->
            pattern.findAll(content).forEach { match ->
                match.groupValues.get(1)?.let { slug ->
                    references.add(slug.trim())
                }
            }
        }

        return references
    }

    /**
     * Calculates similarity score based on shared tags
     */
    private fun calculateTagSimilarity(fragment1: Fragment, fragment2: Fragment): Float {
        if (fragment1.tags.isEmpty() || fragment2.tags.isEmpty()) return 0f

        val intersection = fragment1.tags.intersect(fragment2.tags.toSet()).size
        val union = fragment1.tags.union(fragment2.tags.toSet()).size

        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    /**
     * Calculates similarity score based on shared categories
     */
    private fun calculateCategorySimilarity(fragment1: Fragment, fragment2: Fragment): Float {
        if (fragment1.categories.isEmpty() || fragment2.categories.isEmpty()) return 0f

        val intersection = fragment1.categories.intersect(fragment2.categories.toSet()).size
        val union = fragment1.categories.union(fragment2.categories.toSet()).size

        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    /**
     * Helper extension to get distinct by key
     */
    private fun <T, K> List<T>.distinctBy(selector: (T) -> K): List<T> {
        val seen = mutableSetOf<K>()
        return filter { element ->
            val key = selector(element)
            if (key in seen) {
                false
            } else {
                seen.add(key)
                true
            }
        }
    }
}
