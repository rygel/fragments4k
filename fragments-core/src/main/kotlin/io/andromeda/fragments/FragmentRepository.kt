package io.andromeda.fragments

interface FragmentRepository {
    suspend fun getAll(): List<Fragment>
    suspend fun getAllVisible(): List<Fragment>
    suspend fun getBySlug(slug: String): Fragment?
    suspend fun getByYearMonthAndSlug(year: String, month: String, slug: String): Fragment?
    suspend fun getByTag(tag: String): List<Fragment>
    suspend fun getByCategory(category: String): List<Fragment>
    suspend fun reload()
}
