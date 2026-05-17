# Performance Optimization Design

**Date:** 2026-05-17
**Status:** Draft
**Scope:** 7 performance optimization tasks from TODO.md

## Overview

Eliminate O(n) linear scans on every request, add structured indexes at load time, cache generated feed outputs, fix allocation waste in view models, add single-flight protection for cache misses, and replace O(n) eviction with a proven cache library. All changes are internal optimizations — no breaking API changes.

## Task Summary

| # | Task | Estimation |
|---|------|------------|
| 1 | Reload-Time Fragment Indexes | 3-5 days |
| 2 | Blog View Index | 3-5 days |
| 3 | Cache Generated Feed Outputs | 2-3 days |
| 4 | Avoid Duplicate Repository Reads | 1-2 days |
| 5 | Optimize ViewModel Computed Properties | 0.5-1 day |
| 6 | Single-Flight Cache Loads | 1-2 days |
| 7 | Improve Cache Eviction Complexity | 2-3 days |

**Total estimate:** ~13-21 days

## 1. Reload-Time Fragment Indexes

### Problem

`FileSystemFragmentRepository` and `ClasspathFragmentRepository` store fragments as a flat `@Volatile List<Fragment>`. Every query method (`getBySlug`, `getByTag`, `getByCategory`, `getByAuthor`, `getByYearMonthAndSlug`, `getAllVisible`) runs `.find{}` or `.filter{}` against the entire list — O(n) per call. `getAllVisible()` additionally sorts by date on every call.

### Design

Add a private `FragmentIndexes` data class that is rebuilt atomically whenever `cachedFragments` changes (on `loadFragmentsFromDisk()` and `cacheUpdatedFragment()`):

```kotlin
private data class FragmentIndexes(
    val bySlug: Map<String, Fragment>,
    val byTag: Map<String, List<Fragment>>,
    val byCategory: Map<String, List<Fragment>>,
    val byAuthor: Map<String, List<Fragment>>,
    val byYearMonth: Map<Pair<Int, Int>, List<Fragment>>,
    val allVisibleSorted: List<Fragment>,
    val byStatus: Map<FragmentStatus, List<Fragment>>,
)
```

**Build location:** A single private function `buildIndexes(fragments: List<Fragment>): FragmentIndexes` called from `loadFragmentsFromDisk()` and `cacheUpdatedFragment()`.

**Thread safety:** Keep `@Volatile` on `cachedFragments`. Add `@Volatile` on `indexes`. Both are swapped atomically (reference assignments in JVM are atomic for volatile fields).

**Query method changes:**
- `getBySlug(slug)` → `indexes.bySlug[slug]` (O(1))
- `getByTag(tag)` → `indexes.byTag[tag.lowercase()] ?: emptyList()` (O(1))
- `getByCategory(category)` → `indexes.byCategory[category.lowercase()] ?: emptyList()` (O(1))
- `getByAuthor(authorId)` → `indexes.byAuthor[authorId] ?: emptyList()` (O(1))
- `getByYearMonthAndSlug(year, month, slug)` → `indexes.byYearMonth[year.toInt() to month.toInt()]?.find { it.slug == slug }` (O(k) where k = posts in that month)
- `getAllVisible()` → `indexes.allVisibleSorted` (O(1))
- `getByStatus(status)` → `indexes.byStatus[status] ?: emptyList()` (O(1))

**Applied to:** Both `FileSystemFragmentRepository` and `ClasspathFragmentRepository`.

### Index rebuild triggers

| Trigger | Action |
|---------|--------|
| `loadFragmentsFromDisk()` (initial load + `reload()`) | Full rebuild |
| `cacheUpdatedFragment(fragment)` | Rebuild indexes from updated list |

### Acceptance criteria

- All existing query method signatures unchanged
- `getAll()` still returns the flat list (sorted by order) — index is internal
- `reload()` clears and rebuilds indexes
- Existing tests pass without modification
- New unit tests verify O(1) lookup behavior

## 2. Blog View Index

### Problem

`BlogEngine` filters blog-template fragments on every method call. `getOverview()`, `getAllPosts()`, `getByTag()`, `getByCategory()`, `getByYear()`, `getByYearMonth()`, `getByAuthor()` each:
1. Call `repository.getAllVisible()` → O(n) scan (fixed by Task 1)
2. Filter by `isBlogTemplate()` → O(n) filter
3. Call `withResolvedUrls()` → O(n) map + copy
4. Sort by date descending → O(n log n)

With the repository indexes from Task 1, step 1 is O(1), but steps 2-4 still run per request.

### Design

