# Lazy Loading Implementation Plan

> **For agentic workers:** Inline execution — single session.

**Goal:** Add lazy image loading via HTML post-processing and HTMX-based lazy related posts loading.

**Architecture:** Template-engine-agnostic HTML post-processing in FragmentViewModel. HTMX added to all demo apps. Partial render support in templates. Related posts loaded via HTMX intersect trigger.

**Tech Stack:** Kotlin/JVM, HTMX 2.x, Thymeleaf, Pebble, Qute, Maven

---

## File Structure

### New files
- `fragments-core/src/main/kotlin/io/github/rygel/fragments/HtmlLazyLoader.kt` — HTML post-processing utility
- `fragments-core/src/test/kotlin/io/github/rygel/fragments/HtmlLazyLoaderTest.kt` — unit tests

### Modified files (core)
- `fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentViewModel.kt` — wire `HtmlLazyLoader` into `content` property
- `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt` — add `getRelatedPostsFragment()` method

### Modified files (demo apps — HTMX + partial render)
- `demo-spring-boot/src/main/resources/templates/layout.html`
- `demo-javalin/src/main/resources/templates/layout.pebble`
- `demo-quarkus/src/main/resources/templates/layout.qute.html`
- `demo-http4k/src/main/resources/templates/layout.pebble`
- `demo-micronaut/src/main/resources/templates/layout.html`
- All adapter controllers: add `/blog/{year}/{month}/{slug}/related` route

---

## Task 1: HtmlLazyLoader

**Files:**
- Create: `fragments-core/src/main/kotlin/io/github/rygel/fragments/HtmlLazyLoader.kt`
- Create: `fragments-core/src/test/kotlin/io/github/rygel/fragments/HtmlLazyLoaderTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HtmlLazyLoaderTest {
    @Test
    fun addsLazyLoadingToImgTags() {
        val html = "<p><img src=\"/photo.jpg\"></p>"
        val result = HtmlLazyLoader.addLazyLoading(html)
        assertEquals("<p><img src=\"/photo.jpg\" loading=\"lazy\" ></p>", result)
    }

    @Test
    fun doesNotDuplicateExistingLoadingAttribute() {
        val html = "<img src=\"/photo.jpg\" loading=\"eager\">"
        val result = HtmlLazyLoader.addLazyLoading(html)
        assertEquals(html, result)
    }

    @Test
    fun returnsEmptyStringForBlankInput() {
        assertEquals("", HtmlLazyLoader.addLazyLoading(""))
    }

    @Test
    fun returnsOriginalHtmlWithoutImages() {
        val html = "<p>No images here</p>"
        assertEquals(html, HtmlLazyLoader.addLazyLoading(html))
    }

    @Test
    fun handlesMultipleImgTags() {
        val html = "<img src=\"a.jpg\"><img src=\"b.jpg\">"
        val result = HtmlLazyLoader.addLazyLoading(html)
        assertEquals(2, result.split("loading=\"lazy\"").size - 1)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl fragments-core test -Dtest=HtmlLazyLoaderTest -T 4`
Expected: COMPILATION ERROR (HtmlLazyLoader not found)

- [ ] **Step 3: Write implementation**

```kotlin
package io.github.rygel.fragments

object HtmlLazyLoader {
    private val IMG_TAG = Regex("<img\\s", RegexOption.IGNORE_CASE)

    fun addLazyLoading(html: String): String {
        if (html.isBlank() || !html.contains("<img", ignoreCase = true)) return html
        return IMG_TAG.replace(html) { match ->
            val tag = match.value
            if (tag.contains("loading", ignoreCase = true)) tag
            else "$tag loading=\"lazy\" "
        }
    }
}
```

- [ ] **Step 4: Run tests**

Run: `mvn -pl fragments-core test -Dtest=HtmlLazyLoaderTest -T 4`
Expected: All tests PASS

- [ ] **Step 5: Wire into FragmentViewModel**

In `FragmentViewModel.kt`, replace:
```kotlin
val content: String
    get() = fragment.htmlContent
```
with:
```kotlin
val content: String
    get() = HtmlLazyLoader.addLazyLoading(fragment.htmlContent)
```

- [ ] **Step 6: Run existing FragmentViewModel tests**

Run: `mvn -pl fragments-core test -Dtest=FragmentViewModelTest -T 4`
Expected: All tests PASS

- [ ] **Step 7: Commit**

```bash
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/HtmlLazyLoader.kt
git add fragments-core/src/test/kotlin/io/github/rygel/fragments/HtmlLazyLoaderTest.kt
git add fragments-core/src/main/kotlin/io/github/rygel/fragments/FragmentViewModel.kt
git commit -m "feat: add lazy image loading via HtmlLazyLoader utility"
```

---

