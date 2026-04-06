package io.github.rygel.fragments.static

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository

class StaticPageEngine(
    private val repository: FragmentRepository,
    private val pageUrlPrefix: String = "/page",
) {
    fun getRepository(): FragmentRepository = repository

    suspend fun getPage(slug: String): Fragment? = repository.getBySlug(slug)?.let { resolveUrl(it) }

    suspend fun getAllStaticPages(includeDrafts: Boolean = false): List<Fragment> {
        val allFragments =
            if (includeDrafts) {
                repository.getAll()
            } else {
                repository.getAllVisible()
            }
        return allFragments
            .filter { it.template == "static" || it.template.isEmpty() || it.template == "default" }
            .map { resolveUrl(it) }
    }

    private fun resolveUrl(fragment: Fragment): Fragment {
        if (fragment.resolvedUrl != null) return fragment
        return fragment.copy(resolvedUrl = "$pageUrlPrefix/${fragment.slug}")
    }
}
