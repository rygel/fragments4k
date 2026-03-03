package io.andromeda.fragments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FileSystemFragmentRepository(
    private val basePath: String,
    private val extension: String = ".md"
) : FragmentRepository {

    private val logger = LoggerFactory.getLogger(FileSystemFragmentRepository::class.java)
    private val parser = MarkdownParser()
    private var cachedFragments: List<Fragment> = emptyList()
    private var lastLoaded: LocalDateTime = LocalDateTime.MIN

    override suspend fun getAll(): List<Fragment> = withContext(Dispatchers.IO) {
        loadFragments()
    }

    override suspend fun getAllVisible(): List<Fragment> = withContext(Dispatchers.IO) {
        loadFragments().filter { it.visible }.sortedByDescending { it.date }
    }

    override suspend fun getBySlug(slug: String): Fragment? = withContext(Dispatchers.IO) {
        loadFragments().find { it.slug == slug || it.slug == "/$slug" }
    }

    override suspend fun getByYearMonthAndSlug(year: String, month: String, slug: String): Fragment? = 
        withContext(Dispatchers.IO) {
            loadFragments().find { 
                it.slug == slug && 
                it.date?.year == year.toIntOrNull() && 
                it.date?.monthValue == month.toIntOrNull()
            }
        }

    override suspend fun getByTag(tag: String): List<Fragment> = withContext(Dispatchers.IO) {
        loadFragments().filter { it.tags.contains(tag.lowercase()) }
    }

    override suspend fun getByCategory(category: String): List<Fragment> = withContext(Dispatchers.IO) {
        loadFragments().filter { it.categories.contains(category.lowercase()) }
    }

    override suspend fun reload() {
        withContext(Dispatchers.IO) {
            cachedFragments = loadFragmentsFromDisk()
            lastLoaded = LocalDateTime.now()
        }
    }

    private suspend fun loadFragments(): List<Fragment> {
        if (cachedFragments.isEmpty() || lastLoaded == LocalDateTime.MIN) {
            reload()
        }
        return cachedFragments
    }

    private fun loadFragmentsFromDisk(): List<Fragment> {
        val directory = File(basePath)
        if (!directory.exists() || !directory.isDirectory) {
            logger.warn("Fragment directory does not exist: $basePath")
            return emptyList()
        }

        val files = directory.listFiles { file -> file.extension == extension.removePrefix(".") }
            ?: return emptyList()

        return files.mapNotNull { file ->
            try {
                parseFile(file)
            } catch (e: Exception) {
                logger.error("Error parsing fragment file: ${file.absolutePath}", e)
                null
            }
        }.sortedBy { it.order }
    }

    private fun parseFile(file: File): Fragment {
        val content = file.readText()
        val parsed = parser.parse(content)
        val frontMatter = parsed.frontMatter

        val title = frontMatter["title"]?.toString() ?: file.nameWithoutExtension
        val slug = frontMatter["slug"]?.toString() ?: generateSlug(file.nameWithoutExtension)
        val date = MarkdownParser.parseDate(frontMatter["date"])
        val visible = frontMatter["visible"]?.toString()?.toBooleanStrictOrNull() ?: true
        val template = frontMatter["template"]?.toString() ?: "default"
        val preview = frontMatter["preview"]?.toString() ?: extractPreview(parsed.content)
        val order = frontMatter["order"]?.toString()?.toIntOrNull() ?: 0

        val categories = parseStringList(frontMatter["categories"])
        val tags = parseStringList(frontMatter["tags"])
        val language = frontMatter["language"]?.toString() ?: "en"
        val languages = parseLanguagesMap(frontMatter)

        return Fragment(
            title = title,
            slug = slug,
            date = date,
            preview = preview,
            content = parsed.htmlContent,
            frontMatter = frontMatter,
            visible = visible,
            template = template,
            categories = categories,
            tags = tags,
            order = order,
            language = language,
            languages = languages
        )
    }

    private fun generateSlug(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
    }

    private fun extractPreview(content: String): String {
        val moreTagIndex = content.indexOf("<!--more-->", ignoreCase = true)
        val moreTagIndex2 = content.indexOf("<!-- more -->", ignoreCase = true)
        return when {
            moreTagIndex >= 0 -> content.substring(0, moreTagIndex)
            moreTagIndex2 >= 0 -> content.substring(0, moreTagIndex2)
            content.length > 200 -> content.substring(0, 200) + "..."
            else -> content
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStringList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.mapNotNull { it?.toString() }
            is String -> value.split(",").map { it.trim() }
            else -> emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLanguagesMap(frontMatter: Map<String, Any>): Map<String, String> {
        val languagesField = frontMatter["languages"]
        return when (languagesField) {
            is Map<*, *> -> languagesField.mapNotNull { (k, v) -> 
                k?.toString()?.let { key -> v?.toString()?.let { value -> key to value } }
            }.toMap()
            else -> emptyMap()
        }
    }
}
