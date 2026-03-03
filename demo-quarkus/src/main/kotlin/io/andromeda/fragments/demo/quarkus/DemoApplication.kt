package io.andromeda.fragments.demo.quarkus

import io.andromeda.fragments.*
import io.andromeda.fragments.quarkus.FragmentsQuarkusResource
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class DemoApplication {

    private val logger = LoggerFactory.getLogger("DemoApplication")

    @Observes
    fun onStart(@Observes event: StartupEvent) {
        val fragmentsPath = System.getProperty("fragments.path")
            ?: System.getenv("FRAGMENTS_PATH")
            ?: "./content"

        logger.info("Starting Quarkus Fragments Demo")
        logger.info("Loading fragments from: $fragmentsPath")
    }
}

@Singleton
class FragmentsPathProvider {
    fun getFragmentsPath(): String {
        return System.getProperty("fragments.path")
            ?: System.getenv("FRAGMENTS_PATH")
            ?: "./content"
    }
}
