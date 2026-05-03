# Changelog

## [0.6.5] - 2026-05-03

### Added

- **`ClasspathFragmentRepository`** — read-only repository that loads Markdown content from the Java classpath (JAR-bundled), enabling self-contained deployment without an external filesystem. Indexed via `index.list`. (36 end-to-end tests)
- **`fragments-adapter-core` module** — new shared engine (`FragmentsEngine`) that encapsulates all business logic every web framework adapter needs (pages, blog, search, RSS, sitemap, robots.txt, llms.txt, pagination, navigation, footer, SEO). Eliminates duplication across http4k, Javalin, Spring Boot, Quarkus, and Micronaut adapters.
- **`fragments-architecture-tests` module** — ArchUnit-based test suite enforcing structural integrity: layering rules, naming conventions, module dependency isolation, no legacy API usage, no `System.out` logging, and no test dependency leaks into production code.
- **`RssGenerator` tests** — 15 new tests covering the rewritten RSS generator.
- **`SitemapGenerator` tests** — 237 lines of new tests for the sitemap generator.
- Snapshot repository configuration for GitHub Packages.
- Apache 2.0 LICENSE bundled in all module JARs via `META-INF`.

### Changed

- **All five framework adapters** (http4k, Javalin, Spring Boot, Quarkus, Micronaut) now delegate to `FragmentsEngine` from `fragments-adapter-core` instead of duplicating routing logic.
- **`RssGenerator` rewritten with `XMLStreamWriter`** — ensures correct XML escaping and well-formed output. Now accepts `List<FragmentRepository>` for multi-repo aggregation.
- **`SitemapService` removed** — sitemap generation consolidated into `SitemapGenerator` directly.
- Adapters now use the actual configured `siteUrl` for sitemap and feed generation, and support `additionalRepositories` for multi-repo setups.
- Sitemap, RSS, and llms.txt now include all fragments across all configured repositories.
- Sitemap generator produces spec-compliant XML with proper escaping.
- **Quality tooling activated**: detekt static analysis, ArchUnit architecture tests, ktlint formatting checks, and JaCoCo code coverage. All existing warnings fixed.
- Micronaut updated 4.5.1 → 4.10.11, http4k updated 6.39.1.0 → 6.40.0.0.
- **Dependency updates**: Caffeine 3.2.3 → 3.2.4, ArchUnit 1.4.1 → 1.4.2, JTE 3.2.3 → 3.2.4.
- **Plugin updates**: ktlint-maven-plugin 3.5.0 → 3.7.1, maven-source-plugin 3.3.1 → 3.4.0.

### Fixed

- Adapters correctly propagate `siteUrl` for absolute URL generation in sitemaps and feeds.
- Publish workflow `server-id` aligned with `distributionManagement` configuration.
- Flaky `RandomDataGeneratorTest.generateRandomDate` test fixed (off-by-one in date range).
- ktlint 3.7.1 `when`-entry formatting violations resolved across all modules.

### Removed

- `SitemapService` class (logic merged into `SitemapGenerator`).
- Duplicated adapter code (pagination, search form, footer generators) extracted into `fragments-adapter-core`.

---

## [0.6.4] - 2025-04-20

### Added

- `llms.txt` route for LLM-friendly content discovery.
- FAQ schema support (`FaqSchemaGenerator`).
- Breadcrumb JSON-LD generation.
- Per-page `robots` meta tag control.
- Author support and Person schema generation.
- Typed front matter accessors (`getString`, `getInt`, `getBoolean`, etc.).
- Fragment image field for `og:image` / `twitter:image`.
- Canonical URL support on `FragmentViewModel`.
- Content relationships (prev/next, related, translations).
- Content series with ordering.
- Content revisions and audit trail.
- Publication lifecycle (draft, review, scheduled, published, archived, expired).
- Full-text search with autocomplete (Lucene).
- RSS 2.0 feed generation.
- XML sitemap generation with image support.
- CLI tool for project scaffolding.
- Live reload in development.
- HTMX support for partial rendering.
- Image optimization and responsive variants.
- Blog engine with pagination and filtering.
- Static page engine.
- SEO metadata with Open Graph, Twitter Card, and JSON-LD.
- Caching layer with automatic invalidation.
- Framework adapters for HTTP4k, Javalin, Spring Boot, Quarkus, and Micronaut.
- Bill of Materials (BOM) for dependency management.
