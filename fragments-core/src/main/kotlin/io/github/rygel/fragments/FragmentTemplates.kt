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
    const val DEFAULT = "default"
    const val INDEX = "index"
    const val BLOG_OVERVIEW = "blog_overview"
    const val BLOG_POST = "blog_post"
    const val STATIC = "static"
    const val BLOG = "blog"
    const val SEARCH = "search"
    const val ARCHIVE = "archive"

    val BLOG_TEMPLATES: Set<String> = setOf(BLOG, BLOG_POST)
}
