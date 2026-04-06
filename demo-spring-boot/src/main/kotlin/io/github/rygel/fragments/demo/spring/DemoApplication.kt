package io.github.rygel.fragments.demo.spring

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContextException
import org.springframework.context.annotation.ComponentScan
import kotlin.system.exitProcess

@SpringBootApplication
@ComponentScan(basePackages = ["io.github.rygel.fragments.spring", "io.github.rygel.fragments.demo.spring"])
class DemoApplication

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
