package io.github.rygel.fragments

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.yaml.snakeyaml.Yaml
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Parses Markdown files that contain an optional YAML front matter block.
 *
 * Supported Markdown extensions: tables, strikethrough, task lists, auto-links,
 * and footnotes (via flexmark-all).
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
 */
class MarkdownParser {
    private val options = MutableDataSet().apply {
        set(Parser.EXTENSIONS, listOf(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            TaskListExtension.create(),
            AutolinkExtension.create(),
            FootnoteExtension.create(),
        ))
    }
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()
    private val yaml = Yaml()

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
        val frontMatterPattern = Regex("^---\\s*\\n(.*?)\\n---\\s*\\n", RegexOption.DOT_MATCHES_ALL)
        val match = frontMatterPattern.find(markdown)

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
            emptyMap()
        }
    }

    companion object {
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
        fun parseDate(dateValue: Any?): LocalDateTime? {
            return when (dateValue) {
                is java.time.LocalDateTime -> dateValue
                is java.time.LocalDate -> dateValue.atStartOfDay()
                is java.util.Date -> dateValue.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
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
