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

    @Test
    fun fragmentFaqEntriesAreAccessible() {
        val faqEntries = listOf(
            FaqEntry("What is fragments4k?", "A content engine for Kotlin."),
            FaqEntry("What frameworks?", "HTTP4k, Spring Boot, Javalin, Quarkus, Micronaut.")
        )
        val fragment = Fragment(
            title = "FAQ Post",
            slug = "faq-post",
            date = LocalDateTime.of(2024, 6, 1, 12, 0),
            publishDate = null,
            preview = "A post with FAQ entries",
            content = "<p>Content</p>",
            frontMatter = emptyMap(),
            faq = faqEntries
        )

        assertEquals(2, fragment.faq.size)
        assertEquals("What is fragments4k?", fragment.faq[0].question)
        assertEquals("A content engine for Kotlin.", fragment.faq[0].answer)
        assertEquals("What frameworks?", fragment.faq[1].question)
        assertEquals("HTTP4k, Spring Boot, Javalin, Quarkus, Micronaut.", fragment.faq[1].answer)
    }

    @Test
    fun fragmentFaqDefaultsToEmptyList() {
        val fragment = Fragment(
            title = "No FAQ Post",
            slug = "no-faq",
            date = LocalDateTime.of(2024, 6, 1, 12, 0),
            publishDate = null,
            preview = "A post without FAQ",
            content = "<p>Content</p>",
            frontMatter = emptyMap()
        )

        assertTrue(fragment.faq.isEmpty())
    }
}
