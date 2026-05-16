package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
        val fragment =
            createFragment(
                slug = "hello-world",
                resolvedUrl = "/blog/2026/03/hello-world",
            )
        val vm = FragmentViewModel(fragment = fragment, siteUrl = "https://example.com")

        assertEquals("https://example.com/blog/2026/03/hello-world", vm.canonicalUrl)
    }

    @Test
    fun formattedDateUsesDefaultFormat() {
        val fragment = createFragment(date = LocalDateTime.of(2026, 3, 15, 10, 0))
        val vm = FragmentViewModel(fragment = fragment)

        assertEquals("March 15, 2026", vm.formattedDate)
    }

    @Test
    fun formattedDateUsesCustomFormat() {
        val fragment = createFragment(date = LocalDateTime.of(2026, 3, 15, 10, 0))
        val vm = FragmentViewModel(fragment = fragment, dateFormat = "yyyy-MM-dd")

        assertEquals("2026-03-15", vm.formattedDate)
    }

    @Test
    fun formattedDateIsEmptyWhenDateIsNull() {
        val fragment = createFragment(date = null)
        val vm = FragmentViewModel(fragment = fragment)

        assertEquals("", vm.formattedDate)
    }

    @Test
    fun contentPreviewReturnsFragmentPreview() {
        val fragment = createFragment(preview = "<p>Preview text</p>")
        val vm = FragmentViewModel(fragment = fragment)

        assertEquals("<p>Preview text</p>", vm.contentPreview)
    }

    @Test
    fun yearAndMonthExtractedFromDate() {
        val fragment = createFragment(date = LocalDateTime.of(2026, 7, 4, 12, 30))
        val vm = FragmentViewModel(fragment = fragment)

        assertEquals(2026, vm.year)
        assertEquals(7, vm.month)
    }

    @Test
    fun yearAndMonthAreNullWhenDateIsNull() {
        val fragment = createFragment(date = null)
        val vm = FragmentViewModel(fragment = fragment)

        assertEquals(null, vm.year)
        assertEquals(null, vm.month)
    }

    @Test
    fun tableOfContentsExtractsHeadings() {
        val fragment =
            createFragment(
                htmlContent = "# Introduction\n## Section 1\n### Subsection\n## Section 2",
            )
        val vm = FragmentViewModel(fragment = fragment)

        assertEquals(4, vm.tableOfContents.size)
        assertEquals("Introduction", vm.tableOfContents[0].title)
        assertEquals(1, vm.tableOfContents[0].level)
        assertEquals("section-1", vm.tableOfContents[1].anchor)
        assertEquals(2, vm.tableOfContents[1].level)
        assertEquals("subsection", vm.tableOfContents[2].anchor)
        assertEquals(3, vm.tableOfContents[2].level)
    }

    @Test
    fun tableOfContentsIsEmptyWhenNoHeadings() {
        val fragment = createFragment(htmlContent = "Just a paragraph with no headers.")
        val vm = FragmentViewModel(fragment = fragment)

        assertTrue(vm.tableOfContents.isEmpty())
    }

    @Test
    fun relatedPostsReturnsWrappedViewModels() {
        val post1 = createFragment(slug = "post-1", tags = listOf("kotlin"), htmlContent = "# Post 1")
        val post2 = createFragment(slug = "post-2", tags = listOf("kotlin"), htmlContent = "# Post 2")
        val main = createFragment(slug = "main", tags = listOf("kotlin"), htmlContent = "# Main")
        val vm = FragmentViewModel(fragment = main, allFragments = listOf(main, post1, post2))

        assertEquals(2, vm.relatedPosts.size)
        assertEquals("post-1", vm.relatedPosts[0].slug)
        assertTrue(vm.relatedPosts.all { it.formattedDate.isNotEmpty() || it.fragment.date == null })
    }

    @Test
    fun relatedPostsInheritsDateFormat() {
        val post1 = createFragment(slug = "post-1", tags = listOf("kotlin"), date = LocalDateTime.of(2026, 1, 1, 0, 0))
        val main = createFragment(slug = "main", tags = listOf("kotlin"))
        val vm = FragmentViewModel(fragment = main, allFragments = listOf(main, post1), dateFormat = "yyyy-MM-dd")

        assertEquals("2026-01-01", vm.relatedPosts[0].formattedDate)
    }

    private fun createFragment(
        slug: String = "test",
        baseUrl: String = "",
        resolvedUrl: String? = null,
        date: LocalDateTime? = LocalDateTime.of(2026, 3, 15, 10, 0),
        preview: String = "Preview",
        htmlContent: String = "Content",
        tags: List<String> = emptyList(),
    ): Fragment =
        Fragment(
            title = "Test",
            slug = slug,
            baseUrl = baseUrl,
            date = date,
            publishDate = null,
            preview = preview,
            htmlContent = htmlContent,
            frontMatter = emptyMap(),
            resolvedUrl = resolvedUrl,
            tags = tags,
        )
}
