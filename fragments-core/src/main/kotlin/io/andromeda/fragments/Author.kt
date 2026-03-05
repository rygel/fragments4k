package io.andromeda.fragments

import java.time.LocalDateTime

/**
 * Represents an author of content in the Fragments system.
 * Authors can be associated with fragments for multi-author blog support.
 */
data class Author(
    val id: String,
    val name: String,
    val slug: String,
    val email: String? = null,
    val bio: String? = null,
    val avatar: String? = null,
    val website: String? = null,
    val twitter: String? = null,
    val github: String? = null,
    val linkedin: String? = null,
    val location: String? = null,
    val company: String? = null,
    val role: String? = null,
    val socialLinks: Map<String, String> = emptyMap(),
    val joinedDate: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Gets the display name for the author
     */
    val displayName: String
        get() = name

    /**
     * Gets a short bio (truncated to 200 characters)
     */
    val shortBio: String?
        get() = bio?.take(200)?.let { if (bio.length > 200) "$it..." else it }

    /**
     * Gets all social links as a list of pairs (platform, url)
     */
    val allSocialLinks: List<Pair<String, String>>
        get() {
            val links = mutableListOf<Pair<String, String>>()
            twitter?.let { links.add("Twitter" to "https://twitter.com/$it") }
            github?.let { links.add("GitHub" to "https://github.com/$it") }
            linkedin?.let { links.add("LinkedIn" to "https://linkedin.com/in/$it") }
            website?.let { links.add("Website" to it) }
            links.addAll(socialLinks.map { (platform, url) -> platform to url })
            return links
        }

    companion object {
        /**
         * Creates an author from front matter metadata
         */
        fun fromFrontMatter(
            authorName: String?,
            frontMatter: Map<String, Any>,
            id: String = authorName?.slugify() ?: "unknown"
        ): Author? {
            if (authorName == null) return null

            return Author(
                id = id,
                name = authorName,
                slug = frontMatter["author_slug"] as? String ?: authorName.slugify(),
                email = frontMatter["author_email"] as? String,
                bio = frontMatter["author_bio"] as? String,
                avatar = frontMatter["author_avatar"] as? String,
                website = frontMatter["author_website"] as? String,
                twitter = frontMatter["author_twitter"] as? String,
                github = frontMatter["author_github"] as? String,
                linkedin = frontMatter["author_linkedin"] as? String,
                location = frontMatter["author_location"] as? String,
                company = frontMatter["author_company"] as? String,
                role = frontMatter["author_role"] as? String
            )
        }

        private fun String.slugify(): String {
            return this.lowercase()
                .replace(Regex("[^a-z0-9\\s-]"), "")
                .replace(Regex("\\s+"), "-")
                .trim('-')
        }
    }
}

/**
 * Repository interface for managing authors
 */
interface AuthorRepository {
    suspend fun getAll(): List<Author>
    suspend fun getById(id: String): Author?
    suspend fun getByName(name: String): Author?
    suspend fun getBySlug(slug: String): Author?
    suspend fun getBySlugOrId(identifier: String): Author?
    suspend fun register(author: Author)
    suspend fun remove(id: String): Boolean
    suspend fun clear()
    suspend fun count(): Int
}

/**
 * View model for author information in templates
 */
data class AuthorViewModel(
    val author: Author,
    val postCount: Int = 0
) {
    val id: String
        get() = author.id

    val name: String
        get() = author.name

    val email: String?
        get() = author.email

    val bio: String?
        get() = author.bio

    val shortBio: String?
        get() = author.shortBio

    val avatar: String?
        get() = author.avatar

    val website: String?
        get() = author.website

    val twitter: String?
        get() = author.twitter

    val github: String?
        get() = author.github

    val linkedin: String?
        get() = author.linkedin

    val location: String?
        get() = author.location

    val company: String?
        get() = author.company

    val role: String?
        get() = author.role

    val slug: String
        get() = author.slug

    val socialLinks: List<Pair<String, String>>
        get() = author.allSocialLinks
}
