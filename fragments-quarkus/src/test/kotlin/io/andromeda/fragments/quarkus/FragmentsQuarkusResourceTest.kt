package io.github.rygel.fragments.quarkus

import io.github.rygel.fragments.Fragment
import java.time.LocalDateTime
import org.junit.jupiter.api.Test

class FragmentsQuarkusResourceTest {

    @Test
    fun testCompilation() {
        // Simple test to verify the test framework is working
        val fragment = Fragment(
            slug = "test",
            title = "Test",
            content = "# Test Content",
            preview = "Test Content",
            date = LocalDateTime.now(),
            publishDate = LocalDateTime.now(),
            template = "page",
            visible = true,
            tags = emptyList(),
            categories = emptyList(),
            frontMatter = emptyMap()
        )
        assert(fragment.title == "Test")
    }
}
