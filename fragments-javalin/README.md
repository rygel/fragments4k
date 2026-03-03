# Fragments Javalin Adapter

Javalin adapter for the Fragments markdown-based blog and static site library.

## Features

- ✅ Full routing support
- ✅ HTMX support for partial rendering
- ✅ Complete route coverage:
  - Home page
  - Static pages
  - Blog overview with pagination
  - Blog posts by date
  - Tag filtering
  - Category filtering
- ✅ RSS feed generation
- ✅ Kotlin coroutines with suspend functions

## Usage

Add dependency:

```xml
<dependency>
    <groupId>io.andromeda</groupId>
    <artifactId>fragments-javalin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Setup your Javalin app:

```kotlin
val repository = FileSystemFragmentRepository("./content")
val staticEngine = StaticPageEngine(repository)
val blogEngine = BlogEngine(repository)

val app = Javalin.create {
    it.fileRenderer(JavalinPebble().prependTemplateLocation("templates"))
}

app.fragmentsRoutes(staticEngine, blogEngine)
```

Customize RSS feed:

```kotlin
app.fragmentsRoutes(
    staticEngine = staticEngine,
    blogEngine = blogEngine,
    renderer = renderer,
    siteTitle = "My Blog",
    siteDescription = "My Awesome Blog",
    siteUrl = "https://example.com"
)
```

## Routes

| Route | Method | Description |
|-------|--------|-------------|
| `/` | GET | Home page |
| `/page/{slug}` | GET | Static page |
| `/blog` | GET | Blog overview |
| `/blog/page/{page}` | GET | Blog with pagination |
| `/blog/{year}/{month}/{slug}` | GET | Blog post |
| `/blog/tag/{tag}` | GET | Posts by tag |
| `/blog/category/{category}` | GET | Posts by category |
| `/rss.xml` | GET | RSS feed |

## HTMX Support

Routes detect HTMX requests via `HX-Request` header and render partial content accordingly.

## Testing

Integration tests included in `FragmentsJavalinAdapterTest`.
