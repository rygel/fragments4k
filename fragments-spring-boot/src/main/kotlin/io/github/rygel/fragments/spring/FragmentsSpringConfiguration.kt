package io.github.rygel.fragments.spring

import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.static.StaticPageEngine
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
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine = StaticPageEngine(repository)

    @Bean
    fun blogEngine(repository: FragmentRepository): BlogEngine = BlogEngine(repository)

    @Bean
    fun searchEngine(repository: FragmentRepository): LuceneSearchEngine = LuceneSearchEngine(repository)

    @Bean
    fun fragmentsEngine(
        staticEngine: StaticPageEngine,
        blogEngine: BlogEngine,
        searchEngine: LuceneSearchEngine,
    ): FragmentsEngine =
        FragmentsEngine(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            searchEngine = searchEngine,
        )
}
