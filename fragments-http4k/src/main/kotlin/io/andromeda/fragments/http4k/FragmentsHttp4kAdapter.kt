package io.andromeda.fragments.http4k

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.lucene.LuceneSearchEngine
import io.andromeda.fragments.rss.RssGenerator
import io.andromeda.fragments.sitemap.SitemapGenerator
import io.andromeda.fragments.static.StaticPageEngine
import kotlinx.coroutines.runBlocking
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.template.*
import java.nio.file.Path

class FragmentsHttp4kAdapter(
    private val staticEngine: StaticPageEngine,
    private val blogEngine: BlogEngine,
    private val renderer: TemplateRenderer,
    private val searchEngine: LuceneSearchEngine,
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
            "/blog/archive/{year}" bind GET to { request -> handleArchiveYear(request) },
            "/blog/archive/{year}/{month}" bind GET to { request -> handleArchiveYearMonth(request) },
            "/search" bind GET to { request -> handleSearch(request) },
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
                templateName = "index",
                isPartialRender = isHtmxRequest(request)
            )
            renderResponse(viewModel)
        }
    }

    private fun handlePage(request: Request): Response {
        val slug = request.path("slug") ?: return Response(Status.NOT_FOUND)
        return runBlocking {
            val fragment = staticEngine.getPage(slug)
            if (fragment != null) {
                val viewModel = ContentViewModel(
                    viewModel = FragmentViewModel(fragment, isHtmxRequest(request)),
                    templateName = fragment.template
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
            val pageResult = blogEngine.getOverview(page)
            val viewModel = BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(request)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                templateName = "blog_overview",
                isPartialRender = isHtmxRequest(request)
            )
            renderResponse(viewModel)
        }
    }

    private fun handleBlogPost(request: Request): Response {
        val year = request.path("year") ?: return Response(Status.NOT_FOUND)
        val month = request.path("month") ?: return Response(Status.NOT_FOUND)
        val slug = request.path("slug") ?: return Response(Status.NOT_FOUND)
        
        return runBlocking {
            val fragment = blogEngine.getPost(year, month, slug)
            if (fragment != null) {
                val viewModel = ContentViewModel(
                    viewModel = FragmentViewModel(fragment, isHtmxRequest(request)),
                    templateName = fragment.template
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
            val pageResult = blogEngine.getByTag(tag, page)
            val viewModel = BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(request)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                templateName = "blog_overview",
                isPartialRender = isHtmxRequest(request)
            )
            renderResponse(viewModel)
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
                templateName = "blog_overview",
                isPartialRender = isHtmxRequest(request)
            )
            renderResponse(viewModel)
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

    private fun handleSearch(request: Request): Response {
        val query = request.query("q") ?: return Response(Status.BAD_REQUEST).body("Query parameter 'q' is required")
        return runBlocking {
            val results = searchEngine.search(query, maxResults = 50)
            val viewModel = SearchViewModel(
                query = query,
                results = results.map { FragmentViewModel(it.fragment) },
                siteTitle = siteTitle
            )
            renderResponse(viewModel)
        }
    }

    private fun handleArchiveYear(request: Request): Response {
        val year = request.path("year") ?: return Response(Status.NOT_FOUND)
        return runBlocking {
            val yearInt = year.toIntOrNull() ?: return@runBlocking Response(Status.BAD_REQUEST).body("Invalid year")
            val fragments = blogEngine.getByYear(yearInt)
            val viewModel = ArchiveViewModel(
                type = "year",
                year = year,
                fragments = fragments.map { FragmentViewModel(it) },
                siteTitle = siteTitle
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
            val fragments = blogEngine.getByYearMonth(yearInt, monthInt)
            val viewModel = ArchiveViewModel(
                type = "year-month",
                year = year,
                month = month,
                fragments = fragments.map { FragmentViewModel(it) },
                siteTitle = siteTitle
            )
            renderResponse(viewModel)
        }
    }

    private fun isHtmxRequest(request: Request): Boolean {
        return request.header(FragmentViewModel.HTMX_REQUEST_HEADER)?.lowercase() == "true"
    }

    private fun renderResponse(viewModel: ViewModel): Response {
        val rendered = renderer(viewModel)
        return Response(Status.OK).body(rendered)
    }

    data class ContentViewModel(
        val viewModel: FragmentViewModel,
        private val templateName: String
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
        val isPartialRender: Boolean = false
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
        val isPartialRender: Boolean = false
    ) : ViewModel {
        override fun template(): String = templateName
    }

    data class SearchViewModel(
        val query: String,
        val results: List<FragmentViewModel>,
        val siteTitle: String,
        private val templateName: String = "search"
    ) : ViewModel {
        override fun template(): String = templateName
    }

    data class ArchiveViewModel(
        val type: String,
        val year: String,
        val month: String? = null,
        val fragments: List<FragmentViewModel>,
        val siteTitle: String,
        private val templateName: String = "archive"
    ) : ViewModel {
        override fun template(): String = templateName
    }
}
