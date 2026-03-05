package io.andromeda.fragments.blog.test

import io.andromeda.fragments.ContentRelationshipGenerator
import io.andromeda.fragments.ContentRelationships
import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository
import io.andromeda.fragments.FragmentStatus
import io.andromeda.fragments.RelationshipConfig
import io.andromeda.fragments.StatusChangeHistory
import java.time.LocalDateTime

class InMemoryFragmentRepository : FragmentRepository {
    private val fragments = mutableListOf<Fragment>()

    suspend fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
    }

    override suspend fun getAll(): List<Fragment> = fragments.toList()

    override suspend fun getAllVisible(): List<Fragment> {
        val now = LocalDateTime.now()
        return fragments.filter {
            it.visible && when (it.status) {
                io.andromeda.fragments.FragmentStatus.PUBLISHED -> {
                    it.expiryDate == null || !it.expiryDate!!.isBefore(now)
                }
                io.andromeda.fragments.FragmentStatus.SCHEDULED -> {
                    it.publishDate != null && !it.publishDate!!.isAfter(now) &&
                    (it.expiryDate == null || !it.expiryDate!!.isBefore(now))
                }
                io.andromeda.fragments.FragmentStatus.DRAFT,
                io.andromeda.fragments.FragmentStatus.REVIEW,
                io.andromeda.fragments.FragmentStatus.APPROVED,
                io.andromeda.fragments.FragmentStatus.ARCHIVED,
                io.andromeda.fragments.FragmentStatus.EXPIRED -> false
            }
        }
    }

    override suspend fun getBySlug(slug: String): Fragment? =
        fragments.find { it.slug == slug }

    override suspend fun getByYearMonthAndSlug(year: String, month: String, slug: String): Fragment? {
        return fragments.find {
            it.slug == slug &&
            it.date?.year == year.toIntOrNull() &&
            it.date?.monthValue == month.toIntOrNull()
        }
    }

    override suspend fun getByTag(tag: String): List<Fragment> =
        fragments.filter { it.tags.contains(tag) }

    override suspend fun getByCategory(category: String): List<Fragment> =
        fragments.filter { it.categories.contains(category) }

    override suspend fun getByStatus(status: io.andromeda.fragments.FragmentStatus): List<Fragment> =
        fragments.filter { it.status == status }

    override suspend fun getByAuthor(authorId: String): List<Fragment> =
        fragments.filter { it.authorIds.contains(authorId) || it.author == authorId }

    override suspend fun getByAuthors(authorIds: List<String>): List<Fragment> =
        fragments.filter { fragment ->
            authorIds.any { authorId ->
                fragment.authorIds.contains(authorId) || fragment.author == authorId
            }
        }

    override suspend fun updateFragmentStatus(slug: String, status: io.andromeda.fragments.FragmentStatus, force: Boolean, changedBy: String?, reason: String?): Result<Fragment> {
        val index = fragments.indexOfFirst { it.slug == slug }
        return if (index < 0) {
            Result.failure(IllegalArgumentException("Fragment not found: $slug"))
        } else {
            val fragment = fragments[index]
            if (!force && !io.andromeda.fragments.FragmentStatus.canTransition(fragment.status, status)) {
                Result.failure(
                    IllegalStateException(
                        "Cannot transition from ${fragment.status} to $status. " +
                        "Valid transitions: ${io.andromeda.fragments.FragmentStatus.getValidTransitions(fragment.status)}"
                    )
                )
            } else {
                val statusChange = StatusChangeHistory(
                    fromStatus = fragment.status,
                    toStatus = status,
                    changedBy = changedBy,
                    reason = reason
                )
                fragments[index] = fragment.copy(
                    status = status,
                    statusChangeHistory = fragment.statusChangeHistory + statusChange
                )
                Result.success(fragments[index])
            }
        }
    }

    override suspend fun updateMultipleFragmentsStatus(slugs: List<String>, status: io.andromeda.fragments.FragmentStatus, force: Boolean, changedBy: String?, reason: String?): List<Result<Fragment>> {
        return slugs.map { slug ->
            updateFragmentStatus(slug, status, force, changedBy, reason)
        }
    }

    override suspend fun publishMultiple(slugs: List<String>, changedBy: String?, reason: String?): List<Result<Fragment>> {
        return slugs.map { slug ->
            updateFragmentStatus(slug, io.andromeda.fragments.FragmentStatus.PUBLISHED, force = false, changedBy = changedBy, reason = reason)
        }
    }

    override suspend fun unpublishMultiple(slugs: List<String>, changedBy: String?, reason: String?): List<Result<Fragment>> {
        return slugs.map { slug ->
            updateFragmentStatus(slug, io.andromeda.fragments.FragmentStatus.DRAFT, force = false, changedBy = changedBy, reason = reason)
        }
    }

    override suspend fun archiveMultiple(slugs: List<String>, changedBy: String?, reason: String?): List<Result<Fragment>> {
        return slugs.map { slug ->
            updateFragmentStatus(slug, io.andromeda.fragments.FragmentStatus.ARCHIVED, force = false, changedBy = changedBy, reason = reason)
        }
    }

    override suspend fun getScheduledFragmentsDueForPublication(threshold: LocalDateTime): List<Fragment> {
        return fragments.filter { fragment ->
            fragment.status == io.andromeda.fragments.FragmentStatus.SCHEDULED &&
            fragment.publishDate != null &&
            !fragment.publishDate!!.isAfter(threshold)
        }
    }

    override suspend fun publishScheduledFragments(threshold: LocalDateTime): List<Result<Fragment>> {
        val dueFragments = fragments.filter { fragment ->
            fragment.status == io.andromeda.fragments.FragmentStatus.SCHEDULED &&
            fragment.publishDate != null &&
            !fragment.publishDate!!.isAfter(threshold)
        }

        return dueFragments.map { fragment ->
            val index = fragments.indexOfFirst { it.slug == fragment.slug }
            if (index >= 0) {
                val statusChange = StatusChangeHistory(
                    fromStatus = fragment.status,
                    toStatus = io.andromeda.fragments.FragmentStatus.PUBLISHED,
                    changedBy = "system",
                    reason = "Scheduled publication"
                )
                fragments[index] = fragment.copy(
                    status = io.andromeda.fragments.FragmentStatus.PUBLISHED,
                    statusChangeHistory = fragment.statusChangeHistory + statusChange
                )
                Result.success(fragments[index])
            } else {
                Result.failure(IllegalArgumentException("Fragment not found: ${fragment.slug}"))
            }
        }
    }

    override suspend fun scheduleMultiple(slugs: List<String>, publishDate: LocalDateTime, changedBy: String?, reason: String?): List<Result<Fragment>> {
        return slugs.map { slug ->
            val index = fragments.indexOfFirst { it.slug == slug }
            if (index >= 0) {
                val fragment = fragments[index]
                val statusChange = StatusChangeHistory(
                    fromStatus = fragment.status,
                    toStatus = io.andromeda.fragments.FragmentStatus.SCHEDULED,
                    changedBy = changedBy,
                    reason = reason
                )
                fragments[index] = fragment.copy(
                    status = io.andromeda.fragments.FragmentStatus.SCHEDULED,
                    publishDate = publishDate,
                    statusChangeHistory = fragment.statusChangeHistory + statusChange
                )
                Result.success(fragments[index])
            } else {
                Result.failure(IllegalArgumentException("Fragment not found: $slug"))
            }
        }
    }

    override suspend fun expireFragments(threshold: LocalDateTime): List<Result<Fragment>> {
        val expiredFragments = fragments.filter { fragment ->
            fragment.status == io.andromeda.fragments.FragmentStatus.PUBLISHED &&
            fragment.publishDate != null &&
            fragment.publishDate!!.isBefore(threshold)
        }

        return expiredFragments.map { fragment ->
            val index = fragments.indexOfFirst { it.slug == fragment.slug }
            if (index >= 0) {
                val statusChange = StatusChangeHistory(
                    fromStatus = fragment.status,
                    toStatus = io.andromeda.fragments.FragmentStatus.EXPIRED,
                    changedBy = "system",
                    reason = "Content expired"
                )
                fragments[index] = fragment.copy(
                    status = io.andromeda.fragments.FragmentStatus.EXPIRED,
                    statusChangeHistory = fragment.statusChangeHistory + statusChange
                )
                Result.success(fragments[index])
            } else {
                Result.failure(IllegalArgumentException("Fragment not found: ${fragment.slug}"))
            }
        }
    }

    override suspend fun getFragmentsExpiringSoon(threshold: LocalDateTime): List<Fragment> {
        return fragments.filter { fragment ->
            fragment.expiryDate != null &&
            !fragment.expiryDate!!.isAfter(threshold) &&
            fragment.status == io.andromeda.fragments.FragmentStatus.PUBLISHED
        }
    }

    override suspend fun reload() {
        fragments.clear()
    }

    override suspend fun getRelationships(slug: String, config: RelationshipConfig): ContentRelationships? {
        val currentFragment = getBySlug(slug) ?: return null
        val allFragments = getAllVisible()
            .filter { it.slug != currentFragment.slug }

        return ContentRelationshipGenerator.generateRelationships(
            currentFragment = currentFragment,
            allFragments = allFragments,
            config = config
        )
    }
}
