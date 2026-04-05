package io.github.rygel.fragments.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test

/**
 * Enforces naming conventions across the codebase.
 *
 * Consistent naming makes the library predictable for consumers.
 */
class NamingConventionRulesTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("io.github.rygel.fragments")

    @Test
    fun `repository interfaces must end with Repository`() {
        classes()
            .that()
            .resideInAPackage("io.github.rygel.fragments..")
            .and()
            .areInterfaces()
            .and()
            .haveSimpleNameContaining("Repositor")
            .should()
            .haveSimpleNameEndingWith("Repository")
            .because("repository interfaces follow the Repository suffix convention")
            .check(classes)
    }

    @Test
    fun `classes implementing repository interfaces must end with Repository`() {
        classes()
            .that()
            .implement(io.github.rygel.fragments.FragmentRepository::class.java)
            .should()
            .haveSimpleNameEndingWith("Repository")
            .because("repository implementations should be clearly identifiable")
            .check(classes)
    }

    @Test
    fun `factory classes must end with Factory or Generator`() {
        classes()
            .that()
            .resideInAPackage("io.github.rygel.fragments..")
            .and()
            .haveSimpleNameContaining("Factor")
            .should()
            .haveSimpleNameEndingWith("Factory")
            .because("factory classes follow the Factory suffix convention")
            .check(classes)
    }
}
