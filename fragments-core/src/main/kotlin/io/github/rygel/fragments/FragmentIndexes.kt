package io.github.rygel.fragments

import java.time.LocalDateTime

data class FragmentIndexes(
    val bySlug: Map<String, Fragment>,
    val byTag: Map<String, List<Fragment>>,
    val byCategory: Map<String, List<Fragment>>,
    val byAuthor: Map<String, List<Fragment>>,
    val byYearMonth: Map<Pair<Int, Int>, List<Fragment>>,
    val allVisibleSorted: List<Fragment>,
    val byStatus: Map<FragmentStatus, List<Fragment>>,
) {
    companion object {
        private fun isVisible(
            fragment: Fragment,
            now: LocalDateTime,
        ): Boolean {
            if (!fragment.visible) return false
            return when (fragment.status) {
                FragmentStatus.PUBLISHED -> {
                    fragment.expiryDate == null || !fragment.expiryDate.isBefore(now)
                }

                FragmentStatus.SCHEDULED -> {
                    fragment.publishDate != null &&
                        !fragment.publishDate.isAfter(now) &&
                        (fragment.expiryDate == null || !fragment.expiryDate.isBefore(now))
                }

                FragmentStatus.DRAFT,
                FragmentStatus.REVIEW,
                FragmentStatus.APPROVED,
                FragmentStatus.ARCHIVED,
                FragmentStatus.EXPIRED,
                -> {
                    false
                }
            }
        }

        fun build(fragments: List<Fragment>): FragmentIndexes {
            val now = LocalDateTime.now()
            val visible = fragments.filter { isVisible(it, now) }.sortedByDescending { it.date }

            val byTag =
                fragments
                    .flatMap { f -> f.tags.map { tag -> tag.lowercase() to f } }
                    .groupBy({ it.first }, { it.second })

            val byCategory =
                fragments
                    .flatMap { f -> f.categories.map { cat -> cat.lowercase() to f } }
                    .groupBy({ it.first }, { it.second })

            val byAuthor =
                fragments
                    .flatMap { f -> (f.authorIds + listOfNotNull(f.author)).map { aid -> aid to f } }
                    .groupBy({ it.first }, { it.second })

            val byYearMonth =
                fragments
                    .filter { it.date != null }
                    .groupBy { Pair(it.date!!.year, it.date!!.monthValue) }

            return FragmentIndexes(
                bySlug = fragments.associateBy { it.slug },
                byTag = byTag,
                byCategory = byCategory,
                byAuthor = byAuthor,
                byYearMonth = byYearMonth,
                allVisibleSorted = visible,
                byStatus = fragments.groupBy { it.status },
            )
        }

        val EMPTY =
            FragmentIndexes(
                bySlug = emptyMap(),
                byTag = emptyMap(),
                byCategory = emptyMap(),
                byAuthor = emptyMap(),
                byYearMonth = emptyMap(),
                allVisibleSorted = emptyList(),
                byStatus = emptyMap(),
            )
    }
}
