package io.github.rygel.fragments.adapter

/** Pagination state for navigating paged content (blog overview, tag, category pages). */
data class PaginationInfo(
    val currentPage: Int,
    val totalPages: Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val text: String,
)
