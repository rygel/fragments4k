package io.github.rygel.fragments.spring

import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentTemplates
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.static.StaticPageEngine
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FragmentsSpringConfiguration(
    @Value("\${fragments.path:./content}")
    private val fragmentsPath: String,
) {
    private lateinit var searchEngineBean: LuceneSearchEngine

    @Bean
    @ConditionalOnMissingBean
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

    @Bean
    @ConditionalOnMissingBean
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine = StaticPageEngine(repository)

    @Bean
    @ConditionalOnMissingBean
    fun blogEngine(repository: FragmentRepository): BlogEngine = BlogEngine(repository)

    @Bean
    @ConditionalOnMissingBean
    fun searchEngine(repository: FragmentRepository): LuceneSearchEngine {
        searchEngineBean = LuceneSearchEngine(repository)
        return searchEngineBean
    }

    @Bean
    @ConditionalOnMissingBean
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
