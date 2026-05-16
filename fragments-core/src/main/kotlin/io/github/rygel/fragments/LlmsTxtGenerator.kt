package io.github.rygel.fragments

import org.slf4j.LoggerFactory

/**
 * Generates an [llms.txt](https://llmstxt.org/) file for AI crawler discoverability.
 *
 * The output follows the llms.txt markdown format: a site title heading,
 * a blockquote description, then sections grouping content by type
 * (blog posts vs pages). Each entry is a markdown link with a short
 * plain-text preview (max 160 characters).
 *
 * URLs are derived from each fragment's [Fragment.url] property.
 * For correct URLs, ensure your repository configures a `urlBuilder`
 * that matches your adapter routes.
 */
object LlmsTxtGenerator {
    private const val PREVIEW_LENGTH = 160
    private val BLOG_TEMPLATES: Set<String> = FragmentTemplates.BLOG_TEMPLATES
    private val logger = LoggerFactory.getLogger(LlmsTxtGenerator::class.java)

    /**
     * Generates the llms.txt content.
     *
     * @param siteTitle     Human-readable site name used as the top-level heading.
     * @param siteDescription One-line site description rendered as a blockquote.
     * @param siteUrl       Absolute base URL (no trailing slash) for building links.
     * @param repositories  One or more [FragmentRepository] instances whose visible
     *                      fragments should appear in the output.
     * @param resolvedFragments When provided, used instead of loading from [repositories].
     *                      Pass pre-resolved fragments (with [Fragment.resolvedUrl] set)
     *                      to ensure correct date-based blog URLs appear in the output.
     */
    suspend fun generate(
        siteTitle: String,
        siteDescription: String,
        siteUrl: String,
        repositories: List<FragmentRepository>,
        resolvedFragments: List<Fragment>? = null,
    ): String {
        val allCandidates =
            (resolvedFragments ?: repositories.flatMap { it.getAllVisible() })
                .distinctBy { it.slug }

        // Exclude fragments whose URL was not explicitly resolved by a urlBuilder.
        // The Fragment.url fallback (baseUrl/slug) may not match the actual HTTP
        // route, producing incorrect URLs in the published llms.txt. See #65 / #77.
        val skipped = allCandidates.filter { it.resolvedUrl == null }
        if (skipped.isNotEmpty()) {
            logger.warn(
                "Skipping {} fragment(s) without resolvedUrl in llms.txt (configure urlBuilder on the repository): {}",
                skipped.size,
                skipped.joinToString { it.slug },
            )
        }
        val allFragments = allCandidates.filter { it.resolvedUrl != null }

        val blogPosts =
            allFragments
                .filter { it.template in BLOG_TEMPLATES }
                .sortedByDescending { it.date }

        val pages =
            allFragments
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
                    val description = post.previewTextOnly.take(PREVIEW_LENGTH)
                    val absoluteUrl = siteUrl.trimEnd('/') + post.url
                    appendLine("- [${post.title}]($absoluteUrl): $description")
                }
            }

            if (pages.isNotEmpty()) {
                appendLine()
                appendLine("## Pages")
                appendLine()
                for (page in pages) {
                    val description = page.previewTextOnly.take(PREVIEW_LENGTH)
                    val absoluteUrl = siteUrl.trimEnd('/') + page.url
                    appendLine("- [${page.title}]($absoluteUrl): $description")
                }
            }
        }
    }
}
