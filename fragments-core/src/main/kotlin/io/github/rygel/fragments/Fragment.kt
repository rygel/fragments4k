package io.github.rygel.fragments

import java.time.LocalDateTime

/**
 * Records a single status transition for audit trail purposes.
 *
 * Instances are persisted in the fragment's front matter under the
 * `statusChangeHistory` key so the full lifecycle is visible in the source file.
 *
 * @property fromStatus The status before the transition.
 * @property toStatus The status after the transition.
 * @property changedAt Wall-clock time of the transition; defaults to now.
 * @property changedBy Optional identifier of the actor (user ID, "system", etc.).
 * @property reason Optional free-text explanation for the change.
 */
data class StatusChangeHistory(
    val fromStatus: FragmentStatus,
    val toStatus: FragmentStatus,
    val changedAt: LocalDateTime = LocalDateTime.now(),
    val changedBy: String? = null,
    val reason: String? = null
)

/**
 * Publication lifecycle states for a [Fragment].
 *
 * Only the transitions listed in [validTransitions] are permitted by default;
 * pass `force = true` to [FragmentRepository.updateFragmentStatus] to bypass the
 * guard when an emergency override is required.
 *
 * Typical flow: `DRAFT` → `REVIEW` → `APPROVED` → `PUBLISHED`.
 * Scheduled publication: `APPROVED` → `SCHEDULED` → `PUBLISHED` (automatic).
 * Content retirement: `PUBLISHED` → `ARCHIVED` or `EXPIRED` (automatic).
 */
enum class FragmentStatus {
    /** Being written; not visible to readers. */
    DRAFT,

    /** Submitted for editorial review; not visible to readers. */
    REVIEW,

    /** Approved for publication; not yet live. */
    APPROVED,

    /** Live and visible to readers. */
    PUBLISHED,

    /** Queued for automatic publication at [Fragment.publishDate]. */
    SCHEDULED,

    /** Taken offline; retained for historical reference. */
    ARCHIVED,

    /** Past its [Fragment.expiryDate]; no longer shown. */
    EXPIRED;

    companion object {
        private val validTransitions = mapOf(
            DRAFT to setOf(REVIEW, APPROVED, PUBLISHED, SCHEDULED, ARCHIVED),
            REVIEW to setOf(DRAFT, APPROVED, ARCHIVED),
            APPROVED to setOf(DRAFT, PUBLISHED, SCHEDULED, ARCHIVED),
            PUBLISHED to setOf(ARCHIVED, EXPIRED, DRAFT),
            SCHEDULED to setOf(PUBLISHED, DRAFT, ARCHIVED),
            ARCHIVED to setOf(PUBLISHED, DRAFT, EXPIRED, REVIEW, APPROVED),
            EXPIRED to setOf(DRAFT, SCHEDULED, PUBLISHED, ARCHIVED, REVIEW, APPROVED)
        )

        /**
         * Returns `true` if transitioning [from] → [to] is a valid lifecycle move.
         * Use [FragmentRepository.updateFragmentStatus] with `force = true` to
         * bypass this check when needed.
         */
        fun canTransition(from: FragmentStatus, to: FragmentStatus): Boolean {
            return validTransitions[from]?.contains(to) ?: false
        }

        /**
         * Returns all statuses that [from] may legally transition to.
         */
        fun getValidTransitions(from: FragmentStatus): Set<FragmentStatus> {
            return validTransitions[from] ?: emptySet()
        }
    }
}

