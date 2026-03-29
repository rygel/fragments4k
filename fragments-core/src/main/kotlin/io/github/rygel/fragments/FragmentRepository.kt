package io.github.rygel.fragments

import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Primary read/write contract for accessing and managing [Fragment] instances.
 *
 * The default implementation is [FileSystemFragmentRepository], which reads Markdown
 * files from a directory. Custom implementations can back this interface with a
 * database, CMS API, or in-memory store — use `fragments-test-data-factories` for
 * an `InMemoryFragmentRepository` suitable for tests.
 *
 * All operations are `suspend` functions and are safe to call from coroutine
 * contexts. Implementations are expected to run I/O on [kotlinx.coroutines.Dispatchers.IO].
 */
interface FragmentRepository {

    /** Returns every fragment regardless of status or visibility. */
    suspend fun getAll(): List<Fragment>

    /**
     * Returns only the fragments that are currently visible to readers: status must
     * be [FragmentStatus.PUBLISHED] (or [FragmentStatus.SCHEDULED] with a past
     * [Fragment.publishDate]), [Fragment.visible] must be `true`, and
     * [Fragment.expiryDate] must not be in the past. Results are sorted by
     * [Fragment.date] descending.
     */
    suspend fun getAllVisible(): List<Fragment>

    /** Finds a single fragment by its [Fragment.slug], or `null` if not found. */
    suspend fun getBySlug(slug: String): Fragment?

    /**
     * Finds a fragment by year, month, and slug — used for date-based blog URLs
     * such as `/2024/01/hello-world`.
     */
    suspend fun getByYearMonthAndSlug(year: String, month: String, slug: String): Fragment?

    /** Returns all fragments that carry the given [tag] (case-insensitive). */
    suspend fun getByTag(tag: String): List<Fragment>

    /** Returns all fragments in the given [category] (case-insensitive). */
    suspend fun getByCategory(category: String): List<Fragment>

    /** Returns all fragments in the given [status]. */
    suspend fun getByStatus(status: FragmentStatus): List<Fragment>

    /** Returns all fragments where [authorId] appears in [Fragment.authorIds] or [Fragment.author]. */
    suspend fun getByAuthor(authorId: String): List<Fragment>

    /** Returns all fragments authored by any of the given [authorIds]. */
    suspend fun getByAuthors(authorIds: List<String>): List<Fragment>

    /**
     * Transitions a single fragment to [status] and persists the change.
     *
     * The transition is validated against the lifecycle state machine unless
     * [force] is `true`. On success the updated [Fragment] is returned; on
     * failure (fragment not found, invalid transition, I/O error) a [Result.failure]
     * is returned — never throws.
     *
     * @param slug Identifies the fragment to update.
     * @param status The target [FragmentStatus].
     * @param force When `true`, bypasses lifecycle transition validation.
     * @param changedBy Actor identifier recorded in [StatusChangeHistory].
     * @param reason Human-readable reason recorded in [StatusChangeHistory].
     */
    suspend fun updateFragmentStatus(
        slug: String,
        status: FragmentStatus,
        force: Boolean = false,
        changedBy: String? = null,
        reason: String? = null,
    ): Result<Fragment>

    /** Bulk variant of [updateFragmentStatus]; returns one [Result] per slug. */
    suspend fun updateMultipleFragmentsStatus(
        slugs: List<String>,
        status: FragmentStatus,
        force: Boolean = false,
        changedBy: String? = null,
        reason: String? = null,
    ): List<Result<Fragment>>

    /** Convenience bulk operation: transitions each slug to [FragmentStatus.PUBLISHED]. */
    suspend fun publishMultiple(slugs: List<String>, changedBy: String? = null, reason: String? = null): List<Result<Fragment>>

    /** Convenience bulk operation: transitions each slug back to [FragmentStatus.DRAFT]. */
    suspend fun unpublishMultiple(slugs: List<String>, changedBy: String? = null, reason: String? = null): List<Result<Fragment>>

    /** Convenience bulk operation: transitions each slug to [FragmentStatus.ARCHIVED]. */
    suspend fun archiveMultiple(slugs: List<String>, changedBy: String? = null, reason: String? = null): List<Result<Fragment>>

    /**
     * Returns all [FragmentStatus.SCHEDULED] fragments whose [Fragment.publishDate]
     * is on or before [threshold] (defaults to now).
     */
    suspend fun getScheduledFragmentsDueForPublication(threshold: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)): List<Fragment>

    /**
     * Publishes all scheduled fragments that are due at [threshold].
     * Intended to be called from a scheduled job (see [ScheduledPublicationJob]).
     */
    suspend fun publishScheduledFragments(threshold: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)): List<Result<Fragment>>

    /**
     * Transitions multiple fragments to [FragmentStatus.SCHEDULED] with the given [publishDate].
     *
     * @param publishDate The date/time at which the fragments should go live.
     */
    suspend fun scheduleMultiple(
        slugs: List<String>,
        publishDate: LocalDateTime,
        changedBy: String? = null,
        reason: String? = null,
    ): List<Result<Fragment>>

    /**
     * Transitions any published fragments whose [Fragment.expiryDate] is before [threshold]
     * to [FragmentStatus.EXPIRED].
     */
    suspend fun expireFragments(threshold: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)): List<Result<Fragment>>

    /**
     * Returns published fragments that will expire before [threshold]
     * (defaults to 7 days from now), useful for sending expiry warnings.
     */
    suspend fun getFragmentsExpiringSoon(threshold: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC).plusDays(7)): List<Fragment>

    /**
     * Forces the repository to re-read content from its backing store, discarding
     * any in-memory cache. Call this after external content changes (e.g. live-reload).
     */
    suspend fun reload()

    /**
     * Returns the [ContentRelationships] for the fragment identified by [slug],
     * or `null` if the fragment does not exist.
     *
     * @param config Tuning parameters for relationship discovery (max counts, thresholds).
     */
    suspend fun getRelationships(slug: String, config: RelationshipConfig = RelationshipConfig()): ContentRelationships?

    /**
     * Snapshots the current state of the fragment as a [FragmentRevision] for
     * version history / rollback purposes.
     */
    suspend fun createRevision(slug: String, changedBy: String? = null, reason: String? = null): Result<FragmentRevision>

    /** Returns all stored revisions for the given fragment, oldest first. */
    suspend fun getFragmentRevisions(slug: String): List<FragmentRevision>

    /**
     * Reverts the fragment to the content captured in revision [revisionId].
     * The current state is NOT automatically snapshotted before reverting — call
     * [createRevision] first if you need a safety checkpoint.
     */
    suspend fun revertToRevision(slug: String, revisionId: String, changedBy: String? = null, reason: String? = null): Result<Fragment>
}
