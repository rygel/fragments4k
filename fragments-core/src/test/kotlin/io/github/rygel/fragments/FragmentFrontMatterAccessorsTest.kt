package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FragmentFrontMatterAccessorsTest {
    private val now = LocalDateTime.of(2024, 1, 15, 10, 0)

    private fun fragment(frontMatter: Map<String, Any>) =
        Fragment(
            title = "Test",
            slug = "test",
            date = now,
            publishDate = null,
            preview = "",
            htmlContent = "",
            frontMatter = frontMatter,
        )

    // getString

    @Test
    fun testGetStringReturnsStringValue() {
        val f = fragment(mapOf("repo" to "fragments4k"))
        assertEquals("fragments4k", f.getString("repo"))
    }

    @Test
    fun testGetStringConvertsNonStringToString() {
        val f = fragment(mapOf("count" to 42))
        assertEquals("42", f.getString("count"))
    }

    @Test
    fun testGetStringReturnsNullForMissingKey() {
        val f = fragment(emptyMap())
        assertNull(f.getString("missing"))
    }

    // getBoolean

    @Test
    fun testGetBooleanReturnsNativeBoolean() {
        val f = fragment(mapOf("featured" to true))
        assertEquals(true, f.getBoolean("featured"))
    }

    @Test
    fun testGetBooleanParsesStringTrue() {
        val f = fragment(mapOf("featured" to "true"))
        assertEquals(true, f.getBoolean("featured"))
    }

    @Test
    fun testGetBooleanParsesStringFalse() {
        val f = fragment(mapOf("featured" to "false"))
        assertEquals(false, f.getBoolean("featured"))
    }

    @Test
    fun testGetBooleanReturnsNullForMissingKey() {
        val f = fragment(emptyMap())
        assertNull(f.getBoolean("missing"))
    }

    @Test
    fun testGetBooleanReturnsNullForNonBooleanType() {
        val f = fragment(mapOf("featured" to 42))
        assertNull(f.getBoolean("featured"))
    }

    @Test
    fun testGetBooleanReturnsNullForInvalidString() {
        val f = fragment(mapOf("featured" to "yes"))
        assertNull(f.getBoolean("featured"))
    }

    // getInt

    @Test
    fun testGetIntReturnsIntFromInt() {
        val f = fragment(mapOf("count" to 5))
        assertEquals(5, f.getInt("count"))
    }

    @Test
    fun testGetIntConvertsLongToInt() {
        val f = fragment(mapOf("count" to 5L))
        assertEquals(5, f.getInt("count"))
    }

    @Test
    fun testGetIntConvertsDoubleToInt() {
        val f = fragment(mapOf("count" to 5.9))
        assertEquals(5, f.getInt("count"))
    }

    @Test
    fun testGetIntParsesString() {
        val f = fragment(mapOf("count" to "42"))
        assertEquals(42, f.getInt("count"))
    }

    @Test
    fun testGetIntReturnsNullForMissingKey() {
        val f = fragment(emptyMap())
        assertNull(f.getInt("missing"))
    }

    @Test
    fun testGetIntReturnsNullForNonNumericString() {
        val f = fragment(mapOf("count" to "abc"))
        assertNull(f.getInt("count"))
    }

    // getLong

    @Test
    fun testGetLongReturnsLongFromLong() {
        val f = fragment(mapOf("id" to 123456789L))
        assertEquals(123456789L, f.getLong("id"))
    }

    @Test
    fun testGetLongConvertsIntToLong() {
        val f = fragment(mapOf("id" to 42))
        assertEquals(42L, f.getLong("id"))
    }

    @Test
    fun testGetLongParsesString() {
        val f = fragment(mapOf("id" to "999"))
        assertEquals(999L, f.getLong("id"))
    }

    @Test
    fun testGetLongReturnsNullForMissingKey() {
        val f = fragment(emptyMap())
        assertNull(f.getLong("missing"))
    }

    // getDouble

    @Test
    fun testGetDoubleReturnsDoubleFromDouble() {
        val f = fragment(mapOf("rating" to 4.5))
        assertEquals(4.5, f.getDouble("rating"))
    }

    @Test
    fun testGetDoubleConvertsIntToDouble() {
        val f = fragment(mapOf("rating" to 4))
        assertEquals(4.0, f.getDouble("rating"))
    }

    @Test
    fun testGetDoubleParsesString() {
        val f = fragment(mapOf("rating" to "3.14"))
        assertEquals(3.14, f.getDouble("rating"))
    }

    @Test
    fun testGetDoubleReturnsNullForMissingKey() {
        val f = fragment(emptyMap())
        assertNull(f.getDouble("missing"))
    }

    // getStringList

    @Test
    fun testGetStringListReturnsListFromYamlList() {
        val f = fragment(mapOf("tools" to listOf("kotlin", "java", "gradle")))
        assertEquals(listOf("kotlin", "java", "gradle"), f.getStringList("tools"))
    }

    @Test
    fun testGetStringListConvertsNonStringListElements() {
        val f = fragment(mapOf("ids" to listOf(1, 2, 3)))
        assertEquals(listOf("1", "2", "3"), f.getStringList("ids"))
    }

    @Test
    fun testGetStringListParsesCommaSeparatedString() {
        val f = fragment(mapOf("tools" to "kotlin, java, gradle"))
        assertEquals(listOf("kotlin", "java", "gradle"), f.getStringList("tools"))
    }

    @Test
    fun testGetStringListReturnsEmptyListForMissingKey() {
        val f = fragment(emptyMap())
        assertEquals(emptyList<String>(), f.getStringList("missing"))
    }

    @Test
    fun testGetStringListFiltersNullElementsFromList() {
        val f = fragment(mapOf("items" to listOf("a", null, "b")))
        assertEquals(listOf("a", "b"), f.getStringList("items"))
    }

    @Test
    fun testGetStringListReturnsEmptyListForNonListNonStringType() {
        val f = fragment(mapOf("items" to 42))
        assertEquals(emptyList<String>(), f.getStringList("items"))
    }

    @Test
    fun testGetStringListHandlesEmptyCommaSeparatedString() {
        val f = fragment(mapOf("tools" to ""))
        assertEquals(emptyList<String>(), f.getStringList("tools"))
    }
}
