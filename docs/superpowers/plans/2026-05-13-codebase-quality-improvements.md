# Codebase Quality Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Address 10 codebase quality issues: duplicated utilities, missing tests, architecture violations, stale code, and documentation gaps.

**Architecture:** Each task is independent and can be executed in parallel. Tasks are ordered by priority. All changes follow TDD (write failing test → implement → verify pass). Test method names use camelCase only (no backticks per AGENTS.md).

**Tech Stack:** Kotlin 2.2.0, Maven, JUnit 5, Maven Surefire 3.5.5

---

### Task 1: Extract shared `escapeJson`/`escapeHtml`/`titleCase` into `fragments-core`

**Files:**
- Create: `fragments-core/src/main/kotlin/io/github/rygel/fragments/TextEscapeUtils.kt`
- Create: `fragments-core/src/test/kotlin/io/github/rygel/fragments/TextEscapeUtilsTest.kt`
- Modify: `fragments-seo-core/src/main/kotlin/io/github/rygel/fragments/SeoMetadata.kt` (remove private `escapeJson` + `escapeHtml`)
- Modify: `fragments-seo-core/src/main/kotlin/io/github/rygel/fragments/PersonSchemaGenerator.kt` (remove private `escapeJson`)
- Modify: `fragments-seo-core/src/main/kotlin/io/github/rygel/fragments/BreadcrumbGenerator.kt` (remove private `escapeJson` + `titleCase`)
- Modify: `fragments-seo-core/src/main/kotlin/io/github/rygel/fragments/FaqSchemaGenerator.kt` (remove private `escapeJson`)
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/SeoExtensions.kt` (remove private `titleCase`)
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRevisionRepository.kt` (remove private `escapeJson`)
- Modify: `fragments-http4k/src/main/kotlin/io/github/rygel/fragments/http4k/FragmentsHttp4kAdapter.kt` (remove private `escapeJson`, adjust caller)

- [ ] **Step 1: Write failing tests for the shared utility**

Create `fragments-core/src/test/kotlin/io/github/rygel/fragments/TextEscapeUtilsTest.kt`:

```kotlin
package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TextEscapeUtilsTest {
    @Test
    fun testEscapeJsonHandlesAllSpecialChars() {
        val input = "a\\b\"c\bd\ne\rf\tg"
        val escaped = TextEscapeUtils.escapeJson(input)
        assertEquals("a\\\\b\\\"c\\bd\\ne\\rf\\tg", escaped)
    }

    @Test
    fun testEscapeJsonPlainString() {
        assertEquals("hello world", TextEscapeUtils.escapeJson("hello world"))
    }

    @Test
    fun testEscapeHtmlHandlesAllSpecialChars() {
        val input = "<script>alert('xss')&\"more\"</script>"
        val escaped = TextEscapeUtils.escapeHtml(input)
        assertEquals("&lt;script&gt;alert(&#x27;xss&#x27;)&amp;&quot;more&quot;&lt;/script&gt;", escaped)
    }

    @Test
    fun testEscapeHtmlPlainString() {
        assertEquals("hello", TextEscapeUtils.escapeHtml("hello"))
    }

    @Test
    fun testTitleCaseHyphenatedSlug() {
        assertEquals("Hello World", TextEscapeUtils.titleCase("hello-world"))
    }

    @Test
    fun testTitleCaseSingleWord() {
        assertEquals("Blog", TextEscapeUtils.titleCase("blog"))
    }

    @Test
    fun testTitleCaseAlreadyCapitalized() {
        assertEquals("Hello World", TextEscapeUtils.titleCase("Hello-World"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl fragments-core test -Dtest=TextEscapeUtilsTest -q`
Expected: FAIL — `Unresolved reference 'TextEscapeUtils'`

- [ ] **Step 3: Create the shared utility**

Create `fragments-core/src/main/kotlin/io/github/rygel/fragments/TextEscapeUtils.kt`:

```kotlin
package io.github.rygel.fragments

object TextEscapeUtils {
    fun escapeJson(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    fun escapeHtml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")

    fun titleCase(slug: String): String =
        slug.replace("-", " ").split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercaseChar() }
        }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl fragments-core test -Dtest=TextEscapeUtilsTest -q`
Expected: PASS

- [ ] **Step 5: Replace all private copies with `TextEscapeUtils` calls**

In each file below, delete the private `escapeJson`, `escapeHtml`, or `titleCase` function and replace all call sites:

