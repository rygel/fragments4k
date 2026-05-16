package io.github.rygel.fragments.javalin

import io.github.rygel.fragments.FragmentTemplates
import io.github.rygel.fragments.FragmentViewModel
import io.github.rygel.fragments.adapter.ArchiveViewModel
import io.github.rygel.fragments.adapter.AuthorPageViewModel
import io.github.rygel.fragments.adapter.BlogOverviewViewModel
import io.github.rygel.fragments.adapter.CategoryViewModel
import io.github.rygel.fragments.adapter.ContentViewModel
import io.github.rygel.fragments.adapter.ErrorResponse
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.adapter.HomeViewModel
import io.github.rygel.fragments.adapter.SearchViewModel
import io.github.rygel.fragments.adapter.TagViewModel
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

    before("*") { ctx ->
        engine.securityHeaders().forEach { (name, value) ->
            ctx.header(name, value)
        }
    }

    exception(IllegalArgumentException::class.java) { e, ctx ->
        ctx.status(400).json(ErrorResponse.badRequest(e.message ?: "Invalid request"))
    }

    exception(NoSuchElementException::class.java) { e, ctx ->
        ctx.status(404).json(ErrorResponse.notFound(e.message ?: "Resource not found"))
    }

    val render: (Context, String, Any) -> Unit = { ctx, template, viewModel ->
        val html = renderer?.render(template, viewModel) ?: ""
        ctx.html(html)
    }

    fun Context.writeJson(error: ErrorResponse) {
        contentType("application/json")
        val escapedError = error.error.replace("\\", "\\\\").replace("\"", "\\\"")
        val escapedMessage = error.message.replace("\\", "\\\\").replace("\"", "\\\"")
        result("{\"status\":${error.status},\"error\":\"$escapedError\",\"message\":\"$escapedMessage\"}")
    }

    val isHtmxRequest: (Context) -> Boolean = { ctx ->
        engine.isHtmxRequest(ctx.header(FragmentViewModel.HTMX_REQUEST_HEADER))
    }

    get("/") { ctx ->
        ctx.handleAsync {
            val fragments = engine.getHome()
            val viewModel =
                HomeViewModel(
                    fragments = fragments.map { FragmentViewModel(it, isHtmxRequest(ctx), siteUrl = engine.siteUrl) },
                    navigationMenu = engine.nav(),
                    footer = engine.footer(),
                )
            render(ctx, FragmentTemplates.INDEX, viewModel)
        }
    }

    get("/page/{slug}") { ctx ->
        val slug = ctx.pathParam("slug")
        ctx.handleAsync {
            val fragment = engine.getPage(slug)
            if (fragment != null) {
                val fragmentViewModel = FragmentViewModel(fragment, isHtmxRequest(ctx), siteUrl = engine.siteUrl)
                val viewModel =
                    ContentViewModel(
                        viewModel = fragmentViewModel,
                        templateName = fragment.template,
                        navigationMenu = engine.nav(),
                        footer = engine.footer(),
                        socialShareLinks = engine.socialShareLinks(fragment.title, fragment.url),
                    )
                render(ctx, fragment.template, viewModel)
            } else {
                ctx.status(404).writeJson(ErrorResponse.notFound("Page not found: $slug"))
            }
        }
    }

    get("/blog") { ctx ->
        ctx.handleAsync {
            val pageResult = engine.getBlogOverview(page = 1)
            val viewModel =
                BlogOverviewViewModel(
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx), siteUrl = engine.siteUrl) },
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
            render(ctx, FragmentTemplates.BLOG_OVERVIEW, viewModel)
        }
    }

    get("/blog/page/{page}") { ctx ->
        val page = ctx.pathParam("page").toIntOrNull() ?: 1
        ctx.handleAsync {
            val pageResult = engine.getBlogOverview(page = page)
            val viewModel =
                BlogOverviewViewModel(
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx), siteUrl = engine.siteUrl) },
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
            render(ctx, FragmentTemplates.BLOG_OVERVIEW, viewModel)
        }
    }

    get("/blog/{year}/{month}/{slug}") { ctx ->
        val year = ctx.pathParam("year")
        val month = ctx.pathParam("month")
        val slug = ctx.pathParam("slug")
        ctx.handleAsync {
            val fragment = engine.getBlogPost(year, month, slug)
            if (fragment != null) {
                val fragmentViewModel = FragmentViewModel(fragment, isHtmxRequest(ctx), siteUrl = engine.siteUrl)
                val viewModel =
                    ContentViewModel(
                        viewModel = fragmentViewModel,
                        templateName = fragment.template,
                        navigationMenu = engine.nav(),
                        footer = engine.footer(),
                        socialShareLinks = engine.socialShareLinks(fragment.title, fragment.url),
                    )
                render(ctx, fragment.template, viewModel)
            } else {
                ctx.status(404).writeJson(ErrorResponse.notFound("Post not found"))
            }
        }
    }

    get("/blog/tag/{tag}") { ctx ->
        val tag = ctx.pathParam("tag")
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 1
        ctx.handleAsync {
            val pageResult = engine.getByTag(tag, page)
            val viewModel =
                TagViewModel(
                    tag = tag,
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx), siteUrl = engine.siteUrl) },
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
                            basePath = "/blog/tag/$tag",
                        ),
                    footer = engine.footer(),
                )
            render(ctx, FragmentTemplates.BLOG_OVERVIEW, viewModel)
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
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx), siteUrl = engine.siteUrl) },
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
                            basePath = "/blog/category/$category",
                        ),
                    footer = engine.footer(),
                )
            render(ctx, FragmentTemplates.BLOG_OVERVIEW, viewModel)
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
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx), siteUrl = engine.siteUrl) },
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
            render(ctx, FragmentTemplates.BLOG_OVERVIEW, viewModel)
        }
    }

    get("/blog/archive/{year}") { ctx ->
        val year = ctx.pathParam("year")
        val yearInt = year.toIntOrNull() ?: throw IllegalArgumentException("Invalid year")
        ctx.handleAsync {
            val fragments = engine.getByYear(yearInt)
            val viewModel =
                ArchiveViewModel(
                    type = "year",
                    year = year,
                    fragments = fragments.map { FragmentViewModel(it, isHtmxRequest(ctx), siteUrl = engine.siteUrl) },
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
            render(ctx, FragmentTemplates.ARCHIVE, viewModel)
        }
    }

    get("/blog/archive/{year}/{month}") { ctx ->
        val year = ctx.pathParam("year")
        val month = ctx.pathParam("month")
        val yearInt = year.toIntOrNull() ?: throw IllegalArgumentException("Invalid year")
        val monthInt = month.toIntOrNull() ?: throw IllegalArgumentException("Invalid month")

        ctx.handleAsync {
            val fragments = engine.getByYearMonth(yearInt, monthInt)
            val viewModel =
                ArchiveViewModel(
                    type = "year-month",
                    year = year,
                    month = month,
                    fragments = fragments.map { FragmentViewModel(it, isHtmxRequest(ctx), siteUrl = engine.siteUrl) },
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
            render(ctx, FragmentTemplates.ARCHIVE, viewModel)
        }
    }

    val rssHandler: (Context) -> Unit = { ctx ->
        ctx.handleAsync {
            val rssXml = engine.generateRssFeed()
            ctx.contentType("application/rss+xml")
            ctx.result(rssXml)
        }
    }

    get("/rss.xml", rssHandler)
    get("/feed.xml", rssHandler)

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
        val query = ctx.queryParam("q") ?: throw IllegalArgumentException("Query parameter 'q' is required")
        ctx.handleAsync {
            val results = engine.search(query)
            val viewModel =
                SearchViewModel(
                    query = query,
                    results = results.map { FragmentViewModel(it.fragment, isHtmxRequest(ctx), siteUrl = engine.siteUrl) },
                    siteTitle = engine.siteTitle,
                    navigationMenu = engine.nav(),
                    footer = engine.footer(),
                    searchForm = engine.searchForm(),
                )
            render(ctx, FragmentTemplates.SEARCH, viewModel)
        }
    }

    get("/api/autocomplete") { ctx ->
        val query = ctx.queryParam("q") ?: throw IllegalArgumentException("Query parameter 'q' is required")
        val limit = ctx.queryParam("limit")?.toIntOrNull() ?: 10
        ctx.handleAsync {
            val suggestions = engine.autocomplete(query, limit)
            ctx.json(suggestions)
        }
    }
}

interface TemplateRenderer {
    fun render(
        template: String,
        viewModel: Any,
    ): String
}
