# URL Resolution & Naming Consistency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix sitemap/llms.txt/RSS/SEO canonical URLs to match actual adapter routes, and rename `Fragment.content` to `Fragment.htmlContent` for API consistency.

**Architecture:** Three independent changes: (1) Add a `urlBuilder` to adapter configurations so fragments get correct resolved URLs matching their actual routes; (2) Fix `SeoMetadata.fromFragment` to use `fragment.url` instead of hardcoding `/page/{slug}`; (3) Rename `Fragment.content` → `Fragment.htmlContent` with `@Deprecated` backward compatibility.

**Tech Stack:** Kotlin, Maven, JUnit 5, MockK

---

## File Structure

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `fragments-core/.../Fragment.kt` | Rename `content` → `htmlContent`, add deprecated `content` alias |
| Modify | `fragments-core/.../ClasspathFragmentRepository.kt` | Update to use `htmlContent` |
| Modify | `fragments-core/.../FileSystemFragmentRepository.kt` | Update to use `htmlContent`, pass `baseUrl` to Fragment constructor |
| Modify | `fragments-core/.../FragmentViewModel.kt` | Update to use `htmlContent` |
| Modify | `fragments-core/.../ContentRelationshipGenerator.kt` | Update to use `htmlContent` |
| Modify | `fragments-core/.../FileSystemFragmentRevisionRepository.kt` | Update to use `htmlContent` |
| Modify | `fragments-adapter-core/.../FragmentsEngine.kt` | Add `staticPageUrlBuilder` and `blogUrlBuilder` params, pass them through |
| Modify | `fragments-seo-core/.../SeoMetadata.kt` | Fix `fromFragment` to use `fragment.url` instead of hardcoded `/page/{slug}` |
| Modify | `fragments-spring-boot/.../FragmentsSpringConfiguration.kt` | Wire `urlBuilder` into repository + `staticPageUrlBuilder` into engine |
| Modify | `fragments-quarkus/.../FragmentsQuarkusConfiguration.kt` | Same |
| Modify | `fragments-micronaut/.../FragmentsMicronautConfiguration.kt` | Same |
| Modify | `fragments-http4k/.../FragmentsHttp4kAdapter.kt` | Accept engine with urlBuilders |
| Modify | `fragments-javalin/.../FragmentsJavalinAdapter.kt` | Same |
| Modify | `fragments-spring-boot/.../FragmentsSpringController.kt` | Accept engine with urlBuilders |
| Modify | `fragments-quarkus/.../FragmentsQuarkusResource.kt` | Same |
| Modify | `fragments-micronaut/.../FragmentsMicronautController.kt` | Same |
| Modify | `fragments-cli/.../ProjectGenerator.kt` | Update templates to use `htmlContent` |
| Modify | `fragments-lucene-core/.../LuceneSearchEngine.kt` | No change needed (uses `contentTextOnly`) |
| Modify | Tests in multiple modules | Update assertions |

---

## Task 1: Rename Fragment.content → Fragment.htmlContent

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/Fragment.kt`
- Test: `fragments-core/src/test/kotlin/io/github/rygel/fragments/FragmentTest.kt`

- [ ] **Step 1: Rename the field and add deprecated alias in Fragment.kt**

In `Fragment.kt`, change line 176 from:
```kotlin
val content: String,
```
to:
```kotlin
val htmlContent: String,
```

Add a deprecated extension property after the data class (around line 271, after the companion object):
```kotlin
@Deprecated("Use htmlContent instead. content was ambiguously named — it holds rendered HTML, not raw markdown.", ReplaceWith("htmlContent"))
val Fragment.content: String
    get() = htmlContent
```

Update the KDoc at line 141 from:
```kotlin
 * @property content Full rendered HTML body.
```
to:
```kotlin
 * @property htmlContent Full rendered HTML body.
```

Update `hasMoreTag` (line 207-209), `contentTextOnly` (lines 220-229) to use `htmlContent` instead of `content`.

- [ ] **Step 2: Run tests to verify nothing breaks (deprecated alias keeps backward compat)**

Run: `mvn -pl fragments-core test -T 4`
Expected: All tests pass — the `@Deprecated` extension property provides backward compatibility.

- [ ] **Step 3: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/Fragment.kt
git commit -m "refactor: rename Fragment.content to Fragment.htmlContent with deprecated alias

Addresses #89 part 1 — the old name was ambiguous since
ParsedContent.content holds raw markdown but Fragment.content held
rendered HTML."
```

