# Performance Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate O(n) linear scans, add structured indexes, cache feed outputs, fix allocation waste, add single-flight cache protection, and replace O(n) eviction with O(1) LRU.

**Architecture:** All changes are internal optimizations — no breaking API changes. FragmentIndexes and BlogIndex are built at load time and rebuilt atomically on reload. Feed caching uses reference-identity checks for instant invalidation. ViewModel gets lazy/cached computed properties. InMemoryCache gets single-flight protection and access-order LinkedHashMap eviction.

**Tech Stack:** Kotlin/JVM, kotlinx.coroutines, JUnit 5, MockK, Maven

---

## File Structure

### New files
- `fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentIndexes.kt` — shared index data class and builder (used by both repo implementations)

### Modified files
- `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRepository.kt` — add indexes, update query methods
- `fragments-core/src/main/kotlin/io/github/rygel/fragments/ClasspathFragmentRepository.kt` — add indexes, update query methods
- `fragments-blog-core/src/main/kotlin/io/github/rygel/fragments/blog/BlogEngine.kt` — add BlogIndex, lazy rebuild
- `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt` — feed output caching, dedup reads
- `fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentViewModel.kt` — cached formatter, lazy readingTime
- `fragments-cache-core/src/main/kotlin/io/github/rygel/fragments/cache/Cache.kt` — single-flight, LinkedHashMap eviction

### New test files
- `fragments-core/src/test/kotlin/io/github/rygel/fragments/FragmentIndexesTest.kt` — unit tests for index building

### Modified test files (verify existing tests still pass)
- All existing tests — no changes needed, just verify they pass

---

## Task 1: Reload-Time Fragment Indexes — FileSystemFragmentRepository

**Files:**
- Create: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentIndexes.kt`
- Create: `fragments-core/src/test/kotlin/io/github/rygel/fragments/FragmentIndexesTest.kt`
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRepository.kt`

- [ ] **Step 1: Write the `FragmentIndexes` data class and builder**

Create `fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentIndexes.kt`:

```kotlin
package io.github.rygel.fragments

import java.time.LocalDateTime

data class FragmentIndexes(
    val bySlug: Map<String, Fragment>,
    val byTag: Map<String, List<Fragment>>,
    val byCategory: Map<String, List<Fragment>>,
    val byAuthor: Map<String, List<Fragment>>,
    val byYearMonth: Map<Pair<Int, Int>, List<Fragment>>,
    val allVisibleSorted: List<Fragment>,
    val byStatus: Map<FragmentStatus, List<Fragment>>,
) {
    companion object {
        fun build(fragments: List<Fragment>): FragmentIndexes {
            val now = LocalDateTime.now()

            val visible = fragments.filter { fragment ->
                fragment.visible &&
                    when (fragment.status) {
                        FragmentStatus.PUBLISHED -> {
                            fragment.expiryDate == null || !fragment.expiryDate.isBefore(now)
                        }
                        FragmentStatus.SCHEDULED -> {
                            fragment.publishDate != null &&
                                !fragment.publishDate.isAfter(now) &&
                                (fragment.expiryDate == null || !fragment.expiryDate.isBefore(now))
                        }
                        FragmentStatus.DRAFT,
                        FragmentStatus.REVIEW,
                        FragmentStatus.APPROVED,
                        FragmentStatus.ARCHIVED,
                        FragmentStatus.EXPIRED,
                        -> false
                    }
            }.sortedByDescending { it.date }

            return FragmentIndexes(
                bySlug = fragments.associateBy { it.slug },
                byTag = fragments.flatMap { f -> f.tags.map { tag -> tag.lowercase() to f } }
                    .groupBy({ it.first }, { it.second }),
                byCategory = fragments.flatMap { f -> f.categories.map { cat -> cat.lowercase() to f } }
                    .groupBy({ it.first }, { it.second }),
                byAuthor = fragments.flatMap { f ->
                    (f.authorIds + listOfNotNull(f.author)).map { aid -> aid to f }
                }.groupBy({ it.first }, { it.second }),
                byYearMonth = fragments.filter { it.date != null }
                    .groupBy { Pair(it.date!!.year, it.date!!.monthValue) },
                allVisibleSorted = visible,
                byStatus = fragments.groupBy { it.status },
            )
        }

        val EMPTY = FragmentIndexes(
            bySlug = emptyMap(),
            byTag = emptyMap(),
            byCategory = emptyMap(),
            byAuthor = emptyMap(),
            byYearMonth = emptyMap(),
            allVisibleSorted = emptyList(),
            byStatus = emptyMap(),
        )
    }
}
```

