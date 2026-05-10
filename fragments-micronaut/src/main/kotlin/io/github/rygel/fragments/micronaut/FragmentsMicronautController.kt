package io.github.rygel.fragments.micronaut

import io.github.rygel.fragments.AuthorViewModel
import io.github.rygel.fragments.FragmentViewModel
import io.github.rygel.fragments.adapter.ArchiveViewModel
import io.github.rygel.fragments.adapter.AuthorPageViewModel
import io.github.rygel.fragments.adapter.BlogOverviewViewModel
import io.github.rygel.fragments.adapter.CategoryViewModel
import io.github.rygel.fragments.adapter.ContentViewModel
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.adapter.HomeViewModel
import io.github.rygel.fragments.adapter.SearchViewModel
import io.github.rygel.fragments.adapter.TagViewModel
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.QueryValue
import jakarta.inject.Inject

@Controller("/")
class FragmentsMicronautController
    @Inject
    constructor(
        private val engine: FragmentsEngine,
    ) {
        @Get("/")
        suspend fun home(headers: HttpHeaders): HttpResponse<Any> {
            val fragments = engine.getHome()
            val isPartial = isHtmxRequest(headers)
            val viewModel =
                HomeViewModel(
                    fragments = fragments.map { FragmentViewModel(it, isPartial) },
                    isPartialRender = isPartial,
                    navigationMenu = engine.nav(),
                    footer = engine.footer(),
                )
            return HttpResponse.ok(viewModel)
        }

        @Get("/page/{slug}")
        suspend fun page(
            slug: String,
            headers: HttpHeaders,
        ): HttpResponse<Any> {
            val fragment = engine.getPage(slug)
            val isPartial = isHtmxRequest(headers)
            return if (fragment != null) {
                val contentViewModel =
                    ContentViewModel(
                        viewModel = FragmentViewModel(fragment, isPartial),
                        templateName = fragment.template,
                        navigationMenu = engine.nav(),
                        footer = engine.footer(),
                    )
                HttpResponse.ok(contentViewModel)
            } else {
                HttpResponse.notFound("Page not found")
            }
        }

        @Get("/blog")
        suspend fun blogOverview(
            @QueryValue(defaultValue = "1") page: Int,
            headers: HttpHeaders,
        ): HttpResponse<Any> {
            val pageResult = engine.getBlogOverview(page)
            val isPartial = isHtmxRequest(headers)
            val viewModel =
                BlogOverviewViewModel(
                    fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    isPartialRender = isPartial,
                    navigationMenu = engine.nav(),
                    pagination = engine.pagination(pageResult.currentPage, pageResult.totalPages, "/blog"),
                    footer = engine.footer(),
                )
            return HttpResponse.ok(viewModel)
        }

        @Get("/blog/page/{page}")
        suspend fun blogOverviewByPath(
            page: Int,
            headers: HttpHeaders,
        ): HttpResponse<Any> = blogOverview(page, headers)

        @Get("/blog/{year}/{month}/{slug}")
        suspend fun blogPost(
            year: String,
            month: String,
            slug: String,
            headers: HttpHeaders,
        ): HttpResponse<Any> {
            val fragment = engine.getBlogPost(year, month, slug)
            val isPartial = isHtmxRequest(headers)
            return if (fragment != null) {
                val contentViewModel =
                    ContentViewModel(
                        viewModel = FragmentViewModel(fragment, isPartial),
                        templateName = fragment.template,
                        navigationMenu = engine.nav(),
                        footer = engine.footer(),
                    )
                HttpResponse.ok(contentViewModel)
            } else {
                HttpResponse.notFound("Post not found")
            }
        }

        @Get("/blog/tag/{tag}")
        suspend fun byTag(
            tag: String,
            @QueryValue(defaultValue = "1") page: Int,
            headers: HttpHeaders,
        ): HttpResponse<Any> {
            val pageResult = engine.getByTag(tag, page)
            val isPartial = isHtmxRequest(headers)
            val viewModel =
                TagViewModel(
                    tag = tag,
                    fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    isPartialRender = isPartial,
                    navigationMenu = engine.nav(),
                    pagination = engine.pagination(pageResult.currentPage, pageResult.totalPages, "/blog"),
                    footer = engine.footer(),
                )
            return HttpResponse.ok(viewModel)
        }

        @Get("/blog/category/{category}")
        suspend fun byCategory(
            category: String,
            @QueryValue(defaultValue = "1") page: Int,
            headers: HttpHeaders,
        ): HttpResponse<Any> {
            val pageResult = engine.getByCategory(category, page)
            val isPartial = isHtmxRequest(headers)
            val viewModel =
                CategoryViewModel(
                    category = category,
                    fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    isPartialRender = isPartial,
                    navigationMenu = engine.nav(),
                    pagination = engine.pagination(pageResult.currentPage, pageResult.totalPages, "/blog"),
                    footer = engine.footer(),
                )
            return HttpResponse.ok(viewModel)
        }

        @Get("/blog/author/{slug}")
        suspend fun byAuthor(
            slug: String,
            @QueryValue(defaultValue = "1") page: Int,
            headers: HttpHeaders,
        ): HttpResponse<Any> {
            val pageResult = engine.getByAuthor(slug, page)
            val isPartial = isHtmxRequest(headers)
            val author = engine.getAuthor(slug)
            val viewModel =
                AuthorPageViewModel(
                    authorSlug = slug,
                    authorName = author?.name,
                    author = author,
                    fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    isPartialRender = isPartial,
                    navigationMenu = engine.nav(),
                    pagination = engine.pagination(pageResult.currentPage, pageResult.totalPages, "/blog/author/$slug"),
                    footer = engine.footer(),
                )
            return HttpResponse.ok(viewModel)
        }

        private fun isHtmxRequest(headers: HttpHeaders): Boolean = engine.isHtmxRequest(headers.get(FragmentViewModel.HTMX_REQUEST_HEADER))

        @Get("/rss.xml")
        @Produces(value = ["application/rss+xml;charset=utf-8"])
        suspend fun rss(): HttpResponse<String> {
            val rssXml = engine.generateRssFeed()
            return HttpResponse
                .ok(rssXml)
                .header("Content-Type", "application/rss+xml; charset=utf-8")
        }

        @Get("/feed.xml")
        @Produces(value = ["application/rss+xml;charset=utf-8"])
        suspend fun feed(): HttpResponse<String> = rss()

        @Get("/blog/archive/{year}")
        suspend fun archiveYear(
            year: String,
            headers: HttpHeaders,
        ): HttpResponse<Any> {
            val yearInt = year.toIntOrNull() ?: return HttpResponse.badRequest("Invalid year")
            val fragments = engine.getByYear(yearInt)
            val isPartial = isHtmxRequest(headers)
            val viewModel =
                ArchiveViewModel(
                    type = "year",
                    year = year,
                    fragments = fragments.map { FragmentViewModel(it, isPartial) },
                    siteTitle = engine.siteTitle,
                    navigationMenu = engine.nav(),
                    footer = engine.footer(),
                    archiveYearLinks = engine.generateArchiveYearLinks(currentYear = yearInt),
                    archiveBreadcrumbs = engine.generateArchiveBreadcrumbs(currentYear = yearInt),
                )
            return HttpResponse.ok(viewModel)
        }

        @Get("/blog/archive/{year}/{month}")
        suspend fun archiveYearMonth(
            year: String,
            month: String,
            headers: HttpHeaders,
        ): HttpResponse<Any> {
            val yearInt = year.toIntOrNull() ?: return HttpResponse.badRequest("Invalid year")
            val monthInt = month.toIntOrNull() ?: return HttpResponse.badRequest("Invalid month")
            val fragments = engine.getByYearMonth(yearInt, monthInt)
            val isPartial = isHtmxRequest(headers)
            val viewModel =
                ArchiveViewModel(
                    type = "year-month",
                    year = year,
                    month = month,
                    fragments = fragments.map { FragmentViewModel(it, isPartial) },
                    siteTitle = engine.siteTitle,
                    navigationMenu = engine.nav(),
                    footer = engine.footer(),
                    archiveMonthLinks = engine.generateArchiveMonthLinks(year = yearInt, currentMonth = monthInt),
                    archiveBreadcrumbs = engine.generateArchiveBreadcrumbs(currentYear = yearInt, currentMonth = monthInt),
                )
            return HttpResponse.ok(viewModel)
        }

        @Get("/search")
        suspend fun search(
            @QueryValue("q") query: String,
            headers: HttpHeaders,
        ): HttpResponse<Any> {
            val results = engine.search(query)
            val isPartial = isHtmxRequest(headers)
            val viewModel =
                SearchViewModel(
                    query = query,
                    results = results.map { FragmentViewModel(it.fragment, isPartial) },
                    siteTitle = engine.siteTitle,
                    navigationMenu = engine.nav(),
                    footer = engine.footer(),
                    searchForm = engine.searchForm(),
                )
            return HttpResponse.ok(viewModel)
        }

        @Get("/api/autocomplete")
        @Produces(MediaType.APPLICATION_JSON)
        suspend fun autocomplete(
            @QueryValue q: String,
            @QueryValue(defaultValue = "10") limit: Int,
        ): HttpResponse<Any> = HttpResponse.ok(engine.autocomplete(q, limit))

        @Get("/sitemap.xml")
        @Produces("application/xml;charset=utf-8")
        suspend fun sitemap(): HttpResponse<String> {
            val sitemapXml = engine.generateSitemap()
            return HttpResponse
                .ok(sitemapXml)
                .header("Content-Type", "application/xml; charset=utf-8")
        }

        @Get("/robots.txt")
        @Produces("text/plain;charset=utf-8")
        fun robotsTxt(): HttpResponse<String> {
            val body = engine.generateRobotsTxt()
            return HttpResponse
                .ok(body)
                .header("Content-Type", "text/plain; charset=utf-8")
        }

        @Get("/llms.txt")
        @Produces("text/plain;charset=utf-8")
        suspend fun llmsTxt(): HttpResponse<String> {
            val body = engine.generateLlmsTxt()
            return HttpResponse
                .ok(body)
                .header("Content-Type", "text/plain; charset=utf-8")
        }
    }
