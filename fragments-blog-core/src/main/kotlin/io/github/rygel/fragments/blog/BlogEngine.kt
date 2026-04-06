package io.github.rygel.fragments.blog

import io.github.rygel.fragments.ContentRelationships
import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentStatus
import io.github.rygel.fragments.FragmentTemplates
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
    private val relationshipConfig: RelationshipConfig = RelationshipConfig(),
    private val blogUrlPrefix: String = "/blog",
) {
    fun getRepository(): FragmentRepository = repository

    /**
     * Returns `true` if [template] marks the fragment as a blog post.
     * Centralised here so a mistyped template value produces a visible miss
     * rather than silently excluding posts.
     */
    private fun isBlogTemplate(template: String) = template in BLOG_TEMPLATES

    /**
     * Resolves a blog post's URL to include the date-based path prefix,
     * e.g. `/blog/2026/03/hello-world`. If the fragment has no date, falls
     * back to `blogUrlPrefix/slug`.
     */
    private fun resolveUrl(fragment: Fragment): Fragment {
        if (fragment.resolvedUrl != null) return fragment
        val date = fragment.date
        val url =
            if (date != null) {
                "$blogUrlPrefix/${date.year}/${String.format(java.util.Locale.US, "%02d", date.monthValue)}/${fragment.slug}"
            } else {
                "$blogUrlPrefix/${fragment.slug}"
            }
        return fragment.copy(resolvedUrl = url)
    }

    private fun List<Fragment>.withResolvedUrls(): List<Fragment> = map { resolveUrl(it) }

    suspend fun getOverview(
        includeDrafts: Boolean = false,
        page: Int,
    ): Page<Fragment> {
        val allFragments =
            if (includeDrafts) {
                repository.getAll()
            } else {
                repository.getAllVisible()
            }
        val blogPosts =
            allFragments
                .filter { isBlogTemplate(it.template) }
                .withResolvedUrls()
                .sortedByDescending { it.date }
        return Page.create(blogPosts, page, pageSize)
    }

    suspend fun getAllPosts(includeDrafts: Boolean = false): List<Fragment> {
        val allFragments =
            if (includeDrafts) {
                repository.getAll()
            } else {
                repository.getAllVisible()
            }
        return allFragments
            .filter { isBlogTemplate(it.template) }
            .withResolvedUrls()
            .sortedByDescending { it.date }
    }

    suspend fun getDrafts(page: Int): Page<Fragment> {
        val draftFragments =
            repository
                .getAll()
                .filter { isBlogTemplate(it.template) }
                .filter { it.status == FragmentStatus.DRAFT }
                .withResolvedUrls()
                .sortedByDescending { it.date }
        return Page.create(draftFragments, page, pageSize)
    }

    suspend fun getPost(
        year: String,
        month: String,
        slug: String,
    ): Fragment? = repository.getByYearMonthAndSlug(year, month, slug)?.let { resolveUrl(it) }

    suspend fun getByTag(
        tag: String,
        page: Int,
    ): Page<Fragment> {
        val taggedPosts =
            repository
                .getByTag(tag)
                .filter { isBlogTemplate(it.template) }
                .withResolvedUrls()
                .sortedByDescending { it.date }
        return Page.create(taggedPosts, page, pageSize)
    }

    suspend fun getByCategory(
        category: String,
        page: Int,
    ): Page<Fragment> {
        val categorizedPosts =
            repository
                .getByCategory(category)
                .filter { isBlogTemplate(it.template) }
                .withResolvedUrls()
                .sortedByDescending { it.date }
        return Page.create(categorizedPosts, page, pageSize)
    }

    suspend fun getByYear(year: Int): List<Fragment> =
        repository
            .getAllVisible()
            .filter {
                (isBlogTemplate(it.template)) &&
                    it.date?.year == year
            }.withResolvedUrls()
            .sortedByDescending { it.date }

    suspend fun getByYearMonth(
        year: Int,
        month: Int,
    ): List<Fragment> =
        repository
            .getAllVisible()
            .filter {
                (isBlogTemplate(it.template)) &&
                    it.date?.year == year &&
                    it.date?.monthValue == month
            }.withResolvedUrls()
            .sortedByDescending { it.date }

    suspend fun getByAuthor(
        authorId: String,
        page: Int = 1,
    ): Page<Fragment> {
        val authorPosts =
            repository
                .getByAuthor(authorId)
                .filter { isBlogTemplate(it.template) }
                .withResolvedUrls()
                .sortedByDescending { it.date }
        return Page.create(authorPosts, page, pageSize)
    }

    suspend fun getAllTags(): Map<String, Int> =
        repository
            .getAllVisible()
            .flatMap { it.tags }
            .groupingBy { it }
            .eachCount()

    suspend fun getAllCategories(): Map<String, Int> =
        repository
            .getAllVisible()
            .flatMap { it.categories }
            .groupingBy { it }
            .eachCount()

    suspend fun getPostWithRelationships(
        year: String,
        month: String,
        slug: String,
    ): Pair<Fragment?, ContentRelationships?> {
        val fragment = getPost(year, month, slug) // already resolved via getPost
        val relationships = repository.getRelationships(slug, relationshipConfig)
        return Pair(fragment, relationships)
    }

    companion object {
        /**
         * Template values that identify a fragment as a blog post.
         *
         * Delegates to [FragmentTemplates.BLOG_TEMPLATES] so all modules share the same
         * canonical set. Set `template: blog` or `template: blog_post` in front matter.
         */
        val BLOG_TEMPLATES: Set<String> = FragmentTemplates.BLOG_TEMPLATES
    }
}
