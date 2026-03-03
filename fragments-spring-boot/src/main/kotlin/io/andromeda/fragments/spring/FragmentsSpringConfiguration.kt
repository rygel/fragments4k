package io.andromeda.fragments.spring

import io.andromeda.fragments.FragmentRepository
import io.andromeda.fragments.FileSystemFragmentRepository
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FragmentsSpringConfiguration {

    @Bean
    fun fragmentRepository(): FragmentRepository {
        val fragmentsPath = System.getProperty("fragments.path") ?: System.getenv("FRAGMENTS_PATH") ?: "./content"
        return FileSystemFragmentRepository(fragmentsPath)
    }

    @Bean
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine {
        return StaticPageEngine(repository)
    }

    @Bean
    fun blogEngine(repository: FragmentRepository): BlogEngine {
        return BlogEngine(repository)
    }
}
