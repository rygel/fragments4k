package io.andromeda.fragments.test

import io.andromeda.fragments.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ContentRelationshipTest {

    @Test
    fun testPreviousNextPostsByDate() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            title = "First Post",
            slug = "post-1",
            date = LocalDateTime.of(2024, 1, 1, 10, 0),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment2 = Fragment(
            title = "Second Post",
            slug = "post-2",
            date = LocalDateTime.of(2024, 1, 2, 10, 0),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment3 = Fragment(
            title = "Third Post",
            slug = "post-3",
            date = LocalDateTime.of(2024, 1, 3, 10, 0),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment4 = Fragment(
            title = "Fourth Post",
            slug = "post-4",
            date = LocalDateTime.of(2024, 1, 4, 10, 0),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)
        repository.addFragment(fragment3)
        repository.addFragment(fragment4)

        val relationships = ContentRelationshipGenerator.generateRelationships(
            currentFragment = fragment3,
            allFragments = repository.getAll()
        )

        assertEquals("post-2", relationships.previous?.slug)
        assertEquals("post-4", relationships.next?.slug)
    }



    @Test
    fun testRelatedByCategories() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            title = "Technology News",
            slug = "post-1",
            categories = listOf("technology", "news"),
            tags = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment2 = Fragment(
            title = "Tech Review",
            slug = "post-2",
            categories = listOf("technology", "news"),
            tags = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment3 = Fragment(
            title = "Programming",
            slug = "post-3",
            categories = listOf("programming"),
            tags = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)
        repository.addFragment(fragment3)

        val relationships = ContentRelationshipGenerator.generateRelationships(
            currentFragment = fragment1,
            allFragments = repository.getAll()
        )

        assertFalse(relationships.relatedByCategory.isEmpty())
        assertTrue(relationships.relatedByCategory.any { it.slug == "post-2" })
    }

    @Test
    fun testRelatedByContentReferences() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            title = "Main Post",
            slug = "post-1",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "This is the main post. See also {{fragment:post-2}} and {{fragment:post-3}}",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment2 = Fragment(
            title = "Related Post 2",
            slug = "post-2",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment3 = Fragment(
            title = "Related Post 3",
            slug = "post-3",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment4 = Fragment(
            title = "Unrelated Post",
            slug = "post-4",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)
        repository.addFragment(fragment3)
        repository.addFragment(fragment4)

        val relationships = ContentRelationshipGenerator.generateRelationships(
            currentFragment = fragment1,
            allFragments = repository.getAll()
        )

        assertEquals(2, relationships.relatedByContent.size)
        assertTrue(relationships.relatedByContent.any { it.slug == "post-2" })
        assertTrue(relationships.relatedByContent.any { it.slug == "post-3" })
        assertFalse(relationships.relatedByContent.any { it.slug == "post-4" })
    }

    @Test
    fun testTranslations() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val englishFragment = Fragment(
            title = "English Post",
            slug = "post-1-en",
            language = "en",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = mapOf("translation_of" to "post-1"),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val spanishFragment = Fragment(
            title = "Spanish Post",
            slug = "post-1-es",
            language = "es",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = mapOf("translation_of" to "post-1"),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val frenchFragment = Fragment(
            title = "French Post",
            slug = "post-1-fr",
            language = "fr",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = mapOf("translation_of" to "post-1"),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(englishFragment)
        repository.addFragment(spanishFragment)
        repository.addFragment(frenchFragment)

        val relationships = ContentRelationshipGenerator.generateRelationships(
            currentFragment = englishFragment,
            allFragments = repository.getAll()
        )

        assertEquals(2, relationships.translations.size)
        assertEquals("post-1-es", relationships.translations["es"]?.slug)
        assertEquals("post-1-fr", relationships.translations["fr"]?.slug)
        assertNull(relationships.translations["en"])
    }

    @Test
    fun testNoRelationshipsWhenNoMatches() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val sameDate = LocalDateTime.of(2024, 1, 10, 10, 0)
        val fragment1 = Fragment(
            title = "Standalone Post",
            slug = "post-1",
            tags = listOf("unique"),
            categories = listOf("unique"),
            date = sameDate,
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment2 = Fragment(
            title = "Another Post",
            slug = "post-2",
            tags = listOf("different"),
            categories = listOf("different"),
            date = sameDate,
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)

        val relationships = ContentRelationshipGenerator.generateRelationships(
            currentFragment = fragment1,
            allFragments = repository.getAll()
        )

        assertNull(relationships.previous)
        assertNull(relationships.next)
        assertTrue(relationships.relatedByTag.isEmpty())
        assertTrue(relationships.relatedByCategory.isEmpty())
        assertTrue(relationships.relatedByContent.isEmpty())
        assertTrue(relationships.translations.isEmpty())
        assertFalse(relationships.hasRelationships)
    }

    @Test
    fun testContentRelationshipsViewModelIntegration() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            title = "Test Post",
            slug = "post-1",
            date = LocalDateTime.of(2024, 1, 15, 10, 0),
            tags = listOf("test", "example"),
            categories = listOf("testing"),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment2 = Fragment(
            title = "Related Post",
            slug = "post-2",
            date = LocalDateTime.of(2024, 1, 16, 10, 0),
            tags = listOf("test", "example"),
            categories = listOf("testing"),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment3 = Fragment(
            title = "Another Related Post",
            slug = "post-3",
            date = LocalDateTime.of(2024, 1, 17, 10, 0),
            tags = listOf("test", "example"),
            categories = listOf("testing"),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)
        repository.addFragment(fragment3)

        val relationships = repository.getRelationships("post-1")
        val viewModel = FragmentViewModel(fragment1, relationships = relationships)

        assertTrue(viewModel.hasRelationships)
        assertEquals(1, viewModel.relationshipRelatedPosts.size)
    }

    @Test
    fun testExcludeCurrentFromRelated() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            title = "Post 1",
            slug = "post-1",
            tags = listOf("test"),
            categories = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment2 = Fragment(
            title = "Post 2",
            slug = "post-2",
            tags = listOf("test"),
            categories = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment3 = Fragment(
            title = "Post 3",
            slug = "post-3",
            tags = listOf("test"),
            categories = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment1)
        repository.addFragment(fragment2)
        repository.addFragment(fragment3)

        val relationships = ContentRelationshipGenerator.generateRelationships(
            currentFragment = fragment2,
            allFragments = repository.getAll()
        )

        val allRelated = relationships.allRelated
        assertFalse(allRelated.any { it.slug == "post-2" })
    }

    @Test
    fun testRelationshipConfigLimits() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment1 = Fragment(
            title = "Post 1",
            slug = "post-main",
            tags = listOf("test", "example"),
            categories = emptyList(),
            date = LocalDateTime.of(2024, 1, 1, 10, 0),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment1)
        val fragments = (1..10).map { i ->
            Fragment(
                title = "Post $i",
                slug = "post-$i",
                tags = listOf("test", "example"),
                categories = emptyList(),
                date = LocalDateTime.of(2024, 1, i + 1, 10, 0),
                publishDate = null,
                preview = "Content",
                content = "Content",
                frontMatter = emptyMap(),
                visible = true,
                template = "blog",
                status = FragmentStatus.PUBLISHED
            )
        }

        fragments.forEach { repository.addFragment(it) }

        val config = RelationshipConfig(maxRelatedByTag = 3)
        val relationships = ContentRelationshipGenerator.generateRelationships(
            currentFragment = fragment1,
            allFragments = repository.getAll(),
            config = config
        )

        assertTrue(relationships.hasRelationships)
        assertEquals(3, relationships.relatedByTag.size)
    }

    @Test
    fun testRelationshipProperties() {
        val fragment1 = Fragment(
            title = "Previous",
            slug = "prev",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment2 = Fragment(
            title = "Next",
            slug = "next",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment3 = Fragment(
            title = "Related 1",
            slug = "related1",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment4 = Fragment(
            title = "Related 2",
            slug = "related2",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment5 = Fragment(
            title = "Related 3",
            slug = "related3",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val spanishFragment = Fragment(
            title = "Spanish",
            slug = "es-post",
            language = "es",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        val relationships = ContentRelationships(
            previous = fragment1,
            next = fragment2,
            relatedByTag = listOf(fragment3),
            relatedByCategory = listOf(fragment4),
            relatedByContent = listOf(fragment5),
            translations = mapOf("es" to spanishFragment)
        )

        assertTrue(relationships.hasRelationships)
        assertTrue(relationships.hasNavigation)
        assertTrue(relationships.hasRelatedPosts)
        assertTrue(relationships.hasTranslations)

        assertEquals(3, relationships.allRelated.size)
        assertEquals("prev", relationships.previous?.slug)
        assertEquals("next", relationships.next?.slug)
        assertEquals("es-post", relationships.translations["es"]?.slug)
    }

    @Test
    fun testEmptyContentReferences() = runBlocking {
        val repository = InMemoryFragmentRepository()

        val fragment = Fragment(
            title = "Post with no references",
            slug = "post-1",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "This is a simple post with no content references.",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment)

        val relationships = ContentRelationshipGenerator.generateRelationships(
            currentFragment = fragment,
            allFragments = repository.getAll()
        )

        assertTrue(relationships.relatedByContent.isEmpty())
    }

    @Test
    fun testTagSimilarityCalculation() {
        val fragment1 = Fragment(
            title = "Post 1",
            slug = "post-1",
            tags = listOf("kotlin", "programming", "tutorial"),
            categories = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment2 = Fragment(
            title = "Post 2",
            slug = "post-2",
            tags = listOf("kotlin", "programming", "advanced"),
            categories = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment3 = Fragment(
            title = "Post 3",
            slug = "post-3",
            tags = listOf("java", "programming"),
            categories = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        val relationships1 = ContentRelationshipGenerator.generateRelationships(
            currentFragment = fragment1,
            allFragments = listOf(fragment1, fragment2, fragment3)
        )

        assertFalse(relationships1.relatedByTag.isEmpty())
        assertTrue(relationships1.relatedByTag.any { it.slug == "post-2" })
        assertFalse(relationships1.relatedByTag.any { it.slug == "post-3" })
    }

    @Test
    fun testCategorySimilarityCalculation() {
        val fragment1 = Fragment(
            title = "Post 1",
            slug = "post-1",
            categories = listOf("technology", "news"),
            tags = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment2 = Fragment(
            title = "Post 2",
            slug = "post-2",
            categories = listOf("technology", "news"),
            tags = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )
        val fragment3 = Fragment(
            title = "Post 3",
            slug = "post-3",
            categories = listOf("programming"),
            tags = emptyList(),
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Content",
            content = "Content",
            frontMatter = emptyMap(),
            visible = true,
            template = "blog",
            status = FragmentStatus.PUBLISHED
        )

        val relationships1 = ContentRelationshipGenerator.generateRelationships(
            currentFragment = fragment1,
            allFragments = listOf(fragment1, fragment2, fragment3)
        )

        assertFalse(relationships1.relatedByCategory.isEmpty())
        assertTrue(relationships1.relatedByCategory.any { it.slug == "post-2" })
    }
}
