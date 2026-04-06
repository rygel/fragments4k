package io.github.rygel.fragments.micronaut

import io.github.rygel.fragments.AuthorViewModel
import io.github.rygel.fragments.FragmentViewModel
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
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
                val viewModel = FragmentViewModel(fragment, isPartial)
                HttpResponse.ok(viewModel)
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
                val viewModel = FragmentViewModel(fragment, isPartial)
                HttpResponse.ok(viewModel)
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
                    authorName = author?.author?.name,
                    author = author,
                    fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
                    currentPage = pageResult.currentPage,
                    totalPages = pageResult.totalPages,
                    hasNext = pageResult.hasNext,
                    hasPrevious = pageResult.hasPrevious,
                    isPartialRender = isPartial,
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

        data class HomeViewModel(
            val fragments: List<FragmentViewModel>,
            val isPartialRender: Boolean = false,
        )

        data class BlogOverviewViewModel(
            val fragments: List<FragmentViewModel>,
            val currentPage: Int,
            val totalPages: Int,
            val hasNext: Boolean = false,
            val hasPrevious: Boolean = false,
            val isPartialRender: Boolean = false,
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
        )

        data class CategoryViewModel(
            val category: String,
            val fragments: List<FragmentViewModel>,
            val currentPage: Int,
            val totalPages: Int,
            val hasNext: Boolean = false,
            val hasPrevious: Boolean = false,
            val isPartialRender: Boolean = false,
        )
    }
