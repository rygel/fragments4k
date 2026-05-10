package io.github.rygel.fragments.spring

import io.github.rygel.fragments.AuthorViewModel
import io.github.rygel.fragments.FragmentViewModel
import io.github.rygel.fragments.adapter.FragmentsEngine
import io.github.rygel.fragments.lucene.SearchSuggestion
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class FragmentsSpringController(
    private val engine: FragmentsEngine,
) {
    @GetMapping("/")
    suspend fun home(
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model,
    ): String {
        val fragments = engine.getHome()
        val isPartial = engine.isHtmxRequest(htmxRequest)
        model.addAttribute(
            "viewModel",
            HomeViewModel(
                fragments = fragments.map { FragmentViewModel(it, isPartial) },
                isPartialRender = isPartial,
            ),
        )
        return if (isPartial) "index" else "index"
    }

    @GetMapping("/page/{slug}")
    suspend fun page(
        @PathVariable slug: String,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model,
    ): String {
        val fragment = engine.getPage(slug)
        val isPartial = engine.isHtmxRequest(htmxRequest)
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
        model: Model,
    ): String {
        val pageResult = engine.getBlogOverview(page)
        val isPartial = engine.isHtmxRequest(htmxRequest)
        model.addAttribute(
            "viewModel",
            BlogOverviewViewModel(
                fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isPartial,
            ),
        )
        return "blog_overview"
    }

    @GetMapping("/blog/page/{page}")
    suspend fun blogOverviewByPath(
        @PathVariable page: Int,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model,
    ): String = blogOverview(page, htmxRequest, model)

    @GetMapping("/blog/{year}/{month}/{slug}")
    suspend fun blogPost(
        @PathVariable year: String,
        @PathVariable month: String,
        @PathVariable slug: String,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model,
    ): String {
        val fragment = engine.getBlogPost(year, month, slug)
        val isPartial = engine.isHtmxRequest(htmxRequest)
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
        model: Model,
    ): String {
        val pageResult = engine.getByTag(tag, page)
        val isPartial = engine.isHtmxRequest(htmxRequest)
        model.addAttribute(
            "viewModel",
            TagViewModel(
                tag = tag,
                fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isPartial,
            ),
        )
        return "blog_overview"
    }

    @GetMapping("/blog/category/{category}")
    suspend fun byCategory(
        @PathVariable category: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model,
    ): String {
        val pageResult = engine.getByCategory(category, page)
        val isPartial = engine.isHtmxRequest(htmxRequest)
        model.addAttribute(
            "viewModel",
            CategoryViewModel(
                category = category,
                fragments = pageResult.items.map { FragmentViewModel(it, isPartial) },
                currentPage = pageResult.currentPage,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext,
                hasPrevious = pageResult.hasPrevious,
                isPartialRender = isPartial,
            ),
        )
        return "blog_overview"
    }

    @GetMapping("/blog/author/{slug}")
    suspend fun byAuthor(
        @PathVariable slug: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model,
    ): String {
        val pageResult = engine.getByAuthor(slug, page)
        val isPartial = engine.isHtmxRequest(htmxRequest)
        val author = engine.getAuthor(slug)
        model.addAttribute(
            "viewModel",
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
            ),
        )
        return "blog_overview"
    }

    @GetMapping(value = ["/rss.xml", "/feed.xml"], produces = [MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_XML_VALUE])
    suspend fun rss(): String = engine.generateRssFeed()

    @GetMapping("/blog/archive/{year}")
    suspend fun archiveYear(
        @PathVariable year: String,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model,
    ): String {
        val yearInt = year.toIntOrNull() ?: return "error/404"
        val fragments = engine.getByYear(yearInt)
        val isPartial = engine.isHtmxRequest(htmxRequest)
        model.addAttribute(
            "viewModel",
            ArchiveViewModel(
                type = "year",
                year = year,
                fragments = fragments.map { FragmentViewModel(it, isPartial) },
                siteTitle = engine.siteTitle,
            ),
        )
        return "archive"
    }

    @GetMapping("/blog/archive/{year}/{month}")
    suspend fun archiveYearMonth(
        @PathVariable year: String,
        @PathVariable month: String,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model,
    ): String {
        val yearInt = year.toIntOrNull() ?: return "error/404"
        val monthInt = month.toIntOrNull() ?: return "error/404"
        val fragments = engine.getByYearMonth(yearInt, monthInt)
        val isPartial = engine.isHtmxRequest(htmxRequest)
        model.addAttribute(
            "viewModel",
            ArchiveViewModel(
                type = "year-month",
                year = year,
                month = month,
                fragments = fragments.map { FragmentViewModel(it, isPartial) },
                siteTitle = engine.siteTitle,
            ),
        )
        return "archive"
    }

    @GetMapping("/search")
    suspend fun search(
        @RequestParam("q") query: String,
        @RequestHeader(value = FragmentViewModel.HTMX_REQUEST_HEADER, required = false) htmxRequest: String?,
        model: Model,
    ): String {
        val results = engine.search(query)
        val isPartial = engine.isHtmxRequest(htmxRequest)
        model.addAttribute(
            "viewModel",
            SearchViewModel(
                query = query,
                results = results.map { FragmentViewModel(it.fragment, isPartial) },
                siteTitle = engine.siteTitle,
            ),
        )
        return "search"
    }

    @GetMapping("/api/autocomplete")
    @ResponseBody
    suspend fun autocomplete(
        @RequestParam("q") query: String,
        @RequestParam("limit", defaultValue = "10") limit: Int,
    ): List<SearchSuggestion> = engine.autocomplete(query, limit)

    @GetMapping(value = ["/sitemap.xml"], produces = [MediaType.APPLICATION_XML_VALUE])
    suspend fun sitemap(): String = engine.generateSitemap()

    @GetMapping(value = ["/robots.txt"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun robotsTxt(): String = engine.generateRobotsTxt()

    @GetMapping(value = ["/llms.txt"], produces = [MediaType.TEXT_PLAIN_VALUE])
    suspend fun llmsTxt(): String = engine.generateLlmsTxt()

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
