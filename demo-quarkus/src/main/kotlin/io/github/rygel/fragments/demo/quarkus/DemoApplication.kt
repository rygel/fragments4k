package io.github.rygel.fragments.demo.quarkus

import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.quarkus.FragmentsQuarkusResource
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class DemoApplication {
    
    private val logger = LoggerFactory.getLogger("DemoApplication")

    fun onStart(@Observes event: StartupEvent) {
        val fragmentsPath = System.getProperty("fragments.path")
            ?: System.getenv("FRAGMENTS_PATH")
            ?: "./content"

        logger.info("Starting Quarkus Fragments Demo")
        logger.info("Loading fragments from: $fragmentsPath")
    }
}
