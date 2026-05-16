package io.github.rygel.fragments

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.FragmentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * File-system backed implementation of [FragmentRepository].
 *
 * On first access, all Markdown files under [basePath] (including subdirectories)
 * are parsed and cached in memory. The cache is invalidated by calling [reload]
 * (e.g. from [io.github.rygel.fragments.livereload.LiveReloadManager]).
 * Status-mutating operations (publish, archive, etc.) write changes back to the
 * source `.md` file immediately and update the in-memory cache entry.
 *
 * File format: standard Markdown with a YAML front matter block delimited by `---`.
 * Any front matter field not explicitly mapped to a [Fragment] property is retained
 * in [Fragment.frontMatter] and accessible from templates.
 *
 * @param basePath Absolute path to the directory containing the Markdown content files.
 * @param baseUrl URL prefix for all fragments in this repository (e.g. `/projects`).
 *   Combined with each fragment's slug it forms the canonical URL. Leave empty for
 *   repositories whose fragments are routed individually (e.g. `/about`).
 * @param urlBuilder Optional factory that computes the canonical URL for each fragment
 *   after it is parsed, overriding the default `baseUrl/slug` scheme. Use this when
 *   you need date-based or hierarchical paths. Return a **relative path starting with
 *   `/`** — the result is stored in [Fragment.resolvedUrl] and returned by
 *   [Fragment.url]. When non-null, `urlBuilder` takes precedence over `baseUrl`.
 *
 *   **Important:** The URL returned by [Fragment.url] is used by
 *   [io.github.rygel.fragments.sitemap.SitemapGenerator],
 *   [io.github.rygel.fragments.LlmsTxtGenerator], and
 *   [io.github.rygel.fragments.rss.RssGenerator] to build the absolute URLs in their
 *   output. If your HTTP routes differ from the default `baseUrl/slug` pattern (e.g.
 *   `/blog/{year}/{month}/{slug}` for blog posts, or `/about` instead of `/page/about`
 *   for static pages), you **must** provide a `urlBuilder` — otherwise generated
 *   sitemaps, RSS feeds, and llms.txt will contain URLs that don't match your routes.
 *
 *   Example — date-based blog URLs:
 *   ```kotlin
 *   FileSystemFragmentRepository(
 *       basePath = "/content/posts",
*   urlBuilder = { fragment ->
 *       when (fragment.template) {
 *           "blog", "blog_post" -> {
 *               val date = fragment.date ?: return@FileSystemFragmentRepository "/${fragment.slug}"
 *               "/blog/${date.year}/%02d/${fragment.slug}".format(date.monthValue)
 *           }
 *           else -> "/page/${fragment.slug}"
 *       }
 *   },
 *   )
 *   ```
 *   Example — static pages at custom routes:
 *   ```kotlin
 *   FileSystemFragmentRepository(
 *       basePath = "/content/pages",
 *       urlBuilder = { fragment -> "/${fragment.slug}" },
 *   )
 *   ```
 * @param extension File extension to scan for; defaults to `.md`.
 * @param revisionRepository Storage backend for revision snapshots; defaults to a
 *   [FileSystemFragmentRevisionRepository] rooted at [basePath].
 * @param parser [MarkdownParser] instance used to convert `.md` files to [Fragment] objects.
 *   Override this to inject additional flexmark extensions (e.g. `ChatExtension` from
 *   `fragments-chat-core`):
 *   ```kotlin
 *   FileSystemFragmentRepository(
 *       basePath = "/content",
 *       parser = MarkdownParser(extraExtensions = listOf(ChatExtension.create())),
 *   )
 *   ```
 */
