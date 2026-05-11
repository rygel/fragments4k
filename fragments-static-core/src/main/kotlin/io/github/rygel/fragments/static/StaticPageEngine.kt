package io.github.rygel.fragments.static

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentTemplates
import org.slf4j.LoggerFactory

class StaticPageEngine(
    private val repository: FragmentRepository,
    private val pageUrlPrefix: String = "/page",
) {
    private val logger = LoggerFactory.getLogger(StaticPageEngine::class.java)

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
            .filter { it.template == FragmentTemplates.STATIC || it.template.isEmpty() || it.template == "default" }
            .map { resolveUrl(it) }
    }

    private fun resolveUrl(fragment: Fragment): Fragment {
        if (fragment.resolvedUrl != null) return fragment
        logger.warn(
            "Fragment '{}' has no resolvedUrl — falling back to slug-based URL. " +
                "Configure urlBuilder on the repository.",
            fragment.slug,
        )
        return fragment.copy(resolvedUrl = "$pageUrlPrefix/${fragment.slug}")
    }
}
