package io.github.rygel.fragments

/**
 * A single breadcrumb entry with a display name and absolute URL.
 *
 * @property name Human-readable label (e.g. "Home", "Blog", "Hello World").
 * @property url Absolute URL for this breadcrumb level.
 */
data class Breadcrumb(val name: String, val url: String)

/**
 * Generates [BreadcrumbList](https://schema.org/BreadcrumbList) JSON-LD
 * structured data from an explicit crumb list or from a [Fragment]'s URL path.
 *
 * The output is a standalone JSON object (not wrapped in a `<script>` tag)
 * so callers can embed it however they see fit.
 */
object BreadcrumbGenerator {

    /**
     * Builds a BreadcrumbList JSON-LD string from an explicit list of crumbs.
     *
     * @param siteUrl Base URL of the site (e.g. `"https://example.com"`), used
     *   only for documentation; each [Breadcrumb.url] must already be absolute.
     * @param crumbs Ordered breadcrumb trail from root to leaf.
     * @return JSON-LD string representing the BreadcrumbList.
     */
    fun generate(siteUrl: String, crumbs: List<Breadcrumb>): String {
        val items = crumbs.mapIndexed { index, crumb ->
            buildString {
                append("{")
                append("\"@type\":\"ListItem\",")
                append("\"position\":${index + 1},")
                append("\"name\":\"${escapeJson(crumb.name)}\",")
                append("\"item\":\"${escapeJson(crumb.url)}\"")
                append("}")
            }
        }

        return buildString {
            append("{")
            append("\"@context\":\"https://schema.org\",")
            append("\"@type\":\"BreadcrumbList\",")
            append("\"itemListElement\":[")
            append(items.joinToString(","))
            append("]")
            append("}")
        }
    }

    /**
     * Auto-generates breadcrumbs from a [Fragment]'s [url][Fragment.url] path.
     *
     * Rules:
     * - The first crumb is always **Home** pointing at `siteUrl/`.
     * - Purely numeric path segments (years, months, days) are skipped so that
     *   `/blog/2026/03/hello-world` produces `Home > Blog > Hello World` rather
     *   than `Home > Blog > 2026 > 03 > Hello World`.
     * - The last crumb uses the fragment's [title][Fragment.title].
     * - Intermediate segments are title-cased from their path component
     *   (hyphens replaced with spaces).
     *
     * @param fragment The fragment to derive breadcrumbs from.
     * @param siteUrl Base URL of the site without a trailing slash.
     * @return JSON-LD string representing the BreadcrumbList.
     */
    fun fromFragment(fragment: Fragment, siteUrl: String): String {
        val normalizedSiteUrl = siteUrl.trimEnd('/')
        val path = fragment.url.trimStart('/')
        val segments = path.split("/").filter { it.isNotEmpty() }

        val crumbs = mutableListOf(Breadcrumb("Home", "$normalizedSiteUrl/"))

        if (segments.isNotEmpty()) {
            // Non-numeric intermediate segments (everything except the last)
            var pathSoFar = ""
            for (i in 0 until segments.size - 1) {
                val segment = segments[i]
                pathSoFar += "/$segment"
                if (segment.all { it.isDigit() }) continue
                crumbs.add(Breadcrumb(titleCase(segment), "$normalizedSiteUrl$pathSoFar"))
            }

            // Last segment always uses fragment title
            crumbs.add(Breadcrumb(fragment.title, "$normalizedSiteUrl/$path"))
        }

        return generate(normalizedSiteUrl, crumbs)
    }

    private fun titleCase(slug: String): String =
        slug.replace("-", " ").split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercaseChar() }
        }

    private fun escapeJson(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
}
