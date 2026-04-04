# Fragments Cache Core

Caching decorator for `FragmentRepository` with automatic invalidation on status changes.

## Usage

```kotlin
val cache = ConcurrentMapFragmentCache()  // or your own FragmentCache implementation
val cachedRepo = CachedFragmentRepository(
    delegate = FileSystemFragmentRepository("./content"),
    fragmentCache = cache
)

// Use cachedRepo anywhere a FragmentRepository is expected.
// All reads are cached; writes (status updates, scheduling, etc.) automatically invalidate.
```

`CachedFragmentRepository` implements the full `FragmentRepository` interface. Cache entries are invalidated whenever fragment status changes (publish, archive, expire, revert, schedule).