Add a `BlogIndex` data class inside `BlogEngine`, rebuilt when the underlying repository signals a reload:

```kotlin
private data class BlogIndex(
    val allPostsSorted: List<Fragment>,
    val byTag: Map<String, List<Fragment>>,
    val byCategory: Map<String, List<Fragment>>,
    val byYear: Map<Int, List<Fragment>>,
    val byYearMonth: Map<Pair<Int, Int>, List<Fragment>>,
    val byAuthor: Map<String, List<Fragment>>,
    val allTags: Map<String, Int>,
    val allCategories: Map<String, Int>,
)
```

**Reload-awareness:** `BlogEngine` currently has no reload hook. Add a `fun rebuildIndex()` that:
1. Gets `repository.getAllVisible()` (now O(1) from Task 1 indexes)
2. Filters to blog templates
3. Resolves URLs
4. Sorts by date
5. Partitions into maps

**Who triggers rebuild?** Two options:
- **Option A (preferred):** `BlogEngine` rebuilds lazily — checks a generation counter that increments on `repository.reload()`. First access after reload triggers rebuild.
- **Option B:** Explicit `rebuildIndex()` called by the adapter alongside `repository.reload()`.

Using Option A for zero API surface change. Add a `@Volatile private var indexGeneration: Int = 0` and `@Volatile private var repositoryGeneration: Int = 0`. On each access, compare against the repository's generation (expose `FragmentRepository.reloadGeneration(): Int`).

**Simplified alternative:** Since `FragmentRepository.reload()` already swaps `cachedFragments` reference, we can use reference identity. Store the `List<Fragment>` reference used to build the index. If `repository.getAllVisible()` returns a different reference, rebuild. This avoids adding a method to `FragmentRepository` interface.

### Acceptance criteria

- All `BlogEngine` method signatures unchanged
- `getOverview()` returns paginated results from pre-built index
- `getAllTags()` and `getAllCategories()` use pre-computed maps
- Reload (content change) triggers index rebuild on next access
- Existing tests pass without modification

## 3. Cache Generated Feed Outputs

### Problem

`FragmentsEngine.generateRssFeed()`, `generateSitemap()`, `generateLlmsTxt()` each call `collectResolvedFragments()` and regenerate their output from scratch on every request. RSS, sitemap, and llms.txt are typically crawled by bots every few minutes — the output only changes when content changes.

### Design

Add feed-level caching inside `FragmentsEngine`:

```kotlin
@Volatile private var cachedFeedOutput: FeedOutput? = null
@Volatile private var feedContentSnapshot: List<Fragment>? = null
```

**Cache invalidation strategy:** Reference-identity check. On each `generateAllFeeds()` / individual call:
1. Call `collectResolvedFragments()` (deduplicated by Task 4)
2. Compare fragment list identity to `feedContentSnapshot`
3. If same reference (no reload happened), return cached `FeedOutput`
4. If different, regenerate and cache

For individual feed methods (`generateRssFeed()`, etc.), use the same cache but extract the relevant field.

**Alternative (time-based):** Use the existing `InMemoryCache` from `fragments-cache-core` with a 5-minute TTL. Simpler but risks serving stale content for up to 5 minutes after a content change during development.

**Chosen approach:** Reference-identity check — instant invalidation on reload, no TTL configuration needed. Falls back to time-based TTL only if a `cacheFeedOutputs: Duration?` parameter is set on `FragmentsEngine`.

### Acceptance criteria

