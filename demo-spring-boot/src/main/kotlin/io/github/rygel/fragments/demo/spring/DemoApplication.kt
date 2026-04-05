package io.github.rygel.fragments.demo.spring

import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.static.StaticPageEngine
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContextException
import org.springframework.context.annotation.Bean
import kotlin.system.exitProcess

@SpringBootApplication
class DemoApplication {
    @Bean
    fun fragmentRepository(): FragmentRepository {
        val fragmentsPath =
            System.getProperty("fragments.path")
                ?: System.getenv("FRAGMENTS_PATH")
                ?: "./content"
        return FileSystemFragmentRepository(fragmentsPath)
    }

    @Bean
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine = StaticPageEngine(repository)

    @Bean
    fun blogEngine(repository: FragmentRepository): BlogEngine = BlogEngine(repository)
}

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("DemoApplication")

    val fragmentsPath =
        System.getProperty("fragments.path")
            ?: System.getenv("FRAGMENTS_PATH")
            ?: "./content"

    logger.info("Starting Fragments Spring Boot Demo")
    logger.info("Loading fragments from: $fragmentsPath")

    try {
        runApplication<DemoApplication>(*args)
    } catch (e: ApplicationContextException) {
        logger.error("Failed to start application", e)
        exitProcess(1)
    }
}
