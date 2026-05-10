package io.github.rygel.fragments.quarkus

import io.github.rygel.fragments.AuthorViewModel
import io.github.rygel.fragments.FragmentViewModel
import io.github.rygel.fragments.adapter.FragmentsEngine
import jakarta.inject.Inject
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/")
class FragmentsQuarkusResource
    @Inject
    constructor(
        private val engine: FragmentsEngine,
    ) {
        @GET
        suspend fun home(
            @Context headers: HttpHeaders,
        ): Response {
            val fragments = engine.getHome()
            val isPartial = isHtmxRequest(headers)
            val viewModel =
                HomeViewModel(
                    fragments = fragments.map { FragmentViewModel(it, isPartial) },
                    isPartialRender = isPartial,
                )
            return Response.ok(viewModel).build()
        }

        @GET
        @Path("/page/{slug}")
        suspend fun page(
            @PathParam("slug") slug: String,
            @Context headers: HttpHeaders,
        ): Response {
            val fragment = engine.getPage(slug)
            val isPartial = isHtmxRequest(headers)
            return if (fragment != null) {
                val viewModel = FragmentViewModel(fragment, isPartial)
                Response.ok(viewModel).build()
            } else {
                Response.status(Response.Status.NOT_FOUND).entity("Page not found").build()
            }
        }

        @GET
        @Path("/blog")
        suspend fun blogOverview(
            @QueryParam("page") page: Int?,
            @Context headers: HttpHeaders,
        ): Response {
            val pageResult = engine.getBlogOverview(page ?: 1)
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
            return Response.ok(viewModel).build()
        }

        @GET
        @Path("/blog/page/{page}")
        suspend fun blogOverviewByPath(
            @PathParam("page") page: Int,
            @Context headers: HttpHeaders,
        ): Response = blogOverview(page, headers)

        @GET
        @Path("/blog/{year}/{month}/{slug}")
        suspend fun blogPost(
            @PathParam("year") year: String,
            @PathParam("month") month: String,
            @PathParam("slug") slug: String,
            @Context headers: HttpHeaders,
        ): Response {
            val fragment = engine.getBlogPost(year, month, slug)
            val isPartial = isHtmxRequest(headers)
            return if (fragment != null) {
                val viewModel = FragmentViewModel(fragment, isPartial)
                Response.ok(viewModel).build()
            } else {
                Response.status(Response.Status.NOT_FOUND).entity("Post not found").build()
            }
        }

        @GET
        @Path("/blog/tag/{tag}")
        suspend fun byTag(
            @PathParam("tag") tag: String,
            @QueryParam("page") page: Int?,
            @Context headers: HttpHeaders,
        ): Response {
            val pageResult = engine.getByTag(tag, page ?: 1)
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
            return Response.ok(viewModel).build()
        }

        @GET
        @Path("/blog/category/{category}")
        suspend fun byCategory(
            @PathParam("category") category: String,
            @QueryParam("page") page: Int?,
            @Context headers: HttpHeaders,
        ): Response {
            val pageResult = engine.getByCategory(category, page ?: 1)
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
            return Response.ok(viewModel).build()
        }

        @GET
        @Path("/blog/author/{slug}")
        suspend fun byAuthor(
            @PathParam("slug") slug: String,
            @QueryParam("page") page: Int?,
            @Context headers: HttpHeaders,
        ): Response {
            val pageResult = engine.getByAuthor(slug, page ?: 1)
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
            return Response.ok(viewModel).build()
        }

        private fun isHtmxRequest(headers: HttpHeaders): Boolean =
            engine.isHtmxRequest(headers.getHeaderString(FragmentViewModel.HTMX_REQUEST_HEADER))

        @GET
        @Path("/rss.xml")
        @Produces("application/rss+xml")
        suspend fun rss(): Response {
            val rssXml = engine.generateRssFeed()
            return Response
                .ok()
                .header("Content-Type", "application/rss+xml; charset=utf-8")
                .entity(rssXml)
                .build()
        }

        @GET
        @Path("/blog/archive/{year}")
        suspend fun archiveYear(
            @PathParam("year") year: String,
            @Context headers: HttpHeaders,
        ): Response {
            val yearInt = year.toIntOrNull() ?: return Response.status(Response.Status.BAD_REQUEST).entity("Invalid year").build()
            val fragments = engine.getByYear(yearInt)
            val isPartial = isHtmxRequest(headers)
            val viewModel =
                ArchiveViewModel(
                    type = "year",
                    year = year,
                    fragments = fragments.map { FragmentViewModel(it, isPartial) },
                    siteTitle = engine.siteTitle,
                )
            return Response.ok(viewModel).build()
        }

        @GET
        @Path("/blog/archive/{year}/{month}")
        suspend fun archiveYearMonth(
            @PathParam("year") year: String,
            @PathParam("month") month: String,
            @Context headers: HttpHeaders,
        ): Response {
            val yearInt = year.toIntOrNull() ?: return Response.status(Response.Status.BAD_REQUEST).entity("Invalid year").build()
            val monthInt = month.toIntOrNull() ?: return Response.status(Response.Status.BAD_REQUEST).entity("Invalid month").build()
            val fragments = engine.getByYearMonth(yearInt, monthInt)
            val isPartial = isHtmxRequest(headers)
            val viewModel =
                ArchiveViewModel(
                    type = "year-month",
                    year = year,
                    month = month,
                    fragments = fragments.map { FragmentViewModel(it, isPartial) },
                    siteTitle = engine.siteTitle,
                )
            return Response.ok(viewModel).build()
        }

        @GET
        @Path("/search")
        suspend fun search(
            @QueryParam("q") query: String?,
            @Context headers: HttpHeaders,
        ): Response {
            if (query.isNullOrBlank()) return Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'q' is required").build()
            val results = engine.search(query)
            val isPartial = isHtmxRequest(headers)
            val viewModel =
                SearchViewModel(
                    query = query,
                    results = results.map { FragmentViewModel(it.fragment, isPartial) },
                    siteTitle = engine.siteTitle,
                )
            return Response.ok(viewModel).build()
        }

        @GET
        @Path("/api/autocomplete")
        @Produces(MediaType.APPLICATION_JSON)
        suspend fun autocomplete(
            @QueryParam("q") query: String,
            @QueryParam("limit") @DefaultValue("10") limit: Int,
        ): Response = Response.ok(engine.autocomplete(query, limit)).build()

        @GET
        @Path("/sitemap.xml")
        @Produces("application/xml")
        suspend fun sitemap(): Response {
            val sitemapXml = engine.generateSitemap()
            return Response
                .ok()
                .header("Content-Type", "application/xml; charset=utf-8")
                .entity(sitemapXml)
                .build()
        }

        @GET
        @Path("/robots.txt")
        @Produces("text/plain")
        fun robotsTxt(): Response {
            val body = engine.generateRobotsTxt()
            return Response
                .ok()
                .header("Content-Type", "text/plain; charset=utf-8")
                .entity(body)
                .build()
        }

        @GET
        @Path("/llms.txt")
        @Produces("text/plain")
        suspend fun llmsTxt(): Response {
            val body = engine.generateLlmsTxt()
            return Response
                .ok()
                .header("Content-Type", "text/plain; charset=utf-8")
                .entity(body)
                .build()
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

        data class ArchiveViewModel(
            val type: String,
            val year: String,
            val month: String? = null,
            val fragments: List<FragmentViewModel>,
            val siteTitle: String,
        )

        data class SearchViewModel(
            val query: String,
            val results: List<FragmentViewModel>,
            val siteTitle: String,
        )
    }
