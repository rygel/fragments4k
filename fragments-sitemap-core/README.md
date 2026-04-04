# Fragments Sitemap Core

XML sitemap generation with image support.

## Usage

```kotlin
val sitemapGenerator = SitemapGenerator(
    repository = repository,
    siteUrl = "https://example.com",
    lastModified = null  // or a specific LocalDateTime
)

val sitemapXml: String = sitemapGenerator.generateSitemap()
```

The generated sitemap uses fully qualified absolute URLs as required by the spec. Image URLs are automatically included from front matter fields (`image`, `og:image`, `twitter:image`) and extracted from HTML content.

All framework adapters automatically serve this at `/sitemap.xml`.
