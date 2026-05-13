package io.github.rygel.fragments.test

import io.github.rygel.fragments.NavigationMenuGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NavigationMenuGeneratorTest {
    @Test
    fun testDefaultMainMenuHasHomeAndBlog() {
        val menu = NavigationMenuGenerator.generateMainMenu()

        assertEquals(2, menu.size)
        assertEquals("Home", menu[0].label)
        assertEquals("/", menu[0].url)
        assertEquals("Blog", menu[1].label)
        assertEquals("/blog", menu[1].url)
    }

    @Test
    fun testMainMenuWithArchiveUrl() {
        val menu =
            NavigationMenuGenerator.generateMainMenu(
                archiveUrl = "/blog/archive",
            )

        assertEquals(3, menu.size)
        assertEquals("Home", menu[0].label)
        assertEquals("Blog", menu[1].label)
        assertEquals("Archive", menu[2].label)
        assertEquals("/blog/archive", menu[2].url)
    }

    @Test
    fun testMainMenuWithSearchUrl() {
        val menu =
            NavigationMenuGenerator.generateMainMenu(
                searchUrl = "/search",
            )

        assertEquals(3, menu.size)
        assertEquals("Home", menu[0].label)
        assertEquals("Blog", menu[1].label)
        assertEquals("Search", menu[2].label)
        assertEquals("/search", menu[2].url)
    }

    @Test
    fun testMainMenuWithAllOptionalLinksAndCustomUrls() {
        val menu =
            NavigationMenuGenerator.generateMainMenu(
                siteUrl = "https://example.com",
                blogUrl = "https://example.com/blog",
                archiveUrl = "https://example.com/archive",
                searchUrl = "https://example.com/search",
            )

        assertEquals(4, menu.size)
        assertEquals("Home", menu[0].label)
        assertEquals("https://example.com", menu[0].url)
        assertEquals("Blog", menu[1].label)
        assertEquals("https://example.com/blog", menu[1].url)
        assertEquals("Archive", menu[2].label)
        assertEquals("https://example.com/archive", menu[2].url)
        assertEquals("Search", menu[3].label)
        assertEquals("https://example.com/search", menu[3].url)
    }

    @Test
    fun testDefaultBlogMenuHasBlogHomeAndArchive() {
        val menu = NavigationMenuGenerator.generateBlogMenu()

        assertEquals(2, menu.size)
        assertEquals("Blog Home", menu[0].label)
        assertEquals("/blog", menu[0].url)
        assertEquals("Archive", menu[1].label)
        assertEquals("/blog/archive", menu[1].url)
    }

    @Test
    fun testBlogMenuWithoutArchive() {
        val menu =
            NavigationMenuGenerator.generateBlogMenu(
                archiveUrl = null,
            )

        assertEquals(1, menu.size)
        assertEquals("Blog Home", menu[0].label)
        assertEquals("/blog", menu[0].url)
    }

    @Test
    fun testBlogMenuWithCustomBaseUrl() {
        val menu =
            NavigationMenuGenerator.generateBlogMenu(
                baseUrl = "/news",
                archiveUrl = "/news/archive",
            )

        assertEquals(2, menu.size)
        assertEquals("Blog Home", menu[0].label)
        assertEquals("/news", menu[0].url)
        assertEquals("Archive", menu[1].label)
        assertEquals("/news/archive", menu[1].url)
    }
}
