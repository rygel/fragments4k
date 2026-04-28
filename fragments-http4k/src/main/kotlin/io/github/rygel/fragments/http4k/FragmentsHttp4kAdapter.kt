package io.github.rygel.fragments.http4k

import io.github.rygel.fragments.ArchiveNavigationLink
import io.github.rygel.fragments.AuthorViewModel
import io.github.rygel.fragments.FragmentViewModel
import io.github.rygel.fragments.NavigationLink
import io.github.rygel.fragments.SeoMetadata
import io.github.rygel.fragments.adapter.FooterConfig
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.adapter.PaginationInfo
import io.github.rygel.fragments.adapter.SearchFormConfig
import kotlinx.coroutines.runBlocking
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

class FragmentsHttp4kAdapter(
    private val engine: FragmentsEngine,
    private val renderer: TemplateRenderer,
) {
    fun createRoutes(): RoutingHttpHandler =
        routes(
            "/" bind GET to { request -> handleHome(request) },
            "/page/{slug}" bind GET to { request -> handlePage(request) },
            "/blog" bind GET to { request -> handleBlogOverview(request) },
            "/blog/page/{page}" bind GET to { request -> handleBlogOverview(request) },
            "/blog/{year}/{month}/{slug}" bind GET to { request -> handleBlogPost(request) },
            "/blog/tag/{tag}" bind GET to { request -> handleByTag(request) },
            "/blog/category/{category}" bind GET to { request -> handleByCategory(request) },
            "/blog/author/{slug}" bind GET to { request -> handleByAuthor(request) },
            "/blog/archive/{year}" bind GET to { request -> handleArchiveYear(request) },
            "/blog/archive/{year}/{month}" bind GET to { request -> handleArchiveYearMonth(request) },
            "/search" bind GET to { request -> handleSearch(request) },
            "/rss.xml" bind GET to { _ -> handleRss() },
            "/sitemap.xml" bind GET to { _ -> handleSitemap() },
            "/robots.txt" bind GET to { _ -> handleRobotsTxt() },
            "/llms.txt" bind GET to { _ -> handleLlmsTxt() },
        )

    private fun handleHome(request: Request): Response =
        runBlocking {
            val fragments = engine.getHome()
            val viewModel =
                BlogOverviewViewModel(
                    fragments = fragments.map { FragmentViewModel(it) },
                    currentPage = 1,
                    totalPages = 1,
                    templateName = "index",
                    isPartialRender = isHtmxRequest(request),
                    navigationMenu = engine.nav(),
                    pagination =
                        PaginationInfo(
                            currentPage = 1,
                            totalPages = 1,
                            hasPrevious = false,
                            hasNext = false,
                            text = "",
                        ),
                    footer = engine.footer(),
                )
            renderResponse(viewModel)
        }

    private fun handlePage(request: Request): Response {
        val slug = request.path("slug") ?: return Response(Status.NOT_FOUND)
        return runBlocking {
            val fragment = engine.getPage(slug)
            if (fragment != null) {
                val fragmentViewModel = FragmentViewModel(fragment, isHtmxRequest(request), siteUrl = engine.siteUrl)
                val viewModel =
                    ContentViewModel(
                        viewModel = fragmentViewModel,
                        templateName = fragment.template,
                        navigationMenu = engine.nav(),
                        footer = engine.footer(),
                        seoMetadata = engine.generateSeoMetadata(fragment, pagePath = "page/${fragment.slug}"),
                    )
                renderResponse(viewModel)
            } else {
                Response(Status.NOT_FOUND).body("Page not found: $slug")
            }
        }
    }

    private fun handleBlogOverview(request: Request): Response {
        val page = request.path("page")?.toIntOrNull() ?: 1
        return runBlocking {
            val pageResult = engine.getBlogOverview(page)
            val viewModel =
                BlogOverviewViewModel(
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(request)) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    templateName = "blog_overview",
                    isPartialRender = isHtmxRequest(request),
                    navigationMenu = engine.nav(),
                    pagination =
                        engine.pagination(
                            currentPage = pageResult.currentPage,
                            totalPages = pageResult.totalPages,
                            basePath = "/blog",
                        ),
                    footer = engine.footer(),
                )
            renderResponse(viewModel)
        }
    }

    private fun handleBlogPost(request: Request): Response {
        val year = request.path("year") ?: return Response(Status.NOT_FOUND)
        val month = request.path("month") ?: return Response(Status.NOT_FOUND)
        val slug = request.path("slug") ?: return Response(Status.NOT_FOUND)

        return runBlocking {
            val fragment = engine.getBlogPost(year, month, slug)
            if (fragment != null) {
                val fragmentViewModel = FragmentViewModel(fragment, isHtmxRequest(request), siteUrl = engine.siteUrl)
                val viewModel =
                    ContentViewModel(
                        viewModel = fragmentViewModel,
                        templateName = fragment.template,
                        navigationMenu = engine.nav(),
                        footer = engine.footer(),
                        seoMetadata = engine.generateSeoMetadata(fragment, pagePath = "blog/$year/$month/$slug"),
                    )
                renderResponse(viewModel)
            } else {
                Response(Status.NOT_FOUND).body("Post not found")
            }
        }
    }

    private fun handleByTag(request: Request): Response {
        val tag = request.path("tag") ?: return Response(Status.NOT_FOUND)
        val page = request.query("page")?.toIntOrNull() ?: 1
        return runBlocking {
            val pageResult = engine.getByTag(tag, page)
            val viewModel =
                BlogOverviewViewModel(
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(request)) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    templateName = "blog_overview",
                    isPartialRender = isHtmxRequest(request),
                    tag = tag,
                    navigationMenu = engine.nav(),
                    pagination =
                        engine.pagination(
                            currentPage = pageResult.currentPage,
                            totalPages = pageResult.totalPages,
                            basePath = "/blog",
                        ),
                    footer = engine.footer(),
                )
            renderResponse(viewModel)
        }
    }

    private fun handleByCategory(request: Request): Response {
        val category = request.path("category") ?: return Response(Status.NOT_FOUND)
        return runBlocking {
            val pageResult = engine.getByCategory(category, 1)
            val viewModel =
                BlogOverviewViewModel(
                    fragments = pageResult.items.map { FragmentViewModel(it) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    templateName = "blog_overview",
                    isPartialRender = isHtmxRequest(request),
                    navigationMenu = engine.nav(),
                    pagination =
                        engine.pagination(
                            currentPage = pageResult.currentPage,
                            totalPages = pageResult.totalPages,
                            basePath = "/blog",
                        ),
                    footer = engine.footer(),
                )
            renderResponse(viewModel)
        }
    }

    private fun handleByAuthor(request: Request): Response {
        val slug = request.path("slug") ?: return Response(Status.NOT_FOUND)
        val page = request.query("page")?.toIntOrNull() ?: 1
        return runBlocking {
            val pageResult = engine.getByAuthor(slug, page)
            val authorViewModel = engine.getAuthor(slug)
            val viewModel =
                AuthorPageViewModel(
                    authorSlug = slug,
                    authorName = authorViewModel?.name,
                    author = authorViewModel,
                    fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(request)) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    isPartialRender = isHtmxRequest(request),
                    navigationMenu = engine.nav(),
                    pagination =
                        engine.pagination(
                            currentPage = pageResult.currentPage,
                            totalPages = pageResult.totalPages,
                            basePath = "/blog/author/$slug",
                        ),
                    footer = engine.footer(),
                )
            renderResponse(viewModel)
        }
    }

    private fun handleRss(): Response =
        runBlocking {
            val rssXml = engine.generateRssFeed()
            Response(Status.OK)
                .header("Content-Type", "application/rss+xml; charset=utf-8")
                .body(rssXml)
        }

    private fun handleSitemap(): Response =
        runBlocking {
            val sitemapXml = engine.generateSitemap()
            Response(Status.OK)
                .header("Content-Type", "application/xml; charset=utf-8")
                .body(sitemapXml)
        }

    private fun handleRobotsTxt(): Response {
        val body = engine.generateRobotsTxt()
        return Response(Status.OK)
            .header("Content-Type", "text/plain; charset=utf-8")
            .body(body)
    }

    private fun handleLlmsTxt(): Response =
        runBlocking {
            val body = engine.generateLlmsTxt()
            Response(Status.OK)
                .header("Content-Type", "text/plain; charset=utf-8")
                .body(body)
        }

    private fun handleSearch(request: Request): Response {
        val query = request.query("q") ?: return Response(Status.BAD_REQUEST).body("Query parameter 'q' is required")
        return runBlocking {
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
            renderResponse(viewModel)
        }
    }

    private fun handleArchiveYear(request: Request): Response {
        val year = request.path("year") ?: return Response(Status.NOT_FOUND)
        return runBlocking {
            val yearInt = year.toIntOrNull() ?: return@runBlocking Response(Status.BAD_REQUEST).body("Invalid year")
            val fragments = engine.getByYear(yearInt)
            val viewModel =
                ArchiveViewModel(
                    type = "year",
                    year = year,
                    fragments = fragments.map { FragmentViewModel(it) },
                    siteTitle = engine.siteTitle,
                    navigationMenu = engine.nav(),
                    footer = engine.footer(),
                    archiveYearLinks = engine.generateArchiveYearLinks(currentYear = yearInt),
                    archiveBreadcrumbs = engine.generateArchiveBreadcrumbs(currentYear = yearInt),
                )
            renderResponse(viewModel)
        }
    }

    private fun handleArchiveYearMonth(request: Request): Response {
        val year = request.path("year") ?: return Response(Status.NOT_FOUND)
        val month = request.path("month") ?: return Response(Status.NOT_FOUND)
        return runBlocking {
            val yearInt = year.toIntOrNull() ?: return@runBlocking Response(Status.BAD_REQUEST).body("Invalid year")
            val monthInt = month.toIntOrNull() ?: return@runBlocking Response(Status.BAD_REQUEST).body("Invalid month")
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
                    archiveMonthLinks = engine.generateArchiveMonthLinks(year = yearInt, currentMonth = monthInt),
                    archiveBreadcrumbs = engine.generateArchiveBreadcrumbs(currentYear = yearInt, currentMonth = monthInt),
                )
            renderResponse(viewModel)
        }
    }

    private fun isHtmxRequest(request: Request): Boolean = engine.isHtmxRequest(request.header(FragmentViewModel.HTMX_REQUEST_HEADER))

    private fun renderResponse(viewModel: ViewModel): Response {
        val rendered = renderer(viewModel)
        return Response(Status.OK).body(rendered)
    }

    data class ContentViewModel(
        val viewModel: FragmentViewModel,
        private val templateName: String,
        val navigationMenu: List<NavigationLink>,
        val footer: FooterConfig,
        val seoMetadata: SeoMetadata? = null,
    ) : ViewModel {
        override fun template(): String = templateName
    }

    data class BlogOverviewViewModel(
        val fragments: List<FragmentViewModel>,
        val currentPage: Int,
        val totalPages: Int,
        val hasNext: Boolean = false,
        val hasPrevious: Boolean = false,
        private val templateName: String = "blog_overview",
        val tag: String? = null,
        val category: String? = null,
        val isPartialRender: Boolean = false,
        val navigationMenu: List<NavigationLink>,
        val pagination: PaginationInfo,
        val footer: FooterConfig,
    ) : ViewModel {
        override fun template(): String = templateName
    }

    data class CategoryViewModel(
        val category: String,
        val fragments: List<FragmentViewModel>,
        val currentPage: Int,
        val totalPages: Int,
        val hasNext: Boolean = false,
        val hasPrevious: Boolean = false,
        private val templateName: String = "blog_overview",
        val isPartialRender: Boolean = false,
        val navigationMenu: List<NavigationLink>,
        val pagination: PaginationInfo,
        val footer: FooterConfig,
    ) : ViewModel {
        override fun template(): String = templateName
    }

    data class SearchViewModel(
        val query: String,
        val results: List<FragmentViewModel>,
        val siteTitle: String,
        private val templateName: String = "search",
        val navigationMenu: List<NavigationLink>,
        val footer: FooterConfig,
        val searchForm: SearchFormConfig,
    ) : ViewModel {
        override fun template(): String = templateName
    }

    data class AuthorPageViewModel(
        val authorSlug: String,
        val authorName: String? = null,
        val author: AuthorViewModel? = null,
        val fragments: List<FragmentViewModel>,
        val currentPage: Int,
        val totalPages: Int,
        val hasNext: Boolean = false,
        val hasPrevious: Boolean = false,
        private val templateName: String = "blog_overview",
        val isPartialRender: Boolean = false,
        val navigationMenu: List<NavigationLink>,
        val pagination: PaginationInfo,
        val footer: FooterConfig,
    ) : ViewModel {
        override fun template(): String = templateName
    }

    data class ArchiveViewModel(
        val type: String,
        val year: String,
        val month: String? = null,
        val fragments: List<FragmentViewModel>,
        val siteTitle: String,
        private val templateName: String = "archive",
        val navigationMenu: List<NavigationLink>,
        val footer: FooterConfig,
        val archiveYearLinks: List<ArchiveNavigationLink>? = null,
        val archiveMonthLinks: List<ArchiveNavigationLink>? = null,
        val archiveBreadcrumbs: List<ArchiveNavigationLink>? = null,
    ) : ViewModel {
        override fun template(): String = templateName
    }
}
