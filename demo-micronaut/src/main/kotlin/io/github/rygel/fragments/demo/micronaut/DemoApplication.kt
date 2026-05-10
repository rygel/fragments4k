package io.github.rygel.fragments.demo.micronaut

import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.Micronaut
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class DemoApplication {
    private val logger = LoggerFactory.getLogger("DemoApplication")

    @Inject
    fun onStart(
        micronaut: Micronaut,
        event: StartupEvent,
    ) {
        val fragmentsPath =
            System.getProperty("fragments.path")
                ?: System.getenv("FRAGMENTS_PATH")
                ?: "./content"

        logger.info("Starting Micronaut Fragments Demo")
        logger.info("Loading fragments from: $fragmentsPath")
    }
}
