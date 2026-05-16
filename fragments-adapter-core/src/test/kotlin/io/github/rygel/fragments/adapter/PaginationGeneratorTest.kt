package io.github.rygel.fragments.adapter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PaginationGeneratorTest {
    @Test
    fun testFirstPageOfMany() {
        val result =
            PaginationGenerator.generateSimpleControls(
                currentPage = 1,
                totalPages = 5,
                basePath = "/blog",
            )

        assertFalse(result.hasPrevious)
        assertTrue(result.hasNext)
        assertEquals("Page 1 of 5", result.text)
        assertEquals(1, result.currentPage)
        assertEquals(5, result.totalPages)
    }

    @Test
    fun testMiddlePage() {
        val result =
            PaginationGenerator.generateSimpleControls(
                currentPage = 3,
                totalPages = 5,
                basePath = "/blog",
            )

        assertTrue(result.hasPrevious)
        assertTrue(result.hasNext)
        assertEquals("Page 3 of 5", result.text)
    }

    @Test
    fun testLastPageOfMany() {
        val result =
            PaginationGenerator.generateSimpleControls(
                currentPage = 5,
                totalPages = 5,
                basePath = "/blog",
            )

        assertTrue(result.hasPrevious)
        assertFalse(result.hasNext)
        assertEquals("Page 5 of 5", result.text)
    }

    @Test
    fun testSinglePage() {
        val result =
            PaginationGenerator.generateSimpleControls(
                currentPage = 1,
                totalPages = 1,
                basePath = "/blog",
            )

        assertFalse(result.hasPrevious)
        assertFalse(result.hasNext)
        assertEquals("", result.text)
    }

    @Test
    fun testHidesPageNumbersWhenDisabled() {
        val result =
            PaginationGenerator.generateSimpleControls(
                currentPage = 2,
                totalPages = 5,
                basePath = "/blog",
                showPageNumbers = false,
            )

        assertEquals("", result.text)
        assertTrue(result.hasPrevious)
        assertTrue(result.hasNext)
    }
}
