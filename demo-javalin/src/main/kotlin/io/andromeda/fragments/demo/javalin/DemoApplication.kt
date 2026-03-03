package io.andromeda.fragments.demo.javalin

import io.andromeda.fragments.*
import io.andromeda.fragments.javalin.fragmentsRoutes
import io.javalin.Javalin
import io.javalin.rendering.template.JavalinPebble
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

    val app = Javalin.create {
        it.staticFiles.enableWebjars()
        it.fileRenderer(JavalinPebble().prependTemplateLocation("templates"))
    }

    app.fragmentsRoutes(staticEngine, blogEngine)

    app.exception(Exception::class.java) { e, ctx ->
        logger.error("Error handling request", e)
        ctx.status(500).result("Internal Server Error: ${e.message}")
    }

    val port = System.getProperty("server.port")?.toIntOrNull() ?: 8080
    logger.info("Starting Javalin demo server on port $port")
    app.start(port)
}
