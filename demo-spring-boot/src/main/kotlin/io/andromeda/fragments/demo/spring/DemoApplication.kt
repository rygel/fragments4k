package io.andromeda.fragments.demo.spring

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.static.StaticPageEngine
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

@SpringBootApplication
class DemoApplication {

    @Bean
    fun fragmentRepository(): FragmentRepository {
        val fragmentsPath = System.getProperty("fragments.path")
            ?: System.getenv("FRAGMENTS_PATH")
            ?: "./content"
        return FileSystemFragmentRepository(fragmentsPath)
    }

    @Bean
    fun staticPageEngine(repository: FragmentRepository): StaticPageEngine {
        return StaticPageEngine(repository)
    }

    @Bean
    fun blogEngine(repository: FragmentRepository): BlogEngine {
        return BlogEngine(repository)
    }
}

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("DemoApplication")

    val fragmentsPath = System.getProperty("fragments.path")
        ?: System.getenv("FRAGMENTS_PATH")
        ?: "./content"

    logger.info("Starting Fragments Spring Boot Demo")
    logger.info("Loading fragments from: $fragmentsPath")

    try {
        runApplication<DemoApplication>(*args)
    } catch (e: Exception) {
        logger.error("Failed to start application", e)
        exitProcess(1)
    }
}
