# Fragments4k Audit Findings

## Security Audit

### HIGH

| # | Issue | File | Fix | Status |
|---|-------|------|-----|--------|
| S3 | No HTML sanitization on rendered markdown — XSS via `<script>` tags in fragment content | `MarkdownParser.kt` | Add OWASP Java HTML Sanitizer or Jsoup after flexmark rendering | **Done** (PR #110 — jsoup `HtmlSanitizer`) |
| S4 | `FileSystemFragmentRepository.cachedFragments` is not volatile — race condition on concurrent `loadFragments()` calls (line 91 is plain `var` but line 94 `cachedRelationships` is `@Volatile` — inconsistent) | `FileSystemFragmentRepository.kt:91-92` | Add `@Volatile` to `cachedFragments` and `lastLoaded` | **Done** (PR #110) |
| S5/A7 | `LuceneSearchEngine.close()` is **never called** — file handle and memory leaks on shutdown | All adapter configurations | Call `close()` in DI shutdown lifecycle (`@PreDestroy` or equivalent) | **Done** (PR #110 — `@PreDestroy`/shutdown hooks) |

### MEDIUM

| # | Issue | File | Fix | Status |
|---|-------|------|-----|--------|
| S6 | `findRelatedPosts()` loads all fragments into memory to find 5 related — optimization opportunity (not a disk hit, cachedFragments is already in memory) | `FragmentViewModel.kt:230-257` | Pre-compute relationships via `ContentRelationshipGenerator` at repository level | Pending |
| S7 | CSP allows external CDN `cdnjs.cloudflare.com` + `unsafe-inline` for styles | `FragmentsEngine.kt:65` | Consider removing CDN from CSP, removing `'unsafe-inline'`, or serving Prism locally | Pending |
| S8 | No file size limits in `parseFragmentFile()` — DoS via multi-GB files | `FileSystemFragmentRepository.kt:309` | Add `MAX_FILE_SIZE` constant, check `file.length()` before `readText()` | **Done** (PR #110 — 10MB limit) |
| A5 | `LuceneSearchEngine.index()` has race condition — `search()` can call during rebuild, reader swap is not atomic | `LuceneSearchEngine.kt:79-119` | Use `Mutex` to protect index rebuilds | **Done** (PR #110) |

### LOW

| # | Issue | File | Fix | Status |
|---|-------|------|-----|--------|
| S1 | No authentication on any routes — adapters expose read-only content only; mutations are repository-level methods not exposed as HTTP endpoints. Consumer's responsibility. | All 5 adapter modules | Document that auth should be added by consumers | Pending |
| S2 | No authorization on status-mutating operations — these are repository methods, not HTTP endpoints. Not exploitable via web. | `FileSystemFragmentRepository.kt` | Document in API docs | Pending |
| S9 | LiveReload socket is unauthenticated — dev-only feature, standard LiveReload protocol | `LiveReloadManager.kt` | Document as dev-only | Pending |
| S10 | Lucene index directory has no explicit file permission restrictions | `LuceneSearchEngine.kt:70-75` | Document OS-level permission requirements | Pending |

---

## Architecture Audit

### HIGH

| # | Issue | File | Fix | Status |
|---|-------|------|-----|--------|
| A5 | (see Security — Lucene race condition) | | | |

### MEDIUM

| # | Issue | File | Fix | Status |
|---|-------|------|-----|--------|
| A2 | `FragmentRepository` interface has 20+ methods — `ClasspathFragmentRepository` throws `UnsupportedOperationException` for 75%. Splitting has tradeoffs (more dependencies vs smaller interfaces) | `FragmentRepository.kt:17-187` | Split into `FragmentReadRepository` + `FragmentWriteRepository` or add abstract base class with default implementations | Pending |
| A3 | `FragmentsEngine` is a facade with 40+ methods — Facade pattern is intentional but the surface area is large | `FragmentsEngine.kt:31-256` | Keep as facade but document the design decision. Consider splitting if it grows further | Pending |
| A8 | No global exception handler in any adapter — raw error strings exposed | All 5 adapter modules | Add framework-specific handlers (`@ControllerAdvice`, `ExceptionMapper`, etc.) | **Done** (PR #110) |
| A9 | Adapters expose raw exceptions as HTTP error strings | All adapter modules | Create consistent `ErrorResponse` objects via centralized error handling | **Done** (PR #110 — shared `ErrorResponse` data class) |
| A11 | Static page engine filters by template string — fragile `template == "static"` comparison | `StaticPageEngine.kt:25` | Use centralized `FragmentTemplates` constants or template type enum | **Done** (PR #110 — `FragmentTemplates` constants) |
| A12 | `FragmentsEngine.siteUrl` passed to constructor but not used consistently (e.g., `LlmsTxtGenerator`) | `FragmentsEngine.kt:31-38` | Make siteUrl usage consistent or remove from constructor | **Done** (PR #114 — all 51 FragmentViewModel calls updated) |
| A13 | Template name is runtime `Set<String>` check — typos not caught at compile time | `BlogEngine.kt:200` | Use enum or sealed class for template types | **Done** (PR #115 — `DEFAULT` constant, `isBlogTemplate()` consolidation) |

### LOW

| # | Issue | File | Fix | Status |
|---|-------|------|-----|--------|
| A1 | `Fragment` is a data class with 30+ properties — not a god object (no behavior, just data). Splitting adds indirection without reducing complexity. | `Fragment.kt:169-279` | Design preference — acceptable as-is | Pending |
| A4 | `FragmentViewModel` computes reading time, extracts TOC, finds related posts — presentation logic belongs on the ViewModel. Each is <30 lines. | `FragmentViewModel.kt:42-258` | Acceptable as-is — extraction adds files without real benefit | Pending |
| A15 | ViewModels in adapter-core duplicate fields extensively | `ViewModels.kt` | Create common `PagedContentViewModel` base class or use composition | **Skipped** (9 shared data fields, no logic — extraction has high blast radius, low benefit) |
| A16 | `InMemoryCache` uses both `ConcurrentHashMap` (thread-safe) AND `Mutex` — unnecessary overhead for simple ops | `Cache.kt:173-174` | Use `ConcurrentHashMap` directly for get/put, `Mutex` only for compound operations | **Done** (PR #115 — `HashMap` + `AtomicReference` for statistics) |
| A17 | `FileSystemFragmentRevisionRepository.init` calls `mkdirs()` which can fail silently | `FileSystemFragmentRevisionRepository.kt:20-24` | Move to first use or add error handling | **Done** (PR #115) |
| A19 | `Result.failure` exception logs details but returns generic message — caller can't display helpful feedback | `FileSystemFragmentRepository.kt:187-188` | Include full transition validation details in returned exception message | **Done** (PR #115 — context-rich IOException wrapping) |

### WON'T FIX

| # | Issue | Reason |
|---|-------|--------|
| A6 | `fragments-adapter-core` depends on 7 core modules | Aggregation is the module's purpose — a "bundle" module is just a rename |
| A10 | BlogEngine/StaticPageEngine duplicate URL resolution | 3-line trivial fallback — not worth extracting |
| A14 | ContentRelationshipGenerator is static object | Stateless pure functions — `object` is correct Kotlin |
| A18 | SeriesPart holds direct Fragment reference | Direct references are better for read-heavy CMS (avoids N+1 lookups) |

---

## Summary

| Severity | Security | Architecture | Total |
|----------|----------|--------------|-------|
| HIGH | 3 | 0 | 3 |
| MEDIUM | 2 | 7 | 9 |
| LOW | 4 | 6 | 10 |
| **Total** | **9** | **13** | **22** |

## Implementation Priority

1. **S4** — Add `@Volatile` to cachedFragments/lastLoaded (1-line fix)
2. **S3** — Add HTML sanitization after markdown rendering
3. **S5** — Wire LuceneSearchEngine.close() to DI lifecycle
4. **A5** — Add Mutex to LuceneSearchEngine.index()
5. **S8** — Add file size limit to parseFragmentFile()
6. **A8/A9** — Add global exception handlers to adapters
7. **A11** — Use template constants instead of raw strings
8. **A12** — Consistent siteUrl usage
9. **A13** — Template type enum
10. **A15** — PagedContentViewModel base class
11. **S7** — CSP hardening (optional — may serve Prism locally)
12. **S6** — Pre-compute related posts (optimization)