## Task 2: Related Posts Endpoint (FragmentsEngine)

**Files:**
- Modify: `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt`

- [ ] **Step 1: Add `getRelatedPostsFragment()` method**

Add after `getBlogPostWithRelationships`:
```kotlin
suspend fun getRelatedPostsFragment(year: String, month: String, slug: String): String? {
    val post = blogEngine.getPost(year, month, slug) ?: return null
    val allPosts = blogEngine.getAllPosts()
    val vm = FragmentViewModel(
        fragment = post,
        allFragments = allPosts,
        siteUrl = siteUrl,
    )
    val related = vm.relatedPosts
    if (related.isEmpty()) return null
    return buildString {
        appendLine("<h3>Related Posts</h3>")
        appendLine("<ul>")
        for (r in related) {
            appendLine("<li><a href=\"${r.url}\">${r.title}</a></li>")
        }
        appendLine("</ul>")
    }
}
```

- [ ] **Step 2: Run adapter-core tests**

Run: `mvn -pl fragments-adapter-core test -T 4`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt
git commit -m "feat: add getRelatedPostsFragment for HTMX lazy loading"
```

---

## Task 3: Spring Boot Demo — HTMX + Partial Render

**Files:**
- Modify: `demo-spring-boot/src/main/resources/templates/layout.html`
- Modify: `demo-spring-boot/src/main/resources/templates/blog_post.html`
- Modify: `fragments-spring-boot/src/main/kotlin/io/github/rygel/fragments/spring/FragmentsSpringController.kt`

- [ ] **Step 1: Add HTMX script and partial render to layout.html**

```html
<!DOCTYPE html>
<html th:unless="${isPartialRender}">
<head th:unless="${isPartialRender}">
    <meta charset="UTF-8">
    <title th:text="${viewModel.title}">Fragments4k</title>
    <script src="https://unpkg.com/htmx.org@2.0.4"></script>
</head>
<body>
<header th:unless="${isPartialRender}">
    <nav>
        <a href="/">Home</a>
        <a href="/blog">Blog</a>
    </nav>
</header>
<main th:fragment="content">
    <th:block th:replace="${templateName} :: content" />
</main>
<footer th:unless="${isPartialRender}">
    <p>Powered by Fragments4k</p>
</footer>
</body>
</html>
```

- [ ] **Step 2: Add related posts section to blog_post.html**

```html
<th:block th:fragment="content">
    <article>
        <h1 th:text="${viewModel.title}">Title</h1>
        <div th:utext="${viewModel.content}">Content</div>
    </article>
    <section id="related-posts"
             hx-trigger="intersect once"
             hx-get="/blog/__YEAR__/__MONTH__/${viewModel.slug}/related"
             hx-target="this"
             hx-swap="innerHTML">
        <div class="loading">Loading related posts...</div>
    </section>
</th:block>
```

Note: The `__YEAR__/__MONTH__` placeholders need to be actual year/month values. Since `FragmentViewModel` has `year` and `month` properties, the blog_post template should use those:
```html
<section id="related-posts"
         hx-trigger="intersect once"
         th:hx-get="'/blog/' + ${viewModel.year} + '/' + ${viewModel.month} + '/' + ${viewModel.slug} + '/related'"
         hx-target="this"
         hx-swap="innerHTML">
    <div class="loading">Loading related posts...</div>
</section>
```

- [ ] **Step 3: Add related posts route to Spring controller**

```kotlin
@GetMapping("/blog/{year}/{month}/{slug}/related")
suspend fun relatedPosts(
    @PathVariable year: String,
    @PathVariable month: String,
    @PathVariable slug: String,
): ResponseEntity<String> {
    val html = engine.getRelatedPostsFragment(year, month, slug) ?: return ResponseEntity.noContent().build()
    return ResponseEntity.ok().body(html)
}
```

- [ ] **Step 4: Commit**

```bash
git add demo-spring-boot/src/main/resources/templates/
git add fragments-spring-boot/src/main/kotlin/io/github/rygel/fragments/spring/FragmentsSpringController.kt
git commit -m "feat: HTMX lazy loading for Spring Boot demo"
```

---

## Task 4: Javalin Demo — HTMX + Partial Render

**Files:**
- Modify: `demo-javalin/src/main/resources/templates/layout.pebble`
- Modify: `demo-javalin/src/main/resources/templates/blog_post.pebble`
- Modify: `fragments-javalin/src/main/kotlin/io/github/rygel/fragments/javalin/FragmentsJavalinAdapter.kt`

- [ ] **Step 1: Update layout.pebble**

```twig
{% if not isPartialRender %}
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>{{ viewModel.title }}</title>
    <script src="https://unpkg.com/htmx.org@2.0.4"></script>
</head>
<body>
<header>
    <nav>
        <a href="/">Home</a>
        <a href="/blog">Blog</a>
    </nav>
