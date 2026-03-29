package io.github.rygel.fragments.http4k

data class PaginationInfo(
    val currentPage: Int,
    val totalPages: Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val text: String
)
