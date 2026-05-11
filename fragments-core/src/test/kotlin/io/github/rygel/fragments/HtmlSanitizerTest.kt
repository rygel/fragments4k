package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HtmlSanitizerTest {
    @Test
    fun testRemovesScriptTags() {
        val input = """<p>Hello</p><script>alert("xss")</script>"""
        val result = HtmlSanitizer.sanitize(input)
        assertFalse(result.contains("<script"))
        assertTrue(result.contains("<p>Hello</p>"))
    }

    @Test
    fun testRemovesEventHandlers() {
        val input = """<p onclick="alert('xss')">Hello</p>"""
        val result = HtmlSanitizer.sanitize(input)
        assertFalse(result.contains("onclick"))
        assertTrue(result.contains("Hello"))
    }

    @Test
    fun testRemovesIframes() {
        val input = """<iframe src="https://evil.com"></iframe><p>Safe</p>"""
        val result = HtmlSanitizer.sanitize(input)
        assertFalse(result.contains("<iframe"))
        assertTrue(result.contains("<p>Safe</p>"))
    }

    @Test
    fun testPreservesAllowedTags() {
        val input = """<p><a href="https://example.com">link</a> <strong>bold</strong> <em>italic</em></p>"""
        val result = HtmlSanitizer.sanitize(input)
        assertTrue(result.contains("<a href="))
        assertTrue(result.contains("<strong>bold</strong>"))
        assertTrue(result.contains("<em>italic</em>"))
    }

    @Test
    fun testPreservesTables() {
        val input = """<table><tr><td>cell</td></tr></table>"""
        val result = HtmlSanitizer.sanitize(input)
        assertTrue(result.contains("<table>"))
        assertTrue(result.contains("<td>cell</td>"))
    }

    @Test
    fun testRemovesJavascriptUrls() {
        val input = """<a href="javascript:alert('xss')">click</a>"""
        val result = HtmlSanitizer.sanitize(input)
        assertFalse(result.contains("javascript:"))
    }
}