| File | Remove function | Replace calls with |
|------|-----------------|-------------------|
| `SeoMetadata.kt` | `private fun escapeHtml` + `private fun escapeJson` | `TextEscapeUtils.escapeHtml` / `TextEscapeUtils.escapeJson` |
| `PersonSchemaGenerator.kt` | `private fun escapeJson` | `TextEscapeUtils.escapeJson` |
| `BreadcrumbGenerator.kt` | `private fun titleCase` + `private fun escapeJson` | `TextEscapeUtils.titleCase` / `TextEscapeUtils.escapeJson` |
| `FaqSchemaGenerator.kt` | `private fun escapeJson` | `TextEscapeUtils.escapeJson` |
| `SeoExtensions.kt` | `private fun titleCase` | `TextEscapeUtils.titleCase` |
| `FileSystemFragmentRevisionRepository.kt` | `private fun escapeJson` | `TextEscapeUtils.escapeJson` |
| `FragmentsHttp4kAdapter.kt` | `private fun escapeJson` (the one that wraps in quotes) | `TextEscapeUtils.escapeJson` and update the one caller to wrap in quotes: `"\"${TextEscapeUtils.escapeJson(value)}\""` |

- [ ] **Step 6: Run full test suite**

Run: `mvn test -T 4 -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: extract duplicated escapeJson/escapeHtml/titleCase into TextEscapeUtils"
```

---

### Task 2: Add tests for `fragments-static-core`

**Files:**
- Create: `fragments-static-core/src/test/kotlin/io/github/rygel/fragments/static/StaticPageEngineTest.kt`

- [ ] **Step 1: Write failing tests**

Create `fragments-static-core/src/test/kotlin/io/github/rygel/fragments/static/StaticPageEngineTest.kt`:

```kotlin
package io.github.rygel.fragments.static

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentStatus
import io.github.rygel.fragments.FragmentTemplates
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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

    @Test
    fun testGetPageReturnsMatchingFragment() {
        val page = makeStaticPage("about", "About Us")
        repository.add(page)

        val result = engine.getPage("about")
        assertNotNull(result)
        assertEquals("About Us", result!!.title)
    }

    @Test
    fun testGetPageReturnsNullForMissingSlug() {
        assertNull(engine.getPage("nonexistent"))
    }

    @Test
    fun testGetPageResolvesUrlWhenNull() {
        val page = makeStaticPage("about", "About", resolvedUrl = null)
        repository.add(page)

        val result = engine.getPage("about")
        assertNotNull(result)
        assertEquals("/page/about", result!!.resolvedUrl)
    }

    @Test
    fun testGetPagePreservesExistingResolvedUrl() {
        val page = makeStaticPage("about", "About", resolvedUrl = "/custom-about")
        repository.add(page)

        val result = engine.getPage("about")
        assertNotNull(result)
        assertEquals("/custom-about", result!!.resolvedUrl)
    }

    @Test
    fun testGetAllStaticPagesFiltersByTemplate() {
        repository.add(makeStaticPage("about", "About", template = FragmentTemplates.STATIC))
        repository.add(makeStaticPage("default-page", "Default", template = FragmentTemplates.DEFAULT))
        repository.add(makeBlogPost("blog-post", "Blog"))

        val pages = engine.getAllStaticPages()
        assertEquals(2, pages.size)
        assertTrue(pages.all { it.template != "blog" })
    }

    @Test
    fun testGetAllStaticPagesExcludesDraftsByDefault() {
        repository.add(makeStaticPage("published", "Published"))
        repository.add(makeStaticPage("draft", "Draft", status = FragmentStatus.DRAFT))

        val pages = engine.getAllStaticPages()
        assertEquals(1, pages.size)
        assertEquals("Published", pages[0].title)
    }

    @Test
    fun testGetAllStaticPagesIncludesDraftsWhenRequested() {
        repository.add(makeStaticPage("published", "Published"))
        repository.add(makeStaticPage("draft", "Draft", status = FragmentStatus.DRAFT))

        val pages = engine.getAllStaticPages(includeDrafts = true)
        assertEquals(2, pages.size)
    }

    @Test
    fun testCustomPageUrlPrefix() {
        val customEngine = StaticPageEngine(repository, pageUrlPrefix = "/custom")
        repository.add(makeStaticPage("about", "About", resolvedUrl = null))

        val result = customEngine.getPage("about")
        assertNotNull(result)
        assertEquals("/custom/about", result!!.resolvedUrl)
    }

    private fun makeStaticPage(
        slug: String,
        title: String,
        template: String = FragmentTemplates.STATIC,
        status: FragmentStatus = FragmentStatus.PUBLISHED,
        resolvedUrl: String? = "/page/$slug",
    ) = Fragment(
        title = title,
        slug = slug,
        status = status,
        date = LocalDateTime.of(2024, 1, 1, 0, 0),
        publishDate = null,
        preview = "Preview of $title",
        htmlContent = "<p>$title content</p>",
        frontMatter = emptyMap(),
        template = template,
        resolvedUrl = resolvedUrl,
    )

    private fun makeBlogPost(slug: String, title: String) = Fragment(
        title = title,
        slug = slug,
        status = FragmentStatus.PUBLISHED,
        date = LocalDateTime.of(2024, 3, 1, 0, 0),
        publishDate = null,
        preview = "Preview",
        htmlContent = "<p>Blog</p>",
        frontMatter = emptyMap(),
        template = "blog",
    )
}
```

