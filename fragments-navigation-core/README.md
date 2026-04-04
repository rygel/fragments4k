# Fragments Navigation Core

Menu, pagination, archive navigation, search form, and footer generators for templates.

## Navigation Menu

```kotlin
val menu = NavigationMenuGenerator.generateMainMenu(
    siteUrl = "/",
    blogUrl = "/blog",
    archiveUrl = "/blog/archive",
    searchUrl = "/search"
)
// Returns List<NavigationLink> for template rendering
```

## Archive Navigation

```kotlin
// Year links
val yearLinks = ArchiveNavigationGenerator.generateYearLinks(
    baseUrl = "/blog/archive",
    availableYears = listOf(2024, 2025, 2026),
    currentYear = 2026
)

// Month links for a year
val monthLinks = ArchiveNavigationGenerator.generateMonthLinks(year = 2026, currentMonth = 3)

// Breadcrumbs
val crumbs = ArchiveNavigationGenerator.generateBreadcrumbs(currentYear = 2026, currentMonth = 3)
```

## Pagination

```kotlin
val pagination = PaginationGenerator.generateSimpleControls(
    currentPage = 2,
    totalPages = 10,
    basePath = "/blog"
)
```

## Search Form

```kotlin
val searchForm = SearchFormGenerator.generate(
    actionUrl = "/search",
    paramName = "q",
    placeholderText = "Search articles...",
    buttonText = "Search",
    method = "get"
)
```

## Footer

```kotlin
val footer = FooterGenerator.generate()
```
