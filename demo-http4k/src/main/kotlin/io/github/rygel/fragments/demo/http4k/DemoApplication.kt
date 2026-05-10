package io.github.rygel.fragments.demo.http4k

import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.http4k.FragmentsHttp4kAdapter
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.static.StaticPageEngine
import kotlinx.coroutines.runBlocking
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.http4k.template.PebbleTemplates
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("DemoApplication")

    val fragmentsPath =
        System.getProperty("fragments.path")
            ?: System.getenv("FRAGMENTS_PATH")
            ?: "./content"

    logger.info("Loading fragments from: $fragmentsPath")

    val repository =
        FileSystemFragmentRepository(
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
    val staticEngine = StaticPageEngine(repository)
    val blogEngine = BlogEngine(repository)

    runBlocking {
        val searchEngine = LuceneSearchEngine(repository, null)
        searchEngine.index()

        val engine =
            FragmentsEngine(
                staticEngine = staticEngine,
                blogEngine = blogEngine,
                searchEngine = searchEngine,
                siteTitle = "Fragments4k HTTP4k Demo",
                siteDescription = "A demo blog powered by Fragments4k with HTTP4k",
                siteUrl = "http://localhost:8080",
            )

        val renderer = PebbleTemplates().HotReload("src/main/resources/templates")
        val adapter = FragmentsHttp4kAdapter(engine, renderer)

        val errorHandler: Filter =
            CatchAll { e ->
                logger.error("Unhandled exception", e)
                Response(Status.INTERNAL_SERVER_ERROR).body("Internal Server Error")
            }

        val app: HttpHandler = errorHandler.then(adapter.createRoutes())

        logger.info("Starting HTTP4k demo server on port 8080")
        logger.info("RSS feed available at: http://localhost:8080/rss.xml")

        app.asServer(Netty(8080)).start()
    }
}
