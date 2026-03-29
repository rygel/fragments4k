package io.github.rygel.fragments.http4k

object PaginationGenerator {
    fun generateSimpleControls(
        currentPage: Int,
        totalPages: Int,
        basePath: String,
        showPageNumbers: Boolean = true
    ): PaginationInfo {
        val hasPrevious = currentPage > 1
        val hasNext = currentPage < totalPages
        
        val text = buildString {
            if (showPageNumbers && totalPages > 1) {
                append("Page $currentPage")
                if (totalPages > 1) {
                    append(" of $totalPages")
                }
            }
        }
        
        return PaginationInfo(
            currentPage = currentPage,
            totalPages = totalPages,
            hasPrevious = hasPrevious,
            hasNext = hasNext,
            text = text
        )
    }
}
