package io.github.rygel.fragments.javalin

import io.github.rygel.fragments.ArchiveNavigationLink
import io.github.rygel.fragments.AuthorViewModel
import io.github.rygel.fragments.FragmentViewModel
import io.github.rygel.fragments.NavigationLink
import io.github.rygel.fragments.adapter.FooterConfig
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.adapter.PaginationInfo
import io.github.rygel.fragments.adapter.SearchFormConfig
import io.javalin.config.RoutesConfig
import io.javalin.http.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.CompletableFuture

fun RoutesConfig.fragmentsRoutes(
    engine: FragmentsEngine,
    renderer: TemplateRenderer?,
) {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun Context.handleAsync(block: suspend () -> Unit) {
        future {
            val cf = CompletableFuture<Void?>()
            scope.launch {
                try {
                    block()
                    cf.complete(null)
                } catch (e: IOException) {
                    cf.completeExceptionally(e)
                }
            }
            cf
        }
    }

    val render: (Context, String, Any) -> Unit = { ctx, template, viewModel ->
        val html = renderer?.render(template, viewModel) ?: ""
        ctx.html(html)
    }

    val isHtmxRequest: (Context) -> Boolean = { ctx ->
        engine.isHtmxRequest(ctx.header(FragmentViewModel.HTMX_REQUEST_HEADER))
    }

    get("/") { ctx ->
        ctx.handleAsync {
            val fragments = engine.getHome()
            val viewModel =
                HomeViewModel(
                    fragments = fragments.map { FragmentViewModel(it) },
                    isPartialRender = isHtmxRequest(ctx),
                    navigationMenu = engine.nav(),
                    footer = engine.footer(),
                )
            render(ctx, "index", viewModel)
        }
    }

    get("/page/{slug}") { ctx ->
        val slug = ctx.pathParam("slug")
        ctx.handleAsync {
            val fragment = engine.getPage(slug)
            if (fragment != null) {
                val fragmentViewModel = FragmentViewModel(fragment, isHtmxRequest(ctx))
                val viewModel =
                    ContentViewModel(
                        viewModel = fragmentViewModel,
                        templateName = fragment.template,
                        navigationMenu = engine.nav(),
                        footer = engine.footer(),
                    )
                render(ctx, fragment.template, viewModel)
            } else {
                ctx.status(404).result("Page not found: $slug")
            }
        }
    }

    get("/blog") { ctx ->
        ctx.handleAsync {
            val pageResult = engine.getBlogOverview(page = 1)
            val viewModel =
                BlogOverviewViewModel(
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    isPartialRender = isHtmxRequest(ctx),
                    navigationMenu = engine.nav(),
                    pagination =
                        engine.pagination(
                            currentPage = pageResult.currentPage,
                            totalPages = pageResult.totalPages,
                            basePath = "/blog",
                        ),
                    footer = engine.footer(),
                )
            render(ctx, "blog_overview", viewModel)
        }
    }

    get("/blog/page/{page}") { ctx ->
        val page = ctx.pathParam("page").toIntOrNull() ?: 1
        ctx.handleAsync {
            val pageResult = engine.getBlogOverview(page = page)
            val viewModel =
                BlogOverviewViewModel(
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    isPartialRender = isHtmxRequest(ctx),
                    navigationMenu = engine.nav(),
                    pagination =
                        engine.pagination(
                            currentPage = pageResult.currentPage,
                            totalPages = pageResult.totalPages,
                            basePath = "/blog",
                        ),
                    footer = engine.footer(),
                )
            render(ctx, "blog_overview", viewModel)
        }
    }

    get("/blog/{year}/{month}/{slug}") { ctx ->
        val year = ctx.pathParam("year")
        val month = ctx.pathParam("month")
        val slug = ctx.pathParam("slug")
        ctx.handleAsync {
            val fragment = engine.getBlogPost(year, month, slug)
            if (fragment != null) {
                val fragmentViewModel = FragmentViewModel(fragment, isHtmxRequest(ctx))
                val viewModel =
                    ContentViewModel(
                        viewModel = fragmentViewModel,
                        templateName = fragment.template,
                        navigationMenu = engine.nav(),
                        footer = engine.footer(),
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
        ctx.handleAsync {
            val pageResult = engine.getByTag(tag, page)
            val viewModel =
                BlogOverviewViewModel(
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    tag = tag,
                    isPartialRender = isHtmxRequest(ctx),
                    navigationMenu = engine.nav(),
                    pagination =
                        engine.pagination(
                            currentPage = pageResult.currentPage,
                            totalPages = pageResult.totalPages,
                            basePath = "/blog",
                        ),
                    footer = engine.footer(),
                )
            render(ctx, "blog_overview", viewModel)
        }
    }

    get("/blog/category/{category}") { ctx ->
        val category = ctx.pathParam("category")
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 1
        ctx.handleAsync {
            val pageResult = engine.getByCategory(category, page)
            val viewModel =
                CategoryViewModel(
                    category = category,
                    fragments = pageResult.items.map { FragmentViewModel(it) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    isPartialRender = isHtmxRequest(ctx),
                    navigationMenu = engine.nav(),
                    pagination =
                        engine.pagination(
                            currentPage = pageResult.currentPage,
                            totalPages = pageResult.totalPages,
                            basePath = "/blog",
                        ),
                    footer = engine.footer(),
                )
            render(ctx, "blog_overview", viewModel)
        }
    }

    get("/blog/author/{slug}") { ctx ->
        val slug = ctx.pathParam("slug")
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 1
        ctx.handleAsync {
            val pageResult = engine.getByAuthor(slug, page)
            val authorViewModel = engine.getAuthor(slug)
            val viewModel =
                AuthorPageViewModel(
                    authorSlug = slug,
                    authorName = authorViewModel?.name,
                    author = authorViewModel,
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    isPartialRender = isHtmxRequest(ctx),
                    navigationMenu = engine.nav(),
                    pagination =
                        engine.pagination(
                            currentPage = pageResult.currentPage,
                            totalPages = pageResult.totalPages,
                            basePath = "/blog/author/$slug",
                        ),
                    footer = engine.footer(),
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
        ctx.handleAsync {
            val fragments = engine.getByYear(yearInt)
            val viewModel =
                ArchiveViewModel(
                    type = "year",
                    year = year,
                    fragments = fragments.map { FragmentViewModel(it) },
                    siteTitle = engine.siteTitle,
                    navigationMenu = engine.nav(),
                    footer = engine.footer(),
                    archiveYearLinks =
                        engine.generateArchiveYearLinks(
                            currentYear = yearInt,
                        ),
                    archiveBreadcrumbs =
                        engine.generateArchiveBreadcrumbs(
                            currentYear = yearInt,
                        ),
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

        ctx.handleAsync {
            val fragments = engine.getByYearMonth(yearInt, monthInt)
            val viewModel =
                ArchiveViewModel(
                    type = "year-month",
                    year = year,
                    month = month,
                    fragments = fragments.map { FragmentViewModel(it) },
                    siteTitle = engine.siteTitle,
                    navigationMenu = engine.nav(),
                    footer = engine.footer(),
                    archiveMonthLinks =
                        engine.generateArchiveMonthLinks(
                            year = yearInt,
                            currentMonth = monthInt,
                        ),
                    archiveBreadcrumbs =
                        engine.generateArchiveBreadcrumbs(
                            currentYear = yearInt,
                            currentMonth = monthInt,
                        ),
                )
            render(ctx, "archive", viewModel)
        }
    }

    get("/rss.xml") { ctx ->
        ctx.handleAsync {
            val rssXml = engine.generateRssFeed()
            ctx.contentType("application/rss+xml")
            ctx.result(rssXml)
        }
    }

    get("/sitemap.xml") { ctx ->
        ctx.handleAsync {
            val sitemapXml = engine.generateSitemap()
            ctx.contentType("application/xml")
            ctx.result(sitemapXml)
        }
    }

    get("/robots.txt") { ctx ->
        val body = engine.generateRobotsTxt()
        ctx.contentType("text/plain")
        ctx.result(body)
    }

    get("/llms.txt") { ctx ->
        ctx.handleAsync {
            val body = engine.generateLlmsTxt()
            ctx.contentType("text/plain")
            ctx.result(body)
        }
    }

    get("/search") { ctx ->
        val query = ctx.queryParam("q")
        if (query == null) {
            ctx.status(400).result("Query parameter 'q' is required")
            return@get
        }
        ctx.handleAsync {
            val results = engine.search(query)
            val viewModel =
                SearchViewModel(
                    query = query,
                    results = results.map { FragmentViewModel(it.fragment) },
                    siteTitle = engine.siteTitle,
                    navigationMenu = engine.nav(),
                    footer = engine.footer(),
                    searchForm = engine.searchForm(),
                )
            render(ctx, "search", viewModel)
        }
    }
}

data class ContentViewModel(
    val viewModel: FragmentViewModel,
    val templateName: String,
    val navigationMenu: List<NavigationLink>,
    val footer: FooterConfig,
)

data class SearchViewModel(
    val query: String,
    val results: List<FragmentViewModel>,
    val siteTitle: String,
    val navigationMenu: List<NavigationLink>,
    val footer: FooterConfig,
    val searchForm: SearchFormConfig,
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
    val archiveBreadcrumbs: List<ArchiveNavigationLink>? = null,
)

data class HomeViewModel(
    val fragments: List<FragmentViewModel>,
    val isPartialRender: Boolean = false,
    val navigationMenu: List<NavigationLink>,
    val footer: FooterConfig,
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
    val footer: FooterConfig,
)

data class TagViewModel(
    val tag: String,
    val fragments: List<FragmentViewModel>,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isPartialRender: Boolean = false,
)

data class AuthorPageViewModel(
    val authorSlug: String,
    val authorName: String? = null,
    val author: AuthorViewModel? = null,
    val fragments: List<FragmentViewModel>,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isPartialRender: Boolean = false,
    val navigationMenu: List<NavigationLink>,
    val pagination: PaginationInfo,
    val footer: FooterConfig,
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
    val footer: FooterConfig,
)

interface TemplateRenderer {
    fun render(
        template: String,
        viewModel: Any,
    ): String
}
