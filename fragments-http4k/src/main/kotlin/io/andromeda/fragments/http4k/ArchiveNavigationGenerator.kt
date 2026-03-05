package io.andromeda.fragments.http4k

object ArchiveNavigationGenerator {
    fun generateYearLinks(
        baseUrl: String,
        availableYears: List<Int>,
        currentYear: Int
    ): List<ArchiveNavigationLink> {
        return availableYears.map { year ->
            ArchiveNavigationLink(
                label = year.toString(),
                url = "$baseUrl/$year",
                active = year == currentYear
            )
        }
    }
    
    fun generateMonthLinks(
        year: Int,
        currentMonth: Int
    ): List<ArchiveNavigationLink> {
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        
        return monthNames.mapIndexed { index, monthName ->
            val month = index + 1
            ArchiveNavigationLink(
                label = monthName,
                url = "/blog/archive/$year/${String.format("%02d", month)}",
                active = month == currentMonth
            )
        }
    }
    
    fun generateBreadcrumbs(
        currentYear: Int,
        currentMonth: Int? = null
    ): List<ArchiveNavigationLink> {
        val breadcrumbs = mutableListOf<ArchiveNavigationLink>()
        
        breadcrumbs.add(ArchiveNavigationLink(
            label = "Archive",
            url = "/blog/archive"
        ))
        
        breadcrumbs.add(ArchiveNavigationLink(
            label = currentYear.toString(),
            url = "/blog/archive/$currentYear",
            active = currentMonth == null
        ))
        
        if (currentMonth != null) {
            val monthNames = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            breadcrumbs.add(ArchiveNavigationLink(
                label = monthNames[currentMonth - 1],
                url = "/blog/archive/$currentYear/${String.format("%02d", currentMonth)}",
                active = true
            ))
        }
        
        return breadcrumbs
    }
}
