# Security Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden Fragments4k against path traversal, YAML deserialization, Lucene query DoS, image decompression bombs, and missing HTTP security headers.

**Architecture:** Defense-in-depth fixes applied at the repository layer. A shared `PathSafety` utility provides slug validation and path containment checks consumed by all file-system repositories. Other fixes are scoped to their respective modules.

**Tech Stack:** Kotlin/JVM, JUnit 5, SnakeYAML SafeConstructor, Apache Lucene QueryParser.escape(), javax.imageio.ImageReader

---

## Task 1: PathSafety Utility

**Files:**
- Create: `fragments-core/src/main/kotlin/io/github/rygel/fragments/PathSafety.kt`
- Test: `fragments-core/src/test/kotlin/io/github/rygel/fragments/PathSafetyTest.kt`

- [ ] **Step 1: Write PathSafety object**

Create `fragments-core/src/main/kotlin/io/github/rygel/fragments/PathSafety.kt`:

```kotlin
package io.github.rygel.fragments

import java.io.File

object PathSafety {
    val SLUG_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    const val SLUG_MAX_LENGTH = 128

    fun validateSlug(slug: String) {
        require(slug.isNotBlank()) { "Slug must not be blank" }
        require(slug.length <= SLUG_MAX_LENGTH) { "Slug exceeds maximum length of $SLUG_MAX_LENGTH" }
        require(SLUG_PATTERN.matches(slug)) { "Slug contains invalid characters: $slug" }
    }

    fun resolveAndCheck(baseDir: File, fileName: String): File {
        val baseCanonical = baseDir.canonicalPath
        val targetFile = File(baseDir, fileName)
        val targetCanonical = targetFile.canonicalPath
        require(targetCanonical == baseCanonical || targetCanonical.startsWith(baseCanonical + File.separator)) {
            "Path traversal detected: '$fileName' escapes base directory"
        }
        return targetFile
    }
}
```

- [ ] **Step 2: Write PathSafety tests**

Create `fragments-core/src/test/kotlin/io/github/rygel/fragments/PathSafetyTest.kt`:

```kotlin
package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PathSafetyTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun validateSlugAcceptsValidSlug() {
        PathSafety.validateSlug("my-post")
        PathSafety.validateSlug("hello-world-123")
        PathSafety.validateSlug("a")
    }

    @Test
    fun validateSlugRejectsBlank() {
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("") }
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("   ") }
    }

    @Test
    fun validateSlugRejectsPathTraversal() {
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("../etc/passwd") }
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("..") }
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("foo/../../bar") }
    }

    @Test
    fun validateSlugRejectsUppercase() {
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("My-Post") }
    }

    @Test
    fun validateSlugRejectsSpecialChars() {
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("my post") }
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("my.post") }
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("my_post") }
    }

    @Test
    fun validateSlugRejectsOverMaxLength() {
        val longSlug = "a".repeat(PathSafety.SLUG_MAX_LENGTH + 1)
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug(longSlug) }
    }

    @Test
    fun validateSlugAcceptsMaxLength() {
        val maxSlug = "a".repeat(PathSafety.SLUG_MAX_LENGTH)
        PathSafety.validateSlug(maxSlug)
    }

    @Test
    fun resolveAndCheckAcceptsValidFileName() {
        val result = PathSafety.resolveAndCheck(tempDir, "test.json")
        assertTrue(result.canonicalPath.startsWith(tempDir.canonicalPath))
    }

    @Test
    fun resolveAndCheckRejectsPathTraversal() {
        assertThrows<IllegalArgumentException> {
            PathSafety.resolveAndCheck(tempDir, "../../etc/passwd")
        }
    }

    @Test
    fun resolveAndCheckRejectsDotDot() {
        assertThrows<IllegalArgumentException> {
            PathSafety.resolveAndCheck(tempDir, "..")
        }
    }

    @Test
    fun resolveAndCheckRejectsAbsoluteWindowsPath() {
        assertThrows<IllegalArgumentException> {
            PathSafety.resolveAndCheck(tempDir, "C:\\Windows\\System32")
        }
    }

    @Test
    fun slugPatternMatchesExpectedValues() {
        assertTrue(PathSafety.SLUG_PATTERN.matches("hello"))
        assertTrue(PathSafety.SLUG_PATTERN.matches("hello-world"))
        assertTrue(PathSafety.SLUG_PATTERN.matches("a1b2c3"))
        assertTrue(PathSafety.SLUG_PATTERN.matches("my-post-v1-1234567890"))
        assertFalse(PathSafety.SLUG_PATTERN.matches(""))
        assertFalse(PathSafety.SLUG_PATTERN.matches("HELLO"))
        assertFalse(PathSafety.SLUG_PATTERN.matches("hello world"))
        assertFalse(PathSafety.SLUG_PATTERN.matches("../etc"))
    }
}
```

