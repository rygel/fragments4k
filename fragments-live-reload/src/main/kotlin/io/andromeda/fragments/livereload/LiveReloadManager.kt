package io.andromeda.fragments.livereload

import io.andromeda.fragments.FragmentRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.concurrent.TimeUnit
import java.io.IOException

class LiveReloadManager(
    private val repository: FragmentRepository,
    private val contentDir: Path
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(LiveReloadManager::class.java)
    }
    
    private val _reloadEvents = MutableSharedFlow<ReloadEvent>()
    val reloadEvents: SharedFlow<ReloadEvent> = _reloadEvents.asSharedFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var watchJob: Job? = null
    
    suspend fun startWatching() {
        logger.info("Starting live reload for content directory: $contentDir")
        
        if (!Files.exists(contentDir)) {
            logger.warn("Content directory does not exist: $contentDir")
            return
        }
        
        val watchService = FileSystems.getDefault().newWatchService()
        val watchKeys = mutableMapOf<Path, WatchKey>()
        
        // Register watch on content directory recursively
        registerWatchRecursively(contentDir, watchService, watchKeys)
        
        watchJob = coroutineScope.launch {
            logger.info("Live reload started for: $contentDir")

            while (isActive) {
                try {
                    val key = watchService.poll(1, TimeUnit.SECONDS)
                    val changedFiles = mutableListOf<Path>()

                    if (key != null) {
                        for (event in key.pollEvents()) {
                            val context = event.context() as Path
                            val fullPath = (key.watchable() as Path).resolve(context)

                            if (event.kind() == ENTRY_CREATE ||
                                event.kind() == ENTRY_MODIFY ||
                                event.kind() == ENTRY_DELETE) {
                                changedFiles.add(fullPath)
                                logger.debug("File changed: $fullPath (${event.kind()})")
                            }
                        }

                        if (!key.reset()) {
                            logger.warn("Watch key for ${key.watchable()} could not be reset")
                            val path = key.watchable() as Path
                            registerWatchRecursively(path, watchService, watchKeys)
                        }
                    }

                    if (changedFiles.isNotEmpty()) {
                        handleChanges(changedFiles)
                    }
                } catch (e: InterruptedException) {
                    logger.info("Live reload stopped")
                    break
                } catch (e: ClosedWatchServiceException) {
                    logger.warn("Watch service closed")
                    break
                } catch (e: Exception) {
                    logger.error("Error watching for changes", e)
                }
            }
        }
    }

    private fun registerWatchRecursively(
        dir: Path,
        watchService: WatchService,
        watchKeys: MutableMap<Path, WatchKey>
    ) {
        if (!Files.isDirectory(dir)) {
            return
        }
        
        // Register current directory
        try {
            val key = dir.register(
                watchService,
                ENTRY_CREATE,
                ENTRY_MODIFY,
                ENTRY_DELETE
            )
            watchKeys[dir] = key
            logger.debug("Registered watch for: $dir")
        } catch (e: IOException) {
            logger.warn("Could not register watch for $dir", e)
            return
        }
        
        // Recursively register subdirectories
        Files.walk(dir).use { stream ->
            stream
                .filter { it != dir && Files.isDirectory(it) }
                .forEach { subDir ->
                    try {
                        val key = subDir.register(
                            watchService,
                            ENTRY_CREATE,
                            ENTRY_MODIFY,
                            ENTRY_DELETE
                        )
                        watchKeys[subDir] = key
                        logger.debug("Registered watch for: $subDir")
                    } catch (e: IOException) {
                        logger.warn("Could not register watch for $subDir", e)
                    }
                }
        }
    }
    
    private suspend fun handleChanges(changedFiles: List<Path>) {
        logger.info("Detected ${changedFiles.size} changed file(s), reloading fragments")
        
        try {
            // Reload fragments from repository
            // This will trigger repository to re-read content from disk
            val count = repository.getAllVisible().size
            logger.info("Reloaded $count fragments")
            
            // Emit reload event
            _reloadEvents.emit(
                ReloadEvent(
                    type = ReloadType.CONTENT,
                    changedFiles = changedFiles,
                    timestamp = System.currentTimeMillis()
                )
            )
            
            logger.info("Fragments reloaded successfully")
        } catch (e: Exception) {
            logger.error("Failed to reload fragments", e)
            _reloadEvents.emit(
                ReloadEvent(
                    type = ReloadType.ERROR,
                    error = e.message,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun stopWatching() {
        logger.info("Stopping live reload")
        watchJob?.cancel()
        watchJob = null
    }
    
    fun isWatching(): Boolean = watchJob?.isActive == true
}

data class ReloadEvent(
    val type: ReloadType,
    val changedFiles: List<Path> = emptyList(),
    val error: String? = null,
    val timestamp: Long
)

enum class ReloadType {
    CONTENT,
    ERROR
}
