package io.github.rygel.fragments.adapter

import io.github.rygel.fragments.ArchiveNavigationGenerator
import io.github.rygel.fragments.ArchiveNavigationLink
import io.github.rygel.fragments.AuthorRepository
import io.github.rygel.fragments.AuthorViewModel
import io.github.rygel.fragments.ContentRelationships
import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentTemplates
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
     * The default is strict (self-only for scripts, styles, images, and fonts;
     * no inline scripts; no external hosts). Override this to allow CDNs or
     * inline styles if your templates require them:
     * ```
     * contentSecurityPolicy = "default-src 'self'; script-src 'self' cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline'"
     * ```
     */
    val contentSecurityPolicy: String =
        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'",
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(FragmentsEngine::class.java)

    init {
        if (siteUrl == "http://localhost:8080") {
            logger.warn(
                "FragmentsEngine is using the default siteUrl 'http://localhost:8080'. " +
                    "Set siteUrl to your production URL for correct sitemap, RSS, and canonical URLs.",
            )
        }
    }

    @Volatile private var cachedFeedOutput: FeedOutput? = null
    @Volatile private var feedContentSnapshot: List<Fragment>? = null

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
        if (!validation.isValid) throw IllegalArgumentException(validation.errorMessage)
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
        if (!validatedSlug.isValid) throw IllegalArgumentException(validatedSlug.errorMessage)
        return blogEngine.getPost(year, month, validatedSlug.value)
    }

    suspend fun getBlogPostWithRelationships(
        year: String,
        month: String,
        slug: String,
    ): Pair<Fragment?, ContentRelationships?> {
        val validatedSlug = RequestValidation.validateSlug(slug)
        if (!validatedSlug.isValid) throw IllegalArgumentException(validatedSlug.errorMessage)
        return blogEngine.getPostWithRelationships(year, month, validatedSlug.value)
    }

    suspend fun getByTag(
        tag: String,
        page: Int = 1,
    ): Page<Fragment> {
        val validatedTag = RequestValidation.validateTag(tag)
        if (!validatedTag.isValid) throw IllegalArgumentException(validatedTag.errorMessage)
        return blogEngine.getByTag(validatedTag.value, RequestValidation.validatePage(page).value)
    }

    suspend fun getByCategory(
        category: String,
        page: Int = 1,
    ): Page<Fragment> {
        val validatedCategory = RequestValidation.validateCategory(category)
        if (!validatedCategory.isValid) throw IllegalArgumentException(validatedCategory.errorMessage)
        return blogEngine.getByCategory(validatedCategory.value, RequestValidation.validatePage(page).value)
    }

    suspend fun getByAuthor(
        authorId: String,
        page: Int = 1,
    ): Page<Fragment> {
        val validatedAuthor = RequestValidation.validateAuthorId(authorId)
        if (!validatedAuthor.isValid) throw IllegalArgumentException(validatedAuthor.errorMessage)
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
        if (!validation.isValid) throw IllegalArgumentException(validation.errorMessage)
        return blogEngine.getByYear(validation.value)
    }

    suspend fun getByYearMonth(
        year: Int,
        month: Int,
    ): List<Fragment> {
        val validatedYear = RequestValidation.validateYear(year)
        val validatedMonth = RequestValidation.validateMonth(month)
        if (!validatedYear.isValid) throw IllegalArgumentException(validatedYear.errorMessage)
        if (!validatedMonth.isValid) throw IllegalArgumentException(validatedMonth.errorMessage)
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
        if (!validatedQuery.isValid) throw IllegalArgumentException(validatedQuery.errorMessage)
        return searchEngine?.search(validatedQuery.value, RequestValidation.validateMaxResults(maxResults).value) ?: emptyList()
    }

    suspend fun autocomplete(
        query: String,
        limit: Int = 10,
    ): List<SearchSuggestion> {
        val validatedQuery = RequestValidation.validateSearchQuery(query)
        if (!validatedQuery.isValid) throw IllegalArgumentException(validatedQuery.errorMessage)
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
        val uniqueRepos = allRepositories.distinctBy { System.identityHashCode(it) }
        val repoResults = uniqueRepos.associateWith { repo -> repo.getAllVisible() }

        val staticRepo = staticEngine.getRepository()
        val blogRepo = blogEngine.getRepository()

        val staticPages = (repoResults[staticRepo] ?: staticRepo.getAllVisible())
            .filter { it.template == FragmentTemplates.STATIC || it.template.isEmpty() || it.template == FragmentTemplates.DEFAULT }
            .map { fragment ->
                if (fragment.resolvedUrl != null) fragment
                else fragment.copy(resolvedUrl = "/page/${fragment.slug}")
            }

        val blogPosts = (repoResults[blogRepo] ?: blogRepo.getAllVisible())
            .filter { it.template in BlogEngine.BLOG_TEMPLATES }
            .map { fragment ->
                if (fragment.resolvedUrl != null) fragment
                else {
                    val date = fragment.date
                    val url = if (date != null) {
                        "/blog/${date.year}/${String.format(java.util.Locale.US, "%02d", date.monthValue)}/${fragment.slug}"
                    } else {
                        "/blog/${fragment.slug}"
                    }
                    fragment.copy(resolvedUrl = url)
                }
            }

        val additional = additionalRepositories.flatMap { repoResults[it] ?: it.getAllVisible() }
        val providerFragments = additionalFragmentProviders.flatMap { it() }
        return (staticPages + blogPosts + additional + providerFragments).distinctBy { it.slug }
    }

    suspend fun generateRssFeed(): String = generateAllFeeds().rssXml

    suspend fun generateSitemap(): String = generateAllFeeds().sitemapXml

    fun generateRobotsTxt(): String =
        buildString {
            appendLine("User-agent: *")
            appendLine("Allow: /")
            appendLine()
            appendLine("Sitemap: $siteUrl/sitemap.xml")
        }

    suspend fun generateLlmsTxt(): String = generateAllFeeds().llmsTxt

    data class FeedOutput(
        val rssXml: String,
        val sitemapXml: String,
        val llmsTxt: String,
    )

    suspend fun generateAllFeeds(): FeedOutput {
        val fragments = collectResolvedFragments()
        val cached = cachedFeedOutput
        if (cached != null && feedContentSnapshot === fragments) return cached

        val rssXml =
            rssGenerator.generateFeed(
                siteTitle = siteTitle,
                siteDescription = siteDescription,
                siteUrl = siteUrl,
                feedUrl = feedUrl,
                resolvedFragments = fragments,
            )
        val sitemapXml = sitemapGenerator.generateSitemap(fragments)
        val llmsTxt =
            LlmsTxtGenerator.generate(
                siteTitle = siteTitle,
                siteDescription = siteDescription,
                siteUrl = siteUrl,
                repositories = allRepositories,
                resolvedFragments = fragments,
            )
        val output = FeedOutput(rssXml, sitemapXml, llmsTxt)
        cachedFeedOutput = output
        feedContentSnapshot = fragments
        return output
    }

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
            archiveBaseUrl = "/blog/archive",
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

    fun securityHeaders(): Map<String, String> =
        buildMap {
            put("Content-Security-Policy", contentSecurityPolicy)
            put("X-Content-Type-Options", "nosniff")
            put("X-Frame-Options", "DENY")
            put("Referrer-Policy", "strict-origin-when-cross-origin")
            put("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
            if (siteUrl.startsWith("https")) {
                put("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
            }
        }

    fun close() {
        searchEngine?.close()
    }
}
