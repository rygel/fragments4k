package io.github.rygel.fragments

/**
 * Generates Schema.org Person JSON-LD structured data for author pages.
 *
 * The output is suitable for embedding in a `<script type="application/ld+json">` block
 * and helps search engines understand author identity, linking articles back to their
 * creator profiles.
 */
object PersonSchemaGenerator {

    /**
     * Generates a full Person JSON-LD block.
     *
     * @param name Display name of the person.
     * @param siteUrl Base URL of the site (e.g. `"https://example.com"`).
     * @param authorSlug URL-safe slug; used to build the `@id` and default `url`.
     * @param bio Short biography used as `description`.
     * @param image Absolute or site-relative URL to an avatar/profile image.
     * @param url Explicit canonical URL for the person; defaults to `siteUrl/blog/author/authorSlug`.
     * @param socialLinks Full URLs to external profiles (GitHub, Twitter, LinkedIn, etc.).
     */
    fun generate(
        name: String,
        siteUrl: String,
        authorSlug: String? = null,
        bio: String? = null,
        image: String? = null,
        url: String? = null,
        socialLinks: List<String> = emptyList()
    ): String {
        val personUrl = url ?: authorSlug?.let { "$siteUrl/blog/author/$it" }
        val personId = authorSlug?.let { "$siteUrl/blog/author/$it#person" }

        return buildString {
            append("{\n")
            append("    \"@context\": \"https://schema.org\",\n")
            append("    \"@type\": \"Person\",\n")
            personId?.let { append("    \"@id\": \"${escapeJson(it)}\",\n") }
            append("    \"name\": \"${escapeJson(name)}\"")
            bio?.let {
                append(",\n    \"description\": \"${escapeJson(it)}\"")
            }
            image?.let {
                val resolvedImage = if (it.startsWith("http://") || it.startsWith("https://")) it else "$siteUrl$it"
                append(",\n    \"image\": \"${escapeJson(resolvedImage)}\"")
            }
            personUrl?.let {
                append(",\n    \"url\": \"${escapeJson(it)}\"")
            }
            if (socialLinks.isNotEmpty()) {
                val linksJson = socialLinks.joinToString(", ") { link -> "\"${escapeJson(link)}\"" }
                append(",\n    \"sameAs\": [$linksJson]")
            }
            append("\n}")
        }
    }

    /**
     * Convenience method that builds a Person JSON-LD block from an [Author] model.
     *
     * Social link URLs are resolved from [Author.allSocialLinks] (which combines
     * the legacy `twitter`/`github`/`linkedin`/`website` fields with the free-form
     * [Author.socialLinks] map).
     *
     * @param author The author to generate structured data for.
     * @param siteUrl Base URL of the site.
     */
    fun fromAuthor(author: Author, siteUrl: String): String {
        val socialUrls = author.allSocialLinks.map { it.second }
        return generate(
            name = author.name,
            siteUrl = siteUrl,
            authorSlug = author.slug,
            bio = author.bio,
            image = author.avatar,
            url = author.website,
            socialLinks = socialUrls
        )
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
