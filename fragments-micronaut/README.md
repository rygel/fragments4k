# Fragments Micronaut Adapter

Micronaut adapter for the Fragments markdown-based blog and static site library.

## Features

- ✅ Controller with suspend functions
- ✅ HTMX support for partial rendering
- ✅ Complete route coverage:
  - Home page
  - Static pages
  - Blog overview with pagination
  - Blog posts by date
  - Tag filtering
  - Category filtering
- ✅ Dependency injection
- ✅ Thymeleaf template integration
- ✅ Low memory footprint
- ✅ Fast startup time

## Usage

Add dependency:

```xml
<dependency>
    <groupId>io.github.rygel</groupId>
    <artifactId>fragments-micronaut</artifactId>
    <version>0.6.2</version>
</dependency>
```

Configuration:

```kotlin
@Singleton
class FragmentsConfiguration {

    @Singleton
    fun fragmentRepository(): FragmentRepository {
        val fragmentsPath = System.getProperty("fragments.path") ?: "./content"
        return FileSystemFragmentRepository(fragmentsPath)
    }

    @Singleton
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine {
        return StaticPageEngine(repository)
    }

    @Singleton
    fun blogEngine(repository: FragmentRepository): BlogEngine {
        return BlogEngine(repository)
    }
}
```

Controller is auto-configured via DI.

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

Configure fragments path via system property or environment variable:

```bash
java -Dfragments.path=./content -jar your-app.jar
# or
export FRAGMENTS_PATH=./content
```

## Testing

Integration tests included in `FragmentsMicronautControllerTest` using `@MicronautTest`.