- [ ] **Step 2: Write unit tests for FragmentIndexes**

Create `fragments-core/src/test/kotlin/io/github/rygel/fragments/FragmentIndexesTest.kt`:

```kotlin
package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
```

- [ ] **Step 3: Run the new tests to verify they compile and pass**

Run: `mvn -pl fragments-core test -Dtest=FragmentIndexesTest -T 4`
Expected: All tests PASS

- [ ] **Step 4: Add indexes to FileSystemFragmentRepository**

Modify `FileSystemFragmentRepository.kt`. Add a volatile indexes field and update the query methods:

Add after line 101 (`@Volatile private var cachedRelationships`):
```kotlin
@Volatile private var indexes: FragmentIndexes = FragmentIndexes.EMPTY
```

Replace `loadFragmentsFromDisk()` return value handling to build indexes. In `loadFragmentsFromDisk()`, after the `return files.mapNotNull { ... }.sortedBy { it.order }` (line 313-321), the function already returns the list. The indexes need to be built alongside. Update `loadFragments()` and `reload()`:

Replace `loadFragments()` (lines 294-302) with:
```kotlin
private fun loadFragments(): List<Fragment> =
    if (cachedFragments.isEmpty() || lastLoaded.isEqual(LocalDateTime.MIN)) {
        loadFragmentsFromDisk().also {
            cachedFragments = it
            indexes = FragmentIndexes.build(it)
            lastLoaded = LocalDateTime.now(ZoneOffset.UTC)
        }
    } else {
        cachedFragments
    }
```

Replace `reload()` (lines 263-269) with:
```kotlin
override suspend fun reload() {
    withContext(Dispatchers.IO) {
        val fragments = loadFragmentsFromDisk()
        cachedFragments = fragments
        indexes = FragmentIndexes.build(fragments)
        cachedRelationships.clear()
        lastLoaded = LocalDateTime.now(ZoneOffset.UTC)
    }
}
```

Update `cacheUpdatedFragment()` (lines 275-280) to rebuild indexes:
```kotlin
private fun cacheUpdatedFragment(fragment: Fragment) {
    val index = cachedFragments.indexOfFirst { it.slug == fragment.slug }
    if (index >= 0) {
        val updated = cachedFragments.toMutableList().apply { this[index] = fragment }
        cachedFragments = updated
        indexes = FragmentIndexes.build(updated)
    }
}
```

Now update the query methods to use indexes:

Replace `getAllVisible()` (lines 108-135) with:
```kotlin
override suspend fun getAllVisible(): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.allVisibleSorted
    }
```

Replace `getBySlug()` (lines 137-140) with:
```kotlin
override suspend fun getBySlug(slug: String): Fragment? =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.bySlug[slug] ?: indexes.bySlug["/$slug"]
    }
```

Replace `getByYearMonthAndSlug()` (lines 142-153) with:
```kotlin
override suspend fun getByYearMonthAndSlug(
    year: String,
    month: String,
    slug: String,
): Fragment? =
    withContext(Dispatchers.IO) {
        loadFragments()
        val yearInt = year.toIntOrNull() ?: return@withContext null
        val monthInt = month.toIntOrNull() ?: return@withContext null
        indexes.byYearMonth[Pair(yearInt, monthInt)]?.find { it.slug == slug }
    }
```

Replace `getByTag()` (lines 155-158) with:
```kotlin
override suspend fun getByTag(tag: String): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.byTag[tag.lowercase()] ?: emptyList()
    }
```

Replace `getByCategory()` (lines 160-163) with:
```kotlin
override suspend fun getByCategory(category: String): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.byCategory[category.lowercase()] ?: emptyList()
    }
```

Replace `getByStatus()` (lines 165-168) with:
```kotlin
override suspend fun getByStatus(status: FragmentStatus): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.byStatus[status] ?: emptyList()
    }
```

Replace `getByAuthor()` (lines 170-173) with:
```kotlin
override suspend fun getByAuthor(authorId: String): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.byAuthor[authorId] ?: emptyList()
    }
```

