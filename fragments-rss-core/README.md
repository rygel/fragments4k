# Fragments RSS Core

RSS 2.0 feed generation from fragment content.

## Usage

```kotlin
val rssGenerator = RssGenerator(repository)

val feedXml: String = rssGenerator.generateFeed(
    siteTitle = "My Blog",
    siteDescription = "A blog about Kotlin",
    siteUrl = "https://example.com",
    feedUrl = "https://example.com/rss.xml"
)
```

The generated feed includes all visible fragments with absolute URLs, categories, tags, and proper XML escaping. All framework adapters automatically serve this at `/rss.xml`.