---

## Task 2: Update repositories and core classes to use htmlContent

**Files:**
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/ClasspathFragmentRepository.kt`
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRepository.kt`
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentViewModel.kt`
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/ContentRelationshipGenerator.kt`
- Modify: `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRevisionRepository.kt`
- Modify: `fragments-cli/src/main/kotlin/io/github/rygel/fragments/cli/ProjectGenerator.kt`
- Modify: `fragments-test-data-factories/...` (if it references `content`)
- Test: existing tests should still pass with deprecated alias

- [ ] **Step 1: Update ClasspathFragmentRepository.kt**

Line 304 — change:
```kotlin
content = parsed.htmlContent,
```
to:
```kotlin
htmlContent = parsed.htmlContent,
```

- [ ] **Step 2: Update FileSystemFragmentRepository.kt**

Line 327 — change:
```kotlin
content = parsed.htmlContent,
```
to:
```kotlin
htmlContent = parsed.htmlContent,
```

Also at line 699 in `revertToRevision`, change:
```kotlin
append(result.content)
```
to:
```kotlin
append(result.htmlContent)
```

Also fix line 318-344 — the `baseUrl` is NOT passed to the Fragment constructor. Add it:
After line 321, add:
```kotlin
baseUrl = baseUrl,
```
(This is a separate bug fix — `FileSystemFragmentRepository` ignores its own `baseUrl` when constructing Fragments, unlike `ClasspathFragmentRepository` which passes it at line 298.)

- [ ] **Step 3: Update FragmentViewModel.kt**

Line 100-101 — change:
```kotlin
val content: String
    get() = fragment.content
```
to:
```kotlin
val content: String
    get() = fragment.htmlContent
```

(ViewModel keeps `content` name since templates use `{{ content }}` — this is the view layer where "content" is unambiguous.)

- [ ] **Step 4: Update ContentRelationshipGenerator.kt**

Line 212 — change:
```kotlin
pattern.findAll(fragment.content).forEach { match ->
```
to:
```kotlin
pattern.findAll(fragment.htmlContent).forEach { match ->
```

- [ ] **Step 5: Update FileSystemFragmentRevisionRepository.kt**

Line 41 — change:
```kotlin
content = fragment.content,
```
to:
```kotlin
htmlContent = fragment.htmlContent,
```

- [ ] **Step 6: Update ProjectGenerator.kt**

Lines 636, 722, 788 — change template strings from `fragment.content` to `fragment.htmlContent`:
- Line 636: `<div>{{ fragment.htmlContent|raw }}</div>`
- Line 722: `<div th:utext="${'$'}{fragment.htmlContent}">Content</div>`
- Line 788: `<div>{fragment.htmlContent}</div>`

- [ ] **Step 7: Update test files**

Update any test that constructs `Fragment(content = ...)` to use `Fragment(htmlContent = ...)`:

- `ClasspathFragmentRepositoryTest.kt` line 222: Change assertion from `fragment.content.contains(...)` to `fragment.htmlContent.contains(...)` (the deprecated alias may still work, but be explicit)
- `FragmentTest.kt`: Change all `content =` to `htmlContent =` in test fragment constructors
- `SitemapGeneratorTest.kt` line 227: Change `content = "Test content"` to `htmlContent = "Test content"`
- `LlmsTxtGeneratorTest.kt` line 245: Change `content =` to `htmlContent =`
- `InMemoryFragmentRevisionRepository.kt` line 36: Change `content = fragment.content` to `htmlContent = fragment.htmlContent`
- `InMemoryFragmentRepository.kt`: Change any `content =` to `htmlContent =` in fragment constructors
- `ChatExtensionE2ETest.kt` line 154: Change `fragment.content.contains(...)` to `fragment.htmlContent.contains(...)`
- `TestFactoriesTest.kt` line 343: Change `fragment.content` to `fragment.htmlContent`
- `SeoMetadataTest.kt`: Change any `content =` to `htmlContent =` in fragment constructors

- [ ] **Step 8: Run full test suite**

Run: `mvn test -T 4`
Expected: All tests pass.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor: migrate all internal code to Fragment.htmlContent

