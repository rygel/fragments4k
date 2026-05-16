package io.github.rygel.fragments.demo.javalin

import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.FragmentTemplates
import io.github.rygel.fragments.adapter.ErrorResponse
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.javalin.fragmentsRoutes
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.static.StaticPageEngine
import io.javalin.Javalin
import io.javalin.rendering.template.JavalinPebble
import kotlinx.coroutines.runBlocking
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
    val staticEngine = StaticPageEngine(repository)
    val blogEngine = BlogEngine(repository)
    val searchEngine = LuceneSearchEngine(repository, null)

    val engine =
        FragmentsEngine(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            searchEngine = searchEngine,
            siteTitle = "Fragments4k Javalin Demo",
            siteDescription = "A demo blog powered by Fragments4k with Javalin",
            siteUrl = "http://localhost:8080",
        )

    Runtime.getRuntime().addShutdownHook(Thread({ engine.close() }, "fragments-shutdown"))

    val pebble = JavalinPebble()

    val app =
        Javalin.create { config ->
            config.staticFiles.enableWebjars()
            config.fileRenderer(pebble)
            config.routes.fragmentsRoutes(engine, renderer = PebbleTemplateRenderer())
            config.routes.exception(Exception::class.java) { e, ctx ->
                when (e) {
                    is IllegalArgumentException -> {
                        logger.warn("Bad request: {}", e.message)
                        ctx.status(400).json(ErrorResponse(400, "Bad Request", e.message ?: "Invalid request"))
                    }

                    is NoSuchElementException -> {
                        logger.warn("Not found: {}", e.message)
                        ctx.status(404).json(ErrorResponse(404, "Not Found", e.message ?: "Resource not found"))
                    }

                    else -> {
                        logger.error("Unhandled exception", e)
                        ctx.status(500).json(ErrorResponse(500, "Internal Server Error", "An unexpected error occurred"))
                    }
                }
            }
        }

    runBlocking {
        repository.reload()
        searchEngine.index()
    }

    val port = System.getProperty("server.port")?.toIntOrNull() ?: 8080
    logger.info("Starting Javalin demo server on port $port")
    logger.info("RSS feed available at: http://localhost:$port/rss.xml")

    app.start(port)
}
