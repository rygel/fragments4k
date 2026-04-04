package io.github.rygel.fragments

/**
 * Generates [FAQPage](https://schema.org/FAQPage) JSON-LD structured data
 * from [FaqEntry] lists.
 *
 * The output is a self-contained JSON-LD object suitable for embedding in a
 * `<script type="application/ld+json">` block. Search engines use this markup
 * to display rich FAQ snippets in search results.
 *
 * Usage:
 * ```kotlin
 * val jsonLd = FaqSchemaGenerator.fromFragment(fragment)
 * // or
 * val jsonLd = FaqSchemaGenerator.generate(listOf(FaqEntry("Q?", "A.")))
 * ```
 */
object FaqSchemaGenerator {

    /**
     * Generates FAQPage JSON-LD from a list of [FaqEntry] objects.
     *
     * @param faqEntries The FAQ question/answer pairs.
     * @return A JSON-LD string, or an empty string if [faqEntries] is empty.
     */
    fun generate(faqEntries: List<FaqEntry>): String {
        if (faqEntries.isEmpty()) return ""

        return buildString {
            append("{\n")
            append("""  "@context": "https://schema.org",""")
            append("\n")
            append("""  "@type": "FAQPage",""")
            append("\n")
            append("""  "mainEntity": [""")
            append("\n")
            faqEntries.forEachIndexed { index, entry ->
                append("    {\n")
                append("""      "@type": "Question",""")
                append("\n")
                append("""      "name": "${escapeJson(entry.question)}",""")
                append("\n")
                append("      \"acceptedAnswer\": {\n")
                append("""        "@type": "Answer",""")
                append("\n")
                append("""        "text": "${escapeJson(entry.answer)}"""")
                append("\n")
                append("      }\n")
                append("    }")
                if (index < faqEntries.size - 1) append(",")
                append("\n")
            }
            append("  ]\n")
            append("}")
        }
    }

    /**
     * Convenience method that extracts FAQ entries from a [Fragment] and
     * generates the JSON-LD.
     *
     * @param fragment The fragment whose [Fragment.faq] entries to use.
     * @return A JSON-LD string, or an empty string if the fragment has no FAQ entries.
     */
    fun fromFragment(fragment: Fragment): String {
        return generate(fragment.faq)
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
