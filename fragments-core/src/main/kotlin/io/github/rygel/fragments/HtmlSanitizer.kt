package io.github.rygel.fragments

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

object HtmlSanitizer {
    private val SAFE_LIST =
        Safelist
            .relaxed()
            .addTags("kbd", "mark", "abbr", "details", "summary")
            .addAttributes(":all", "class", "id")
            .addAttributes("a", "target", "rel")
            .addAttributes("details", "open")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("th", "colspan", "rowspan")

    private val MORE_TAG_PATTERN = Regex("<!--\\s*more\\s*-->", RegexOption.IGNORE_CASE)
    private const val PLACEHOLDER = "FRAGMENTS4K_MORE_TAG_PLACEHOLDER"

    fun sanitize(html: String): String {
        val tags = MORE_TAG_PATTERN.findAll(html).map { it.value }.toList()
        if (tags.isEmpty()) return Jsoup.clean(html, SAFE_LIST)
        var placeholdered = html
        tags.forEachIndexed { i, tag ->
            placeholdered = placeholdered.replaceFirst(tag, "$PLACEHOLDER$i")
        }
        val cleaned = Jsoup.clean(placeholdered, SAFE_LIST)
        var result = cleaned
        tags.forEachIndexed { i, tag ->
            result = result.replace("$PLACEHOLDER$i", tag)
        }
        return result
    }
}
