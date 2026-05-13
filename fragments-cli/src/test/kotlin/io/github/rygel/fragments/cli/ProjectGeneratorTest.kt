package io.github.rygel.fragments.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ProjectGeneratorTest {
    @TempDir
    lateinit var tempDir: Path

    private fun generateProject(framework: String): File {
        val projectDir = tempDir.resolve("test-project").toFile()
        ProjectGenerator.generate(
            projectDir = projectDir,
            projectName = "my-blog",
            framework = framework,
            packageName = "com.example.blog",
        )
        return projectDir
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun generatesCorrectDirectoryStructure(framework: String) {
        val dir = generateProject(framework)

        assertTrue(dir.resolve("pom.xml").isFile, "pom.xml should exist")
        assertTrue(dir.resolve("README.md").isFile, "README.md should exist")
        assertTrue(dir.resolve(".gitignore").isFile, ".gitignore should exist")

        assertTrue(dir.resolve("src/main/kotlin").isDirectory, "src/main/kotlin should exist")
        assertTrue(
            dir.resolve("src/main/kotlin/com/example/blog").isDirectory,
            "package directory should exist",
        )
        assertTrue(dir.resolve("src/test/kotlin").isDirectory, "src/test/kotlin should exist")
        assertTrue(
            dir.resolve("src/main/resources").isDirectory,
            "src/main/resources should exist",
        )
        assertTrue(
            dir.resolve("src/test/resources").isDirectory,
            "src/test/resources should exist",
        )

        assertTrue(dir.resolve("content").isDirectory, "content/ should exist")
        assertTrue(dir.resolve("content/pages").isDirectory, "content/pages/ should exist")
        assertTrue(dir.resolve("content/blog").isDirectory, "content/blog/ should exist")

        assertTrue(
            dir.resolve("src/main/resources/templates").isDirectory,
            "templates directory should exist",
        )
        assertTrue(
            dir.resolve("src/main/resources/static").isDirectory,
            "static directory should exist",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun generatesMainClass(framework: String) {
        val dir = generateProject(framework)

        val mainFile = dir.resolve("src/main/kotlin/com/example/blog/DemoApplication.kt")
        assertTrue(mainFile.isFile, "DemoApplication.kt should exist for $framework")
        val content = mainFile.readText()
        assertTrue(content.contains("package com.example.blog"), "should have correct package")
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun pomXmlContainsProjectName(framework: String) {
        val dir = generateProject(framework)

        val pomContent = dir.resolve("pom.xml").readText()
        assertTrue(
            pomContent.contains("<artifactId>my-blog</artifactId>"),
            "pom.xml should contain artifactId my-blog",
        )
        assertTrue(
            pomContent.contains("<name>my-blog</name>"),
            "pom.xml should contain name my-blog",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun pomXmlContainsBasePackage(framework: String) {
        val dir = generateProject(framework)

        val pomContent = dir.resolve("pom.xml").readText()
        assertTrue(
            pomContent.contains("<groupId>com.example</groupId>"),
            "pom.xml should contain groupId from package",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun pomXmlContainsFrameworkDependency(framework: String) {
        val dir = generateProject(framework)

        val pomContent = dir.resolve("pom.xml").readText()
        val expectedArtifact = "fragments-$framework"
        assertTrue(
            pomContent.contains(expectedArtifact),
            "pom.xml should contain $expectedArtifact dependency",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun generatesSampleContentFiles(framework: String) {
        val dir = generateProject(framework)

        val blogPost = dir.resolve("content/blog/2024/03/welcome-to-fragments4k.md")
        assertTrue(blogPost.isFile, "sample blog post should exist")
        val blogContent = blogPost.readText()
        assertTrue(blogContent.contains("Welcome to Fragments4k"), "blog post should have title")
        assertTrue(blogContent.contains("---"), "blog post should have front matter")

        val indexPage = dir.resolve("content/pages/index.md")
        assertTrue(indexPage.isFile, "sample index page should exist")
        val indexContent = indexPage.readText()
        assertTrue(indexContent.contains("Welcome to My Blog"), "index page should have title")
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun generatesGitignore(framework: String) {
        val dir = generateProject(framework)

        val gitignore = dir.resolve(".gitignore")
        assertTrue(gitignore.isFile, ".gitignore should exist")
        val content = gitignore.readText()
        assertTrue(content.contains("target/"), ".gitignore should contain target/")
        assertTrue(content.contains(".idea/"), ".gitignore should contain .idea/")
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun generatesReadmeWithFramework(framework: String) {
        val dir = generateProject(framework)

        val readme = dir.resolve("README.md")
        assertTrue(readme.isFile, "README.md should exist")
        val content = readme.readText()
        assertTrue(content.contains(framework), "README should mention $framework")
        assertTrue(content.contains("Fragments4k"), "README should mention Fragments4k")
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "javalin"])
    fun generatesPebbleTemplatesForPebbleFrameworks(framework: String) {
        val dir = generateProject(framework)

        val templatesDir = dir.resolve("src/main/resources/templates")
        assertTrue(
            templatesDir.resolve("layout.pebble").isFile,
            "layout.pebble should exist for $framework",
        )
        assertTrue(
            templatesDir.resolve("index.pebble").isFile,
            "index.pebble should exist for $framework",
        )
        assertTrue(
            templatesDir.resolve("blog_overview.pebble").isFile,
            "blog_overview.pebble should exist for $framework",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["spring-boot", "micronaut"])
    fun generatesThymeleafTemplatesForThymeleafFrameworks(framework: String) {
        val dir = generateProject(framework)

        val templatesDir = dir.resolve("src/main/resources/templates")
        assertTrue(
            templatesDir.resolve("layout.html").isFile,
            "layout.html should exist for $framework",
        )
        assertTrue(
            templatesDir.resolve("index.html").isFile,
            "index.html should exist for $framework",
        )
        assertTrue(
            templatesDir.resolve("blog_overview.html").isFile,
            "blog_overview.html should exist for $framework",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["quarkus"])
    fun generatesQuteTemplatesForQuarkus(framework: String) {
        val dir = generateProject(framework)

        val templatesDir = dir.resolve("src/main/resources/templates")
        assertTrue(
            templatesDir.resolve("index.html").isFile,
            "index.html should exist for quarkus",
        )
        assertTrue(
            templatesDir.resolve("blog_overview.html").isFile,
            "blog_overview.html should exist for quarkus",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["spring-boot", "quarkus", "micronaut"])
    fun generatesApplicationPropertiesForSupportedFrameworks(framework: String) {
        val dir = generateProject(framework)

        val props = dir.resolve("src/main/resources/application.properties")
        assertTrue(props.isFile, "application.properties should exist for $framework")
        val content = props.readText()
        assertTrue(content.contains("8080"), "application.properties should configure port 8080")
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun pomXmlContainsBomImport(framework: String) {
        val dir = generateProject(framework)

        val pomContent = dir.resolve("pom.xml").readText()
        assertTrue(
            pomContent.contains("<artifactId>fragments-bom</artifactId>"),
            "pom.xml should import fragments-bom",
        )
        assertTrue(
            pomContent.contains("<type>pom</type>"),
            "fragments-bom should be imported as pom type",
        )
        assertTrue(
            pomContent.contains("<scope>import</scope>"),
            "fragments-bom should be imported with import scope",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun pomXmlContainsVersionProperties(framework: String) {
        val dir = generateProject(framework)

        val pomContent = dir.resolve("pom.xml").readText()
        assertTrue(
            pomContent.contains("<java.version>17</java.version>"),
            "pom.xml should declare java.version property",
        )
        assertTrue(
            pomContent.contains("<kotlin.version>2.2.0</kotlin.version>"),
            "pom.xml should declare kotlin.version property",
        )
        assertTrue(
            pomContent.contains("<fragments.version>0.6.6</fragments.version>"),
            "pom.xml should declare fragments.version property",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k"])
    fun pomXmlContainsHttp4kVersionProperty(framework: String) {
        val dir = generateProject(framework)

        val pomContent = dir.resolve("pom.xml").readText()
        assertTrue(
            pomContent.contains("<http4k.version>6.31.1.0</http4k.version>"),
            "pom.xml should declare http4k.version property",
        )
        assertTrue(
            pomContent.contains("{http4k.version}"),
            "http4k dependencies should use version property",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["javalin"])
    fun pomXmlContainsJavalinVersionProperty(framework: String) {
        val dir = generateProject(framework)

        val pomContent = dir.resolve("pom.xml").readText()
        assertTrue(
            pomContent.contains("<javalin.version>7.1.0</javalin.version>"),
            "pom.xml should declare javalin.version property",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["spring-boot"])
    fun pomXmlContainsSpringBootVersionProperty(framework: String) {
        val dir = generateProject(framework)

        val pomContent = dir.resolve("pom.xml").readText()
        assertTrue(
            pomContent.contains("<spring-boot.version>3.3.2</spring-boot.version>"),
            "pom.xml should declare spring-boot.version property",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun readmeContainsProductionChecklist(framework: String) {
        val dir = generateProject(framework)

        val readme = dir.resolve("README.md").readText()
        assertTrue(
            readme.contains("Production Checklist"),
            "README should contain production checklist section",
        )
        assertTrue(
            readme.contains("siteUrl"),
            "README should mention setting siteUrl",
        )
        assertTrue(
            readme.contains("CSP"),
            "README should mention CSP configuration",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["http4k", "spring-boot", "javalin", "quarkus", "micronaut"])
    fun readmeContainsSecurityNote(framework: String) {
        val dir = generateProject(framework)

        val readme = dir.resolve("README.md").readText()
        assertTrue(
            readme.contains("sanitizes all HTML content"),
            "README should explain HTML sanitization",
        )
        assertTrue(
            readme.contains("SanitizerProfile.STRICT"),
            "README should mention strict sanitizer profile option",
        )
    }
}
