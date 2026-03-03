package io.andromeda.fragments.blog

import io.andromeda.fragments.Fragment

data class Page<T>(
    val items: List<T>,
    val currentPage: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    companion object {
        fun <T> create(items: List<T>, page: Int, pageSize: Int): Page<T> {
            val totalItems = items.size
            val totalPages = (totalItems + pageSize - 1) / pageSize
            val startIndex = (page - 1) * pageSize
            val endIndex = minOf(startIndex + pageSize, totalItems)
            
            val pageItems = if (startIndex < totalItems) {
                items.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            return Page(
                items = pageItems,
                currentPage = page,
                pageSize = pageSize,
                totalItems = totalItems,
                totalPages = totalPages,
                hasNext = page < totalPages,
                hasPrevious = page > 1
            )
        }
    }
}
