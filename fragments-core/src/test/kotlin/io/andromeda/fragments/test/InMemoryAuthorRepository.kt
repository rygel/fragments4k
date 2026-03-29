package io.github.rygel.fragments.test

import io.github.rygel.fragments.Author
import io.github.rygel.fragments.AuthorRepository

class InMemoryAuthorRepository : AuthorRepository {
    private val authors = mutableListOf<Author>()

    suspend fun addAuthor(author: Author) {
        authors.add(author)
    }

    override suspend fun getAll(): List<Author> = authors.toList()

    override suspend fun getById(id: String): Author? =
        authors.find { it.id == id }

    override suspend fun getByName(name: String): Author? =
        authors.find { it.name == name }

    override suspend fun getBySlug(slug: String): Author? =
        authors.find { it.slug == slug }

    override suspend fun getBySlugOrId(identifier: String): Author? =
        authors.find { it.id == identifier || it.slug == identifier }

    override suspend fun register(author: Author) {
        val existingIndex = authors.indexOfFirst { it.id == author.id }
        if (existingIndex >= 0) {
            authors[existingIndex] = author
        } else {
            authors.add(author)
        }
    }

    override suspend fun remove(id: String): Boolean {
        return authors.removeIf { it.id == id }
    }

    override suspend fun clear() {
        authors.clear()
    }

    override suspend fun count(): Int = authors.size
}
