# Test Data Factories

Test data factories for Fragments CMS to make writing tests faster and easier.

## Available Factories

### FragmentFactory
Creates test Fragment objects with fluent builder pattern.

```kotlin
// Basic fragment
val fragment = FragmentFactory.create()

// Published fragment
val published = FragmentFactory.published()

// Fragment with custom properties
val custom = FragmentFactory.Builder()
    .title("Custom Title")
    .categories(listOf("kotlin", "java"))
    .tags(listOf("test", "example"))
    .build()
```

### AuthorFactory
Creates test Author objects with fluent builder pattern.

```kotlin
// Basic author
val author = AuthorFactory.create()

// Full profile author
val fullProfile = AuthorFactory.fullProfile()

// Author with social links
val withLinks = AuthorFactory.withSocialLinks(
    "twitter" to "@testauthor",
    "github" to "testauthor"
)
```

### ContentSeriesFactory
Creates test ContentSeries objects with fluent builder pattern.

```kotlin
// Basic series
val series = ContentSeriesFactory.create()

// Active series
val active = ContentSeriesFactory.active()
```

### RandomDataGenerator
Generates random test data for testing.

```kotlin
val generator = RandomDataGenerator()

// Random fragment
val randomFragment = generator.randomFragment()

// Random author
val randomAuthor = generator.randomAuthor()

// Random tags (3 tags)
val tags = generator.randomTags(3)

// Random HTML content
val htmlContent = generator.randomHtmlContent()
```

### TestScenario
Creates complex test scenarios with multiple entities.

```kotlin
// Simple blog
val simpleBlog = TestScenario.simpleBlog()

// Blog with drafts
val blogWithDrafts = TestScenario.blogWithDrafts()

// Multi-author blog
val multiAuthorBlog = TestScenario.multiAuthorBlog()

// Complex scenario
val complexBlog = TestScenario.complexBlog()
```

## Usage Examples

### Creating test fragments

```kotlin
// Simple fragment
val fragment = FragmentFactory.Builder()
    .title("Test Post")
    .content("<p>Test content</p>")
    .categories(listOf("kotlin", "java"))
    .tags(listOf("tutorial", "example"))
    .status(FragmentStatus.PUBLISHED)
    .visible(true)
    .build()
```

### Creating test scenarios

```kotlin
val scenario = TestScenario.Builder()
    .name("Integration Test Scenario")
    .description("A complex scenario for integration testing")
    .fragments(
        FragmentFactory.createMany(10).map { it.copy(status = FragmentStatus.PUBLISHED) }
    )
    .authors(
        AuthorFactory.createMany(3)
    )
    .contentSeries(
        ContentSeriesFactory.createMany(2)
    )
    .build()
```

## Benefits

- **Faster test writing**: Builders make creating complex test data simple
- **Consistent test data**: Factories ensure consistent test data structure
- **Less boilerplate**: Reusable factory methods reduce code duplication
- **Flexible scenarios**: TestScenario class for complex test cases
- **Random data generation**: Easy to create varied test data

## Best Practices

1. **Use factories in tests**: Always prefer factories over manual object creation
2. **Customize for specific cases**: Use builder methods when you need specific properties
3. **Generate random data**: Use RandomDataGenerator for varied test scenarios
4. **Create test scenarios**: Use TestScenario for complex multi-entity tests
5. **Keep tests readable**: Descriptive test names and meaningful assertions