class FileSystemFragmentRepository(
    private val basePath: String,
    val baseUrl: String = "",
    val urlBuilder: ((Fragment) -> String)? = null,
    private val extension: String = ".md",
    private val revisionRepository: FragmentRevisionRepository = FileSystemFragmentRevisionRepository(basePath),
    private val parser: MarkdownParser = MarkdownParser(),
) : FragmentRepository {
    private val logger = LoggerFactory.getLogger(FileSystemFragmentRepository::class.java)

    private val canonicalBasePath: String by lazy {
        File(basePath).canonicalPath
    }

    @Volatile private var cachedFragments: List<Fragment> = emptyList()

    @Volatile private var lastLoaded: LocalDateTime = LocalDateTime.MIN

    @Volatile private var cachedRelationships: MutableMap<String, ContentRelationships> = mutableMapOf()

    override suspend fun getAll(): List<Fragment> =
        withContext(Dispatchers.IO) {
            loadFragments()
        }

    override suspend fun getAllVisible(): List<Fragment> =
        withContext(Dispatchers.IO) {
            val now = LocalDateTime.now()
            loadFragments()
                .filter { fragment ->
                    fragment.visible &&
                        when (fragment.status) {
                            FragmentStatus.PUBLISHED -> {
                                fragment.expiryDate == null || !fragment.expiryDate.isBefore(now)
                            }

                            FragmentStatus.SCHEDULED -> {
                                fragment.publishDate != null &&
                                    !fragment.publishDate.isAfter(now) &&
                                    (fragment.expiryDate == null || !fragment.expiryDate.isBefore(now))
                            }

                            FragmentStatus.DRAFT,
                            FragmentStatus.REVIEW,
                            FragmentStatus.APPROVED,
                            FragmentStatus.ARCHIVED,
                            FragmentStatus.EXPIRED,
                            -> {
                                false
                            }
                        }
                }.sortedByDescending { it.date }
        }

    override suspend fun getBySlug(slug: String): Fragment? =
        withContext(Dispatchers.IO) {
            loadFragments().find { it.slug == slug || it.slug == "/$slug" }
        }

    override suspend fun getByYearMonthAndSlug(
        year: String,
        month: String,
        slug: String,
    ): Fragment? =
        withContext(Dispatchers.IO) {
            loadFragments().find {
                it.slug == slug &&
                    it.date?.year == year.toIntOrNull() &&
                    it.date?.monthValue == month.toIntOrNull()
            }
        }

    override suspend fun getByTag(tag: String): List<Fragment> =
        withContext(Dispatchers.IO) {
            loadFragments().filter { it.tags.contains(tag.lowercase()) }
        }

    override suspend fun getByCategory(category: String): List<Fragment> =
        withContext(Dispatchers.IO) {
            loadFragments().filter { it.categories.contains(category.lowercase()) }
        }

    override suspend fun getByStatus(status: FragmentStatus): List<Fragment> =
        withContext(Dispatchers.IO) {
            loadFragments().filter { it.status == status }
        }

    override suspend fun getByAuthor(authorId: String): List<Fragment> =
        withContext(Dispatchers.IO) {
            loadFragments().filter { it.authorIds.contains(authorId) || it.author == authorId }
        }

    override suspend fun getByAuthors(authorIds: List<String>): List<Fragment> =
        withContext(Dispatchers.IO) {
            loadFragments().filter { fragment ->
                authorIds.any { authorId ->
                    fragment.authorIds.contains(authorId) || fragment.author == authorId
                }
            }
        }

    override suspend fun updateFragmentStatus(
        slug: String,
        status: FragmentStatus,
        force: Boolean,
        changedBy: String?,
        reason: String?,
    ): Result<Fragment> {
        return withContext(Dispatchers.IO) {
            val file = getFragmentFile(slug)
            if (file == null || !file.exists()) {
                logger.warn("Fragment file not found for slug: $slug")
                return@withContext Result.failure(IllegalArgumentException("Fragment not found: $slug"))
            }

            val currentFragment = parseFragmentFile(file)
            if (!force && !FragmentStatus.canTransition(currentFragment.status, status)) {
                val valid =
                    FragmentStatus
                        .getValidTransitions(currentFragment.status)
                        .joinToString(", ") { it.name.lowercase() }
                logger.warn("Invalid status transition for '{}': {} -> {}", slug, currentFragment.status, status)
                return@withContext Result.failure(
                    IllegalStateException(
                        "Cannot transition '$slug' from ${currentFragment.status} to $status. " +
                            "Valid transitions from ${currentFragment.status}: [$valid]. " +
                            "Pass force=true to bypass this check.",
                    ),
                )
            }

            try {
                val content = file.readText()
                val parsed = parser.parse(content)
                val updatedFrontMatter = parsed.frontMatter.toMutableMap()
                updatedFrontMatter["status"] = status.name

                val updatedHistory =
                    currentFragment.statusChangeHistory +
                        StatusChangeHistory(fromStatus = currentFragment.status, toStatus = status, changedBy = changedBy, reason = reason)
                updatedFrontMatter["statusChangeHistory"] = serializeStatusHistory(updatedHistory)

                val newContent =
                    buildString {
                        append("---\n")
                        dumpFrontMatter(updatedFrontMatter, this)
                        append("---\n")
                        append(parsed.content)
                    }

                file.writeText(newContent)

                val updatedFragment = parseFragmentFile(file)
                cacheUpdatedFragment(updatedFragment)

                logger.info("Updated fragment status: $slug -> $status (by: $changedBy, reason: $reason)")
                Result.success(updatedFragment)
            } catch (e: IOException) {
                logger.error("Failed to update fragment status: $slug", e)
                Result.failure(IOException("Failed to update status of fragment '$slug' to $status: ${e.message}", e))
            }
        }
    }

    /**
     * Serializes a [StatusChangeHistory] list to the YAML-friendly map structure written
     * into front matter. Centralised here so [updateFragmentStatus] and [scheduleMultiple]
     * share the same format.
     */
    private fun serializeStatusHistory(history: List<StatusChangeHistory>): List<Map<String, Any?>> =
        history.map { entry ->
            mapOf(
                "fromStatus" to entry.fromStatus.name,
                "toStatus" to entry.toStatus.name,
                "changedAt" to entry.changedAt.toString(),
                "changedBy" to entry.changedBy,
                "reason" to entry.reason,
            )
        }

    override suspend fun reload() {
        withContext(Dispatchers.IO) {
            cachedFragments = loadFragmentsFromDisk()
            cachedRelationships.clear()
            lastLoaded = LocalDateTime.now(ZoneOffset.UTC)
        }
    }

    private fun getFragmentFile(slug: String): File? =
        safeWalk()
            .find { it.nameWithoutExtension == slug }

    private fun cacheUpdatedFragment(fragment: Fragment) {
        val index = cachedFragments.indexOfFirst { it.slug == fragment.slug }
        if (index >= 0) {
            cachedFragments = cachedFragments.toMutableList().apply { this[index] = fragment }
        }
    }

    private fun isWithinBasePath(file: File): Boolean {
        val canonical = file.canonicalPath
        return canonical == canonicalBasePath || canonical.startsWith(canonicalBasePath + File.separatorChar)
    }

    private fun safeWalk(): Sequence<File> =
        File(canonicalBasePath)
            .walkTopDown()
            .filter { it.isFile }
            .filter { it.extension == extension.removePrefix(".") }
            .filter { isWithinBasePath(it) }

    private fun loadFragments(): List<Fragment> =
        if (cachedFragments.isEmpty() || lastLoaded.isEqual(LocalDateTime.MIN)) {
            loadFragmentsFromDisk()
        } else {
            cachedFragments
        }

    private fun loadFragmentsFromDisk(): List<Fragment> {
        val directory = File(basePath)
        if (!directory.exists() || !directory.isDirectory) {
            logger.warn("Fragment directory does not exist: $basePath")
            return emptyList()
        }

        val files = safeWalk().toList()

        return files
            .mapNotNull { file ->
                try {
                    parseFragmentFile(file)
                } catch (e: IOException) {
                    logger.error("Error parsing fragment file: ${file.absolutePath}", e)
                    null
                }
            }.sortedBy { it.order }
    }

    private fun parseFragmentFile(file: File): Fragment {
        if (file.length() > MAX_FILE_SIZE) {
            throw IOException("Fragment file exceeds ${MAX_FILE_SIZE / (1024 * 1024)}MB limit: ${file.absolutePath}")
        }
        val content = file.readText()
        val parsed = parser.parse(content)
        val frontMatter = parsed.frontMatter

        val title = frontMatter["title"]?.toString() ?: file.nameWithoutExtension
        val slug =
            (frontMatter["slug"]?.toString() ?: generateSlug(file.nameWithoutExtension))
                .also { raw ->
                    if (!SLUG_PATTERN.matches(raw)) {
                        logger.warn("Invalid slug '{}' in file '{}' — slugs must match pattern [a-z0-9]+(-[a-z0-9]+)*", raw, file.name)
                    }
                }
        val date = MarkdownParser.parseDate(frontMatter["date"])

        val statusString = frontMatter["status"]?.toString()
        val status =
            try {
                FragmentStatus.valueOf(statusString ?: "PUBLISHED")
            } catch (e: IllegalArgumentException) {
                logger.error("Failed to parse status '$statusString' for file ${file.name}, defaulting to PUBLISHED", e)
                FragmentStatus.PUBLISHED
            }

        val visible = frontMatter["visible"]?.toString()?.toBooleanStrictOrNull() ?: true
        val template = frontMatter["template"]?.toString() ?: "default"
        val preview = frontMatter["preview"]?.toString() ?: extractPreview(parsed.content)
        val order = frontMatter["order"]?.toString()?.toIntOrNull() ?: 0
        val publishDate = MarkdownParser.parseDate(frontMatter["publishDate"])
        val expiryDate = MarkdownParser.parseDate(frontMatter["expiryDate"])

        val categories = parseStringList(frontMatter["categories"])
        val tags = parseStringList(frontMatter["tags"])
        val language = frontMatter["language"]?.toString() ?: "en"
        val languages = parseLanguagesMap(frontMatter)
        val image = frontMatter["image"] as? String
        val author = frontMatter["author"]?.toString()
        val authorIds = parseStringList(frontMatter["authorIds"])
        val statusChangeHistory = parseStatusChangeHistory(frontMatter)
        val faq = parseFaqEntries(frontMatter)
        val seriesSlug = frontMatter["series"]?.toString()
        val seriesPart = frontMatter["seriesPart"]?.toString()?.toIntOrNull()
        val seriesTitle = frontMatter["seriesTitle"]?.toString()

        val fragment =
            Fragment(
                title = title,
                slug = slug,
                baseUrl = baseUrl,
                status = status,
                date = date,
                publishDate = publishDate,
                expiryDate = expiryDate,
                preview = preview,
                htmlContent = parsed.htmlContent,
                frontMatter = frontMatter,
                visible = visible,
                template = template,
                categories = categories,
                tags = tags,
                order = order,
                language = language,
                languages = languages,
                image = image,
                author = author,
                authorIds = authorIds,
                statusChangeHistory = statusChangeHistory,
                faq = faq,
                seriesSlug = seriesSlug,
                seriesPart = seriesPart,
                seriesTitle = seriesTitle,
            )
        return if (urlBuilder != null) fragment.copy(resolvedUrl = urlBuilder(fragment)) else fragment
    }

    private fun generateSlug(name: String): String =
        name
            .lowercase()
            .replace(SLUG_NON_ALPHANUMERIC, "")
            .replace(SLUG_WHITESPACE, "-")
            .replace(SLUG_CONSECUTIVE_DASHES, "-")
            .trim('-')

    private fun extractPreview(content: String): String {
        val moreTagIndex = MORE_TAG_PATTERN.find(content)
        return when {
            moreTagIndex != null -> content.substring(0, moreTagIndex.range.first)
            content.length > 200 -> content.substring(0, 200) + "..."
            else -> content
        }
    }

    companion object {
        private const val MAX_FILE_SIZE = 10L * 1024 * 1024
        private val SLUG_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
        private val SLUG_NON_ALPHANUMERIC = Regex("[^a-z0-9\\s-]")
        private val SLUG_WHITESPACE = Regex("\\s+")
        private val SLUG_CONSECUTIVE_DASHES = Regex("-+")
        private val MORE_TAG_PATTERN = Regex("<!--\\s*more\\s*-->", RegexOption.IGNORE_CASE)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStringList(value: Any?): List<String> =
        when (value) {
            is List<*> -> value.mapNotNull { it?.toString() }
            is String -> value.split(",").map { it.trim() }
            else -> emptyList()
        }

    private fun parseFaqEntries(frontMatter: Map<String, Any>): List<FaqEntry> {
        val faqField = frontMatter["faq"] ?: return emptyList()
        return when (faqField) {
            is List<*> -> {
                faqField.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        val question = item["q"]?.toString()
                        val answer = item["a"]?.toString()
                        if (question != null && answer != null) FaqEntry(question, answer) else null
                    } else {
                        null
                    }
                }
            }

            else -> {
                emptyList()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLanguagesMap(frontMatter: Map<String, Any>): Map<String, String> {
        val languagesField = frontMatter["languages"]
        return when (languagesField) {
            is Map<*, *> -> {
                languagesField
                    .mapNotNull { (k, v) ->
                        k?.toString()?.let { key -> v?.toString()?.let { value -> key to value } }
                    }.toMap()
            }

            else -> {
                emptyMap()
            }
        }
    }

    private fun parseStatusChangeHistory(frontMatter: Map<String, Any>): List<StatusChangeHistory> {
        val historyField = frontMatter["statusChangeHistory"] ?: return emptyList()
        return when (historyField) {
            is List<*> -> {
                historyField.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        try {
                            StatusChangeHistory(
                                fromStatus = FragmentStatus.valueOf(item["fromStatus"]?.toString() ?: ""),
                                toStatus = FragmentStatus.valueOf(item["toStatus"]?.toString() ?: ""),
                                changedAt = item["changedAt"]?.toString()?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
                                changedBy = item["changedBy"]?.toString(),
                                reason = item["reason"]?.toString(),
                            )
                        } catch (e: IllegalArgumentException) {
                            logger.warn("Failed to parse status change history entry: $item", e)
                            null
                        } catch (e: DateTimeParseException) {
                            logger.warn("Failed to parse status change history entry: $item", e)
                            null
                        }
                    } else {
                        null
                    }
                }
            }

            else -> {
                emptyList()
            }
        }
    }

    override suspend fun updateMultipleFragmentsStatus(
        slugs: List<String>,
        status: FragmentStatus,
        force: Boolean,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> =
        withContext(Dispatchers.IO) {
            slugs.map { slug ->
                updateFragmentStatus(slug, status, force, changedBy, reason)
            }
        }

    override suspend fun publishMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> =
        withContext(Dispatchers.IO) {
            slugs.map { slug ->
                updateFragmentStatus(slug, FragmentStatus.PUBLISHED, force = false, changedBy = changedBy, reason = reason)
            }
        }

    override suspend fun unpublishMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> =
        withContext(Dispatchers.IO) {
            slugs.map { slug ->
                updateFragmentStatus(slug, FragmentStatus.DRAFT, force = false, changedBy = changedBy, reason = reason)
            }
        }

    override suspend fun archiveMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> =
        withContext(Dispatchers.IO) {
            slugs.map { slug ->
                updateFragmentStatus(slug, FragmentStatus.ARCHIVED, force = false, changedBy = changedBy, reason = reason)
            }
        }

    override suspend fun getScheduledFragmentsDueForPublication(threshold: LocalDateTime): List<Fragment> =
        withContext(Dispatchers.IO) {
            loadFragments().filter { fragment ->
                fragment.status == FragmentStatus.SCHEDULED &&
                    fragment.publishDate != null &&
                    !fragment.publishDate.isAfter(threshold)
            }
        }

    override suspend fun publishScheduledFragments(threshold: LocalDateTime): List<Result<Fragment>> =
        withContext(Dispatchers.IO) {
            val dueFragments =
                loadFragments().filter { fragment ->
                    fragment.status == FragmentStatus.SCHEDULED &&
                        fragment.publishDate != null &&
                        !fragment.publishDate.isAfter(threshold)
                }

            dueFragments.map { fragment ->
                updateFragmentStatus(
                    slug = fragment.slug,
                    status = FragmentStatus.PUBLISHED,
                    force = true,
                    changedBy = "system",
                    reason = "Scheduled publication",
                )
            }
        }

    override suspend fun scheduleMultiple(
        slugs: List<String>,
        publishDate: LocalDateTime,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> {
        return withContext(Dispatchers.IO) {
            slugs.map { slug ->
                val file = getFragmentFile(slug)
                if (file == null || !file.exists()) {
                    logger.warn("Fragment file not found for slug: $slug")
                    return@map Result.failure<Fragment>(IllegalArgumentException("Fragment not found: $slug"))
                }

                val currentFragment = parseFragmentFile(file)

                try {
                    val content = file.readText()
                    val parsed = parser.parse(content)
                    val updatedFrontMatter = parsed.frontMatter.toMutableMap()
                    updatedFrontMatter["status"] = FragmentStatus.SCHEDULED.name
                    updatedFrontMatter["publishDate"] = publishDate.toString()

                    val updatedHistory =
                        currentFragment.statusChangeHistory +
                            StatusChangeHistory(
                                fromStatus = currentFragment.status,
                                toStatus = FragmentStatus.SCHEDULED,
                                changedBy = changedBy,
                                reason = reason,
                            )
                    updatedFrontMatter["statusChangeHistory"] = serializeStatusHistory(updatedHistory)

                    val newContent =
                        buildString {
                            append("---\n")
                            dumpFrontMatter(updatedFrontMatter, this)
                            append("---\n")
                            append(parsed.content)
                        }

                    file.writeText(newContent)

                    val updatedFragment = parseFragmentFile(file)
                    cacheUpdatedFragment(updatedFragment)

                    logger.info("Scheduled fragment: $slug for $publishDate (by: $changedBy, reason: $reason)")
                    Result.success(updatedFragment)
                } catch (e: IOException) {
                    logger.error("Failed to schedule fragment: $slug", e)
                    Result.failure(IOException("Failed to schedule fragment '$slug' for $publishDate: ${e.message}", e))
                }
            }
        }
    }

    override suspend fun getFragmentsExpiringSoon(threshold: LocalDateTime): List<Fragment> =
        withContext(Dispatchers.IO) {
            loadFragments().filter { fragment ->
                fragment.expiryDate != null &&
                    !fragment.expiryDate.isAfter(threshold) &&
                    fragment.status == FragmentStatus.PUBLISHED
            }
        }

    override suspend fun expireFragments(threshold: LocalDateTime): List<Result<Fragment>> =
        withContext(Dispatchers.IO) {
            val expiredFragments =
                loadFragments().filter { fragment ->
                    fragment.status == FragmentStatus.PUBLISHED &&
                        fragment.expiryDate != null &&
                        fragment.expiryDate.isBefore(threshold)
                }

            expiredFragments.map { fragment ->
                updateFragmentStatus(
                    slug = fragment.slug,
                    status = FragmentStatus.EXPIRED,
                    force = true,
                    changedBy = "system",
                    reason = "Content expired",
                )
            }
        }

    private fun dumpFrontMatter(
        frontMatter: Map<String, Any>,
        output: StringBuilder,
    ) {
        frontMatter.forEach { (key, value) ->
            dumpValue(key, value, output, indent = "")
        }
    }

    private fun dumpValue(
        key: String,
        value: Any?,
        output: StringBuilder,
        indent: String,
    ) {
        when (value) {
            null -> {
                output.append("$indent$key: null\n")
            }

            is String -> {
                output.append("$indent$key: \"${value.yamlEscape()}\"\n")
            }

            is Boolean -> {
                output.append("$indent$key: $value\n")
            }

            is Number -> {
                output.append("$indent$key: $value\n")
            }

            is LocalDateTime -> {
                output.append("$indent$key: \"$value\"\n")
            }

            is Map<*, *> -> {
                output.append("$indent$key:\n")
                value.forEach { (k, v) -> dumpValue(k.toString(), v, output, "$indent  ") }
            }

            is List<*> -> {
                output.append("$indent$key:\n")
                value.forEach { item -> dumpListItem(item, output, "$indent  ") }
            }

            else -> {
                output.append("$indent$key: \"${value.toString().yamlEscape()}\"\n")
            }
        }
    }

    private fun dumpListItem(
        item: Any?,
        output: StringBuilder,
        indent: String,
    ) {
        when (item) {
            is Map<*, *> -> {
                val entries = item.entries.toList()
                if (entries.isEmpty()) {
                    output.append("$indent- {}\n")
                    return
                }
                // First key uses block sequence indicator `- `; subsequent keys align under it.
                val innerIndent = "$indent  "
                entries.forEachIndexed { index, (k, v) ->
                    if (index == 0) {
                        output.append("$indent- ")
                        dumpValue(k.toString(), v, output, "")
                    } else {
                        dumpValue(k.toString(), v, output, innerIndent)
                    }
                }
            }

            else -> {
                output.append("$indent- \"${item?.toString().orEmpty().yamlEscape()}\"\n")
            }
        }
    }

    /** Escapes characters that would produce invalid YAML inside a double-quoted scalar. */
    private fun String.yamlEscape(): String =
        this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

    override suspend fun getRelationships(
        slug: String,
        config: io.github.rygel.fragments.RelationshipConfig,
    ): ContentRelationships? {
        return withContext(Dispatchers.IO) {
            cachedRelationships[slug] ?: run {
                val currentFragment = getBySlug(slug) ?: return@withContext null
                val allFragments =
                    getAllVisible()
                        .filter { it.slug != currentFragment.slug }

                val relationships =
                    ContentRelationshipGenerator.generateRelationships(
                        currentFragment = currentFragment,
                        allFragments = allFragments,
                        config = config,
                    )
                cachedRelationships[slug] = relationships
                relationships
            }
        }
    }

    override suspend fun createRevision(
        slug: String,
        changedBy: String?,
        reason: String?,
    ): Result<FragmentRevision> =
        withContext(Dispatchers.IO) {
            val fragment = getBySlug(slug)
            if (fragment == null) {
                return@withContext Result.failure(IllegalArgumentException("Fragment not found: $slug"))
            }
            try {
                val revision = revisionRepository.saveRevision(fragment, changedBy, reason)
                Result.success(revision)
            } catch (e: IOException) {
                logger.error("Failed to create revision for fragment: $slug", e)
                Result.failure(IOException("Failed to create revision for fragment '$slug': ${e.message}", e))
            }
        }

    override suspend fun getFragmentRevisions(slug: String): List<FragmentRevision> =
        withContext(Dispatchers.IO) {
            revisionRepository.getRevisions(slug)
        }

    override suspend fun revertToRevision(
        slug: String,
        revisionId: String,
        changedBy: String?,
        reason: String?,
    ): Result<Fragment> =
        withContext(Dispatchers.IO) {
            val fragment = getBySlug(slug)
            if (fragment == null) {
                return@withContext Result.failure(IllegalArgumentException("Fragment not found: $slug"))
            }

            val revertedFragment = revisionRepository.revertToRevision(slug, revisionId, changedBy, reason)
            if (revertedFragment.isFailure) {
                return@withContext Result.failure(
                    revertedFragment.exceptionOrNull()
                        ?: IllegalStateException("Revert failed for '$slug' to revision $revisionId"),
                )
            }

            val result =
                revertedFragment.getOrNull()
                    ?: return@withContext Result.failure(
                        IllegalStateException("Revert produced null result for '$slug' revision $revisionId"),
                    )
            try {
                val file =
                    getFragmentFile(slug) ?: return@withContext Result.failure(IllegalArgumentException("Fragment file not found: $slug"))

                val content = file.readText()
                val parsed = parser.parse(content)
                val updatedFrontMatter = parsed.frontMatter.toMutableMap()
                updatedFrontMatter["title"] = result.title

                val newContent =
                    buildString {
                        append("---\n")
                        dumpFrontMatter(updatedFrontMatter, this)
                        append("---\n")
                        append(result.content)
                    }

                file.writeText(newContent)

                val updatedFragment = parseFragmentFile(file)
                cacheUpdatedFragment(updatedFragment)

                Result.success(updatedFragment)
            } catch (e: IOException) {
                logger.error("Failed to revert fragment: $slug", e)
                Result.failure(IOException("Failed to write reverted fragment '$slug' (revision $revisionId): ${e.message}", e))
            }
        }
}
