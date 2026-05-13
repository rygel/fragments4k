package io.github.rygel.fragments

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

enum class SanitizerProfile {
    RELAXED_TRUSTED_AUTHOR,
    STRICT,
}

object HtmlSanitizer {
    private val RELAXED_SAFE_LIST =
        Safelist
            .relaxed()
            .addTags("kbd", "mark", "abbr", "details", "summary")
            .addAttributes(":all", "class", "id")
            .addAttributes("a", "target", "rel")
            .addAttributes("details", "open")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("th", "colspan", "rowspan")

    private val STRICT_SAFE_LIST =
        Safelist
            .basic()
            .addTags("kbd", "mark", "abbr", "details", "summary")
            .addAttributes("a", "target", "rel")
            .addAttributes("details", "open")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("th", "colspan", "rowspan")

    private val MORE_TAG_PATTERN = Regex("<!--\\s*more\\s*-->", RegexOption.IGNORE_CASE)
    private const val PLACEHOLDER = "FRAGMENTS4K_MORE_TAG_PLACEHOLDER"

    fun sanitize(
        html: String,
        profile: SanitizerProfile = SanitizerProfile.RELAXED_TRUSTED_AUTHOR,
    ): String {
        val safeList = if (profile == SanitizerProfile.STRICT) STRICT_SAFE_LIST else RELAXED_SAFE_LIST
        val tags = MORE_TAG_PATTERN.findAll(html).map { it.value }.toList()
        if (tags.isEmpty()) return Jsoup.clean(html, safeList)
        var placeholdered = html
        tags.forEachIndexed { i, tag ->
            placeholdered = placeholdered.replaceFirst(tag, "$PLACEHOLDER$i")
        }
        val cleaned = Jsoup.clean(placeholdered, safeList)
        var result = cleaned
        tags.forEachIndexed { i, tag ->
            result = result.replace("$PLACEHOLDER$i", tag)
        }
        return result
    }
}
