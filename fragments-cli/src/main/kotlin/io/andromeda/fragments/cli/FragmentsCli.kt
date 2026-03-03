package io.andromeda.fragments.cli

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@Command(
    name = "fragments",
    mixinStandardHelpOptions = true,
    version = ["Fragments4k CLI 1.0.0-SNAPSHOT"],
    description = ["Command-line tool for scaffolding Fragments4k projects"],
    subcommands = [InitCommand::class, RunCommand::class]
)
class FragmentsCli : Runnable {
    
    override fun run() {
        CommandLine(this).usage(System.out)
    }
    
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(FragmentsCli()).execute(*args)
            System.exit(exitCode)
        }
    }
}

@Command(
    name = "init",
    description = ["Initialize a new Fragments4k project"]
)
class InitCommand : Runnable {
    
    @Parameters(
        index = "0",
        description = ["Project name"]
    )
    private lateinit var projectName: String
    
    @Option(
        names = ["-f", "--framework"],
        description = ["Web framework to use (default: http4k)"],
        defaultValue = "http4k"
    )
    private var framework: String = "http4k"
    
    @Option(
        names = ["-d", "--directory"],
        description = ["Output directory (default: current directory)"],
        defaultValue = "."
    )
    private var directory: String = "."
    
    @Option(
        names = ["-p", "--package"],
        description = ["Package name (default: io.andromeda.fragments.demo)"],
        defaultValue = "io.andromeda.fragments.demo"
    )
    private var packageName: String = "io.andromeda.fragments.demo"
    
    override fun run() {
        val frameworks = listOf("http4k", "javalin", "spring-boot", "quarkus", "micronaut")
        
        if (framework !in frameworks) {
            println("Error: Invalid framework '$framework'")
            println("Valid frameworks: ${frameworks.joinToString(", ")}")
            System.exit(1)
        }
        
        val projectDir = Paths.get(directory, projectName).toFile()
        
        if (projectDir.exists()) {
            println("Error: Directory '$projectName' already exists")
            System.exit(1)
        }
        
        println("Creating Fragments4k project: $projectName")
        println("Framework: $framework")
        println("Package: $packageName")
        println()
        
        try {
            ProjectGenerator.generate(projectDir, projectName, framework, packageName)
            println()
            println("✓ Project created successfully!")
            println()
            println("Next steps:")
            println("  cd $projectName")
            println("  mvn clean install")
            when (framework) {
                "http4k", "javalin", "spring-boot", "micronaut" -> 
                    println("  mvn exec:java -Dexec.mainClass=${packageName}.${getMainClassName(framework)}")
                "quarkus" ->
                    println("  mvn quarkus:dev")
            }
        } catch (e: Exception) {
            println("Error creating project: ${e.message}")
            e.printStackTrace()
            System.exit(1)
        }
    }
    
    private fun getMainClassName(framework: String): String {
        return when (framework) {
            "http4k" -> "DemoApplicationKt"
            "javalin" -> "DemoApplicationKt"
            "spring-boot" -> "DemoApplicationKt"
            "quarkus" -> "DemoApplicationKt"
            "micronaut" -> "DemoApplicationKt"
            else -> "DemoApplicationKt"
        }
    }
}

@Command(
    name = "run",
    description = ["Run the Fragments4k development server"]
)
class RunCommand : Runnable {
    
    @Option(
        names = ["-p", "--port"],
        description = ["Port to listen on (default: 8080)"],
        defaultValue = "8080"
    )
    private var port: Int = 8080
    
    @Option(
        names = ["-d", "--content-dir"],
        description = ["Content directory (default: content)"],
        defaultValue = "content"
    )
    private var contentDir: String = "content"
    
    override fun run() {
        println("Starting development server on port $port")
        println("Content directory: $contentDir")
        println("Note: Live reload feature not yet implemented")
        println()
        println("Use framework-specific commands to run the server:")
        println("  HTTP4k/Javalin: mvn exec:java")
        println("  Spring Boot: mvn spring-boot:run")
        println("  Quarkus: mvn quarkus:dev")
        println("  Micronaut: mvn micronaut:run")
    }
}
