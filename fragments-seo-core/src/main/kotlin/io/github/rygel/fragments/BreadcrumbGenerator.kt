package io.github.rygel.fragments

/**
 * A single breadcrumb entry with a display name and absolute URL.
 *
 * @property name Human-readable label (e.g. "Home", "Blog", "Hello World").
 * @property url Absolute URL for this breadcrumb level.
 */
data class Breadcrumb(
    val name: String,
    val url: String,
)

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
    fun generate(
        siteUrl: String,
        crumbs: List<Breadcrumb>,
    ): String {
        val items =
            crumbs.mapIndexed { index, crumb ->
                buildString {
                    append("{")
                    append("\"@type\":\"ListItem\",")
                    append("\"position\":${index + 1},")
                    append("\"name\":\"${TextEscapeUtils.escapeJson(crumb.name)}\",")
                    append("\"item\":\"${TextEscapeUtils.escapeJson(crumb.url)}\"")
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


}