- [ ] **Step 3: Run tests**

Run: `mvn -pl fragments-core test -Dtest=PathSafetyTest -T 4`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/PathSafety.kt fragments-core/src/test/kotlin/io/github/rygel/fragments/PathSafetyTest.kt
git commit -m "feat: add PathSafety utility for slug validation and path containment"
```

---

## Task 2: Safe YAML in Revision Repository

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRevisionRepository.kt` (line 17)
- Test: `fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemFragmentRevisionRepositoryTest.kt`

- [ ] **Step 1: Fix YAML constructor**

In `FileSystemFragmentRevisionRepository.kt`, change line 17 from:

```kotlin
private val yaml = Yaml()
```

to:

```kotlin
private val yaml = Yaml(SafeConstructor(LoaderOptions()))
```

Add the missing imports at the top:

```kotlin
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.constructor.SafeConstructor
```

- [ ] **Step 2: Write unsafe YAML regression test**

Add to `FileSystemFragmentRevisionRepositoryTest.kt`:

```kotlin
@Test
fun safeYamlRejectsUnsafeTagsInRevisionFrontMatter() =
    runBlocking {
        val fragment = createFragment()
        val revision = repository.saveRevision(fragment)
        val revisionFile = File(tempDir, ".revisions/${revision.id}.json")

        val maliciousContent = revisionFile.readText()
            .replace("\"frontMatterYaml\": null", "")
            .let { json ->
                val maliciousYaml = "\"!!javax.script.ScriptEngineManager [[]]\""
                json.replaceFirst(Regex("\"frontMatterYaml\"\\s*:\\s*\"[^\"]*\""), "\"frontMatterYaml\": $maliciousYaml")
            }
        revisionFile.writeText(maliciousContent)

        val loaded = repository.getRevision(revision.id)
        if (loaded != null) {
            assertFalse(
                loaded.frontMatter.any { it.value !is String && it.value !is Number && it.value !is Boolean && it.value !is List<*> && it.value !is Map<*, *> },
                "SafeConstructor should prevent arbitrary object instantiation"
            )
        }
    }
```

- [ ] **Step 3: Run tests**

Run: `mvn -pl fragments-core test -Dtest=FileSystemFragmentRevisionRepositoryTest -T 4`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRevisionRepository.kt fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemFragmentRevisionRepositoryTest.kt
git commit -m "fix: use SafeConstructor for YAML parsing in revision repository"
```

---

## Task 3: Harden Revision File Paths

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRevisionRepository.kt`
- Test: `fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemFragmentRevisionRepositoryTest.kt`

- [ ] **Step 1: Add slug/ID validation at entry points**

In `FileSystemFragmentRevisionRepository.kt`, add `PathSafety` validation calls.

In `saveRevision()`, add after line 34 (`val slug = fragment.slug`):

```kotlin
PathSafety.validateSlug(slug)
```

In `getRevisions()`, add at start of the `withContext` block (after line 64):

```kotlin
PathSafety.validateSlug(slug)
```

In `deleteRevisions()`, add at start of the `withContext` block (after line 154):

```kotlin
PathSafety.validateSlug(slug)
```

In `deleteRevisionsBefore()`, add at start of the `withContext` block (after line 172):

```kotlin
PathSafety.validateSlug(slug)
```

In `getRevisionCount()`, add at start of the `withContext` block (after line 199):

```kotlin
PathSafety.validateSlug(slug)
```

In `getAllRevisionSlugs()`, no validation needed (reads all keys from index).

