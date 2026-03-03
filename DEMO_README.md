# Fragments4k Demo Applications

This directory contains demo applications showing how to use Fragments4k with different web frameworks.

## Available Demos

All demos use the same content structure and sample Markdown files.

### demo-http4k
Demo application using HTTP4k framework.

**Template Engine:** Pebble  
**Server:** Netty

#### Running the Demo

```bash
cd demo-http4k

# Build and run
mvn clean package
java -jar target/demo-http4k-1.0.0-SNAPSHOT.jar

# Or run directly with Maven
mvn exec:java
```

The server will start on port 8080.

#### Customizing Content Path

```bash
# Via system property
java -Dfragments.path=/path/to/content -jar target/demo-http4k-1.0.0-SNAPSHOT.jar

# Via environment variable
export FRAGMENTS_PATH=/path/to/content
java -jar target/demo-http4k-1.0.0-SNAPSHOT.jar
```

### demo-javalin
Demo application using Javalin framework.

**Template Engine:** Pebble  
**Server:** Javalin (Jetty)

#### Running the Demo

```bash
cd demo-javalin

# Build and run
mvn clean package
java -jar target/demo-javalin-1.0.0-SNAPSHOT.jar

# Or run directly with Maven
mvn exec:java
```

The server will start on port 8080.

#### Available Endpoints

- `http://localhost:8080/` - Home page
- `http://localhost:8080/page/{slug}` - Static pages
- `http://localhost:8080/blog` - Blog overview
- `http://localhost:8080/blog/{year}/{month}/{slug}` - Blog post
- `http://localhost:8080/blog/tag/{tag}` - Posts by tag
- `http://localhost:8080/blog/category/{category}` - Posts by category
- `http://localhost:8080/rss.xml` - RSS feed

#### Customizing Content Path

```bash
# Via system property
java -Dfragments.path=/path/to/content -jar target/demo-javalin-1.0.0-SNAPSHOT.jar

# Via environment variable
export FRAGMENTS_PATH=/path/to/content
java -jar target/demo-javalin-1.0.0-SNAPSHOT.jar
```

### demo-spring-boot
Demo application using Spring Boot framework.

**Template Engine:** Thymeleaf  
**Server:** Tomcat (embedded)

#### Running the Demo

```bash
cd demo-spring-boot

# Build and run
mvn clean package
java -jar target/demo-spring-boot-1.0.0-SNAPSHOT.jar

# Or run directly with Maven
mvn spring-boot:run
```

The server will start on port 8080.

#### Customizing Content Path

```bash
# Via system property
java -Dfragments.path=/path/to/content -jar target/demo-spring-boot-1.0.0-SNAPSHOT.jar

# Via environment variable
export FRAGMENTS_PATH=/path/to/content
java -jar target/demo-spring-boot-1.0.0-SNAPSHOT.jar
```

### demo-quarkus
Demo application using Quarkus framework.

**Template Engine:** Qute  
**Server:** Netty (Quarkus)

#### Running the Demo

```bash
cd demo-quarkus

# Run in dev mode (hot reload)
mvn quarkus:dev

# Build and run
mvn clean package
java -jar target/demo-quarkus-1.0.0-SNAPSHOT-runner.jar

# Build native image
mvn package -Pnative
./target/demo-quarkus-1.0.0-SNAPSHOT-runner
```

The server will start on port 8080.

#### Customizing Content Path

```bash
# Via system property
java -Dfragments.path=/path/to/content -jar target/demo-quarkus-1.0.0-SNAPSHOT-runner.jar

# Via environment variable
export FRAGMENTS_PATH=/path/to/content
java -jar target/demo-quarkus-1.0.0-SNAPSHOT-runner.jar
```

### demo-micronaut
Demo application using Micronaut framework.

**Template Engine:** Thymeleaf  
**Server:** Netty (Micronaut)

#### Running the Demo

```bash
cd demo-micronaut

# Run in dev mode
mvn mn:run

# Build and run
mvn clean package
java -jar target/demo-micronaut-1.0.0-SNAPSHOT.jar
```

The server will start on port 8080.

#### Customizing Content Path

```bash
# Via system property
java -Dfragments.path=/path/to/content -jar target/demo-micronaut-1.0.0-SNAPSHOT.jar

# Via environment variable
export FRAGMENTS_PATH=/path/to/content
java -jar target/demo-micronaut-1.0.0-SNAPSHOT.jar
```

## Front Matter

Each markdown file can include YAML front matter:

```yaml
---
title: My Post
date: 2024-03-03
tags: [kotlin, tutorial]
categories: [tech]
visible: true
template: blog_post
---

# My Post

Content goes here...
```

## Available Fields

- `title` - Post/page title
- `date` - Publication date
- `tags` - Array of tags
- `categories` - Array of categories
- `visible` - Whether to show (true/false)
- `template` - Template to use (optional)
- `slug` - URL-friendly identifier (optional, auto-generated)
- `ordering` - Sort order (optional)
- `content` - Full HTML content
- `preview` - Content before `<!--more-->` tag

## Template Variables

Templates receive a `viewModel` object with:

- `title` - Fragment title
- `content` - Full HTML content
- `preview` - Content preview
- `formattedDate` - Formatted date string
- `date` - Original date object
- `tags` - List of tags
- `categories` - List of categories
- `slug` - URL slug
- `year` - Year
- `month` - Month (as string)
- `monthValue` - Month (as number 1-12)

## Next Steps

1. Explore the demo applications
2. Customize the content in `content/` directory
3. Modify templates in `src/main/resources/templates/`
4. Integrate into your own application

## Additional Frameworks

Demo applications for additional frameworks (Javalin, Quarkus, Micronaut) can be created following the same pattern.
