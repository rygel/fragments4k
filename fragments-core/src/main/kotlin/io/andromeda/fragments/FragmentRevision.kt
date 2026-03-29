package io.github.rygel.fragments

import java.time.LocalDateTime

data class FragmentRevision(
    val id: String,
    val fragmentSlug: String,
    val version: Int,
    val title: String,
    val content: String,
    val preview: String,
    val frontMatter: Map<String, Any>,
    val changedBy: String? = null,
    val changedAt: LocalDateTime = LocalDateTime.now(),
    val changeReason: String? = null,
    val previousRevisionId: String? = null,
    val diff: String? = null
) {
    val isInitial: Boolean
        get() = previousRevisionId == null

    val nextRevisionId: String? = null
}
