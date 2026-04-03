package io.github.rygel.fragments.rss

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import java.time.format.DateTimeFormatter

class RssGenerator(
    private val repository: FragmentRepository
) {

    suspend fun generateFeed(
        siteTitle: String = "My Blog",
        siteDescription: String = "My Awesome Blog",
        siteUrl: String = "http://localhost:8080",
        feedUrl: String = "http://localhost:8080/rss.xml"
    ): String {
        val fragments = repository.getAllVisible()
            .sortedByDescending { it.date }
            .take(20)

        val lastBuildDate = fragments.firstOrNull()?.date?.format(formatter)
            ?: java.time.LocalDateTime.now().format(formatter)

        val items = fragments.joinToString(separator = "\n") { fragment ->
            renderItem(fragment, siteUrl)
        }

        return buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<rss version=\"2.0\">")
            appendLine("  <channel>")
            appendLine("    <title>${escapeXml(siteTitle)}</title>")
            appendLine("    <description>${escapeXml(siteDescription)}</description>")
            appendLine("    <link>${escapeXml(siteUrl)}</link>")
            appendLine("    <lastBuildDate>$lastBuildDate</lastBuildDate>")
            appendLine("    <atom:link href=\"${escapeXml(feedUrl)}\" rel=\"self\" type=\"application/rss+xml\" xmlns:atom=\"http://www.w3.org/2005/Atom\" />")
            appendLine(items)
            appendLine("  </channel>")
            appendLine("</rss>")
        }
    }

    private fun renderItem(fragment: Fragment, siteUrl: String): String {
        val pubDate = fragment.date?.format(formatter) ?: ""
        val fullUrl = "$siteUrl${fragment.url}"

        return buildString {
            appendLine("    <item>")
            appendLine("      <title>${escapeXml(fragment.title)}</title>")
            appendLine("      <link>${escapeXml(fullUrl)}</link>")
            appendLine("      <description>${escapeXml(fragment.previewTextOnly)}</description>")
            appendLine("      <pubDate>$pubDate</pubDate>")
            appendLine("      <guid>${escapeXml(fullUrl)}</guid>")

            fragment.categories.forEach { category ->
                appendLine("      <category>${escapeXml(category)}</category>")
            }

            fragment.tags.forEach { tag ->
                appendLine("      <category>${escapeXml(tag)}</category>")
            }

            appendLine("    </item>")
        }
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss")
    }
}
