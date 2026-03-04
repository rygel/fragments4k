package io.andromeda.fragments.test

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentViewModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

/**
 * Tests for HTMX partial vs full rendering functionality.
 * Tests FragmentViewModel behavior with HTMX request headers.
 */
class HtmxRenderingTest {

    @Test
    fun testViewModelDefaultsToFullRender() {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Test content",
            preview = "Preview",
            date = LocalDateTime.of(2024, 3, 1, 10, 0),
            frontMatter = emptyMap(),
            visible = true,
            template = "default"
        )

        val viewModel = FragmentViewModel(fragment)

        assertFalse(viewModel.isPartialRender, "ViewModel should default to full render")
        assertEquals("Test Post", viewModel.title)
        assertEquals("Test content", viewModel.content)
    }

    @Test
    fun testViewModelExplicitFullRender() {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Test content",
            preview = "Preview",
            date = LocalDateTime.of(2024, 3, 1, 10, 0),
            frontMatter = emptyMap(),
            visible = true,
            template = "default"
        )

        val viewModel = FragmentViewModel(
            fragment = fragment,
            isPartialRender = false
        )

        assertFalse(viewModel.isPartialRender, "ViewModel should be in full render mode")
        assertEquals("Test Post", viewModel.title)
        assertEquals("test-post", viewModel.slug)
    }

    @Test
    fun testViewModelPartialRender() {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Test content",
            preview = "Preview",
            date = LocalDateTime.of(2024, 3, 1, 10, 0),
            frontMatter = emptyMap(),
            visible = true,
            template = "default"
        )

        val viewModel = FragmentViewModel(
            fragment = fragment,
            isPartialRender = true
        )

        assertTrue(viewModel.isPartialRender, "ViewModel should be in partial render mode")
        assertEquals("Test Post", viewModel.title)
        assertEquals("test-post", viewModel.slug)
    }

    @Test
    fun testFromHtmxRequestWithHxRequestTrue() {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Content",
            preview = "Preview",
            date = LocalDateTime.of(2024, 3, 1, 10, 0),
            frontMatter = emptyMap(),
            visible = true,
            template = "default"
        )

        val headers = mapOf(
            "HX-Request" to "true",
            "HX-Current-URL" to "http://localhost:8080/blog/test-post"
        )

        val isHtmxRequest = headers[FragmentViewModel.HTMX_REQUEST_HEADER]?.lowercase() == "true"
        val viewModel = FragmentViewModel(fragment, isPartialRender = isHtmxRequest)

        assertTrue(viewModel.isPartialRender, "ViewModel should detect HTMX request")
        assertEquals("Test Post", viewModel.title)
    }

    @Test
    fun testFromHtmxRequestWithHxRequestFalse() {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Content",
            preview = "Preview",
            date = LocalDateTime.of(2024, 3, 1, 10, 0),
            frontMatter = emptyMap(),
            visible = true,
            template = "default"
        )

        val headers = mapOf(
            "HX-Request" to "false"
        )

        val isHtmxRequest = headers[FragmentViewModel.HTMX_REQUEST_HEADER]?.lowercase() == "true"
        val viewModel = FragmentViewModel(fragment, isPartialRender = isHtmxRequest)

        assertFalse(viewModel.isPartialRender, "ViewModel should not be partial when HX-Request is false")
        assertEquals("Test Post", viewModel.title)
    }

    @Test
    fun testFromHtmxRequestMissingHeader() {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Content",
            preview = "Preview",
            date = LocalDateTime.of(2024, 3, 1, 10, 0),
            frontMatter = emptyMap(),
            visible = true,
            template = "default"
        )

        val headers = emptyMap<String, String>()

        val isHtmxRequest = headers[FragmentViewModel.HTMX_REQUEST_HEADER]?.lowercase() == "true"
        val viewModel = FragmentViewModel(fragment, isPartialRender = isHtmxRequest)

        assertFalse(viewModel.isPartialRender, "ViewModel should default to full render when header is missing")
        assertEquals("Test Post", viewModel.title)
    }

    @Test
    fun testFromHtmxRequestCaseInsensitive() {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Content",
            preview = "Preview",
            date = LocalDateTime.of(2024, 3, 1, 10, 0),
            frontMatter = emptyMap(),
            visible = true,
            template = "default"
        )

        val headers = mapOf(
            "HX-Request" to "TRUE"
        )

        val isHtmxRequest = headers[FragmentViewModel.HTMX_REQUEST_HEADER]?.lowercase() == "true"
        val viewModel = FragmentViewModel(fragment, isPartialRender = isHtmxRequest)

        assertTrue(viewModel.isPartialRender, "ViewModel should detect HTMX request with uppercase TRUE")
        assertEquals("Test Post", viewModel.title)
    }

    @Test
    fun testViewModelWithAdditionalContext() {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "Content",
            preview = "Preview",
            date = LocalDateTime.of(2024, 3, 1, 10, 0),
            frontMatter = emptyMap(),
            visible = true,
            template = "default"
        )

        val context = mapOf(
            "author" to "John Doe",
            "readTime" to "5 min",
            "viewCount" to 1234
        )

        val viewModel = FragmentViewModel(
            fragment = fragment,
            isPartialRender = false,
            additionalContext = context
        )

        assertEquals("Test Post", viewModel.title)
        assertEquals(context, viewModel.additionalContext)
        assertEquals(3, viewModel.additionalContext.size)
        assertEquals("John Doe", viewModel.additionalContext["author"])
    }

    @Test
    fun testViewModelPreservesFragmentProperties() {
        val fragment = Fragment(
            slug = "test-post",
            title = "Test Post",
            content = "This is the main content",
            preview = "Preview text",
            date = LocalDateTime.of(2024, 3, 1, 10, 0),
            frontMatter = mapOf(
                "tags" to listOf("kotlin", "testing"),
                "categories" to listOf("programming", " tutorials")
            ),
            visible = true,
            template = "blog_post"
        )

        val viewModel = FragmentViewModel(fragment, isPartialRender = true)

        assertEquals("This is the main content", viewModel.content)
        assertEquals("Preview text", viewModel.preview)
        assertEquals("test-post", viewModel.slug)
        assertEquals("blog_post", viewModel.template)
        assertEquals(listOf("kotlin", "testing"), viewModel.tags)
        assertEquals(listOf("programming", " tutorials"), viewModel.categories)
        assertNotNull(viewModel.date)
    }

    @Test
    fun testMultipleViewModelsMixedRenderModes() {
        val fragment1 = Fragment(
            slug = "post-1",
            title = "Post 1",
            content = "Content 1",
            preview = "Preview 1",
            date = LocalDateTime.of(2024, 3, 1, 10, 0),
            frontMatter = emptyMap(),
            visible = true,
            template = "default"
        )
        val fragment2 = Fragment(
            slug = "post-2",
            title = "Post 2",
            content = "Content 2",
            preview = "Preview 2",
            date = LocalDateTime.of(2024, 3, 2, 10, 0),
            frontMatter = emptyMap(),
            visible = true,
            template = "default"
        )

        val viewModels = listOf(
            FragmentViewModel(fragment1, isPartialRender = true),
            FragmentViewModel(fragment2, isPartialRender = false),
            FragmentViewModel(fragment1)  // defaults to false
        )

        assertTrue(viewModels[0].isPartialRender, "First should be partial")
        assertFalse(viewModels[1].isPartialRender, "Second should be full")
        assertFalse(viewModels[2].isPartialRender, "Third should be full (default)")
    }
}
