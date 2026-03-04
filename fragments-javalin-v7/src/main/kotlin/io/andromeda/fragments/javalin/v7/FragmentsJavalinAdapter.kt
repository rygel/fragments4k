package io.andromeda.fragments.javalin.v7

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentViewModel
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.blog.PageResult
import io.andromeda.fragments.lucene.LuceneSearchEngine
import io.andromeda.fragments.rss.RssGenerator
import io.andromeda.fragments.sitemap.SitemapGenerator
import io.andromeda.fragments.static.StaticPageEngine
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import kotlinx.coroutines.runBlocking

/**
 * Configures Fragments routes for Javalin 7.
 * 
 * In Javalin 7, routes must be defined in the config.routes block during Javalin.create().
 * Usage:
 * ```kotlin
 * val app = Javalin.create { config ->
 *     config.fragmentsRoutes(
 *         staticEngine = staticEngine,
 *         blogEngine = blogEngine,
 *         searchEngine = searchEngine
 *     )
 * }
 * ```
 */
fun JavalinConfig.fragmentsRoutes(
    staticEngine: StaticPageEngine,
    blogEngine: BlogEngine,
    renderer: TemplateRenderer? = null,
    searchEngine: LuceneSearchEngine,
    siteTitle: String = "My Blog",
    siteDescription: String = "My Awesome Blog",
    siteUrl: String = "http://localhost:8080",
    feedUrl: String = "http://localhost:8080/rss.xml"
) {
    val rssGenerator = RssGenerator(repository = staticEngine.getRepository())
    val sitemapGenerator = SitemapGenerator(
        repository = staticEngine.getRepository(),
        siteUrl = siteUrl,
        lastModified = null
    )

    val isHtmxRequest: (Context) -> Boolean = { ctx ->
        ctx.header(FragmentViewModel.HTMX_REQUEST_HEADER)?.lowercase() == "true"
    }

    routes {
        get("/") { ctx ->
            runBlocking {
                val fragments = staticEngine.getAllStaticPages()
                renderFragmentList(ctx, renderer, fragments, isHtmxRequest(ctx))
            }
        }

        get("/page/{slug}") { ctx ->
            val slug = ctx.pathParam("slug")
            runBlocking {
                val fragment = staticEngine.getPage(slug)
                if (fragment != null) {
                    renderFragment(ctx, renderer, fragment, isHtmxRequest(ctx))
                } else {
                    ctx.status(404).result("Page not found: $slug")
                }
            }
        }

        get("/blog") { ctx ->
            runBlocking {
                val pageResult = blogEngine.getOverview(1)
                renderBlogOverview(ctx, renderer, pageResult, isHtmxRequest(ctx))
            }
        }

        get("/blog/page/{page}") { ctx ->
            val page = ctx.pathParam("page").toIntOrNull() ?: 1
            runBlocking {
                val pageResult = blogEngine.getOverview(page)
                renderBlogOverview(ctx, renderer, pageResult, isHtmxRequest(ctx))
            }
        }

        get("/blog/{year}/{month}/{slug}") { ctx ->
            val year = ctx.pathParam("year")
            val month = ctx.pathParam("month")
            val slug = ctx.pathParam("slug")
            runBlocking {
                val fragment = blogEngine.getPost(year, month, slug)
                if (fragment != null) {
                    renderFragment(ctx, renderer, fragment, isHtmxRequest(ctx))
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
                renderBlogOverview(ctx, renderer, pageResult, isHtmxRequest(ctx), tag = tag)
            }
        }

        get("/blog/category/{category}") { ctx ->
            val category = ctx.pathParam("category")
            val page = ctx.queryParam("page")?.toIntOrNull() ?: 1
            runBlocking {
                val pageResult = blogEngine.getByCategory(category, page)
                renderCategoryOverview(ctx, renderer, pageResult, category, isHtmxRequest(ctx))
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
                renderArchive(ctx, renderer, fragments, year, siteTitle)
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
                renderArchive(ctx, renderer, fragments, year, siteTitle, month)
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
                renderSearchResults(ctx, renderer, results.map { it.fragment }, query, siteTitle)
            }
        }
    }
}

private fun renderFragmentList(
    ctx: Context,
    renderer: TemplateRenderer?,
    fragments: List<Fragment>,
    isPartial: Boolean
) {
    val viewModel = HomeViewModel(
        fragments = fragments.map { FragmentViewModel(it) },
        isPartialRender = isPartial
    )
    render(ctx, renderer, "index", viewModel)
}

private fun renderFragment(
    ctx: Context,
    renderer: TemplateRenderer?,
    fragment: Fragment,
    isPartial: Boolean
) {
    val viewModel = ContentViewModel(
        viewModel = FragmentViewModel(fragment, isPartial),
        templateName = fragment.template
    )
    render(ctx, renderer, fragment.template, viewModel)
}

private fun renderBlogOverview(
    ctx: Context,
    renderer: TemplateRenderer?,
    pageResult: PageResult<Fragment>,
    isPartial: Boolean,
    tag: String? = null
) {
    val viewModel = BlogOverviewViewModel(
        fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
        currentPage = pageResult.currentPage,
        totalPages = pageResult.totalPages,
        hasNext = pageResult.hasNext,
        hasPrevious = pageResult.hasPrevious,
        tag = tag,
        isPartialRender = isPartial
    )
    render(ctx, renderer, "blog_overview", viewModel)
}

private fun renderCategoryOverview(
    ctx: Context,
    renderer: TemplateRenderer?,
    pageResult: PageResult<Fragment>,
    category: String,
    isPartial: Boolean
) {
    val viewModel = CategoryViewModel(
        category = category,
        fragments = pageResult.items.map { FragmentViewModel(it) },
        currentPage = pageResult.currentPage,
        totalPages = pageResult.totalPages,
        hasNext = pageResult.hasNext,
        hasPrevious = pageResult.hasPrevious,
        isPartialRender = isPartial
    )
    render(ctx, renderer, "blog_overview", viewModel)
}

private fun renderArchive(
    ctx: Context,
    renderer: TemplateRenderer?,
    fragments: List<Fragment>,
    year: String,
    siteTitle: String,
    month: String? = null
) {
    val viewModel = ArchiveViewModel(
        type = if (month != null) "year-month" else "year",
        year = year,
        month = month,
        fragments = fragments.map { FragmentViewModel(it) },
        siteTitle = siteTitle
    )
    render(ctx, renderer, "archive", viewModel)
}

private fun renderSearchResults(
    ctx: Context,
    renderer: TemplateRenderer?,
    fragments: List<Fragment>,
    query: String,
    siteTitle: String
) {
    val viewModel = SearchViewModel(
        query = query,
        results = fragments.map { FragmentViewModel(it) },
        siteTitle = siteTitle
    )
    render(ctx, renderer, "search", viewModel)
}

private fun render(ctx: Context, renderer: TemplateRenderer?, template: String, viewModel: Any) {
    val html = renderer?.render(template, viewModel) ?: ""
    ctx.html(html)
}

data class ContentViewModel(
    val viewModel: FragmentViewModel,
    val templateName: String
)

data class SearchViewModel(
    val query: String,
    val results: List<FragmentViewModel>,
    val siteTitle: String
)

data class ArchiveViewModel(
    val type: String,
    val year: String,
    val month: String? = null,
    val fragments: List<FragmentViewModel>,
    val siteTitle: String
)

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
    val tag: String? = null,
    val category: String? = null,
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

interface TemplateRenderer {
    fun render(template: String, viewModel: Any): String
}
