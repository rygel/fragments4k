package io.github.rygel.fragments.micronaut

import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.static.StaticPageEngine
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class FragmentsMicronautConfiguration {
    @Singleton
    fun fragmentRepository(): FragmentRepository {
        val fragmentsPath = System.getProperty("fragments.path") ?: System.getenv("FRAGMENTS_PATH") ?: "./content"
        return FileSystemFragmentRepository(
            basePath = fragmentsPath,
            urlBuilder = { fragment ->
                when (fragment.template) {
                    "blog", "blog_post" -> {
                        val date = fragment.date ?: return@FileSystemFragmentRepository "/${fragment.slug}"
                        "/blog/${date.year}/${"%02d".format(date.monthValue)}/${fragment.slug}"
                    }

                    else -> {
                        "/page/${fragment.slug}"
                    }
                }
            },
        )
    }

    @Singleton
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine = StaticPageEngine(repository)

    @Singleton
    fun blogEngine(repository: FragmentRepository): BlogEngine = BlogEngine(repository)

    @Singleton
    fun searchEngine(repository: FragmentRepository): LuceneSearchEngine = LuceneSearchEngine(repository)

    @Singleton
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
