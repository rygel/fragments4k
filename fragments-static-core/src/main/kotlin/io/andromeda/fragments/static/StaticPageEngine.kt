package io.andromeda.fragments.static

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository
import io.andromeda.fragments.FragmentStatus

class StaticPageEngine(private val repository: FragmentRepository) {

    fun getRepository(): FragmentRepository = repository

    suspend fun getPage(slug: String): Fragment? {
        return repository.getBySlug(slug)
    }

    suspend fun getAllStaticPages(includeDrafts: Boolean = false): List<Fragment> {
        val allFragments = if (includeDrafts) {
            repository.getAll()
        } else {
            repository.getAllVisible()
        }
        return allFragments
            .filter { it.template == "static" || it.template.isEmpty() || it.template == "default" }
    }
}
