package io.andromeda.fragments

import java.time.LocalDateTime

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

enum class SeriesStatus {
    ACTIVE,
    INACTIVE,
    DRAFT;

    val isActive: Boolean
        get() = this == ACTIVE

    val isInactive: Boolean
        get() = this == INACTIVE

    val isDraft: Boolean
        get() = this == DRAFT
}

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

data class SeriesNavigation(
    val series: ContentSeries,
    val parts: List<SeriesPart>,
    val currentPart: SeriesPart?,
    val previousPart: SeriesPart?,
    val nextPart: SeriesPart?,
    val totalParts: Int
) {
    val isFirstPart: Boolean
        get() = currentPart?.partNumber == 1

    val isLastPart: Boolean
        get() = currentPart?.partNumber == totalParts

    val progress: Int
        get() = if (totalParts > 0) {
            ((currentPart?.partNumber ?: 0) * 100) / totalParts
        } else {
            0
        }
}
