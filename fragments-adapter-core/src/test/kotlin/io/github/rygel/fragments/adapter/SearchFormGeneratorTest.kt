package io.github.rygel.fragments.adapter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SearchFormGeneratorTest {
    @Test
    fun testDefaultConfig() {
        val config = SearchFormGenerator.generate()

        assertEquals("/search", config.actionUrl)
        assertEquals("q", config.paramName)
        assertEquals("Search articles...", config.placeholderText)
        assertEquals("Search", config.buttonText)
        assertEquals("get", config.method)
    }

    @Test
    fun testCustomConfig() {
        val config =
            SearchFormGenerator.generate(
                actionUrl = "/api/search",
                paramName = "query",
                placeholderText = "Type here...",
                buttonText = "Go",
                method = "post",
            )

        assertEquals("/api/search", config.actionUrl)
        assertEquals("query", config.paramName)
        assertEquals("Type here...", config.placeholderText)
        assertEquals("Go", config.buttonText)
        assertEquals("post", config.method)
    }
}
