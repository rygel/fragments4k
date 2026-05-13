package io.github.rygel.fragments.adapter

import io.github.rygel.fragments.ArchiveNavigationGenerator
import io.github.rygel.fragments.ArchiveNavigationLink
import io.github.rygel.fragments.AuthorRepository
import io.github.rygel.fragments.AuthorViewModel
import io.github.rygel.fragments.ContentRelationships
import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.LlmsTxtGenerator
import io.github.rygel.fragments.NavigationLink
import io.github.rygel.fragments.NavigationMenuGenerator
import io.github.rygel.fragments.SocialShareGenerator
import io.github.rygel.fragments.SocialShareLink
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.blog.Page
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.lucene.SearchResult
import io.github.rygel.fragments.lucene.SearchSuggestion
import io.github.rygel.fragments.rss.RssGenerator
import io.github.rygel.fragments.sitemap.SitemapGenerator
import io.github.rygel.fragments.static.StaticPageEngine

/**
 * Framework-agnostic engine that encapsulates all shared content logic for Fragments adapters.
 *
 * Each web framework adapter (http4k, Javalin, Spring, Quarkus, Micronaut) delegates
 * to this engine for business logic, then maps the results to framework-specific
 * HTTP responses. This eliminates duplicated logic across adapters.
 *
 * ## Design: Facade Pattern
 *
 * This class intentionally uses the Facade pattern — it aggregates blog, static page,
 * RSS, sitemap, search, and navigation logic behind a single entry point. While the
 * method count is large, each method is a thin delegation to a specialised component
 * (BlogEngine, StaticPageEngine, RssGenerator, etc.). Splitting into smaller facades
 * would increase coupling complexity without reducing actual code — the adapters still
 * need all these capabilities together.
 */
