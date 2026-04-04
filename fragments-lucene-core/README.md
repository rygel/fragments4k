# Fragments Lucene Core

Full-text search and autocomplete powered by Apache Lucene.

## Usage

```kotlin
// Create and index
val searchEngine = LuceneSearchEngine(repository)
// Or with persistent index:
val searchEngine = LuceneSearchEngine(repository, indexPath = Path.of("./search-index"))
// Or multiple repositories:
val searchEngine = LuceneSearchEngine(listOf(blogRepo, projectRepo))

searchEngine.index()  // build/rebuild the index
```

### Simple Search

```kotlin
val results: List<SearchResult> = searchEngine.search("kotlin coroutines", maxResults = 20)
results.forEach { result ->
    println("${result.fragment.title} (score: ${result.score})")
}
```

### Advanced Search

```kotlin
val results = searchEngine.search(SearchOptions(
    query = "kotlin web framework",
    maxResults = 10,
    phraseSearch = true,      // exact phrase matching
    fuzzySearch = false,       // typo-tolerant matching
    fuzzyThreshold = 0.7f     // fuzzy match sensitivity (0.0 - 1.0)
))
```

### Autocomplete

```kotlin
val suggestions: List<SearchSuggestion> = searchEngine.autocomplete("kot", limit = 5)
suggestions.forEach { s ->
    println("${s.text} (${s.type})")  // "Kotlin" (TITLE), "kotlin" (TAG)
}
```

### Search by Tag/Category

```kotlin
val kotlinPosts = searchEngine.searchByTag("kotlin")
val techPosts = searchEngine.searchByCategory("tech")
```

### Cleanup

```kotlin
searchEngine.close()  // release Lucene resources
```

## Indexed Fields

| Field | Stored | Description |
|-------|--------|-------------|
| `title` | Yes | Fragment title (boosted 2x in search) |
| `content` | No | Full rendered content |
| `slug` | Yes | URL slug |
| `tag` | Yes | Tags (multi-valued) |
| `category` | Yes | Categories (multi-valued) |
| `date` | Yes | Publication date |