Also create a minimal `InMemoryFragmentRepository` test helper. Check if one already exists in `fragments-core` test-jar or `fragments-test-data-factories`. If so, add that dependency to `fragments-static-core/pom.xml` test scope. If not, create a minimal one:

Create `fragments-static-core/src/test/kotlin/io/github/rygel/fragments/static/InMemoryFragmentRepository.kt`:

```kotlin
package io.github.rygel.fragments.static

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentStatus

class InMemoryFragmentRepository : FragmentRepository {
    private val fragments = mutableListOf<Fragment>()

    fun add(fragment: Fragment) {
        fragments.add(fragment)
    }

    override suspend fun getAll(): List<Fragment> = fragments.toList()

    override suspend fun getAllVisible(): List<Fragment> =
        fragments.filter { it.visible && it.status == FragmentStatus.PUBLISHED }

    override suspend fun getBySlug(slug: String): Fragment? =
        fragments.find { it.slug == slug }

    override suspend fun getByTag(tag: String): List<Fragment> =
        fragments.filter { tag in it.tags }

    override suspend fun getByCategory(category: String): List<Fragment> =
        fragments.filter { category in it.categories }

    override suspend fun getByStatus(status: FragmentStatus): List<Fragment> =
        fragments.filter { it.status == status }

    override suspend fun getByAuthor(authorId: String): List<Fragment> =
        fragments.filter { authorId in it.authorIds }

    override suspend fun getByName(name: String): Fragment? =
        fragments.find { it.title.equals(name, ignoreCase = true) }

    override suspend fun getBySlugOrId(identifier: String): Fragment? = getBySlug(identifier)

    override suspend fun getByYearMonthAndSlug(year: Int, month: Int, slug: String): Fragment? =
        fragments.find { it.slug == slug }

    override suspend fun getScheduledFragmentsDueForPublication(before: LocalDateTime): List<Fragment> =
        emptyList()

    override suspend fun reload() {}

    override suspend fun updateFragmentStatus(slug: String, status: FragmentStatus): Boolean = false

    override suspend fun createRevision(slug: String): Boolean = false

    override suspend fun getFragmentRevisions(slug: String): List<io.github.rygel.fragments.FragmentRevision> =
        emptyList()

    override suspend fun revertToRevision(slug: String, revisionId: String): Boolean = false
}
```

Note: Import `java.time.LocalDateTime` for `getScheduledFragmentsDueForPublication`. Also check if `FragmentRepository` has other methods — add stubs as needed.

- [ ] **Step 2: Run tests to verify they pass**

