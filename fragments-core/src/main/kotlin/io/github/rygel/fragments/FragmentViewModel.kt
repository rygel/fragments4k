package io.github.rygel.fragments

import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * A single heading entry extracted from a fragment's content for building a
 * table-of-contents widget.
 *
 * @property level Heading depth: 1 = `<h1>`, 2 = `<h2>`, etc.
 * @property title Plain-text heading content.
 * @property anchor URL-safe anchor slug derived from [title].
 */
data class TableOfContentsItem(
    val level: Int,
    val title: String,
    val anchor: String
)

/**
 * Presentation model that wraps a [Fragment] with computed properties needed by
 * templates: reading time, relationships (prev/next/related), and HTMX-aware
 * partial rendering detection.
 *
 * Passed directly into JTE (or any other) templates so that template code stays
 * free of business logic.
 *
 * @property fragment The underlying domain object.
 * @property isPartialRender `true` when only an HTML fragment (not a full page)
 *   should be rendered — set automatically via [fromHtmxRequest].
 * @property pageTitle Override for the `<title>` element; falls back to [Fragment.title].
 * @property additionalContext Arbitrary extra values made available to templates
 *   (e.g. site-wide config, feature flags).
 * @property relationships Pre-loaded relationship data; `null` means not loaded yet.
 */
data class FragmentViewModel(
    val fragment: Fragment,
    val isPartialRender: Boolean = false,
    val pageTitle: String? = null,
    val additionalContext: Map<String, Any> = emptyMap(),
    private val allFragments: List<Fragment> = emptyList(),
    val relationships: ContentRelationships? = null
) {
    companion object {
        const val HTMX_REQUEST_HEADER = "HX-Request"
        const val HTMX_CURRENT_URL_HEADER = "HX-Current-URL"

        /** Average adult reading speed used for [FragmentViewModel.readingTime] calculation. */
        const val WORDS_PER_MINUTE = 225
    }

    /**
     * Returns a copy of this view model with [isPartialRender] derived from the
     * presence of an `HX-Request: true` header — wire this up in your framework
     * adapter's request handler.
     */
    fun fromHtmxRequest(headers: Map<String, String>): FragmentViewModel {
        val isHtmxRequest = headers[HTMX_REQUEST_HEADER]?.lowercase() == "true"
        return copy(isPartialRender = isHtmxRequest)
    }

    val title: String
        get() = pageTitle ?: fragment.title

    val hasPreviousPost: Boolean
        get() = relationships?.previous != null

    val hasNextPost: Boolean
        get() = relationships?.next != null

    val previousPost: Fragment?
        get() = relationships?.previous

    val nextPost: Fragment?
        get() = relationships?.next

    val relationshipRelatedPosts: List<Fragment>
        get() = relationships?.allRelated ?: emptyList()

    val translations: Map<String, Fragment>
        get() = relationships?.translations ?: emptyMap()

    val hasRelationships: Boolean
        get() = relationships?.hasRelationships ?: false

    val content: String
        get() = fragment.content

    val preview: String
        get() = fragment.preview

    val slug: String
        get() = fragment.slug

    val template: String
        get() = fragment.template

    val date
        get() = fragment.date

    val tags
        get() = fragment.tags

    val categories
        get() = fragment.categories

    val author: String?
        get() = fragment.author

    /**
     * Converts the fragment's authoring [Fragment.date] (UTC) to the given [zoneId].
     *
     * All dates stored in [Fragment] are UTC. Use this method — or its siblings —
     * to produce a user-local [ZonedDateTime] for display in templates.
     *
     * Example (JTE template):
     * ```
     * ${model.dateInZone(ZoneId.of("Europe/Berlin")).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}
     * ```
     */
    fun dateInZone(zoneId: ZoneId): ZonedDateTime? =
        fragment.date?.atZone(ZoneOffset.UTC)?.withZoneSameInstant(zoneId)

    /**
     * Converts [Fragment.publishDate] (UTC) to the given [zoneId].
     * Returns `null` when [Fragment.publishDate] is not set.
     */
    fun publishDateInZone(zoneId: ZoneId): ZonedDateTime? =
        fragment.publishDate?.atZone(ZoneOffset.UTC)?.withZoneSameInstant(zoneId)

    /**
     * Converts [Fragment.expiryDate] (UTC) to the given [zoneId].
     * Returns `null` when [Fragment.expiryDate] is not set.
     */
    fun expiryDateInZone(zoneId: ZoneId): ZonedDateTime? =
        fragment.expiryDate?.atZone(ZoneOffset.UTC)?.withZoneSameInstant(zoneId)

    /** Estimated reading time based on [WORDS_PER_MINUTE]. */
    val readingTime: ReadingTime
        get() = calculateReadingTime()

    /** Human-readable reading time string, e.g. `"3m read"` or `"45s read"`. */
    val formattedReadingTime: String
        get() = readingTime.text

    /**
     * Breakdown of the estimated time required to read this fragment's content.
     *
     * @property minutes Whole minutes component.
     * @property seconds Remaining seconds component.
     * @property text Formatted display string (e.g. `"3m 12s read"`).
     */
    data class ReadingTime(
        val minutes: Int,
        val seconds: Int,
        val text: String
    )

    private fun calculateReadingTime(): ReadingTime {
        val words = content.split(Regex("\\s+")).size
        val totalSeconds = (words.toDouble() / WORDS_PER_MINUTE) * 60
        val minutes = totalSeconds.toInt() / 60
        val seconds = totalSeconds.toInt() % 60
        
        val text = when {
            minutes == 0 -> "${seconds}s read"
            seconds == 0 -> "${minutes}m read"
            else -> "${minutes}m ${seconds}s read"
        }
        
        return ReadingTime(minutes, seconds, text)
    }

    private fun extractTableOfContents(): List<TableOfContentsItem> {
        val items = mutableListOf<TableOfContentsItem>()
        val headerPattern = Regex("^(#{1,6})\\s+(.+)$", RegexOption.MULTILINE)
        
        headerPattern.findAll(content).forEach { match ->
            val level = match.groupValues[1].length
            val title = match.groupValues[2].trim()
            val anchor = title.lowercase()
                .replace(Regex("[^a-z0-9\\s-]"), "")
                .replace(Regex("\\s+"), "-")
            
            items.add(TableOfContentsItem(level, title, anchor))
        }
        
        return items
    }

    private fun findRelatedPosts(): List<Fragment> {
        if (allFragments.isEmpty()) return emptyList()

        val scoredFragments = allFragments
            .filter { it.slug != fragment.slug && it.visible }
            .map { other ->
                var score = 0.0
                
                tags.forEach { tag ->
                    if (other.tags.contains(tag)) score += 2.0
                }
                
                categories.forEach { category ->
                    if (other.categories.contains(category)) score += 1.5
                }
                
                if (fragment.template == "blog" && other.template == "blog") {
                    score += 0.5
                }
                
                other to score
            }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        
        return scoredFragments
    }
}
