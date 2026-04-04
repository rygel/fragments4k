# Fragments Spring Boot Adapter

Spring Boot adapter for the Fragments markdown-based blog and static site library.

## Features

- ✅ Full REST controller with suspend functions
- ✅ HTMX support for partial rendering
- ✅ Complete route coverage:
  - Home page
  - Static pages
  - Blog overview with pagination
  - Blog posts by date
  - Tag filtering
  - Category filtering
- ✅ Spring dependency injection
- ✅ Thymeleaf template integration

## Usage

Add dependency:

```xml
<dependency>
    <groupId>io.github.rygel</groupId>
    <artifactId>fragments-spring-boot</artifactId>
    <version>0.6.2</version>
</dependency>
```

Configuration:

```kotlin
@Configuration
class FragmentsConfiguration {

    @Bean
    fun fragmentRepository(): FragmentRepository {
        return FileSystemFragmentRepository("./content")
    }

    @Bean
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine {
        return StaticPageEngine(repository)
    }

    @Bean
    fun blogEngine(repository: FragmentRepository): BlogEngine {
        return BlogEngine(repository)
    }
}
```

Controller is auto-configured via `FragmentsSpringController`.

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

## Testing

Integration tests included in `FragmentsSpringControllerTest`.
