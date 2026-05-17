package io.github.rygel.fragments.blog

import io.github.rygel.fragments.ContentRelationships
import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentStatus
import io.github.rygel.fragments.FragmentTemplates
import io.github.rygel.fragments.RelationshipConfig
import org.slf4j.LoggerFactory

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
    private val logger = LoggerFactory.getLogger(BlogEngine::class.java)

    private data class BlogIndex(
        val allPostsSorted: List<Fragment>,
        val byTag: Map<String, List<Fragment>>,
        val byCategory: Map<String, List<Fragment>>,
        val byYear: Map<Int, List<Fragment>>,
        val byYearMonth: Map<Pair<Int, Int>, List<Fragment>>,
        val byAuthor: Map<String, List<Fragment>>,
        val allTags: Map<String, Int>,
        val allCategories: Map<String, Int>,
    )

    @Volatile private var blogIndex: BlogIndex? = null

    @Volatile private var indexSnapshot: List<Fragment>? = null

    private suspend fun ensureIndex(): BlogIndex {
        val visible = repository.getAllVisible()
        val current = blogIndex
        if (current != null && indexSnapshot === visible) return current

        val blogPosts =
            visible
                .filter { isBlogTemplate(it.template) }
                .withResolvedUrls()
                .sortedByDescending { it.date }

        val byTag =
            blogPosts
                .flatMap { f -> f.tags.map { tag -> tag.lowercase() to f } }
                .groupBy({ it.first }, { it.second })

        val byCategory =
            blogPosts
                .flatMap { f -> f.categories.map { cat -> cat.lowercase() to f } }
                .groupBy({ it.first }, { it.second })

        val byAuthor =
            blogPosts
                .flatMap { f -> (f.authorIds + listOfNotNull(f.author)).map { aid -> aid to f } }
                .groupBy({ it.first }, { it.second })

        val byYearMonth =
            blogPosts
                .filter { it.date != null }
                .groupBy { Pair(it.date!!.year, it.date!!.monthValue) }

        val index =
            BlogIndex(
                allPostsSorted = blogPosts,
                byTag = byTag,
                byCategory = byCategory,
                byYear = blogPosts.filter { it.date != null }.groupBy { it.date!!.year },
                byYearMonth = byYearMonth,
                byAuthor = byAuthor,
                allTags = blogPosts.flatMap { it.tags }.groupingBy { it }.eachCount(),
                allCategories = blogPosts.flatMap { it.categories }.groupingBy { it }.eachCount(),
            )
        blogIndex = index
        indexSnapshot = visible
        return index
    }

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
        logger.warn(
            "Fragment '{}' has no resolvedUrl — falling back to date-based URL. " +
                "Configure urlBuilder on the repository.",
            fragment.slug,
        )
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
        if (includeDrafts) {
            val blogPosts =
                repository
                    .getAll()
                    .filter { isBlogTemplate(it.template) }
                    .withResolvedUrls()
                    .sortedByDescending { it.date }
            return Page.create(blogPosts, page, pageSize)
        }
        return Page.create(ensureIndex().allPostsSorted, page, pageSize)
    }

    suspend fun getAllPosts(includeDrafts: Boolean = false): List<Fragment> {
        if (includeDrafts) {
            return repository
                .getAll()
                .filter { isBlogTemplate(it.template) }
                .withResolvedUrls()
                .sortedByDescending { it.date }
        }
        return ensureIndex().allPostsSorted
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
        val posts = ensureIndex().byTag[tag.lowercase()] ?: emptyList()
        return Page.create(posts, page, pageSize)
    }

    suspend fun getByCategory(
        category: String,
        page: Int,
    ): Page<Fragment> {
        val posts = ensureIndex().byCategory[category.lowercase()] ?: emptyList()
        return Page.create(posts, page, pageSize)
    }

    suspend fun getByYear(year: Int): List<Fragment> = ensureIndex().byYear[year] ?: emptyList()

    suspend fun getByYearMonth(
        year: Int,
        month: Int,
    ): List<Fragment> = ensureIndex().byYearMonth[Pair(year, month)] ?: emptyList()

    suspend fun getByAuthor(
        authorId: String,
        page: Int = 1,
    ): Page<Fragment> {
        val posts = ensureIndex().byAuthor[authorId] ?: emptyList()
        return Page.create(posts, page, pageSize)
    }

    suspend fun getAllTags(): Map<String, Int> = ensureIndex().allTags

    suspend fun getAllCategories(): Map<String, Int> = ensureIndex().allCategories

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
