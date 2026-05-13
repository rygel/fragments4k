package io.github.rygel.fragments

/**
 * A question/answer pair for FAQ structured data generation.
 */
data class FaqItem(
    val question: String,
    val answer: String,
)

/**
 * Generates [FAQPage](https://schema.org/FAQPage) JSON-LD structured data
 * from [FaqItem] lists.
 *
 * The output is a self-contained JSON-LD object suitable for embedding in a
 * `<script type="application/ld+json">` block. Search engines use this markup
 * to display rich FAQ snippets in search results.
 *
 * Usage:
 * ```kotlin
 * val jsonLd = FaqSchemaGenerator.generate(listOf(FaqItem("Q?", "A.")))
 * ```
 */
object FaqSchemaGenerator {
    /**
     * Generates FAQPage JSON-LD from a list of [FaqItem] objects.
     *
     * @param faqItems The FAQ question/answer pairs.
     * @return A JSON-LD string, or an empty string if [faqItems] is empty.
     */
    fun generate(faqItems: List<FaqItem>): String {
        if (faqItems.isEmpty()) return ""

        return buildString {
            append("{\n")
            append("""  "@context": "https://schema.org",""")
            append("\n")
            append("""  "@type": "FAQPage",""")
            append("\n")
            append("""  "mainEntity": [""")
            append("\n")
            faqItems.forEachIndexed { index, entry ->
                append("    {\n")
                append("""      "@type": "Question",""")
                append("\n")
                append("""      "name": "${TextEscapeUtils.escapeJson(entry.question)}",""")
                append("\n")
                append("      \"acceptedAnswer\": {\n")
                append("""        "@type": "Answer",""")
                append("\n")
                append("""        "text": "${TextEscapeUtils.escapeJson(entry.answer)}"""")
                append("\n")
                append("      }\n")
                append("    }")
                if (index < faqItems.size - 1) append(",")
                append("\n")
            }
            append("  ]\n")
            append("}")
        }
    }


}
