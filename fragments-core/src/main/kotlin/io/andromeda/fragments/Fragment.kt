package io.andromeda.fragments

import java.time.LocalDateTime

data class StatusChangeHistory(
    val fromStatus: FragmentStatus,
    val toStatus: FragmentStatus,
    val changedAt: LocalDateTime = LocalDateTime.now(),
    val changedBy: String? = null,
    val reason: String? = null
)

enum class FragmentStatus {
    DRAFT,
    REVIEW,
    APPROVED,
    PUBLISHED,
    SCHEDULED,
    ARCHIVED,
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

        fun canTransition(from: FragmentStatus, to: FragmentStatus): Boolean {
            return validTransitions[from]?.contains(to) ?: false
        }

        fun getValidTransitions(from: FragmentStatus): Set<FragmentStatus> {
            return validTransitions[from] ?: emptySet()
        }
    }
}

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
    val hasMoreTag: Boolean
        get() = content.contains("<!--more-->", ignoreCase = true) ||
                content.contains("<!-- more -->", ignoreCase = true)

    val previewTextOnly: String
        get() = preview.replace(Regex("<[^>]*>"), "").trim()

    val contentTextOnly: String
        get() = if (hasMoreTag) {
            content.substringBefore("<!--more-->")
                .substringBefore("<!-- more -->")
                .replace(Regex("<[^>]*>"), "")
                .trim()
        } else {
            content.replace(Regex("<[^>]*>"), "").trim()
        }

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
