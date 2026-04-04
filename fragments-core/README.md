# Fragments Core

Domain model, Markdown parsing, and file-system repository for fragments4k.

## Key Classes

### Fragment

The core domain model representing a single piece of content. Loaded from a Markdown file with YAML front matter.

```kotlin
val fragment: Fragment

fragment.title           // page title
fragment.slug            // URL-safe identifier
fragment.url             // canonical relative URL
fragment.content         // rendered HTML
fragment.preview         // HTML excerpt (up to <!--more--> tag)
fragment.previewTextOnly // plain-text excerpt
fragment.date            // authoring date (LocalDateTime)
fragment.status          // publication lifecycle state
fragment.tags            // tag list
fragment.categories      // category list
fragment.author          // author name
fragment.image           // cover image path from front matter
fragment.frontMatter     // raw YAML map for custom fields
```

See the [front matter reference](../README.md#front-matter-reference) for all available fields.

### Typed Front Matter Accessors

Extension functions for safe access to custom front matter fields:

```kotlin
fragment.getString("githubRepo")      // String?
fragment.getBoolean("featured")       // Boolean?
fragment.getInt("priority")           // Int?
fragment.getLong("viewCount")         // Long?
fragment.getDouble("rating")          // Double?
fragment.getStringList("customTags")  // List<String>
```

### FragmentViewModel

Presentation model wrapping a `Fragment` with computed properties for templates:

```kotlin
val vm = FragmentViewModel(
    fragment = fragment,
    siteUrl = "https://example.com",   // enables canonicalUrl
    relationships = relationships       // enables prev/next/related
)

vm.canonicalUrl       // "https://example.com/blog/2026/03/hello-world"
vm.readingTime        // ReadingTime(minutes, seconds, text)
vm.formattedReadingTime  // "3m 12s read"
vm.previousPost       // previous fragment (when relationships loaded)
vm.nextPost           // next fragment
```

### FileSystemFragmentRepository

Reads Markdown files from a directory, parses front matter, and serves fragments:

```kotlin
// Simple usage
val repo = FileSystemFragmentRepository("./content")

// With URL builder for date-based paths
val blogRepo = FileSystemFragmentRepository(
    basePath = "./content/blog",
    urlBuilder = { fragment ->
        val d = fragment.date ?: return@FileSystemFragmentRepository "/${fragment.slug}"
        "/blog/${d.year}/%02d/${fragment.slug}".format(d.monthValue)
    }
)

// With base URL prefix
val projectRepo = FileSystemFragmentRepository(
    basePath = "./content/projects",
    baseUrl = "/projects"
)
```

### FragmentStatus

Publication lifecycle: `DRAFT` → `REVIEW` → `APPROVED` → `PUBLISHED` → `ARCHIVED` / `EXPIRED`.

Includes `SCHEDULED` for automatic publication at a future date. Transitions are guarded — use `force = true` to bypass.

### MarkdownParser

Parses Markdown with YAML front matter using flexmark. Supports tables, strikethrough, task lists, auto-links, and footnotes. Extensible via `extraExtensions` parameter (e.g. `ChatExtension`).
