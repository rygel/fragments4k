package io.github.rygel.fragments.blog

import io.github.rygel.fragments.ContentRelationships
import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentStatus
import io.github.rygel.fragments.RelationshipConfig

class BlogEngine(
    private val repository: FragmentRepository,
    private val pageSize: Int = 10,
    private val relationshipConfig: RelationshipConfig = RelationshipConfig()
) {

    suspend fun getOverview(includeDrafts: Boolean = false, page: Int): Page<Fragment> {
        val allFragments = if (includeDrafts) {
            repository.getAll()
        } else {
            repository.getAllVisible()
        }
        val blogPosts = allFragments
            .filter { it.template == "blog" || it.template.isEmpty() }
            .sortedByDescending { it.date }
        return Page.create(blogPosts, page, pageSize)
    }

    suspend fun getDrafts(page: Int): Page<Fragment> {
        val draftFragments = repository.getAll()
            .filter { it.template == "blog" || it.template.isEmpty() }
            .filter { it.status == FragmentStatus.DRAFT }
            .sortedByDescending { it.date }
        return Page.create(draftFragments, page, pageSize)
    }

    suspend fun getPost(year: String, month: String, slug: String): Fragment? {
        return repository.getByYearMonthAndSlug(year, month, slug)
    }

    suspend fun getByTag(tag: String, page: Int): Page<Fragment> {
        val taggedPosts = repository.getByTag(tag)
            .filter { it.template == "blog" || it.template.isEmpty() }
            .sortedByDescending { it.date }
        return Page.create(taggedPosts, page, pageSize)
    }

    suspend fun getByCategory(category: String, page: Int): Page<Fragment> {
        val categorizedPosts = repository.getByCategory(category)
            .filter { it.template == "blog" || it.template.isEmpty() }
            .sortedByDescending { it.date }
        return Page.create(categorizedPosts, page, pageSize)
    }

    suspend fun getByYear(year: Int): List<Fragment> {
        return repository.getAllVisible()
            .filter { 
                (it.template == "blog" || it.template.isEmpty()) &&
                it.date?.year == year
            }
            .sortedByDescending { it.date }
    }

    suspend fun getByYearMonth(year: Int, month: Int): List<Fragment> {
        return repository.getAllVisible()
            .filter { 
                (it.template == "blog" || it.template.isEmpty()) &&
                it.date?.year == year &&
                it.date?.monthValue == month
            }
            .sortedByDescending { it.date }
    }

    suspend fun getAllTags(): Map<String, Int> {
        return repository.getAllVisible()
            .flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
    }

    suspend fun getAllCategories(): Map<String, Int> {
        return repository.getAllVisible()
            .flatMap { it.categories }
            .groupingBy { it }
            .eachCount()
    }

    suspend fun getPostWithRelationships(year: String, month: String, slug: String): Pair<Fragment?, ContentRelationships?> {
        val fragment = getPost(year, month, slug)
        val relationships = repository.getRelationships(slug, relationshipConfig)
        return Pair(fragment, relationships)
    }
}
