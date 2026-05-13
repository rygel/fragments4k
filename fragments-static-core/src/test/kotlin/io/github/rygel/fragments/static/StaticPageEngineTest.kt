package io.github.rygel.fragments.static

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentStatus
import io.github.rygel.fragments.FragmentTemplates
import io.github.rygel.fragments.test.InMemoryFragmentRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class StaticPageEngineTest {
    private lateinit var repository: InMemoryFragmentRepository
    private lateinit var engine: StaticPageEngine

    @BeforeEach
    fun setUp() {
        repository = InMemoryFragmentRepository()
        engine = StaticPageEngine(repository)
    }

    private fun aFragment(
        slug: String = "test-page",
        template: String = FragmentTemplates.STATIC,
        status: FragmentStatus = FragmentStatus.PUBLISHED,
        resolvedUrl: String? = null,
        visible: Boolean = true,
    ) = Fragment(
        title = "Test Page",
        slug = slug,
        status = status,
        date = LocalDateTime.now(),
        publishDate = null,
        preview = "preview",
        htmlContent = "<p>content</p>",
        frontMatter = emptyMap(),
        visible = visible,
        template = template,
        resolvedUrl = resolvedUrl,
    )

    @Test
    fun testGetPageReturnsMatchingFragment() =
        runBlocking {
            val fragment = aFragment(slug = "about")
            repository.addFragment(fragment)

            val result = engine.getPage("about")

            assertNotNull(result)
            assertEquals("about", result!!.slug)
        }

    @Test
    fun testGetPageReturnsNullForMissingSlug() =
        runBlocking {
            assertNull(engine.getPage("nonexistent"))
        }

    @Test
    fun testGetPageResolvesUrlWhenNull() =
        runBlocking {
            repository.addFragment(aFragment(slug = "about", resolvedUrl = null))

            val result = engine.getPage("about")

            assertNotNull(result)
            assertEquals("/page/about", result!!.resolvedUrl)
        }

    @Test
    fun testGetPagePreservesExistingResolvedUrl() =
        runBlocking {
            repository.addFragment(aFragment(slug = "about", resolvedUrl = "/custom/about-page"))

            val result = engine.getPage("about")

            assertNotNull(result)
            assertEquals("/custom/about-page", result!!.resolvedUrl)
        }

    @Test
    fun testGetAllStaticPagesFiltersByTemplate() =
        runBlocking {
            repository.addFragment(aFragment(slug = "static-page", template = FragmentTemplates.STATIC))
            repository.addFragment(aFragment(slug = "default-page", template = FragmentTemplates.DEFAULT))
            repository.addFragment(aFragment(slug = "blog-post", template = FragmentTemplates.BLOG))

            val results = engine.getAllStaticPages()

            assertEquals(2, results.size)
            val slugs = results.map { it.slug }.toSet()
            assertTrue(slugs.contains("static-page"))
            assertTrue(slugs.contains("default-page"))
        }

    @Test
    fun testGetAllStaticPagesExcludesDraftsByDefault() =
        runBlocking {
            repository.addFragment(aFragment(slug = "published", status = FragmentStatus.PUBLISHED))
            repository.addFragment(aFragment(slug = "draft", status = FragmentStatus.DRAFT, visible = false))

            val results = engine.getAllStaticPages(includeDrafts = false)

            assertEquals(1, results.size)
            assertEquals("published", results[0].slug)
        }

    @Test
    fun testGetAllStaticPagesIncludesDraftsWhenFlagSet() =
        runBlocking {
            repository.addFragment(aFragment(slug = "published", status = FragmentStatus.PUBLISHED))
            repository.addFragment(aFragment(slug = "draft", status = FragmentStatus.DRAFT, visible = false))

            val results = engine.getAllStaticPages(includeDrafts = true)

            assertEquals(2, results.size)
            val slugs = results.map { it.slug }.toSet()
            assertTrue(slugs.contains("published"))
            assertTrue(slugs.contains("draft"))
        }

    @Test
    fun testCustomPageUrlPrefix() =
        runBlocking {
            val customEngine = StaticPageEngine(repository, pageUrlPrefix = "/docs")
            repository.addFragment(aFragment(slug = "guide", resolvedUrl = null))

            val result = customEngine.getPage("guide")

            assertNotNull(result)
            assertEquals("/docs/guide", result!!.resolvedUrl)
        }
}