Run: `mvn -pl fragments-static-core test -q`
Expected: PASS (tests should pass since `StaticPageEngine` already works)

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "test: add StaticPageEngine tests covering page lookup, URL resolution, draft filtering"
```

---

### Task 3: Add tests for `fragments-cli` (ProjectGenerator)

**Files:**
- Create: `fragments-cli/src/test/kotlin/io/github/rygel/fragments/cli/ProjectGeneratorTest.kt`

- [ ] **Step 1: Write tests for project scaffolding**

Create `fragments-cli/src/test/kotlin/io/github/rygel/fragments/cli/ProjectGeneratorTest.kt`:

```kotlin
package io.github.rygel.fragments.cli

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectGeneratorTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    fun testGeneratesHttp4kProjectStructure() {
        ProjectGenerator.generateProject(
            projectName = "test-app",
            framework = "http4k",
            basePackage = "com.example",
            outputDir = projectDir.absolutePath,
        )

        assertTrue(File(projectDir, "pom.xml").exists())
        assertTrue(File(projectDir, "README.md").exists())
        assertTrue(File(projectDir, "src/main/kotlin").isDirectory)
        assertTrue(File(projectDir, "content/pages").isDirectory)
        assertTrue(File(projectDir, "content/posts").isDirectory)
        assertTrue(File(projectDir, ".gitignore").exists())
    }

    @Test
    fun testGeneratesSpringBootProjectStructure() {
        ProjectGenerator.generateProject(
            projectName = "test-app",
            framework = "spring-boot",
            basePackage = "com.example",
            outputDir = projectDir.absolutePath,
        )

        assertTrue(File(projectDir, "pom.xml").exists())
        assertTrue(File(projectDir, "src/main/kotlin").isDirectory)
    }

    @Test
    fun testGeneratesJavalinProjectStructure() {
        ProjectGenerator.generateProject(
            projectName = "test-app",
            framework = "javalin",
            basePackage = "com.example",
            outputDir = projectDir.absolutePath,
        )

        assertTrue(File(projectDir, "pom.xml").exists())
    }

    @Test
    fun testGeneratesQuarkusProjectStructure() {
        ProjectGenerator.generateProject(
            projectName = "test-app",
            framework = "quarkus",
            basePackage = "com.example",
            outputDir = projectDir.absolutePath,
        )

        assertTrue(File(projectDir, "pom.xml").exists())
    }

    @Test
    fun testGeneratesMicronautProjectStructure() {
        ProjectGenerator.generateProject(
            projectName = "test-app",
            framework = "micronaut",
            basePackage = "com.example",
            outputDir = projectDir.absolutePath,
        )

        assertTrue(File(projectDir, "pom.xml").exists())
    }

    @Test
    fun testPomXmlContainsProjectName() {
        ProjectGenerator.generateProject(
            projectName = "my-awesome-site",
            framework = "http4k",
            basePackage = "com.example",
            outputDir = projectDir.absolutePath,
        )

        val pomContent = File(projectDir, "pom.xml").readText()
        assertTrue(pomContent.contains("my-awesome-site"))
    }

    @Test
    fun testPomXmlContainsBasePackage() {
        ProjectGenerator.generateProject(
            projectName = "test-app",
            framework = "http4k",
            basePackage = "com.mycompany",
            outputDir = projectDir.absolutePath,
        )

        val pomContent = File(projectDir, "pom.xml").readText()
        assertTrue(pomContent.contains("com.mycompany"))
    }

    @Test
    fun testGeneratesSampleContent() {
        ProjectGenerator.generateProject(
            projectName = "test-app",
            framework = "http4k",
            basePackage = "com.example",
            outputDir = projectDir.absolutePath,
        )

        assertTrue(File(projectDir, "content/posts").listFiles()?.isNotEmpty() == true || File(projectDir, "content/pages").listFiles()?.isNotEmpty() == true)
    }
}
```

Note: Check `ProjectGenerator.kt` for the exact method signature of `generateProject`. It may be named differently (e.g., `generate`). Adjust accordingly.

- [ ] **Step 2: Run tests**

Run: `mvn -pl fragments-cli test -q`
Expected: PASS (or FAIL if method signature differs — adjust and re-run)

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "test: add ProjectGenerator scaffolding tests for all 5 frameworks"
```

---

### Task 4: Add tests for `NavigationMenuGenerator`

**Files:**
- Create: `fragments-navigation-core/src/test/kotlin/io/github/rygel/fragments/test/NavigationMenuGeneratorTest.kt`

- [ ] **Step 1: Write failing tests**

Create `fragments-navigation-core/src/test/kotlin/io/github/rygel/fragments/test/NavigationMenuGeneratorTest.kt`:

