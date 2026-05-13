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
    fun testRepositoryInterfacesInCoreMustBeInterfacesNotClasses() {
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
            .and()
            .doNotHaveSimpleName("ClasspathFragmentRepository")
            .should()
            .beInterfaces()
            .because("core repository contracts must be interfaces so adapters can provide their own implementations")
            .check(classes)
    }

    // -- Feature module isolation from adapters --------------------------------

    @Test
    fun testBlogModuleMustNotDependOnAdapters() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.blog..")
    }

    @Test
    fun testCacheModuleMustNotDependOnAdapters() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.cache..")
    }

    @Test
    fun testRssModuleMustNotDependOnAdapters() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.rss..")
    }

    @Test
    fun testLuceneModuleMustNotDependOnAdapters() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.lucene..")
    }

    @Test
    fun testSitemapModuleMustNotDependOnAdapters() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.sitemap..")
    }

    @Test
    fun testLivereloadModuleMustNotDependOnAdapters() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.livereload..")
    }

    @Test
    fun testChatModuleMustNotDependOnAdapters() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.chat..")
    }

    @Test
    fun testNavigationModuleMustNotDependOnAdapters() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.navigation..")
    }

    @Test
    fun testSocialModuleMustNotDependOnAdapters() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.social..")
    }

    @Test
    fun testSeoModuleMustNotDependOnAdapters() {
        featureModuleMustNotDependOnAdapters("io.github.rygel.fragments.seo..")
    }

    @Test
    fun testImageModuleMustNotDependOnAdapters() {
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
