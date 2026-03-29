package io.github.rygel.fragments

import io.github.rygel.fragments.test.InMemoryContentSeriesRepository
import io.github.rygel.fragments.test.InMemoryFragmentRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ContentSeriesTest {

    private lateinit var seriesRepository: ContentSeriesRepository
    private lateinit var fragmentRepository: InMemoryFragmentRepository
    private val now = LocalDateTime.of(2024, 1, 15, 10, 0)

    @BeforeEach
    fun setup() = runBlocking {
        seriesRepository = InMemoryContentSeriesRepository()
        fragmentRepository = InMemoryFragmentRepository()
    }

    @Test
    fun testCreateContentSeries() = runBlocking {
        val series = ContentSeries(
            slug = "kotlin-tutorial",
            title = "Kotlin Tutorial Series",
            description = "A comprehensive guide to Kotlin",
            status = SeriesStatus.ACTIVE,
            order = 1,
            tags = listOf("kotlin", "tutorial"),
            categories = listOf("programming")
        )

        val result = seriesRepository.createSeries(series)

        assertTrue(result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("kotlin-tutorial", created?.slug)
        assertEquals("Kotlin Tutorial Series", created?.title)
        assertTrue(created?.isActive == true)
    }

    @Test
    fun testGetSeriesBySlug() = runBlocking {
        val series = ContentSeries(
            slug = "spring-boot-guide",
            title = "Spring Boot Guide",
            status = SeriesStatus.ACTIVE
        )

        seriesRepository.createSeries(series)

        val retrieved = seriesRepository.getBySlug("spring-boot-guide")

        assertNotNull(retrieved)
        assertEquals("Spring Boot Guide", retrieved?.title)
    }

    @Test
    fun testGetActiveSeriesOnlyActive() = runBlocking {
        seriesRepository.createSeries(
            ContentSeries(
                slug = "active-series",
                title = "Active Series",
                status = SeriesStatus.ACTIVE
            )
        )

        seriesRepository.createSeries(
            ContentSeries(
                slug = "inactive-series",
                title = "Inactive Series",
                status = SeriesStatus.INACTIVE
            )
        )

        seriesRepository.createSeries(
            ContentSeries(
                slug = "draft-series",
                title = "Draft Series",
                status = SeriesStatus.DRAFT
            )
        )

        val activeSeries = seriesRepository.getActiveSeries()

        assertEquals(1, activeSeries.size)
        assertEquals("active-series", activeSeries[0].slug)
    }

    @Test
    fun testUpdateContentSeries() = runBlocking {
        val original = ContentSeries(
            slug = "updatable-series",
            title = "Original Title",
            status = SeriesStatus.DRAFT
        )

        seriesRepository.createSeries(original)

        val updated = original.copy(
            title = "Updated Title",
            status = SeriesStatus.ACTIVE
        )

        val result = seriesRepository.updateSeries(updated)

        assertTrue(result.isSuccess)
        val retrieved = seriesRepository.getBySlug("updatable-series")
        assertEquals("Updated Title", retrieved?.title)
        assertEquals(SeriesStatus.ACTIVE, retrieved?.status)
    }

    @Test
    fun testDeleteContentSeries() = runBlocking {
        val series = ContentSeries(
            slug = "deletable-series",
            title = "Delete Me"
        )

        seriesRepository.createSeries(series)

        var retrieved = seriesRepository.getBySlug("deletable-series")
        assertNotNull(retrieved)

        val result = seriesRepository.deleteSeries("deletable-series")

        assertTrue(result.isSuccess)
        retrieved = seriesRepository.getBySlug("deletable-series")
        assertNull(retrieved)
    }

    @Test
    fun testFragmentInSeries() = runBlocking {
        fragmentRepository.addFragment(
            Fragment(
                title = "Tutorial Part 1",
                slug = "tutorial-1",
                status = FragmentStatus.PUBLISHED,
                date = now,
                publishDate = now,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "tutorial-series",
                seriesPart = 1,
                seriesTitle = "Introduction"
            )
        )

        val fragment = fragmentRepository.getBySlug("tutorial-1")

        assertTrue(fragment?.isInSeries == true)
        assertEquals("tutorial-series", fragment?.seriesSlug)
        assertEquals(1, fragment?.seriesPart)
        assertEquals("Introduction", fragment?.seriesTitle)
    }

    @Test
    fun testFragmentNotInSeries() = runBlocking {
        fragmentRepository.addFragment(
            Fragment(
                title = "Standalone Post",
                slug = "standalone",
                status = FragmentStatus.PUBLISHED,
                date = now,
                publishDate = now,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap()
            )
        )

        val fragment = fragmentRepository.getBySlug("standalone")

        assertFalse(fragment?.isInSeries == true)
        assertNull(fragment?.seriesSlug)
        assertNull(fragment?.seriesPart)
    }

    @Test
    fun testGetFragmentsInSeries() = runBlocking {
        fragmentRepository.addFragment(
            Fragment(
                title = "Part 1",
                slug = "series-1",
                status = FragmentStatus.PUBLISHED,
                date = now,
                publishDate = now,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "test-series",
                seriesPart = 1
            )
        )

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 2",
                slug = "series-2",
                status = FragmentStatus.PUBLISHED,
                date = now.plusDays(1),
                publishDate = now.plusDays(1),
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "test-series",
                seriesPart = 2
            )
        )

        fragmentRepository.addFragment(
            Fragment(
                title = "Standalone",
                slug = "standalone",
                status = FragmentStatus.PUBLISHED,
                date = now,
                publishDate = now,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap()
            )
        )

        val fragmentsInSeries = seriesRepository.getFragmentsInSeries("test-series", fragmentRepository)

        assertEquals(2, fragmentsInSeries.size)
        assertEquals("series-1", fragmentsInSeries[0].slug)
        assertEquals("series-2", fragmentsInSeries[1].slug)
    }

    @Test
    fun testSeriesNavigation() = runBlocking {
        val series = ContentSeries(
            slug = "navigation-series",
            title = "Navigation Test Series",
            status = SeriesStatus.ACTIVE
        )

        seriesRepository.createSeries(series)

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 1",
                slug = "nav-1",
                status = FragmentStatus.PUBLISHED,
                date = now,
                publishDate = now,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "navigation-series",
                seriesPart = 1
            )
        )

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 2",
                slug = "nav-2",
                status = FragmentStatus.PUBLISHED,
                date = now.plusDays(1),
                publishDate = now.plusDays(1),
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "navigation-series",
                seriesPart = 2
            )
        )

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 3",
                slug = "nav-3",
                status = FragmentStatus.PUBLISHED,
                date = now.plusDays(2),
                publishDate = now.plusDays(2),
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "navigation-series",
                seriesPart = 3
            )
        )

        val navigation = seriesRepository.getSeriesNavigation("navigation-series", "nav-2", fragmentRepository)

        assertNotNull(navigation)
        assertEquals("navigation-series", navigation?.series?.slug)
        assertEquals(3, navigation?.totalParts)
        assertNotNull(navigation?.currentPart)
        assertEquals("nav-2", navigation?.currentPart?.fragment?.slug)
        assertEquals(2, navigation?.currentPart?.partNumber)
        assertNotNull(navigation?.previousPart)
        assertEquals("nav-1", navigation?.previousPart?.fragment?.slug)
        assertNotNull(navigation?.nextPart)
        assertEquals("nav-3", navigation?.nextPart?.fragment?.slug)
    }

    @Test
    fun testSeriesNavigationFirstPart() = runBlocking {
        val series = ContentSeries(
            slug = "first-part-series",
            title = "First Part Test",
            status = SeriesStatus.ACTIVE
        )

        seriesRepository.createSeries(series)

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 1",
                slug = "first-1",
                status = FragmentStatus.PUBLISHED,
                date = now,
                publishDate = now,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "first-part-series",
                seriesPart = 1
            )
        )

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 2",
                slug = "first-2",
                status = FragmentStatus.PUBLISHED,
                date = now.plusDays(1),
                publishDate = now.plusDays(1),
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "first-part-series",
                seriesPart = 2
            )
        )

        val navigation = seriesRepository.getSeriesNavigation("first-part-series", "first-1", fragmentRepository)

        assertNotNull(navigation)
        assertTrue(navigation?.isFirstPart == true)
        assertNull(navigation?.previousPart)
        assertNotNull(navigation?.nextPart)
        assertEquals("first-2", navigation?.nextPart?.fragment?.slug)
    }

    @Test
    fun testSeriesNavigationLastPart() = runBlocking {
        val series = ContentSeries(
            slug = "last-part-series",
            title = "Last Part Test",
            status = SeriesStatus.ACTIVE
        )

        seriesRepository.createSeries(series)

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 1",
                slug = "last-1",
                status = FragmentStatus.PUBLISHED,
                date = now,
                publishDate = now,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "last-part-series",
                seriesPart = 1
            )
        )

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 2",
                slug = "last-2",
                status = FragmentStatus.PUBLISHED,
                date = now.plusDays(1),
                publishDate = now.plusDays(1),
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "last-part-series",
                seriesPart = 2
            )
        )

        val navigation = seriesRepository.getSeriesNavigation("last-part-series", "last-2", fragmentRepository)

        assertNotNull(navigation)
        assertTrue(navigation?.isLastPart == true)
        assertNotNull(navigation?.previousPart)
        assertEquals("last-1", navigation?.previousPart?.fragment?.slug)
        assertNull(navigation?.nextPart)
    }

    @Test
    fun testGetNextPartInSeries() = runBlocking {
        val series = ContentSeries(
            slug = "next-part-series",
            title = "Next Part Test",
            status = SeriesStatus.ACTIVE
        )

        seriesRepository.createSeries(series)

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 1",
                slug = "next-1",
                status = FragmentStatus.PUBLISHED,
                date = now,
                publishDate = now,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "next-part-series",
                seriesPart = 1
            )
        )

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 2",
                slug = "next-2",
                status = FragmentStatus.PUBLISHED,
                date = now.plusDays(1),
                publishDate = now.plusDays(1),
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "next-part-series",
                seriesPart = 2
            )
        )

        val nextPart = seriesRepository.getNextPartInSeries("next-part-series", 1, fragmentRepository)

        assertNotNull(nextPart)
        assertEquals("next-2", nextPart?.slug)
        assertEquals(2, nextPart?.seriesPart)
    }

    @Test
    fun testGetPreviousPartInSeries() = runBlocking {
        val series = ContentSeries(
            slug = "prev-part-series",
            title = "Previous Part Test",
            status = SeriesStatus.ACTIVE
        )

        seriesRepository.createSeries(series)

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 1",
                slug = "prev-1",
                status = FragmentStatus.PUBLISHED,
                date = now,
                publishDate = now,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "prev-part-series",
                seriesPart = 1
            )
        )

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 2",
                slug = "prev-2",
                status = FragmentStatus.PUBLISHED,
                date = now.plusDays(1),
                publishDate = now.plusDays(1),
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "prev-part-series",
                seriesPart = 2
            )
        )

        val previousPart = seriesRepository.getPreviousPartInSeries("prev-part-series", 2, fragmentRepository)

        assertNotNull(previousPart)
        assertEquals("prev-1", previousPart?.slug)
        assertEquals(1, previousPart?.seriesPart)
    }

    @Test
    fun testGetPreviousPartInSeriesFirstPart() = runBlocking {
        val series = ContentSeries(
            slug = "first-test-series",
            title = "First Test",
            status = SeriesStatus.ACTIVE
        )

        seriesRepository.createSeries(series)

        fragmentRepository.addFragment(
            Fragment(
                title = "Part 1",
                slug = "first-test-1",
                status = FragmentStatus.PUBLISHED,
                date = now,
                publishDate = now,
                preview = "Preview",
                content = "Content",
                frontMatter = emptyMap(),
                seriesSlug = "first-test-series",
                seriesPart = 1
            )
        )

        val previousPart = seriesRepository.getPreviousPartInSeries("first-test-series", 1, fragmentRepository)

        assertNull(previousPart)
    }

    @Test
    fun testSeriesStatusProperties() {
        val activeStatus = SeriesStatus.ACTIVE
        val inactiveStatus = SeriesStatus.INACTIVE
        val draftStatus = SeriesStatus.DRAFT

        assertTrue(activeStatus.isActive)
        assertFalse(activeStatus.isInactive)
        assertFalse(activeStatus.isDraft)

        assertFalse(inactiveStatus.isActive)
        assertTrue(inactiveStatus.isInactive)
        assertFalse(inactiveStatus.isDraft)

        assertFalse(draftStatus.isActive)
        assertFalse(draftStatus.isInactive)
        assertTrue(draftStatus.isDraft)
    }

    @Test
    fun testSeriesProgress() = runBlocking {
        val series = ContentSeries(
            slug = "progress-series",
            title = "Progress Test",
            status = SeriesStatus.ACTIVE
        )

        seriesRepository.createSeries(series)

        repeat(5) { index ->
            fragmentRepository.addFragment(
                Fragment(
                    title = "Part ${index + 1}",
                    slug = "progress-${index + 1}",
                    status = FragmentStatus.PUBLISHED,
                    date = now.plusDays(index.toLong()),
                    publishDate = now.plusDays(index.toLong()),
                    preview = "Preview",
                    content = "Content",
                    frontMatter = emptyMap(),
                    seriesSlug = "progress-series",
                    seriesPart = index + 1
                )
            )
        }

        val navigation2 = seriesRepository.getSeriesNavigation("progress-series", "progress-2", fragmentRepository)
        assertEquals(40, navigation2?.progress)

        val navigation4 = seriesRepository.getSeriesNavigation("progress-series", "progress-4", fragmentRepository)
        assertEquals(80, navigation4?.progress)
    }

    @Test
    fun testSeriesPartTitleGeneration() {
        val fragment1 = Fragment(
            title = "Tutorial",
            slug = "test",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap(),
            seriesSlug = "series",
            seriesPart = 1,
            seriesTitle = null
        )

        assertEquals("Part 1", fragment1.seriesPartTitle)

        val fragment2 = Fragment(
            title = "Tutorial",
            slug = "test2",
            status = FragmentStatus.PUBLISHED,
            date = now,
            publishDate = now,
            preview = "Preview",
            content = "Content",
            frontMatter = emptyMap(),
            seriesSlug = "series",
            seriesPart = 2,
            seriesTitle = "Advanced Topics"
        )

        assertEquals("Advanced Topics", fragment2.seriesPartTitle)
    }

    @Test
    fun testGetSeriesByTag() = runBlocking {
        seriesRepository.createSeries(
            ContentSeries(
                slug = "kotlin-series",
                title = "Kotlin Series",
                tags = listOf("kotlin", "tutorial"),
                status = SeriesStatus.ACTIVE
            )
        )

        seriesRepository.createSeries(
            ContentSeries(
                slug = "java-series",
                title = "Java Series",
                tags = listOf("java", "tutorial"),
                status = SeriesStatus.ACTIVE
            )
        )

        seriesRepository.createSeries(
            ContentSeries(
                slug = "other-series",
                title = "Other Series",
                tags = listOf("other"),
                status = SeriesStatus.ACTIVE
            )
        )

        val kotlinSeries = seriesRepository.getSeriesByTag("kotlin")

        assertEquals(1, kotlinSeries.size)
        assertEquals("kotlin-series", kotlinSeries[0].slug)
    }

    @Test
    fun testGetSeriesByCategory() = runBlocking {
        seriesRepository.createSeries(
            ContentSeries(
                slug = "programming-series",
                title = "Programming Series",
                categories = listOf("programming"),
                status = SeriesStatus.ACTIVE
            )
        )

        seriesRepository.createSeries(
            ContentSeries(
                slug = "design-series",
                title = "Design Series",
                categories = listOf("design"),
                status = SeriesStatus.ACTIVE
            )
        )

        val programmingSeries = seriesRepository.getSeriesByCategory("programming")

        assertEquals(1, programmingSeries.size)
        assertEquals("programming-series", programmingSeries[0].slug)
    }
}
