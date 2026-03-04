package io.andromeda.fragments

import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

data class ArchiveNavigationLink(
    val label: String,
    val url: String,
    val isActive: Boolean = false
)

object ArchiveNavigationGenerator {
    fun generateYearLinks(
        baseUrl: String = "/blog/archive",
        availableYears: List<Int> = emptyList(),
        currentYear: Int? = null
    ): List<ArchiveNavigationLink> {
        val years = if (availableYears.isEmpty()) {
            (2024 downTo 2020).toList()
        } else {
            availableYears.sortedDescending()
        }

        return years.map { year ->
            ArchiveNavigationLink(
                label = year.toString(),
                url = "$baseUrl/$year",
                isActive = currentYear == year
            )
        }
    }

    fun generateMonthLinks(
        baseUrl: String = "/blog/archive",
        year: Int,
        currentMonth: Int? = null
    ): List<ArchiveNavigationLink> {
        val months = (1..12).toList()
        val yearPath = "$baseUrl/$year"

        return months.map { month ->
            val monthName = Month.of(month)
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            ArchiveNavigationLink(
                label = monthName,
                url = "$yearPath/${String.format("%02d", month)}",
                isActive = currentMonth == month
            )
        }
    }

    fun generateBreadcrumbs(
        baseUrl: String = "/blog",
        currentYear: Int? = null,
        currentMonth: Int? = null
    ): List<ArchiveNavigationLink> {
        val breadcrumbs = mutableListOf<ArchiveNavigationLink>()
        breadcrumbs.add(ArchiveNavigationLink(
            label = "Blog",
            url = baseUrl,
            isActive = currentYear == null && currentMonth == null
        ))

        currentYear?.let { year ->
            breadcrumbs.add(ArchiveNavigationLink(
                label = year.toString(),
                url = "$baseUrl/$year",
                isActive = currentMonth == null
            ))

            currentMonth?.let { month ->
                val monthName = Month.of(month)
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                breadcrumbs.add(ArchiveNavigationLink(
                    label = monthName,
                    url = "$baseUrl/$year/${String.format("%02d", month)}",
                    isActive = true
                ))
            }
        }

        return breadcrumbs
    }
}
