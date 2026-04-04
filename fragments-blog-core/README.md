# Fragments Blog Core

Blog engine with pagination, filtering, and content relationships.

## Usage

```kotlin
val blogEngine = BlogEngine(
    repository = repository,
    pageSize = 10,
    relationshipConfig = RelationshipConfig()
)

// Paginated overview (excludes drafts by default)
val page = blogEngine.getOverview(page = 1)
page.items         // List<Fragment>
page.currentPage   // 1
page.totalPages    // 5
page.hasNext       // true
page.hasPrevious   // false

// Single post with prev/next relationships
val (post, relationships) = blogEngine.getPostWithRelationships("2026", "03", "hello-world")

// Filter by tag or category
val kotlinPosts = blogEngine.getByTag("kotlin", page = 1)
val techPosts = blogEngine.getByCategory("tech", page = 1)

// Archive
val yearPosts = blogEngine.getByYear(2026)
val monthPosts = blogEngine.getByYearMonth(2026, 3)

// Tag/category counts
val tagCounts = blogEngine.getAllTags()       // Map<String, Int>
val catCounts = blogEngine.getAllCategories()  // Map<String, Int>
```

Blog posts are fragments with a `blog`-prefixed template (e.g. `blog_post`, `blog_overview`) and a non-null `date` field.
