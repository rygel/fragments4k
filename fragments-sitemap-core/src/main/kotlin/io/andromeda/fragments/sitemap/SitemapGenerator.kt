package io.andromeda.fragments.sitemap

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository
import io.andromeda.fragments.sitemap.ChangeFrequency
import io.andromeda.fragments.sitemap.SitemapImage
import io.andromeda.fragments.sitemap.SitemapUrl
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SitemapGenerator(
    private val repository: FragmentRepository,
    private val siteUrl: String,
    private val lastModified: LocalDateTime? = null
) {

    suspend fun generateSitemap(): String {
        return withContext(Dispatchers.IO) {
            val fragments = repository.getAllVisible()
            val lastModDate = lastModified ?: fragments.mapNotNull { it.date }.maxOrNull() ?: LocalDateTime.now()
            val lastModDateFormatted = lastModDate.format(formatter)

            val urls = fragments.map { fragment ->
                val url = "$siteUrl/${fragment.slug}"
                val modDate = fragment.date?.format(formatter) ?: lastModDateFormatted
                val changeFreq = ChangeFrequency.fromFragment(fragment, lastModified)
                val priority = ChangeFrequency.calculatePriority(fragment, lastModified)
                val image = extractFragmentImage(fragment)

                SitemapUrl(
                    loc = url,
                    lastmod = modDate,
                    changefreq = changeFreq,
                    priority = priority,
                    image = image
                )
            }.sortedByDescending { it.priority }.joinToString(separator = "\n") { sitemapUrl ->
                val imageXml = sitemapUrl.image?.let { image ->
                    """
                    <image:image>
                        <image:loc>${image.loc}</image:loc>
                        ${image.caption?.let { "<image:caption>${it}</image:caption>" } ?: ""}
                        ${image.title?.let { "<image:title>${it}</image:title>" } ?: ""}
                    </image:image>
                    """
                } ?: ""

                """
                <url>
                    <loc>${sitemapUrl.loc}</loc>
                    <lastmod>${sitemapUrl.lastmod}</lastmod>
                    <changefreq>${sitemapUrl.changefreq.value}</changefreq>
                    <priority>${"%.1f".format(sitemapUrl.priority)}</priority>$imageXml
                </url>
                """
            }

            """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                    xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">
                <url>
                    <loc>$siteUrl</loc>
                    <lastmod>$lastModDateFormatted</lastmod>
                    <changefreq>weekly</changefreq>
                    <priority>1.0</priority>
                </url>
                $urls
            </urlset>
            """
        }
    }

    private fun extractFragmentImage(fragment: Fragment): SitemapImage? {
        val imageUrl = extractImageUrl(fragment)
        return imageUrl?.let {
            SitemapImage(
                loc = it,
                caption = fragment.title,
                title = fragment.title
            )
        }
    }

    private fun extractImageUrl(fragment: Fragment): String? {
        fragment.frontMatter["image"]?.let { return it.toString() }
        fragment.frontMatter["og:image"]?.let { return it.toString() }
        fragment.frontMatter["twitter:image"]?.let { return it.toString() }
        
        val imgTagPattern = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        imgTagPattern.find(fragment.preview)?.groupValues?.get(1)?.let { return it }
        
        return null
    }

    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }
}

