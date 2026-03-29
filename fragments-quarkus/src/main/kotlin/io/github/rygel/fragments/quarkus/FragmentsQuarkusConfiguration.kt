package io.github.rygel.fragments.quarkus

import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.static.StaticPageEngine
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class FragmentsQuarkusConfiguration(
    @ConfigProperty(name = "fragments.path", defaultValue = "./content")
    private val fragmentsPath: String
) {

    @Produces
    @ApplicationScoped
    fun fragmentRepository(): FragmentRepository {
        return FileSystemFragmentRepository(fragmentsPath)
    }

    @Produces
    @ApplicationScoped
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine {
        return StaticPageEngine(repository)
    }

    @Produces
    @ApplicationScoped
    fun blogEngine(repository: FragmentRepository): BlogEngine {
        return BlogEngine(repository)
    }
}