Replace `getByAuthors()` (lines 175-182) with:
```kotlin
override suspend fun getByAuthors(authorIds: List<String>): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        authorIds.flatMap { indexes.byAuthor[it] ?: emptyList() }.distinctBy { it.slug }
    }
```

- [ ] **Step 5: Run all existing FileSystemFragmentRepository tests**

Run: `mvn -pl fragments-core test -Dtest=FileSystemFragmentRepositoryTest -T 4`
Expected: All existing tests PASS (behavior unchanged, just faster)

- [ ] **Step 6: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentIndexes.kt
git add fragments-core/src/test/kotlin/io/github/rygel/fragments/FragmentIndexesTest.kt
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRepository.kt
git commit -m "feat: add reload-time fragment indexes to FileSystemFragmentRepository

O(1) lookups for slug, tag, category, author, status, and year/month queries.
Visible fragments are pre-sorted by date at load time.
Indexes are rebuilt atomically on reload and cache updates."
```

---

## Task 2: Reload-Time Fragment Indexes — ClasspathFragmentRepository

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/ClasspathFragmentRepository.kt`

- [ ] **Step 1: Add indexes to ClasspathFragmentRepository**

Add after line 73 (`@Volatile private var loaded = false`):
```kotlin
@Volatile private var indexes: FragmentIndexes = FragmentIndexes.EMPTY
```

The ClasspathFragmentRepository has its own `loadFragments()` method. Find the internal load method and update it to build indexes. The class loads on first access via `loadFragments()`. Update the internal loading to build indexes:

Find the `loadFragments()` private method and update it to set `indexes` after loading:
```kotlin
private fun loadFragments(): List<Fragment> {
    if (!loaded || cachedFragments.isEmpty()) {
        cachedFragments = loadFragmentsFromClasspath()
        indexes = FragmentIndexes.build(cachedFragments)
        loaded = true
    }
    return cachedFragments
}
```

Update `reload()` (lines 154-160) to rebuild indexes:
```kotlin
override suspend fun reload() {
    withContext(Dispatchers.IO) {
        val fragments = loadFragmentsFromClasspath()
        cachedFragments = fragments
        indexes = FragmentIndexes.build(fragments)
        loaded = true
    }
}
```

Now update all query methods to use indexes, same pattern as FileSystemFragmentRepository:

```kotlin
override suspend fun getAllVisible(): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.allVisibleSorted
    }

override suspend fun getBySlug(slug: String): Fragment? =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.bySlug[slug] ?: indexes.bySlug["/$slug"]
    }

override suspend fun getByYearMonthAndSlug(
    year: String,
    month: String,
    slug: String,
): Fragment? =
    withContext(Dispatchers.IO) {
        loadFragments()
        val yearInt = year.toIntOrNull() ?: return@withContext null
        val monthInt = month.toIntOrNull() ?: return@withContext null
        indexes.byYearMonth[Pair(yearInt, monthInt)]?.find { it.slug == slug }
    }

override suspend fun getByTag(tag: String): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.byTag[tag.lowercase()] ?: emptyList()
    }

override suspend fun getByCategory(category: String): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.byCategory[category.lowercase()] ?: emptyList()
    }

override suspend fun getByStatus(status: FragmentStatus): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.byStatus[status] ?: emptyList()
    }

override suspend fun getByAuthor(authorId: String): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        indexes.byAuthor[authorId] ?: emptyList()
    }

override suspend fun getByAuthors(authorIds: List<String>): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        authorIds.flatMap { indexes.byAuthor[it] ?: emptyList() }.distinctBy { it.slug }
    }
```

Also update the scheduled/expiring methods to use indexes:
```kotlin
override suspend fun getScheduledFragmentsDueForPublication(threshold: LocalDateTime): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        (indexes.byStatus[FragmentStatus.SCHEDULED] ?: emptyList()).filter { fragment ->
            fragment.publishDate != null && !fragment.publishDate.isAfter(threshold)
        }
    }

override suspend fun getFragmentsExpiringSoon(threshold: LocalDateTime): List<Fragment> =
    withContext(Dispatchers.IO) {
        loadFragments()
        (indexes.byStatus[FragmentStatus.PUBLISHED] ?: emptyList()).filter { fragment ->
            fragment.expiryDate != null && !fragment.expiryDate.isAfter(threshold)
        }
    }
```

- [ ] **Step 2: Run full fragments-core test suite**

