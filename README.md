# Fragments4k

A framework-agnostic Markdown-based content library for Kotlin/JVM with adapters for HTTP4k, Javalin, Spring Boot, Quarkus, and Micronaut.

## Overview

Fragments4k turns a directory of Markdown files with YAML front matter into a fully-featured blog or content site. It handles parsing, routing, pagination, search, RSS, sitemaps, SEO metadata, and more — you bring the framework and templates.

## Features

### Content
- Markdown content with YAML front matter
- Static pages and blog posts with pagination
- Tag, category, and date-based filtering
- Full-text search with autocomplete (Lucene)
- RSS/Atom feed generation
- XML sitemap generation with image support
- Content relationships (prev/next, related, translations)
- Publication lifecycle (draft, review, scheduled, published, archived, expired)
- Content revisions and audit trail

### SEO
- `SeoMetadata` with Open Graph, Twitter Card, and JSON-LD generation
- Canonical URL support on `FragmentViewModel`
- `/robots.txt` route on all adapters
- Per-page `image` field for `og:image` / `twitter:image`
- Per-page `robots` meta tag control

### Developer Experience
- HTMX support for partial rendering
- Live reload in development
- CLI tool for project scaffolding
- Typed front matter accessors (`getString`, `getInt`, `getBoolean`, etc.)
- Coroutines-based async operations throughout
- Caching layer with automatic invalidation
- Image optimization and responsive variants

### Framework Adapters

| Adapter | Template Engine | Notes |
|---------|----------------|-------|
| [HTTP4k](fragments-http4k/) | Any `(Any) -> String` | Template-engine agnostic |
| [Javalin](fragments-javalin/) | Any via `TemplateRenderer` | Javalin 7+ |
| [Spring Boot](fragments-spring-boot/) | Thymeleaf (default) | `@Controller` with suspend functions |
| [Quarkus](fragments-quarkus/) | Qute (default) | JAX-RS resource |
| [Micronaut](fragments-micronaut/) | Any via Micronaut Views | `@Controller` with suspend functions |

## Quick Start

### 1. Add dependency

```xml
<dependency>
    <groupId>io.github.rygel</groupId>
    <artifactId>fragments-http4k</artifactId> <!-- or fragments-javalin, fragments-spring-boot, etc. -->
    <version>0.6.2</version>
</dependency>
```

### 2. Create content

```
content/
├── about.md
└── blog/
    ├── 2026-01-15-hello-world.md
    └── 2026-03-20-second-post.md
```

### 3. Write front matter

```yaml
---
title: Hello World
date: 2026-01-15
tags: [kotlin, jvm]
categories: [tutorials]
author: Jane Doe
image: /static/images/hello-cover.jpg
template: blog_post
---

Your Markdown content here.

<!--more-->

Content after the fold (preview stops at the <!--more--> tag).
```

### 4. Wire up the adapter

```kotlin
val repository = FileSystemFragmentRepository("./content")
val staticEngine = StaticPageEngine(repository)
val blogEngine = BlogEngine(repository)

// HTTP4k example
val adapter = FragmentsHttp4kAdapter(
    staticEngine = staticEngine,
    blogEngine = blogEngine,
    renderer = myTemplateRenderer,
    searchEngine = LuceneSearchEngine(repository),
    siteUrl = "https://example.com"
)
adapter.createRoutes().asServer(Netty(8080)).start()
```

