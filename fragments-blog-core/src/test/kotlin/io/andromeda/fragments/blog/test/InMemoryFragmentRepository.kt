package io.andromeda.fragments.blog.test

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository

class InMemoryFragmentRepository : FragmentRepository {
    private val fragments = mutableListOf<Fragment>()

    suspend fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
    }

    override suspend fun getAll(): List<Fragment> = fragments.toList()

    override suspend fun getAllVisible(): List<Fragment> =
        fragments.filter { it.visible }

    override suspend fun getBySlug(slug: String): Fragment? =
        fragments.find { it.slug == slug }

    override suspend fun getByYearMonthAndSlug(year: String, month: String, slug: String): Fragment? {
        return fragments.find {
            it.slug == slug &&
            it.date?.year == year.toIntOrNull() &&
            it.date?.monthValue == month.toIntOrNull()
        }
    }

    override suspend fun getByTag(tag: String): List<Fragment> =
        fragments.filter { it.tags.contains(tag) }

    override suspend fun getByCategory(category: String): List<Fragment> =
        fragments.filter { it.categories.contains(category) }

    override suspend fun reload() {
        fragments.clear()
    }
}
