package io.andromeda.fragments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FileSystemAuthorRepository(
    private val basePath: Path,
    private val extension: String = ".author.yml"
) : AuthorRepository {

    private val logger = LoggerFactory.getLogger(FileSystemAuthorRepository::class.java)
    private val yaml = Yaml()
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private var cachedAuthors: List<Author> = emptyList()
    private var lastLoaded: LocalDateTime = LocalDateTime.MIN

    override suspend fun getAll(): List<Author> = withContext(Dispatchers.IO) {
        loadAuthors()
    }

    override suspend fun getById(id: String): Author? = withContext(Dispatchers.IO) {
        loadAuthors().find { it.id == id }
    }

    override suspend fun getByName(name: String): Author? = withContext(Dispatchers.IO) {
        loadAuthors().find { it.name == name }
    }

    override suspend fun getBySlug(slug: String): Author? = withContext(Dispatchers.IO) {
        loadAuthors().find { it.slug == slug }
    }

    override suspend fun getBySlugOrId(identifier: String): Author? = withContext(Dispatchers.IO) {
        loadAuthors().find { it.id == identifier || it.slug == identifier }
    }

    override suspend fun register(author: Author) = withContext(Dispatchers.IO) {
        val file = getAuthorFile(author.slug)
        if (file != null && file.exists()) {
            val existing = parseAuthorFile(file)
            if (existing != null) {
                val index = cachedAuthors.indexOfFirst { it.id == existing!!.id }
                if (index >= 0) {
                    cachedAuthors = cachedAuthors.toMutableList().apply { this[index] = author }
                }
                saveAuthor(author)
                cacheUpdatedAuthor(author)
                logger.info("Updated author: ${author.slug}")
                return@withContext
            }
        }

        saveAuthor(author)
        cacheUpdatedAuthor(author)
        logger.info("Registered author: ${author.slug}")
    }

    override suspend fun remove(id: String): Boolean = withContext(Dispatchers.IO) {
        val author = getById(id) ?: return@withContext false

        val file = getAuthorFile(author.slug)
        if (file == null || !file.exists()) {
            logger.warn("Author file not found: ${author.slug}")
            return@withContext false
        }

        try {
            Files.delete(file.toPath())
            invalidateCache()
            logger.info("Deleted author: ${author.slug}")
            true
        } catch (e: Exception) {
            logger.error("Failed to delete author: ${author.slug}", e)
            false
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        val authorsDir = File(basePath.toFile(), "authors")
        if (authorsDir.exists()) {
            authorsDir.listFiles()?.forEach { file ->
                try {
                    Files.delete(file.toPath())
                } catch (e: Exception) {
                    logger.error("Failed to delete author file: ${file.name}", e)
                }
            }
        }

        invalidateCache()
        logger.info("Cleared all authors")
    }

    override suspend fun count(): Int = withContext(Dispatchers.IO) {
        loadAuthors().size
    }

    private fun loadAuthors(): List<Author> {
        if (cachedAuthors.isEmpty() || lastLoaded == LocalDateTime.MIN) {
            val authorsDir = File(basePath.toFile(), "authors")
            if (!authorsDir.exists()) {
                authorsDir.mkdirs()
                cachedAuthors = emptyList()
                lastLoaded = LocalDateTime.now()
                return cachedAuthors
            }

            val files = authorsDir.listFiles { file ->
                file.extension == extension.removePrefix(".").removePrefix("yml") ||
                file.extension == extension.removePrefix(".").removePrefix("yaml")
            } ?: emptyArray()

            cachedAuthors = files.mapNotNull { parseAuthorFile(it) }
                .sortedBy { it.name.lowercase() }
            lastLoaded = LocalDateTime.now()
        }

        return cachedAuthors
    }

    private fun parseAuthorFile(file: File): Author? {
        return try {
            val content = file.readText()
            val data = yaml.load<Map<String, Any>>(content)

            Author(
                id = data["id"] as? String ?: file.nameWithoutExtension,
                name = data["name"] as? String ?: "",
                slug = data["slug"] as? String ?: file.nameWithoutExtension,
                email = data["email"] as? String,
                bio = data["bio"] as? String,
                avatar = data["avatar"] as? String,
                website = data["website"] as? String,
                twitter = data["twitter"] as? String,
                github = data["github"] as? String,
                linkedin = data["linkedin"] as? String,
                location = data["location"] as? String,
                company = data["company"] as? String,
                role = data["role"] as? String,
                socialLinks = (data["socialLinks"] as? Map<String, String>) ?: emptyMap(),
                joinedDate = (data["joinedDate"] as? String)?.let { LocalDateTime.parse(it, formatter) } ?: LocalDateTime.now()
            )
        } catch (e: Exception) {
            logger.error("Failed to parse author file: ${file.name}", e)
            null
        }
    }

    private fun saveAuthor(author: Author) {
        val authorsDir = File(basePath.toFile(), "authors")
        if (!authorsDir.exists()) {
            authorsDir.mkdirs()
        }

        val file = File(authorsDir, "${author.slug}$extension")
        val data = mapOf(
            "id" to author.id,
            "name" to author.name,
            "slug" to author.slug,
            "email" to author.email,
            "bio" to author.bio,
            "avatar" to author.avatar,
            "website" to author.website,
            "twitter" to author.twitter,
            "github" to author.github,
            "linkedin" to author.linkedin,
            "location" to author.location,
            "company" to author.company,
            "role" to author.role,
            "socialLinks" to author.socialLinks,
            "joinedDate" to author.joinedDate.format(formatter)
        )

        val yamlContent = yaml.dump(data)
        file.writeText(yamlContent)
        logger.info("Saved author: ${author.slug}")
    }

    private fun getAuthorFile(slug: String): File? {
        val authorsDir = File(basePath.toFile(), "authors")
        if (!authorsDir.exists()) return null

        val files = authorsDir.listFiles { file ->
            file.nameWithoutExtension == slug
        }

        return files?.firstOrNull()
    }

    private fun cacheUpdatedAuthor(updatedAuthor: Author) {
        val index = cachedAuthors.indexOfFirst { it.id == updatedAuthor.id }
        if (index >= 0) {
            cachedAuthors = cachedAuthors.toMutableList().apply { this[index] = updatedAuthor }
        } else {
            cachedAuthors = cachedAuthors + updatedAuthor
        }
    }

    private fun invalidateCache() {
        cachedAuthors = emptyList()
        lastLoaded = LocalDateTime.MIN
    }
}
