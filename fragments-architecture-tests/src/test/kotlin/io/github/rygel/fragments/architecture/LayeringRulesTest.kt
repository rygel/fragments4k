package io.github.rygel.fragments.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * Enforces the layered architecture of the library.
 *
 * Layer hierarchy (top → bottom):
 *   Adapters (http4k, javalin, spring, quarkus, micronaut)
 *     ↓ depend on
 *   Feature modules (blog, cache, rss, lucene, sitemap, livereload, chat, navigation, social, seo)
 *     ↓ depend on
 *   Core (fragments-core: domain model, repository interfaces, markdown parsing)
 *
 * Lower layers must never reach up to higher layers.
 */
class LayeringRulesTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("io.github.rygel.fragments")

    // -- Core API contract -----------------------------------------------------

    @Test
    fun `repository interfaces in core must be interfaces not classes`() {
        classes()
            .that()
            .resideInAPackage("io.github.rygel.fragments")
            .and()
            .haveSimpleNameEndingWith("Repository")
            .and()
            .areNotAnonymousClasses()
            .and()
            .doNotHaveSimpleName("FileSystemFragmentRepository")
            .and()
            .doNotHaveSimpleName("FileSystemAuthorRepository")
            .and()
            .doNotHaveSimpleName("FileSystemContentSeriesRepository")
            .and()
            .doNotHaveSimpleName("FileSystemFragmentRevisionRepository")
            .and()
            .doNotHaveSimpleName("InMemoryFragmentRepository")
            .should()
            .beInterfaces()
            .because("core repository contracts must be interfaces so adapters can provide their own implementations")
            .check(classes)
    }

    // -- Feature module isolation from adapters --------------------------------

    @Test
    fun `blog module must not depend on adapters`() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.blog..")
    }

    @Test
    fun `cache module must not depend on adapters`() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.cache..")
    }

    @Test
    fun `rss module must not depend on adapters`() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.rss..")
    }

    @Test
    fun `lucene module must not depend on adapters`() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.lucene..")
    }

    @Test
    fun `sitemap module must not depend on adapters`() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.sitemap..")
    }

    @Test
    fun `livereload module must not depend on adapters`() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.livereload..")
    }

    @Test
    fun `chat module must not depend on adapters`() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.chat..")
    }

    @Test
    fun `navigation module must not depend on adapters`() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.navigation..")
    }

    @Test
    fun `social module must not depend on adapters`() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.social..")
    }

    @Test
    fun `seo module must not depend on adapters`() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.seo..")
    }

    @Test
    fun `image module must not depend on adapters`() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.image..")
    }

    // -- Helpers --------------------------------------------------------------

    private val adapterPackages =
        arrayOf(
            "io.github.rygel.fragments.http4k..",
            "io.github.rygel.fragments.javalin..",
            "io.github.rygel.fragments.spring..",
            "io.github.rygel.fragments.quarkus..",
            "io.github.rygel.fragments.micronaut..",
            "io.github.rygel.fragments.cli..",
        )

    private fun featureModuleMustNotDependOnAdapters(featurePackage: String) {
        noClasses()
            .that()
            .resideInAPackage(featurePackage)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*adapterPackages)
            .because("feature modules sit below adapters in the dependency hierarchy")
            .check(classes)
    }
}
