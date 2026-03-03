package io.andromeda.fragments.javalin

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.rss.RssGenerator
import io.andromeda.fragments.sitemap.SitemapGenerator
import io.andromeda.fragments.static.StaticPageEngine
import io.javalin.Javalin
import io.javalin.rendering.template.JavalinPebble
import io.javalin.rendering.template.ModelAndView
import org.jetbrains.kotlinx.coroutines.runBlocking
import java.nio.file.Paths

fun Javalin.fragmentsRoutes(
    staticEngine: StaticPageEngine,
    blogEngine: BlogEngine,
    renderer: TemplateRenderer,
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
                val viewModel = FragmentViewModel(fragment, isHtmxRequest(ctx))
                render(ctx, fragment.template, viewModel)
            } else {
                ctx.status(404).result("Page not found")
            }
        }
    }

    get("/blog") { ctx ->
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 1
        runBlocking {
            val pageResult = blogEngine.getOverview(page)
            val viewModel = BlogOverviewViewModel(
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

    get("/blog/page/{page}") { ctx ->
        val page = ctx.pathParam("page").toIntOrNull() ?: 1
        runBlocking {
            val pageResult = blogEngine.getOverview(page)
            val viewModel = BlogOverviewViewModel(
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

    get("/blog/{year}/{month}/{slug}") { ctx ->
        val year = ctx.pathParam("year")
        val month = ctx.pathParam("month")
        val slug = ctx.pathParam("slug")
        runBlocking {
            val fragment = blogEngine.getPost(year, month, slug)
            if (fragment != null) {
                val viewModel = FragmentViewModel(fragment, isHtmxRequest(ctx))
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
            val viewModel = TagViewModel(
                tag = tag,
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

    private fun render(ctx: JavalinContext, template: String, viewModel: Any) {
        val html = renderer.render(template, viewModel)
        ctx.html(html)
    }

    private fun isHtmxRequest(ctx: JavalinContext): Boolean {
        return ctx.header("HX-Request")?.lowercase() == "true"
    }
}
    }

    get("/page/{slug}") { ctx ->
        val slug = ctx.pathParam("slug")
        runBlocking {
            val fragment = staticEngine.getPage(slug)
            if (fragment != null) {
                val viewModel = FragmentViewModel(fragment, isHtmxRequest(ctx))
                render(ctx, fragment.template, viewModel)
            } else {
                ctx.status(404).result("Page not found")
            }
        }
    }

    get("/blog") { ctx ->
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 1
        runBlocking {
            val pageResult = blogEngine.getOverview(page)
            val viewModel = BlogOverviewViewModel(
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

    get("/blog/page/{page}") { ctx ->
        val page = ctx.pathParam("page").toIntOrNull() ?: 1
        runBlocking {
            val pageResult = blogEngine.getOverview(page)
            val viewModel = BlogOverviewViewModel(
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

    get("/blog/{year}/{month}/{slug}") { ctx ->
        val year = ctx.pathParam("year")
        val month = ctx.pathParam("month")
        val slug = ctx.pathParam("slug")
        runBlocking {
            val fragment = blogEngine.getPost(year, month, slug)
            if (fragment != null) {
                val viewModel = FragmentViewModel(fragment, isHtmxRequest(ctx))
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
            val viewModel = TagViewModel(
                tag = tag,
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

    get("/rss.xml") { ctx ->
        runBlocking {
            val rssXml = rssGenerator.generateFeed()
            ctx.contentType("application/rss+xml")
            ctx.result(rssXml)
        }
    }

    private fun render(ctx: JavalinContext, template: String, viewModel: Any) {
        val html = renderer.render(template, viewModel)
        ctx.html(html)
    }

    private fun isHtmxRequest(ctx: JavalinContext): Boolean {
        return ctx.header("HX-Request")?.lowercase() == "true"
    }
}

class FragmentsController(
    private val staticEngine: StaticPageEngine,
    private val blogEngine: BlogEngine,
    private val renderer: TemplateRenderer
) {

    fun home(ctx: Context) {
        ctx.future {
            val fragments = staticEngine.getAllStaticPages()
            val viewModel = HomeViewModel(
                fragments = fragments.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                isPartialRender = isHtmxRequest(ctx)
            )
            ctx.result(renderer.render("index", viewModel))
        }
    }

    fun page(ctx: Context) {
        val slug = ctx.pathParam("slug")
        ctx.future {
            val fragment = staticEngine.getPage(slug)
            if (fragment != null) {
                val viewModel = FragmentViewModel(fragment, isHtmxRequest(ctx))
                ctx.result(renderer.render(fragment.template, viewModel))
            } else {
                ctx.status(404).result("Page not found")
            }
        }
    }

    fun blogOverview(ctx: Context) {
        val page = ctx.pathParamMap()["page"]?.toIntOrNull() ?: 1
        ctx.future {
            val pageResult = blogEngine.getOverview(page)
            val viewModel = BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(ctx)
            )
            ctx.result(renderer.render("blog_overview", viewModel))
        }
    }

    fun blogPost(ctx: Context) {
        val year = ctx.pathParam("year")
        val month = ctx.pathParam("month")
        val slug = ctx.pathParam("slug")
        ctx.future {
            val fragment = blogEngine.getPost(year, month, slug)
            if (fragment != null) {
                val viewModel = FragmentViewModel(fragment, isHtmxRequest(ctx))
                ctx.result(renderer.render(fragment.template, viewModel))
            } else {
                ctx.status(404).result("Post not found")
            }
        }
    }

    fun byTag(ctx: Context) {
        val tag = ctx.pathParam("tag")
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 1
        ctx.future {
            val pageResult = blogEngine.getByTag(tag, page)
            val viewModel = TagViewModel(
                tag = tag,
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(ctx)
            )
            ctx.result(renderer.render("blog_overview", viewModel))
        }
    }

    fun byCategory(ctx: Context) {
        val category = ctx.pathParam("category")
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 1
        ctx.future {
            val pageResult = blogEngine.getByCategory(category, page)
            val viewModel = CategoryViewModel(
                category = category,
                fragments = pageResult.items.map { FragmentViewModel(it, isHtmxRequest(ctx)) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isHtmxRequest(ctx)
            )
            ctx.result(renderer.render("blog_overview", viewModel))
        }
    }

    private fun isHtmxRequest(ctx: Context): Boolean {
        return ctx.header(FragmentViewModel.HTMX_REQUEST_HEADER)?.lowercase() == "true"
    }

    private fun Context.future(block: suspend () -> Unit) {
        this.future(kotlinx.coroutines.Dispatchers.IO) {
            block()
        }
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

interface TemplateRenderer {
    fun render(template: String, viewModel: Any): String
}
