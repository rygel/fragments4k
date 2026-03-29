package io.github.rygel.fragments.javalin

import io.github.rygel.fragments.*
import io.github.rygel.fragments.blog.BlogEngine
import io.github.rygel.fragments.lucene.LuceneSearchEngine
import io.github.rygel.fragments.rss.RssGenerator
import io.github.rygel.fragments.sitemap.SitemapGenerator
import io.github.rygel.fragments.static.StaticPageEngine
import io.javalin.Javalin
import io.javalin.http.Context
import kotlinx.coroutines.runBlocking

fun Javalin.fragmentsRoutes(
    staticEngine: StaticPageEngine,
    blogEngine: BlogEngine,
    renderer: TemplateRenderer?,
    searchEngine: LuceneSearchEngine,
    siteTitle: String = "My Blog",
    siteDescription: String = "My Awesome Blog",
    siteUrl: String = "http://localhost:8080",
    feedUrl: String = "http://localhost:8080/rss.xml"
) {
    val rssGenerator = RssGenerator(
        repository = staticEngine.getRepository()
    )
    val sitemapGenerator = SitemapGenerator(
        repository = staticEngine.getRepository(),
        siteUrl = siteUrl,
        lastModified = null
    )

    val render: (Context, String, Any) -> Unit = { ctx, template, viewModel ->
        val html = renderer?.render(template, viewModel) ?: ""
        ctx.html(html)
    }

    val isHtmxRequest: (Context) -> Boolean = { ctx ->
        ctx.header(FragmentViewModel.HTMX_REQUEST_HEADER)?.lowercase() == "true"
    }

    get("/") { ctx ->
        runBlocking {
            val fragments = staticEngine.getAllStaticPages()
            val viewModel = HomeViewModel(
                fragments = fragments.map { FragmentViewModel(it) },
                isPartialRender = isHtmxRequest(ctx),
                navigationMenu = NavigationMenuGenerator.generateMainMenu(),
                footer = FooterGenerator.generate()
            )
            render(ctx, "index", viewModel)
        }
    }

    get("/page/{slug}") { ctx ->
        val slug = ctx.pathParam("slug")
        runBlocking {
            val fragment = staticEngine.getPage(slug)
            if (fragment != null) {
                val fragmentViewModel = FragmentViewModel(fragment, isHtmxRequest(ctx))
                val viewModel = ContentViewModel(
                    viewModel = fragmentViewModel,
                    templateName = fragment.template,
                    navigationMenu = NavigationMenuGenerator.generateMainMenu(),
                    footer = FooterGenerator.generate()
                )
                render(ctx, fragment.template, viewModel)
            } else {
                ctx.status(404).result("Page not found: $slug")
            }
        }
    }

    get("/blog") { ctx ->
        runBlocking {
            val pageResult = blogEngine.getOverview(includeDrafts = false, page = 1)
            val viewModel = BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(ctx),
                navigationMenu = NavigationMenuGenerator.generateMainMenu(
                    siteUrl = "/",
                    blogUrl = "/blog",
                    archiveUrl = "/blog/archive",
                    searchUrl = "/search"
                ),
                pagination = PaginationGenerator.generateSimpleControls(
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    basePath = "/blog"
                ),
                footer = FooterGenerator.generate()
            )
            render(ctx, "blog_overview", viewModel)
        }
    }

    get("/blog/page/{page}") { ctx ->
        val page = ctx.pathParam("page").toIntOrNull() ?: 1
        runBlocking {
            val pageResult = blogEngine.getOverview(includeDrafts = false, page = page)
            val viewModel = BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(ctx),
                navigationMenu = NavigationMenuGenerator.generateMainMenu(
                    siteUrl = "/",
                    blogUrl = "/blog",
                    archiveUrl = "/blog/archive",
                    searchUrl = "/search"
                ),
                pagination = PaginationGenerator.generateSimpleControls(
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    basePath = "/blog"
                ),
                footer = FooterGenerator.generate()
            )
            render(ctx, "blog_overview", viewModel)
        }
    }

    get("/blog/{year}/{month}/{slug}") { ctx ->
        val year = ctx.pathParam("year")
        val month = ctx.pathParam("month")
        val slug = ctx.pathParam("slug")
        runBlocking {
            val fragment = blogEngine.getPost(year, month, slug)
            if (fragment != null) {
                val fragmentViewModel = FragmentViewModel(fragment, isHtmxRequest(ctx))
                val viewModel = ContentViewModel(
                    viewModel = fragmentViewModel,
                    templateName = fragment.template,
                    navigationMenu = NavigationMenuGenerator.generateMainMenu(),
                    footer = FooterGenerator.generate()
                )
                render(ctx, fragment.template, viewModel)
            } else {
                ctx.status(404).result("Post not found")
            }
        }
    }

    get("/blog/tag/{tag}") { ctx ->
        val tag = ctx.pathParam("tag")
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 1
        runBlocking {
            val pageResult = blogEngine.getByTag(tag, page)
            val viewModel = BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                tag = tag,
                isPartialRender = isHtmxRequest(ctx),
                navigationMenu = NavigationMenuGenerator.generateMainMenu(
                    siteUrl = "/",
                    blogUrl = "/blog",
                    archiveUrl = "/blog/archive",
                    searchUrl = "/search"
                ),
                pagination = PaginationGenerator.generateSimpleControls(
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    basePath = "/blog"
                ),
                footer = FooterGenerator.generate()
            )
            render(ctx, "blog_overview", viewModel)
        }
    }

    get("/blog/category/{category}") { ctx ->
        val category = ctx.pathParam("category")
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 1
        runBlocking {
            val pageResult = blogEngine.getByCategory(category, page)
            val viewModel = CategoryViewModel(
                category = category,
                fragments = pageResult.items.map { FragmentViewModel(it) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(ctx),
                navigationMenu = NavigationMenuGenerator.generateMainMenu(
                    siteUrl = "/",
                    blogUrl = "/blog",
                    archiveUrl = "/blog/archive",
                    searchUrl = "/search"
                ),
                pagination = PaginationGenerator.generateSimpleControls(
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    basePath = "/blog"
                ),
                footer = FooterGenerator.generate()
            )
            render(ctx, "blog_overview", viewModel)
        }
    }

    get("/blog/archive/{year}") { ctx ->
        val year = ctx.pathParam("year")
        val yearInt = year.toIntOrNull()
        if (yearInt == null) {
            ctx.status(400).result("Invalid year")
            return@get
        }
        runBlocking {
            val fragments = blogEngine.getByYear(yearInt)
            val viewModel = ArchiveViewModel(
                type = "year",
                year = year,
                fragments = fragments.map { FragmentViewModel(it) },
                siteTitle = siteTitle,
                navigationMenu = NavigationMenuGenerator.generateMainMenu(
                    siteUrl = "/",
                    blogUrl = "/blog",
                    archiveUrl = "/blog/archive",
                    searchUrl = "/search"
                ),
                footer = FooterGenerator.generate(),
                archiveYearLinks = ArchiveNavigationGenerator.generateYearLinks(
                    baseUrl = "/blog/archive",
                    availableYears = emptyList(),
                    currentYear = yearInt
                ),
                archiveBreadcrumbs = ArchiveNavigationGenerator.generateBreadcrumbs(
                    currentYear = yearInt
                )
            )
            render(ctx, "archive", viewModel)
        }
    }

    get("/blog/archive/{year}/{month}") { ctx ->
        val year = ctx.pathParam("year")
        val month = ctx.pathParam("month")
        val yearInt = year.toIntOrNull()
        val monthInt = month.toIntOrNull()

        if (yearInt == null) {
            ctx.status(400).result("Invalid year")
            return@get
        }
        if (monthInt == null) {
            ctx.status(400).result("Invalid month")
            return@get
        }

        runBlocking {
            val fragments = blogEngine.getByYearMonth(yearInt, monthInt)
            val viewModel = ArchiveViewModel(
                type = "year-month",
                year = year,
                month = month,
                fragments = fragments.map { FragmentViewModel(it) },
                siteTitle = siteTitle,
                navigationMenu = NavigationMenuGenerator.generateMainMenu(
                    siteUrl = "/",
                    blogUrl = "/blog",
                    archiveUrl = "/blog/archive",
                    searchUrl = "/search"
                ),
                footer = FooterGenerator.generate(),
                archiveMonthLinks = ArchiveNavigationGenerator.generateMonthLinks(
                    year = yearInt,
                    currentMonth = monthInt
                ),
                archiveBreadcrumbs = ArchiveNavigationGenerator.generateBreadcrumbs(
                    currentYear = yearInt,
                    currentMonth = monthInt
                )
            )
            render(ctx, "archive", viewModel)
        }
    }

    get("/rss.xml") { ctx ->
        runBlocking {
            val rssXml = rssGenerator.generateFeed(
                siteTitle = siteTitle,
                siteDescription = siteDescription,
                siteUrl = siteUrl,
                feedUrl = feedUrl
            )
            ctx.contentType("application/rss+xml")
            ctx.result(rssXml)
        }
    }

    get("/sitemap.xml") { ctx ->
        runBlocking {
            val sitemapXml = sitemapGenerator.generateSitemap()
            ctx.contentType("application/xml")
            ctx.result(sitemapXml)
        }
    }

    get("/search") { ctx ->
        val query = ctx.queryParam("q")
        if (query == null) {
            ctx.status(400).result("Query parameter 'q' is required")
            return@get
        }
        runBlocking {
            val results = searchEngine.search(query, maxResults = 50)
            val viewModel = SearchViewModel(
                query = query,
                results = results.map { FragmentViewModel(it.fragment) },
                siteTitle = siteTitle,
                navigationMenu = NavigationMenuGenerator.generateMainMenu(
                    siteUrl = "/",
                    blogUrl = "/blog",
                    archiveUrl = "/blog/archive",
                    searchUrl = null
                ),
                footer = FooterGenerator.generate(),
                searchForm = SearchFormGenerator.generate(
                    actionUrl = "/search",
                    paramName = "q",
                    placeholderText = "Search articles...",
                    buttonText = "Search",
                    method = "get"
                )
            )
            render(ctx, "search", viewModel)
        }
    }

}

