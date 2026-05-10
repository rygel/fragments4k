package io.github.rygel.fragments.adapter

import io.github.rygel.fragments.ArchiveNavigationLink
import io.github.rygel.fragments.AuthorViewModel
import io.github.rygel.fragments.FragmentViewModel
import io.github.rygel.fragments.NavigationLink

data class HomeViewModel(
    val fragments: List<FragmentViewModel>,
    val isPartialRender: Boolean = false,
    val navigationMenu: List<NavigationLink> = emptyList(),
    val footer: FooterConfig? = null,
)

data class BlogOverviewViewModel(
    val fragments: List<FragmentViewModel>,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isPartialRender: Boolean = false,
    val tag: String? = null,
    val category: String? = null,
    val navigationMenu: List<NavigationLink> = emptyList(),
    val pagination: PaginationInfo = PaginationInfo(1, 1, false, false, ""),
    val footer: FooterConfig? = null,
)

data class CategoryViewModel(
    val category: String,
    val fragments: List<FragmentViewModel>,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isPartialRender: Boolean = false,
    val navigationMenu: List<NavigationLink> = emptyList(),
    val pagination: PaginationInfo = PaginationInfo(1, 1, false, false, ""),
    val footer: FooterConfig? = null,
)

data class TagViewModel(
    val tag: String,
    val fragments: List<FragmentViewModel>,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isPartialRender: Boolean = false,
    val navigationMenu: List<NavigationLink> = emptyList(),
    val pagination: PaginationInfo = PaginationInfo(1, 1, false, false, ""),
    val footer: FooterConfig? = null,
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
    val navigationMenu: List<NavigationLink> = emptyList(),
    val pagination: PaginationInfo = PaginationInfo(1, 1, false, false, ""),
    val footer: FooterConfig? = null,
)

data class ArchiveViewModel(
    val type: String,
    val year: String,
    val month: String? = null,
    val fragments: List<FragmentViewModel>,
    val siteTitle: String,
    val navigationMenu: List<NavigationLink> = emptyList(),
    val footer: FooterConfig? = null,
    val archiveYearLinks: List<ArchiveNavigationLink>? = null,
    val archiveMonthLinks: List<ArchiveNavigationLink>? = null,
    val archiveBreadcrumbs: List<ArchiveNavigationLink>? = null,
)

data class SearchViewModel(
    val query: String,
    val results: List<FragmentViewModel>,
    val siteTitle: String,
    val navigationMenu: List<NavigationLink> = emptyList(),
    val footer: FooterConfig? = null,
    val searchForm: SearchFormConfig? = null,
)