```kotlin
package io.github.rygel.fragments.test

import io.github.rygel.fragments.NavigationMenuGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NavigationMenuGeneratorTest {
    @Test
    fun testGenerateMainMenuDefaultLinks() {
        val links = NavigationMenuGenerator.generateMainMenu()

        assertEquals(2, links.size)
        assertEquals("Home", links[0].label)
        assertEquals("/", links[0].url)
        assertEquals("Blog", links[1].label)
        assertEquals("/blog", links[1].url)
    }

    @Test
    fun testGenerateMainMenuWithArchiveUrl() {
        val links = NavigationMenuGenerator.generateMainMenu(archiveUrl = "/blog/archive")

        assertEquals(3, links.size)
        assertEquals("Archive", links[2].label)
        assertEquals("/blog/archive", links[2].url)
    }

    @Test
    fun testGenerateMainMenuWithSearchUrl() {
        val links = NavigationMenuGenerator.generateMainMenu(searchUrl = "/search")

        assertTrue(links.any { it.label == "Search" && it.url == "/search" })
    }

    @Test
    fun testGenerateMainMenuWithAllOptionalLinks() {
        val links = NavigationMenuGenerator.generateMainMenu(
            siteUrl = "/home",
            blogUrl = "/news",
            archiveUrl = "/news/archive",
            searchUrl = "/search",
        )

        assertEquals(4, links.size)
        assertEquals("/home", links[0].url)
        assertEquals("/news", links[1].url)
    }

    @Test
    fun testGenerateBlogMenuDefaultLinks() {
        val links = NavigationMenuGenerator.generateBlogMenu()

        assertEquals(2, links.size)
        assertEquals("Blog Home", links[0].label)
        assertEquals("/blog", links[0].url)
        assertEquals("Archive", links[1].label)
        assertEquals("/blog/archive", links[1].url)
    }

    @Test
    fun testGenerateBlogMenuWithoutArchive() {
        val links = NavigationMenuGenerator.generateBlogMenu(archiveUrl = null)

        assertEquals(1, links.size)
        assertEquals("Blog Home", links[0].label)
    }

    @Test
    fun testGenerateBlogMenuCustomBaseUrl() {
        val links = NavigationMenuGenerator.generateBlogMenu(baseUrl = "/news")

        assertEquals("/news", links[0].url)
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn -pl fragments-navigation-core test -q`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "test: add NavigationMenuGenerator tests for main menu and blog menu"
```

---

### Task 5: Fix inverted dependency — move SeoExtensions to `fragments-seo-core`

The `fragments-core` → `fragments-seo-core` dependency we just created is architecturally wrong (core depending on a feature module). Fix by keeping the extension functions in `fragments-seo-core` and accepting `Fragment`/`Author` parameters directly instead of extension receivers.

**Files:**
- Delete: `fragments-core/src/main/kotlin/io/github/rygel/fragments/SeoExtensions.kt`
- Modify: `fragments-core/pom.xml` (remove `fragments-seo-core` dependency)
- Modify: `fragments-seo-core/pom.xml` (add back `fragments-core` dependency as `compileOnly`)
- Create: `fragments-seo-core/src/main/kotlin/io/github/rygel/fragments/SeoFragmentExtensions.kt`
- Modify: `pom.xml` (revert module ordering — `fragments-core` before `fragments-seo-core` again)

- [ ] **Step 1: Create extension functions in seo-core that accept Fragment/Author as parameters**

The trick: `fragments-seo-core` declares `fragments-core` as `compileOnly` (not `implementation`). The extension functions exist in seo-core but only work at compile time when the consumer also has `fragments-core`. This avoids the transitive dep issue while keeping the correct dependency direction.

Modify `fragments-seo-core/pom.xml` to add `fragments-core` as `compileOnly`:

```xml
<dependency>
    <groupId>io.github.rygel</groupId>
    <artifactId>fragments-core</artifactId>
    <version>${project.version}</version>
    <scope>compileOnly</scope>
</dependency>
```

Create `fragments-seo-core/src/main/kotlin/io/github/rygel/fragments/SeoFragmentExtensions.kt` — move the content from `fragments-core/SeoExtensions.kt` into this file verbatim.

- [ ] **Step 2: Remove SeoExtensions.kt from fragments-core and remove the dependency**

Delete `fragments-core/src/main/kotlin/io/github/rygel/fragments/SeoExtensions.kt`.

Remove from `fragments-core/pom.xml`:

```xml
<dependency>
    <groupId>io.github.rygel</groupId>
    <artifactId>fragments-seo-core</artifactId>
    <version>${project.version}</version>
</dependency>
```

Revert the module ordering in parent `pom.xml` — move `fragments-core` back before `fragments-seo-core`:

```xml
<module>fragments-core</module>
...
<module>fragments-seo-core</module>
```

- [ ] **Step 3: Run full build**

Run: `mvn test -T 4 -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: move SeoExtensions to seo-core with compileOnly dep on core, fix inverted dependency"
```

---

### Task 6: Remove duplicate TODO entry for "Search Result Caching"

**Files:**
- Modify: `TODO.md` (lines ~400-406)

- [ ] **Step 1: Delete the duplicate entry**

In `TODO.md`, find the second "Search Result Caching" entry (the one marked `[ ]` with no implementation details, around lines 400-406) and delete it. Keep the first one (marked `[x]` with full implementation details).

- [ ] **Step 2: Commit**

```bash
git add TODO.md
git commit -m "docs: remove duplicate Search Result Caching TODO entry"
```

---

### Task 7: Add KDoc to social and navigation module public APIs

**Files:**
- Modify: `fragments-social-core/src/main/kotlin/io/github/rygel/fragments/SocialShareLink.kt`
- Modify: `fragments-social-core/src/main/kotlin/io/github/rygel/fragments/SocialShareGenerator.kt`
- Modify: `fragments-social-core/src/main/kotlin/io/github/rygel/fragments/SocialPlatform.kt`
- Modify: `fragments-navigation-core/src/main/kotlin/io/github/rygel/fragments/NavigationMenuGenerator.kt`
- Modify: `fragments-navigation-core/src/main/kotlin/io/github/rygel/fragments/NavigationLink.kt`

- [ ] **Step 1: Add KDoc to all public declarations**

`SocialPlatform.kt`:
```kotlin
/**
 * Social media platforms supported for share link generation.
 *
 * Each platform defines a display name and a URL template with `{title}` and `{url}` placeholders.
 */
