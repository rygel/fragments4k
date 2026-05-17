# Lazy Loading Optimization Design

**Date:** 2026-05-17
**Status:** Draft
**Scope:** Lazy loading images via HTML post-processing + HTMX-based content loading for related posts

## Overview

Add lazy loading across all framework adapters at two levels:
1. **HTML post-processing** — `loading="lazy"` added to `<img>` tags in `FragmentViewModel.content`
2. **HTMX lazy content loading** — related posts loaded on scroll intersect via HTMX, partial render support

## Section 1: HTML Image Lazy Loading

### Problem

Markdown content is rendered to HTML with plain `<img src="...">` tags. No `loading="lazy"` attribute. Browser loads all images on page load regardless of viewport.

### Solution

Add `HtmlLazyLoader` utility in `fragments-core`:

```kotlin
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

### Integration

`FragmentViewModel.content` getter delegates to `HtmlLazyLoader.addLazyLoading(fragment.htmlContent)`. Since `content` is a computed getter (not cached), the lazy loading is applied on every access — zero overhead for partial renders.

```kotlin
val content: String
    get() = HtmlLazyLoader.addLazyLoading(fragment.htmlContent)
```

### Acceptance

- Every `<img>` tag in rendered blog content has `loading="lazy"`
- Tags that already have `loading` attribute are not modified
- No template changes needed — works across all adapters

## Section 2: HTMX Integration

### Problem

HTMX headers are read and `isPartialRender` is set on every request, but no template uses it. Full HTML pages are always rendered, causing HTMX to extract content via DOM parsing. Related posts (`relatedPosts` in FragmentViewModel) are computed eagerly when the property is accessed, even if the user never scrolls to the bottom.

### Solution

Three sub-tasks:

**A) Add HTMX script** — Single `<script src="https://unpkg.com/htmx.org@2.0.4"></script>` tag in each demo app's base layout template:
- `demo-spring-boot/src/main/resources/templates/layout.html`
- `demo-javalin/src/main/resources/templates/layout.pebble`
- `demo-quarkus/src/main/resources/templates/layout.qute.html`
- `demo-http4k/src/main/resources/templates/layout.pebble`
- `demo-micronaut/src/main/resources/templates/layout.html`

**B) Partial render templates** — Wrap full-page chrome in `if !isPartialRender` blocks:

Thymeleaf (`layout.html`):
```html
<!DOCTYPE html>
<html th:unless="${isPartialRender}">
  <head th:unless="${isPartialRender}">
    ...
  </head>
  <body>
    <header th:unless="${isPartialRender}">...</header>
    <main th:fragment="content">...</main>
    <footer th:unless="${isPartialRender}">...</footer>
  </body>
</html>
```

Pebble (`layout.pebble`):
```twig
{% if not isPartialRender %}
<!DOCTYPE html>
<html>
<head>...</head>
<body>
<header>...</header>
{% endif %}
<main>{% block content %}{% endblock %}</main>
{% if not isPartialRender %}
<footer>...</footer>
</body>
</html>
{% endif %}
```

Qute (`layout.qute.html`):
```html
{#if !isPartialRender}
<!DOCTYPE html>
<html>
<head>...</head>
<body>
<header>...</header>
{/if}
<main>{#insert content}{/insert}</main>
{#if !isPartialRender}
<footer>...</footer>
</body>
</html>
{/if}
```

**C) Related posts endpoint** — Add `GET /blog/{year}/{month}/{slug}/related` to all adapter controllers. Delegates to `FragmentsEngine.getRelatedPostsFragment(year, month, slug): String` which:
1. Fetches the post via `blogEngine.getPost(year, month, slug)`
2. Resolves `relatedPosts` (lazy, computes once)
3. Renders a minimal HTML snippet of related post links

The blog post template gets an HTMX trigger:
```html
<section id="related-posts"
  hx-trigger="intersect once"
  hx-get="/blog/2026/03/hello-world/related"
  hx-target="this"
  hx-swap="innerHTML">
  <div class="loading">Loading related posts...</div>
</section>
```

### FragmentsEngine additions

```kotlin
suspend fun getRelatedPostsFragment(year: String, month: String, slug: String): String? {
    val post = blogEngine.getPost(year, month, slug) ?: return null
    val vm = FragmentViewModel(
        fragment = post,
        allFragments = blogEngine.getAllPosts(),
        siteUrl = siteUrl,
    )
    // Access triggers lazy computation
    val related = vm.relatedPosts
    if (related.isEmpty()) return null
    return buildString {
        appendLine("<h3>Related Posts</h3><ul>")
        for (r in related) {
            appendLine("<li><a href=\"${r.url}\">${r.title}</a></li>")
        }
        appendLine("</ul>")
    }
}
```

### Acceptance

- HTMX script loaded on all pages
- HTMX requests only receive content fragment (no HTML/HEAD/BODY wrapper)
- Related posts section loads via HTMX intersect trigger
- No related posts API call occurs until the user scrolls near the bottom

## Out of Scope

- Responsive `<picture>` with `srcset` integration (requires connecting image optimizer to rendering pipeline)
- Lazy loading below-the-fold non-image content beyond related posts
- IntersectionObserver for anything beyond HTMX's built-in `intersect` trigger
- Client-side JS bundle or build tooling
- Progressive image loading (blur-up, LQIP)
