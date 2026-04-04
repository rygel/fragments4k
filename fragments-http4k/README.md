# Fragments HTTP4k Adapter

HTTP4k adapter for the Fragments markdown-based blog and static site library.

## Features

- Full routing support with `RoutingHttpHandler`
- HTMX support for partial rendering
- Template-engine agnostic — uses `TemplateRenderer` (`(Any) -> String`)
- `/robots.txt`, `/sitemap.xml`, `/rss.xml` built-in
- Complete route coverage: home, pages, blog, tags, categories, archive, search

## Usage

Add dependency:

```xml
<dependency>
    <groupId>io.github.rygel</groupId>
    <artifactId>fragments-http4k</artifactId>
    <version>0.6.2</version>
</dependency>
```

The adapter does **not** include a template engine. Add your preferred engine separately:

```xml
<!-- Pebble -->
<dependency>
    <groupId>org.http4k</groupId>
    <artifactId>http4k-template-pebble</artifactId>
</dependency>

<!-- Or JTE, Handlebars, etc. — anything that produces (Any) -> String -->
```

Setup:

```kotlin
val repository = FileSystemFragmentRepository("./content")
val staticEngine = StaticPageEngine(repository)
val blogEngine = BlogEngine(repository)
val searchEngine = LuceneSearchEngine(repository)
searchEngine.index()

val adapter = FragmentsHttp4kAdapter(
    staticEngine = staticEngine,
    blogEngine = blogEngine,
    renderer = PebbleTemplates().HotReload("src/main/resources/templates"),
    searchEngine = searchEngine,
    siteTitle = "My Blog",
    siteDescription = "A blog powered by Fragments4k",
    siteUrl = "https://example.com"
)

adapter.createRoutes().asServer(Netty(8080)).start()
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
| `/blog/archive/{year}` | GET | Archive by year |
| `/blog/archive/{year}/{month}` | GET | Archive by year/month |
| `/search?q=` | GET | Full-text search |
| `/rss.xml` | GET | RSS feed |
| `/sitemap.xml` | GET | XML sitemap |
| `/robots.txt` | GET | Robots directives |

## HTMX Support

Routes detect HTMX requests via the `HX-Request` header and set `isPartialRender = true` on the view model for partial content rendering.

## Testing

Integration tests in `FragmentsHttp4kAdapterTest`.
