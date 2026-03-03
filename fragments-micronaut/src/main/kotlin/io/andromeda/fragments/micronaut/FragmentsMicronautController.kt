package io.andromeda.fragments.micronaut

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.rss.RssGenerator
import io.andromeda.fragments.sitemap.SitemapGenerator
import io.andromeda.fragments.static.StaticPageEngine
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import jakarta.inject.Inject

@Controller("/")
class FragmentsMicronautController @Inject constructor(
    private val staticEngine: StaticPageEngine,
    private val blogEngine: BlogEngine,
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
    private val feedUrl: String = "http://localhost:8080/rss.xml"
) {

    @Get("/")
    suspend fun home(headers: HttpHeaders): HttpResponse<Any> {
        val fragments = staticEngine.getAllStaticPages()
        val isPartial = isHtmxRequest(headers)
        val viewModel = HomeViewModel(
            fragments = fragments.map { FragmentViewModel(it, isPartial) },
            isPartialRender = isPartial
        )
        return HttpResponse.ok(viewModel)
    }

    @Get("/page/{slug}")
    suspend fun page(slug: String, headers: HttpHeaders): HttpResponse<Any> {
        val fragment = staticEngine.getPage(slug)
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
        headers: HttpHeaders
    ): HttpResponse<Any> {
        val pageResult = blogEngine.getOverview(page)
        val isPartial = isHtmxRequest(headers)
        val viewModel = BlogOverviewViewModel(
            fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
            currentPage = pageResult.currentPage,
            totalPages = pageResult.totalPages,
            hasNext = pageResult.hasNext,
            hasPrevious = pageResult.hasPrevious,
            isPartialRender = isPartial
        )
        return HttpResponse.ok(viewModel)
    }

    @Get("/blog/page/{page}")
    suspend fun blogOverviewByPath(page: Int, headers: HttpHeaders): HttpResponse<Any> {
        return blogOverview(page, headers)
    }

    @Get("/blog/{year}/{month}/{slug}")
    suspend fun blogPost(
        year: String,
        month: String,
        slug: String,
        headers: HttpHeaders
    ): HttpResponse<Any> {
        val fragment = blogEngine.getPost(year, month, slug)
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
        headers: HttpHeaders
    ): HttpResponse<Any> {
        val pageResult = blogEngine.getByTag(tag, page)
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
        return HttpResponse.ok(viewModel)
    }

    @Get("/blog/category/{category}")
    suspend fun byCategory(
        category: String,
        @QueryValue(defaultValue = "1") page: Int,
        headers: HttpHeaders
    ): HttpResponse<Any> {
        val pageResult = blogEngine.getByCategory(category, page)
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
        return HttpResponse.ok(viewModel)
    }

    private fun isHtmxRequest(headers: HttpHeaders): Boolean {
        return headers.get(FragmentViewModel.HTMX_REQUEST_HEADER)?.lowercase() == "true"
    }

    @Get(value = ["/rss.xml", "/feed.xml"], produces = [MediaType.APPLICATION_XML, "application/rss+xml"])
    suspend fun rss(): HttpResponse<String> {
        val rssXml = rssGenerator.generateFeed(
            siteTitle = siteTitle,
            siteDescription = siteDescription,
            siteUrl = siteUrl,
            feedUrl = feedUrl
        )
        return HttpResponse.ok()
            .header("Content-Type", "application/rss+xml; charset=utf-8")
            .body(rssXml)
    }

    @Get(value = ["/sitemap.xml"], produces = [MediaType.APPLICATION_XML])
    suspend fun sitemap(): HttpResponse<String> {
        val sitemapXml = sitemapGenerator.generateSitemap()
        return HttpResponse.ok()
            .header("Content-Type", "application/xml; charset=utf-8")
            .body(sitemapXml)
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
