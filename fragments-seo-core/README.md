# Fragments SEO Core

SEO metadata generation: Open Graph, Twitter Cards, JSON-LD structured data, canonical URLs, and robots directives.

## Usage

### From a Fragment

```kotlin
val seo = SeoMetadata.fromFragment(
    fragment = fragment,
    siteUrl = "https://example.com",
    siteName = "My Site",
    pagePath = "blog/2026/03/hello-world",
    ogType = "article",    // "website" for static pages
    // author defaults from fragment.author when not passed
    // imageUrl defaults from fragment.image when not passed
)
```

### For a Custom Page

```kotlin
val seo = SeoMetadata.forPage(
    title = "Search Results",
    description = "Search our articles",
    siteUrl = "https://example.com",
    pagePath = "search",
    siteName = "My Site"
)
```

### Generate Meta Tags

```kotlin
// All tags at once (standard + OG + Twitter + JSON-LD)
seo.generateAllMetaTags()

// Or individually
seo.generateStandardMetaTags()   // <meta name="description">, <link rel="canonical">, robots
seo.generateOpenGraphTags()       // og:title, og:description, og:url, og:image, og:type, etc.
seo.generateTwitterCardTags()     // twitter:card, twitter:title, twitter:image
seo.generateJsonLd()              // <script type="application/ld+json">
```

### Robots Control

```kotlin
// Default: "index, follow"
val seo = SeoMetadata(
    title = "Search",
    description = "...",
    canonicalUrl = "...",
    robots = "noindex, nofollow"  // exclude from search engines
)
```
