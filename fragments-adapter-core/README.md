# Fragments Adapter Core

Shared routing engine that all framework adapters delegate to, eliminating duplication across http4k, Javalin, Spring Boot, Quarkus, and Micronaut adapters.

## Usage

This module is a transitive dependency of every adapter — you don't add it directly. It is used by adapter implementations internally.

## Key Classes

### FragmentsEngine

Central facade that accepts a `StaticPageEngine`, `BlogEngine`, optional `LuceneSearchEngine`, plus config (site title, URL, author repo, navigation, footer). Exposes methods for every operation an adapter needs:

```kotlin
val engine = FragmentsEngine(
    staticEngine = staticEngine,
    blogEngine = blogEngine,
    searchEngine = searchEngine,
    siteTitle = "My Blog",
    siteUrl = "https://example.com"
)

engine.getHome()
engine.getBlogPost(year, month, slug)
engine.search("kotlin coroutines")
engine.generateRssFeed()
engine.generateSitemap()
engine.generateRobotsTxt()
engine.generateLlmsTxt()
```

### PaginationGenerator

```kotlin
val info: PaginationInfo = PaginationGenerator.generateSimpleControls(
    currentPage = 2,
    totalPages = 10,
    basePath = "/blog"
)
// info.currentPage, info.totalPages, info.hasPrevious, info.hasNext, info.text
```

### SearchFormGenerator

```kotlin
val config: SearchFormConfig = SearchFormGenerator.create()
// config.actionUrl, config.paramName, config.placeholderText, config.buttonText
```

### FooterGenerator

```kotlin
val footer: FooterConfig = FooterGenerator.create()
// footer.copyrightText, footer.year, footer.poweredByName, footer.poweredByUrl
```
