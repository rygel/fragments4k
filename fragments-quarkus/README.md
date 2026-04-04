# Fragments Quarkus Adapter

Quarkus adapter for the Fragments markdown-based blog and static site library.

## Features

- ✅ Jakarta REST resource with suspend functions
- ✅ HTMX support for partial rendering
- ✅ Complete route coverage:
  - Home page
  - Static pages
  - Blog overview with pagination
  - Blog posts by date
  - Tag filtering
  - Category filtering
- ✅ CDI dependency injection
- ✅ Qute template integration
- ✅ Native compilation support

## Usage

Add dependency:

```xml
<dependency>
    <groupId>io.github.rygel</groupId>
    <artifactId>fragments-quarkus</artifactId>
    <version>0.6.2</version>
</dependency>
```

Configuration:

```kotlin
@ApplicationScoped
class FragmentsConfiguration(
    @ConfigProperty(name = "fragments.path", defaultValue = "./content")
    private val fragmentsPath: String
) {

    @Produces
    @ApplicationScoped
    fun fragmentRepository(): FragmentRepository {
        return FileSystemFragmentRepository(fragmentsPath)
    }

    @Produces
    @ApplicationScoped
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine {
        return StaticPageEngine(repository)
    }

    @Produces
    @ApplicationScoped
    fun blogEngine(repository: FragmentRepository): BlogEngine {
        return BlogEngine(repository)
    }
}
```

Resource is auto-configured via CDI.

## Routes

| Route | Method | Description |
|-------|--------|-------------|
| `/` | GET | Home page |
| `/page/{slug}` | GET | Static page |
| `/blog` | GET | Blog overview |
| `/blog/page/{page}` | GET | Blog with pagination |
| `/blog/{year}/{month}/{slug}` | GET | Blog post |
| `/blog/tag/{tag}` | GET | Posts by tag |
| `/blog/category/{category}` | GET | Posts by category |

## HTMX Support

Routes detect HTMX requests via `HX-Request` header and render partial content accordingly.

## Configuration

Configure fragments path in `application.properties`:

```properties
fragments.path=./content
```

## Testing

Integration tests included in `FragmentsQuarkusResourceTest` using `@QuarkusTest`.

## Native Image

Build native executable:

```bash
mvn clean package -Pnative
```