The deprecated Fragment.content extension property maintains backward
compatibility for library consumers. This commit updates all internal
usage to the new name."
```

---

## Task 3: Fix SeoMetadata.fromFragment canonical URL

**Files:**
- Modify: `fragments-seo-core/src/main/kotlin/io/github/rygel/fragments/SeoMetadata.kt`
- Modify: `fragments-seo-core/src/test/kotlin/io/github/rygel/fragments/test/SeoMetadataTest.kt`

- [ ] **Step 1: Fix canonical URL in SeoMetadata.fromFragment**

In `SeoMetadata.kt` lines 189-194, change:
```kotlin
val canonicalUrl =
    if (pagePath != null) {
        "$siteUrl/$pagePath"
    } else {
        "$siteUrl/page/${fragment.slug}"
    }
```
to:
```kotlin
val canonicalUrl =
    if (pagePath != null) {
        "$siteUrl/$pagePath"
    } else {
        "$siteUrl${fragment.url}"
    }
```

This makes the canonical URL use the fragment's actual URL (which comes from `resolvedUrl` or `baseUrl/slug`) instead of hardcoding `/page/{slug}`.

- [ ] **Step 2: Update SeoMetadataTest to verify the fix**

Add a test that verifies `fromFragment` uses `fragment.url` when no `pagePath` is given:
```kotlin
@Test
fun fromFragmentUsesFragmentUrlForCanonicalWhenNoPagePath() {
    val fragment = Fragment(
        title = "Test",
        slug = "my-post",
        htmlContent = "<p>Content</p>",
        preview = "Preview",
        date = LocalDateTime.of(2024, 1, 1, 10, 0),
        publishDate = null,
        frontMatter = emptyMap(),
        resolvedUrl = "/blog/2024/01/my-post",
    )
    val seo = SeoMetadata.fromFragment(
        fragment = fragment,
        siteUrl = "https://example.com",
    )
    assertEquals("https://example.com/blog/2024/01/my-post", seo.canonicalUrl)
}
```

Also add a test for the pagePath override still working:
```kotlin
@Test
fun fromFragmentUsesPagePathWhenProvided() {
    val fragment = Fragment(
        title = "Test",
        slug = "my-post",
        htmlContent = "<p>Content</p>",
        preview = "Preview",
        date = LocalDateTime.of(2024, 1, 1, 10, 0),
        publishDate = null,
        frontMatter = emptyMap(),
    )
    val seo = SeoMetadata.fromFragment(
        fragment = fragment,
        siteUrl = "https://example.com",
        pagePath = "custom/path",
    )
    assertEquals("https://example.com/custom/path", seo.canonicalUrl)
}
```

- [ ] **Step 3: Run tests**

Run: `mvn -pl fragments-seo-core test -T 4`
Expected: All tests pass.

- [ ] **Step 4: Commit**

```bash
git add fragments-seo-core/
git commit -m "fix: use fragment.url for SEO canonical URL instead of hardcoded /page/{slug}

