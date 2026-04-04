package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FragmentViewModelTest {

    @Test
    fun canonicalUrlCombinesSiteUrlAndFragmentUrl() {
        val fragment = createFragment(slug = "hello-world", baseUrl = "/blog/2026/03")
        val vm = FragmentViewModel(fragment = fragment, siteUrl = "https://example.com")

        assertEquals("https://example.com/blog/2026/03/hello-world", vm.canonicalUrl)
    }

    @Test
    fun canonicalUrlIsEmptyWhenSiteUrlNotSet() {
        val fragment = createFragment(slug = "hello-world")
        val vm = FragmentViewModel(fragment = fragment)

        assertEquals("", vm.canonicalUrl)
    }

    @Test
    fun canonicalUrlUsesResolvedUrlWhenPresent() {
        val fragment = createFragment(
            slug = "hello-world",
            resolvedUrl = "/blog/2026/03/hello-world"
        )
        val vm = FragmentViewModel(fragment = fragment, siteUrl = "https://example.com")

        assertEquals("https://example.com/blog/2026/03/hello-world", vm.canonicalUrl)
    }

    private fun createFragment(
        slug: String,
        baseUrl: String = "",
        resolvedUrl: String? = null
    ): Fragment {
        return Fragment(
            title = "Test",
            slug = slug,
            baseUrl = baseUrl,
            date = LocalDateTime.of(2026, 3, 15, 10, 0),
            publishDate = null,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap(),
            resolvedUrl = resolvedUrl
        )
    }
}
