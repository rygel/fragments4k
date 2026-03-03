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
- ✅ Full-text search (Lucene)
- ✅ HTMX support for partial rendering
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

Each adapter uses the `FileSystemFragmentRepository` which reads content from a directory:

```kotlin
val repository = FileSystemFragmentRepository("./content")
```

## Project Structure

```
fragments4k/
├── fragments-core/              # Domain model and parsing
├── fragments-static-core/        # Static pages engine
├── fragments-blog-core/         # Blog engine
├── fragments-rss-core/          # RSS generation
├── fragments-lucene-core/       # Search integration
├── fragments-http4k/            # HTTP4k adapter
├── fragments-javalin/           # Javalin adapter
├── fragments-spring-boot/       # Spring Boot adapter
├── fragments-quarkus/           # Quarkus adapter
└── fragments-micronaut/         # Micronaut adapter
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
