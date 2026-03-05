package io.andromeda.fragments.quarkus

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentViewModel
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.rss.RssGenerator
import io.andromeda.fragments.sitemap.SitemapGenerator
import io.andromeda.fragments.static.StaticPageEngine
import io.quarkus.qute.TemplateInstance
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.Produces

@Path("/")
class FragmentsQuarkusResource @Inject constructor(
    val staticEngine: StaticPageEngine,
    val blogEngine: BlogEngine,
    private val siteTitle: String = "My Blog",
    private val siteDescription: String = "My Awesome Blog",
    private val siteUrl: String = "http://localhost:8080",
    private val feedUrl: String = "http://localhost:8080/rss.xml"
) {
    private val rssGenerator: RssGenerator by lazy { RssGenerator(repository = staticEngine.getRepository()) }
    private val sitemapGenerator: SitemapGenerator by lazy { SitemapGenerator(repository = staticEngine.getRepository(), siteUrl = siteUrl, lastModified = null) }

    @GET
    suspend fun home(@Context headers: HttpHeaders): Response {
        val fragments = staticEngine.getAllStaticPages()
        val isPartial = isHtmxRequest(headers)
        val viewModel = HomeViewModel(
            fragments = fragments.map { FragmentViewModel(it, isPartial) },
            isPartialRender = isPartial
        )
        return Response.ok(viewModel).build()
    }

    @GET
    @Path("/page/{slug}")
    suspend fun page(
        @PathParam("slug") slug: String,
        @Context headers: HttpHeaders
    ): Response {
        val fragment = staticEngine.getPage(slug)
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
        @Context headers: HttpHeaders
    ): Response {
        val pageResult = blogEngine.getOverview(includeDrafts = false, page = page ?: 1)
        val isPartial = isHtmxRequest(headers)
        val viewModel = BlogOverviewViewModel(
            fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
            currentPage = pageResult.currentPage,
            totalPages = pageResult.totalPages,
            hasNext = pageResult.hasNext,
            hasPrevious = pageResult.hasPrevious,
            isPartialRender = isPartial
        )
        return Response.ok(viewModel).build()
    }

    @GET
    @Path("/blog/page/{page}")
    suspend fun blogOverviewByPath(
        @PathParam("page") page: Int,
        @Context headers: HttpHeaders
    ): Response {
        return blogOverview(page, headers)
    }

    @GET
    @Path("/blog/{year}/{month}/{slug}")
    suspend fun blogPost(
        @PathParam("year") year: String,
        @PathParam("month") month: String,
        @PathParam("slug") slug: String,
        @Context headers: HttpHeaders
    ): Response {
        val fragment = blogEngine.getPost(year, month, slug)
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
        @Context headers: HttpHeaders
    ): Response {
        val pageResult = blogEngine.getByTag(tag, page ?: 1)
        val isPartial = isHtmxRequest(headers)
        val viewModel = TagViewModel(
            tag = tag,
            fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
            currentPage = pageResult.currentPage,
            totalPages = pageResult.totalPages,
            hasNext = pageResult.hasNext,
            hasPrevious = pageResult.hasPrevious,
            isPartialRender = isPartial
        )
        return Response.ok(viewModel).build()
    }

    @GET
    @Path("/blog/category/{category}")
    suspend fun byCategory(
        @PathParam("category") category: String,
        @QueryParam("page") page: Int?,
        @Context headers: HttpHeaders
    ): Response {
        val pageResult = blogEngine.getByCategory(category, page ?: 1)
        val isPartial = isHtmxRequest(headers)
        val viewModel = CategoryViewModel(
            category = category,
            fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
            currentPage = pageResult.currentPage,
            totalPages = pageResult.totalPages,
            hasNext = pageResult.hasNext,
            hasPrevious = pageResult.hasPrevious,
            isPartialRender = isPartial
        )
        return Response.ok(viewModel).build()
    }

    private fun isHtmxRequest(headers: HttpHeaders): Boolean {
        return headers.getHeaderString(FragmentViewModel.HTMX_REQUEST_HEADER)?.lowercase() == "true"
    }

    @GET
    @Path("/rss.xml")
    @Produces("application/rss+xml")
    suspend fun rss(): Response {
        val rssXml = rssGenerator.generateFeed(
            siteTitle = siteTitle,
            siteDescription = siteDescription,
            siteUrl = siteUrl,
            feedUrl = feedUrl
        )
        return Response.ok()
            .header("Content-Type", "application/rss+xml; charset=utf-8")
            .entity(rssXml)
            .build()
    }

    @GET
    @Path("/sitemap.xml")
    @Produces("application/xml")
    suspend fun sitemap(): Response {
        val sitemapXml = sitemapGenerator.generateSitemap()
        return Response.ok()
            .header("Content-Type", "application/xml; charset=utf-8")
            .entity(sitemapXml)
            .build()
    }

    data class HomeViewModel(
        val fragments: List<FragmentViewModel>,
        val isPartialRender: Boolean = false
    )

    data class BlogOverviewViewModel(
        val fragments: List<FragmentViewModel>,
        val currentPage: Int,
        val totalPages: Int,
        val hasNext: Boolean = false,
        val hasPrevious: Boolean = false,
        val isPartialRender: Boolean = false
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
        val isPartialRender: Boolean = false
    )
}
