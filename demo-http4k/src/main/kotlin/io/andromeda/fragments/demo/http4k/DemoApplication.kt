package io.andromeda.fragments.demo.http4k

import io.andromeda.fragments.FileSystemFragmentRepository
import io.andromeda.fragments.http4k.FragmentsHttp4kAdapter
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.lucene.LuceneSearchEngine
import io.andromeda.fragments.static.StaticPageEngine
import kotlinx.coroutines.runBlocking
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.http4k.template.PebbleTemplates
import org.slf4j.LoggerFactory
import java.nio.file.Files

fun main() {
    val logger = LoggerFactory.getLogger("DemoApplication")
    
    val fragmentsPath = System.getProperty("fragments.path")
        ?: System.getenv("FRAGMENTS_PATH")
        ?: "./content"

    logger.info("Loading fragments from: $fragmentsPath")

    val repository = FileSystemFragmentRepository(fragmentsPath)
    val staticEngine = StaticPageEngine(repository)
    val blogEngine = BlogEngine(repository)
    
    runBlocking {
        val searchEngine = LuceneSearchEngine(repository, null)
        searchEngine.index()
        
        val renderer = PebbleTemplates().HotReload("src/main/resources/templates")
        val adapter = FragmentsHttp4kAdapter(
            staticEngine = staticEngine,
            blogEngine = blogEngine,
            renderer = renderer,
            searchEngine = searchEngine,
            siteTitle = "Fragments4k HTTP4k Demo",
            siteDescription = "A demo blog powered by Fragments4k with HTTP4k",
            siteUrl = "http://localhost:8080"
        )

        val errorHandler: Filter = CatchAll { e ->
            logger.error("Error handling request", e)
            Response(Status.INTERNAL_SERVER_ERROR).body("Internal Server Error: ${e.message}")
        }
        
        val app: HttpHandler = errorHandler.then(adapter.createRoutes())

        logger.info("Starting HTTP4k demo server on port 8080")
        logger.info("RSS feed available at: http://localhost:8080/rss.xml")
        
        app.asServer(Netty(8080)).start()
    }
}
