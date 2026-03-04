package io.andromeda.fragments

import java.time.LocalDateTime

data class Fragment(
    val title: String,
    val slug: String,
    val date: LocalDateTime?,
    val preview: String,
    val content: String,
    val frontMatter: Map<String, Any>,
    val visible: Boolean = true,
    val template: String = "default",
    val categories: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val order: Int = 0,
    val language: String = "en",
    val languages: Map<String, String> = emptyMap(),
    val author: String? = null
) {
    val hasMoreTag: Boolean
        get() = content.contains("<!--more-->", ignoreCase = true) ||
                content.contains("<!-- more -->", ignoreCase = true)

    val previewTextOnly: String
        get() = preview.replace(Regex("<[^>]*>"), "").trim()

    val contentTextOnly: String
        get() = if (hasMoreTag) {
            content.substringBefore("<!--more-->")
                .substringBefore("<!-- more -->")
                .replace(Regex("<[^>]*>"), "")
                .trim()
        } else {
            content.replace(Regex("<[^>]*>"), "").trim()
        }
}
