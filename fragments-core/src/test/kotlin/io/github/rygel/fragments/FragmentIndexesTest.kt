package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FragmentIndexesTest {
    private fun makeFragment(
        slug: String,
        template: String = "default",
        visible: Boolean = true,
        status: FragmentStatus = FragmentStatus.PUBLISHED,
        tags: List<String> = emptyList(),
        categories: List<String> = emptyList(),
        authorIds: List<String> = emptyList(),
        author: String? = null,
        date: LocalDateTime? = LocalDateTime.of(2024, 1, 15, 10, 0),
    ) = Fragment(
        slug = slug,
        title = slug,
        status = status,
        visible = visible,
        template = template,
        tags = tags,
        categories = categories,
        authorIds = authorIds,
        author = author,
        date = date,
        publishDate = null,
        expiryDate = null,
        preview = "",
        htmlContent = "",
        frontMatter = emptyMap(),
    )

    @Test
    fun bySlugReturnsCorrectFragment() {
        val f1 = makeFragment("alpha")
        val f2 = makeFragment("beta")
        val indexes = FragmentIndexes.build(listOf(f1, f2))
        assertEquals(f1, indexes.bySlug["alpha"])
        assertEquals(f2, indexes.bySlug["beta"])
        assertNull(indexes.bySlug["gamma"])
    }

    @Test
    fun byTagGroupsCorrectly() {
        val f1 = makeFragment("a", tags = listOf("kotlin", "jvm"))
        val f2 = makeFragment("b", tags = listOf("Kotlin"))
        val f3 = makeFragment("c", tags = listOf("rust"))
        val indexes = FragmentIndexes.build(listOf(f1, f2, f3))
        assertEquals(listOf(f1, f2), indexes.byTag["kotlin"])
        assertEquals(listOf(f1), indexes.byTag["jvm"])
        assertEquals(listOf(f3), indexes.byTag["rust"])
        assertTrue(indexes.byTag["python"].isNullOrEmpty())
    }

    @Test
    fun byCategoryGroupsCorrectly() {
        val f1 = makeFragment("a", categories = listOf("Tutorial"))
        val f2 = makeFragment("b", categories = listOf("tutorial", "advanced"))
        val indexes = FragmentIndexes.build(listOf(f1, f2))
        assertEquals(listOf(f1, f2), indexes.byCategory["tutorial"])
        assertEquals(listOf(f2), indexes.byCategory["advanced"])
    }

    @Test
    fun byAuthorGroupsByAuthorIds() {
        val f1 = makeFragment("a", authorIds = listOf("user1"))
        val f2 = makeFragment("b", authorIds = listOf("user1", "user2"))
        val indexes = FragmentIndexes.build(listOf(f1, f2))
        assertEquals(listOf(f1, f2), indexes.byAuthor["user1"])
        assertEquals(listOf(f2), indexes.byAuthor["user2"])
    }

    @Test
    fun byAuthorIncludesLegacyAuthorField() {
        val f1 = makeFragment("a", author = "bob")
        val indexes = FragmentIndexes.build(listOf(f1))
        assertEquals(listOf(f1), indexes.byAuthor["bob"])
    }

    @Test
    fun byYearMonthGroupsByDate() {
        val f1 = makeFragment("a", date = LocalDateTime.of(2024, 3, 10, 0, 0))
        val f2 = makeFragment("b", date = LocalDateTime.of(2024, 3, 20, 0, 0))
        val f3 = makeFragment("c", date = LocalDateTime.of(2024, 5, 1, 0, 0))
        val indexes = FragmentIndexes.build(listOf(f1, f2, f3))
        assertEquals(listOf(f1, f2), indexes.byYearMonth[Pair(2024, 3)])
        assertEquals(listOf(f3), indexes.byYearMonth[Pair(2024, 5)])
        assertNull(indexes.byYearMonth[Pair(2023, 12)])
    }

    @Test
    fun allVisibleSortedExcludesDraftsAndInvisible() {
        val f1 = makeFragment("published", status = FragmentStatus.PUBLISHED, date = LocalDateTime.of(2024, 1, 1, 0, 0))
        val f2 = makeFragment("draft", status = FragmentStatus.DRAFT, date = LocalDateTime.of(2024, 2, 1, 0, 0))
        val f3 = makeFragment("invisible", visible = false, date = LocalDateTime.of(2024, 3, 1, 0, 0))
        val indexes = FragmentIndexes.build(listOf(f1, f2, f3))
        assertEquals(listOf(f1), indexes.allVisibleSorted)
    }

    @Test
    fun allVisibleSortedIsSortedByDateDescending() {
        val f1 = makeFragment("old", date = LocalDateTime.of(2024, 1, 1, 0, 0))
        val f2 = makeFragment("new", date = LocalDateTime.of(2024, 6, 1, 0, 0))
        val indexes = FragmentIndexes.build(listOf(f1, f2))
        assertEquals(listOf(f2, f1), indexes.allVisibleSorted)
    }

    @Test
    fun byStatusGroupsCorrectly() {
        val f1 = makeFragment("a", status = FragmentStatus.PUBLISHED)
        val f2 = makeFragment("b", status = FragmentStatus.DRAFT)
        val f3 = makeFragment("c", status = FragmentStatus.PUBLISHED)
        val indexes = FragmentIndexes.build(listOf(f1, f2, f3))
        assertEquals(listOf(f1, f3), indexes.byStatus[FragmentStatus.PUBLISHED])
        assertEquals(listOf(f2), indexes.byStatus[FragmentStatus.DRAFT])
    }

    @Test
    fun emptyReturnsAllEmpty() {
        val empty = FragmentIndexes.EMPTY
        assertTrue(empty.bySlug.isEmpty())
        assertTrue(empty.allVisibleSorted.isEmpty())
        assertTrue(empty.byTag.isEmpty())
    }
}
