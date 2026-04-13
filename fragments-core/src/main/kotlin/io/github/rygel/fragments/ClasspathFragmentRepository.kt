package io.github.rygel.fragments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * Classpath-backed read-only implementation of [FragmentRepository].
 *
 * Reads Markdown files bundled inside the application JAR (or exploded classpath).
 * On first access, all `.md` files listed in the [index file][INDEX_FILE_NAME] under
 * [basePath] are parsed and cached in memory.
 *
 * ### Index file
 *
 * The repository discovers content via a plain-text index file at
 * `<basePath>/index.list`, one relative filename per line (e.g. `hello-world.md`).
 * Generate this file at build time — for Maven, add an `exec-maven-plugin` goal or
 * an Ant task that writes the list of `.md` files under `src/main/resources/<basePath>`.
 *
 * ### Usage
 *
 * ```kotlin
 * val blogRepository = ClasspathFragmentRepository(
 *     basePath = "content/blog",
 *     baseUrl = "/blog",
 *     urlBuilder = { "/blog/${it.date?.year}/%02d/${it.slug}".format(it.date?.monthValue) },
 * )
 * ```
 *
 * ### Write operations
 *
 * All status-mutating operations throw [UnsupportedOperationException] — classpath
 * resources are read-only at runtime.
 *
 * @param basePath Classpath-relative path to the directory containing `.md` files
 *   (e.g. `"content/blog"`). Must not start with `/`.
 * @param baseUrl URL prefix for all fragments (e.g. `/blog`).
 * @param urlBuilder Optional factory that computes the canonical URL for each fragment,
 *   stored in [Fragment.resolvedUrl]. Takes precedence over [baseUrl].
 *   The resolved URL is used by [io.github.rygel.fragments.sitemap.SitemapGenerator],
 *   [io.github.rygel.fragments.LlmsTxtGenerator], and
 *   [io.github.rygel.fragments.rss.RssGenerator] when building absolute URLs. If your
 *   HTTP routes differ from the default `baseUrl/slug` pattern, provide a `urlBuilder`
 *   so that generated sitemaps, RSS feeds, and llms.txt contain correct URLs.
 * @param classLoader ClassLoader used to load resources; defaults to the thread context
 *   class loader.
 * @param parser [MarkdownParser] instance used to parse `.md` files.
 */
