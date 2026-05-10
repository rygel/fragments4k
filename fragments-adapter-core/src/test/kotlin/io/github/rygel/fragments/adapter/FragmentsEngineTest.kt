package io.github.rygel.fragments.adapter

import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.static.StaticPageEngine
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FragmentsEngineTest {
    private lateinit var engine: FragmentsEngine

    @BeforeEach
    fun setUp() {
        val mockStaticEngine = mockk<StaticPageEngine>()
        val mockBlogEngine = mockk<BlogEngine>()
        val mockStaticRepo = mockk<FragmentRepository>()
        val mockBlogRepo = mockk<FragmentRepository>()
        every { mockStaticEngine.getRepository() } returns mockStaticRepo
        every { mockBlogEngine.getRepository() } returns mockBlogRepo
        engine = FragmentsEngine(staticEngine = mockStaticEngine, blogEngine = mockBlogEngine)
    }

    @Test
    fun testIsHtmxRequestWithTrue() {
        assertTrue(engine.isHtmxRequest("true"))
    }

    @Test
    fun testIsHtmxRequestWithFalse() {
        assertFalse(engine.isHtmxRequest(null))
    }

    @Test
    fun testIsHtmxRequestWithMixedCase() {
        assertTrue(engine.isHtmxRequest("True"))
    }

    @Test
    fun testIsHtmxRequestWithFalseValue() {
        assertFalse(engine.isHtmxRequest("false"))
    }

    @Test
    fun testSearchFormReturnsConfig() {
        val config = engine.searchForm()
        assertNotNull(config)
        assertEquals("/search", config.actionUrl)
        assertEquals("q", config.paramName)
        assertEquals("get", config.method)
    }

    @Test
    fun testCspHeaderReturnsPolicy() {
        val csp = engine.cspHeader()
        assertNotNull(csp)
        assertTrue(csp.contains("default-src 'self'"))
        assertTrue(csp.contains("script-src 'self'"))
    }

    @Test
    fun testNavReturnsDefaultMenu() {
        val nav = engine.nav()
        assertNotNull(nav)
        assertTrue(nav.isNotEmpty())
        assertEquals("Home", nav[0].label)
        assertEquals("Blog", nav[1].label)
    }

    @Test
    fun testFooterReturnsDefaultConfig() {
        val footer = engine.footer()
        assertNotNull(footer)
        assertEquals("Fragments4k", footer.poweredByName)
        assertEquals("\u00a9", footer.copyrightText)
    }

    @Test
    fun testGenerateRobotsTxt() {
        val robotsTxt = engine.generateRobotsTxt()
        assertTrue(robotsTxt.contains("User-agent: *"))
        assertTrue(robotsTxt.contains("Allow: /"))
        assertTrue(robotsTxt.contains("Sitemap: http://localhost:8080/sitemap.xml"))
    }

    @Test
    fun testPaginationGeneratesCorrectInfo() {
        val result = engine.pagination(1, 5, "/blog")
        assertEquals(1, result.currentPage)
        assertEquals(5, result.totalPages)
        assertFalse(result.hasPrevious)
        assertTrue(result.hasNext)
        assertEquals("Page 1 of 5", result.text)
    }

    @Test
    fun testGenerateArchiveYearLinks() {
        val links = engine.generateArchiveYearLinks(2024)
        assertNotNull(links)
        assertTrue(links.isNotEmpty())
        assertTrue(links.any { it.label == "2024" && it.isActive })
    }

    @Test
    fun testGenerateArchiveMonthLinks() {
        val links = engine.generateArchiveMonthLinks(2025, 6)
        assertNotNull(links)
        assertEquals(12, links.size)
        assertTrue(links.any { it.label == "Jun" && it.isActive })
    }

    @Test
    fun testGenerateArchiveBreadcrumbs() {
        val breadcrumbs = engine.generateArchiveBreadcrumbs(2025)
        assertNotNull(breadcrumbs)
        assertTrue(breadcrumbs.isNotEmpty())
        assertEquals("Blog", breadcrumbs[0].label)
        assertEquals("2025", breadcrumbs[1].label)
    }

    @Test
    fun testSocialShareLinksReturnsNonEmpty() {
        val links = engine.socialShareLinks("Title", "http://example.com")
        assertNotNull(links)
        assertTrue(links.isNotEmpty())
    }
}
