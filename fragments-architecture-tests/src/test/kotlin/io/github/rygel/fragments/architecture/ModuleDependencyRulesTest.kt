package io.github.rygel.fragments.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Test

/**
 * Enforces module isolation boundaries.
 *
 * Core and feature modules must never depend on adapter or framework modules.
 * Adapter modules must never depend on each other — they are interchangeable.
 */
class ModuleDependencyRulesTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("io.github.rygel.fragments")

    // -- Core isolation -------------------------------------------------------

    @Test
    fun `core must not depend on any adapter module`() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments")
            .and()
            .resideOutsideOfPackages(
                "io.github.rygel.fragments.http4k..",
                "io.github.rygel.fragments.javalin..",
                "io.github.rygel.fragments.spring..",
                "io.github.rygel.fragments.quarkus..",
                "io.github.rygel.fragments.micronaut..",
                "io.github.rygel.fragments.cli..",
                "io.github.rygel.fragments.demo..",
            ).should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "io.github.rygel.fragments.http4k..",
                "io.github.rygel.fragments.javalin..",
                "io.github.rygel.fragments.spring..",
                "io.github.rygel.fragments.quarkus..",
                "io.github.rygel.fragments.micronaut..",
                "io.github.rygel.fragments.cli..",
            ).because("core and feature modules must be framework-agnostic")
            .check(classes)
    }

    @Test
    fun `http4k adapter must not depend on other adapters`() {
        adapterMustNotDependOnOtherAdapters("io.github.rygel.fragments.http4k..")
    }

    @Test
    fun `javalin adapter must not depend on other adapters`() {
        adapterMustNotDependOnOtherAdapters("io.github.rygel.fragments.javalin..")
    }

    @Test
    fun `spring adapter must not depend on other adapters`() {
        adapterMustNotDependOnOtherAdapters("io.github.rygel.fragments.spring..")
    }

    @Test
    fun `quarkus adapter must not depend on other adapters`() {
        adapterMustNotDependOnOtherAdapters("io.github.rygel.fragments.quarkus..")
    }

    @Test
    fun `micronaut adapter must not depend on other adapters`() {
        adapterMustNotDependOnOtherAdapters("io.github.rygel.fragments.micronaut..")
    }

    // -- Cycle detection ------------------------------------------------------

    @Test
    fun `there must be no cyclic dependencies between packages`() {
        slices()
            .matching("io.github.rygel.fragments.(*)..")
            .should()
            .beFreeOfCycles()
            .because("cyclic dependencies make the library impossible to split or evolve independently")
            .check(classes)
    }

    // -- Helpers --------------------------------------------------------------

    private val adapterPackages =
        listOf(
            "io.github.rygel.fragments.http4k..",
            "io.github.rygel.fragments.javalin..",
            "io.github.rygel.fragments.spring..",
            "io.github.rygel.fragments.quarkus..",
            "io.github.rygel.fragments.micronaut..",
        )

    private fun adapterMustNotDependOnOtherAdapters(adapterPackage: String) {
        val otherAdapters = adapterPackages.filter { it != adapterPackage }.toTypedArray()
        noClasses()
            .that()
            .resideInAPackage(adapterPackage)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*otherAdapters)
            .because("adapters are interchangeable and must not couple to each other")
            .check(classes)
    }
}