- `generateRssFeed()`, `generateSitemap()`, `generateLlmsTxt()`, `generateAllFeeds()` use cached output when content hasn't changed
- Cache is invalidated immediately on repository reload
- Existing tests pass (mock repositories always return new list instances, so they won't accidentally use cache)
- New test verifies that two consecutive calls return the same String reference

## 4. Avoid Duplicate Repository Reads During Feed Collection

### Problem

`FragmentsEngine.collectResolvedFragments()` calls three separate repository chains:
1. `staticEngine.getAllStaticPages()` → `staticRepository.getAllVisible()`
2. `blogEngine.getAllPosts()` → `blogRepository.getAllVisible()`
3. `additionalRepositories.flatMap { it.getAllVisible() }`

If `staticEngine` and `blogEngine` share the same repository (common in single-repo setups), `getAllVisible()` runs twice.

Additionally, `collectResolvedFragments()` is called separately by `generateRssFeed()`, `generateSitemap()`, and `generateLlmsTxt()` — three full traversals per feed generation batch.

### Design

**Step 1: De-duplicate repositories.** Change `collectResolvedFragments()` to:
1. Collect all unique repositories (by identity)
2. Call `getAllVisible()` once per unique repository
3. Partition results by static/blog template
4. Apply URL resolution per partition

```kotlin
suspend fun collectResolvedFragments(): List<Fragment> {
    val uniqueRepos = allRepositories.distinctBy { System.identityHashCode(it) }
    val repoResults = uniqueRepos.associateWith { it.getAllVisible() }
    
    val staticPages = staticEngine.filterStaticPages(repoResults[staticEngine.getRepository()] ?: emptyList())
    val blogPosts = blogEngine.filterAndResolvePosts(repoResults[blogEngine.getRepository()] ?: emptyList())
    val additional = additionalRepositories.flatMap { repoResults[repo] ?: it.getAllVisible() }
    val providerFragments = additionalFragmentProviders.flatMap { it() }
    
    return (staticPages + blogPosts + additional + providerFragments).distinctBy { it.slug }
}
```

**Step 2: Combine with Task 3 caching.** With feed output caching, `collectResolvedFragments()` is only called when the cache is cold. The de-duplication ensures minimum work during those cold starts.

### Acceptance criteria

- Shared-repo setups call `getAllVisible()` once (not twice) per cold feed generation
- `allRepositories` already contains the full list — reuse it
- Existing tests pass

## 5. Optimize ViewModel Computed Properties

### Problem

Two allocation wastes in `FragmentViewModel`:

1. **`formattedDate`** (line 137): Creates a new `DateTimeFormatter.ofPattern(dateFormat)` on every access. On a listing page with 20 posts, that's 20 `DateTimeFormatter` instances per render.

2. **`readingTime`** (line 180-181): Recomputes on every access — splits content into words, calculates minutes/seconds, builds a string. Not cached, not lazy.

3. **`relatedPosts`** (line 150-152): Already `by lazy` — good.

### Design

**`formattedDate`:** Cache the `DateTimeFormatter` in the companion object keyed by pattern string:

```kotlin
private val formatterCache = java.util.concurrent.ConcurrentHashMap<String, DateTimeFormatter>()

val formattedDate: String
    get() = fragment.date?.format(formatterCache.getOrPut(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) }) ?: ""
```

Thread-safe, no new allocations after warm-up. The cache is bounded by the number of distinct `dateFormat` values used in the application (typically 1-3).

**`readingTime`:** Make it lazy:

```kotlin
val readingTime: ReadingTime by lazy { calculateReadingTime() }
```

This changes the semantics slightly — `readingTime` is computed once per `FragmentViewModel` instance instead of recomputing on every access. Since `FragmentViewModel` is created per request and the underlying `Fragment` doesn't change within a request, this is safe and correct.

### Acceptance criteria

- `formattedDate` does not allocate `DateTimeFormatter` on repeated access
- `readingTime` computes once per `FragmentViewModel` instance
- All existing tests pass
- No behavioral change — same output, fewer allocations

## 6. Single-Flight Cache Loads

### Problem

`InMemoryCache.getOrCompute()` (lines 199-242 in `Cache.kt`):
1. Calls `get(key)` — acquires mutex, checks store, releases mutex (miss)
2. Calls `compute()` — no lock held
3. Acquires mutex, checks if another thread filled it (double-check)
4. If still empty, stores result

Between step 1 and step 3, concurrent callers for the same key all execute `compute()`. For expensive computations (e.g., full fragment reload, search index rebuild), this is a thundering-herd problem.

### Design

Add a `ConcurrentHashMap<K, Deferred<V>>` for in-flight computations:

```kotlin
private val inFlight = ConcurrentHashMap<K, kotlinx.coroutines.Deferred<V>>()

override suspend fun getOrCompute(key: K, compute: suspend () -> V): V {
    val cached = get(key)
    if (cached != null) return cached

    val deferred = inFlight.getOrPut(key) {
        coroutineScope { async { compute() } }
    }
    
    return try {
        val value = deferred.await()
        put(key, value)
        value
    } finally {
        inFlight.remove(key)
    }
}
```

**Key considerations:**
- `inFlight` map must be cleaned up even if computation fails (the `finally` block)
- The `Deferred` must be created in a `coroutineScope` to avoid leaking coroutines
- After the `await()`, we still do a `put()` which acquires the mutex — but the expensive work is done only once
- Need to handle the edge case where `deferred.await()` throws: remove from `inFlight` and propagate the exception

**Alternative:** Use `Mutex` per key (simpler but doesn't leverage coroutines). Use a striped lock approach to avoid unbounded key-level Mutex creation.

**Chosen approach:** `Deferred<V>` via `CompletableDeferred` — idiomatic Kotlin coroutines single-flight pattern.

### Acceptance criteria

- Concurrent callers for the same key only execute `compute()` once
- The first caller's result is returned to all waiters
- Exceptions are propagated to all waiters and the in-flight entry is cleaned up
- Existing tests pass
- New test: 10 concurrent calls to `getOrCompute(sameKey)` only invoke `compute` once

## 7. Improve Cache Eviction Complexity

### Problem

`InMemoryCache.enforceMaxSize()` (lines 315-327 in `Cache.kt`) calls `store.entries.minByOrNull { it.value.createdAt }` on every eviction — O(n) scan to find the oldest entry. Under high churn with a small `maxSize`, eviction runs frequently, making `put()` O(n).

### Design

Replace the `HashMap<K, CacheEntry<V>>` store with **Caffeine**, a proven Java caching library that provides O(1) eviction via Window TinyLFU policy.

**Option A: Use Caffeine as the backing store**

Add `com.github.ben-manes.caffeine:caffeine` as a dependency to `fragments-cache-core`. Wrap it:

```kotlin
class InMemoryCache<K, V>(
    private val configuration: CacheConfiguration = CacheConfiguration.DEFAULT,
) : Cache<K, V> {
    private val caffeine = Caffeine.newBuilder()
        .apply { configuration.maxSize?.let { maximumSize(it) } }
        .apply { configuration.ttl?.let { expireAfterWrite(it) } }
        .recordStats()
        .build<K, CacheEntry<V>>()
    ...
}
```

Pros: Battle-tested, O(1) eviction, built-in statistics, async loading support.
Cons: New dependency (~1.2MB).

**Option B: Maintain insertion order with LinkedHashMap**

Replace `HashMap` with `LinkedHashMap` (access-order mode) and evict from the head. O(1) amortized eviction.

Pros: No new dependency.
Cons: LRU is less sophisticated than Window TinyLFU; manual implementation of TTL expiration.

**Chosen approach: Option B (LinkedHashMap)**. The project philosophy is minimal dependencies. LRU eviction is sufficient for this use case. The `LinkedHashMap(16, 0.75f, true)` constructor enables access-order mode — `afterNodeAccess()` promotes entries to the tail on read. Eviction removes from the head (oldest/least-recently-used). The TTL expiration check remains in `get()`.

```kotlin
private val store = object : LinkedHashMap<K, CacheEntry<V>>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, CacheEntry<V>>): Boolean {
        val maxSize = configuration.maxSize ?: return false
        if (size > maxSize) {
            if (configuration.recordStats) {
                statistics.updateAndGet { it.eviction() }
            }
            return true
        }
        return false
    }
}
```

This eliminates the O(n) `minByOrNull` scan entirely. The `LinkedHashMap` automatically handles eviction in O(1) on every `put()`.

### Acceptance criteria

- `enforceMaxSize()` no longer scans all entries
- Eviction is O(1) amortized
- Access-order LRU: recently-read entries survive longer than unread entries
- TTL expiration still works correctly
- Existing tests pass
- Cache statistics (eviction count, hit/miss) remain accurate

## Dependencies Between Tasks

```
Task 1 (Fragment Indexes) ← foundation for Task 2
Task 2 (Blog Index) depends on Task 1
Task 4 (Dedup Reads) independent
Task 3 (Feed Cache) pairs well with Task 4, but independent
Task 5 (ViewModel Optimize) independent
Task 6 (Single-Flight) independent  
Task 7 (Cache Eviction) independent
```

Recommended build order: **1 → 2**, then **4 → 3**, and **5, 6, 7** in parallel.

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Indexes use more memory | Typical blogs have <1000 fragments. `FragmentIndexes` adds ~6 maps of references — ~50KB total. Negligible. |
| Stale index on content change | Indexes rebuilt atomically with `cachedFragments` swap. Same semantics as current code. |
| `FragmentViewModel.readingTime` lazy change | `FragmentViewModel` is per-request; underlying `Fragment` is immutable within a request. Safe. |
| Caffeine dependency (if chosen) | Option B (LinkedHashMap) avoids this entirely. |
| Single-flight `Deferred` leak on cancellation | `finally` block removes from `inFlight` map. `CompletableDeferred` handles cancellation. |

## Out of Scope

- Image variant generation optimization (Task 8 in TODO.md) — separate concern, requires image module changes
- HTTP response caching — already implemented as `HTTPResponseCache`
- Search result caching — already implemented as `CachedLuceneSearchEngine`
- Fragment caching layer — already implemented as `FragmentCache`
