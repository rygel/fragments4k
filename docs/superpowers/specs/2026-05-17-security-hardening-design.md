# Security Hardening Design

**Date:** 2026-05-17
**Status:** Approved
**Scope:** 6 security hardening tasks from TODO.md (High Priority)

## Overview

Harden the Fragments4k codebase against path traversal, YAML deserialization, query DoS, decompression bombs, and missing security headers. All fixes are defense-in-depth additions — no breaking API changes.

## Task Summary

| # | Task | Risk | Estimation |
|---|------|------|------------|
| 1 | Path Containment Utility | Foundation | 0.5 day |
| 2 | Harden Revision File Paths | Path traversal | 0.5 day |
| 3 | Safe YAML in Revision Repo | Deserialization | 0.25 day |
| 4 | Validate Series Slugs | Path traversal | 0.5 day |
| 5 | Harden Lucene Query Handling | DoS | 1 day |
| 6 | Preflight Image Metadata | Memory exhaustion | 1 day |
| 7 | Expand HTTP Security Headers | Missing hardening | 0.5 day |

## 1. Path Containment Utility

Create `PathSafety` object in `fragments-core/src/main/kotlin/io/github/rygel/fragments/PathSafety.kt`.

### Slug Validation

```kotlin
val SLUG_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
const val SLUG_MAX_LENGTH = 128

fun validateSlug(slug: String) {
    require(slug.isNotBlank()) { "Slug must not be blank" }
    require(slug.length <= SLUG_MAX_LENGTH) { "Slug exceeds max length $SLUG_MAX_LENGTH" }
    require(slug.matches(SLUG_PATTERN)) { "Invalid slug format: $slug" }
}
```

Matches the existing pattern in `RequestValidation.kt` and `FileSystemAuthorRepository.kt`.

### Path Containment

```kotlin
fun resolveAndCheck(baseDir: File, fileName: String): File {
    val baseCanonical = baseDir.canonicalPath
    val target = File(baseDir, fileName).canonicalPath
    require(target.startsWith(baseCanonical + File.separator) || target == baseCanonical) {
        "Path traversal detected: $fileName escapes $baseDir"
    }
    return File(target)
}
```

Used by all file-system repositories before constructing file paths from user-controlled input.

### Consumers

- `FileSystemFragmentRevisionRepository` — revision IDs and slugs
- `FileSystemContentSeriesRepository` — series slugs
- `FileSystemAuthorRepository` — author slugs (already validates, but will use shared utility)
- `FileSystemFragmentRepository` — fragment slugs (currently logs warnings only)

## 2. Harden Revision File Paths

**File:** `FileSystemFragmentRevisionRepository.kt`

### Changes

- Add `PathSafety.validateSlug(slug)` at entry points: `createRevision()`, `getRevisions()`, `compareRevisions()`
- Validate revision IDs via `PathSafety.validateSlug()` (revision IDs follow `"$slug-v$version-$timestamp"` format, which matches the slug pattern since version and timestamp are alphanumeric)
- Replace all `File(revisionsDir, "$id.json")` with `PathSafety.resolveAndCheck(revisionsDir, "$id.json")`
- Locations: `loadRevision()`, `deleteRevision()`, `deleteRevisionsBefore()`, revision file creation

### Tests

- `should reject revision ID with path traversal` — `../../etc/passwd` throws
- `should reject slug with path separator` — `foo/bar` throws
- `should reject blank revision ID` — throws
- `should accept valid revision ID` — `my-post-v1-1234567890` succeeds

## 3. Safe YAML in Revision Repository

**File:** `FileSystemFragmentRevisionRepository.kt:17`

### Change

Replace:
```kotlin
private val yaml = Yaml()
```

With:
```kotlin
private val yaml = Yaml(SafeConstructor(LoaderOptions()))
```

This aligns with `FileSystemContentSeriesRepository.kt:22` and `FileSystemAuthorRepository.kt` which already use `SafeConstructor`.

### Test

- `should reject unsafe YAML tags in revision front matter` — revision YAML containing `!!javax.script.ScriptEngineManager` throws during load, not deserialized

## 4. Validate Content Series Slugs

**File:** `FileSystemContentSeriesRepository.kt`

### Changes

- Add `PathSafety.validateSlug(slug)` to `saveSeries()`, `deleteSeries(slug)`, `getBySlug(slug)`
- Replace `File(seriesDir, "${series.slug}$extension")` with `PathSafety.resolveAndCheck(seriesDir, "${series.slug}$extension")`

