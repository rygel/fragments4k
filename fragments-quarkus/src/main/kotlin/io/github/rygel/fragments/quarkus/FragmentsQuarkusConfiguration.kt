package io.github.rygel.fragments.quarkus

import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.static.StaticPageEngine
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class FragmentsQuarkusConfiguration(
    @field:ConfigProperty(name = "fragments.path", defaultValue = "./content")
    private val fragmentsPath: String,
) {
    @Produces
    @ApplicationScoped
    fun fragmentRepository(): FragmentRepository =
        FileSystemFragmentRepository(
            basePath = fragmentsPath,
            urlBuilder = { fragment ->
                when (fragment.template) {
                    "blog", "blog_post" -> {
                        val date = fragment.date ?: return@FileSystemFragmentRepository "/${fragment.slug}"
                        "/blog/${date.year}/${"%02d".format(date.monthValue)}/${fragment.slug}"
                    }
                    else -> "/page/${fragment.slug}"
                }
            },
        )

    @Produces
    @ApplicationScoped
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine = StaticPageEngine(repository)

    @Produces
    @ApplicationScoped
    fun blogEngine(repository: FragmentRepository): BlogEngine = BlogEngine(repository)

    @Produces
    @ApplicationScoped
    fun searchEngine(repository: FragmentRepository): LuceneSearchEngine = LuceneSearchEngine(repository)

    @Produces
    @ApplicationScoped
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
