package io.andromeda.fragments.spring

import io.andromeda.fragments.*
import io.andromeda.fragments.blog.BlogEngine
import io.andromeda.fragments.rss.RssGenerator
import io.andromeda.fragments.sitemap.SitemapGenerator
import io.andromeda.fragments.static.StaticPageEngine
import io.andromeda.fragments.FragmentRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.http.MediaType

@Controller
class FragmentsSpringController(
    private val repository: FragmentRepository,
    private val blogEngine: BlogEngine,
    private val siteTitle: String = "My Blog",
    private val siteDescription: String = "My Awesome Blog",
    private val siteUrl: String = "http://localhost:8080",
    private val feedUrl: String = "http://localhost:8080/rss.xml"
) {
    private val rssGenerator: RssGenerator by lazy { RssGenerator(repository) }
    private val sitemapGenerator: SitemapGenerator by lazy { SitemapGenerator(repository, siteUrl, lastModified = null) }

    @GetMapping("/")
    suspend fun home(
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model
    ): String {
        val fragments = repository.getAllVisible()
        val isPartial = isHtmxRequest(htmxRequest)
        model.addAttribute("viewModel", HomeViewModel(
            fragments = fragments.map { FragmentViewModel(it, isPartial) },
            isPartialRender = isPartial
        ))
        return if (isPartial) "index" else "index"
    }

    @GetMapping("/page/{slug}")
    suspend fun page(
        @PathVariable slug: String,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model
    ): String {
        val fragment = repository.getBySlug(slug)
        val isPartial = isHtmxRequest(htmxRequest)
        return if (fragment != null) {
            model.addAttribute("viewModel", FragmentViewModel(fragment, isPartial))
            fragment.template
        } else {
            "error/404"
        }
    }

    @GetMapping("/blog")
    suspend fun blogOverview(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model
    ): String {
        val pageResult = blogEngine.getOverview(page)
        val isPartial = isHtmxRequest(htmxRequest)
        model.addAttribute("viewModel", BlogOverviewViewModel(
            fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
            currentPage = pageResult.currentPage,
            totalPages = pageResult.totalPages,
            hasNext = pageResult.hasNext,
            hasPrevious = pageResult.hasPrevious,
            isPartialRender = isPartial
        ))
        return "blog_overview"
    }

    @GetMapping("/blog/page/{page}")
    suspend fun blogOverviewByPath(
        @PathVariable page: Int,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model
    ): String {
        return blogOverview(page, htmxRequest, model)
    }

    @GetMapping("/blog/{year}/{month}/{slug}")
    suspend fun blogPost(
        @PathVariable year: String,
        @PathVariable month: String,
        @PathVariable slug: String,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model
    ): String {
        val fragment = blogEngine.getPost(year, month, slug)
        val isPartial = isHtmxRequest(htmxRequest)
        return if (fragment != null) {
            model.addAttribute("viewModel", FragmentViewModel(fragment, isPartial))
            fragment.template
        } else {
            "error/404"
        }
    }

    @GetMapping("/blog/tag/{tag}")
    suspend fun byTag(
        @PathVariable tag: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model
    ): String {
        val pageResult = blogEngine.getByTag(tag, page)
        val isPartial = isHtmxRequest(htmxRequest)
        model.addAttribute("viewModel", TagViewModel(
            tag = tag,
            fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
            currentPage = pageResult.currentPage,
            totalPages = pageResult.totalPages,
            hasNext = pageResult.hasNext,
            hasPrevious = pageResult.hasPrevious,
            isPartialRender = isPartial
        ))
        return "blog_overview"
    }

    @GetMapping("/blog/category/{category}")
    suspend fun byCategory(
        @PathVariable category: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model
    ): String {
        val pageResult = blogEngine.getByCategory(category, page)
        val isPartial = isHtmxRequest(htmxRequest)
        model.addAttribute("viewModel", CategoryViewModel(
            category = category,
            fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
            currentPage = pageResult.currentPage,
            totalPages = pageResult.totalPages,
            hasNext = pageResult.hasNext,
            hasPrevious = pageResult.hasPrevious,
            isPartialRender = isPartial
        ))
        return "blog_overview"
    }

    @GetMapping(value = ["/rss.xml", "/feed.xml"], produces = [MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_XML_VALUE])
    suspend fun rss(): String {
        val rssXml = rssGenerator.generateFeed(
            siteTitle = siteTitle,
            siteDescription = siteDescription,
            siteUrl = siteUrl,
            feedUrl = feedUrl
        )
        return rssXml
    }

    @GetMapping(value = ["/sitemap.xml"], produces = [MediaType.APPLICATION_XML_VALUE])
    suspend fun sitemap(): String {
        return sitemapGenerator.generateSitemap()
    }

    private fun isHtmxRequest(header: String?): Boolean {
        return header?.lowercase() == "true"
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
