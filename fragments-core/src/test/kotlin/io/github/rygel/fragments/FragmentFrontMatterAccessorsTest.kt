package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FragmentFrontMatterAccessorsTest {

    private val now = LocalDateTime.of(2024, 1, 15, 10, 0)

    private fun fragment(frontMatter: Map<String, Any>) = Fragment(
        title = "Test",
        slug = "test",
        date = now,
        publishDate = null,
        preview = "",
        content = "",
        frontMatter = frontMatter
    )

    // getString

    @Test
    fun `getString returns string value`() {
        val f = fragment(mapOf("repo" to "fragments4k"))
        assertEquals("fragments4k", f.getString("repo"))
    }

    @Test
    fun `getString converts non-string to string`() {
        val f = fragment(mapOf("count" to 42))
        assertEquals("42", f.getString("count"))
    }

    @Test
    fun `getString returns null for missing key`() {
        val f = fragment(emptyMap())
        assertNull(f.getString("missing"))
    }

    // getBoolean

    @Test
    fun `getBoolean returns native boolean`() {
        val f = fragment(mapOf("featured" to true))
        assertEquals(true, f.getBoolean("featured"))
    }

    @Test
    fun `getBoolean parses string true`() {
        val f = fragment(mapOf("featured" to "true"))
        assertEquals(true, f.getBoolean("featured"))
    }

    @Test
    fun `getBoolean parses string false`() {
        val f = fragment(mapOf("featured" to "false"))
        assertEquals(false, f.getBoolean("featured"))
    }

    @Test
    fun `getBoolean returns null for missing key`() {
        val f = fragment(emptyMap())
        assertNull(f.getBoolean("missing"))
    }

    @Test
    fun `getBoolean returns null for non-boolean type`() {
        val f = fragment(mapOf("featured" to 42))
        assertNull(f.getBoolean("featured"))
    }

    @Test
    fun `getBoolean returns null for invalid string`() {
        val f = fragment(mapOf("featured" to "yes"))
        assertNull(f.getBoolean("featured"))
    }

    // getInt

    @Test
    fun `getInt returns int from Int`() {
        val f = fragment(mapOf("count" to 5))
        assertEquals(5, f.getInt("count"))
    }

    @Test
    fun `getInt converts Long to Int`() {
        val f = fragment(mapOf("count" to 5L))
        assertEquals(5, f.getInt("count"))
    }

    @Test
    fun `getInt converts Double to Int`() {
        val f = fragment(mapOf("count" to 5.9))
        assertEquals(5, f.getInt("count"))
    }

    @Test
    fun `getInt parses string`() {
        val f = fragment(mapOf("count" to "42"))
        assertEquals(42, f.getInt("count"))
    }

    @Test
    fun `getInt returns null for missing key`() {
        val f = fragment(emptyMap())
        assertNull(f.getInt("missing"))
    }

    @Test
    fun `getInt returns null for non-numeric string`() {
        val f = fragment(mapOf("count" to "abc"))
        assertNull(f.getInt("count"))
    }

    // getLong

    @Test
    fun `getLong returns long from Long`() {
        val f = fragment(mapOf("id" to 123456789L))
        assertEquals(123456789L, f.getLong("id"))
    }

    @Test
    fun `getLong converts Int to Long`() {
        val f = fragment(mapOf("id" to 42))
        assertEquals(42L, f.getLong("id"))
    }

    @Test
    fun `getLong parses string`() {
        val f = fragment(mapOf("id" to "999"))
        assertEquals(999L, f.getLong("id"))
    }

    @Test
    fun `getLong returns null for missing key`() {
        val f = fragment(emptyMap())
        assertNull(f.getLong("missing"))
    }

    // getDouble

    @Test
    fun `getDouble returns double from Double`() {
        val f = fragment(mapOf("rating" to 4.5))
        assertEquals(4.5, f.getDouble("rating"))
    }

    @Test
    fun `getDouble converts Int to Double`() {
        val f = fragment(mapOf("rating" to 4))
        assertEquals(4.0, f.getDouble("rating"))
    }

    @Test
    fun `getDouble parses string`() {
        val f = fragment(mapOf("rating" to "3.14"))
        assertEquals(3.14, f.getDouble("rating"))
    }

    @Test
    fun `getDouble returns null for missing key`() {
        val f = fragment(emptyMap())
        assertNull(f.getDouble("missing"))
    }

    // getStringList

    @Test
    fun `getStringList returns list from YAML list`() {
        val f = fragment(mapOf("tools" to listOf("kotlin", "java", "gradle")))
        assertEquals(listOf("kotlin", "java", "gradle"), f.getStringList("tools"))
    }

    @Test
    fun `getStringList converts non-string list elements`() {
        val f = fragment(mapOf("ids" to listOf(1, 2, 3)))
        assertEquals(listOf("1", "2", "3"), f.getStringList("ids"))
    }

    @Test
    fun `getStringList parses comma-separated string`() {
        val f = fragment(mapOf("tools" to "kotlin, java, gradle"))
        assertEquals(listOf("kotlin", "java", "gradle"), f.getStringList("tools"))
    }

    @Test
    fun `getStringList returns empty list for missing key`() {
        val f = fragment(emptyMap())
        assertEquals(emptyList<String>(), f.getStringList("missing"))
    }

    @Test
    fun `getStringList filters null elements from list`() {
        val f = fragment(mapOf("items" to listOf("a", null, "b")))
        assertEquals(listOf("a", "b"), f.getStringList("items"))
    }

    @Test
    fun `getStringList returns empty list for non-list non-string type`() {
        val f = fragment(mapOf("items" to 42))
        assertEquals(emptyList<String>(), f.getStringList("items"))
    }

    @Test
    fun `getStringList handles empty comma-separated string`() {
        val f = fragment(mapOf("tools" to ""))
        assertEquals(emptyList<String>(), f.getStringList("tools"))
    }
}