### Tests

- `should reject series slug with path traversal` — `../../etc/passwd` throws
- `should reject blank series slug` — throws
- `should accept valid series slug` — `my-tutorial` succeeds

## 5. Harden Public Lucene Query Handling

**File:** `LuceneSearchEngine.kt`

### Changes

**Standard search (public):** Escape query text with `QueryParser.escape(query)` before passing to `MultiFieldQueryParser`. This treats all user input as literal text, preventing boolean operators, wildcards, field-specific queries, and fuzzy syntax.

**Advanced search (admin/internal):** Add `SearchType.ADVANCED` to `SearchOptions` that bypasses escaping, preserving raw Lucene syntax for trusted callers.

**Cross-cutting guards (all modes):**
- Reject leading wildcards: if the parsed query contains a term starting with `*` or `?`, throw `IllegalArgumentException`
- Cap token count: split query on whitespace, if > 20 tokens truncate with a warning log

### Files touched

- `LuceneSearchEngine.kt` — `buildStandardQuery()`, `buildQuery()` dispatch
- `SearchOptions.kt` (or wherever SearchOptions is defined) — add `SearchType` enum with `STANDARD` and `ADVANCED`

### Tests

- `should treat special characters as literal in standard search` — `title:"secret" OR content:admin` finds nothing (treated as plain text)
- `should reject leading wildcard` — `*admin` throws
- `should truncate excessive token count` — 30-word query uses only first 20 tokens
- `should allow raw syntax in advanced mode` — `title:hello AND content:world` parses correctly

## 6. Preflight Image Metadata

**File:** `BasicImageOptimizer.kt`

### Changes

Add `preflightImage(stream)` method that:

1. Marks the stream position (requires `BufferedInputStream` wrapping)
2. Gets `ImageReader` via `ImageIO.getImageReaders(stream)`
3. Calls `reader.setInput(stream)` and reads `reader.getWidth(0)` / `reader.getHeight(0)` — lightweight metadata-only read
4. Rejects if width > `MAX_DIMENSION` or height > `MAX_DIMENSION`
5. Rejects if `(width.toLong() * height.toLong()) > MAX_PIXEL_COUNT` (200M pixels)
6. Resets stream position
7. Returns the `ImageReader` for potential reuse

Modify `optimize()` overloads to call `preflightImage()` before `ImageIO.read()`.

### Constants

Add to `ImageOptimizer.kt`:
```kotlin
const val MAX_PIXEL_COUNT = 200_000_000L
```

### Tests

- `should reject image exceeding max dimension` — 25000x1 image throws before decode
- `should reject image exceeding max pixel count` — 15000x15000 image throws
- `should reject unsupported format` — non-image input throws with clear error
- `should accept valid image within limits` — normal image passes preflight

## 7. Expand HTTP Security Headers

**File:** `FragmentsEngine.kt` — `securityHeaders()` method

### Changes

Add to the returned map:

```kotlin
"Permissions-Policy" to "camera=(), microphone=(), geolocation=()"
```

Add conditional HSTS (only when HTTPS is configured):
```kotlin
if (config.isHttpsEnabled) {
    "Strict-Transport-Security" to "max-age=63072000; includeSubDomains; preload"
}
```

The `FragmentsEngine.Config` already has `siteUrl`. Add a computed `val isHttpsEnabled: Boolean get() = siteUrl.startsWith("https")`.

### Adapter impact

None. All adapters delegate to `securityHeaders()`, so the new headers propagate automatically.

### Tests

- `should include Permissions-Policy in security headers` — verify header present
- `should include HSTS when site URL is HTTPS` — verify header present
- `should omit HSTS when site URL is HTTP` — verify header absent

## Implementation Order

1. **PathSafety utility** — foundation for tasks 2 and 4
2. **Safe YAML** (task 3) — one-line fix, independent
3. **Revision path hardening** (task 2) — depends on PathSafety
4. **Series slug validation** (task 4) — depends on PathSafety
5. **Lucene query hardening** (task 5) — independent
6. **Image preflight** (task 6) — independent
7. **HTTP headers** (task 7) — independent

Tasks 3, 5, 6, 7 are independent and can be parallelized after PathSafety is complete.

## Non-Goals

- No changes to the public API surface (interfaces remain unchanged)
- No rate limiting implementation (infrastructure concern, out of scope)
- No Caffeine cache migration (separate TODO item)
- No changes to test infrastructure or build configuration
