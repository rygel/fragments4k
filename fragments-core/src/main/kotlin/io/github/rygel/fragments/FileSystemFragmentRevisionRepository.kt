package io.github.rygel.fragments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FileSystemFragmentRevisionRepository(
    private val basePath: String
) : FragmentRevisionRepository {

    private val logger = LoggerFactory.getLogger(FileSystemFragmentRevisionRepository::class.java)
    private val revisionsDir = File(basePath, ".revisions")
    private val fragmentsIndexFile = File(revisionsDir, "index.json")
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    init {
        if (!revisionsDir.exists()) {
            revisionsDir.mkdirs()
        }
    }

    override suspend fun saveRevision(fragment: Fragment, changedBy: String?, reason: String?): FragmentRevision = withContext(Dispatchers.IO) {
        val slug = fragment.slug
        val version = getNextVersion(slug)
        
        val revision = FragmentRevision(
            id = generateRevisionId(slug, version),
            fragmentSlug = slug,
            version = version,
            title = fragment.title,
            content = fragment.content,
            preview = fragment.preview,
            frontMatter = fragment.frontMatter,
            changedBy = changedBy,
            changedAt = LocalDateTime.now(),
            changeReason = reason,
            previousRevisionId = getPreviousRevisionId(slug, version),
            diff = null
        )

        val revisionFile = File(revisionsDir, "${revision.id}.json")
        revisionFile.writeText(serializeRevision(revision))
        
        updateIndex(slug, revision.id)
        
        logger.info("Saved revision ${revision.id} for fragment $slug")
        return@withContext revision
    }

    override suspend fun getRevisions(slug: String): List<FragmentRevision> = withContext(Dispatchers.IO) {
        val index = loadIndex()
        val revisionIds = index[slug] ?: return@withContext emptyList()
        
        revisionIds.mapNotNull { id ->
            try {
                loadRevision(id)
            } catch (e: Exception) {
                logger.warn("Failed to load revision: $id", e)
                null
            }
        }.sortedByDescending { it.version }
    }

    override suspend fun getRevision(id: String): FragmentRevision? = withContext(Dispatchers.IO) {
        try {
            loadRevision(id)
        } catch (e: Exception) {
            logger.warn("Failed to load revision: $id", e)
            null
        }
    }

    override suspend fun getLatestRevision(slug: String): FragmentRevision? = withContext(Dispatchers.IO) {
        val revisions = getRevisions(slug)
        revisions.firstOrNull()
    }

    override suspend fun getRevisionAtVersion(slug: String, version: Int): FragmentRevision? = withContext(Dispatchers.IO) {
        val revisions = getRevisions(slug)
        revisions.firstOrNull { it.version == version }
    }

    override suspend fun compareRevisions(fromId: String, toId: String): String? = withContext(Dispatchers.IO) {
        val from = loadRevision(fromId)
        val to = loadRevision(toId)
        
        if (from == null || to == null) {
            return@withContext null
        }
        
        generateDiff(from, to)
    }

    override suspend fun revertToRevision(slug: String, revisionId: String, changedBy: String?, reason: String?): Result<Fragment> = withContext(Dispatchers.IO) {
        val revision = loadRevision(revisionId)
        if (revision == null || revision.fragmentSlug != slug) {
            return@withContext Result.failure(IllegalArgumentException("Revision not found: $revisionId"))
        }

        val revertedFragment = Fragment(
            title = revision.title,
            slug = slug,
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.now(),
            publishDate = null,
            preview = revision.preview,
            content = revision.content,
            frontMatter = revision.frontMatter,
            statusChangeHistory = emptyList()
        )

        return@withContext Result.success(revertedFragment)
    }

    override suspend fun deleteRevisions(slug: String): Int = withContext(Dispatchers.IO) {
        val index = loadIndex()
        val ids = index[slug] ?: return@withContext 0
        
        ids.forEach { id ->
            File(revisionsDir, "$id.json").delete()
        }
        
        index.remove(slug)
        saveIndex(index)
        
        return@withContext ids.size
    }

    override suspend fun deleteRevisionsBefore(slug: String, before: LocalDateTime): Int = withContext(Dispatchers.IO) {
        val index = loadIndex()
        val ids = index[slug] ?: return@withContext 0
        
        val toRemove = ids.filter { id ->
            try {
                val revision = loadRevision(id)
                revision?.changedAt?.isBefore(before) == true
            } catch (e: Exception) {
                false
            }
        }
        
        toRemove.forEach { id ->
            File(revisionsDir, "$id.json").delete()
        }
        
        index[slug] = (ids - toRemove.toSet()).toMutableList()
        saveIndex(index)
        
        return@withContext toRemove.size
    }

    override suspend fun getRevisionCount(slug: String): Int = withContext(Dispatchers.IO) {
        val index = loadIndex()
        index[slug]?.size ?: 0
    }

    override suspend fun getAllRevisionSlugs(): List<String> = withContext(Dispatchers.IO) {
        val index = loadIndex()
        index.keys.toList()
    }

    private fun generateRevisionId(slug: String, version: Int): String {
        return "$slug-v$version-${System.currentTimeMillis()}"
    }

    private fun getNextVersion(slug: String): Int {
        val index = loadIndex()
        val ids = index[slug] ?: return 1
        return ids.size + 1
    }

    private fun getPreviousRevisionId(slug: String, version: Int): String? {
        if (version <= 1) return null
        val index = loadIndex()
        val ids = index[slug] ?: return null
        return ids.getOrNull(version - 2)
    }

    private fun serializeRevision(revision: FragmentRevision): String {
        return """
            |{
            |  "id": "${revision.id}",
            |  "fragmentSlug": "${revision.fragmentSlug}",
            |  "version": ${revision.version},
            |  "title": "${escapeJson(revision.title)}",
            |  "content": "${escapeJson(revision.content)}",
            |  "preview": "${escapeJson(revision.preview)}",
            |  "changedBy": ${revision.changedBy?.let { "\"$it\"" } ?: "null"},
            |  "changedAt": "${revision.changedAt.format(formatter)}",
            |  "changeReason": ${revision.changeReason?.let { "\"$it\"" } ?: "null"},
            |  "previousRevisionId": ${revision.previousRevisionId?.let { "\"$it\"" } ?: "null"}
            |}
        """.trimMargin().trim()
    }

    private fun loadRevision(id: String): FragmentRevision? {
        val file = File(revisionsDir, "$id.json")
        if (!file.exists()) return null
        
        val content = file.readText()
        val map = parseJsonToMap(content)
        
        return FragmentRevision(
            id = map["id"] as String,
            fragmentSlug = map["fragmentSlug"] as String,
            version = (map["version"] as Number).toInt(),
            title = map["title"] as String,
            content = map["content"] as String,
            preview = map["preview"] as String,
            frontMatter = emptyMap(),
            changedBy = map["changedBy"] as String?,
            changedAt = LocalDateTime.parse(map["changedAt"] as String, formatter),
            changeReason = map["changeReason"] as String?,
            previousRevisionId = map["previousRevisionId"] as String?,
            diff = null
        )
    }

    private fun updateIndex(slug: String, revisionId: String) {
        val index = loadIndex()
        val ids = index.getOrPut(slug) { mutableListOf() } as MutableList<String>
        ids.add(revisionId)
        saveIndex(index)
    }

    private fun loadIndex(): MutableMap<String, MutableList<String>> {
        if (!fragmentsIndexFile.exists()) {
            return mutableMapOf()
        }
        
        return try {
            val content = fragmentsIndexFile.readText()
            parseJsonToMap(content).mapValues { (_, value) ->
                if (value is List<*>) {
                    value.filterIsInstance<String>().toMutableList()
                } else {
                    mutableListOf()
                }
            }.toMutableMap()
        } catch (e: Exception) {
            logger.warn("Failed to load index, starting fresh", e)
            mutableMapOf()
        }
    }

    private fun saveIndex(index: MutableMap<String, MutableList<String>>) {
        val json = index.entries.joinToString(",\n  ") { (key, value) ->
            "\"$key\": [${value.joinToString(", ") { "\"$it\"" }}]"
        }
        fragmentsIndexFile.writeText("{\n  $json\n}")
    }

    private fun parseJsonToMap(content: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        var current = ""
        var inString = false
        var inObject = false
        var inArray = false
        var escape = false
        var currentKey = ""
        
        content.forEach { char ->
            when {
                escape -> {
                    current += char
                    escape = false
                }
                char == '\\' -> {
                    escape = true
                }
                char == '"' && !inArray -> {
                    inString = !inString
                }
                char == ':' && !inString && !inObject -> {
                    currentKey = current.trim().removeSurrounding("\"")
                    current = ""
                }
                char == ',' && !inString && !inObject && !inArray -> {
                    if (currentKey.isNotEmpty()) {
                        result[currentKey] = parseJsonValue(current.trim())
                    }
                    current = ""
                    currentKey = ""
                }
                char == '{' && !inString && !inArray -> {
                    inObject = true
                }
                char == '}' && !inString && !inArray -> {
                    inObject = false
                    if (currentKey.isNotEmpty()) {
                        result[currentKey] = parseJsonValue(current.trim())
                    }
                    current = ""
                    currentKey = ""
                }
                char == '[' && !inString && !inObject -> {
                    inArray = true
                }
                char == ']' && !inString -> {
                    inArray = false
                }
                else -> {
                    current += char
                }
            }
        }
        
        if (currentKey.isNotEmpty()) {
            result[currentKey] = parseJsonValue(current.trim())
        }
        
        return result
    }

    private fun parseJsonValue(value: String): Any {
        return when {
            value.startsWith("\"") && value.endsWith("\"") -> value.removeSurrounding("\"")
            value == "true" -> true
            value == "false" -> false
            value == "null" -> null as Any
            value.toIntOrNull() != null -> value.toInt() as Any
            value.toDoubleOrNull() != null -> value.toDouble() as Any
            else -> value as Any
        }
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun generateDiff(from: FragmentRevision, to: FragmentRevision): String {
        return buildString {
            if (from.title != to.title) {
                appendLine("Title changed from \"${from.title}\" to \"${to.title}\"")
            }
            if (from.content != to.content) {
                val contentDiff = simpleDiff(from.content, to.content)
                appendLine("Content changed:")
                append(contentDiff)
            }
        }
    }

    private fun simpleDiff(original: String, modified: String): String {
        val originalLines = original.lines()
        val modifiedLines = modified.lines()
        val maxLines = maxOf(originalLines.size, modifiedLines.size)

        return buildString {
            for (i in 0 until maxLines) {
                val originalLine = originalLines.getOrNull(i)
                val modifiedLine = modifiedLines.getOrNull(i)

                when {
                    originalLine == null && modifiedLine != null -> appendLine("+ $modifiedLine")
                    originalLine != null && modifiedLine == null -> appendLine("- $originalLine")
                    originalLine != modifiedLine -> {
                        appendLine("- $originalLine")
                        appendLine("+ $modifiedLine")
                    }
                }
            }
        }
    }
}