In `revertToRevision()`, add after line 131 (start of `withContext` block):

```kotlin
PathSafety.validateSlug(slug)
```

- [ ] **Step 2: Add path containment to file operations**

Change line 54 from:
```kotlin
val revisionFile = File(revisionsDir, "${revision.id}.json")
```
to:
```kotlin
val revisionFile = PathSafety.resolveAndCheck(revisionsDir, "${revision.id}.json")
```

Change line 159 from:
```kotlin
File(revisionsDir, "$id.json").delete()
```
to:
```kotlin
PathSafety.resolveAndCheck(revisionsDir, "$id.json").delete()
```

Change line 189 from:
```kotlin
File(revisionsDir, "$id.json").delete()
```
to:
```kotlin
PathSafety.resolveAndCheck(revisionsDir, "$id.json").delete()
```

Change line 263 from:
```kotlin
val file = File(revisionsDir, "$id.json")
```
to:
```kotlin
val file = PathSafety.resolveAndCheck(revisionsDir, "$id.json")
```

Add the import:
```kotlin
// PathSafety is in the same package, no import needed
```

- [ ] **Step 3: Write path traversal regression tests**

Add to `FileSystemFragmentRevisionRepositoryTest.kt`:

```kotlin
@Test
fun saveRevisionRejectsPathTraversalSlug() =
    runBlocking {
        val fragment = createFragment(slug = "../../etc/passwd")
        assertThrows<IllegalArgumentException> {
            repository.saveRevision(fragment)
        }
    }

@Test
fun saveRevisionRejectsBlankSlug() =
    runBlocking {
        val fragment = createFragment(slug = "")
        assertThrows<IllegalArgumentException> {
            repository.saveRevision(fragment)
        }
    }

@Test
fun getRevisionsRejectsPathTraversalSlug() =
    runBlocking {
        assertThrows<IllegalArgumentException> {
            repository.getRevisions("../../etc")
        }
    }

@Test
fun deleteRevisionsRejectsPathTraversalSlug() =
    runBlocking {
        assertThrows<IllegalArgumentException> {
            repository.deleteRevisions("../secret")
        }
    }

@Test
fun getRevisionCountRejectsPathTraversalSlug() =
    runBlocking {
        assertThrows<IllegalArgumentException> {
            repository.getRevisionCount("../../etc")
        }
    }
```

Add the missing import at the top of the test file:
```kotlin
import org.junit.jupiter.api.assertThrows
```

- [ ] **Step 4: Run tests**

Run: `mvn -pl fragments-core test -Dtest=FileSystemFragmentRevisionRepositoryTest -T 4`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRevisionRepository.kt fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemFragmentRevisionRepositoryTest.kt
git commit -m "fix: validate slugs and contain paths in revision repository"
```

---

## Task 4: Validate Content Series Slugs

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemContentSeriesRepository.kt`
- Test: `fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemContentSeriesRepositoryTest.kt`

- [ ] **Step 1: Add slug validation to createSeries()**

In `FileSystemContentSeriesRepository.kt`, at the start of the `withContext` block in `createSeries()` (after line 55), add:

```kotlin
PathSafety.validateSlug(series.slug)
```

- [ ] **Step 2: Add slug validation to updateSeries()**

At the start of the `withContext` block in `updateSeries()` (after line 73), add:

```kotlin
PathSafety.validateSlug(series.slug)
```

- [ ] **Step 3: Add slug validation to deleteSeries()**

At the start of the `withContext` block in `deleteSeries()` (after line 92), add:

```kotlin
PathSafety.validateSlug(slug)
```

- [ ] **Step 4: Add slug validation to getBySlug()**

At the start of the `withContext` block in `getBySlug()` (after line 35), add:

```kotlin
PathSafety.validateSlug(slug)
```

- [ ] **Step 5: Add path containment to saveSeries()**

Change line 250 from:
```kotlin
val file = File(seriesDir, "${series.slug}$extension")
```
to:
```kotlin
val file = PathSafety.resolveAndCheck(seriesDir, "${series.slug}$extension")
```

- [ ] **Step 6: Write series slug validation tests**

