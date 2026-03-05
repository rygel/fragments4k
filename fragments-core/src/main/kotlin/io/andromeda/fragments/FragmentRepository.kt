package io.andromeda.fragments

import java.time.LocalDateTime

interface FragmentRepository {
    suspend fun getAll(): List<Fragment>
    suspend fun getAllVisible(): List<Fragment>
    suspend fun getBySlug(slug: String): Fragment?
    suspend fun getByYearMonthAndSlug(year: String, month: String, slug: String): Fragment?
    suspend fun getByTag(tag: String): List<Fragment>
    suspend fun getByCategory(category: String): List<Fragment>
    suspend fun getByStatus(status: io.andromeda.fragments.FragmentStatus): List<Fragment>
    suspend fun getByAuthor(authorId: String): List<Fragment>
    suspend fun getByAuthors(authorIds: List<String>): List<Fragment>
    suspend fun updateFragmentStatus(slug: String, status: io.andromeda.fragments.FragmentStatus, force: Boolean = false, changedBy: String? = null, reason: String? = null): Result<Fragment>
    suspend fun updateMultipleFragmentsStatus(slugs: List<String>, status: io.andromeda.fragments.FragmentStatus, force: Boolean = false, changedBy: String? = null, reason: String? = null): List<Result<Fragment>>
    suspend fun publishMultiple(slugs: List<String>, changedBy: String? = null, reason: String? = null): List<Result<Fragment>>
    suspend fun unpublishMultiple(slugs: List<String>, changedBy: String? = null, reason: String? = null): List<Result<Fragment>>
    suspend fun archiveMultiple(slugs: List<String>, changedBy: String? = null, reason: String? = null): List<Result<Fragment>>
    suspend fun getScheduledFragmentsDueForPublication(threshold: LocalDateTime = LocalDateTime.now()): List<Fragment>
    suspend fun publishScheduledFragments(threshold: LocalDateTime = LocalDateTime.now()): List<Result<Fragment>>
    suspend fun scheduleMultiple(slugs: List<String>, publishDate: LocalDateTime, changedBy: String? = null, reason: String? = null): List<Result<Fragment>>
    suspend fun expireFragments(threshold: LocalDateTime = LocalDateTime.now()): List<Result<Fragment>>
    suspend fun getFragmentsExpiringSoon(threshold: LocalDateTime = LocalDateTime.now().plusDays(7)): List<Fragment>
    suspend fun reload()
    suspend fun getRelationships(slug: String, config: RelationshipConfig = RelationshipConfig()): ContentRelationships?
    suspend fun createRevision(slug: String, changedBy: String? = null, reason: String? = null): Result<FragmentRevision>
    suspend fun getFragmentRevisions(slug: String): List<FragmentRevision>
    suspend fun revertToRevision(slug: String, revisionId: String, changedBy: String? = null, reason: String? = null): Result<Fragment>
}