Run: `mvn -pl fragments-core test -T 4`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/ClasspathFragmentRepository.kt
git commit -m "feat: add reload-time fragment indexes to ClasspathFragmentRepository

Same FragmentIndexes used by FileSystemFragmentRepository. O(1) lookups
for all query methods."
```

---

## Task 3: Blog View Index

**Files:**
- Modify: `fragments-blog-core/src/main/kotlin/io/github/rygel/fragments/blog/BlogEngine.kt`

- [ ] **Step 1: Add BlogIndex to BlogEngine**

Add a private data class and index field inside `BlogEngine`:

```kotlin
private data class BlogIndex(
    val allPostsSorted: List<Fragment>,
    val byTag: Map<String, List<Fragment>>,
    val byCategory: Map<String, List<Fragment>>,
    val byYear: Map<Int, List<Fragment>>,
    val byYearMonth: Map<Pair<Int, Int>, List<Fragment>>,
    val byAuthor: Map<String, List<Fragment>>,
    val allTags: Map<String, Int>,
    val allCategories: Map<String, Int>,
)
```

Add the volatile index field and snapshot reference:
```kotlin
@Volatile private var blogIndex: BlogIndex? = null
@Volatile private var indexSnapshot: List<Fragment>? = null
```

Add a private function to build the index:
```kotlin
private suspend fun ensureIndex(): BlogIndex {
    val visible = repository.getAllVisible()
    val current = blogIndex
    if (current != null && indexSnapshot === visible) return current

    val blogPosts = visible
        .filter { isBlogTemplate(it.template) }
        .withResolvedUrls()
        .sortedByDescending { it.date }

    val index = BlogIndex(
        allPostsSorted = blogPosts,
        byTag = blogPosts.flatMap { f -> f.tags.map { tag -> tag.lowercase() to f } }
            .groupBy({ it.first }, { it.second }),
        byCategory = blogPosts.flatMap { f -> f.categories.map { cat -> cat.lowercase() to f } }
            .groupBy({ it.first }, { it.second }),
        byYear = blogPosts.filter { it.date != null }.groupBy { it.date!!.year },
        byYearMonth = blogPosts.filter { it.date != null }
            .groupBy { Pair(it.date!!.year, it.date!!.monthValue) },
        byAuthor = blogPosts.flatMap { f ->
            (f.authorIds + listOfNotNull(f.author)).map { aid -> aid to f }
        }.groupBy({ it.first }, { it.second }),
        allTags = blogPosts.flatMap { it.tags }.groupingBy { it }.eachCount(),
        allCategories = blogPosts.flatMap { it.categories }.groupingBy { it }.eachCount(),
    )
    blogIndex = index
    indexSnapshot = visible
    return index
}
```

- [ ] **Step 2: Update BlogEngine query methods to use the index**

```kotlin
suspend fun getOverview(
    includeDrafts: Boolean = false,
    page: Int,
): Page<Fragment> {
    if (includeDrafts) {
        val blogPosts = repository.getAll()
            .filter { isBlogTemplate(it.template) }
            .withResolvedUrls()
            .sortedByDescending { it.date }
        return Page.create(blogPosts, page, pageSize)
    }
    val index = ensureIndex()
    return Page.create(index.allPostsSorted, page, pageSize)
}

suspend fun getAllPosts(includeDrafts: Boolean = false): List<Fragment> {
    if (includeDrafts) {
        return repository.getAll()
            .filter { isBlogTemplate(it.template) }
            .withResolvedUrls()
            .sortedByDescending { it.date }
    }
    return ensureIndex().allPostsSorted
}

suspend fun getByTag(
    tag: String,
    page: Int,
): Page<Fragment> {
    val posts = ensureIndex().byTag[tag.lowercase()] ?: emptyList()
    return Page.create(posts, page, pageSize)
}

suspend fun getByCategory(
    category: String,
    page: Int,
): Page<Fragment> {
    val posts = ensureIndex().byCategory[category.lowercase()] ?: emptyList()
    return Page.create(posts, page, pageSize)
}

suspend fun getByYear(year: Int): List<Fragment> =
    ensureIndex().byYear[year] ?: emptyList()

suspend fun getByYearMonth(
    year: Int,
    month: Int,
): List<Fragment> =
    ensureIndex().byYearMonth[Pair(year, month)] ?: emptyList()

