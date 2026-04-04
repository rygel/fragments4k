package io.github.rygel.fragments

/**
 * Generates an [llms.txt](https://llmstxt.org/) file for AI crawler discoverability.
 *
 * The output follows the llms.txt markdown format: a site title heading,
 * a blockquote description, then sections grouping content by type
 * (blog posts vs pages). Each entry is a markdown link with a short
 * plain-text preview (max 160 characters).
 */
object LlmsTxtGenerator {

    /**
     * Template values that identify a fragment as a blog post.
     * Kept in sync with `BlogEngine.BLOG_TEMPLATES`.
     */
    private val BLOG_TEMPLATES: Set<String> = setOf("blog", "blog_post")

    /**
     * Generates the llms.txt content.
     *
     * @param siteTitle     Human-readable site name used as the top-level heading.
     * @param siteDescription One-line site description rendered as a blockquote.
     * @param siteUrl       Absolute base URL (no trailing slash) for building links.
     * @param repositories  One or more [FragmentRepository] instances whose visible
     *                      fragments should appear in the output.
     */
    suspend fun generate(
        siteTitle: String,
        siteDescription: String,
        siteUrl: String,
        repositories: List<FragmentRepository>
    ): String {
        val allFragments = repositories
            .flatMap { it.getAllVisible() }
            .distinctBy { it.slug }

        val blogPosts = allFragments
            .filter { it.template in BLOG_TEMPLATES }
            .sortedByDescending { it.date }

        val pages = allFragments
            .filter { it.template !in BLOG_TEMPLATES }
            .sortedBy { it.title }

        return buildString {
            appendLine("# $siteTitle")
            appendLine()
            appendLine("> $siteDescription")

            if (blogPosts.isNotEmpty()) {
                appendLine()
                appendLine("## Blog Posts")
                appendLine()
                for (post in blogPosts) {
                    val description = post.previewTextOnly.take(160)
                    val absoluteUrl = siteUrl.trimEnd('/') + post.url
                    appendLine("- [${post.title}]($absoluteUrl): $description")
                }
            }

            if (pages.isNotEmpty()) {
                appendLine()
                appendLine("## Pages")
                appendLine()
                for (page in pages) {
                    val description = page.previewTextOnly.take(160)
                    val absoluteUrl = siteUrl.trimEnd('/') + page.url
                    appendLine("- [${page.title}]($absoluteUrl): $description")
                }
            }
        }
    }
}
