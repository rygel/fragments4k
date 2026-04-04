# Fragments Live Reload

Live reload support for Fragments4k development mode.

## Features

- Watch content directory for file changes (create, modify, delete)
- Automatic fragment repository reload
- Recursive directory watching
- Kotlin coroutines support
- Event-based notification system

## Usage

### Basic Setup

1. Add dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.rygel</groupId>
    <artifactId>fragments-live-reload</artifactId>
    <version>0.6.2</version>
</dependency>
```

2. Create a `LiveReloadManager` instance in your application:

```kotlin
import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.livereload.LiveReloadManager
import java.nio.file.Paths

val repository = FileSystemFragmentRepository("content")
val liveReloadManager = LiveReloadManager(
    repository = repository,
    contentDir = Paths.get("content")
)
```

3. Start watching in your application startup:

```kotlin
// In your main function
val liveReloadManager = LiveReloadManager(repository, Paths.get("content"))
liveReloadManager.startWatching()

// Add shutdown hook
Runtime.getRuntime().addShutdownHook(Thread {
    liveReloadManager.stopWatching()
})
```

### CLI Integration

Use the CLI with the `--watch` flag:

```bash
java -jar fragments-cli.jar run --watch
```

## How It Works

1. The `LiveReloadManager` watches the content directory for file changes
2. When changes are detected (create, modify, delete), it triggers:
   - Repository reload to re-read fragments from disk
   - Emission of a `ReloadEvent` to notify listeners
3. The repository automatically refreshes its fragment cache

## Event Types

- `ReloadType.CONTENT`: Content files changed
- `ReloadType.ERROR`: An error occurred during reload

## Example: Integration with HTTP4k

```kotlin
fun main() {
    val repository = FileSystemFragmentRepository("content")
    val staticEngine = StaticPageEngine(repository)
    val blogEngine = BlogEngine(repository)
    
    // Start live reload
    val liveReloadManager = LiveReloadManager(
        repository = repository,
        contentDir = Paths.get("content")
    )
    
    runBlocking {
        liveReloadManager.startWatching()
    }
    
    // Setup server and routes
    val adapter = FragmentsHttp4kAdapter(
        staticEngine = staticEngine,
        blogEngine = blogEngine,
        renderer = renderer,
        siteTitle = "My Blog"
    )
    
    val server = adapter.createRoutes().asServer(Netty(8080))
    
    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            liveReloadManager.stopWatching()
        }
        server.stop()
    })
    
    server.start()
    server.block()
}
```

## Performance Considerations

- Live reload uses Java's WatchService API which is efficient
- Recursive directory watching is implemented by registering each subdirectory
- Debouncing is handled by the file system watcher
- Coroutines ensure non-blocking operations

## Limitations

- Some file systems (e.g., network drives) may not support file watching
- Very rapid file changes may cause multiple reload events
- The repository must support cache invalidation on reload

## Future Enhancements

- WebSocket support for client-side auto-refresh
- Debouncing configuration
- Ignore patterns (e.g., `.git`, `target`, etc.)
- Configurable reload delay