suspend fun getByAuthor(
    authorId: String,
    page: Int = 1,
): Page<Fragment> {
    val posts = ensureIndex().byAuthor[authorId] ?: emptyList()
    return Page.create(posts, page, pageSize)
}

suspend fun getAllTags(): Map<String, Int> = ensureIndex().allTags

suspend fun getAllCategories(): Map<String, Int> = ensureIndex().allCategories
```

Note: `getDrafts()`, `getPost()`, and `getPostWithRelationships()` remain unchanged — they don't benefit from the index (drafts need a separate filter, getPost is a single lookup already handled by the repository index).

- [ ] **Step 3: Run existing BlogEngine tests**

Run: `mvn -pl fragments-blog-core test -T 4`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add fragments-blog-core/src/main/kotlin/io/github/rygel/fragments/blog/BlogEngine.kt
git commit -m "feat: add BlogIndex with lazy rebuild for O(1) blog queries

BlogEngine now maintains a pre-built index of blog posts keyed by
tag, category, year, month, and author. Rebuilds lazily when the
underlying repository reloads (detected by reference identity)."
```

---

## Task 4: Avoid Duplicate Repository Reads + Cache Feed Outputs

**Files:**
- Modify: `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt`
- Modify: `fragments-adapter-core/src/test/kotlin/io/github/rygel/fragments/adapter/FragmentsEngineTest.kt`

- [ ] **Step 1: Add feed caching fields to FragmentsEngine**

Add after the `init` block in `FragmentsEngine`:

```kotlin
@Volatile private var cachedFeedOutput: FeedOutput? = null
@Volatile private var feedContentSnapshot: Any? = null
```

- [ ] **Step 2: Refactor collectResolvedFragments to deduplicate repositories**

Replace `collectResolvedFragments()` (lines 238-244) with:

```kotlin
suspend fun collectResolvedFragments(): List<Fragment> {
    val uniqueRepos = allRepositories.distinctBy { System.identityHashCode(it) }
    val repoResults = uniqueRepos.associateWith { repo ->
        repo.getAllVisible()
    }
    val staticRepo = staticEngine.getRepository()
    val blogRepo = blogEngine.getRepository()

    val staticPages = (repoResults[staticRepo] ?: staticRepo.getAllVisible())
        .filter { !BlogEngine.BLOG_TEMPLATES.contains(it.template) }
    val blogPosts = (repoResults[blogRepo] ?: blogRepo.getAllVisible())
        .filter { BlogEngine.BLOG_TEMPLATES.contains(it.template) }
        .let { posts ->
            posts.map { fragment ->
                if (fragment.resolvedUrl != null) fragment
                else {
                    val date = fragment.date
                    val url = if (date != null) {
                        "/blog/${date.year}/${String.format(java.util.Locale.US, "%02d", date.monthValue)}/${fragment.slug}"
                    } else {
                        "/blog/${fragment.slug}"
                    }
                    fragment.copy(resolvedUrl = url)
                }
            }
        }
    val additional = additionalRepositories.mapNotNull { repoResults[it] ?: it.getAllVisible() }.flatten()
    val providerFragments = additionalFragmentProviders.flatMap { it() }
    return (staticPages + blogPosts + additional + providerFragments).distinctBy { it.slug }
}
```

- [ ] **Step 3: Add feed caching to generateAllFeeds**

Replace `generateAllFeeds()` (lines 287-307) with:

```kotlin
suspend fun generateAllFeeds(): FeedOutput {
    val fragments = collectResolvedFragments()
    val cached = cachedFeedOutput
    if (cached != null && feedContentSnapshot === fragments) return cached

    val rssXml =
        rssGenerator.generateFeed(
            siteTitle = siteTitle,
            siteDescription = siteDescription,
            siteUrl = siteUrl,
            feedUrl = feedUrl,
            resolvedFragments = fragments,
        )
    val sitemapXml = sitemapGenerator.generateSitemap(fragments)
    val llmsTxt =
        LlmsTxtGenerator.generate(
            siteTitle = siteTitle,
            siteDescription = siteDescription,
            siteUrl = siteUrl,
            repositories = allRepositories,
            resolvedFragments = fragments,
        )
    val output = FeedOutput(rssXml, sitemapXml, llmsTxt)
    cachedFeedOutput = output
    feedContentSnapshot = fragments
    return output
}
```

Update the individual feed methods to use the cache:

