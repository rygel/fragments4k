package io.github.rygel.fragments

import io.github.rygel.fragments.test.InMemoryFragmentRepository
import io.github.rygel.fragments.test.InMemoryFragmentRevisionRepository as TestInMemoryFragmentRevisionRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FragmentRevisionTest {

    private lateinit var repository: InMemoryFragmentRepository
    private lateinit var revisionRepository: TestInMemoryFragmentRevisionRepository

    @BeforeEach
    fun setUp() {
        repository = InMemoryFragmentRepository()
        revisionRepository = TestInMemoryFragmentRevisionRepository()
    }

    @Test
    fun fragmentRevisionHasCorrectProperties() = runBlocking {
        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        val revision = revisionRepository.saveRevision(fragment, "user1", "Initial revision")

        assertEquals("test-fragment", revision.fragmentSlug)
        assertEquals(1, revision.version)
        assertEquals("Test Fragment", revision.title)
        assertEquals("Test content", revision.content)
        assertEquals("user1", revision.changedBy)
        assertEquals("Initial revision", revision.changeReason)
        assertNotNull(revision.changedAt)
        assertTrue(revision.isInitial)
        assertNull(revision.previousRevisionId)
    }

    @Test
    fun saveRevisionIncrementsVersion() = runBlocking {
        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        val revision1 = revisionRepository.saveRevision(fragment, "user1", "Revision 1")
        val revision2 = revisionRepository.saveRevision(fragment, "user1", "Revision 2")
        val revision3 = revisionRepository.saveRevision(fragment, "user2", "Revision 3")

        assertEquals(1, revision1.version)
        assertEquals(2, revision2.version)
        assertEquals(3, revision3.version)
        assertEquals(revision1.id, revision2.previousRevisionId)
        assertEquals(revision2.id, revision3.previousRevisionId)
    }

    @Test
    fun getRevisionsReturnsAllRevisionsForFragment() = runBlocking {
        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        revisionRepository.saveRevision(fragment, "user1", "Revision 1")
        revisionRepository.saveRevision(fragment, "user1", "Revision 2")
        revisionRepository.saveRevision(fragment, "user2", "Revision 3")

        val revisions = revisionRepository.getRevisions("test-fragment")
        assertEquals(3, revisions.size)
        assertEquals(3, revisions[0].version)
        assertEquals(2, revisions[1].version)
        assertEquals(1, revisions[2].version)
    }

    @Test
    fun getRevisionByIdReturnsCorrectRevision() = runBlocking {
        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        val revision = revisionRepository.saveRevision(fragment, "user1", "Initial revision")
        val retrieved = revisionRepository.getRevision(revision.id)

        assertNotNull(retrieved)
        assertEquals(revision.id, retrieved!!.id)
        assertEquals(revision.version, retrieved.version)
        assertEquals(revision.title, retrieved.title)
    }

    @Test
    fun getLatestRevisionReturnsMostRecent() = runBlocking {
        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        revisionRepository.saveRevision(fragment, "user1", "Revision 1")
        revisionRepository.saveRevision(fragment, "user1", "Revision 2")
        revisionRepository.saveRevision(fragment, "user2", "Revision 3")

        val latest = revisionRepository.getLatestRevision("test-fragment")
        assertNotNull(latest)
        assertEquals(3, latest!!.version)
        assertEquals("Revision 3", latest.changeReason)
    }

    @Test
    fun getRevisionAtVersionReturnsCorrectVersion() = runBlocking {
        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        revisionRepository.saveRevision(fragment, "user1", "Revision 1")
        revisionRepository.saveRevision(fragment, "user1", "Revision 2")
        revisionRepository.saveRevision(fragment, "user2", "Revision 3")

        val revision = revisionRepository.getRevisionAtVersion("test-fragment", 2)
        assertNotNull(revision)
        assertEquals(2, revision!!.version)
        assertEquals("Revision 2", revision.changeReason)
    }

    @Test
    fun revertToRevisionCreatesCorrectFragment() = runBlocking {
        val fragment = Fragment(
            title = "Original Title",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Original preview",
            content = "Original content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        val revision = revisionRepository.saveRevision(fragment, "user1", "Initial revision")

        val result = revisionRepository.revertToRevision("test-fragment", revision.id, "user2", "Reverting to initial")
        assertTrue(result.isSuccess)

        val revertedFragment = result.getOrNull()
        assertNotNull(revertedFragment)
        assertEquals("Original Title", revertedFragment!!.title)
        assertEquals("Original content", revertedFragment.content)
        assertEquals("test-fragment", revertedFragment.slug)
    }

    @Test
    fun revertToRevisionFailsForInvalidId() = runBlocking {
        val result = revisionRepository.revertToRevision("test-fragment", "invalid-id", "user2", "Reverting")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun deleteRevisionsRemovesAllRevisions() = runBlocking {
        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        revisionRepository.saveRevision(fragment, "user1", "Revision 1")
        revisionRepository.saveRevision(fragment, "user1", "Revision 2")
        revisionRepository.saveRevision(fragment, "user2", "Revision 3")

        val deleted = revisionRepository.deleteRevisions("test-fragment")
        assertEquals(3, deleted)

        val revisions = revisionRepository.getRevisions("test-fragment")
        assertEquals(0, revisions.size)
    }

    @Test
    fun getRevisionCountReturnsCorrectCount() = runBlocking {
        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        assertEquals(0, revisionRepository.getRevisionCount("test-fragment"))

        revisionRepository.saveRevision(fragment, "user1", "Revision 1")
        assertEquals(1, revisionRepository.getRevisionCount("test-fragment"))

        revisionRepository.saveRevision(fragment, "user1", "Revision 2")
        revisionRepository.saveRevision(fragment, "user2", "Revision 3")
        assertEquals(3, revisionRepository.getRevisionCount("test-fragment"))
    }

    @Test
    fun getAllRevisionSlugsReturnsAllSlugs() = runBlocking {
        val fragment1 = Fragment(
            title = "Test Fragment 1",
            slug = "test-fragment-1",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        val fragment2 = Fragment(
            title = "Test Fragment 2",
            slug = "test-fragment-2",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        revisionRepository.saveRevision(fragment1, "user1", "Revision 1")
        revisionRepository.saveRevision(fragment2, "user1", "Revision 2")

        val slugs = revisionRepository.getAllRevisionSlugs()
        assertEquals(2, slugs.size)
        assertTrue(slugs.contains("test-fragment-1"))
        assertTrue(slugs.contains("test-fragment-2"))
    }

    @Test
    fun deleteRevisionsBeforeRemovesOldRevisions() = runBlocking {
        val now = LocalDateTime.now()

        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        revisionRepository.saveRevision(fragment, "user1", "Old revision")
        
        Thread.sleep(100)
        
        val newNow = LocalDateTime.now()
        revisionRepository.saveRevision(fragment, "user1", "New revision")

        val deleted = revisionRepository.deleteRevisionsBefore("test-fragment", newNow)
        assertEquals(1, deleted)

        val revisions = revisionRepository.getRevisions("test-fragment")
        assertEquals(1, revisions.size)
        assertEquals("New revision", revisions[0].changeReason)
    }

    @Test
    fun repositoryCreateRevisionWorks() = runBlocking {
        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment)

        val result = repository.createRevision("test-fragment", "user1", "Initial revision")
        assertTrue(result.isSuccess)

        val revision = result.getOrNull()
        assertNotNull(revision)
        assertEquals("test-fragment", revision!!.fragmentSlug)
        assertEquals("Test Fragment", revision.title)
    }

    @Test
    fun repositoryGetFragmentRevisionsWorks() = runBlocking {
        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment)
        repository.createRevision("test-fragment", "user1", "Revision 1")
        repository.createRevision("test-fragment", "user1", "Revision 2")

        val revisions = repository.getFragmentRevisions("test-fragment")
        assertEquals(2, revisions.size)
    }

    @Test
    fun repositoryRevertToRevisionWorks() = runBlocking {
        val fragment = Fragment(
            title = "Original Title",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Original preview",
            content = "Original content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        repository.addFragment(fragment)
        val revisionResult = repository.createRevision("test-fragment", "user1", "Initial revision")
        assertTrue(revisionResult.isSuccess)

        val revisionId = revisionResult.getOrNull()!!.id

        val revertResult = repository.revertToRevision("test-fragment", revisionId, "user2", "Reverting")
        assertTrue(revertResult.isSuccess)

        val revertedFragment = revertResult.getOrNull()
        assertNotNull(revertedFragment)
        assertEquals("Original Title", revertedFragment!!.title)
        assertEquals("Original content", revertedFragment.content)

        val currentFragment = repository.getBySlug("test-fragment")
        assertNotNull(currentFragment)
        assertEquals("Original Title", currentFragment!!.title)
        assertEquals("Original content", currentFragment.content)
    }

    @Test
    fun revisionRepositoryClearResetsState() = runBlocking {
        val fragment = Fragment(
            title = "Test Fragment",
            slug = "test-fragment",
            date = LocalDateTime.now(),
            publishDate = null,
            preview = "Test preview",
            content = "Test content",
            frontMatter = emptyMap(),
            status = FragmentStatus.PUBLISHED
        )

        revisionRepository.saveRevision(fragment, "user1", "Revision 1")
        revisionRepository.saveRevision(fragment, "user1", "Revision 2")

        assertEquals(2, revisionRepository.getRevisionCount("test-fragment"))
        assertEquals(1, revisionRepository.getAllRevisionSlugs().size)

        revisionRepository.clear()

        assertEquals(0, revisionRepository.getRevisionCount("test-fragment"))
        assertEquals(0, revisionRepository.getAllRevisionSlugs().size)
    }
}
