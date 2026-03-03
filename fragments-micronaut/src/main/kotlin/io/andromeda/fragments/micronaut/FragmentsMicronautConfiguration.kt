package io.andromeda.fragments.micronaut

import io.andromeda.fragments.FragmentRepository
import io.andromeda.fragments.FileSystemFragmentRepository
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
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
