package io.github.rygel.fragments.demo.micronaut

import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.Micronaut
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

@Singleton
class DemoApplication {
    private val logger = LoggerFactory.getLogger("DemoApplication")

    @Inject
    lateinit var repository: FragmentRepository

    @Inject
    lateinit var searchEngine: LuceneSearchEngine

    @Inject
    fun onStart(
        micronaut: Micronaut,
        event: StartupEvent,
    ) {
        val fragmentsPath =
            System.getProperty("fragments.path")
                ?: System.getenv("FRAGMENTS_PATH")
                ?: "./content"

        runBlocking {
            repository.reload()
            searchEngine.index()
        }

        logger.info("Starting Micronaut Fragments Demo")
        logger.info("Loading fragments from: $fragmentsPath")
    }
}
