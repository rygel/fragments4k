package io.andromeda.fragments

import java.time.LocalDateTime

interface PublicationNotificationService {
    suspend fun notifyScheduledPublication(fragment: Fragment, scheduledDate: LocalDateTime)
    suspend fun notifyPublicationReminder(fragment: Fragment, hoursBefore: Int)
    suspend fun notifyPublicationSuccess(fragment: Fragment)
    suspend fun notifyPublicationFailure(fragment: Fragment, error: Throwable)
}

data class PublicationNotification(
    val fragmentSlug: String,
    val fragmentTitle: String,
    val scheduledDate: LocalDateTime,
    val author: String?,
    val notificationType: NotificationType,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class NotificationType {
    SCHEDULED,
    REMINDER,
    SUCCESS,
    FAILURE
}

interface PublicationNotificationListener {
    suspend fun onNotification(notification: PublicationNotification)
}

class NoOpPublicationNotificationService : PublicationNotificationService {
    override suspend fun notifyScheduledPublication(fragment: Fragment, scheduledDate: LocalDateTime) {
    }

    override suspend fun notifyPublicationReminder(fragment: Fragment, hoursBefore: Int) {
    }

    override suspend fun notifyPublicationSuccess(fragment: Fragment) {
    }

    override suspend fun notifyPublicationFailure(fragment: Fragment, error: Throwable) {
    }
}

class LoggingPublicationNotificationService(
    private val listeners: List<PublicationNotificationListener> = emptyList()
) : PublicationNotificationService {

    override suspend fun notifyScheduledPublication(fragment: Fragment, scheduledDate: LocalDateTime) {
        val notification = PublicationNotification(
            fragmentSlug = fragment.slug,
            fragmentTitle = fragment.title,
            scheduledDate = scheduledDate,
            author = fragment.primaryAuthor,
            notificationType = NotificationType.SCHEDULED,
            message = "Fragment '${fragment.title}' scheduled for publication at $scheduledDate"
        )
        listeners.forEach { it.onNotification(notification) }
    }

    override suspend fun notifyPublicationReminder(fragment: Fragment, hoursBefore: Int) {
        val scheduledDate = fragment.publishDate ?: return
        val notification = PublicationNotification(
            fragmentSlug = fragment.slug,
            fragmentTitle = fragment.title,
            scheduledDate = scheduledDate,
            author = fragment.primaryAuthor,
            notificationType = NotificationType.REMINDER,
            message = "Reminder: Fragment '${fragment.title}' will be published in $hoursBefore hours at $scheduledDate"
        )
        listeners.forEach { it.onNotification(notification) }
    }

    override suspend fun notifyPublicationSuccess(fragment: Fragment) {
        val notification = PublicationNotification(
            fragmentSlug = fragment.slug,
            fragmentTitle = fragment.title,
            scheduledDate = fragment.publishDate ?: LocalDateTime.now(),
            author = fragment.primaryAuthor,
            notificationType = NotificationType.SUCCESS,
            message = "Fragment '${fragment.title}' successfully published"
        )
        listeners.forEach { it.onNotification(notification) }
    }

    override suspend fun notifyPublicationFailure(fragment: Fragment, error: Throwable) {
        val notification = PublicationNotification(
            fragmentSlug = fragment.slug,
            fragmentTitle = fragment.title,
            scheduledDate = fragment.publishDate ?: LocalDateTime.now(),
            author = fragment.primaryAuthor,
            notificationType = NotificationType.FAILURE,
            message = "Failed to publish fragment '${fragment.title}': ${error.message}"
        )
        listeners.forEach { it.onNotification(notification) }
    }
}