data class ContentViewModel(
    val viewModel: FragmentViewModel,
    val templateName: String,
    val navigationMenu: List<NavigationLink>,
    val footer: FooterConfig
)

data class SearchViewModel(
    val query: String,
    val results: List<FragmentViewModel>,
    val siteTitle: String,
    val navigationMenu: List<NavigationLink>,
    val footer: FooterConfig,
    val searchForm: SearchFormConfig
)

data class ArchiveViewModel(
    val type: String,
    val year: String,
    val month: String? = null,
    val fragments: List<FragmentViewModel>,
    val siteTitle: String,
    val navigationMenu: List<NavigationLink>,
    val footer: FooterConfig,
    val archiveYearLinks: List<ArchiveNavigationLink>? = null,
    val archiveMonthLinks: List<ArchiveNavigationLink>? = null,
    val archiveBreadcrumbs: List<ArchiveNavigationLink>? = null
)

data class HomeViewModel(
    val fragments: List<FragmentViewModel>,
    val isPartialRender: Boolean = false,
    val navigationMenu: List<NavigationLink>,
    val footer: FooterConfig
)

data class BlogOverviewViewModel(
    val fragments: List<FragmentViewModel>,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val tag: String? = null,
    val category: String? = null,
    val isPartialRender: Boolean = false,
    val navigationMenu: List<NavigationLink>,
    val pagination: PaginationInfo,
    val footer: FooterConfig
)

data class TagViewModel(
    val tag: String,
    val fragments: List<FragmentViewModel>,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isPartialRender: Boolean = false
)

data class CategoryViewModel(
    val category: String,
    val fragments: List<FragmentViewModel>,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isPartialRender: Boolean = false,
    val navigationMenu: List<NavigationLink>,
    val pagination: PaginationInfo,
    val footer: FooterConfig
)

interface TemplateRenderer {
    fun render(template: String, viewModel: Any): String
}