class ClasspathFragmentRepository(
    private val basePath: String,
    val baseUrl: String = "",
    val urlBuilder: ((Fragment) -> String)? = null,
    private val classLoader: ClassLoader =
        Thread.currentThread().contextClassLoader
            ?: ClasspathFragmentRepository::class.java.classLoader,
    private val parser: MarkdownParser = MarkdownParser(),
) : FragmentRepository {
    private val logger = LoggerFactory.getLogger(ClasspathFragmentRepository::class.java)
    private val normalizedBase = basePath.trimEnd('/')
    private var cachedFragments: List<Fragment> = emptyList()
    private var loaded = false

    companion object {
        const val INDEX_FILE_NAME = "index.list"
    }

    override suspend fun getAll(): List<Fragment> = withContext(Dispatchers.IO) { loadFragments() }

    override suspend fun getAllVisible(): List<Fragment> =
        withContext(Dispatchers.IO) {
            val now = LocalDateTime.now()
            loadFragments()
                .filter { fragment ->
                    fragment.visible &&
                        when (fragment.status) {
                            FragmentStatus.PUBLISHED ->
                                fragment.expiryDate == null || !fragment.expiryDate.isBefore(now)
                            FragmentStatus.SCHEDULED ->
                                fragment.publishDate != null &&
                                    !fragment.publishDate.isAfter(now) &&
                                    (fragment.expiryDate == null || !fragment.expiryDate.isBefore(now))
                            else -> false
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
                authorIds.any { fragment.authorIds.contains(it) || fragment.author == it }
            }
        }

    override suspend fun reload() {
        withContext(Dispatchers.IO) {
            loaded = false
            cachedFragments = loadFragmentsFromClasspath()
            loaded = true
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

    override suspend fun getFragmentsExpiringSoon(threshold: LocalDateTime): List<Fragment> =
        withContext(Dispatchers.IO) {
            loadFragments().filter { fragment ->
                fragment.expiryDate != null &&
                    !fragment.expiryDate.isAfter(threshold) &&
                    fragment.status == FragmentStatus.PUBLISHED
            }
        }

    override suspend fun getRelationships(
        slug: String,
        config: RelationshipConfig,
    ): ContentRelationships? =
        withContext(Dispatchers.IO) {
            val current = getBySlug(slug) ?: return@withContext null
            val others = getAllVisible().filter { it.slug != current.slug }
            ContentRelationshipGenerator.generateRelationships(current, others, config)
        }

    // ── Write operations — not supported on classpath resources ──────────────

    override suspend fun updateFragmentStatus(
        slug: String,
        status: FragmentStatus,
        force: Boolean,
        changedBy: String?,
        reason: String?,
    ): Result<Fragment> = Result.failure(UnsupportedOperationException("ClasspathFragmentRepository is read-only"))

    override suspend fun updateMultipleFragmentsStatus(
        slugs: List<String>,
        status: FragmentStatus,
        force: Boolean,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> = slugs.map { updateFragmentStatus(it, status, force, changedBy, reason) }

    override suspend fun publishMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> = updateMultipleFragmentsStatus(slugs, FragmentStatus.PUBLISHED, false, changedBy, reason)

    override suspend fun unpublishMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> = updateMultipleFragmentsStatus(slugs, FragmentStatus.DRAFT, false, changedBy, reason)

    override suspend fun archiveMultiple(
        slugs: List<String>,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> = updateMultipleFragmentsStatus(slugs, FragmentStatus.ARCHIVED, false, changedBy, reason)

    override suspend fun scheduleMultiple(
        slugs: List<String>,
        publishDate: LocalDateTime,
        changedBy: String?,
        reason: String?,
    ): List<Result<Fragment>> = slugs.map { Result.failure(UnsupportedOperationException("ClasspathFragmentRepository is read-only")) }

    override suspend fun publishScheduledFragments(threshold: LocalDateTime): List<Result<Fragment>> = emptyList()

    override suspend fun expireFragments(threshold: LocalDateTime): List<Result<Fragment>> = emptyList()

    override suspend fun createRevision(
        slug: String,
        changedBy: String?,
        reason: String?,
    ): Result<FragmentRevision> = Result.failure(UnsupportedOperationException("ClasspathFragmentRepository is read-only"))

    override suspend fun getFragmentRevisions(slug: String): List<FragmentRevision> = emptyList()

    override suspend fun revertToRevision(
        slug: String,
        revisionId: String,
        changedBy: String?,
        reason: String?,
    ): Result<Fragment> = Result.failure(UnsupportedOperationException("ClasspathFragmentRepository is read-only"))

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun loadFragments(): List<Fragment> {
        if (!loaded) {
            cachedFragments = loadFragmentsFromClasspath()
            loaded = true
        }
        return cachedFragments
    }

    private fun loadFragmentsFromClasspath(): List<Fragment> {
        val indexPath = "$normalizedBase/$INDEX_FILE_NAME"
        val indexStream = classLoader.getResourceAsStream(indexPath)
        if (indexStream == null) {
            logger.warn("Content index not found on classpath: $indexPath — no fragments loaded")
            return emptyList()
        }

        val filenames =
            indexStream
                .bufferedReader()
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }

        return filenames
            .mapNotNull { filename ->
                val resourcePath = "$normalizedBase/$filename"
                val stream = classLoader.getResourceAsStream(resourcePath)
                if (stream == null) {
                    logger.warn("Fragment resource not found on classpath: $resourcePath")
                    return@mapNotNull null
                }
                try {
                    val content = stream.bufferedReader().readText()
                    val nameWithoutExtension = filename.substringAfterLast('/').removeSuffix(".md")
                    parseFragment(content, nameWithoutExtension)
                } catch (e: IllegalArgumentException) {
                    logger.error("Error parsing classpath fragment: $resourcePath", e)
                    null
                } catch (e: IllegalStateException) {
                    logger.error("Error parsing classpath fragment: $resourcePath", e)
                    null
                } catch (e: java.io.IOException) {
                    logger.error("Error parsing classpath fragment: $resourcePath", e)
                    null
                }
            }.sortedBy { it.order }
    }

    private fun parseFragment(
        content: String,
        nameWithoutExtension: String,
    ): Fragment {
        val parsed = parser.parse(content)
        val frontMatter = parsed.frontMatter

        val title = frontMatter["title"]?.toString() ?: nameWithoutExtension
        val slug = frontMatter["slug"]?.toString() ?: generateSlug(nameWithoutExtension)
        val date = MarkdownParser.parseDate(frontMatter["date"])
        val status =
            try {
                FragmentStatus.valueOf(frontMatter["status"]?.toString() ?: "PUBLISHED")
            } catch (e: IllegalArgumentException) {
                FragmentStatus.PUBLISHED
            }

        val fragment =
            Fragment(
                title = title,
                slug = slug,
                baseUrl = baseUrl,
                status = status,
                date = date,
                publishDate = MarkdownParser.parseDate(frontMatter["publishDate"]),
                expiryDate = MarkdownParser.parseDate(frontMatter["expiryDate"]),
                preview = frontMatter["preview"]?.toString() ?: extractPreview(parsed.content),
                content = parsed.htmlContent,
                frontMatter = frontMatter,
                visible = frontMatter["visible"]?.toString()?.toBooleanStrictOrNull() ?: true,
                template = frontMatter["template"]?.toString() ?: "default",
                categories = parseStringList(frontMatter["categories"]),
                tags = parseStringList(frontMatter["tags"]),
                order = frontMatter["order"]?.toString()?.toIntOrNull() ?: 0,
                language = frontMatter["language"]?.toString() ?: "en",
                image = frontMatter["image"] as? String,
                author = frontMatter["author"]?.toString(),
                authorIds = parseStringList(frontMatter["authorIds"]),
                faq = parseFaqEntries(frontMatter),
                seriesSlug = frontMatter["series"]?.toString(),
                seriesPart = frontMatter["seriesPart"]?.toString()?.toIntOrNull(),
                seriesTitle = frontMatter["seriesTitle"]?.toString(),
            )
        return if (urlBuilder != null) fragment.copy(resolvedUrl = urlBuilder(fragment)) else fragment
    }

    private fun generateSlug(name: String): String =
        name
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")

    private fun extractPreview(content: String): String {
        val moreTag = Regex("<!--\\s*more\\s*-->", RegexOption.IGNORE_CASE).find(content)
        return when {
            moreTag != null -> content.substring(0, moreTag.range.first)
            content.length > 200 -> content.substring(0, 200) + "..."
            else -> content
        }
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
            is List<*> ->
                faqField.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        val q = item["q"]?.toString()
                        val a = item["a"]?.toString()
                        if (q != null && a != null) FaqEntry(q, a) else null
                    } else {
                        null
                    }
                }
            else -> emptyList()
        }
    }
}
