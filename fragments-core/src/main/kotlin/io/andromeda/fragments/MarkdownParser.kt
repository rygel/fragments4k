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

    data class ParsedContent(
        val frontMatter: Map<String, Any>,
        val content: String,
        val htmlContent: String
    )

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
