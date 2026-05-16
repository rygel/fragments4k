package io.github.rygel.fragments

import java.time.LocalDateTime
import java.time.ZoneOffset

interface ScheduledPublicationJob {
    suspend fun execute(threshold: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)): ScheduledPublicationResult
}

data class ScheduledPublicationResult(
    val published: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
    val executionTime: Long = 0,
    val errors: List<String> = emptyList(),
) {
    val total: Int
        get() = published + failed + skipped

    val success: Boolean
        get() = failed == 0
}

interface ScheduledPublicationListener {
    suspend fun onBeforePublications(fragments: List<Fragment>)

    suspend fun onAfterPublications(result: ScheduledPublicationResult)

    suspend fun onPublicationError(
        fragment: Fragment,
        error: Throwable,
    )
}

class DefaultScheduledPublicationJob(
    private val repository: FragmentRepository,
    private val listeners: List<ScheduledPublicationListener> = emptyList(),
) : ScheduledPublicationJob {
    override suspend fun execute(threshold: LocalDateTime): ScheduledPublicationResult {
        val startTime = System.currentTimeMillis()
        var publishedCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()

        val fragmentsToPublish = repository.getScheduledFragmentsDueForPublication(threshold)
        val slugToFragment = fragmentsToPublish.associateBy { it.slug }

        if (fragmentsToPublish.isEmpty()) {
            return ScheduledPublicationResult(
                skipped = 0,
                executionTime = System.currentTimeMillis() - startTime,
            )
        }

        listeners.forEach { it.onBeforePublications(fragmentsToPublish) }

        val results = repository.publishScheduledFragments(threshold)

        results.forEach { result ->
            if (result.isSuccess) {
                publishedCount++
            } else {
                failedCount++
                val error = result.exceptionOrNull()
                if (error != null) {
                    errors.add(error.message ?: "Unknown error")
                    val failedSlug = result.getOrNull()?.slug ?: "unknown"
                    val failedFragment = slugToFragment[failedSlug]
                    if (failedFragment != null) {
                        listeners.forEach { listener ->
                            listener.onPublicationError(failedFragment, error)
                        }
                    }
                }
            }
        }

        val finalResult =
            ScheduledPublicationResult(
                published = publishedCount,
                failed = failedCount,
                executionTime = System.currentTimeMillis() - startTime,
                errors = errors,
            )

        listeners.forEach { it.onAfterPublications(finalResult) }

        return finalResult
    }
}
