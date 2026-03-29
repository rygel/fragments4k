package io.github.rygel.fragments.sitemap

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentStatus
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class SitemapUrl(
    val loc: String,
    val lastmod: String,
    val changefreq: ChangeFrequency,
    val priority: Double = 0.5,
    val image: SitemapImage? = null
)

data class SitemapImage(
    val loc: String,
    val caption: String? = null,
    val title: String? = null
)

enum class ChangeFrequency(val value: String) {
    ALWAYS("always"),
    HOURLY("hourly"),
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    YEARLY("yearly"),
    NEVER("never");

    companion object {
        fun fromFragment(fragment: Fragment, lastModified: LocalDateTime?): ChangeFrequency {
            val fragmentDate = fragment.date ?: return NEVER
            if (lastModified == null) return WEEKLY

            val daysSinceModification = Duration.between(lastModified, LocalDateTime.now()).toDays()

            return when {
                fragment.status == FragmentStatus.PUBLISHED && daysSinceModification < 1 -> DAILY
                fragment.status == FragmentStatus.PUBLISHED && daysSinceModification < 7 -> WEEKLY
                fragment.status == FragmentStatus.PUBLISHED && daysSinceModification < 30 -> MONTHLY
                fragment.status == FragmentStatus.PUBLISHED && daysSinceModification < 365 -> YEARLY
                else -> MONTHLY
            }
        }

        fun calculatePriority(fragment: Fragment, lastModified: LocalDateTime?): Double {
            val fragmentDate = fragment.date ?: return 0.5
            if (lastModified == null) return 0.8

            val daysSinceModification = Duration.between(lastModified, LocalDateTime.now()).toDays()

            return when {
                fragment.status == FragmentStatus.PUBLISHED && daysSinceModification < 1 -> 1.0
                fragment.status == FragmentStatus.PUBLISHED && daysSinceModification < 7 -> 0.9
                fragment.status == FragmentStatus.PUBLISHED && daysSinceModification < 30 -> 0.8
                fragment.status == FragmentStatus.PUBLISHED && daysSinceModification < 365 -> 0.6
                fragment.isInSeries -> 0.7
                fragment.categories.isNotEmpty() -> 0.8
                else -> 0.5
            }
        }
    }
}
