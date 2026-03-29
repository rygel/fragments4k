package io.github.rygel.fragments.test

import io.github.rygel.fragments.ArchiveNavigationGenerator
import io.github.rygel.fragments.ArchiveNavigationLink
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ArchiveNavigationTest {
    @Test
    fun testGenerateYearLinksWithDefaultYears() {
        val yearLinks = ArchiveNavigationGenerator.generateYearLinks()

        assertEquals(5, yearLinks.size)
        assertEquals("2024", yearLinks[0].label)
        assertEquals("2023", yearLinks[1].label)
        assertEquals("2022", yearLinks[2].label)
        assertEquals("2021", yearLinks[3].label)
        assertEquals("2020", yearLinks[4].label)

        assertEquals("/blog/archive/2024", yearLinks[0].url)
        assertEquals("/blog/archive/2023", yearLinks[1].url)
    }

    @Test
    fun testGenerateYearLinksWithCustomYears() {
        val availableYears = listOf(2021, 2022, 2023, 2024)
        val yearLinks = ArchiveNavigationGenerator.generateYearLinks(
            baseUrl = "/blog/archive",
            availableYears = availableYears
        )

        assertEquals(4, yearLinks.size)
        assertEquals("2024", yearLinks[0].label)
        assertEquals("2023", yearLinks[1].label)
        assertEquals("2022", yearLinks[2].label)
        assertEquals("2021", yearLinks[3].label)

        assertEquals("/blog/archive/2024", yearLinks[0].url)
        assertEquals("/blog/archive/2021", yearLinks[3].url)
    }

    @Test
    fun testGenerateYearLinksWithCurrentYear() {
        val yearLinks = ArchiveNavigationGenerator.generateYearLinks(
            baseUrl = "/blog/archive",
            availableYears = listOf(2023, 2024),
            currentYear = 2024
        )

        assertEquals(2, yearLinks.size)
        assertTrue(yearLinks[0].isActive)
        assertFalse(yearLinks[1].isActive)

        assertEquals("2024", yearLinks[0].label)
        assertEquals("2023", yearLinks[1].label)
    }

    @Test
    fun testGenerateMonthLinks() {
        val monthLinks = ArchiveNavigationGenerator.generateMonthLinks(
            baseUrl = "/blog/archive",
            year = 2024,
            currentMonth = null
        )

        assertEquals(12, monthLinks.size)
        assertEquals("Jan", monthLinks[0].label)
        assertEquals("Feb", monthLinks[1].label)
        assertEquals("Mar", monthLinks[2].label)
        assertEquals("Apr", monthLinks[3].label)
        assertEquals("May", monthLinks[4].label)
        assertEquals("Jun", monthLinks[5].label)
        assertEquals("Jul", monthLinks[6].label)
        assertEquals("Aug", monthLinks[7].label)
        assertEquals("Sep", monthLinks[8].label)
        assertEquals("Oct", monthLinks[9].label)
        assertEquals("Nov", monthLinks[10].label)
        assertEquals("Dec", monthLinks[11].label)

        assertEquals("/blog/archive/2024/01", monthLinks[0].url)
        assertEquals("/blog/archive/2024/02", monthLinks[1].url)
        assertEquals("/blog/archive/2024/12", monthLinks[11].url)

        monthLinks.forEach { assertFalse(it.isActive) }
    }

    @Test
    fun testGenerateMonthLinksWithCurrentMonth() {
        val monthLinks = ArchiveNavigationGenerator.generateMonthLinks(
            baseUrl = "/blog/archive",
            year = 2024,
            currentMonth = 6
        )

        assertEquals(12, monthLinks.size)

        val currentMonthLink = monthLinks[5]
        assertEquals("Jun", currentMonthLink.label)
        assertEquals("/blog/archive/2024/06", currentMonthLink.url)
        assertTrue(currentMonthLink.isActive)

        monthLinks.filterIndexed { index, _ -> index != 5 }.forEach {
            assertFalse(it.isActive)
        }
    }

    @Test
    fun testGenerateBreadcrumbsForBlogRoot() {
        val breadcrumbs = ArchiveNavigationGenerator.generateBreadcrumbs(
            baseUrl = "/blog",
            currentYear = null,
            currentMonth = null
        )

        assertEquals(1, breadcrumbs.size)
        assertEquals("Blog", breadcrumbs[0].label)
        assertEquals("/blog", breadcrumbs[0].url)
        assertTrue(breadcrumbs[0].isActive)
    }

    @Test
    fun testGenerateBreadcrumbsForYear() {
        val breadcrumbs = ArchiveNavigationGenerator.generateBreadcrumbs(
            baseUrl = "/blog",
            currentYear = 2024,
            currentMonth = null
        )

        assertEquals(2, breadcrumbs.size)

        assertEquals("Blog", breadcrumbs[0].label)
        assertEquals("/blog", breadcrumbs[0].url)
        assertFalse(breadcrumbs[0].isActive)

        assertEquals("2024", breadcrumbs[1].label)
        assertEquals("/blog/2024", breadcrumbs[1].url)
        assertTrue(breadcrumbs[1].isActive)
    }

    @Test
    fun testGenerateBreadcrumbsForYearMonth() {
        val breadcrumbs = ArchiveNavigationGenerator.generateBreadcrumbs(
            baseUrl = "/blog",
            currentYear = 2024,
            currentMonth = 6
        )

        assertEquals(3, breadcrumbs.size)

        assertEquals("Blog", breadcrumbs[0].label)
        assertEquals("/blog", breadcrumbs[0].url)
        assertFalse(breadcrumbs[0].isActive)

        assertEquals("2024", breadcrumbs[1].label)
        assertEquals("/blog/2024", breadcrumbs[1].url)
        assertFalse(breadcrumbs[1].isActive)

        assertEquals("Jun", breadcrumbs[2].label)
        assertEquals("/blog/2024/06", breadcrumbs[2].url)
        assertTrue(breadcrumbs[2].isActive)
    }

    @Test
    fun testGenerateBreadcrumbsWithCustomBaseUrl() {
        val breadcrumbs = ArchiveNavigationGenerator.generateBreadcrumbs(
            baseUrl = "/archives",
            currentYear = 2024,
            currentMonth = null
        )

        assertEquals(2, breadcrumbs.size)
        assertEquals("/archives", breadcrumbs[0].url)
        assertEquals("/archives/2024", breadcrumbs[1].url)
    }

    @Test
    fun testArchiveNavigationLinkDataClass() {
        val link = ArchiveNavigationLink(
            label = "2024",
            url = "/blog/archive/2024",
            isActive = true
        )

        assertEquals("2024", link.label)
        assertEquals("/blog/archive/2024", link.url)
        assertTrue(link.isActive)
    }

    @Test
    fun testArchiveNavigationLinkDefaultActive() {
        val link = ArchiveNavigationLink(
            label = "2024",
            url = "/blog/archive/2024"
        )

        assertFalse(link.isActive)
    }

    @Test
    fun testYearLinksDescendingOrder() {
        val availableYears = listOf(2021, 2023, 2020, 2024, 2022)
        val yearLinks = ArchiveNavigationGenerator.generateYearLinks(
            baseUrl = "/blog/archive",
            availableYears = availableYears
        )

        assertEquals(5, yearLinks.size)
        assertEquals("2024", yearLinks[0].label)
        assertEquals("2023", yearLinks[1].label)
        assertEquals("2022", yearLinks[2].label)
        assertEquals("2021", yearLinks[3].label)
        assertEquals("2020", yearLinks[4].label)
    }

    @Test
    fun testMonthLinksAllMonthsPresent() {
        val monthLinks = ArchiveNavigationGenerator.generateMonthLinks(
            baseUrl = "/blog/archive",
            year = 2024,
            currentMonth = null
        )

        val expectedMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                  "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        assertEquals(expectedMonths.size, monthLinks.size)

        monthLinks.forEachIndexed { index, link ->
            assertEquals(expectedMonths[index], link.label)
            assertEquals("/blog/archive/2024/${String.format("%02d", index + 1)}", link.url)
        }
    }

    @Test
    fun testMultipleCurrentYearsInBreadcrumbs() {
        val breadcrumbs1 = ArchiveNavigationGenerator.generateBreadcrumbs(
            baseUrl = "/blog",
            currentYear = 2023,
            currentMonth = null
        )

        assertEquals(2, breadcrumbs1.size)
        assertEquals("2023", breadcrumbs1[1].label)
        assertTrue(breadcrumbs1[1].isActive)

        val breadcrumbs2 = ArchiveNavigationGenerator.generateBreadcrumbs(
            baseUrl = "/blog",
            currentYear = 2024,
            currentMonth = null
        )

        assertEquals(2, breadcrumbs2.size)
        assertEquals("2024", breadcrumbs2[1].label)
        assertTrue(breadcrumbs2[1].isActive)
    }

    @Test
    fun testMultipleCurrentMonthsInBreadcrumbs() {
        val breadcrumbs1 = ArchiveNavigationGenerator.generateBreadcrumbs(
            baseUrl = "/blog",
            currentYear = 2024,
            currentMonth = 3
        )

        assertEquals(3, breadcrumbs1.size)
        assertEquals("Mar", breadcrumbs1[2].label)
        assertTrue(breadcrumbs1[2].isActive)

        val breadcrumbs2 = ArchiveNavigationGenerator.generateBreadcrumbs(
            baseUrl = "/blog",
            currentYear = 2024,
            currentMonth = 9
        )

        assertEquals(3, breadcrumbs2.size)
        assertEquals("Sep", breadcrumbs2[2].label)
        assertTrue(breadcrumbs2[2].isActive)
    }
}
