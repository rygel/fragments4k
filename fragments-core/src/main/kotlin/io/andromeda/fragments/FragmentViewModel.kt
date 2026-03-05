package io.andromeda.fragments

data class TableOfContentsItem(
    val level: Int,
    val title: String,
    val anchor: String
)

data class FragmentViewModel(
    val fragment: Fragment,
    val isPartialRender: Boolean = false,
    val pageTitle: String? = null,
    val additionalContext: Map<String, Any> = emptyMap(),
    private val allFragments: List<Fragment> = emptyList(),
    val relationships: ContentRelationships? = null
) {
    companion object {
        const val HTMX_REQUEST_HEADER = "HX-Request"
        const val HTMX_CURRENT_URL_HEADER = "HX-Current-URL"
        const val WORDS_PER_MINUTE = 225
    }

    fun fromHtmxRequest(headers: Map<String, String>): FragmentViewModel {
        val isHtmxRequest = headers[HTMX_REQUEST_HEADER]?.lowercase() == "true"
        return copy(isPartialRender = isHtmxRequest)
    }

    val title: String
        get() = pageTitle ?: fragment.title

    val hasPreviousPost: Boolean
        get() = relationships?.previous != null

    val hasNextPost: Boolean
        get() = relationships?.next != null

    val previousPost: Fragment?
        get() = relationships?.previous

    val nextPost: Fragment?
        get() = relationships?.next

    val relationshipRelatedPosts: List<Fragment>
        get() = relationships?.allRelated ?: emptyList()

    val translations: Map<String, Fragment>
        get() = relationships?.translations ?: emptyMap()

    val hasRelationships: Boolean
        get() = relationships?.hasRelationships ?: false

    val content: String
        get() = fragment.content

    val preview: String
        get() = fragment.preview

    val slug: String
        get() = fragment.slug

    val template: String
        get() = fragment.template

    val date
        get() = fragment.date

    val tags
        get() = fragment.tags

    val categories
        get() = fragment.categories

    val author: String?
        get() = fragment.author

    val readingTime: ReadingTime
        get() = calculateReadingTime()

    val formattedReadingTime: String
        get() = readingTime.text

    val tableOfContents: List<TableOfContentsItem>
        get() = extractTableOfContents()

    val relatedPosts: List<Fragment>
        get() = findRelatedPosts()

    val socialShareLinks: List<SocialShareLink>
        get() = generateSocialShareLinks()

    val navigationMenu: List<NavigationLink>
        get() = generateNavigationMenu()

    /**
     * Generates social share links for this fragment
     */
    fun generateSocialShareLinks(
        baseUrl: String = "http://localhost:8080",
        platforms: List<SocialPlatform> = SocialPlatform.entries
    ): List<SocialShareLink> {
        val url = buildUrl(baseUrl)
        return SocialShareGenerator.generateShareLinks(title, url, platforms)
    }

    private fun buildUrl(baseUrl: String): String {
        val year = fragment.date?.year ?: "0000"
        val month = String.format("%02d", fragment.date?.monthValue ?: 1)
        return "$baseUrl/blog/$year/$month/$slug"
    }

    fun generateNavigationMenu(
        includeArchive: Boolean = false,
        includeSearch: Boolean = false,
        baseUrl: String = "/"
    ): List<NavigationLink> {
        return NavigationMenuGenerator.generateMainMenu(
            siteUrl = "/",
            blogUrl = "/blog",
            archiveUrl = if (includeArchive) "/blog/archive" else null,
            searchUrl = if (includeSearch) "/search" else null
        )
    }

    /**
     * Generates SEO metadata for this fragment
     */
    fun generateSeoMetadata(
        siteUrl: String,
        siteName: String? = null,
        pagePath: String? = null,
        author: String? = null,
        imageUrl: String? = null
    ): SeoMetadata {
        return SeoMetadata.fromFragment(
            fragment = fragment,
            siteUrl = siteUrl,
            siteName = siteName,
            pagePath = pagePath,
            author = author,
            imageUrl = imageUrl
        )
    }

    data class ReadingTime(
        val minutes: Int,
        val seconds: Int,
        val text: String
    )

    private fun calculateReadingTime(): ReadingTime {
        val words = content.split(Regex("\\s+")).size
        val totalSeconds = (words.toDouble() / WORDS_PER_MINUTE) * 60
        val minutes = totalSeconds.toInt() / 60
        val seconds = totalSeconds.toInt() % 60
        
        val text = when {
            minutes == 0 -> "${seconds}s read"
            seconds == 0 -> "${minutes}m read"
            else -> "${minutes}m ${seconds}s read"
        }
        
        return ReadingTime(minutes, seconds, text)
    }

    private fun extractTableOfContents(): List<TableOfContentsItem> {
        val items = mutableListOf<TableOfContentsItem>()
        val headerPattern = Regex("^(#{1,6})\\s+(.+)$", RegexOption.MULTILINE)
        
        headerPattern.findAll(content).forEach { match ->
            val level = match.groupValues[1].length
            val title = match.groupValues[2].trim()
            val anchor = title.lowercase()
                .replace(Regex("[^a-z0-9\\s-]"), "")
                .replace(Regex("\\s+"), "-")
            
            items.add(TableOfContentsItem(level, title, anchor))
        }
        
        return items
    }

    private fun findRelatedPosts(): List<Fragment> {
        if (allFragments.isEmpty()) return emptyList()

        val scoredFragments = allFragments
            .filter { it.slug != fragment.slug && it.visible }
            .map { other ->
                var score = 0.0
                
                tags.forEach { tag ->
                    if (other.tags.contains(tag)) score += 2.0
                }
                
                categories.forEach { category ->
                    if (other.categories.contains(category)) score += 1.5
                }
                
                if (fragment.template == "blog" && other.template == "blog") {
                    score += 0.5
                }
                
                other to score
            }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        
        return scoredFragments
    }
}
