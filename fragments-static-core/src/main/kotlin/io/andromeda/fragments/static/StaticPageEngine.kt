package io.andromeda.fragments.static

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository

class StaticPageEngine(private val repository: FragmentRepository) {

    suspend fun getPage(slug: String): Fragment? {
        return repository.getBySlug(slug)
    }

    suspend fun getAllStaticPages(): List<Fragment> {
        return repository.getAllVisible()
            .filter { it.template == "static" || it.template.isEmpty() || it.template == "default" }
    }
}
