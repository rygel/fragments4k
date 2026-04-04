package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FragmentTest {

    @Test
    fun fragmentImageFieldIsAccessible() {
        val fragment = Fragment(
            title = "Post with Image",
            slug = "post-with-image",
            date = LocalDateTime.of(2024, 6, 1, 12, 0),
            publishDate = null,
            preview = "A post that has a cover image",
            content = "<p>Content with image</p>",
            frontMatter = mapOf("image" to "/static/images/cover.jpg"),
            image = "/static/images/cover.jpg"
        )

        assertEquals("/static/images/cover.jpg", fragment.image)
    }

    @Test
    fun fragmentImageFieldDefaultsToNull() {
        val fragment = Fragment(
            title = "Post without Image",
            slug = "post-without-image",
            date = LocalDateTime.of(2024, 6, 1, 12, 0),
            publishDate = null,
            preview = "A post without a cover image",
            content = "<p>Content without image</p>",
            frontMatter = emptyMap()
        )

        assertNull(fragment.image)
    }
}
