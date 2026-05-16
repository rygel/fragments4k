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

    @Test
    fun testPreservesMoreTagPlaceholder() {
        val input = """<p>Preview content</p><!--more--><p>Full content</p>"""
        val result = HtmlSanitizer.sanitize(input)
        assertTrue(result.contains("<!--more-->"), "The more-tag marker must survive sanitization")
    }

    @Test
    fun testPreservesAllowedAttributes() {
        val input = """<p class="intro" id="first">Hello</p>"""
        val result = HtmlSanitizer.sanitize(input)
        assertTrue(result.contains("class=\"intro\""))
        assertTrue(result.contains("id=\"first\""))
    }

    @Test
    fun testPreservesDetailsSummary() {
        val input = """<details><summary>Click</summary><p>Content</p></details>"""
        val result = HtmlSanitizer.sanitize(input)
        assertTrue(result.contains("<details>"))
        assertTrue(result.contains("<summary>Click</summary>"))
    }

    @Test
    fun testStrictProfileRemovesClassAndId() {
        val input = """<p class="intro" id="first">Hello</p><script>alert(1)</script>"""
        val result = HtmlSanitizer.sanitize(input, SanitizerProfile.STRICT)
        assertFalse(result.contains("class="))
        assertFalse(result.contains("id="))
        assertFalse(result.contains("<script"))
        assertTrue(result.contains("<p>Hello</p>"))
    }

    @Test
    fun testStrictProfilePreservesBasicFormatting() {
        val input = """<p><strong>bold</strong> <em>italic</em> <a href="https://example.com">link</a></p>"""
        val result = HtmlSanitizer.sanitize(input, SanitizerProfile.STRICT)
        assertTrue(result.contains("<strong>bold</strong>"))
        assertTrue(result.contains("<em>italic</em>"))
        assertTrue(result.contains("<a href="))
    }

    @Test
    fun testDefaultProfileIsRelaxedTrusted() {
        val input = """<p class="intro">Hello</p>"""
        val defaultResult = HtmlSanitizer.sanitize(input)
        val explicitResult = HtmlSanitizer.sanitize(input, SanitizerProfile.RELAXED_TRUSTED_AUTHOR)
        assertTrue(defaultResult.contains("class="))
        assertTrue(explicitResult.contains("class="))
    }
}
