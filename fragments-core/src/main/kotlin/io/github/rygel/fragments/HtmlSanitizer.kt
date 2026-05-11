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

    fun sanitize(html: String): String = Jsoup.clean(html, SAFE_LIST)
}