See the [framework adapter READMEs](#modules) for framework-specific setup.

## Front Matter Reference

All available front matter fields:

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `title` | String | filename | Page title |
| `slug` | String | from filename | URL-safe identifier |
| `date` | Date | — | Authoring date (`yyyy-MM-dd` or `yyyy-MM-dd'T'HH:mm`) |
| `publishDate` | Date | — | Scheduled publication date |
| `expiryDate` | Date | — | Auto-expire after this date |
| `status` | String | `PUBLISHED` | Lifecycle state (see below) |
| `visible` | Boolean | `true` | Include in visible listings |
| `template` | String | `default` | Template identifier |
| `tags` | List | `[]` | Tag slugs |
| `categories` | List | `[]` | Category slugs |
| `author` | String | — | Author name or ID |
| `authorIds` | List | `[]` | Multiple author IDs |
| `image` | String | — | Cover image path (e.g. `/static/images/cover.jpg`) |
| `preview` | String | auto-extracted | Custom preview HTML |
| `order` | Int | `0` | Sort key for manual ordering |
| `language` | String | `en` | BCP-47 language tag |
| `languages` | Map | `{}` | Translations (`{de: "hallo-welt", fr: "bonjour-monde"}`) |
| `series` | String | — | Series slug |
| `seriesPart` | Int | — | Position in series (1-based) |
| `seriesTitle` | String | — | Display title for this part |

Any additional fields are preserved in `Fragment.frontMatter` and accessible via [typed accessors](#typed-front-matter-accessors).

## Content Lifecycle

Fragments follow a publication lifecycle with guarded transitions:

```
DRAFT → REVIEW → APPROVED → PUBLISHED
                     ↓
                 SCHEDULED → PUBLISHED (automatic at publishDate)
                                ↓
                            ARCHIVED
                            EXPIRED (automatic at expiryDate)
```

Manage status programmatically:

```kotlin
repository.updateFragmentStatus("my-post", FragmentStatus.PUBLISHED)
repository.scheduleMultiple(listOf("post-1"), publishDate = myDateTime)
repository.archiveMultiple(listOf("old-post"))
```

## URL Builder

By default, fragment URLs are `/<slug>`. Use `urlBuilder` for custom URL schemes:

```kotlin
// Date-based blog URLs: /blog/2026/03/hello-world
val blogRepo = FileSystemFragmentRepository(
    basePath = "./content/blog",
    urlBuilder = { fragment ->
        val date = fragment.date ?: return@FileSystemFragmentRepository "/${fragment.slug}"
        "/blog/${date.year}/%02d/${fragment.slug}".format(date.monthValue)
    }
)

// Prefix-based URLs: /projects/my-project
val projectRepo = FileSystemFragmentRepository(
    basePath = "./content/projects",
    baseUrl = "/projects"
)
```

## Typed Front Matter Accessors

Access custom front matter fields safely without unchecked casts:

```kotlin
// Instead of: fragment.frontMatter["githubRepo"] as? String
fragment.getString("githubRepo")        // String?
fragment.getBoolean("featured")         // Boolean?
fragment.getInt("priority")             // Int?
fragment.getLong("viewCount")           // Long?
fragment.getDouble("rating")            // Double?
fragment.getStringList("customTags")    // List<String>
```

These handle SnakeYAML type coercion automatically (e.g. `Number.toInt()` for any numeric type).

## SEO

### SeoMetadata

Generate complete SEO tags from a fragment:

```kotlin
val seo = SeoMetadata.fromFragment(
    fragment = fragment,
    siteUrl = "https://example.com",
    siteName = "My Site",
    pagePath = "blog/2026/03/hello-world",
    ogType = "article"  // or "website" for static pages
)

// In your template:
seo.generateAllMetaTags()  // outputs <meta>, <link rel="canonical">, OG tags, Twitter Card, JSON-LD
```

When `author` is not passed, it defaults from `fragment.author`. When `imageUrl` is not passed, it defaults from `fragment.image`.

### Canonical URL on FragmentViewModel

```kotlin
val vm = FragmentViewModel(fragment = fragment, siteUrl = "https://example.com")

// In templates:
// <link rel="canonical" href="${vm.canonicalUrl}">
// <meta property="og:url" content="${vm.canonicalUrl}">
```

### robots.txt

All adapters automatically serve `/robots.txt`:

```
User-agent: *
Allow: /

Sitemap: https://example.com/sitemap.xml
```

## Search

Full-text search powered by Apache Lucene:

```kotlin
val searchEngine = LuceneSearchEngine(repository)
searchEngine.index()  // build the index

// Simple search
val results = searchEngine.search("kotlin coroutines", maxResults = 20)

// Advanced search
val results = searchEngine.search(SearchOptions(
    query = "kotlin",
    phraseSearch = true,   // exact phrase matching
    fuzzySearch = false,
    maxResults = 10
))

// Autocomplete suggestions
val suggestions = searchEngine.autocomplete("kot", limit = 5)

// Search by tag or category
val kotlinPosts = searchEngine.searchByTag("kotlin")
```

## Template Integration

`FragmentViewModel` is passed to your templates with these properties:

```kotlin
vm.title              // page title
vm.content            // rendered HTML
vm.preview            // preview HTML (up to <!--more--> tag)
vm.slug               // URL slug
vm.url                // relative URL path
vm.canonicalUrl       // absolute URL (when siteUrl is set)
vm.date               // authoring date
vm.tags               // tag list
vm.categories         // category list
vm.author             // author name
vm.template           // template identifier
vm.readingTime        // ReadingTime(minutes, seconds, text)
vm.formattedReadingTime  // e.g. "3m 12s read"
vm.fragment.image     // cover image path
vm.fragment.frontMatter  // raw front matter map

// Relationships (when loaded)
vm.previousPost       // previous post in chronological order
vm.nextPost           // next post
vm.relationshipRelatedPosts  // related posts by tags/categories
vm.translations       // language → fragment map
```

### Example: JTE template

```html
@param vm: FragmentViewModel

<article>
    <h1>${vm.title}</h1>
    <time>${vm.date}</time>
    <span>${vm.formattedReadingTime}</span>
    
    @if(vm.fragment.image != null)
        <img src="${vm.fragment.image}" alt="${vm.title}">
    @endif
    
    $unsafe{vm.content}
    
    @if(vm.hasPreviousPost)
        <a href="${vm.previousPost.url}">Previous</a>
    @endif
    @if(vm.hasNextPost)
        <a href="${vm.nextPost.url}">Next</a>
    @endif
</article>
```

## Modules

| Module | Description |
|--------|-------------|
| [fragments-core](fragments-core/) | Domain model, Markdown parsing, repository |
| [fragments-blog-core](fragments-blog-core/) | Blog engine with pagination and filtering |
| [fragments-static-core](fragments-static-core/) | Static page engine |
| [fragments-seo-core](fragments-seo-core/) | SEO metadata, Open Graph, JSON-LD |
| [fragments-lucene-core](fragments-lucene-core/) | Full-text search and autocomplete |
| [fragments-rss-core](fragments-rss-core/) | RSS 2.0 feed generation |
| [fragments-sitemap-core](fragments-sitemap-core/) | XML sitemap generation |
| [fragments-cache-core](fragments-cache-core/) | Caching decorator for repositories |
| [fragments-navigation-core](fragments-navigation-core/) | Menu and archive navigation generators |
| [fragments-chat-core](fragments-chat-core/) | Chat/conversation Markdown extension |
| [fragments-social-core](fragments-social-core/) | Social media share link generation |
| [fragments-image-optimization-core](fragments-image-optimization-core/) | Image resizing and optimization |
| [fragments-live-reload](fragments-live-reload/) | File-watching live reload for development |
| [fragments-cli](fragments-cli/) | CLI tool for scaffolding and dev server |
| [fragments-http4k](fragments-http4k/) | HTTP4k framework adapter |
| [fragments-javalin](fragments-javalin/) | Javalin framework adapter |
| [fragments-spring-boot](fragments-spring-boot/) | Spring Boot framework adapter |
| [fragments-quarkus](fragments-quarkus/) | Quarkus framework adapter |
| [fragments-micronaut](fragments-micronaut/) | Micronaut framework adapter |
| [fragments-bom](fragments-bom/) | Bill of Materials for dependency management |

## CLI Tool

```bash
# Scaffold a new project
java -jar fragments-cli.jar init my-blog --framework=http4k

# Available frameworks: http4k, javalin, spring-boot, quarkus, micronaut

# Run dev server with live reload
java -jar fragments-cli.jar run --watch --content-dir=./content --port=8080
```

## Building

```bash
./mvnw verify -T 4
```

## License

[Apache License 2.0](LICENSE)
