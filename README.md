# Fragments4k

A framework-agnostic Markdown-based blog and static site library for Kotlin, with adapters for multiple JVM web frameworks.

## Overview

Fragments4k is a Kotlin port of the Fragments library, designed to be framework-agnostic while providing seamless integration with popular JVM web frameworks through adapters.

## Features

### Core Features
- ✅ Markdown content with YAML front matter
- ✅ Static pages and blog posts
- ✅ Pagination support
- ✅ Tag and category filtering
- ✅ Date-based blog routing
- ✅ RSS feed generation
- ✅ Sitemap generation for SEO
- ✅ Full-text search (Lucene)
- ✅ HTMX support for partial rendering
- ✅ Live reload in development mode
- ✅ CLI scaffolding tool for quick project setup
- ✅ Coroutines-based async operations

### Framework Adapters

Choose your framework:

| Adapter | Status | Template Engine |
|---------|--------|-----------------|
| HTTP4k | ✅ Complete | Pebble |
| Javalin | ✅ Complete | Pebble |
| Spring Boot | ✅ Complete | Thymeleaf |
| Quarkus | ✅ Complete | Qute |
| Micronaut | ✅ Complete | Thymeleaf |

## Quick Start

### Dependency

Add the appropriate adapter to your `pom.xml`:

**HTTP4k:**
```xml
<dependency>
    <groupId>io.andromeda</groupId>
    <artifactId>fragments-http4k</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Javalin:**
```xml
<dependency>
    <groupId>io.andromeda</groupId>
    <artifactId>fragments-javalin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Spring Boot:**
```xml
<dependency>
    <groupId>io.andromeda</groupId>
    <artifactId>fragments-spring-boot</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Quarkus:**
```xml
<dependency>
    <groupId>io.andromeda</groupId>
    <artifactId>fragments-quarkus</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Micronaut:**
```xml
<dependency>
    <groupId>io.andromeda</groupId>
    <artifactId>fragments-micronaut</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Content Structure

Create markdown files in your content directory:

```
content/
├── index.md
├── about.md
├── blog/
│   ├── 2024-01-15-my-first-post.md
│   └── 2024-02-20-second-post.md
```

### Front Matter

Each markdown file can include YAML front matter:

```yaml
---
title: My First Post
date: 2024-01-15
tags: [kotlin, programming]
categories: [tech]
visible: true
template: blog_post
---

# My First Post

This is the content of my blog post...
```

## Configuration

Each adapter uses `FileSystemFragmentRepository` which reads content from a directory:

```kotlin
val repository = FileSystemFragmentRepository("./content")
```

## CLI Tool

Fragments4k includes a CLI tool for scaffolding new projects and running with live reload:

### Scaffold a New Project

```bash
# Generate a new HTTP4k project
java -jar fragments-cli.jar init my-blog --framework=http4k

# Generate a Spring Boot project
java -jar fragments-cli.jar init my-blog --framework=spring-boot

# Available frameworks: http4k, javalin, spring-boot, quarkus, micronaut
```

The CLI generates:
- Complete project structure with Maven configuration
- Framework-specific templates (Pebble, Thymeleaf, Qute)
- Sample markdown content
- README.md with framework-specific run instructions

### Run with Live Reload

```bash
# Start with live reload enabled
java -jar fragments-cli.jar run --watch

# Custom content directory
java -jar fragments-cli.jar run --watch --content-dir=./content

# Custom port
java -jar fragments-cli.jar run --watch --port=3000
```

### Live Reload Integration

To enable live reload in your application:

1. Add the live-reload dependency:

```xml
<dependency>
    <groupId>io.andromeda</groupId>
    <artifactId>fragments-live-reload</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. Start watching in your application:

```kotlin
import io.andromeda.fragments.livereload.LiveReloadManager
import java.nio.file.Paths

val liveReloadManager = LiveReloadManager(
    repository = repository,
    contentDir = Paths.get("content")
)

// Start watching (async)
runBlocking {
    liveReloadManager.startWatching()
}

// Add shutdown hook
Runtime.getRuntime().addShutdownHook(Thread {
    runBlocking {
        liveReloadManager.stopWatching()
    }
})
```

See [Live Reload README](fragments-live-reload/README.md) for more details.

## Project Structure

```
fragments4k/
├── fragments-core/              # Domain model and parsing
├── fragments-static-core/        # Static pages engine
├── fragments-blog-core/         # Blog engine
├── fragments-rss-core/          # RSS generation
├── fragments-sitemap-core/       # Sitemap generation
├── fragments-lucene-core/       # Search integration
├── fragments-live-reload/       # Live reload in development
├── fragments-cli/               # CLI scaffolding tool
├── fragments-http4k/            # HTTP4k adapter
├── fragments-javalin/           # Javalin adapter
├── fragments-spring-boot/       # Spring Boot adapter
├── fragments-quarkus/           # Quarkus adapter
├── fragments-micronaut/         # Micronaut adapter
├── demo-http4k/               # HTTP4k demo application
├── demo-spring-boot/           # Spring Boot demo application
├── demo-javalin/              # Javalin demo application
├── demo-quarkus/              # Quarkus demo application
└── demo-micronaut/            # Micronaut demo application
```

## Building

```bash
mvn clean install
```

## Testing

```bash
mvn test
```

## CI/CD

GitHub Actions workflows:
- `.github/workflows/ci.yml` - Build and test pipeline

## Documentation

- [Implementation Status](IMPLEMENTATION_STATUS.md)
- [HTTP4k Adapter README](fragments-http4k/README.md)
- [Javalin Adapter README](fragments-javalin/README.md)
- [Spring Boot Adapter README](fragments-spring-boot/README.md)
- [Quarkus Adapter README](fragments-quarkus/README.md)
- [Micronaut Adapter README](fragments-micronaut/README.md)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

[Specify your license here]

## Acknowledgments

Based on the original [Fragments](https://github.com/your-repo/fragments) Java library.
