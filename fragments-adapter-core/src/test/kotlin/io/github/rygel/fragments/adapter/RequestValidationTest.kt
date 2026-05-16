package io.github.rygel.fragments.adapter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RequestValidationTest {
    @Test
    fun testValidSlugIsAccepted() {
        val result = RequestValidation.validateSlug("my-blog-post")
        assertTrue(result.isValid)
        assertEquals("my-blog-post", result.value)
    }

    @Test
    fun testInvalidSlugIsRejected() {
        val result = RequestValidation.validateSlug("../etc/passwd")
        assertFalse(result.isValid)
    }

    @Test
    fun testSlugWithSpacesIsRejected() {
        val result = RequestValidation.validateSlug("has spaces")
        assertFalse(result.isValid)
    }

    @Test
    fun testEmptySlugIsRejected() {
        val result = RequestValidation.validateSlug("")
        assertFalse(result.isValid)
    }

    @Test
    fun testSlugMaxLength() {
        val longSlug = "a".repeat(200)
        val result = RequestValidation.validateSlug(longSlug)
        assertFalse(result.isValid)
    }

    @Test
    fun testPageNumberIsClampedToMinimum() {
        val result = RequestValidation.validatePage(-1)
        assertTrue(result.isValid)
        assertEquals(1, result.value)
    }

    @Test
    fun testPageNumberIsClampedToMaximum() {
        val result = RequestValidation.validatePage(10001)
        assertTrue(result.isValid)
        assertEquals(10000, result.value)
    }

    @Test
    fun testValidPageNumber() {
        val result = RequestValidation.validatePage(5)
        assertTrue(result.isValid)
        assertEquals(5, result.value)
    }

    @Test
    fun testAutocompleteLimitIsClamped() {
        val tooLow = RequestValidation.validateAutocompleteLimit(-1)
        assertTrue(tooLow.isValid)
        assertEquals(1, tooLow.value)

        val tooHigh = RequestValidation.validateAutocompleteLimit(999)
        assertTrue(tooHigh.isValid)
        assertEquals(50, tooHigh.value)
    }

    @Test
    fun testValidAutocompleteLimit() {
        val result = RequestValidation.validateAutocompleteLimit(10)
        assertTrue(result.isValid)
        assertEquals(10, result.value)
    }

    @Test
    fun testSearchQueryIsTrimmedAndLengthLimited() {
        val result = RequestValidation.validateSearchQuery("  hello world  ")
        assertTrue(result.isValid)
        assertEquals("hello world", result.value)
    }

    @Test
    fun testBlankSearchQueryIsRejected() {
        val result = RequestValidation.validateSearchQuery("   ")
        assertFalse(result.isValid)
    }

    @Test
    fun testOverlongSearchQueryIsRejected() {
        val longQuery = "a".repeat(501)
        val result = RequestValidation.validateSearchQuery(longQuery)
        assertFalse(result.isValid)
    }

    @Test
    fun testValidTagIsAccepted() {
        val result = RequestValidation.validateTag("kotlin")
        assertTrue(result.isValid)
        assertEquals("kotlin", result.value)
    }

    @Test
    fun testInvalidTagIsRejected() {
        val result = RequestValidation.validateTag("<script>")
        assertFalse(result.isValid)
    }

    @Test
    fun testValidCategoryIsAccepted() {
        val result = RequestValidation.validateCategory("technology")
        assertTrue(result.isValid)
        assertEquals("technology", result.value)
    }

    @Test
    fun testMonthOutsideRangeIsRejected() {
        val tooLow = RequestValidation.validateMonth(0)
        assertFalse(tooLow.isValid)

        val tooHigh = RequestValidation.validateMonth(13)
        assertFalse(tooHigh.isValid)
    }

    @Test
    fun testValidMonthIsAccepted() {
        val result = RequestValidation.validateMonth(6)
        assertTrue(result.isValid)
        assertEquals(6, result.value)
    }

    @Test
    fun testYearOutsideRangeIsRejected() {
        val tooLow = RequestValidation.validateYear(1800)
        assertFalse(tooLow.isValid)

        val tooHigh = RequestValidation.validateYear(4000)
        assertFalse(tooHigh.isValid)
    }

    @Test
    fun testValidYearIsAccepted() {
        val result = RequestValidation.validateYear(2024)
        assertTrue(result.isValid)
        assertEquals(2024, result.value)
    }

    @Test
    fun testValidAuthorIdIsAccepted() {
        val result = RequestValidation.validateAuthorId("john-doe")
        assertTrue(result.isValid)
        assertEquals("john-doe", result.value)
    }

    @Test
    fun testMaxResultsIsClamped() {
        val tooLow = RequestValidation.validateMaxResults(-1)
        assertTrue(tooLow.isValid)
        assertEquals(1, tooLow.value)

        val tooHigh = RequestValidation.validateMaxResults(500)
        assertTrue(tooHigh.isValid)
        assertEquals(100, tooHigh.value)
    }
}
