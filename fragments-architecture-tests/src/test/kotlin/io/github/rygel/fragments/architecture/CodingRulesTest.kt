package io.github.rygel.fragments.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * Enforces coding standards that protect library quality.
 *
 * These rules prevent common mistakes: using legacy APIs, leaking
 * implementation details, or introducing framework coupling in core code.
 */
class CodingRulesTest {
    private val allClasses =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("io.github.rygel.fragments")

    // -- Modern API usage -----------------------------------------------------

    @Test
    fun testMustNotUseJavaUtilDateUseJavaTimeInstead() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments..")
            .and()
            .doNotHaveFullyQualifiedName("io.github.rygel.fragments.MarkdownParser")
            .and()
            .doNotHaveFullyQualifiedName("io.github.rygel.fragments.MarkdownParser\$Companion")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.util.Date")
            .because(
                "java.util.Date is error-prone; use java.time.LocalDateTime or java.time.Instant. " +
                    "MarkdownParser is excluded because it converts SnakeYAML's legacy Date output to java.time",
            ).check(allClasses)
    }

    @Test
    fun testMustNotUseJavaUtilCalendar() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.util.Calendar")
            .because("java.util.Calendar is legacy; use java.time APIs")
            .check(allClasses)
    }

    @Test
    fun testMustNotUseJavaUtilHashtable() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.util.Hashtable")
            .because("Hashtable is legacy; use HashMap or ConcurrentHashMap")
            .check(allClasses)
    }

    @Test
    fun testMustNotUseJavaUtilVector() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.util.Vector")
            .because("Vector is legacy; use ArrayList or CopyOnWriteArrayList")
            .check(allClasses)
    }

    // -- Logging discipline ---------------------------------------------------

    @Test
    fun testCoreAndFeatureModulesMustNotUseSystemOutOrSystemErr() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments..")
            .and()
            .resideOutsideOfPackages(
                "io.github.rygel.fragments.cli..",
                "io.github.rygel.fragments.demo..",
            ).should()
            .accessClassesThat()
            .haveFullyQualifiedName("java.io.PrintStream")
            .because("library code must use SLF4J for logging, not System.out/err")
            .check(allClasses)
    }

    // -- No JUnit in production code ------------------------------------------

    @Test
    fun testProductionCodeMustNotDependOnJUnit() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.junit..",
                "org.junit.jupiter..",
            ).because("test frameworks must not leak into production code")
            .check(allClasses)
    }

    @Test
    fun testProductionCodeMustNotDependOnMockK() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.mockk..")
            .because("mocking frameworks must not leak into production code")
            .check(allClasses)
    }

    // -- Framework isolation in core ------------------------------------------

    @Test
    fun testCoreMustNotDependOnSpring() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
            ).because("the core module must remain framework-agnostic")
            .check(allClasses)
    }

    @Test
    fun testCoreMustNotDependOnJakartaEE() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "jakarta..",
            ).because("the core module must remain framework-agnostic")
            .check(allClasses)
    }

    @Test
    fun testCoreMustNotDependOnMicronaut() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "io.micronaut..",
            ).because("the core module must remain framework-agnostic")
            .check(allClasses)
    }

    @Test
    fun testCoreMustNotDependOnHttp4k() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.http4k..",
            ).because("the core module must remain framework-agnostic")
            .check(allClasses)
    }

    @Test
    fun testCoreMustNotDependOnJavalin() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "io.javalin..",
            ).because("the core module must remain framework-agnostic")
            .check(allClasses)
    }
}
