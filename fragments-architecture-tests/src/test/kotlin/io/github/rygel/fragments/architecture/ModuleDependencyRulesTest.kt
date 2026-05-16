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
    fun testCoreMustNotDependOnAnyAdapterModule() {
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
    fun testHttp4kAdapterMustNotDependOnOtherAdapters() {
        adapterMustNotDependOnOtherAdapters("io.github.rygel.fragments.http4k..")
    }

    @Test
    fun testJavalinAdapterMustNotDependOnOtherAdapters() {
        adapterMustNotDependOnOtherAdapters("io.github.rygel.fragments.javalin..")
    }

    @Test
    fun testSpringAdapterMustNotDependOnOtherAdapters() {
        adapterMustNotDependOnOtherAdapters("io.github.rygel.fragments.spring..")
    }

    @Test
    fun testQuarkusAdapterMustNotDependOnOtherAdapters() {
        adapterMustNotDependOnOtherAdapters("io.github.rygel.fragments.quarkus..")
    }

    @Test
    fun testMicronautAdapterMustNotDependOnOtherAdapters() {
        adapterMustNotDependOnOtherAdapters("io.github.rygel.fragments.micronaut..")
    }

    // -- Cycle detection ------------------------------------------------------

    @Test
    fun testNoCyclicDependenciesBetweenPackages() {
        slices()
            .matching("io.github.rygel.fragments.(*)..")
            .should()
            .beFreeOfCycles()
            .because("cyclic dependencies make the library impossible to split or evolve independently")
            .check(classes)
    }

    // -- Feature module isolation ---------------------------------------------

    private val featurePackages =
        listOf(
            "io.github.rygel.fragments.blog..",
            "io.github.rygel.fragments.rss..",
            "io.github.rygel.fragments.sitemap..",
            "io.github.rygel.fragments.image..",
            "io.github.rygel.fragments.navigation..",
            "io.github.rygel.fragments.social..",
            "io.github.rygel.fragments.seo..",
            "io.github.rygel.fragments.chat..",
            "io.github.rygel.fragments.cache..",
        )

    private val allowedFeatureDependencies =
        mapOf(
            "io.github.rygel.fragments.lucene.." to
                setOf(
                    "io.github.rygel.fragments.blog..",
                    "io.github.rygel.fragments.cache..",
                ),
        )

    @Test
    fun testFeatureModulesMustNotDependOnAdapterCore() {
        noClasses()
            .that()
            .resideInAnyPackage(*allFeaturePackages.toTypedArray())
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.github.rygel.fragments.adapter..")
            .because("feature modules must not depend on adapter-core (routing/validation concerns)")
            .check(classes)
    }

    private val allFeaturePackages =
        listOf(
            "io.github.rygel.fragments.blog..",
            "io.github.rygel.fragments.rss..",
            "io.github.rygel.fragments.lucene..",
            "io.github.rygel.fragments.sitemap..",
            "io.github.rygel.fragments.image..",
            "io.github.rygel.fragments.navigation..",
            "io.github.rygel.fragments.social..",
            "io.github.rygel.fragments.seo..",
            "io.github.rygel.fragments.chat..",
            "io.github.rygel.fragments.cache..",
        )

    @Test
    fun testFeatureModulesMustNotDependOnOtherFeatureModules() {
        for (feature in allFeaturePackages) {
            val allowed = allowedFeatureDependencies[feature] ?: emptySet()
            val others =
                allFeaturePackages
                    .filter { it != feature && it !in allowed }
                    .toTypedArray()
            noClasses()
                .that()
                .resideInAPackage(feature)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(*others)
                .because(
                    "feature module ${feature.removeSuffix(
                        "..",
                    )} must be self-contained (allowed: ${allowed.map { it.removeSuffix("..") }})",
                ).check(classes)
        }
    }

    @Test
    fun testCliMustNotDependOnFeatureModules() {
        noClasses()
            .that()
            .resideInAPackage("io.github.rygel.fragments.cli..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*allFeaturePackages.toTypedArray())
            .because("CLI generator should only produce project scaffolding, not depend on feature modules")
            .check(classes)
    }

    @Test
    fun testDemoPackagesMustNotAppearInLibraryModules() {
        noClasses()
            .that()
            .resideOutsideOfPackages(
                "io.github.rygel.fragments.cli..",
                "io.github.rygel.fragments.demo..",
            ).should()
            .haveNameMatching(".*Demo.*")
            .because("demo classes belong only in the CLI or demo packages")
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
