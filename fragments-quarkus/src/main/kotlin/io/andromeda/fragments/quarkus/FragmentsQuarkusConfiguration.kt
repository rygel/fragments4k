package io.andromeda.fragments.quarkus

import io.andromeda.fragments.FragmentRepository
import io.andromeda.fragments.FileSystemFragmentRepository
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
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
