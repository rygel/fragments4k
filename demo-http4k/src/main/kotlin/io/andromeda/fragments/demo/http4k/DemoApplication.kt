package io.andromeda.fragments.demo.http4k

import io.andromeda.fragments.FileSystemFragmentRepository
import io.andromeda.fragments.http4k.FragmentsHttp4kAdapter
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.server.asServer
import org.http4k.server.netty.Netty
import org.http4k.template.PebbleTemplates
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("DemoApplication")

    val fragmentsPath = System.getProperty("fragments.path")
        ?: System.getenv("FRAGMENTS_PATH")
        ?: "./content"

    logger.info("Loading fragments from: $fragmentsPath")

    val repository = FileSystemFragmentRepository(fragmentsPath)
    val staticEngine = StaticPageEngine(repository)
    val blogEngine = BlogEngine(repository)

    val renderer = PebbleTemplates().HotReload("src/main/resources/templates")
    val adapter = FragmentsHttp4kAdapter(
        staticEngine = staticEngine,
        blogEngine = blogEngine,
        renderer = renderer,
        siteTitle = "Fragments4k HTTP4k Demo",
        siteDescription = "A demo blog powered by Fragments4k with HTTP4k",
        siteUrl = "http://localhost:8080"
    )

    val server = CatchAll { e ->
        logger.error("Error handling request", e)
        org.http4k.core.Response(org.http4k.core.Status.INTERNAL_SERVER_ERROR).body("Internal Server Error: ${e.message}")
    }.then(adapter.createRoutes()).asServer(Netty(8080))

    logger.info("Starting HTTP4k demo server on port 8080")
    logger.info("RSS feed available at: http://localhost:8080/rss.xml")
    server.start()
    server.block()
}