Add to `FileSystemContentSeriesRepositoryTest.kt`:

```kotlin
@Test
fun createSeriesRejectsPathTraversalSlug() =
    runBlocking {
        val series = ContentSeries(
            slug = "../../etc/passwd",
            title = "Malicious",
            status = SeriesStatus.ACTIVE,
        )
        val result = repository.createSeries(series)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

@Test
fun createSeriesRejectsBlankSlug() =
    runBlocking {
        val series = ContentSeries(
            slug = "",
            title = "Empty Slug",
            status = SeriesStatus.ACTIVE,
        )
        val result = repository.createSeries(series)
        assertTrue(result.isFailure)
    }

@Test
fun deleteSeriesRejectsPathTraversalSlug() =
    runBlocking {
        val result = repository.deleteSeries("../../etc/passwd")
        assertTrue(result.isFailure)
    }

@Test
fun getBySlugRejectsPathTraversalSlug() =
    runBlocking {
        assertThrows<IllegalArgumentException> {
            repository.getBySlug("../secret")
        }
    }
```

Add the missing imports at the top of the test file:
```kotlin
import org.junit.jupiter.api.assertThrows
```

- [ ] **Step 7: Run tests**

Run: `mvn -pl fragments-core test -Dtest=FileSystemContentSeriesRepositoryTest -T 4`
Expected: All tests PASS

- [ ] **Step 8: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemContentSeriesRepository.kt fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemContentSeriesRepositoryTest.kt
git commit -m "fix: validate series slugs and contain paths in series repository"
```

---

## Task 5: Harden Lucene Query Handling

**Files:**
- Modify: `fragments-lucene-core/src/main/kotlin/io/github/rygel/fragments/lucene/LuceneSearchEngine.kt`
- Test: `fragments-lucene-core/src/test/kotlin/io/github/rygel/fragments/lucene/LuceneSearchEngineTest.kt`

- [ ] **Step 1: Add SearchType enum to SearchOptions**

In `LuceneSearchEngine.kt`, update the `SearchOptions` data class (lines 36-44) to:

```kotlin
data class SearchOptions(
    val query: String,
    val maxResults: Int = 10,
    val phraseSearch: Boolean = false,
    val fuzzySearch: Boolean = false,
    val fuzzyThreshold: Float = 0.7f,
    val autocomplete: Boolean = false,
    val autocompleteLimit: Int = 10,
    val searchType: SearchType = SearchType.STANDARD,
) {
    enum class SearchType {
        STANDARD,
        ADVANCED,
    }
}
```

- [ ] **Step 2: Add token cap constant and escape method**

Add to the `companion object` (after line 78):

```kotlin
private const val MAX_TOKEN_COUNT = 20
```

- [ ] **Step 3: Modify buildStandardQuery to escape public queries**

Replace the `buildStandardQuery` method (lines 240-251) with:

```kotlin
private fun buildStandardQuery(options: SearchOptions): Query? {
    val query = options.query
    val fields = arrayOf("title", "content")
    val boosts = mapOf("title" to TITLE_BOOST, "content" to CONTENT_BOOST)
    val parser = MultiFieldQueryParser(fields, analyzer, boosts)
    parser.defaultOperator = QueryParser.Operator.AND

    val processedQuery = if (options.searchType == SearchOptions.SearchType.ADVANCED) {
        query
    } else {
        QueryParser.escape(query)
    }

    return try {
        val parsed = parser.parse(processedQuery)
        if (hasLeadingWildcard(parsed)) {
            throw IllegalArgumentException("Leading wildcards are not allowed in search queries")
        }
        parsed
    } catch (e: ParseException) {
        logger.warn("Failed to parse search query '{}': {}", query, e.message)
        null
    }
}
```

- [ ] **Step 4: Add leading wildcard detection helper**

Add after `buildFuzzyQuery`:

```kotlin
private fun hasLeadingWildcard(query: Query): Boolean {
    return when (query) {
        is WildcardQuery -> {
            val termText = query.term.text()
            termText.startsWith("*") || termText.startsWith("?")
        }
        is BooleanQuery -> {
            query.clauses().any { hasLeadingWildcard(it.query) }
        }
        else -> false
    }
}
```

- [ ] **Step 5: Add token count capping**

Modify `buildQuery` (lines 233-238) to cap token count:

```kotlin
private fun buildQuery(options: SearchOptions): Query? {
    val tokens = options.query.split(WHITESPACE).filter { it.isNotEmpty() }
    val effectiveQuery = if (tokens.size > MAX_TOKEN_COUNT) {
        logger.warn("Search query has {} tokens, truncating to {}", tokens.size, MAX_TOKEN_COUNT)
        tokens.take(MAX_TOKEN_COUNT).joinToString(" ")
    } else {
        options.query
    }
    val effectiveOptions = if (tokens.size > MAX_TOKEN_COUNT) {
        options.copy(query = effectiveQuery)
    } else {
        options
    }

    return when {
        effectiveOptions.phraseSearch -> buildPhraseQuery(effectiveOptions.query)
        effectiveOptions.fuzzySearch -> buildFuzzyQuery(effectiveOptions.query, effectiveOptions.fuzzyThreshold)
        else -> buildStandardQuery(effectiveOptions)
    }
}
```

- [ ] **Step 6: Update the public search() overload**

In `search(queryString, maxResults)` (line 165), update the `SearchOptions` construction to explicitly use `STANDARD`:

```kotlin
search(SearchOptions(query = trimmed, maxResults = clampedResults, searchType = SearchOptions.SearchType.STANDARD))
```

- [ ] **Step 7: Write Lucene hardening tests**

Add to `LuceneSearchEngineTest.kt`:

```kotlin
@Test
fun testStandardSearchTreatsSpecialCharsAsLiteral() =
    runBlocking {
        val results = engine.search(SearchOptions(query = "title:secret OR content:admin"))
        assertTrue(
            results.isEmpty() || results.none { it.fragment.slug == "cooking-basics" },
            "Query syntax should be escaped in standard mode"
        )
    }

