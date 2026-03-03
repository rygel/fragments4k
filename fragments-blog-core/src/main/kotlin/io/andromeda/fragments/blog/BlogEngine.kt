package io.andromeda.fragments.blog

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository

class BlogEngine(
    private val repository: FragmentRepository,
    private val pageSize: Int = 10
) {

    suspend fun getOverview(page: Int): Page<Fragment> {
        val blogPosts = repository.getAllVisible()
            .filter { it.template == "blog" || it.template.isEmpty() }
            .sortedByDescending { it.date }
        return Page.create(blogPosts, page, pageSize)
    }

    suspend fun getPost(year: String, month: String, slug: String): Fragment? {
        return repository.getByYearMonthAndSlug(year, month, slug)
    }

    suspend fun getByTag(tag: String, page: Int): Page<Fragment> {
        val taggedPosts = repository.getByTag(tag)
            .filter { it.template == "blog" || it.template.isEmpty() }
            .sortedByDescending { it.date }
        return Page.create(taggedPosts, page, pageSize)
    }

    suspend fun getByCategory(category: String, page: Int): Page<Fragment> {
        val categorizedPosts = repository.getByCategory(category)
            .filter { it.template == "blog" || it.template.isEmpty() }
            .sortedByDescending { it.date }
        return Page.create(categorizedPosts, page, pageSize)
    }

    suspend fun getAllTags(): Map<String, Int> {
        return repository.getAllVisible()
            .flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
    }

    suspend fun getAllCategories(): Map<String, Int> {
        return repository.getAllVisible()
            .flatMap { it.categories }
            .groupingBy { it }
            .eachCount()
    }
}
