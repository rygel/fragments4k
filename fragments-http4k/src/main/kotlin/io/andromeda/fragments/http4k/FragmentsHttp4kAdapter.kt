package io.andromeda.fragments.http4k

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.rss.RssGenerator
import io.andromeda.fragments.sitemap.SitemapGenerator
import io.andromeda.fragments.static.StaticPageEngine
import kotlinx.coroutines.runBlocking
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.*

class FragmentsHttp4kAdapter(
    private val staticEngine: StaticPageEngine,
    private val blogEngine: BlogEngine,
    private val renderer: TemplateRenderer,
    private val rssGenerator: RssGenerator = RssGenerator(
        repository = staticEngine.getRepository()
    ),
    private val sitemapGenerator: SitemapGenerator = SitemapGenerator(
        repository = staticEngine.getRepository(),
        siteUrl = "http://localhost:8080",
        lastModified = null
    ),
    private val siteTitle: String = "My Blog",
    private val siteDescription: String = "My Awesome Blog",
    private val siteUrl: String = "http://localhost:8080",
    private val feedUrl: String = "$siteUrl/rss.xml"
) {

    fun createRoutes(): RoutingHttpHandler {
        return routes(
            "/" bind GET to { request -> handleHome(request) },
            "/page/{slug}" bind GET to { request -> handlePage(request) },
            "/blog" bind GET to { request -> handleBlogOverview(request) },
            "/blog/page/{page}" bind GET to { request -> handleBlogOverview(request) },
            "/blog/{year}/{month}/{slug}" bind GET to { request -> handleBlogPost(request) },
            "/blog/tag/{tag}" bind GET to { request -> handleByTag(request) },
            "/blog/category/{category}" bind GET to { request -> handleByCategory(request) },
            "/rss.xml" bind GET to { _ -> handleRss() },
            "/sitemap.xml" bind GET to { _ -> handleSitemap() }
        )
    }

    private fun handleHome(request: Request): Response {
        return runBlocking {
            val fragments = staticEngine.getAllStaticPages()
            val viewModel = BlogOverviewViewModel(
                fragments = fragments.map { FragmentViewModel(it) },
                currentPage = 1,
                totalPages = 1,
                isPartialRender = isHtmxRequest(request)
            )
            renderResponse(viewModel, "index")
        }
    }

    private fun handlePage(request: Request): Response {
        val slug = request.path("slug") ?: return Response(Status.NOT_FOUND)
        return runBlocking {
            val fragment = staticEngine.getPage(slug)
            if (fragment != null) {
                val viewModel = FragmentViewModel(fragment, isHtmxRequest(request))
                renderResponse(viewModel, fragment.template)
            } else {
                Response(Status.NOT_FOUND).body("Page not found: $slug")
            }
        }
    }

    private fun handleBlogOverview(request: Request): Response {
        val page = request.path("page")?.toIntOrNull() ?: 1
        return runBlocking {
            val pageResult = blogEngine.getOverview(page)
            val viewModel = BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(request)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(request)
            )
            renderResponse(viewModel, "blog_overview")
        }
    }

    private fun handleBlogPost(request: Request): Response {
        val year = request.path("year") ?: return Response(Status.NOT_FOUND)
        val month = request.path("month") ?: return Response(Status.NOT_FOUND)
        val slug = request.path("slug") ?: return Response(Status.NOT_FOUND)
        
        return runBlocking {
            val fragment = blogEngine.getPost(year, month, slug)
            if (fragment != null) {
                val viewModel = FragmentViewModel(fragment, isHtmxRequest(request))
                renderResponse(viewModel, fragment.template)
            } else {
                Response(Status.NOT_FOUND).body("Post not found")
            }
        }
    }

    private fun handleByTag(request: Request): Response {
        val tag = request.path("tag") ?: return Response(Status.NOT_FOUND)
        val page = request.query("page")?.toIntOrNull() ?: 1
        return runBlocking {
            val pageResult = blogEngine.getByTag(tag, page)
            val viewModel = BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(request)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(request)
            )
            renderResponse(viewModel, "blog_overview")
        }
    }

    private fun handleByCategory(request: Request): Response {
        val category = request.path("category") ?: return Response(Status.NOT_FOUND)
        return runBlocking {
            val pageResult = blogEngine.getByCategory(category, 1)
            val viewModel = CategoryViewModel(
                category = category,
                fragments = pageResult.items.map { FragmentViewModel(it) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(request)
            )
            renderResponse(viewModel, "blog_overview")
        }
    }

    private fun handleRss(): Response {
        return runBlocking {
            val rssXml = rssGenerator.generateFeed(
                siteTitle = siteTitle,
                siteDescription = siteDescription,
                siteUrl = siteUrl,
                feedUrl = feedUrl
            )
            Response(Status.OK)
                .header("Content-Type", "application/rss+xml; charset=utf-8")
                .body(rssXml)
        }
    }

    private fun handleSitemap(): Response {
        return runBlocking {
            val sitemapXml = sitemapGenerator.generateSitemap()
            Response(Status.OK)
                .header("Content-Type", "application/xml; charset=utf-8")
                .body(sitemapXml)
        }
    }

    private fun isHtmxRequest(request: Request): Boolean {
        return request.header(FragmentViewModel.HTMX_REQUEST_HEADER)?.lowercase() == "true"
    }

    private fun renderResponse(viewModel: Any, template: String): Response {
        val rendered = renderer(viewModel)
        return Response(Status.OK).body(rendered)
    }

    data class BlogOverviewViewModel(
        val fragments: List<FragmentViewModel>,
        val currentPage: Int,
        val totalPages: Int,
        val hasNext: Boolean = false,
        val hasPrevious: Boolean = false,
        val isPartialRender: Boolean = false
    )
}