class FragmentsEngine(
    val staticEngine: StaticPageEngine,
    val blogEngine: BlogEngine,
    val searchEngine: LuceneSearchEngine? = null,
    val siteTitle: String = "My Blog",
    val siteDescription: String = "My Awesome Blog",
    val siteUrl: String = "http://localhost:8080",
    val feedUrl: String = "$siteUrl/rss.xml",
    val authorRepository: AuthorRepository? = null,
    /**
     * Extra repositories whose visible fragments are included in sitemap, RSS, and llms.txt.
     *
     * Fragments from these repositories are included **without URL resolution** — their
     * [Fragment.url] falls back to `baseUrl/slug`. For fragments that require date-based
     * or prefix-based URLs (e.g. `/projects/my-app`), use [additionalFragmentProviders]
     * instead and supply a suspend function that returns pre-resolved fragments.
     */
    private val additionalRepositories: List<FragmentRepository> = emptyList(),
    /**
     * Extra suspend functions that supply pre-resolved fragments for sitemap, RSS, and llms.txt.
     *
     * Use this when an additional content source needs custom URL resolution that goes
     * beyond `baseUrl/slug`. Each provider is called lazily at feed-generation time.
     *
     * Example — a projects repository where URLs follow `/projects/{slug}`:
     * ```kotlin
     * additionalFragmentProviders = listOf {
     *     projectsRepo.getAllVisible().map { it.copy(resolvedUrl = "/projects/${it.slug}") }
     * }
     * ```
     */
    val additionalFragmentProviders: List<suspend () -> List<Fragment>> = emptyList(),
    val navigationMenu: List<NavigationLink>? = null,
    val footer: FooterConfig? = null,
    /**
     * Content-Security-Policy header value sent with every response.
     *
     * The default allows `cdnjs.cloudflare.com` (Prism syntax highlighting).
     * Override this to restrict or allow additional CDNs (e.g. `"default-src 'self'; script-src 'self'; style-src 'self'"`)
     * if you serve libraries locally or use different CDN hosts.
     */
    val contentSecurityPolicy: String =
        "default-src 'self'; script-src 'self' cdnjs.cloudflare.com; style-src 'self' cdnjs.cloudflare.com",
) {
    private val allRepositories: List<FragmentRepository> =
        listOf(staticEngine.getRepository(), blogEngine.getRepository()) + additionalRepositories

    private val rssGenerator: RssGenerator = RssGenerator(allRepositories)
    private val sitemapGenerator: SitemapGenerator = SitemapGenerator(allRepositories, siteUrl)

    fun nav(): List<NavigationLink> = navigationMenu ?: NavigationMenuGenerator.generateMainMenu()

    fun footer(): FooterConfig = footer ?: FooterGenerator.generate()

    // -- Static pages ---------------------------------------------------------

    suspend fun getHome(): List<Fragment> = staticEngine.getAllStaticPages()

    suspend fun getPage(slug: String): Fragment? {
        val validation = RequestValidation.validateSlug(slug)
        if (!validation.isValid) return null
        return staticEngine.getPage(validation.value)
    }

    // -- Blog -----------------------------------------------------------------

    suspend fun getBlogOverview(
        page: Int = 1,
        includeDrafts: Boolean = false,
    ): Page<Fragment> = blogEngine.getOverview(includeDrafts, RequestValidation.validatePage(page).value)

    suspend fun getBlogPost(
        year: String,
        month: String,
        slug: String,
    ): Fragment? {
        val validatedSlug = RequestValidation.validateSlug(slug)
        if (!validatedSlug.isValid) return null
        return blogEngine.getPost(year, month, validatedSlug.value)
    }

    suspend fun getBlogPostWithRelationships(
        year: String,
        month: String,
        slug: String,
    ): Pair<Fragment?, ContentRelationships?> {
        val validatedSlug = RequestValidation.validateSlug(slug)
        if (!validatedSlug.isValid) return null to null
        return blogEngine.getPostWithRelationships(year, month, validatedSlug.value)
    }

    suspend fun getByTag(
        tag: String,
        page: Int = 1,
    ): Page<Fragment> {
        val validatedTag = RequestValidation.validateTag(tag)
        if (!validatedTag.isValid) return Page.create(emptyList(), 1, 10)
        return blogEngine.getByTag(validatedTag.value, RequestValidation.validatePage(page).value)
    }

    suspend fun getByCategory(
        category: String,
        page: Int = 1,
    ): Page<Fragment> {
        val validatedCategory = RequestValidation.validateCategory(category)
        if (!validatedCategory.isValid) return Page.create(emptyList(), 1, 10)
        return blogEngine.getByCategory(validatedCategory.value, RequestValidation.validatePage(page).value)
    }

    suspend fun getByAuthor(
        authorId: String,
        page: Int = 1,
    ): Page<Fragment> {
        val validatedAuthor = RequestValidation.validateAuthorId(authorId)
        if (!validatedAuthor.isValid) return Page.create(emptyList(), 1, 10)
        return blogEngine.getByAuthor(validatedAuthor.value, RequestValidation.validatePage(page).value)
    }

    suspend fun getAuthor(slugOrId: String): AuthorViewModel? {
        val validation = RequestValidation.validateAuthorId(slugOrId)
        if (!validation.isValid) return null
        val author = authorRepository?.getBySlugOrId(validation.value) ?: return null
        val postCount = blogEngine.getByAuthor(validation.value, 1).totalItems
        return AuthorViewModel(author, postCount = postCount)
    }

    suspend fun getByYear(year: Int): List<Fragment> {
        val validation = RequestValidation.validateYear(year)
        if (!validation.isValid) return emptyList()
        return blogEngine.getByYear(validation.value)
    }

    suspend fun getByYearMonth(
        year: Int,
        month: Int,
    ): List<Fragment> {
        val validatedYear = RequestValidation.validateYear(year)
        val validatedMonth = RequestValidation.validateMonth(month)
        if (!validatedYear.isValid || !validatedMonth.isValid) return emptyList()
        return blogEngine.getByYearMonth(validatedYear.value, validatedMonth.value)
    }

    suspend fun getAllTags(): Map<String, Int> = blogEngine.getAllTags()

    suspend fun getAllCategories(): Map<String, Int> = blogEngine.getAllCategories()

    // -- Search ---------------------------------------------------------------

    suspend fun search(
        query: String,
        maxResults: Int = 50,
    ): List<SearchResult> {
        val validatedQuery = RequestValidation.validateSearchQuery(query)
        if (!validatedQuery.isValid) return emptyList()
        return searchEngine?.search(validatedQuery.value, RequestValidation.validateMaxResults(maxResults).value) ?: emptyList()
    }

    suspend fun autocomplete(
        query: String,
        limit: Int = 10,
    ): List<SearchSuggestion> {
        val validatedQuery = RequestValidation.validateSearchQuery(query)
        if (!validatedQuery.isValid) return emptyList()
        return searchEngine?.autocomplete(validatedQuery.value, RequestValidation.validateAutocompleteLimit(limit).value) ?: emptyList()
    }

    // -- Feed generation ------------------------------------------------------

    /**
     * Collects all visible fragments with their URLs fully resolved
     * (date-based blog paths, page prefixes, etc.) so that sitemap/RSS/llms
     * generators emit correct absolute URLs rather than raw slug paths.
     *
     * Exposed as `public` for static-site generation scenarios where you need to
     * call multiple feed generators in sequence and want to avoid redundant
     * repository reads — collect once and pass the result to each generator directly:
     *
     * ```kotlin
     * val fragments = engine.collectResolvedFragments()
     * val sitemap = sitemapGenerator.generateSitemap(fragments)
     * val rss = rssGenerator.generateFeed(..., resolvedFragments = fragments)
     * ```
     */
    suspend fun collectResolvedFragments(): List<Fragment> {
        val staticPages = staticEngine.getAllStaticPages()
        val blogPosts = blogEngine.getAllPosts()
        val additional = additionalRepositories.flatMap { it.getAllVisible() }
        val providerFragments = additionalFragmentProviders.flatMap { it() }
        return (staticPages + blogPosts + additional + providerFragments).distinctBy { it.slug }
    }

    suspend fun generateRssFeed(): String =
        rssGenerator.generateFeed(
            siteTitle = siteTitle,
            siteDescription = siteDescription,
            siteUrl = siteUrl,
            feedUrl = feedUrl,
            resolvedFragments = collectResolvedFragments(),
        )

    suspend fun generateSitemap(): String = sitemapGenerator.generateSitemap(collectResolvedFragments())

    fun generateRobotsTxt(): String =
        buildString {
            appendLine("User-agent: *")
            appendLine("Allow: /")
            appendLine()
            appendLine("Sitemap: $siteUrl/sitemap.xml")
        }

    suspend fun generateLlmsTxt(): String =
        LlmsTxtGenerator.generate(
            siteTitle = siteTitle,
            siteDescription = siteDescription,
            siteUrl = siteUrl,
            repositories = allRepositories,
            resolvedFragments = collectResolvedFragments(),
        )

    // -- Archive navigation ---------------------------------------------------

    fun generateArchiveYearLinks(
        currentYear: Int,
        availableYears: List<Int> = emptyList(),
    ): List<ArchiveNavigationLink> =
        ArchiveNavigationGenerator.generateYearLinks(
            baseUrl = "/blog/archive",
            availableYears = availableYears,
            currentYear = currentYear,
        )

    fun generateArchiveMonthLinks(
        year: Int,
        currentMonth: Int,
    ): List<ArchiveNavigationLink> =
        ArchiveNavigationGenerator.generateMonthLinks(
            year = year,
            currentMonth = currentMonth,
        )

    fun generateArchiveBreadcrumbs(
        currentYear: Int,
        currentMonth: Int? = null,
    ): List<ArchiveNavigationLink> =
        ArchiveNavigationGenerator.generateBreadcrumbs(
            currentYear = currentYear,
            currentMonth = currentMonth,
        )

    // -- Social sharing -------------------------------------------------------

    fun socialShareLinks(
        title: String,
        url: String,
    ): List<SocialShareLink> = SocialShareGenerator.generateShareLinks(title = title, url = url)

    // -- Utilities ------------------------------------------------------------

    fun isHtmxRequest(header: String?): Boolean = header?.lowercase() == "true"

    fun pagination(
        currentPage: Int,
        totalPages: Int,
        basePath: String,
    ): PaginationInfo =
        PaginationGenerator.generateSimpleControls(
            currentPage = currentPage,
            totalPages = totalPages,
            basePath = basePath,
        )

    fun searchForm(): SearchFormConfig = SearchFormGenerator.generate()

    fun cspHeader(): String = contentSecurityPolicy

    fun close() {
        searchEngine?.close()
    }
}
