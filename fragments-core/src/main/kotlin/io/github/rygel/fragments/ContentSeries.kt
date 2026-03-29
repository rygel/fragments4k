package io.github.rygel.fragments

import java.time.LocalDateTime

/**
 * A named, ordered collection of [Fragment] entries published as a series.
 *
 * Fragments belong to a series via [Fragment.seriesSlug] and [Fragment.seriesPart].
 * Use [SeriesNavigation] in templates to render prev/next links and a progress
 * indicator within the series.
 *
 * @property slug URL-safe identifier for the series landing page.
 * @property title Display name of the series.
 * @property description Optional introductory text shown on the series index page.
 * @property status Controls whether the series is publicly visible.
 * @property order Sort key when multiple series are listed together.
 * @property tags Inherited tags applied to all parts of this series.
 * @property categories Inherited categories applied to all parts.
 * @property metadata Arbitrary extra key/value pairs for template use.
 */
data class ContentSeries(
    val slug: String,
    val title: String,
    val description: String? = null,
    val status: SeriesStatus = SeriesStatus.ACTIVE,
    val order: Int = 0,
    val tags: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val metadata: Map<String, Any> = emptyMap()
) {
    val isActive: Boolean
        get() = status == SeriesStatus.ACTIVE

    val statusText: String
        get() = status.name.lowercase().replaceFirstChar { it.uppercase() }
}

/** Publication state of a [ContentSeries]. */
enum class SeriesStatus {
    /** Visible and accepting new parts. */
    ACTIVE,

    /** Hidden from public listings. */
    INACTIVE,

    /** Work-in-progress; not yet published. */
    DRAFT;

    val isActive: Boolean
        get() = this == ACTIVE

    val isInactive: Boolean
        get() = this == INACTIVE

    val isDraft: Boolean
        get() = this == DRAFT
}

/**
 * A single part of a [ContentSeries], pairing a [Fragment] with its position.
 *
 * @property fragment The content fragment for this part.
 * @property partNumber 1-based position within the series.
 * @property partTitle Optional display title override (falls back to "Part N" when absent).
 */
data class SeriesPart(
    val fragment: Fragment,
    val partNumber: Int,
    val partTitle: String? = null
) {
    val isNext: Boolean
        get() = partNumber == 1

    val isLast: Boolean
        get() = false
}

/**
 * Navigation context for rendering a series page: the full ordered part list,
 * the current part, and direct links to the adjacent parts.
 *
 * @property series The [ContentSeries] this navigation belongs to.
 * @property parts All parts in order.
 * @property currentPart The part currently being viewed; `null` on the series index page.
 * @property previousPart The part before [currentPart], or `null` if on the first part.
 * @property nextPart The part after [currentPart], or `null` if on the last part.
 * @property totalParts Total number of parts in the series.
 */
data class SeriesNavigation(
    val series: ContentSeries,
    val parts: List<SeriesPart>,
    val currentPart: SeriesPart?,
    val previousPart: SeriesPart?,
    val nextPart: SeriesPart?,
    val totalParts: Int
) {
    /** `true` when [currentPart] is the first entry in the series. */
    val isFirstPart: Boolean
        get() = currentPart?.partNumber == 1

    /** `true` when [currentPart] is the last entry in the series. */
    val isLastPart: Boolean
        get() = currentPart?.partNumber == totalParts

    /** Completion percentage (0–100) of the reader's progress through the series. */
    val progress: Int
        get() = if (totalParts > 0) {
            ((currentPart?.partNumber ?: 0) * 100) / totalParts
        } else {
            0
        }
}