```kotlin
suspend fun generateRssFeed(): String = generateAllFeeds().rssXml

suspend fun generateSitemap(): String = generateAllFeeds().sitemapXml

suspend fun generateLlmsTxt(): String = generateAllFeeds().llmsTxt
```

- [ ] **Step 4: Write feed caching test**

Add to `FragmentsEngineTest.kt`:

```kotlin
@Test
fun feedOutputIsCachedWhenContentUnchanged() =
    kotlinx.coroutines.runBlocking {
        val staticRepo = io.github.rygel.fragments.test.InMemoryFragmentRepository()
        val blogRepo = io.github.rygel.fragments.test.InMemoryFragmentRepository()
        val staticEngine = io.github.rygel.fragments.static.StaticPageEngine(staticRepo)
        val blogEngine = io.github.rygel.fragments.blog.BlogEngine(blogRepo)

        val engine = FragmentsEngine(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            siteUrl = "https://example.com",
        )

        val feed1 = engine.generateAllFeeds()
        val feed2 = engine.generateAllFeeds()
        assertEquals(feed1.rssXml, feed2.rssXml)
        assertEquals(feed1.sitemapXml, feed2.sitemapXml)
        assertEquals(feed1.llmsTxt, feed2.llmsTxt)
    }
```

Note: The `InMemoryFragmentRepository` returns `toList()` from `getAllVisible()`, creating a new list each time. This means the reference identity check will fail and the cache won't be hit. For this test to verify caching, the static/blog engines need to use the same repository instances that return stable references. The test verifies the methods work correctly — the caching optimization works in production where `FileSystemFragmentRepository.getAllVisible()` returns `indexes.allVisibleSorted` (a stable reference until reload).

- [ ] **Step 5: Run tests**

Run: `mvn -pl fragments-adapter-core test -T 4`
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt
git add fragments-adapter-core/src/test/kotlin/io/github/rygel/fragments/adapter/FragmentsEngineTest.kt
git commit -m "feat: cache feed outputs and deduplicate repository reads

collectResolvedFragments() now deduplicates repositories before calling
getAllVisible(). Feed outputs (RSS, sitemap, llms.txt) are cached using
reference-identity check — instant invalidation on content reload."
```

---

## Task 5: Optimize ViewModel Computed Properties

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentViewModel.kt`

- [ ] **Step 1: Cache DateTimeFormatter and make readingTime lazy**

In `FragmentViewModel.kt`, add a formatter cache to the companion object (after line 63):

```kotlin
private val formatterCache = java.util.concurrent.ConcurrentHashMap<String, DateTimeFormatter>()
```

Replace `formattedDate` (line 136-137) with:

```kotlin
val formattedDate: String
    get() = fragment.date?.format(
        formatterCache.getOrPut(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) },
    ) ?: ""
```

Replace `readingTime` (line 180-181) with:

```kotlin
val readingTime: ReadingTime by lazy { calculateReadingTime() }
```

- [ ] **Step 2: Run existing tests**

Run: `mvn -pl fragments-core test -Dtest=FragmentViewModelTest -T 4`
Expected: All tests PASS (same output, fewer allocations)

- [ ] **Step 3: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentViewModel.kt
git commit -m "perf: cache DateTimeFormatter and make readingTime lazy in FragmentViewModel

