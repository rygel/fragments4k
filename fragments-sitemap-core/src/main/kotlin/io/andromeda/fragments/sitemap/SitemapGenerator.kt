package io.andromeda.fragments.sitemap

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SitemapGenerator(
    private val repository: FragmentRepository,
    private val siteUrl: String,
    private val lastModified: LocalDateTime?
) {

    suspend fun generateSitemap(): String {
        return withContext(Dispatchers.IO) {
            val fragments = repository.getAllVisible()
            val lastModDate = lastModified ?: fragments.maxOfOrNull { it.modifiedDate } ?: LocalDateTime.now()
            val lastModDateFormatted = lastModDate.format(formatter)

            val urls = fragments.joinToString(separator = "\n") { fragment ->
                val url = "$siteUrl/${fragment.slug}"
                val modDate = fragment.modifiedDate?.format(formatter) ?: lastModDateFormatted
                val changeFreq = if (fragment.modifiedDate != null && fragment.modifiedDate > lastModified.minusMonths(1)) "always" else "monthly"

                """
                <url>
                    <loc>$url</loc>
                    <lastmod>$modDate</lastmod>
                    <changefreq>$changeFreq</changefreq>
                </url>
                """
            }

            return """
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
    <url>
        <loc>$siteUrl</loc>
        <lastmod>$lastModDateFormatted</lastmod>
        <changefreq>weekly</changefreq>
    </url>
    $urls
</urlset>
"""
        }
    }

    companion object {
        private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    }
}