Addresses #77 — canonical URLs now match the actual route where the
page is served, not a hardcoded pattern."
```

---

## Task 4: Wire urlBuilder into adapter configurations

**Files:**
- Modify: `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt`
- Modify: `fragments-spring-boot/src/main/kotlin/io/github/rygel/fragments/spring/FragmentsSpringConfiguration.kt`
- Modify: `fragments-quarkus/src/main/kotlin/io/github/rygel/fragments/quarkus/FragmentsQuarkusConfiguration.kt`
- Modify: `fragments-micronaut/src/main/kotlin/io/github/rygel/fragments/micronaut/FragmentsMicronautConfiguration.kt`

- [ ] **Step 1: Add staticPageUrlBuilder and blogUrlBuilder to FragmentsEngine**

In `FragmentsEngine.kt`, add two new constructor parameters after `footer`:
```kotlin
class FragmentsEngine(
    val staticEngine: StaticPageEngine,
    val blogEngine: BlogEngine,
    val searchEngine: LuceneSearchEngine,
    val siteTitle: String = "My Blog",
    val siteDescription: String = "My Awesome Blog",
    val siteUrl: String = "http://localhost:8080",
    val feedUrl: String = "$siteUrl/rss.xml",
    val authorRepository: AuthorRepository? = null,
    additionalRepositories: List<FragmentRepository> = emptyList(),
    val navigationMenu: List<NavigationLink>? = null,
    val footer: FooterConfig? = null,
    val staticPageUrlBuilder: ((Fragment) -> String)? = { "/page/${it.slug}" },
    val blogUrlBuilder: ((Fragment) -> String)? = null,
) {
```

The `staticPageUrlBuilder` defaults to `{ "/page/${it.slug}" }` matching the current adapter route `/page/{slug}`.

The `blogUrlBuilder` defaults to `null`. When set, it provides date-based URLs like `/blog/2026/03/slug`.

These builders are passed to the repositories during construction — but since `FragmentsEngine` receives pre-constructed engines, we need a different approach. Instead, apply URL resolution post-load.

**Better approach:** Add a `urlResolver` function to `FragmentsEngine` that maps fragments to their correct URLs. The sitemap, RSS, and llms.txt generators already use `fragment.url`, so if we set `resolvedUrl` correctly on fragments, everything works.

Since `FragmentsEngine` receives pre-built `StaticPageEngine` and `BlogEngine`, the URL builders must be applied at repository construction time (in the configurations), not in `FragmentsEngine`. So we don't change `FragmentsEngine` — we change the configurations.

- [ ] **Step 1 (revised): Update adapter configurations to pass urlBuilder**

**FragmentsSpringConfiguration.kt** — change `fragmentRepository()` to create separate repositories for static pages and blog posts:
```kotlin
@Configuration
class FragmentsSpringConfiguration {
    @Bean
    fun staticPageRepository(): FragmentRepository {
        val fragmentsPath = System.getProperty("fragments.path") ?: System.getenv("FRAGMENTS_PATH") ?: "./content"
        return FileSystemFragmentRepository(
            basePath = fragmentsPath,
            urlBuilder = { "/page/${it.slug}" },
        )
    }

    @Bean
    fun blogRepository(): FragmentRepository {
        val fragmentsPath = System.getProperty("fragments.path") ?: System.getenv("FRAGMENTS_PATH") ?: "./content"
        return FileSystemFragmentRepository(
            basePath = fragmentsPath,
            urlBuilder = { fragment ->
                val date = fragment.date ?: return@FileSystemFragmentRepository "/blog/${fragment.slug}"
                "/blog/${date.year}/${"%02d".format(date.monthValue)}/${fragment.slug}"
            },
        )
    }

    @Bean
    fun staticPageEngine(staticPageRepository: FragmentRepository): StaticPageEngine =
        StaticPageEngine(staticPageRepository)

    @Bean
    fun blogEngine(blogRepository: FragmentRepository): BlogEngine =
        BlogEngine(blogRepository)

    @Bean
    fun searchEngine(staticPageRepository: FragmentRepository): LuceneSearchEngine =
        LuceneSearchEngine(staticPageRepository)

    @Bean
    fun fragmentsEngine(
        staticEngine: StaticPageEngine,
        blogEngine: BlogEngine,
        searchEngine: LuceneSearchEngine,
    ): FragmentsEngine =
        FragmentsEngine(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            searchEngine = searchEngine,
        )
}
```

**Wait — this creates two separate repositories pointing at the same directory, each loading all files.** That doubles memory usage and causes slug conflicts. The current architecture shares one repository between both engines.

**Better approach:** Apply the URL builder differently. Since both `StaticPageEngine` and `BlogEngine` share one repository, we can't use different `urlBuilder` lambdas for different content types at the repository level.

Instead, the URL should be resolved at the point where it's used — in the generators. Add a `urlResolver` parameter to `FragmentsEngine` that the generators use.

**Final approach:**

1. Add a `urlResolver: (Fragment) -> String` parameter to `FragmentsEngine` (defaults to the current `fragment.url` behavior).
2. Pass this resolver to `SitemapGenerator`, `LlmsTxtGenerator`, and `RssGenerator`.
3. In adapter configurations, provide a resolver that uses fragment metadata (template, date) to build correct URLs.

- [ ] **Step 1 (final): Add urlResolver to FragmentsEngine**

In `FragmentsEngine.kt`, add a constructor parameter:
```kotlin
class FragmentsEngine(
    val staticEngine: StaticPageEngine,
    val blogEngine: BlogEngine,
    val searchEngine: LuceneSearchEngine,
    val siteTitle: String = "My Blog",
    val siteDescription: String = "My Awesome Blog",
    val siteUrl: String = "http://localhost:8080",
    val feedUrl: String = "$siteUrl/rss.xml",
    val authorRepository: AuthorRepository? = null,
    additionalRepositories: List<FragmentRepository> = emptyList(),
    val navigationMenu: List<NavigationLink>? = null,
    val footer: FooterConfig? = null,
    val urlResolver: (Fragment) -> String = { it.url },
) {
```

Then pass `urlResolver` to generators. Since `SitemapGenerator`, `LlmsTxtGenerator`, and `RssGenerator` all use `fragment.url` directly, we need to either:
a) Pass `urlResolver` into each generator, OR
b) Post-process fragments to set `resolvedUrl` before they reach generators.

Option (b) is cleaner — no need to change generator APIs. In `FragmentsEngine`, override the `allRepositories` to wrap them:

Actually, the cleanest solution is to pass a `urlBuilder` to the single shared repository that handles both static and blog content based on the fragment's template:

```kotlin
FileSystemFragmentRepository(
    basePath = fragmentsPath,
    urlBuilder = { fragment ->
        when (fragment.template) {
            "blog", "blog_post" -> {
                val date = fragment.date ?: return@FileSystemFragmentRepository "/${fragment.slug}"
                "/blog/${date.year}/${"%02d".format(date.monthValue)}/${fragment.slug}"
            }
            else -> "/page/${fragment.slug}"
        }
    },
)
```

This is the simplest fix. It handles both content types in one lambda, matching the actual adapter routes.

- [ ] **Step 2: Update Spring configuration**

In `FragmentsSpringConfiguration.kt`, change `fragmentRepository()`:
```kotlin
@Bean
fun fragmentRepository(): FragmentRepository {
    val fragmentsPath = System.getProperty("fragments.path") ?: System.getenv("FRAGMENTS_PATH") ?: "./content"
    return FileSystemFragmentRepository(fragmentsPath) {
        fragment ->
        when (fragment.template) {
            "blog", "blog_post" -> {
                val date = fragment.date ?: return@FileSystemFragmentRepository "/${fragment.slug}"
                "/blog/${date.year}/${"%02d".format(date.monthValue)}/${fragment.slug}"
            }
            else -> "/page/${fragment.slug}"
        }
    }
}
```

- [ ] **Step 3: Update Quarkus configuration**

Same change in `FragmentsQuarkusConfiguration.kt`:
```kotlin
@Produces
@ApplicationScoped
fun fragmentRepository(): FragmentRepository = FileSystemFragmentRepository(fragmentsPath) { fragment ->
    when (fragment.template) {
        "blog", "blog_post" -> {
            val date = fragment.date ?: return@FileSystemFragmentRepository "/${fragment.slug}"
            "/blog/${date.year}/${"%02d".format(date.monthValue)}/${fragment.slug}"
        }
        else -> "/page/${fragment.slug}"
    }
}
```

- [ ] **Step 4: Update Micronaut configuration**

Same change in `FragmentsMicronautConfiguration.kt`:
```kotlin
@Singleton
fun fragmentRepository(): FragmentRepository {
    val fragmentsPath = System.getProperty("fragments.path") ?: System.getenv("FRAGMENTS_PATH") ?: "./content"
    return FileSystemFragmentRepository(fragmentsPath) { fragment ->
        when (fragment.template) {
            "blog", "blog_post" -> {
                val date = fragment.date ?: return@FileSystemFragmentRepository "/${fragment.slug}"
                "/blog/${date.year}/${"%02d".format(date.monthValue)}/${fragment.slug}"
            }
            else -> "/page/${fragment.slug}"
        }
    }
}
```

- [ ] **Step 5: Update demo apps**

Update `demo-http4k/DemoApplication.kt` and any other demo apps similarly.

- [ ] **Step 6: Update ProjectGenerator scaffolding**

In `fragments-cli/src/main/kotlin/io/github/rygel/fragments/cli/ProjectGenerator.kt`, update all generated configuration templates to include the `urlBuilder` lambda.

- [ ] **Step 7: Run full test suite**

Run: `mvn test -T 4`
Expected: All tests pass. Existing tests may need updates if they assert on `fragment.url` or `resolvedUrl`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "fix: wire urlBuilder into adapter configurations for correct sitemap/RSS URLs

Blog posts now resolve to /blog/YYYY/MM/slug and static pages to
/page/slug, matching the actual adapter routes. This fixes sitemap,
llms.txt, RSS, and SEO canonical URLs.

Addresses #65 and #77."
```

---

## Task 5: Add tests for URL resolution end-to-end

**Files:**
- Create: `fragments-core/src/test/kotlin/io/github/rygel/fragments/UrlResolutionTest.kt`
- Modify: `fragments-sitemap-core/src/test/kotlin/io/github/rygel/fragments/sitemap/SitemapGeneratorTest.kt`

- [ ] **Step 1: Add test for date-based blog URL resolution**

```kotlin
package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UrlResolutionTest {
    @Test
    fun blogUrlBuilderGeneratesDateBasedPaths() {
        val urlBuilder: (Fragment) -> String = { fragment ->
            when (fragment.template) {
                "blog", "blog_post" -> {
                    val date = fragment.date ?: return@UrlResolutionTest "/${fragment.slug}"
                    "/blog/${date.year}/${"%02d".format(date.monthValue)}/${fragment.slug}"
                }
                else -> "/page/${fragment.slug}"
            }
        }
        val fragment = Fragment(
            title = "Test Post",
            slug = "hello-world",
            htmlContent = "<p>Content</p>",
            preview = "Preview",
            date = LocalDateTime.of(2026, 3, 15, 10, 0),
            publishDate = null,
            frontMatter = emptyMap(),
            template = "blog_post",
        )
        assertEquals("/blog/2026/03/hello-world", urlBuilder(fragment))
    }

    @Test
    fun staticPageUrlBuilderGeneratesPagePaths() {
        val urlBuilder: (Fragment) -> String = { fragment ->
            when (fragment.template) {
                "blog", "blog_post" -> {
                    val date = fragment.date ?: return@UrlResolutionTest "/${fragment.slug}"
                    "/blog/${date.year}/${"%02d".format(date.monthValue)}/${fragment.slug}"
                }
                else -> "/page/${fragment.slug}"
            }
        }
        val fragment = Fragment(
            title = "About",
            slug = "about",
            htmlContent = "<p>About page</p>",
            preview = "Preview",
            date = null,
            publishDate = null,
            frontMatter = emptyMap(),
            template = "static",
        )
        assertEquals("/page/about", urlBuilder(fragment))
    }
}
```

- [ ] **Step 2: Add sitemap test verifying date-based URLs appear**

In `SitemapGeneratorTest.kt`, add:
```kotlin
@Test
fun `sitemap uses resolvedUrl for blog post date-based paths`() =
    runBlocking {
        coEvery { repository.getAllVisible() } returns
            listOf(
                fragment("hello-world", "Hello World", url = "/blog/2026/03/hello-world"),
            )
        val generator = SitemapGenerator(repository, "https://example.com")

        val xml = generator.generateSitemap()
        assertValidXml(xml)

        assertTrue(
            xml.contains("https://example.com/blog/2026/03/hello-world"),
            "should contain date-based blog URL",
        )
    }
```

- [ ] **Step 3: Run tests**

Run: `mvn test -T 4`
Expected: All tests pass.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test: add URL resolution tests for blog and static page paths"
```

---

## Self-Review

### Spec Coverage

| Issue | Requirement | Task |
|-------|------------|------|
| #89 | Rename Fragment.content → htmlContent | Task 1, 2 |
| #89 | @Deprecated alias | Task 1 |
| #77 | Fix sitemap URLs for static pages | Task 4 (urlBuilder) |
| #77 | Fix SEO canonical URL | Task 3 |
| #65 | Blog posts get /blog/YYYY/MM/slug | Task 4 (urlBuilder) |
| #65 | Project pages get /page/slug | Task 4 (urlBuilder) |
| #65 | llms.txt URLs correct | Task 4 (urlBuilder, uses fragment.url) |
| #65 | RSS feed URLs correct | Task 4 (urlBuilder, uses fragment.url) |

### Placeholder Scan

No TBD, TODO, or placeholder patterns found.

### Type Consistency

- `htmlContent` used consistently across all tasks
- `urlBuilder` lambda type `(Fragment) -> String` matches constructor parameter in `FileSystemFragmentRepository`
- `resolvedUrl` field is `String?` — `urlBuilder` returns `String` (non-null), which is correct

### Notes

- The `FileSystemFragmentRepository.baseUrl` bug (not passed to Fragment constructor) is fixed as part of Task 2, Step 2. This is a separate but related fix.
- The `@Deprecated` extension property on `Fragment.content` provides source-level backward compatibility. Binary compatibility is maintained since the extension is an addition, not a removal of the constructor parameter (which is renamed, so this IS a binary-breaking change — acceptable for a 0.x version).
- Adapter configurations (Spring, Quarkus, Micronaut) all share the same `urlBuilder` pattern. The http4k and Javalin adapters receive a pre-built `FragmentsEngine` and don't create repositories themselves — the caller does.
