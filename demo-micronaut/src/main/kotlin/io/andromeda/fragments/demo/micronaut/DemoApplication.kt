package io.andromeda.fragments.demo.micronaut

import io.andromeda.fragments.*
import io.andromeda.fragments.micronaut.FragmentsMicronautController
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.Micronaut
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import jakarta.inject.Inject

@Singleton
class DemoApplication {

    private val logger = LoggerFactory.getLogger("DemoApplication")

    @Inject
    lateinit var controller: FragmentsMicronautController

    @Inject
    fun onStart(micronaut: Micronaut, event: StartupEvent) {
        val fragmentsPath = System.getProperty("fragments.path")
            ?: System.getenv("FRAGMENTS_PATH")
            ?: "./content"

        logger.info("Starting Micronaut Fragments Demo")
        logger.info("Loading fragments from: $fragmentsPath")
        logger.info("Application started: ${micronaut.applicationProperties.name}")
    }
}
