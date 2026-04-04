package io.github.rygel.fragments

import kotlin.math.min

/**
 * Generates content relationships between fragments.
 *
 * [generateRelationships] builds inverted indexes (tag → fragments, category → fragments,
 * slug → fragment, translation_of → fragments) from the supplied fragment list before
 * calling any helper, so that each relationship type resolves candidates in O(k) time
 * where k is the size of the candidate set — not O(n) over the entire corpus.
 */
object ContentRelationshipGenerator {

    private val REFERENCE_PATTERNS = listOf(
        Regex("""\{\{fragment:([^}]+)\}\}"""),
        Regex("""\{%\s*fragment\s+([^\s%]+)\s*%\}"""),
        Regex("""\[fragment:([^\]]+)\]"""),
        Regex("""<fragment\s+id=["']([^"']+)["']>"""),
    )

    /**
     * Generates all relationships for a given fragment.
     *
     * Builds index structures once from [allFragments] so that tag, category, slug, and
     * translation lookups are O(k) rather than repeated O(n) linear scans.
     */
    fun generateRelationships(
        currentFragment: Fragment,
        allFragments: List<Fragment>,
        config: RelationshipConfig = RelationshipConfig()
    ): ContentRelationships {
        val publishedFragments = allFragments
            .filter { it.status == FragmentStatus.PUBLISHED }
            .filter { it.slug != currentFragment.slug }

        // Build indexes once — amortises cost across all relationship lookups below.
        val tagIndex: Map<String, List<Fragment>> = publishedFragments
            .flatMap { fragment -> fragment.tags.map { tag -> tag to fragment } }
            .groupBy({ it.first }, { it.second })

        val categoryIndex: Map<String, List<Fragment>> = publishedFragments
            .flatMap { fragment -> fragment.categories.map { cat -> cat to fragment } }
            .groupBy({ it.first }, { it.second })

        val slugIndex: Map<String, Fragment> = publishedFragments.associateBy { it.slug }

        val translationIndex: Map<String, List<Fragment>> = publishedFragments
            .mapNotNull { fragment ->
                (fragment.frontMatter["translation_of"] as? String)?.let { key -> key to fragment }
            }
            .groupBy({ it.first }, { it.second })

        val previous = findPrevious(currentFragment, publishedFragments)
        val next = findNext(currentFragment, publishedFragments)
        val relatedByTag = findRelatedByTag(currentFragment, tagIndex, config)
        val relatedByCategory = findRelatedByCategory(currentFragment, categoryIndex, config)
        val relatedByContent = findRelatedByContent(currentFragment, slugIndex, config)
        val translations = findTranslations(currentFragment, translationIndex, config)

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
     * Finds the previous fragment by date (the most recent one older than [currentFragment]).
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
     * Finds the next fragment by date (the oldest one newer than [currentFragment]).
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
     * Finds fragments related by shared tags using the pre-built [tagIndex].
     *
     * Candidate fragments are gathered directly from the index rather than scanning
     * all fragments, so the cost is O(|currentTags| × avg_fragments_per_tag).
     */
    private fun findRelatedByTag(
        currentFragment: Fragment,
        tagIndex: Map<String, List<Fragment>>,
        config: RelationshipConfig
    ): List<Fragment> {
        if (currentFragment.tags.isEmpty()) return emptyList()

        val excluded = parseExcludedSlugs(currentFragment.frontMatter["exclude"])

        // Collect candidates via the tag index and count shared-tag hits per slug.
        val hitCount = mutableMapOf<String, Int>()
        val bySlug = mutableMapOf<String, Fragment>()
        for (tag in currentFragment.tags) {
            for (candidate in tagIndex[tag].orEmpty()) {
                if (candidate.slug == currentFragment.slug || candidate.slug in excluded) continue
                hitCount[candidate.slug] = (hitCount[candidate.slug] ?: 0) + 1
                bySlug[candidate.slug] = candidate
            }
        }

        return hitCount
            .filter { (_, count) -> count >= config.minSharedTags }
            .keys
            .mapNotNull { bySlug[it] }
            .sortedByDescending { calculateTagSimilarity(currentFragment, it) }
            .take(config.maxRelatedByTag)
    }

    /**
     * Finds fragments related by shared categories using the pre-built [categoryIndex].
     *
     * Same O(|currentCategories| × avg_fragments_per_category) strategy as [findRelatedByTag].
     */
    private fun findRelatedByCategory(
        currentFragment: Fragment,
        categoryIndex: Map<String, List<Fragment>>,
        config: RelationshipConfig
    ): List<Fragment> {
        if (currentFragment.categories.isEmpty()) return emptyList()

        val excluded = parseExcludedSlugs(currentFragment.frontMatter["exclude"])

        val hitCount = mutableMapOf<String, Int>()
        val bySlug = mutableMapOf<String, Fragment>()
        for (category in currentFragment.categories) {
            for (candidate in categoryIndex[category].orEmpty()) {
                if (candidate.slug == currentFragment.slug || candidate.slug in excluded) continue
                hitCount[candidate.slug] = (hitCount[candidate.slug] ?: 0) + 1
                bySlug[candidate.slug] = candidate
            }
        }

        return hitCount
            .filter { (_, count) -> count >= config.minSharedCategories }
            .keys
            .mapNotNull { bySlug[it] }
            .sortedByDescending { calculateCategorySimilarity(currentFragment, it) }
            .take(config.maxRelatedByCategory)
    }

    /**
     * Finds fragments referenced by slug tokens in [currentFragment]'s content,
     * using the pre-built [slugIndex] for O(|referencedSlugs|) lookups.
     */
    private fun findRelatedByContent(
        currentFragment: Fragment,
        slugIndex: Map<String, Fragment>,
        config: RelationshipConfig
    ): List<Fragment> {
        if (currentFragment.contentTextOnly.isBlank()) return emptyList()

        val referencedSlugs = extractContentReferences(currentFragment)
        if (referencedSlugs.isEmpty()) return emptyList()

        return referencedSlugs
            .mapNotNull { slug -> slugIndex[slug] }
            .take(config.maxRelatedByContent)
    }

    /**
     * Finds alternate-language translations of [currentFragment] using the pre-built
     * [translationIndex] for O(1) lookup by `translation_of` key.
     */
    private fun findTranslations(
        currentFragment: Fragment,
        translationIndex: Map<String, List<Fragment>>,
        config: RelationshipConfig
    ): Map<String, Fragment> {
        val translationKey = currentFragment.frontMatter["translation_of"] as? String
            ?: return emptyMap()

        return translationIndex[translationKey]
            .orEmpty()
            .filterNot { it.language == currentFragment.language }
            .associateBy { it.language }
    }

    /**
     * Extracts content references from fragment content.
     * Looks for patterns like `{{fragment:slug}}`, `{% fragment slug %}`,
     * `[fragment:slug]`, or `<fragment id="slug">`.
     */
    private fun extractContentReferences(fragment: Fragment): Set<String> {
        val references = mutableSetOf<String>()
        REFERENCE_PATTERNS.forEach { pattern ->
            pattern.findAll(fragment.content).forEach { match ->
                references.add(match.groupValues[1].trim())
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
     * Converts the `exclude` front matter value to a set of slugs, handling both
     * a YAML list (`exclude: [slug-a, slug-b]`) and a plain string
     * (`exclude: slug-a`). Returns an empty set when the field is absent or has an
     * unrecognised type so that no fragments are silently filtered out.
     */
    private fun parseExcludedSlugs(value: Any?): Set<String> = when (value) {
        is List<*> -> value.mapNotNull { it?.toString() }.toSet()
        is String -> setOf(value)
        else -> emptySet()
    }

}
