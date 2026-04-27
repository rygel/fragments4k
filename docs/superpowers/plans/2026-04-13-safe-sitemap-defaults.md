# Safe Sitemap/RSS/llms.txt Defaults — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generators skip fragments without `resolvedUrl`, so sitemaps/RSS/llms.txt never emit guessed URLs that may not match actual HTTP routes.

**Architecture:** Add a `resolvedUrl != null` filter in each generator's fragment pipeline and log a WARN on first skip. Existing tests already use `resolvedUrl` in sitemap and RSS; LlmsTxt tests need updating.

**Tech Stack:** Kotlin, SLF4J, JUnit 5, MockK

---

## Task 1: SitemapGenerator — skip unresolved fragments

**Files:**
- Modify: `fragments-sitemap-core/src/main/kotlin/io/github/rygel/fragments/sitemap/SitemapGenerator.kt:33-38`
- Test: `fragments-sitemap-core/src/test/kotlin/io/github/rygel/fragments/sitemap/SitemapGeneratorTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `SitemapGeneratorTest.kt`:

```kotlin
@Test
fun `fragments without resolvedUrl are excluded from sitemap`() =
    runBlocking {
        coEvery { repository.getAllVisible() } returns
            listOf(
                fragment("resolved-post", "Resolved Post", resolvedUrl = "/blog/2026/03/resolved-post"),
                Fragment(
                    title = "Unresolved Page",
                    slug = "unresolved-page",
                    content = "Content",
                    preview = "Preview",
                    publishDate = null,
                    frontMatter = emptyMap(),
                    date = LocalDateTime.of(2026, 1, 15, 10, 0),
                    status = FragmentStatus.PUBLISHED,
                    visible = true,
                    template = "default",
                ),
            )
        val xml = SitemapGenerator(repository, "https://example.com").generateSitemap()
        assertTrue(xml.contains("/blog/2026/03/resolved-post"), "resolved fragment must be present")
        assertFalse(xml.contains("unresolved-page"), "unresolved fragment must be excluded")
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -T 4 -pl fragments-sitemap-core -Dtest="SitemapGeneratorTest#fragments without resolvedUrl are excluded from sitemap" -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL — the unresolved fragment's URL `/unresolved-page` appears in the output.

- [ ] **Step 3: Implement the filter in SitemapGenerator**

In `SitemapGenerator.kt`, add a logger and filter. Change lines 33-38 from:

```kotlin
val fragments =
    (resolvedFragments ?: repositories.flatMap { it.getAllVisible() })
        .distinctBy { it.slug }
        .filter { it.template !in excludedTemplates }
```

to:

```kotlin
val allCandidates =
    (resolvedFragments ?: repositories.flatMap { it.getAllVisible() })
        .distinctBy { it.slug }
        .filter { it.template !in excludedTemplates }

val skipped = allCandidates.filter { it.resolvedUrl == null }
if (skipped.isNotEmpty()) {
    logger.warn(
        "Skipping {} fragment(s) without resolvedUrl in sitemap (configure urlBuilder on the repository): {}",
        skipped.size,
        skipped.joinToString { it.slug },
    )
}
val fragments = allCandidates.filter { it.resolvedUrl != null }
```

Also add a logger field and the SLF4J import at the top of the class:

```kotlin
import org.slf4j.LoggerFactory
```

Inside the class body (before the `constructor`):

```kotlin
private val logger = LoggerFactory.getLogger(SitemapGenerator::class.java)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -T 4 -pl fragments-sitemap-core -Dtest="SitemapGeneratorTest" -Dsurefire.failIfNoSpecifiedTests=false`
Expected: ALL tests pass.

- [ ] **Step 5: Verify the "multiple repositories" test still passes**

The existing test `fragments from multiple repositories are all included` creates `fragment("about", "About")` which uses the helper's default `resolvedUrl = "/$slug"` = `"/about"`, so it already has a resolvedUrl and should pass unchanged.

- [ ] **Step 6: Commit**

```bash
git add fragments-sitemap-core/
git commit -m "fix: SitemapGenerator skips fragments without resolvedUrl

Fragments that have no urlBuilder configured get resolvedUrl=null,
which produces guessed URLs in the sitemap. These are now excluded
with a WARN log so developers notice during development.

Refs #65, #77"
```

---

## Task 2: RssGenerator — skip unresolved fragments

**Files:**
- Modify: `fragments-rss-core/src/main/kotlin/io/github/rygel/fragments/rss/RssGenerator.kt:18-30`
- Modify: `fragments-rss-core/pom.xml` (add slf4j-api dependency)
- Test: `fragments-rss-core/src/test/kotlin/io/github/rygel/fragments/rss/RssGeneratorTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `RssGeneratorTest.kt`:

```kotlin
@Test
fun `fragments without resolvedUrl are excluded from feed`() =
    runBlocking {
        coEvery { repository.getAllVisible() } returns
            listOf(
                fragment("resolved-post", "Resolved Post"),
                Fragment(
                    title = "Unresolved Post",
                    slug = "unresolved-post",
                    content = "Content",
                    preview = "Preview",
                    publishDate = null,
                    frontMatter = emptyMap(),
                    date = LocalDateTime.of(2026, 1, 15, 10, 0),
                    status = FragmentStatus.PUBLISHED,
                    visible = true,
                    template = "blog",
                ),
            )
        val xml = RssGenerator(repository).generateFeed(siteUrl = "https://example.com")
        assertValidXml(xml)
        assertTrue(xml.contains("Resolved Post"), "resolved fragment must be present")
        assertFalse(xml.contains("Unresolved Post"), "unresolved fragment must be excluded")
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -T 4 -pl fragments-rss-core -Dtest="RssGeneratorTest#fragments without resolvedUrl are excluded from feed" -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL — the unresolved fragment appears in the output.

- [ ] **Step 3: Add slf4j-api dependency to fragments-rss-core/pom.xml**

Add after the `kotlinx-coroutines-core` dependency:

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
</dependency>
```

- [ ] **Step 4: Implement the filter in RssGenerator**

In `RssGenerator.kt`, add import and logger:

```kotlin
import org.slf4j.LoggerFactory
```

Add logger field inside the class (after the secondary constructor):

```kotlin
private val logger = LoggerFactory.getLogger(RssGenerator::class.java)
```

Change the fragment pipeline in `generateFeed` from:

```kotlin
val fragments =
    (resolvedFragments ?: repositories.flatMap { it.getAllVisible() })
        .filter { it.template in FragmentTemplates.BLOG_TEMPLATES }
        .distinctBy { it.slug }
        .sortedByDescending { it.date }
        .take(MAX_ITEMS)
```

to:

```kotlin
val allCandidates =
    (resolvedFragments ?: repositories.flatMap { it.getAllVisible() })
        .filter { it.template in FragmentTemplates.BLOG_TEMPLATES }
        .distinctBy { it.slug }

val skipped = allCandidates.filter { it.resolvedUrl == null }
if (skipped.isNotEmpty()) {
    logger.warn(
        "Skipping {} fragment(s) without resolvedUrl in RSS feed (configure urlBuilder on the repository): {}",
        skipped.size,
        skipped.joinToString { it.slug },
    )
}
val fragments = allCandidates
    .filter { it.resolvedUrl != null }
    .sortedByDescending { it.date }
    .take(MAX_ITEMS)
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -T 4 -pl fragments-rss-core -Dtest="RssGeneratorTest" -Dsurefire.failIfNoSpecifiedTests=false`
Expected: ALL tests pass.

- [ ] **Step 6: Commit**

```bash
git add fragments-rss-core/
git commit -m "fix: RssGenerator skips fragments without resolvedUrl

Same safe-default behavior as SitemapGenerator — fragments without an
explicit resolvedUrl are excluded from the RSS feed with a WARN log.

Refs #65, #77"
```

---

## Task 3: LlmsTxtGenerator — skip unresolved fragments

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/LlmsTxtGenerator.kt:33-45`
- Test: `fragments-core/src/test/kotlin/io/github/rygel/fragments/LlmsTxtGeneratorTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `LlmsTxtGeneratorTest.kt`:

```kotlin
@Test
fun fragmentsWithoutResolvedUrlAreExcluded() =
    runBlocking {
        val repo = InMemoryFragmentRepository()
        repo.addFragment(
            createFragment(
                "resolved-post",
                "Resolved Post",
                template = "blog_post",
                date = LocalDateTime.of(2024, 3, 15, 10, 0),
                preview = "<p>A resolved blog post.</p>",
            ).copy(resolvedUrl = "/blog/2024/03/resolved-post"),
        )
        repo.addFragment(
            createFragment(
                "unresolved-post",
                "Unresolved Post",
                template = "blog_post",
                date = LocalDateTime.of(2024, 4, 1, 10, 0),
                preview = "<p>An unresolved blog post.</p>",
            ),
        )
        repo.addFragment(
            createFragment(
                "resolved-page",
                "Resolved Page",
                template = "page",
                preview = "<p>A resolved page.</p>",
            ).copy(resolvedUrl = "/about"),
        )
        repo.addFragment(
            createFragment(
                "unresolved-page",
                "Unresolved Page",
                template = "page",
                preview = "<p>An unresolved page.</p>",
            ),
        )

        val result =
            LlmsTxtGenerator.generate(
                siteTitle = "Test",
                siteDescription = "Test",
                siteUrl = "https://example.com",
                repositories = listOf(repo),
            )

        assertTrue(result.contains("Resolved Post"))
        assertFalse(result.contains("Unresolved Post"))
        assertTrue(result.contains("Resolved Page"))
        assertFalse(result.contains("Unresolved Page"))
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -T 4 -pl fragments-core -Dtest="LlmsTxtGeneratorTest#fragmentsWithoutResolvedUrlAreExcluded" -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL — unresolved fragments appear in the output.

- [ ] **Step 3: Implement the filter in LlmsTxtGenerator**

In `LlmsTxtGenerator.kt`, add import:

```kotlin
import org.slf4j.LoggerFactory
```

Add logger inside the object:

```kotlin
private val logger = LoggerFactory.getLogger(LlmsTxtGenerator::class.java)
```

Change the fragment pipeline (lines 33-45) from:

```kotlin
val allFragments =
    (resolvedFragments ?: repositories.flatMap { it.getAllVisible() })
        .distinctBy { it.slug }

val blogPosts =
    allFragments
        .filter { it.template in BLOG_TEMPLATES }
        .sortedByDescending { it.date }

val pages =
    allFragments
        .filter { it.template !in BLOG_TEMPLATES }
        .sortedBy { it.title }
```

to:

```kotlin
val allCandidates =
    (resolvedFragments ?: repositories.flatMap { it.getAllVisible() })
        .distinctBy { it.slug }

val skipped = allCandidates.filter { it.resolvedUrl == null }
if (skipped.isNotEmpty()) {
    logger.warn(
        "Skipping {} fragment(s) without resolvedUrl in llms.txt (configure urlBuilder on the repository): {}",
        skipped.size,
        skipped.joinToString { it.slug },
    )
}
val allFragments = allCandidates.filter { it.resolvedUrl != null }

val blogPosts =
    allFragments
        .filter { it.template in BLOG_TEMPLATES }
        .sortedByDescending { it.date }

val pages =
    allFragments
        .filter { it.template !in BLOG_TEMPLATES }
        .sortedBy { it.title }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -T 4 -pl fragments-core -Dtest="LlmsTxtGeneratorTest" -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL — existing tests that don't set resolvedUrl will now produce empty output.

- [ ] **Step 5: Update existing LlmsTxtGeneratorTest to set resolvedUrl**

The existing `createFragment` helper does not set `resolvedUrl`. All existing tests rely on the `/$slug` fallback. Update the `createFragment` helper to accept and set `resolvedUrl`:

```kotlin
private fun createFragment(
    slug: String,
    title: String,
    template: String = "default",
    date: LocalDateTime? = null,
    preview: String = "<p>Preview text</p>",
    resolvedUrl: String = "/$slug",
): Fragment =
    Fragment(
        slug = slug,
        title = title,
        content = "<p>Content for $title</p>",
        date = date,
        publishDate = date,
        preview = preview,
        template = template,
        visible = true,
        frontMatter = mapOf("title" to title, "slug" to slug),
        resolvedUrl = resolvedUrl,
    )
```

Then update the new test's resolved fragments to use the helper directly instead of `.copy()`:

```kotlin
repo.addFragment(
    createFragment(
        "resolved-post",
        "Resolved Post",
        template = "blog_post",
        date = LocalDateTime.of(2024, 3, 15, 10, 0),
        preview = "<p>A resolved blog post.</p>",
        resolvedUrl = "/blog/2024/03/resolved-post",
    ),
)
```

And for unresolved fragments, pass `resolvedUrl = null` explicitly via `.copy()`:

```kotlin
repo.addFragment(
    createFragment(
        "unresolved-post",
        "Unresolved Post",
        template = "blog_post",
        date = LocalDateTime.of(2024, 4, 1, 10, 0),
        preview = "<p>An unresolved blog post.</p>",
    ).copy(resolvedUrl = null),
)
```

- [ ] **Step 6: Run all LlmsTxtGeneratorTest tests**

Run: `mvn test -T 4 -pl fragments-core -Dtest="LlmsTxtGeneratorTest" -Dsurefire.failIfNoSpecifiedTests=false`
Expected: ALL tests pass.

- [ ] **Step 7: Commit**

```bash
git add fragments-core/
git commit -m "fix: LlmsTxtGenerator skips fragments without resolvedUrl

Same safe-default behavior as SitemapGenerator and RssGenerator.
Updated test helper to set resolvedUrl by default.

Refs #65, #77"
```

---

## Task 4: Full build verification

- [ ] **Step 1: Run full build**

Run: `mvn verify -T 4`
Expected: BUILD SUCCESS with all tests passing.

- [ ] **Step 2: Run ktlint and detekt checks**

These run as part of `mvn verify`. Confirm 0 code smells and 0 formatting issues in the output.

- [ ] **Step 3: Commit any formatting fixes if needed**

If ktlint auto-format made changes:
```bash
git add -u
git commit -m "style: apply ktlint formatting"
```
