package io.andromeda.fragments.rss

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository
import java.time.format.DateTimeFormatter

class RssGenerator(
    private val repository: FragmentRepository,
    private val siteTitle: String,
    private val siteDescription: String,
    private val siteUrl: String,
    private val feedUrl: String
) {

    suspend fun generateFeed(): String {
        val fragments = repository.getAllVisible()
            .sortedByDescending { it.date }
            .take(20)

        val lastBuildDate = fragments.firstOrNull()?.date?.format(formatter) 
            ?: java.time.LocalDateTime.now().format(formatter)

        val items = fragments.joinToString(separator = "\n") { fragment ->
            renderItem(fragment)
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

    private fun renderItem(fragment: Fragment): String {
        val pubDate = fragment.date?.format(formatter) ?: ""
        val fullUrl = "$siteUrl/${fragment.slug}"
        
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
        private val formatter = DateTimeFormatter.RFC_1123_DATE_TIME
    }
}