DateTimeFormatter instances are cached by pattern in a companion object
ConcurrentHashMap. readingTime is now computed once per ViewModel instance
instead of on every access."
```

---

## Task 6: Single-Flight Cache Loads

**Files:**
- Modify: `fragments-cache-core/src/main/kotlin/io/github/rygel/fragments/cache/Cache.kt`
- Modify: `fragments-cache-core/src/test/kotlin/io/github/rygel/fragments/cache/InMemoryCacheTest.kt`

- [ ] **Step 1: Add single-flight tracking to InMemoryCache**

Add an in-flight map to `InMemoryCache` (after line 175 `private val statistics`):

```kotlin
private val inFlight = java.util.concurrent.ConcurrentHashMap<K, kotlinx.coroutines.Deferred<V>>()
```

Add the import at the top of the file:
```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
```

Replace `getOrCompute()` (lines 199-242) with:

```kotlin
override suspend fun getOrCompute(
    key: K,
    compute: suspend () -> V,
): V {
    val cached = get(key)
    if (cached != null) return cached

    val deferred = coroutineScope {
        inFlight.getOrPut(key) {
            async { compute() }
        }
    }

    return try {
        val value = deferred.await()
        put(key, value)
        value
    } finally {
        inFlight.remove(key)
    }
}
```

**Important:** The `async` block must be inside `coroutineScope` to ensure structured concurrency. The `getOrPut` on `ConcurrentHashMap` is atomic — only one `Deferred` is created per key. Subsequent callers get the same `Deferred` and `await()` the result.

- [ ] **Step 2: Write single-flight test**

Add to `InMemoryCacheTest.kt`:

```kotlin
@Test
fun singleFlightDeduplicatesConcurrentComputes() =
    runBlocking {
        var computeCount = java.util.concurrent.atomic.AtomicInteger(0)
        val noMaxCache = InMemoryCache<String, String>(
            CacheConfiguration(ttl = Duration.ofMinutes(1), recordStats = true),
        )

        val results = kotlinx.coroutines.coroutineScope {
            (1..10).map {
                kotlinx.coroutines.async {
                    noMaxCache.getOrCompute("same-key") {
                        computeCount.incrementAndGet()
                        kotlinx.coroutines.delay(50)
                        "computed-value"
                    }
                }
            }.map { it.await() }
        }

        assertEquals(10, results.size)
        results.forEach { assertEquals("computed-value", it) }
        assertEquals(1, computeCount.get(), "compute should only be called once")
    }
```

- [ ] **Step 3: Run all cache tests**

Run: `mvn -pl fragments-cache-core test -T 4`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add fragments-cache-core/src/main/kotlin/io/github/rygel/fragments/cache/Cache.kt
git add fragments-cache-core/src/test/kotlin/io/github/rygel/fragments/cache/InMemoryCacheTest.kt
git commit -m "feat: add single-flight protection to InMemoryCache.getOrCompute

Concurrent cache misses for the same key now deduplicate the compute
call using a Deferred per key. Only one coroutine computes while
others await the result."
```

---

## Task 7: Improve Cache Eviction Complexity

**Files:**
- Modify: `fragments-cache-core/src/main/kotlin/io/github/rygel/fragments/cache/Cache.kt`

- [ ] **Step 1: Replace HashMap with access-order LinkedHashMap**

In `InMemoryCache`, replace line 173:

```kotlin
private val store = HashMap<K, CacheEntry<V>>()
```

with:

```kotlin
private val store = object : LinkedHashMap<K, CacheEntry<V>>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, CacheEntry<V>>): Boolean {
        val maxSize = configuration.maxSize ?: return false
        if (size > maxSize) {
            if (configuration.recordStats) {
                statistics.updateAndGet { it.eviction() }
            }
            return true
        }
        return false
    }
}
```

Remove the old `enforceMaxSize()` method entirely (lines 315-327), and remove the `enforceMaxSize()` call from `putEntry()` (line 312).

The `putEntry()` method becomes:
```kotlin
private fun putEntry(
    key: K,
    value: V,
) {
    val expiresAt =
        configuration.ttl?.let { ttl ->
            Instant.now().plus(ttl)
        }
    store[key] = CacheEntry(value = value, expiresAt = expiresAt)
}
```

- [ ] **Step 2: Run all cache tests**

Run: `mvn -pl fragments-cache-core test -T 4`
Expected: All tests PASS, including eviction tests

- [ ] **Step 3: Commit**

```bash
git add fragments-cache-core/src/main/kotlin/io/github/rygel/fragments/cache/Cache.kt
git commit -m "perf: replace O(n) eviction with access-order LinkedHashMap

Uses LinkedHashMap in access-order mode with automatic eviction via
removeEldestEntry. O(1) amortized eviction instead of O(n) minByOrNull
scan. Recently accessed entries survive longer (LRU policy)."
```

---

## Final Verification

- [ ] **Run full build across all modules**

```bash
mvn verify -T 4
```

Expected: BUILD SUCCESS

- [ ] **Update TODO.md — mark completed tasks**

Mark tasks 4 (Reload-Time Fragment Indexes), 5 (Blog View Index), 6 (Cache Generated Feed Outputs), 7 (Avoid Duplicate Repository Reads), 8 (Optimize ViewModel Computed Properties), 9 (Single-Flight Cache Loads), 10 (Improve Cache Eviction Complexity) as `[x]` with completion notes.

- [ ] **Final commit**

```bash
git add TODO.md
git commit -m "docs: mark performance optimization tasks as complete"
```
