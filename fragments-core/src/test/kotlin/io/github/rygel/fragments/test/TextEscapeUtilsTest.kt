package io.github.rygel.fragments.test

import io.github.rygel.fragments.TextEscapeUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextEscapeUtilsTest {
    @Test
    fun testEscapeJsonBackslash() {
        assertEquals("\\\\", TextEscapeUtils.escapeJson("\\"))
    }

    @Test
    fun testEscapeJsonDoubleQuote() {
        assertEquals("\\\"", TextEscapeUtils.escapeJson("\""))
    }

    @Test
    fun testEscapeJsonBackspace() {
        assertEquals("\\b", TextEscapeUtils.escapeJson("\b"))
    }

    @Test
    fun testEscapeJsonNewline() {
        assertEquals("\\n", TextEscapeUtils.escapeJson("\n"))
    }

    @Test
    fun testEscapeJsonCarriageReturn() {
        assertEquals("\\r", TextEscapeUtils.escapeJson("\r"))
    }

    @Test
    fun testEscapeJsonTab() {
        assertEquals("\\t", TextEscapeUtils.escapeJson("\t"))
    }

    @Test
    fun testEscapeJsonPlainString() {
        assertEquals("hello world", TextEscapeUtils.escapeJson("hello world"))
    }

    @Test
    fun testEscapeJsonMixed() {
        assertEquals(
            "line1\\nline2\\ttab\\\"quoted\\\"\\bslash\\\\end",
            TextEscapeUtils.escapeJson("line1\nline2\ttab\"quoted\"\bslash\\end"),
        )
    }

    @Test
    fun testEscapeJsonEmpty() {
        assertEquals("", TextEscapeUtils.escapeJson(""))
    }

    @Test
    fun testEscapeHtmlAmpersand() {
        assertEquals("&amp;", TextEscapeUtils.escapeHtml("&"))
    }

    @Test
    fun testEscapeHtmlLessThan() {
        assertEquals("&lt;", TextEscapeUtils.escapeHtml("<"))
    }

    @Test
    fun testEscapeHtmlGreaterThan() {
        assertEquals("&gt;", TextEscapeUtils.escapeHtml(">"))
    }

    @Test
    fun testEscapeHtmlDoubleQuote() {
        assertEquals("&quot;", TextEscapeUtils.escapeHtml("\""))
    }

    @Test
    fun testEscapeHtmlSingleQuote() {
        assertEquals("&#x27;", TextEscapeUtils.escapeHtml("'"))
    }

    @Test
    fun testEscapeHtmlPlainString() {
        assertEquals("hello world", TextEscapeUtils.escapeHtml("hello world"))
    }

    @Test
    fun testEscapeHtmlMixed() {
        assertEquals(
            "&lt;b&gt;bold &amp;amp;&quot;text&quot;&#x27;s&lt;/b&gt;",
            TextEscapeUtils.escapeHtml("<b>bold &amp;\"text\"'s</b>"),
        )
    }

    @Test
    fun testEscapeHtmlEmpty() {
        assertEquals("", TextEscapeUtils.escapeHtml(""))
    }

    @Test
    fun testTitleCaseSimpleSlug() {
        assertEquals("Hello World", TextEscapeUtils.titleCase("hello-world"))
    }

    @Test
    fun testTitleCaseSingleWord() {
        assertEquals("Hello", TextEscapeUtils.titleCase("hello"))
    }

    @Test
    fun testTitleCaseMultipleDashes() {
        assertEquals("A B C D", TextEscapeUtils.titleCase("a-b-c-d"))
    }

    @Test
    fun testTitleCaseAlreadyCapitalized() {
        assertEquals("Hello World", TextEscapeUtils.titleCase("Hello-World"))
    }

    @Test
    fun testTitleCaseNoDashes() {
        assertEquals("Hello", TextEscapeUtils.titleCase("hello"))
    }

    @Test
    fun testTitleCaseEmpty() {
        assertEquals("", TextEscapeUtils.titleCase(""))
    }
}
