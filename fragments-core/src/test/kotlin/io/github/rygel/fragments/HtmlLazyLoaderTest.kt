package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HtmlLazyLoaderTest {
    @Test
    fun addsLazyLoadingToImgTags() {
        val html = "<p><img src=\"/photo.jpg\"></p>"
        val result = HtmlLazyLoader.addLazyLoading(html)
        assertEquals("<p><img loading=\"lazy\" src=\"/photo.jpg\"></p>", result)
    }

    @Test
    fun doesNotDuplicateExistingLoadingAttribute() {
        val html = "<img src=\"/photo.jpg\" loading=\"eager\">"
        val result = HtmlLazyLoader.addLazyLoading(html)
        assertEquals(html, result)
    }

    @Test
    fun returnsEmptyStringForBlankInput() {
        assertEquals("", HtmlLazyLoader.addLazyLoading(""))
    }

    @Test
    fun returnsOriginalHtmlWithoutImages() {
        val html = "<p>No images here</p>"
        assertEquals(html, HtmlLazyLoader.addLazyLoading(html))
    }

    @Test
    fun handlesMultipleImgTags() {
        val html = "<img src=\"a.jpg\"><img src=\"b.jpg\">"
        val result = HtmlLazyLoader.addLazyLoading(html)
        assertEquals(2, result.split("loading=\"lazy\"").size - 1)
    }
}