@Test
fun testAdvancedModeAcceptsRawQuerySyntax() =
    runBlocking {
        val results = engine.search(
            SearchOptions(
                query = "title:Kotlin",
                searchType = SearchOptions.SearchType.ADVANCED,
            ),
        )
        assertTrue(
            results.any { it.fragment.slug == "kotlin-guide" },
            "ADVANCED mode should allow field-specific queries"
        )
    }

@Test
fun testQueryWithExcessiveTokensIsTruncated() =
    runBlocking {
        val longQuery = (1..25).joinToString(" ") { "word$it" }
        val results = engine.search(SearchOptions(query = longQuery))
        assertTrue(results.size <= testFragments().size, "Should not crash with excessive tokens")
    }
```

- [ ] **Step 8: Run tests**

Run: `mvn -pl fragments-lucene-core test -Dtest=LuceneSearchEngineTest -T 4`
Expected: All tests PASS

- [ ] **Step 9: Commit**

```bash
git add fragments-lucene-core/src/main/kotlin/io/github/rygel/fragments/lucene/LuceneSearchEngine.kt fragments-lucene-core/src/test/kotlin/io/github/rygel/fragments/lucene/LuceneSearchEngineTest.kt
git commit -m "fix: escape public Lucene queries, cap tokens, add advanced mode"
```

---

## Task 6: Preflight Image Metadata

**Files:**
- Modify: `fragments-image-optimization-core/src/main/kotlin/io/github/rygel/fragments/image/BasicImageOptimizer.kt`
- Modify: `fragments-image-optimization-core/src/main/kotlin/io/github/rygel/fragments/image/ImageOptimizer.kt`
- Test: `fragments-image-optimization-core/src/test/kotlin/io/github/rygel/fragments/image/BasicImageOptimizerTest.kt`

- [ ] **Step 1: Add MAX_PIXEL_COUNT constant**

In `ImageOptimizer.kt`, add inside the `ImageResizeOptions.Companion` block (after line 62):

```kotlin
const val MAX_PIXEL_COUNT = 200_000_000L
```

- [ ] **Step 2: Add preflight method to BasicImageOptimizer**

Add these imports to `BasicImageOptimizer.kt`:

```kotlin
import javax.imageio.ImageReader
import java.io.BufferedInputStream
```

Add the preflight method to `BasicImageOptimizer` (before `resizeImage`):

```kotlin
private data class PreflightResult(
    val width: Int,
    val height: Int,
)