</header>
{% endif %}
<main>{% block content %}{% endblock %}</main>
{% if not isPartialRender %}
<footer>
    <p>Powered by Fragments4k</p>
</footer>
</body>
</html>
{% endif %}
```

- [ ] **Step 2: Update blog_post.pebble with related posts section**

```twig
{% extends "layout.pebble" %}
{% block content %}
<article>
    <h1>{{ viewModel.title }}</h1>
    <div>{{ viewModel.content }}</div>
</article>
<section id="related-posts"
         hx-trigger="intersect once"
         hx-get="/blog/{{ viewModel.year }}/{{ viewModel.month }}/{{ viewModel.slug }}/related"
         hx-target="this"
         hx-swap="innerHTML">
    <div class="loading">Loading related posts...</div>
</section>
{% endblock %}
```

- [ ] **Step 3: Add route to Javalin adapter**

```kotlin
get("/blog/{year}/{month}/{slug}/related") { ctx ->
    ctx.handleAsync {
        val year = ctx.pathParam("year")
        val month = ctx.pathParam("month")
        val slug = ctx.pathParam("slug")
        val html = engine.getRelatedPostsFragment(year, month, slug)
        if (html != null) ctx.contentType("text/html").result(html)
        else ctx.status(204)
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add demo-javalin/src/main/resources/templates/
git add fragments-javalin/src/main/kotlin/io/github/rygel/fragments/javalin/FragmentsJavalinAdapter.kt
git commit -m "feat: HTMX lazy loading for Javalin demo"
```

---

## Task 5: http4k Demo — HTMX + Partial Render

**Files:**
- Modify: `demo-http4k/src/main/resources/templates/layout.pebble`
- Modify: `demo-http4k/src/main/resources/templates/blog_post.pebble`
- Modify: `fragments-http4k/src/main/kotlin/io/github/rygel/fragments/http4k/FragmentsHttp4kAdapter.kt`

Follow same pattern as Javalin (Pebble templates). Add route:
```kotlin
"/blog/{year}/{month}/{slug}/related" bindMethod GET to { request ->
    val year = request.path("year") ?: return@to Response(Status.BAD_REQUEST)
    val month = request.path("month") ?: return@to Response(Status.BAD_REQUEST)
    val slug = request.path("slug") ?: return@to Response(Status.BAD_REQUEST)
    val html = runBlocking { engine.getRelatedPostsFragment(year, month, slug) }
    html?.let { Response(Status.OK).body(it) } ?: Response(Status.NO_CONTENT)
}
```

---

## Task 6: Quarkus Demo — HTMX + Partial Render

**Files:**
- Modify: `demo-quarkus/src/main/resources/templates/layout.qute.html`
- Modify: `demo-quarkus/src/main/resources/templates/blog_post.qute.html`
- Modify: `fragments-quarkus/src/main/kotlin/io/github/rygel/fragments/quarkus/FragmentsQuarkusResource.kt`

Follow same pattern with Qute templates. Add route:
```kotlin
@GET
@Path("/blog/{year}/{month}/{slug}/related")
suspend fun relatedPosts(@PathParam("year") year: String, @PathParam("month") month: String, @PathParam("slug") slug: String): Response {
    val html = engine.getRelatedPostsFragment(year, month, slug)
    return if (html != null) Response.ok(html).build() else Response.noContent().build()
}
```

---

## Task 7: Micronaut Demo — HTMX + Partial Render

**Files:**
- Modify: `demo-micronaut/src/main/resources/templates/layout.html`
- Modify: `demo-micronaut/src/main/resources/templates/blog_post.html`
- Modify: `fragments-micronaut/src/main/kotlin/io/github/rygel/fragments/micronaut/FragmentsMicronautController.kt`

Follow same pattern as Spring Boot (Thymeleaf templates). Add route:
```kotlin
@Get("/blog/{year}/{month}/{slug}/related")
suspend fun relatedPosts(@PathVariable year: String, @PathVariable month: String, @PathVariable slug: String): HttpResponse<String> {
    val html = engine.getRelatedPostsFragment(year, month, slug)
    return if (html != null) HttpResponse.ok(html) else HttpResponse.noContent()
}
```

---

## Task 8: Update TODO.md

- [ ] **Mark Lazy Loading Optimization as complete**

---

## Self-Review Check

1. **Spec coverage:** Covers all spec items: HtmlLazyLoader (Task 1), related posts endpoint (Task 2), HTMX script + partial render (Tasks 3-7), all 5 demo apps covered.
2. **Placeholder scan:** No TBD/TODO placeholders.
3. **Type consistency:** `getRelatedPostsFragment` returns `String?`, consistent across all adapters.
4. **No gaps:** All spec requirements mapped to tasks.
