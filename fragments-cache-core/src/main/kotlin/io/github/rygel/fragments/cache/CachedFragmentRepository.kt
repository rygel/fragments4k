package io.github.rygel.fragments.cache

import io.github.rygel.fragments.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * Cached repository decorator that adds caching to FragmentRepository operations
 */
class CachedFragmentRepository(
    private val delegate: FragmentRepository,
    private val fragmentCache: FragmentCache
) : FragmentRepository {
    
    private val logger = org.slf4j.LoggerFactory.getLogger(CachedFragmentRepository::class.java)
    
    override suspend fun getAll(): List<Fragment> {
        logger.debug("Getting all fragments")
        return delegate.getAll()
    }
    
    override suspend fun getAllVisible(): List<Fragment> {
        logger.debug("Getting all visible fragments")
        
        val cached = fragmentCache.getVisibleFragments()
        if (cached != null) {
            logger.debug("Cache hit for visible fragments")
            return cached
        }
        
        logger.debug("Cache miss for visible fragments, loading from repository")
        val fragments = delegate.getAllVisible()
        
        fragmentCache.putVisibleFragments(fragments)
        
        fragments.forEach { fragment ->
            fragmentCache.putFragment(fragment)
        }
        
        return fragments
    }
    
    override suspend fun getBySlug(slug: String): Fragment? {
        logger.debug("Getting fragment by slug: $slug")
        
        val cached = fragmentCache.getFragment(slug)
        if (cached != null) {
            logger.debug("Cache hit for fragment: $slug")
            return cached
        }
        
        logger.debug("Cache miss for fragment: $slug, loading from repository")
        val fragment = delegate.getBySlug(slug)
        
        if (fragment != null && fragment.isPublished) {
            fragmentCache.putFragment(fragment)
        }
        
        return fragment
    }
    
    override suspend fun getByYearMonthAndSlug(year: String, month: String, slug: String): Fragment? {
        logger.debug("Getting fragment by year/month/slug: $year/$month/$slug")
        return delegate.getByYearMonthAndSlug(year, month, slug)
    }
    
    override suspend fun getByTag(tag: String): List<Fragment> {
        logger.debug("Getting fragments by tag: $tag")
        
        val cached = fragmentCache.getFragmentsByTag(tag)
        if (cached != null) {
            logger.debug("Cache hit for tag: $tag")
            return cached
        }
        
        logger.debug("Cache miss for tag: $tag, loading from repository")
        val fragments = delegate.getByTag(tag)
        
        fragmentCache.putFragmentsByTag(tag, fragments)
        
        return fragments
    }
    
    override suspend fun getByCategory(category: String): List<Fragment> {
        logger.debug("Getting fragments by category: $category")
        
        val cached = fragmentCache.getFragmentsByCategory(category)
        if (cached != null) {
            logger.debug("Cache hit for category: $category")
            return cached
        }
        
        logger.debug("Cache miss for category: $category, loading from repository")
        val fragments = delegate.getByCategory(category)
        
        fragmentCache.putFragmentsByCategory(category, fragments)
        
        return fragments
    }
    
    override suspend fun getByStatus(status: FragmentStatus): List<Fragment> {
        logger.debug("Getting fragments by status: $status")
        return delegate.getByStatus(status)
    }
    
    override suspend fun getByAuthor(authorId: String): List<Fragment> {
        logger.debug("Getting fragments by author: $authorId")
        
        val cached = fragmentCache.getFragmentsByAuthor(authorId)
        if (cached != null) {
            logger.debug("Cache hit for author: $authorId")
            return cached
        }
        
        logger.debug("Cache miss for author: $authorId, loading from repository")
        val fragments = delegate.getByAuthor(authorId)
        
        fragmentCache.putFragmentsByAuthor(authorId, fragments)
        
        return fragments
    }
    
    override suspend fun getByAuthors(authorIds: List<String>): List<Fragment> {
        logger.debug("Getting fragments by authors: ${authorIds.size}")
        return delegate.getByAuthors(authorIds)
    }
    
    override suspend fun updateFragmentStatus(
        slug: String,
        status: FragmentStatus,
        force: Boolean,
        changedBy: String?,
        reason: String?
    ): Result<Fragment> {
        logger.debug("Updating fragment status: $slug -> $status")
        
        val result = delegate.updateFragmentStatus(slug, status, force, changedBy, reason)
        
        if (result.isSuccess) {
            fragmentCache.invalidateFragment(slug)
            fragmentCache.invalidateFragmentLists()
        }
        
        return result
    }
    
    override suspend fun updateMultipleFragmentsStatus(
        slugs: List<String>,
        status: FragmentStatus,
        force: Boolean,
        changedBy: String?,
        reason: String?
    ): List<Result<Fragment>> {
        logger.debug("Updating status for ${slugs.size} fragments: $status")
        
        val results = delegate.updateMultipleFragmentsStatus(slugs, status, force, changedBy, reason)
        
        val successCount = results.count { it.isSuccess }
        if (successCount > 0) {
            slugs.forEach { fragmentCache.invalidateFragment(it) }
            fragmentCache.invalidateFragmentLists()
        }
        
        return results
    }
    
    override suspend fun publishMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?
    ): List<Result<Fragment>> {
        logger.debug("Publishing ${slugs.size} fragments")
        
        val results = delegate.publishMultiple(slugs, changedBy, reason)
        
        val successCount = results.count { it.isSuccess }
        if (successCount > 0) {
            slugs.forEach { fragmentCache.invalidateFragment(it) }
            fragmentCache.invalidateFragmentLists()
        }
        
        return results
    }
    
    override suspend fun unpublishMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?
    ): List<Result<Fragment>> {
        logger.debug("Unpublishing ${slugs.size} fragments")
        
        val results = delegate.unpublishMultiple(slugs, changedBy, reason)
        
        val successCount = results.count { it.isSuccess }
        if (successCount > 0) {
            slugs.forEach { fragmentCache.invalidateFragment(it) }
            fragmentCache.invalidateFragmentLists()
        }
        
        return results
    }
    
    override suspend fun archiveMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?
    ): List<Result<Fragment>> {
        logger.debug("Archiving ${slugs.size} fragments")
        
        val results = delegate.archiveMultiple(slugs, changedBy, reason)
        
        val successCount = results.count { it.isSuccess }
        if (successCount > 0) {
            slugs.forEach { fragmentCache.invalidateFragment(it) }
            fragmentCache.invalidateFragmentLists()
        }
        
        return results
    }
    
    override suspend fun getScheduledFragmentsDueForPublication(threshold: LocalDateTime): List<Fragment> {
        logger.debug("Getting scheduled fragments due for publication: $threshold")
        return delegate.getScheduledFragmentsDueForPublication(threshold)
    }
    
    override suspend fun publishScheduledFragments(threshold: LocalDateTime): List<Result<Fragment>> {
        logger.debug("Publishing scheduled fragments: $threshold")
        
        val results = delegate.publishScheduledFragments(threshold)
        
        val successCount = results.count { it.isSuccess }
        if (successCount > 0) {
            fragmentCache.invalidateFragmentLists()
        }
        
        return results
    }
    
    override suspend fun scheduleMultiple(
        slugs: List<String>,
        publishDate: LocalDateTime,
        changedBy: String?,
        reason: String?
    ): List<Result<Fragment>> {
        logger.debug("Scheduling ${slugs.size} fragments for: $publishDate")
        
        val results = delegate.scheduleMultiple(slugs, publishDate, changedBy, reason)
        
        val successCount = results.count { it.isSuccess }
        if (successCount > 0) {
            slugs.forEach { fragmentCache.invalidateFragment(it) }
            fragmentCache.invalidateFragmentLists()
        }
        
        return results
    }
    
    override suspend fun expireFragments(threshold: LocalDateTime): List<Result<Fragment>> {
        logger.debug("Expiring fragments: $threshold")
        
        val results = delegate.expireFragments(threshold)
        
        val successCount = results.count { it.isSuccess }
        if (successCount > 0) {
            fragmentCache.invalidateFragmentLists()
        }
        
        return results
    }
    
    override suspend fun getFragmentsExpiringSoon(threshold: LocalDateTime): List<Fragment> {
        logger.debug("Getting fragments expiring soon: $threshold")
        return delegate.getFragmentsExpiringSoon(threshold)
    }
    
    override suspend fun reload() {
        logger.debug("Reloading repository")
        fragmentCache.clearAll()
        delegate.reload()
    }
    
    override suspend fun getRelationships(
        slug: String,
        config: RelationshipConfig
    ): ContentRelationships? {
        logger.debug("Getting relationships for: $slug")
        
        val cached = fragmentCache.getRelationships(slug)
        if (cached != null) {
            logger.debug("Cache hit for relationships: $slug")
            return cached
        }
        
        logger.debug("Cache miss for relationships: $slug, loading from repository")
        val relationships = delegate.getRelationships(slug, config)
        
        if (relationships != null) {
            fragmentCache.putRelationships(slug, relationships)
        }
        
        return relationships
    }
    
    override suspend fun createRevision(
        slug: String,
        changedBy: String?,
        reason: String?
    ): Result<FragmentRevision> {
        logger.debug("Creating revision for: $slug")
        return delegate.createRevision(slug, changedBy, reason)
    }
    
    override suspend fun getFragmentRevisions(slug: String): List<FragmentRevision> {
        logger.debug("Getting revisions for: $slug")
        return delegate.getFragmentRevisions(slug)
    }
    
    override suspend fun revertToRevision(
        slug: String,
        revisionId: String,
        changedBy: String?,
        reason: String?
    ): Result<Fragment> {
        logger.debug("Reverting to revision for: $slug")
        
        val result = delegate.revertToRevision(slug, revisionId, changedBy, reason)
        
        if (result.isSuccess) {
            fragmentCache.invalidateFragment(slug)
        }
        
        return result
    }
}