enum class SocialPlatform(
    /** Human-readable platform name. */
    val displayName: String,
    /** URL template with `{title}` and `{url}` placeholders for share link generation. */
    val shareUrlTemplate: String,
) {
```

`SocialShareLink.kt`:
```kotlin
/**
 * A generated social media share link for a specific platform.
 */
data class SocialShareLink(
    /** The social platform this link targets. */
    val platform: SocialPlatform,
    /** The fully resolved share URL with encoded title and page URL. */
    val url: String,
    /** Display title for the share link. */
    val title: String,
)
```

`SocialShareGenerator.kt`:
```kotlin
/**
 * Generates social media share links for a given page URL and title.
 *
 * Produces platform-specific share URLs with URL-encoded title and page parameters.
 */
object SocialShareGenerator {
    /**
     * Generates share links for the specified platforms.
     *
     * @param title The page title to include in the share text.
     * @param url The canonical page URL to share.
     * @param platforms The platforms to generate links for. Defaults to all supported platforms.
     * @return A list of [SocialShareLink] instances, one per platform.
     */
```

`NavigationLink.kt`:
```kotlin
/**
 * A single navigation link with label, URL, and optional styling.
 */
data class NavigationLink(
    /** Display text for the link. */
    val label: String,
    /** Target URL for the link. */
    val url: String,
    /** Optional CSS class name for styling. */
    val cssClass: String = "",
    /** Whether this link represents the current page. */
    val isActive: Boolean = false,
)
```

`NavigationMenuGenerator.kt`:
```kotlin
/**
 * Generates standard navigation menus for site headers, sidebars, and blog sections.
 */
object NavigationMenuGenerator {
    /**
     * Generates a main site navigation menu.
     *
     * Always includes Home and Blog links. Optionally includes Archive and Search links.
     *
     * @param siteUrl URL for the Home link. Defaults to `"/"`.
     * @param blogUrl URL for the Blog link. Defaults to `"/blog"`.
     * @param archiveUrl Optional URL for an Archive link.
     * @param searchUrl Optional URL for a Search link.
     * @return Ordered list of [NavigationLink] instances.
     */
    fun generateMainMenu(
        siteUrl: String = "/",
        blogUrl: String = "/blog",
        archiveUrl: String? = null,
        searchUrl: String? = null,
    ): List<NavigationLink> {

    /**
     * Generates a blog-specific navigation menu.
     *
     * Always includes a Blog Home link. Optionally includes an Archive link.
     *
     * @param baseUrl URL for the Blog Home link. Defaults to `"/blog"`.
     * @param archiveUrl Optional URL for the Archive link. Defaults to `"/blog/archive"`.
     * @param currentYear Unused — reserved for future active-state highlighting.
     * @param currentMonth Unused — reserved for future active-state highlighting.
     * @return Ordered list of [NavigationLink] instances.
     */
    fun generateBlogMenu(
        baseUrl: String = "/blog",
        archiveUrl: String? = "/blog/archive",
        currentYear: Int? = null,
        currentMonth: Int? = null,
    ): List<NavigationLink> {
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -T 4 -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "docs: add KDoc to social and navigation module public APIs"
```

---

### Task 8: Replace hardcoded years in `ArchiveNavigationGenerator`

**Files:**
- Modify: `fragments-navigation-core/src/main/kotlin/io/github/rygel/fragments/ArchiveNavigationGenerator.kt:19-24`
- Modify: `fragments-navigation-core/src/test/kotlin/io/github/rygel/fragments/test/ArchiveNavigationTest.kt` (update `testGenerateYearLinksWithDefaultYears`)

- [ ] **Step 1: Update the test first**

In `ArchiveNavigationTest.kt`, replace `testGenerateYearLinksWithDefaultYears`:

```kotlin
@Test
fun testGenerateYearLinksWithDefaultYears() {
    val currentYear = java.time.LocalDate.now().year
    val yearLinks = ArchiveNavigationGenerator.generateYearLinks()

    assertEquals(5, yearLinks.size)
    assertEquals(currentYear.toString(), yearLinks[0].label)
    assertEquals((currentYear - 1).toString(), yearLinks[1].label)
    assertEquals((currentYear - 2).toString(), yearLinks[2].label)
    assertEquals((currentYear - 3).toString(), yearLinks[3].label)
    assertEquals((currentYear - 4).toString(), yearLinks[4].label)

    assertEquals("/blog/archive/$currentYear", yearLinks[0].url)
    assertEquals("/blog/archive/${currentYear - 1}", yearLinks[1].url)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl fragments-navigation-core test -Dtest=ArchiveNavigationTest#testGenerateYearLinksWithDefaultYears -q`
Expected: FAIL — asserts 2024 but code returns 2026

- [ ] **Step 3: Fix the implementation**

In `ArchiveNavigationGenerator.kt`, replace line 21:

```kotlin
(2024 downTo 2020).toList()
```

with:

```kotlin
(java.time.LocalDate.now().year downTo java.time.LocalDate.now().year - 4).toList()
```

- [ ] **Step 4: Run tests**

Run: `mvn -pl fragments-navigation-core test -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "fix: replace hardcoded 2020-2024 fallback years with dynamic range in ArchiveNavigationGenerator"
```

---

### Task 9: Remove unused dependencies from `fragments-navigation-core`

**Files:**
- Modify: `fragments-navigation-core/pom.xml`

- [ ] **Step 1: Remove `kotlinx-coroutines-core` and `mockk-jvm` dependencies**

In `fragments-navigation-core/pom.xml`, delete these two blocks:

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-core</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>io.mockk</groupId>
     <artifactId>mockk-jvm</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Verify build**

Run: `mvn -pl fragments-navigation-core test -q`
Expected: BUILD SUCCESS (tests don't use coroutines or mockk)

- [ ] **Step 3: Commit**

```bash
git add fragments-navigation-core/pom.xml
git commit -m "chore: remove unused kotlinx-coroutines and mockk deps from navigation module"
```

---

### Task 10: Rename backtick test methods in architecture tests and other files

**Files:**
- Modify: `fragments-architecture-tests/src/test/kotlin/io/github/rygel/fragments/architecture/CodingRulesTest.kt`
- Modify: `fragments-architecture-tests/src/test/kotlin/io/github/rygel/fragments/architecture/LayeringRulesTest.kt`
- Modify: `fragments-architecture-tests/src/test/kotlin/io/github/rygel/fragments/architecture/ModuleDependencyRulesTest.kt`
- Modify: `fragments-architecture-tests/src/test/kotlin/io/github/rygel/fragments/architecture/NamingConventionRulesTest.kt`

Also check and fix any other test files with backtick names found outside the architecture module (ChatExtensionTest.kt, ChatExtensionE2ETest.kt, BlogEngineIntegrationTest.kt, etc.).

- [ ] **Step 1: Rename all backtick test methods in CodingRulesTest.kt**

| Line | Old | New |
|------|-----|-----|
| 23 | `` fun `must not use java util Date - use java time instead`() `` | `fun testMustNotUseJavaUtilDate() {` |
| 41 | `` fun `must not use java util Calendar`() `` | `fun testMustNotUseJavaUtilCalendar() {` |
| 53 | `` fun `must not use java util Hashtable`() `` | `fun testMustNotUseJavaUtilHashtable() {` |
| 65 | `` fun `must not use java util Vector`() `` | `fun testMustNotUseJavaUtilVector() {` |
| 79 | `` fun `core and feature modules must not use System out or System err`() `` | `fun testCoreMustNotUseSystemOutOrErr() {` |
| 97 | `` fun `production code must not depend on JUnit`() `` | `fun testProductionCodeMustNotDependOnJUnit() {` |
| 111 | `` fun `production code must not depend on MockK`() `` | `fun testProductionCodeMustNotDependOnMockK() {` |
| 125 | `` fun `core must not depend on Spring`() `` | `fun testCoreMustNotDependOnSpring() {` |
| 138 | `` fun `core must not depend on Jakarta EE`() `` | `fun testCoreMustNotDependOnJakartaEE() {` |
| 151 | `` fun `core must not depend on Micronaut`() `` | `fun testCoreMustNotDependOnMicronaut() {` |
| 164 | `` fun `core must not depend on http4k`() `` | `fun testCoreMustNotDependOnHttp4k() {` |
| 177 | `` fun `core must not depend on Javalin`() `` | `fun testCoreMustNotDependOnJavalin() {` |

- [ ] **Step 2: Rename all backtick test methods in LayeringRulesTest.kt**

| Line | Old | New |
|------|-----|-----|
| 30 | `` fun `repository interfaces in core must be interfaces not classes`() `` | `fun testRepositoryInterfacesMustBeInterfaces() {` |
| 59 | `` fun `blog module must not depend on adapters`() `` | `fun testBlogModuleMustNotDependOnAdapters() {` |
| 64 | `` fun `cache module must not depend on adapters`() `` | `fun testCacheModuleMustNotDependOnAdapters() {` |
| 69 | `` fun `rss module must not depend on adapters`() `` | `fun testRssModuleMustNotDependOnAdapters() {` |
| 74 | `` fun `lucene module must not depend on adapters`() `` | `fun testLuceneModuleMustNotDependOnAdapters() {` |
| 79 | `` fun `sitemap module must not depend on adapters`() `` | `fun testSitemapModuleMustNotDependOnAdapters() {` |
| 84 | `` fun `livereload module must not depend on adapters`() `` | `fun testLivereloadModuleMustNotDependOnAdapters() {` |
| 89 | `` fun `chat module must not depend on adapters`() `` | `fun testChatModuleMustNotDependOnAdapters() {` |
| 94 | `` fun `navigation module must not depend on adapters`() `` | `fun testNavigationModuleMustNotDependOnAdapters() {` |
| 99 | `` fun `social module must not depend on adapters`() `` | `fun testSocialModuleMustNotDependOnAdapters() {` |
| 104 | `` fun `seo module must not depend on adapters`() `` | `fun testSeoModuleMustNotDependOnAdapters() {` |
| 109 | `` fun `image module must not depend on adapters`() `` | `fun testImageModuleMustNotDependOnAdapters() {` |

- [ ] **Step 3: Rename all backtick test methods in ModuleDependencyRulesTest.kt**

| Line | Old | New |
|------|-----|-----|
| 24 | `` fun `core must not depend on any adapter module`() `` | `fun testCoreMustNotDependOnAdapterModules() {` |
| 51 | `` fun `http4k adapter must not depend on other adapters`() `` | `fun testHttp4kAdapterMustNotDependOnOtherAdapters() {` |
| 56 | `` fun `javalin adapter must not depend on other adapters`() `` | `fun testJavalinAdapterMustNotDependOnOtherAdapters() {` |
| 61 | `` fun `spring adapter must not depend on other adapters`() `` | `fun testSpringAdapterMustNotDependOnOtherAdapters() {` |
| 66 | `` fun `quarkus adapter must not depend on other adapters`() `` | `fun testQuarkusAdapterMustNotDependOnOtherAdapters() {` |
| 71 | `` fun `micronaut adapter must not depend on other adapters`() `` | `fun testMicronautAdapterMustNotDependOnOtherAdapters() {` |
| 78 | `` fun `there must be no cyclic dependencies between packages`() `` | `fun testNoCyclicDependenciesBetweenPackages() {` |

- [ ] **Step 4: Rename all backtick test methods in NamingConventionRulesTest.kt**

| Line | Old | New |
|------|-----|-----|
| 20 | `` fun `repository interfaces must end with Repository`() `` | `fun testRepositoryInterfacesMustEndWithRepository() {` |
| 35 | `` fun `classes implementing repository interfaces must end with Repository`() `` | `fun testRepositoryImplementationsMustEndWithRepository() {` |
| 46 | `` fun `factory classes must end with Factory or Generator`() `` | `fun testFactoryClassesMustEndWithFactoryOrGenerator() {` |

- [ ] **Step 5: Scan for and fix any other backtick test methods outside architecture tests**

Use: `Get-ChildItem -Recurse -Filter "*.kt" | Select-String -Pattern "fun \x60"` to find remaining backtick test names in: ChatExtensionTest.kt, ChatExtensionE2ETest.kt, ClasspathFragmentRepositoryTest.kt, BlogEngineIntegrationTest.kt, FragmentFrontMatterAccessorsTest.kt, LuceneSearchEngineTest.kt, RssGeneratorTest.kt, SitemapGeneratorTest.kt. Rename all to camelCase.

- [ ] **Step 6: Run full test suite**

Run: `mvn test -T 4 -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "style: rename backtick test methods to camelCase for Surefire compatibility"
```
