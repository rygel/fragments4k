package io.github.rygel.fragments

/**
 * Generates structured navigation menus as lists of [NavigationLink] items.
 *
 * Use the factory methods to produce consistent menu data for site templates.
 */
object NavigationMenuGenerator {
    /**
     * Builds the primary site-wide navigation menu.
     *
     * Always includes Home and Blog links. Adds Archive and Search links when
     * their URLs are provided.
     *
     * @param siteUrl URL for the Home link; defaults to `"/"`.
     * @param blogUrl URL for the Blog link; defaults to `"/blog"`.
     * @param archiveUrl Optional URL for the Archive link; omitted when `null`.
     * @param searchUrl Optional URL for the Search link; omitted when `null`.
     * @return Ordered list of [NavigationLink] items for the main menu.
     */
    fun generateMainMenu(
        siteUrl: String = "/",
        blogUrl: String = "/blog",
        archiveUrl: String? = null,
        searchUrl: String? = null,
    ): List<NavigationLink> {
        val links = mutableListOf<NavigationLink>()
        links.add(NavigationLink(label = "Home", url = siteUrl))
        links.add(NavigationLink(label = "Blog", url = blogUrl))
        archiveUrl?.let {
            links.add(NavigationLink(label = "Archive", url = it))
        }
        searchUrl?.let {
            links.add(NavigationLink(label = "Search", url = it))
        }
        return links
    }

    /**
     * Builds the blog-specific sub-navigation menu.
     *
     * Always includes a Blog Home link. Adds an Archive link when its URL is provided.
     *
     * @param baseUrl URL for the Blog Home link; defaults to `"/blog"`.
     * @param archiveUrl Optional URL for the Archive link; defaults to `"/blog/archive"`.
     *   Pass `null` to omit the Archive link entirely.
     * @param currentYear Unused; reserved for future active-state highlighting by year.
     * @param currentMonth Unused; reserved for future active-state highlighting by month.
     * @return Ordered list of [NavigationLink] items for the blog menu.
     */
    fun generateBlogMenu(
        baseUrl: String = "/blog",
        archiveUrl: String? = "/blog/archive",
        currentYear: Int? = null,
        currentMonth: Int? = null,
    ): List<NavigationLink> {
        val links = mutableListOf<NavigationLink>()
        links.add(NavigationLink(label = "Blog Home", url = baseUrl))
        archiveUrl?.let {
            links.add(NavigationLink(label = "Archive", url = it))
        }
        return links
    }
}
