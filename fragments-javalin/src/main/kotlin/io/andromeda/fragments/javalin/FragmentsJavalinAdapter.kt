package io.andromeda.fragments.javalin

import io.andromeda.fragments.FragmentViewModel
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.lucene.LuceneSearchEngine
import io.andromeda.fragments.rss.RssGenerator
import io.andromeda.fragments.sitemap.SitemapGenerator
import io.andromeda.fragments.static.StaticPageEngine
import io.javalin.Javalin
import io.javalin.http.Context
import kotlinx.coroutines.runBlocking

fun Javalin.fragmentsRoutes(
    staticEngine: StaticPageEngine,
    blogEngine: BlogEngine,
    renderer: TemplateRenderer?,
    searchEngine: LuceneSearchEngine,
    siteTitle: String = "My Blog",
    siteDescription: String = "My Awesome Blog",
    siteUrl: String = "http://localhost:8080",
    feedUrl: String = "http://localhost:8080/rss.xml"
) {
    val rssGenerator = RssGenerator(
        repository = staticEngine.getRepository()
    )
    val sitemapGenerator = SitemapGenerator(
        repository = staticEngine.getRepository(),
        siteUrl = siteUrl,
        lastModified = null
    )

    val render: (Context, String, Any) -> Unit = { ctx, template, viewModel ->
        val html = renderer?.render(template, viewModel) ?: ""
        ctx.html(html)
    }

    val isHtmxRequest: (Context) -> Boolean = { ctx ->
        ctx.header(FragmentViewModel.HTMX_REQUEST_HEADER)?.lowercase() == "true"
    }

    get("/") { ctx ->
        runBlocking {
            val fragments = staticEngine.getAllStaticPages()
            val viewModel = HomeViewModel(
                fragments = fragments.map { FragmentViewModel(it) },
                isPartialRender = isHtmxRequest(ctx)
            )
            render(ctx, "index", viewModel)
        }
    }

    get("/page/{slug}") { ctx ->
        val slug = ctx.pathParam("slug")
        runBlocking {
            val fragment = staticEngine.getPage(slug)
            if (fragment != null) {
                val viewModel = ContentViewModel(
                    viewModel = FragmentViewModel(fragment, isHtmxRequest(ctx)),
                    templateName = fragment.template
                )
                render(ctx, fragment.template, viewModel)
            } else {
                ctx.status(404).result("Page not found: $slug")
            }
        }
    }

    get("/blog") { ctx ->
        runBlocking {
            val pageResult = blogEngine.getOverview(1)
            val viewModel = BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(ctx)
            )
            render(ctx, "blog_overview", viewModel)
        }
    }

    get("/blog/page/{page}") { ctx ->
        val page = ctx.pathParam("page").toIntOrNull() ?: 1
        runBlocking {
            val pageResult = blogEngine.getOverview(page)
            val viewModel = BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(ctx)
            )
            render(ctx, "blog_overview", viewModel)
        }
    }

    get("/blog/{year}/{month}/{slug}") { ctx ->
        val year = ctx.pathParam("year")
        val month = ctx.pathParam("month")
        val slug = ctx.pathParam("slug")
        runBlocking {
            val fragment = blogEngine.getPost(year, month, slug)
            if (fragment != null) {
                val viewModel = ContentViewModel(
                    viewModel = FragmentViewModel(fragment, isHtmxRequest(ctx)),
                    templateName = fragment.template
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
        runBlocking {
            val pageResult = blogEngine.getByTag(tag, page)
            val viewModel = BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                tag = tag,
                isPartialRender = isHtmxRequest(ctx)
            )
            render(ctx, "blog_overview", viewModel)
        }
    }

    get("/blog/category/{category}") { ctx ->
        val category = ctx.pathParam("category")
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 1
        runBlocking {
            val pageResult = blogEngine.getByCategory(category, page)
            val viewModel = CategoryViewModel(
                category = category,
                fragments = pageResult.items.map { FragmentViewModel(it) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(ctx)
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
        runBlocking {
            val fragments = blogEngine.getByYear(yearInt)
            val viewModel = ArchiveViewModel(
                type = "year",
                year = year,
                fragments = fragments.map { FragmentViewModel(it) },
                siteTitle = siteTitle
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
        
        runBlocking {
            val fragments = blogEngine.getByYearMonth(yearInt, monthInt)
            val viewModel = ArchiveViewModel(
                type = "year-month",
                year = year,
                month = month,
                fragments = fragments.map { FragmentViewModel(it) },
                siteTitle = siteTitle
            )
            render(ctx, "archive", viewModel)
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
            val viewModel = SearchViewModel(
                query = query,
                results = results.map { FragmentViewModel(it.fragment) },
                siteTitle = siteTitle
            )
            render(ctx, "search", viewModel)
        }
    }

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

interface TemplateRenderer {
    fun render(template: String, viewModel: Any): String
}
