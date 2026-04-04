# Fragments Static Core

Static page engine for non-blog content (about pages, landing pages, etc.).

## Usage

```kotlin
val staticEngine = StaticPageEngine(repository)

// Get a single page by slug
val aboutPage = staticEngine.getPage("about")

// Get all visible static pages
val pages = staticEngine.getAllStaticPages()

// Access the underlying repository
val repo = staticEngine.getRepository()
```

Static pages are fragments that are not blog posts — typically pages like "About", "Contact", or custom landing pages.
