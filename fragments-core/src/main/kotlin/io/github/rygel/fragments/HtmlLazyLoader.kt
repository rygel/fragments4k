package io.github.rygel.fragments

object HtmlLazyLoader {
    private val IMG_TAG = Regex("<img\\s[^>]*>", RegexOption.IGNORE_CASE)

    fun addLazyLoading(html: String): String {
        if (html.isBlank() || !html.contains("<img", ignoreCase = true)) return html
        return IMG_TAG.replace(html) { match ->
            val tag = match.value
            if (tag.contains("loading=", ignoreCase = true)) tag
            else tag.replace("<img ", "<img loading=\"lazy\" ")
        }
    }
}
