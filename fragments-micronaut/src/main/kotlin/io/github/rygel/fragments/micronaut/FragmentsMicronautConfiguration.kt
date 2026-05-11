package io.github.rygel.fragments.micronaut

import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentTemplates
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.static.StaticPageEngine
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton

@Factory
class FragmentsMicronautConfiguration(
    @Property(name = "fragments.path")
    private val fragmentsPath: String = "./content",
) {
    private lateinit var searchEngineBean: LuceneSearchEngine

    @Singleton
    fun fragmentRepository(): FragmentRepository {
        return FileSystemFragmentRepository(
            basePath = fragmentsPath,
            urlBuilder = { fragment ->
                when (fragment.template) {
                    FragmentTemplates.BLOG, FragmentTemplates.BLOG_POST -> {
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
    fun searchEngine(repository: FragmentRepository): LuceneSearchEngine {
        searchEngineBean = LuceneSearchEngine(repository)
        return searchEngineBean
    }

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

    @PreDestroy
    fun cleanup() {
        if (::searchEngineBean.isInitialized) {
            searchEngineBean.close()
        }
    }
}
