package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MarkdownParserTest {
    private val parser = MarkdownParser()

    @Test
    fun parseWithFrontMatter() {
        val markdown =
            """
            ---
            title: "Hello World"
            slug: hello-world
            ---
            This is the **content**.
            """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals("Hello World", result.frontMatter["title"])
        assertEquals("hello-world", result.frontMatter["slug"])
        assertTrue(result.htmlContent.contains("<strong>content</strong>"))
        assertTrue(result.content.contains("This is the **content**."))
    }

    @Test
    fun parseWithoutFrontMatter() {
        val markdown = "Just a paragraph."
        val result = parser.parse(markdown)

        assertEquals(emptyMap<String, Any>(), result.frontMatter)
        assertTrue(result.htmlContent.contains("Just a paragraph."))
        assertEquals(markdown, result.content)
    }

    @Test
    fun parseFrontMatterWithList() {
        val markdown =
            """
            ---
            title: Test
            tags: [kotlin, jvm]
            ---
            Content here.
            """.trimIndent()

        val result = parser.parse(markdown)

        @Suppress("UNCHECKED_CAST")
        val tags = result.frontMatter["tags"] as? List<String>

        assertNotNull(tags)
        assertTrue(tags!!.contains("kotlin"))
        assertTrue(tags.contains("jvm"))
    }

    @Test
    fun parseFrontMatterWithBoolean() {
        val markdown =
            """
            ---
            title: Test
            visible: false
            ---
            Content.
            """.trimIndent()

        val result = parser.parse(markdown)
        assertEquals(false, result.frontMatter["visible"])
    }

    @Test
    fun parseRendersMarkdownTables() {
        val markdown =
            """
            | Header 1 | Header 2 |
            |----------|----------|
            | Cell 1   | Cell 2   |
            """.trimIndent()

        val result = parser.parse(markdown)

        assertTrue(result.htmlContent.contains("<table"))
        assertTrue(result.htmlContent.contains("<td"))
    }

    @Test
    fun parseRendersStrikethrough() {
        val result = parser.parse("~~deleted~~")

        assertTrue(result.htmlContent.contains("<del>deleted</del>") || result.htmlContent.contains("<s>deleted</s>"))
    }

    @Test
    fun parseRendersTaskList() {
        val markdown =
            """
            - [x] Done
            - [ ] Todo
            """.trimIndent()

        val result = parser.parse(markdown)

        assertTrue(result.htmlContent.contains("Done"))
        assertTrue(result.htmlContent.contains("Todo"))
    }

    @Test
    fun parseHandlesEmptyContent() {
        val result = parser.parse("")

        assertEquals(emptyMap<String, Any>(), result.frontMatter)
        assertTrue(result.content.isEmpty())
    }

    @Test
    fun parseHandlesMultilineContent() {
        val markdown =
            """
            ---
            title: Test
            ---
            Line one.

            Line two.

            Line three.
            """.trimIndent()

        val result = parser.parse(markdown)

        assertTrue(result.content.contains("Line one."))
        assertTrue(result.content.contains("Line three."))
    }

    @Test
    fun parseSanitizesHtml() {
        val markdown =
            """
            ---
            title: Test
            ---
            Text with <script>alert('xss')</script> bad content.
            """.trimIndent()

        val result = parser.parse(markdown)

        assertFalse(result.htmlContent.contains("<script"))
        assertTrue(result.htmlContent.contains("Text with"))
    }

    @Test
    fun parseDateWithLocalDateTime() {
        val input = LocalDateTime.of(2026, 3, 15, 10, 30)
        val result = MarkdownParser.parseDate(input)

        assertEquals(input, result)
    }

    @Test
    fun parseDateWithStringDate() {
        val result = MarkdownParser.parseDate("2026-03-15")

        assertNotNull(result)
        assertEquals(2026, result!!.year)
        assertEquals(3, result.monthValue)
        assertEquals(15, result.dayOfMonth)
        assertEquals(0, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun parseDateWithStringDateTime() {
        val result = MarkdownParser.parseDate("2026-03-15T10:30")

        assertNotNull(result)
        assertEquals(2026, result!!.year)
        assertEquals(10, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun parseDateWithStringDateTimeSpace() {
        val result = MarkdownParser.parseDate("2026-03-15 10:30")

        assertNotNull(result)
        assertEquals(2026, result!!.year)
        assertEquals(10, result.hour)
    }

    @Test
    fun parseDateReturnsNullForNull() {
        assertNull(MarkdownParser.parseDate(null))
    }

    @Test
    fun parseDateReturnsNullForUnrecognisedType() {
        assertNull(MarkdownParser.parseDate(42))
    }

    @Test
    fun parseDateReturnsNullForInvalidString() {
        assertNull(MarkdownParser.parseDate("not-a-date"))
    }

    @Test
    fun parseDateReturnsNullForBlankString() {
        assertNull(MarkdownParser.parseDate("   "))
    }

    @Test
    fun parseDateWithJavaUtilDate() {
        val epochSeconds =
            java.time.LocalDateTime
                .of(2026, 3, 15, 10, 30)
                .toEpochSecond(java.time.ZoneOffset.UTC)
        val utilDate = java.util.Date(epochSeconds * 1000)
        val result = MarkdownParser.parseDate(utilDate)

        assertNotNull(result)
        assertEquals(2026, result!!.year)
    }

    @Test
    fun parseFrontMatterWithNestedYaml() {
        val markdown =
            """
            ---
            title: Test
            languages:
              en: hello-world
              de: hallo-welt
            ---
            Content.
            """.trimIndent()

        val result = parser.parse(markdown)

        @Suppress("UNCHECKED_CAST")
        val languages = result.frontMatter["languages"] as? Map<String, String>

        assertNotNull(languages)
        assertEquals("hello-world", languages!!["en"])
        assertEquals("hallo-welt", languages["de"])
    }

    @Test
    fun parseInvalidYamlReturnsEmptyFrontMatter() {
        val markdown =
            """
            ---
            : [invalid yaml
            ---
            Content.
            """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(emptyMap<String, Any>(), result.frontMatter)
    }
}
