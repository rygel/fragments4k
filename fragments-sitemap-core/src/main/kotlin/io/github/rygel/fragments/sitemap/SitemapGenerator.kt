package io.github.rygel.fragments.sitemap

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

/**
 * Generates an XML sitemap from all visible fragments across repositories.
 *
 * URLs are derived from each fragment's [Fragment.url] property, which
 * resolves to [Fragment.resolvedUrl] when set by the repository's `urlBuilder`,
 * or falls back to `baseUrl/slug`. For correct sitemap URLs, ensure your
 * repository configures a `urlBuilder` that matches your adapter routes:
 * - Blog posts: `/blog/{year}/{month}/{slug}`
 * - Static pages: `/page/{slug}`
 *
 * The generated sitemap includes:
 * - All visible published fragments from all repositories
 * - Root site URL with priority 1.0
 * - Image metadata from fragment front matter
 * - Priority based on content type (higher for blog posts)
 * - Change frequency based on fragment status
 */
class SitemapGenerator(
    private val repositories: List<FragmentRepository>,
    private val siteUrl: String,
    private val lastModified: LocalDateTime? = null,
    /**
     * Template names to exclude from the sitemap entirely.
     *
     * By default all visible fragments are included. Pass template names
     * (e.g. `setOf("email", "draft-preview")`) to suppress fragments that
     * should never appear in public sitemaps.
     */
    private val excludedTemplates: Set<String> = emptySet(),
) {
    constructor(
        repository: FragmentRepository,
        siteUrl: String,
        lastModified: LocalDateTime? = null,
        excludedTemplates: Set<String> = emptySet(),
    ) : this(listOf(repository), siteUrl, lastModified, excludedTemplates)

    private val logger = LoggerFactory.getLogger(SitemapGenerator::class.java)

    suspend fun generateSitemap(resolvedFragments: List<Fragment>? = null): String =
        withContext(Dispatchers.IO) {
            val allCandidates =
                (resolvedFragments ?: repositories.flatMap { it.getAllVisible() })
                    .distinctBy { it.slug }
                    .filter { it.template !in excludedTemplates }

            // Exclude fragments whose URL was not explicitly resolved by a urlBuilder.
            // The Fragment.url fallback (baseUrl/slug) may not match the actual HTTP
            // route, producing 404 URLs in the published sitemap. See #65 / #77.
            val skipped = allCandidates.filter { it.resolvedUrl == null }
            if (skipped.isNotEmpty()) {
                logger.warn(
                    "Skipping {} fragment(s) without resolvedUrl in sitemap (configure urlBuilder on the repository): {}",
                    skipped.size,
                    skipped.joinToString { it.slug },
                )
            }
            val fragments = allCandidates.filter { it.resolvedUrl != null }
            val lastModDate = lastModified ?: fragments.mapNotNull { it.date }.maxOrNull() ?: LocalDateTime.now()
            val lastModDateFormatted = lastModDate.format(formatter)

            val urls =
                fragments
                    .map { fragment ->
                        val url = "$siteUrl${fragment.url}"
                        val modDate = fragment.date?.format(formatter) ?: lastModDateFormatted
                        val changeFreq = ChangeFrequency.fromFragment(fragment, lastModified)
                        val priority = ChangeFrequency.calculatePriority(fragment, lastModified)
                        val image = extractFragmentImage(fragment)

                        SitemapUrl(
                            loc = url,
                            lastmod = modDate,
                            changefreq = changeFreq,
                            priority = priority,
                            image = image,
                        )
                    }.sortedByDescending { it.priority }

            val writer = StringWriter()
            val xml = XMLOutputFactory.newInstance().createXMLStreamWriter(writer)

            xml.writeStartDocument("UTF-8", "1.0")
            xml.writeStartElement("urlset")
            xml.writeDefaultNamespace(SITEMAP_NS)
            xml.writeNamespace("image", IMAGE_NS)

            writeUrl(xml, siteUrl, lastModDateFormatted, "weekly", 1.0, null)

            for (url in urls) {
                writeUrl(xml, url.loc, url.lastmod, url.changefreq.value, url.priority, url.image)
            }

            xml.writeEndElement()
            xml.writeEndDocument()
            xml.flush()

            writer.toString()
        }

    private fun writeUrl(
        xml: XMLStreamWriter,
        loc: String,
        lastmod: String,
        changefreq: String,
        priority: Double,
        image: SitemapImage?,
    ) {
        xml.writeStartElement("url")
        writeElement(xml, "loc", loc)
        writeElement(xml, "lastmod", lastmod)
        writeElement(xml, "changefreq", changefreq)
        writeElement(xml, "priority", String.format(java.util.Locale.US, "%.1f", priority))

        if (image != null) {
            xml.writeStartElement(IMAGE_NS, "image")
            writeElement(xml, IMAGE_NS, "loc", image.loc)
            image.caption?.let { writeElement(xml, IMAGE_NS, "caption", it) }
            image.title?.let { writeElement(xml, IMAGE_NS, "title", it) }
            xml.writeEndElement()
        }

        xml.writeEndElement()
    }

    private fun writeElement(
        xml: XMLStreamWriter,
        name: String,
        value: String,
    ) {
        xml.writeStartElement(name)
        xml.writeCharacters(value)
        xml.writeEndElement()
    }

    private fun writeElement(
        xml: XMLStreamWriter,
        ns: String,
        name: String,
        value: String,
    ) {
        xml.writeStartElement(ns, name)
        xml.writeCharacters(value)
        xml.writeEndElement()
    }

    private fun extractFragmentImage(fragment: Fragment): SitemapImage? {
        val imageUrl = extractImageUrl(fragment)
        return imageUrl?.let {
            SitemapImage(
                loc = it,
                caption = fragment.title,
                title = fragment.title,
            )
        }
    }

    private fun extractImageUrl(fragment: Fragment): String? {
        fragment.frontMatter["image"]?.let { return it.toString() }
        fragment.frontMatter["og:image"]?.let { return it.toString() }
        fragment.frontMatter["twitter:image"]?.let { return it.toString() }

        val imgTagPattern = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        imgTagPattern
            .find(fragment.preview)
            ?.groupValues
            ?.get(1)
            ?.let { return it }

        return null
    }

    companion object {
        private const val SITEMAP_NS = "http://www.sitemaps.org/schemas/sitemap/0.9"
        private const val IMAGE_NS = "http://www.google.com/schemas/sitemap-image/1.1"

        /** W3C Datetime (ISO 8601 date) — required by the sitemap protocol. */
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
