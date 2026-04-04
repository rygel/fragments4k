package io.github.rygel.fragments.blog

import io.github.rygel.fragments.ContentRelationships
import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentStatus
import io.github.rygel.fragments.RelationshipConfig

/**
 * Provides paginated, filtered access to blog-post fragments.
 *
 * A fragment is treated as a **blog post** when its `template` front matter field
 * is `"blog"` or `"blog_post"` (see [BLOG_TEMPLATES]). Fragments with any other
 * template value — including `"default"` and `"static"` — are excluded from all
 * methods in this engine.
 */
class BlogEngine(
    private val repository: FragmentRepository,
    private val pageSize: Int = 10,
    private val relationshipConfig: RelationshipConfig = RelationshipConfig()
) {
    /**
     * Returns `true` if [template] marks the fragment as a blog post.
     * Centralised here so a mistyped template value produces a visible miss
     * rather than silently excluding posts.
     */
    private fun isBlogTemplate(template: String) = template in BLOG_TEMPLATES

    suspend fun getOverview(includeDrafts: Boolean = false, page: Int): Page<Fragment> {
        val allFragments = if (includeDrafts) {
            repository.getAll()
        } else {
            repository.getAllVisible()
        }
        val blogPosts = allFragments
            .filter { isBlogTemplate(it.template) }
            .sortedByDescending { it.date }
        return Page.create(blogPosts, page, pageSize)
    }

    suspend fun getDrafts(page: Int): Page<Fragment> {
        val draftFragments = repository.getAll()
            .filter { isBlogTemplate(it.template) }
            .filter { it.status == FragmentStatus.DRAFT }
            .sortedByDescending { it.date }
        return Page.create(draftFragments, page, pageSize)
    }

    suspend fun getPost(year: String, month: String, slug: String): Fragment? {
        return repository.getByYearMonthAndSlug(year, month, slug)
    }

    suspend fun getByTag(tag: String, page: Int): Page<Fragment> {
        val taggedPosts = repository.getByTag(tag)
            .filter { isBlogTemplate(it.template) }
            .sortedByDescending { it.date }
        return Page.create(taggedPosts, page, pageSize)
    }

    suspend fun getByCategory(category: String, page: Int): Page<Fragment> {
        val categorizedPosts = repository.getByCategory(category)
            .filter { isBlogTemplate(it.template) }
            .sortedByDescending { it.date }
        return Page.create(categorizedPosts, page, pageSize)
    }

    suspend fun getByYear(year: Int): List<Fragment> {
        return repository.getAllVisible()
            .filter { 
                (isBlogTemplate(it.template)) &&
                it.date?.year == year
            }
            .sortedByDescending { it.date }
    }

    suspend fun getByYearMonth(year: Int, month: Int): List<Fragment> {
        return repository.getAllVisible()
            .filter { 
                (isBlogTemplate(it.template)) &&
                it.date?.year == year &&
                it.date?.monthValue == month
            }
            .sortedByDescending { it.date }
    }

    suspend fun getByAuthor(authorId: String, page: Int = 1): Page<Fragment> {
        val authorPosts = repository.getByAuthor(authorId)
            .filter { isBlogTemplate(it.template) }
            .sortedByDescending { it.date }
        return Page.create(authorPosts, page, pageSize)
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

    companion object {
        /**
         * Template values that identify a fragment as a blog post.
         *
         * Set `template: blog` or `template: blog_post` in your Markdown front matter.
         * Any other value (e.g. `"default"`, `"static"`, or a custom template name)
         * will cause the fragment to be excluded from all [BlogEngine] results.
         */
        val BLOG_TEMPLATES: Set<String> = setOf("blog", "blog_post")
    }
}