/**
 * Core domain model representing a single piece of content.
 *
 * A fragment is loaded from a Markdown file with a YAML front matter block.
 * The [content] property holds the rendered HTML; [frontMatter] retains the raw
 * key/value map for any custom metadata your templates may need.
 *
 * Example front matter:
 * ```yaml
 * ---
 * title: "Hello World"
 * slug: hello-world
 * status: PUBLISHED
 * date: 2024-01-15
 * tags: [kotlin, jvm]
 * ---
 * ```
 *
 * @property title Human-readable title; falls back to the file name if absent.
 * @property slug URL-safe identifier used for routing (e.g. `/hello-world`).
 * @property status Current publication lifecycle state; defaults to [FragmentStatus.PUBLISHED].
 * @property date Authoring date, used for sorting and archive URLs.
 * @property publishDate When a [FragmentStatus.SCHEDULED] fragment goes live.
 * @property expiryDate Optional date after which the fragment is no longer shown.
 * @property preview Rendered HTML excerpt shown in listing pages. Auto-extracted
 *   from content up to the `<!--more-->` tag when not specified in front matter.
 * @property content Full rendered HTML body.
 * @property frontMatter Raw YAML front matter map; available to templates for
 *   custom metadata.
 * @property visible When `false` the fragment is excluded from [FragmentRepository.getAllVisible]
 *   even if [FragmentStatus.PUBLISHED].
 * @property template Template identifier passed to the rendering engine; defaults to `"default"`.
 * @property categories Lowercase category slugs.
 * @property tags Lowercase tag slugs.
 * @property order Sort key for manually ordered listings (lower = first).
 * @property language BCP-47 language tag (e.g. `"en"`, `"de"`); defaults to `"en"`.
 * @property languages Map of language tag → slug for alternate-language versions of
 *   this content (used by the relationship engine to surface translations).
 * @property author Legacy single-author field (plain name or ID string).
 * @property authorIds Preferred multi-author list; takes precedence over [author].
 * @property statusChangeHistory Ordered audit trail of [StatusChangeHistory] entries.
 * @property seriesSlug Slug of the [ContentSeries] this fragment belongs to, if any.
 * @property seriesPart Position within the series (1-based).
 * @property seriesTitle Optional display title for this part (overrides "Part N").
 */
data class Fragment(
    val title: String,
    val slug: String,
    val status: FragmentStatus = FragmentStatus.PUBLISHED,
    val date: LocalDateTime?,
    val publishDate: LocalDateTime?,
    val expiryDate: LocalDateTime? = null,
    val preview: String,
    val content: String,
    val frontMatter: Map<String, Any>,
    val visible: Boolean = true,
    val template: String = "default",
    val categories: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val order: Int = 0,
    val language: String = "en",
    val languages: Map<String, String> = emptyMap(),
    val author: String? = null,
    val authorIds: List<String> = emptyList(),
    val statusChangeHistory: List<StatusChangeHistory> = emptyList(),
    val seriesSlug: String? = null,
    val seriesPart: Int? = null,
    val seriesTitle: String? = null
) {
    /** `true` if the content contains a `<!--more-->` read-more split marker. */
    val hasMoreTag: Boolean
        get() = content.contains("<!--more-->", ignoreCase = true) ||
                content.contains("<!-- more -->", ignoreCase = true)

    /** Plain-text version of [preview] with all HTML tags stripped. */
    val previewTextOnly: String
        get() = preview.replace(Regex("<[^>]*>"), "").trim()

    /**
     * Plain-text content up to the `<!--more-->` marker (or the full body
     * when no marker is present), with all HTML tags stripped.
     */
    val contentTextOnly: String
        get() = if (hasMoreTag) {
            content.substringBefore("<!--more-->")
                .substringBefore("<!-- more -->")
                .replace(Regex("<[^>]*>"), "")
                .trim()
        } else {
            content.replace(Regex("<[^>]*>"), "").trim()
        }

    /**
     * The first entry in [authorIds], or [author] when [authorIds] is empty.
     * Returns `null` if neither field is set.
     */
    val primaryAuthor: String?
        get() = if (authorIds.isNotEmpty()) authorIds[0] else author

    val statusText: String
        get() = status.name.lowercase().replaceFirstChar { it.uppercase() }

    val isPublished: Boolean
        get() = status == FragmentStatus.PUBLISHED

    val isDraft: Boolean
        get() = status == FragmentStatus.DRAFT

    val isReview: Boolean
        get() = status == FragmentStatus.REVIEW

    val isApproved: Boolean
        get() = status == FragmentStatus.APPROVED

    val isScheduled: Boolean
        get() = status == FragmentStatus.SCHEDULED

    val isArchived: Boolean
        get() = status == FragmentStatus.ARCHIVED

    val isExpired: Boolean
        get() = status == FragmentStatus.EXPIRED

    val isInSeries: Boolean
        get() = seriesSlug != null && seriesPart != null

    val seriesPartTitle: String?
        get() = seriesTitle?.takeIf { it.isNotEmpty() } ?: "Part $seriesPart"
}