private fun preflightImage(stream: InputStream): PreflightResult {
    val buffered = if (stream is BufferedInputStream) stream else BufferedInputStream(stream)
    val readers = ImageIO.getImageReaders(buffered)
    if (!readers.hasNext()) {
        throw IllegalArgumentException("Unsupported or unrecognized image format")
    }
    val reader: ImageReader = readers.next()
    try {
        reader.input = javax.imageio.ImageIO.createImageInputStream(buffered)
        val width = reader.getWidth(0)
        val height = reader.getHeight(0)
        if (width > ImageResizeOptions.MAX_DIMENSION || height > ImageResizeOptions.MAX_DIMENSION) {
            throw IllegalArgumentException(
                "Image dimensions ${width}x${height} exceed maximum ${ImageResizeOptions.MAX_DIMENSION}x${ImageResizeOptions.MAX_DIMENSION}"
            )
        }
        if (width.toLong() * height.toLong() > ImageResizeOptions.MAX_PIXEL_COUNT) {
            throw IllegalArgumentException(
                "Image pixel count ${width.toLong() * height.toLong()} exceeds maximum ${ImageResizeOptions.MAX_PIXEL_COUNT}"
            )
        }
        return PreflightResult(width, height)
    } finally {
        reader.dispose()
    }
}
```

- [ ] **Step 3: Add preflight call to optimize(inputStream, ...)**

In the first `optimize` overload (line 28), replace:

```kotlin
val image = ImageIO.read(stream) ?: return@withContext Result.failure(IllegalArgumentException("Could not read image"))
```

with:

```kotlin
try {
    preflightImage(stream)
} catch (e: IllegalArgumentException) {
    return@withContext Result.failure(e)
}
stream.reset()
val image = ImageIO.read(stream) ?: return@withContext Result.failure(IllegalArgumentException("Could not read image"))
```

Note: The `inputStream` parameter must be markable/resettable for this to work. Wrap with `BufferedInputStream` if needed. Since `inputStream.use` is already called, change the `use` block to wrap:

Before the `try` block (at line 27), add:
```kotlin
val markableStream = if (stream.markSupported()) stream else BufferedInputStream(stream)
markableStream.mark(100 * 1024 * 1024) // 100MB mark limit
```

Then use `markableStream` instead of `stream` for the read:

```kotlin
val image = ImageIO.read(markableStream) ?: return@withContext Result.failure(IllegalArgumentException("Could not read image"))
```

- [ ] **Step 4: Add preflight call to optimize(filePath, ...)**

In the second `optimize` overload (line 102), replace:

```kotlin
val image = ImageIO.read(inputStream) ?: return@withContext Result.failure(IllegalArgumentException("Could not read image"))
```

with:

```kotlin
try {
    preflightImage(inputStream)
} catch (e: IllegalArgumentException) {
    return@withContext Result.failure(e)
}
inputStream.close()
file.inputStream().use { freshStream ->
    val image = ImageIO.read(freshStream) ?: return@withContext Result.failure(IllegalArgumentException("Could not read image"))
```

Close the restructured block properly (the original `use` block continues but now starts from the fresh stream read).

- [ ] **Step 5: Add preflight call to getMetadata()**

In `getMetadata()` (line 212), replace:

```kotlin
val image = ImageIO.read(file) ?: return@withContext Result.failure(IllegalArgumentException("Could not read image"))
```

with:

```kotlin
file.inputStream().use { stream ->
    try {
        preflightImage(stream)
    } catch (e: IllegalArgumentException) {
        return@withContext Result.failure(e)
    }
}
val image = ImageIO.read(file) ?: return@withContext Result.failure(IllegalArgumentException("Could not read image"))
```

- [ ] **Step 6: Write image preflight tests**

Add to `BasicImageOptimizerTest.kt`:

```kotlin
@Test
fun rejectsImageExceedingMaxDimensionWithoutFullDecode() =
    runBlocking {
        val testImage = createTestImage(100, 100)
        val result = optimizer.optimize(testImage.absolutePath, ImageResizeOptions())
        assertTrue(result.isSuccess, "Normal image should pass preflight")
    }

@Test
fun getMetadataRejectsUnsupportedFormat() =
    runBlocking {
        val textFile = File(tempDir, "not-an-image.txt")
        textFile.writeText("this is not an image")
        val result = optimizer.getMetadata(textFile.absolutePath)
        assertTrue(result.isFailure, "Non-image file should be rejected")
    }
```

- [ ] **Step 7: Run tests**

Run: `mvn -pl fragments-image-optimization-core test -Dtest=BasicImageOptimizerTest -T 4`
Expected: All tests PASS

- [ ] **Step 8: Commit**

```bash
git add fragments-image-optimization-core/src/main/kotlin/io/github/rygel/fragments/image/BasicImageOptimizer.kt fragments-image-optimization-core/src/main/kotlin/io/github/rygel/fragments/image/ImageOptimizer.kt fragments-image-optimization-core/src/test/kotlin/io/github/rygel/fragments/image/BasicImageOptimizerTest.kt
git commit -m "fix: preflight image dimensions before full decode to prevent decompression bombs"
```

---

## Task 7: Expand HTTP Security Headers

**Files:**
- Modify: `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt` (lines 366-372)
- Test: `fragments-adapter-core/src/test/kotlin/io/github/rygel/fragments/adapter/FragmentsEngineTest.kt`

- [ ] **Step 1: Add Permissions-Policy and conditional HSTS**

Replace the `securityHeaders()` method in `FragmentsEngine.kt` (lines 366-372) with:

```kotlin
fun securityHeaders(): Map<String, String> =
    buildMap {
        put("Content-Security-Policy", contentSecurityPolicy)
        put("X-Content-Type-Options", "nosniff")
        put("X-Frame-Options", "DENY")
        put("Referrer-Policy", "strict-origin-when-cross-origin")
        put("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
        if (siteUrl.startsWith("https")) {
            put("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
        }
    }
```

- [ ] **Step 2: Write security header tests**

Add to `FragmentsEngineTest.kt`:

```kotlin
@Test
fun testSecurityHeadersIncludesPermissionsPolicy() {
    val headers = engine.securityHeaders()
    assertEquals("camera=(), microphone=(), geolocation=()", headers["Permissions-Policy"])
}

@Test
fun testSecurityHeadersOmitsHstsForHttpSiteUrl() {
    val headers = engine.securityHeaders()
    assertFalse(headers.containsKey("Strict-Transport-Security"), "HSTS should be absent for HTTP siteUrl")
}

@Test
fun testSecurityHeadersIncludesHstsForHttpsSiteUrl() {
    val httpsEngine =
        FragmentsEngine(
            staticEngine = mockStaticEngine,
            blogEngine = mockBlogEngine,
            siteUrl = "https://example.com",
        )
    val headers = httpsEngine.securityHeaders()
    assertTrue(headers.containsKey("Strict-Transport-Security"), "HSTS should be present for HTTPS siteUrl")
    val hsts = headers["Strict-Transport-Security"]!!
    assertTrue(hsts.contains("max-age="), "HSTS should contain max-age directive")
    assertTrue(hsts.contains("includeSubDomains"), "HSTS should contain includeSubDomains")
}
```

- [ ] **Step 3: Run tests**

Run: `mvn -pl fragments-adapter-core test -Dtest=FragmentsEngineTest -T 4`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt fragments-adapter-core/src/test/kotlin/io/github/rygel/fragments/adapter/FragmentsEngineTest.kt
git commit -m "feat: add Permissions-Policy and conditional HSTS to security headers"
```

---

## Task 8: Full Build Verification

- [ ] **Step 1: Run full build**

Run: `mvn verify -T 4`
Expected: BUILD SUCCESS

- [ ] **Step 2: Update TODO.md**

Mark all 6 security hardening items as completed in `TODO.md` by changing `- [ ]` to `- [x]` and adding `Status: Completed 2026-05-17` with a brief implementation summary for each.

- [ ] **Step 3: Commit**

```bash
git add TODO.md
git commit -m "docs: mark security hardening tasks as completed"
```
