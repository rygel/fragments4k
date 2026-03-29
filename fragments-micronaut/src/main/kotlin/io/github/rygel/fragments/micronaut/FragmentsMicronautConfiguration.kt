package io.github.rygel.fragments.micronaut

import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.static.StaticPageEngine
import jakarta.inject.Singleton

@Singleton
class FragmentsMicronautConfiguration {

    @Singleton
    fun fragmentRepository(): FragmentRepository {
        val fragmentsPath = System.getProperty("fragments.path") ?: System.getenv("FRAGMENTS_PATH") ?: "./content"
        return FileSystemFragmentRepository(fragmentsPath)
    }

    @Singleton
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine {
        return StaticPageEngine(repository)
    }

    @Singleton
    fun blogEngine(repository: FragmentRepository): BlogEngine {
        return BlogEngine(repository)
    }
}
