package io.andromeda.fragments.javalin

data class PaginationInfo(
    val currentPage: Int,
    val totalPages: Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val text: String
)
