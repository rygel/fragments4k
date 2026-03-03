package io.andromeda.fragments.cli

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object ProjectGenerator {
    
    fun generate(projectDir: File, projectName: String, framework: String, packageName: String) {
        val packagePath = packageName.replace(".", File.separator)
        val groupId = packageName.substringBeforeLast(".", packageName)
        
        createDirectory(projectDir)
        
        createPomXml(projectDir, projectName, framework, packageName, groupId)
        createSourceDirectories(projectDir, framework, packagePath)
        createContentDirectories(projectDir)
        createTemplateDirectories(projectDir, framework)
        
        when (framework) {
            "http4k" -> createHttp4kMainClass(projectDir, packagePath)
            "javalin" -> createJavalinMainClass(projectDir, packagePath)
            "spring-boot" -> createSpringBootMainClass(projectDir, packagePath)
            "quarkus" -> createQuarkusMainClass(projectDir, packagePath)
            "micronaut" -> createMicronautMainClass(projectDir, packagePath)
        }
        
        createSampleContent(projectDir)
        createTemplates(projectDir, framework)
        createApplicationProperties(projectDir, framework)
        createGitignore(projectDir)
        createReadme(projectDir, framework)
    }
    
    private fun createDirectory(dir: File) {
        dir.mkdirs()
    }
    
    private fun createSourceDirectories(projectDir: File, framework: String, packagePath: String) {
        val kotlinSrc = Paths.get(projectDir.absolutePath, "src", "main", "kotlin").toFile()
        val testSrc = Paths.get(projectDir.absolutePath, "src", "test", "kotlin").toFile()
        val resourcesSrc = Paths.get(projectDir.absolutePath, "src", "main", "resources").toFile()
        val resourcesTest = Paths.get(projectDir.absolutePath, "src", "test", "resources").toFile()
        
        val packageDir = Paths.get(kotlinSrc.absolutePath, packagePath).toFile()
        
        createDirectory(kotlinSrc)
        createDirectory(testSrc)
        createDirectory(resourcesSrc)
        createDirectory(resourcesTest)
        createDirectory(packageDir)
    }
    
    private fun createContentDirectories(projectDir: File) {
        val contentDir = Paths.get(projectDir.absolutePath, "content").toFile()
        val pagesDir = Paths.get(contentDir.absolutePath, "pages").toFile()
        val blogDir = Paths.get(contentDir.absolutePath, "blog").toFile()
        
        createDirectory(contentDir)
        createDirectory(pagesDir)
        createDirectory(blogDir)
    }
    
    private fun createTemplateDirectories(projectDir: File, framework: String) {
        val templatesDir = Paths.get(projectDir.absolutePath, "src", "main", "resources", "templates").toFile()
        val staticDir = Paths.get(projectDir.absolutePath, "src", "main", "resources", "static").toFile()
        
        createDirectory(templatesDir)
        createDirectory(staticDir)
    }
    
    private fun createPomXml(projectDir: File, projectName: String, framework: String, packageName: String, groupId: String) {
        val content = generatePomXmlContent(projectName, framework, packageName, groupId)
        val pomFile = Paths.get(projectDir.absolutePath, "pom.xml").toFile()
        pomFile.writeText(content)
    }
    
    private fun generatePomXmlContent(projectName: String, framework: String, packageName: String, groupId: String): String {
        val version = "1.0.0-SNAPSHOT"
        
        val (frameworkArtifact, frameworkDependency) = when (framework) {
            "http4k" -> "http4k" to """
                <dependency>
                    <groupId>org.http4k</groupId>
                    <artifactId>http4k-core</artifactId>
                    <version>6.31.1.0</version>
                </dependency>
                <dependency>
                    <groupId>org.http4k</groupId>
                    <artifactId>http4k-template-pebble</artifactId>
                    <version>6.31.1.0</version>
                </dependency>
                <dependency>
                    <groupId>org.http4k</groupId>
                    <artifactId>http4k-server-netty</artifactId>
                    <version>6.31.1.0</version>
                </dependency>
            """
            "javalin" -> "javalin" to """
                <dependency>
                    <groupId>io.javalin</groupId>
                    <artifactId>javalin</artifactId>
                    <version>6.1.6</version>
                </dependency>
                <dependency>
                    <groupId>io.javalin</groupId>
                    <artifactId>javalin-rendering</artifactId>
                    <version>6.1.6</version>
                </dependency>
            """
            "spring-boot" -> "spring-boot" to """
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-web</artifactId>
                    <version>3.3.2</version>
                </dependency>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-thymeleaf</artifactId>
                    <version>3.3.2</version>
                </dependency>
            """
            "quarkus" -> "quarkus" to """
                <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-resteasy-reactive</artifactId>
                    <version>3.13.3</version>
                </dependency>
                <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-qute</artifactId>
                    <version>3.13.3</version>
                </dependency>
            """
            "micronaut" -> "micronaut" to """
                <dependency>
                    <groupId>io.micronaut</groupId>
                    <artifactId>micronaut-http-server-netty</artifactId>
                    <version>4.5.1</version>
                </dependency>
                <dependency>
                    <groupId>io.micronaut.views</groupId>
                    <artifactId>micronaut-views-thymeleaf</artifactId>
                    <version>4.5.1</version>
                </dependency>
            """
            else -> throw IllegalArgumentException("Unknown framework: $framework")
        }
        
        return """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>$groupId</groupId>
    <artifactId>$projectName</artifactId>
    <version>$version</version>
    <packaging>jar</packaging>

    <name>$projectName</name>
    <description>A Fragments4k blog project</description>

    <properties>
        <java.version>17</java.version>
        <kotlin.version>2.0.21</kotlin.version>
        <maven.compiler.source>${'$'}{java.version}</maven.compiler.source>
        <maven.compiler.target>${'$'}{java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <fragments.version>1.0.0-SNAPSHOT</fragments.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.andromeda</groupId>
            <artifactId>fragments-$frameworkArtifact</artifactId>
            <version>${'$'}{fragments.version}</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.14</version>
        </dependency>
$frameworkDependency
    </dependencies>

    <repositories>
        <repository>
            <id>central</id>
            <url>https://repo.maven.apache.org/maven2</url>
        </repository>
    </repositories>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${'$'}{kotlin.version}</version>
                <executions>
                    <execution>
                        <id>compile</id>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <jvmTarget>${'$'}{java.version}</jvmTarget>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
            </plugin>
        </plugins>
    </build>
</project>
"""
    }
    
    private fun createHttp4kMainClass(projectDir: File, packagePath: String) {
        val packageDir = Paths.get(projectDir.absolutePath, "src", "main", "kotlin", packagePath).toFile()
        val mainFile = Paths.get(packageDir.absolutePath, "DemoApplication.kt").toFile()
        
        val content = """package ${packagePath.replace(File.separator, ".")}

import io.andromeda.fragments.FileSystemFragmentRepository
import io.andromeda.fragments.http4k.FragmentsHttp4kAdapter
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.server.asServer
import org.http4k.server.netty.Netty
import org.http4k.template.PebbleTemplates
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("DemoApplication")
    
    val fragmentsPath = System.getProperty("fragments.path") 
        ?: System.getenv("FRAGMENTS_PATH")
        ?: "./content"
    
    logger.info("Loading fragments from: " + fragmentsPath)
    
    val repository = FileSystemFragmentRepository(fragmentsPath)
    val staticEngine = StaticPageEngine(repository)
    val blogEngine = BlogEngine(repository)
    
    val renderer = PebbleTemplates().HotReload("src/main/resources/templates")
    val adapter = FragmentsHttp4kAdapter(
        staticEngine = staticEngine,
        blogEngine = blogEngine,
        renderer = renderer,
        siteTitle = "My Fragments Blog",
        siteDescription = "A blog powered by Fragments4k",
        siteUrl = "http://localhost:8080"
    )
    
    val server = CatchAll { e ->
        logger.error("Error handling request", e)
        org.http4k.core.Response(org.http4k.core.Status.INTERNAL_SERVER_ERROR)
            .body("Internal Server Error: ${'$'}{e.message}")
    }.then(adapter.createRoutes()).asServer(Netty(8080))
    
    logger.info("Starting Fragments4k HTTP4k server on port 8080")
    logger.info("RSS feed available at: http://localhost:8080/rss.xml")
    logger.info("Sitemap available at: http://localhost:8080/sitemap.xml")
    server.start()
    server.block()
}
"""
        mainFile.writeText(content)
    }
    
    private fun createJavalinMainClass(projectDir: File, packagePath: String) {
        val packageDir = Paths.get(projectDir.absolutePath, "src", "main", "kotlin", packagePath).toFile()
        val mainFile = Paths.get(packageDir.absolutePath, "DemoApplication.kt").toFile()
        
        val content = """package ${packagePath.replace(File.separator, ".")}

import io.andromeda.fragments.FileSystemFragmentRepository
import io.andromeda.fragments.javalin.fragmentsRoutes
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import io.javalin.Javalin
import io.javalin.rendering.template.JavalinPebble
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("DemoApplication")
    
    val fragmentsPath = System.getProperty("fragments.path")
        ?: System.getenv("FRAGMENTS_PATH")
        ?: "./content"
    
    logger.info("Loading fragments from: " + fragmentsPath)
    
    val repository = FileSystemFragmentRepository(fragmentsPath)
    val staticEngine = StaticPageEngine(repository)
    val blogEngine = BlogEngine(repository)
    
    val app = Javalin.create { config ->
        config.plugins.register(JavalinPebble {
            it.prefix = "templates"
        })
    }
    
    app.fragmentsRoutes(
        staticEngine = staticEngine,
        blogEngine = blogEngine,
        renderer = { template, viewModel ->
            JavalinPebble.extensions.render(template, viewModel)
        },
        siteTitle = "My Fragments Blog",
        siteDescription = "A blog powered by Fragments4k",
        siteUrl = "http://localhost:8080"
    )
    
    logger.info("Starting Fragments4k Javalin server on port 8080")
    logger.info("RSS feed available at: http://localhost:8080/rss.xml")
    logger.info("Sitemap available at: http://localhost:8080/sitemap.xml")
    app.start(8080)
}
"""
        mainFile.writeText(content)
    }
    
    private fun createSpringBootMainClass(projectDir: File, packagePath: String) {
        val packageDir = Paths.get(projectDir.absolutePath, "src", "main", "kotlin", packagePath).toFile()
        val mainFile = Paths.get(packageDir.absolutePath, "DemoApplication.kt").toFile()
        
        val content = """package ${packagePath.replace(File.separator, ".")}

import io.andromeda.fragments.FileSystemFragmentRepository
import io.andromeda.fragments.spring.FragmentsSpringController
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.slf4j.LoggerFactory

@SpringBootApplication
class DemoApplication {
    
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val logger = LoggerFactory.getLogger("DemoApplication")
            logger.info("Starting Fragments4k Spring Boot application")
            SpringApplication.run(DemoApplication::class.java, *args)
        }
    }
    
    @Bean
    fun fragmentRepository(): io.andromeda.fragments.FileSystemFragmentRepository {
        val fragmentsPath = System.getProperty("fragments.path")
            ?: System.getenv("FRAGMENTS_PATH")
            ?: "./content"
        return io.andromeda.fragments.FileSystemFragmentRepository(fragmentsPath)
    }
    
    @Bean
    fun staticPageEngine(repository: io.andromeda.fragments.FileSystemFragmentRepository): io.andromeda.fragments.static.StaticPageEngine {
        return io.andromeda.fragments.static.StaticPageEngine(repository)
    }
    
    @Bean
    fun blogEngine(repository: io.andromeda.fragments.FileSystemFragmentRepository): io.andromeda.fragments.blog.BlogEngine {
        return io.andromeda.fragments.blog.BlogEngine(repository)
    }
    
    @Bean
    fun fragmentsController(
        staticEngine: io.andromeda.fragments.static.StaticPageEngine,
        blogEngine: io.andromeda.fragments.blog.BlogEngine
    ): FragmentsSpringController {
        return FragmentsSpringController(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            siteTitle = "My Fragments Blog",
            siteDescription = "A blog powered by Fragments4k",
            siteUrl = "http://localhost:8080"
        )
    }
}
"""
        mainFile.writeText(content)
    }
    
    private fun createQuarkusMainClass(projectDir: File, packagePath: String) {
        val packageDir = Paths.get(projectDir.absolutePath, "src", "main", "kotlin", packagePath).toFile()
        val mainFile = Paths.get(packageDir.absolutePath, "DemoApplication.kt").toFile()
        
        val content = """package ${packagePath.replace(File.separator, ".")}

import io.andromeda.fragments.FileSystemFragmentRepository
import io.andromeda.fragments.quarkus.FragmentsQuarkusResource
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import org.jboss.logging.Logger

@ApplicationScoped
class DemoApplication {
    
    companion object {
        private val logger = Logger.getLogger(DemoApplication::class.java)
        
        @JvmStatic
        fun main(args: Array<String>) {
            logger.info("Starting Fragments4k Quarkus application")
            io.quarkus.runtime.Quarkus.run(*args)
        }
    }
    
    @Produces
    fun fragmentRepository(): FileSystemFragmentRepository {
        val fragmentsPath = System.getProperty("fragments.path")
            ?: System.getenv("FRAGMENTS_PATH")
            ?: "./content"
        return FileSystemFragmentRepository(fragmentsPath)
    }
    
    @Produces
    fun staticPageEngine(repository: FileSystemFragmentRepository): StaticPageEngine {
        return StaticPageEngine(repository)
    }
    
    @Produces
    fun blogEngine(repository: FileSystemFragmentRepository): BlogEngine {
        return BlogEngine(repository)
    }
}
"""
        mainFile.writeText(content)
    }
    
    private fun createMicronautMainClass(projectDir: File, packagePath: String) {
        val packageDir = Paths.get(projectDir.absolutePath, "src", "main", "kotlin", packagePath).toFile()
        val mainFile = Paths.get(packageDir.absolutePath, "DemoApplication.kt").toFile()
        
        val content = """package ${packagePath.replace(File.separator, ".")}

import io.andromeda.fragments.FileSystemFragmentRepository
import io.andromeda.fragments.micronaut.FragmentsMicronautController
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import io.micronaut.runtime.Micronaut
import org.slf4j.LoggerFactory

object DemoApplication {
    
    private val logger = LoggerFactory.getLogger("DemoApplication")
    
    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Starting Fragments4k Micronaut application")
        Micronaut.build()
            .packages(*args)
            .mainClass(DemoApplication::class.java)
            .start()
    }
}
"""
        mainFile.writeText(content)
    }
    
    private fun createSampleContent(projectDir: File) {
        val blogDir = Paths.get(projectDir.absolutePath, "content", "blog").toFile()
        val pagesDir = Paths.get(projectDir.absolutePath, "content", "pages").toFile()
        
        createSampleBlogPost(blogDir)
        createSampleIndexPage(pagesDir)
    }
    
    private fun createSampleBlogPost(blogDir: File) {
        val postFile = Paths.get(blogDir.absolutePath, "2024", "03", "welcome-to-fragments4k.md").toFile()
        postFile.parentFile.mkdirs()
        
        val content = """---
title: "Welcome to Fragments4k"
date: 2024-03-04
slug: welcome-to-fragments4k
tags: [fragments4k, kotlin, blog]
categories: [announcements]
---

# Welcome to Fragments4k!

This is your first blog post. Fragments4k is a framework-agnostic Markdown-based blog and static site library.

## Getting Started

You can edit this post by modifying the Markdown file in the \`content/blog/2024/03/\` directory.

## Features

- Markdown-based content
- Multiple framework support (HTTP4k, Javalin, Spring Boot, Quarkus, Micronaut)
- RSS feeds
- Sitemap generation
- Tag and category support
- Pagination

Happy blogging!
"""
        postFile.writeText(content)
    }
    
    private fun createSampleIndexPage(pagesDir: File) {
        val indexFile = Paths.get(pagesDir.absolutePath, "index.md").toFile()
        
        val content = """---
title: "Home"
slug: index
---

# Welcome to My Blog

This is the home page of your Fragments4k blog.

## Recent Posts

Check out our latest blog posts!

## About

This blog is powered by Fragments4k - a framework-agnostic Markdown-based blog library for Kotlin.
"""
        indexFile.writeText(content)
    }
    
    private fun createTemplates(projectDir: File, framework: String) {
        when (framework) {
            "http4k", "javalin" -> createPebbleTemplates(projectDir)
            "spring-boot", "micronaut" -> createThymeleafTemplates(projectDir)
            "quarkus" -> createQuteTemplates(projectDir)
        }
    }
    
    private fun createPebbleTemplates(projectDir: File) {
        val templatesDir = Paths.get(projectDir.absolutePath, "src", "main", "resources", "templates").toFile()
        
        Files.writeString(
            Paths.get(templatesDir.absolutePath, "layout.pebble"),
            """<!DOCTYPE html>
<html>
<head>
    <title>{{ title }}</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }
        header { border-bottom: 1px solid #ccc; margin-bottom: 20px; }
        .post { margin-bottom: 30px; }
        .post-title { margin: 0 0 10px 0; }
        .post-meta { color: #666; font-size: 0.9em; }
    </style>
</head>
<body>
    <header>
        <h1>My Fragments Blog</h1>
        <nav>
            <a href="/">Home</a> |
            <a href="/blog">Blog</a> |
            <a href="/rss.xml">RSS</a> |
            <a href="/sitemap.xml">Sitemap</a>
        </nav>
    </header>
    <main>
        {% block content %}{% endblock %}
    </main>
    <footer>
        <p>&copy; 2024 My Fragments Blog. Powered by Fragments4k.</p>
    </footer>
</body>
</html>"""
        )
        
        Files.writeString(
            Paths.get(templatesDir.absolutePath, "index.pebble"),
            """{% extends "layout.pebble" %}
{% block content %}
{% for fragment in fragments %}
    <div class="post">
        <h2 class="post-title">{{ fragment.title }}</h2>
        <div>{{ fragment.content|raw }}</div>
    </div>
{% endfor %}
{% endblock %}"""
        )
        
        Files.writeString(
            Paths.get(templatesDir.absolutePath, "blog_overview.pebble"),
            """{% extends "layout.pebble" %}
{% block content %}
{% for fragment in fragments %}
    <div class="post">
        <h2 class="post-title">
            <a href="{{ fragment.url }}">{{ fragment.title }}</a>
        </h2>
        <div class="post-meta">
            {{ fragment.date|date("yyyy-MM-dd") }} |
            {% if fragment.tags %}
                Tags: {{ fragment.tags|join(", ") }}
            {% endif %}
        </div>
        <div>{{ fragment.preview|raw }}</div>
    </div>
{% endfor %}
{% if totalPages > 1 %}
    <div class="pagination">
        {% if hasPrevious %}<a href="/blog?page={{ currentPage - 1 }}">Previous</a>{% endif %}
        Page {{ currentPage }} of {{ totalPages }}
        {% if hasNext %}<a href="/blog?page={{ currentPage + 1 }}">Next</a>{% endif %}
    </div>
{% endif %}
{% endblock %}"""
        )
    }
    
    private fun createThymeleafTemplates(projectDir: File) {
        val templatesDir = Paths.get(projectDir.absolutePath, "src", "main", "resources", "templates").toFile()
        
        Files.writeString(
            Paths.get(templatesDir.absolutePath, "layout.html"),
            """<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:text="${'$'}{title}">My Blog</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }
        header { border-bottom: 1px solid #ccc; margin-bottom: 20px; }
        .post { margin-bottom: 30px; }
        .post-title { margin: 0 0 10px 0; }
        .post-meta { color: #666; font-size: 0.9em; }
    </style>
</head>
<body>
    <header>
        <h1>My Fragments Blog</h1>
        <nav>
            <a href="/">Home</a> |
            <a href="/blog">Blog</a> |
            <a href="/rss.xml">RSS</a> |
            <a href="/sitemap.xml">Sitemap</a>
        </nav>
    </header>
    <main th:fragment="content">
        <div th:replace=":: content"></div>
    </main>
    <footer>
        <p>&copy; 2024 My Fragments Blog. Powered by Fragments4k.</p>
    </footer>
</body>
</html>"""
        )
        
        Files.writeString(
            Paths.get(templatesDir.absolutePath, "index.html"),
            """<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Home - My Fragments Blog</title>
</head>
<body>
    <header>
        <h1>My Fragments Blog</h1>
    </header>
    <main>
        <div th:each="fragment : ${'$'}{viewModel.fragments}">
            <h2 th:text="${'$'}{fragment.title}">Post Title</h2>
            <div th:utext="${'$'}{fragment.content}">Content</div>
        </div>
    </main>
</body>
</html>"""
        )
        
        Files.writeString(
            Paths.get(templatesDir.absolutePath, "blog_overview.html"),
            """<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Blog - My Fragments Blog</title>
</head>
<body>
    <header>
        <h1>My Fragments Blog</h1>
        <nav>
            <a href="/">Home</a> |
            <a href="/blog">Blog</a>
        </nav>
    </header>
    <main>
        <div th:each="fragment : ${'$'}{viewModel.fragments}">
            <h2>
                <a th:href="${'$'}{fragment.url}" th:text="${'$'}{fragment.title}">Post Title</a>
            </h2>
            <div class="post-meta">
                <span th:text="${'$'}{#temporals.format(fragment.date, 'yyyy-MM-dd')}">2024-03-04</span>
                <span th:if="${'$'}{fragment.tags}">
                    Tags: <span th:each="tag : ${'$'}{fragment.tags}" th:text="${'$'}{tag}">tag</span>
                </span>
            </div>
            <div th:utext="${'$'}{fragment.preview}">Preview</div>
        </div>
    </main>
</body>
</html>"""
        )
    }
    
    private fun createQuteTemplates(projectDir: File) {
        val templatesDir = Paths.get(projectDir.absolutePath, "src", "main", "resources", "templates").toFile()
        
        Files.writeString(
            Paths.get(templatesDir.absolutePath, "index.html"),
            """<!DOCTYPE html>
<html>
<head>
    <title>Home - My Fragments Blog</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }
        header { border-bottom: 1px solid #ccc; margin-bottom: 20px; }
        .post { margin-bottom: 30px; }
        .post-title { margin: 0 0 10px 0; }
        .post-meta { color: #666; font-size: 0.9em; }
    </style>
</head>
<body>
    <header>
        <h1>My Fragments Blog</h1>
    </header>
    <main>
        {#for fragment in viewModel.fragments}
            <div class="post">
                <h2>{fragment.title}</h2>
                <div>{fragment.content}</div>
            </div>
        {/for}
    </main>
</body>
</html>"""
        )
        
        Files.writeString(
            Paths.get(templatesDir.absolutePath, "blog_overview.html"),
            """<!DOCTYPE html>
<html>
<head>
    <title>Blog - My Fragments Blog</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }
        header { border-bottom: 1px solid #ccc; margin-bottom: 20px; }
        .post { margin-bottom: 30px; }
        .post-title { margin: 0 0 10px 0; }
        .post-meta { color: #666; font-size: 0.9em; }
    </style>
</head>
<body>
    <header>
        <h1>My Fragments Blog</h1>
    </header>
    <main>
        {#for fragment in viewModel.fragments}
            <div class="post">
                <h2>
                    <a href="{fragment.url}">{fragment.title}</a>
                </h2>
                <div class="post-meta">
                    {fragment.date}
                    {#if fragment.tags}
                        Tags: {fragment.tags}
                    {/if}
                </div>
                <div>{fragment.preview}</div>
            </div>
        {/for}
    </main>
</body>
</html>"""
        )
    }
    
    private fun createApplicationProperties(projectDir: File, framework: String) {
        val resourcesDir = Paths.get(projectDir.absolutePath, "src", "main", "resources").toFile()
        
        val content = when (framework) {
            "spring-boot" -> """
server.port=8080
server.servlet.context-path=/
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
logging.level.io.andromeda.fragments=INFO
"""
            "quarkus" -> """
quarkus.http.port=8080
quarkus.application.name=fragments-blog
quarkus.log.category."io.andromeda.fragments".level=INFO
"""
            "micronaut" -> """
micronaut.server.port=8080
micronaut.application.name=fragments-blog
"""
            else -> ""
        }
        
        if (content.isNotEmpty()) {
            val propertiesFile = Paths.get(resourcesDir.absolutePath, "application.properties").toFile()
            propertiesFile.writeText(content.trim())
        }
    }
    
    private fun createGitignore(projectDir: File) {
        val gitignoreFile = Paths.get(projectDir.absolutePath, ".gitignore").toFile()
        gitignoreFile.writeText("""# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties
.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
.settings/
.classpath
.project
bin/

# OS
.DS_Store
Thumbs.db
""")
    }
    
    private fun createReadme(projectDir: File, framework: String) {
        val readmeFile = Paths.get(projectDir.absolutePath, "README.md").toFile()
        
        val runCommand = when (framework) {
            "http4k", "javalin" -> "mvn exec:java -Dexec.mainClass=${getPackage()}.${getMainClass()}"
            "spring-boot" -> "mvn spring-boot:run"
            "quarkus" -> "mvn quarkus:dev"
            "micronaut" -> "mvn micronaut:run"
            else -> "mvn exec:java"
        }
        
        readmeFile.writeText("""# My Fragments Blog

A blog powered by Fragments4k with $framework.

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Build and Run

\`\`\`bash
# Build the project
mvn clean install

# Run the application
$runCommand
\`\`\`

### Access Your Blog

Open your browser and navigate to:
- Home: http://localhost:8080
- Blog: http://localhost:8080/blog
- RSS Feed: http://localhost:8080/rss.xml
- Sitemap: http://localhost:8080/sitemap.xml

## Content Management

Edit content files in the \`content/\` directory:
- \`content/pages/\` - Static pages
- \`content/blog/\` - Blog posts organized by year/month

Each content file is a Markdown file with YAML front matter:

\`\`\`markdown
---
title: "Post Title"
date: 2024-03-04
slug: post-slug
tags: [tag1, tag2]
categories: [category]
---

# Content here
\`\`\`

## Customization

- Templates: \`src/main/resources/templates/\`
- Configuration: \`src/main/resources/application.properties\`

## Learn More

- [Fragments4k Documentation](https://github.com/rygel/fragments4k)
- [Framework Documentation](${getFrameworkDocs(framework)})

""")
    }
    
    private fun getPackage(): String {
        return "io.andromeda.fragments.demo"
    }
    
    private fun getMainClass(): String {
        return "DemoApplicationKt"
    }
    
    private fun getFrameworkDocs(framework: String): String {
        return when (framework) {
            "http4k" -> "https://http4k.org"
            "javalin" -> "https://javalin.io"
            "spring-boot" -> "https://spring.io/projects/spring-boot"
            "quarkus" -> "https://quarkus.io"
            "micronaut" -> "https://micronaut.io"
            else -> ""
        }
    }
}
