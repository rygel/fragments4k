package io.github.rygel.fragments.blog

import io.github.rygel.fragments.Fragment

/**
 * A single page of results from a paginated query.
 *
 * @property items The fragment subset for [currentPage].
 * @property currentPage The effective page number, always in `[1, totalPages]`.
 *   Out-of-range requests are clamped: page 0 becomes 1, page 999 on a 5-page
 *   result becomes 5.
 * @property totalPages Always at least 1, even when [totalItems] is 0.
 */
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
        /**
         * Creates a [Page] for the given [page] number (1-based).
         *
         * Out-of-range [page] values are silently clamped to `[1, totalPages]`
         * so callers never receive a page with a [currentPage] that doesn't
         * correspond to the returned [items].
         */
        fun <T> create(items: List<T>, page: Int, pageSize: Int): Page<T> {
            val totalItems = items.size
            val totalPages = maxOf(1, (totalItems + pageSize - 1) / pageSize)
            val clampedPage = page.coerceIn(1, totalPages)
            val startIndex = (clampedPage - 1) * pageSize
            val endIndex = minOf(startIndex + pageSize, totalItems)

            return Page(
                items = items.subList(startIndex, endIndex),
                currentPage = clampedPage,
                pageSize = pageSize,
                totalItems = totalItems,
                totalPages = totalPages,
                hasNext = clampedPage < totalPages,
                hasPrevious = clampedPage > 1
            )
        }
    }
}
