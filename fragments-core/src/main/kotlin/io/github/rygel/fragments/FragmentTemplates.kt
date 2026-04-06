package io.github.rygel.fragments

/**
 * Shared template-name constants used to classify fragments by content type.
 *
 * Centralised here so all modules (BlogEngine, LlmsTxtGenerator, SitemapGenerator, etc.)
 * refer to the same set of values and a mis-typed template name produces a visible
 * miss rather than a silent exclusion.
 *
 * Set the `template` field in your Markdown front matter to one of these values
 * to opt a fragment into the corresponding content class.
 */
object FragmentTemplates {
    /**
     * Template values that identify a fragment as a blog post.
     *
     * Set `template: blog` or `template: blog_post` in front matter.
     * Any other value causes the fragment to be excluded from blog listings,
     * RSS feeds, and blog-specific URL resolution.
     */
    val BLOG_TEMPLATES: Set<String> = setOf("blog", "blog_post")
}
