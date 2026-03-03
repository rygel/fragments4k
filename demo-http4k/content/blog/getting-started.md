---
title: Getting Started with Fragments4k
date: 2024-03-03
tags: [kotlin, fragments, tutorial]
categories: [tutorial]
visible: true
template: blog_post
---

# Getting Started with Fragments4k

This is a tutorial on how to get started with Fragments4k.

## What is Fragments4k?

Fragments4k is a Kotlin library for building blogs and static sites using Markdown content. It's designed to be framework-agnostic, so you can use it with HTTP4k, Javalin, Spring Boot, Quarkus, or Micronaut.

## Installation

Add the dependency for your chosen framework:

```xml
<dependency>
    <groupId>io.andromeda</groupId>
    <artifactId>fragments-http4k</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Creating Content

Create markdown files in your content directory:

```markdown
---
title: My First Post
date: 2024-03-03
tags: [kotlin]
---

# My First Post

This is my first blog post!
```

## Running the Demo

```bash
./mvnw spring-boot:run
# or for other frameworks
./mvnw compile exec:java -Dexec.mainClass="your.MainClass"
```

## Next Steps

- Explore the [documentation](https://github.com/rygel/fragments4k)
- Check out the demo applications
- Read the adapter-specific README files
