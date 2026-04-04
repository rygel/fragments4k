package io.github.rygel.fragments

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.misc.Extension
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.resolver.Resolver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * A SnakeYAML [Resolver] that removes the implicit [Tag.TIMESTAMP] resolver.
 *
 * By default SnakeYAML auto-parses values like `2026-03-29` into [java.util.Date],
 * which loses timezone information and triggers warnings in [MarkdownParser.parseDate].
 * With this resolver dates arrive as plain [String] values and are parsed explicitly
 * by [MarkdownParser.Companion.parseDateString].
 */
private class NoDateResolver : Resolver() {
    override fun addImplicitResolver(tag: Tag, regexp: java.util.regex.Pattern, first: String?, limit: Int) {
        if (tag != Tag.TIMESTAMP) {
            super.addImplicitResolver(tag, regexp, first, limit)
        }
    }
}

/**
 * Parses Markdown files that contain an optional YAML front matter block.
 *
 * Supported Markdown extensions: tables, strikethrough, task lists, auto-links,
 * and footnotes (via flexmark-all). Additional flexmark extensions (e.g.
 * `ChatExtension` from `fragments-chat-core`) can be injected via [extraExtensions].
 *
 * Front matter must be a YAML block delimited by `---` at the very start of the file:
 * ```
 * ---
 * title: "My Post"
 * date: 2024-01-15
 * tags: [kotlin, jvm]
 * ---
 * Regular Markdown content here.
 * ```
 *
 * Use [parse] to get a [ParsedContent] with the separated front matter and rendered HTML.
 * Use the [parseDate] companion function to normalise date values from YAML to [java.time.LocalDateTime].
 *
 * @param extraExtensions Additional flexmark [Extension] instances appended to the
 *   default set. Pass extensions here rather than constructing a separate parser
 *   so that the YAML front matter stripping and caching logic is shared.
 */
class MarkdownParser(extraExtensions: List<Extension> = emptyList()) {

    private val logger = LoggerFactory.getLogger(MarkdownParser::class.java)
    private val options = MutableDataSet().apply {
        set(
            Parser.EXTENSIONS,
            listOf(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create(),
                AutolinkExtension.create(),
                FootnoteExtension.create(),
            ) + extraExtensions,
        )
    }
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()
    private val yaml = run {
        val loaderOptions = LoaderOptions()
        val dumperOptions = DumperOptions()
        Yaml(
            SafeConstructor(loaderOptions),
            Representer(dumperOptions),
            dumperOptions,
            loaderOptions,
            NoDateResolver(),
        )
    }

    /**
     * The result of parsing a Markdown file.
     *
     * @property frontMatter Raw key/value map extracted from the YAML front matter block.
     *   Empty map when no front matter is present.
     * @property content Raw Markdown body (front matter stripped).
     * @property htmlContent [content] rendered to HTML.
     */
    data class ParsedContent(
        val frontMatter: Map<String, Any>,
        val content: String,
        val htmlContent: String
    )

    /**
     * Parses [markdown] into a [ParsedContent], separating front matter from body
     * and converting the body to HTML.
     */
    fun parse(markdown: String): ParsedContent {
        val match = FRONT_MATTER_PATTERN.find(markdown)

        return if (match != null) {
            val frontMatterYaml = match.groupValues[1]
            val rawContent = markdown.substring(match.range.last + 1)
            val frontMatter = parseFrontMatter(frontMatterYaml)
            val htmlContent = renderer.render(parser.parse(rawContent))
            ParsedContent(frontMatter, rawContent, htmlContent)
        } else {
            ParsedContent(emptyMap(), markdown, renderer.render(parser.parse(markdown)))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseFrontMatter(yamlContent: String): Map<String, Any> {
        return try {
            yaml.load(yamlContent) as? Map<String, Any> ?: emptyMap()
        } catch (e: Exception) {
            logger.warn("Failed to parse YAML front matter — returning empty map. Cause: ${e.message}")
            emptyMap()
        }
    }

    companion object {
        // \n? at end allows files with no trailing newline after the closing ---
        private val FRONT_MATTER_PATTERN = Regex("^---\\s*\\n(.*?)\\n---\\s*\\n?", RegexOption.DOT_MATCHES_ALL)

        private val logger = LoggerFactory.getLogger(MarkdownParser::class.java)

        private val DATE_TIME_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        )
        
        private val DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        /**
         * Converts a raw YAML date value to [LocalDateTime].
         *
         * Handles all common representations produced by SnakeYAML:
         * - [java.time.LocalDateTime] / [java.time.LocalDate] — passed through directly.
         * - [java.util.Date] — converted via the system default time zone.
         * - [String] — tried against `yyyy-MM-dd'T'HH:mm`, `yyyy-MM-dd HH:mm`,
         *   and `yyyy-MM-dd` (midnight) in that order.
         *
         * Returns `null` for unrecognised types or unparseable strings.
         */
        /**
         * All dates stored in [io.github.rygel.fragments.Fragment] are treated as **UTC**.
         * String dates without timezone information are assumed to be UTC. `java.util.Date`
         * values (produced by SnakeYAML for `yyyy-MM-dd` YAML values) are converted via UTC.
         * Convert to a user-local timezone at display time via
         * [io.github.rygel.fragments.FragmentViewModel.dateInZone].
         */
        fun parseDate(dateValue: Any?): LocalDateTime? {
            return when (dateValue) {
                is java.time.LocalDateTime -> dateValue
                is java.time.LocalDate -> {
                    logger.warn(
                        "Date '{}' has no time or timezone — treating as UTC midnight. " +
                        "Use 'yyyy-MM-dd''T''HH:mm' to suppress this warning.",
                        dateValue
                    )
                    dateValue.atStartOfDay()
                }
                is java.util.Date -> {
                    logger.warn(
                        "Date '{}' (java.util.Date from SnakeYAML) has no explicit timezone — " +
                        "treating as UTC. Use 'yyyy-MM-dd''T''HH:mm' format in your front matter " +
                        "to suppress this warning.",
                        dateValue
                    )
                    dateValue.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime()
                }
                is String -> parseDateString(dateValue)
                else -> null
            }
        }
        
        private fun parseDateString(dateString: String?): LocalDateTime? {
            if (dateString.isNullOrBlank()) return null
            
            // Try date-time formatters first
            for (formatter in DATE_TIME_FORMATTERS) {
                try {
                    return LocalDateTime.parse(dateString, formatter)
                } catch (e: Exception) {
                    // Continue to next formatter
                }
            }
            
            // Try date-only format
            try {
                return LocalDate.parse(dateString, DATE_ONLY_FORMATTER).atStartOfDay()
            } catch (e: Exception) {
                // Continue
            }
            
            return null
        }
    }
}
