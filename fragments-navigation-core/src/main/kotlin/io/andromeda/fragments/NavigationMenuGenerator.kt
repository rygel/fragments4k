package io.andromeda.fragments

object NavigationMenuGenerator {
    fun generateMainMenu(
        siteUrl: String = "/",
        blogUrl: String = "/blog",
        archiveUrl: String? = null,
        searchUrl: String? = null
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

    fun generateBlogMenu(
        baseUrl: String = "/blog",
        archiveUrl: String? = "/blog/archive",
        currentYear: Int? = null,
        currentMonth: Int? = null
    ): List<NavigationLink> {
        val links = mutableListOf<NavigationLink>()
        links.add(NavigationLink(label = "Blog Home", url = baseUrl))
        archiveUrl?.let {
            links.add(NavigationLink(label = "Archive", url = it))
        }
        return links
    }
}
