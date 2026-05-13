package io.github.rygel.fragments

/**
 * SEO metadata for a fragment or page.
 * Provides structured data for search engines and social media sharing.
 */
data class SeoMetadata(
    val title: String,
    val description: String,
    val canonicalUrl: String,
    val ogTitle: String? = null,
    val ogDescription: String? = null,
    val ogImage: String? = null,
    val ogType: String = "article",
    val ogSiteName: String? = null,
    val twitterCard: String = "summary_large_image",
    val twitterTitle: String? = null,
    val twitterDescription: String? = null,
    val twitterImage: String? = null,
    val keywords: List<String> = emptyList(),
    val author: String? = null,
    val authorUrl: String? = null,
    val authorSocialLinks: List<String> = emptyList(),
    val publishedDate: String? = null,
    val modifiedDate: String? = null,
    val locale: String = "en_US",
    val robots: String = "index, follow",
    val additionalMetaTags: List<String> = emptyList(),
    val additionalJsonLd: String = "",
) {
    fun generateOpenGraphTags(): List<String> {
        val tags = mutableListOf<String>()

        tags.add("""<meta property="og:title" content="${TextEscapeUtils.escapeHtml(ogTitle ?: title)}">""")
        tags.add("""<meta property="og:description" content="${TextEscapeUtils.escapeHtml(ogDescription ?: description)}">""")
        tags.add("""<meta property="og:url" content="$canonicalUrl">""")
        tags.add("""<meta property="og:type" content="$ogType">""")

        ogSiteName?.let { tags.add("""<meta property="og:site_name" content="${TextEscapeUtils.escapeHtml(it)}">""") }
        ogImage?.let { tags.add("""<meta property="og:image" content="$it">""") }

        if (ogType == "article") {
            author?.let { tags.add("""<meta property="article:author" content="${TextEscapeUtils.escapeHtml(it)}">""") }
            publishedDate?.let { tags.add("""<meta property="article:published_time" content="$it">""") }
            modifiedDate?.let { tags.add("""<meta property="article:modified_time" content="$it">""") }
            keywords.forEach { tag ->
                tags.add("""<meta property="article:tag" content="${TextEscapeUtils.escapeHtml(tag)}">""")
            }
        }

        tags.add("""<meta property="og:locale" content="$locale">""")

        return tags
    }

    fun generateTwitterCardTags(): List<String> {
        val tags = mutableListOf<String>()

        tags.add("""<meta name="twitter:card" content="$twitterCard">""")
        tags.add("""<meta name="twitter:title" content="${TextEscapeUtils.escapeHtml(twitterTitle ?: title)}">""")
        tags.add("""<meta name="twitter:description" content="${TextEscapeUtils.escapeHtml(twitterDescription ?: description)}">""")

        (twitterImage ?: ogImage)?.let { tags.add("""<meta name="twitter:image" content="$it">""") }

        return tags
    }

    fun generateStandardMetaTags(): List<String> {
        val tags = mutableListOf<String>()

        tags.add("""<meta name="description" content="${TextEscapeUtils.escapeHtml(description)}">""")

        if (keywords.isNotEmpty()) {
            tags.add("""<meta name="keywords" content="${keywords.joinToString(", ") { TextEscapeUtils.escapeHtml(it) }}">""")
        }

        author?.let { tags.add("""<meta name="author" content="${TextEscapeUtils.escapeHtml(it)}">""") }

        tags.add("""<meta name="robots" content="$robots">""")
        tags.add("""<link rel="canonical" href="$canonicalUrl">""")

        return tags
    }

    fun generateJsonLd(): String {
        val jsonBuilder = StringBuilder()
        jsonBuilder.append(
            """
            {
                "@context": "https://schema.org",
                "@type": "${if (ogType == "article") "BlogPosting" else "WebPage"}",
                "headline": "${TextEscapeUtils.escapeJson(title)}",
                "description": "${TextEscapeUtils.escapeJson(description)}",
                "url": "$canonicalUrl"
            """.trimIndent(),
        )

        author?.let {
            val escapedAuthor = TextEscapeUtils.escapeJson(it)
            jsonBuilder.append(
                ",\n                \"author\": {\n                    \"@type\": \"Person\",\n                    \"name\": \"$escapedAuthor\"",
            )
            authorUrl?.let { url ->
                jsonBuilder.append(",\n                    \"url\": \"${TextEscapeUtils.escapeJson(url)}\"")
            }
            if (authorSocialLinks.isNotEmpty()) {
                val linksJson = authorSocialLinks.joinToString(", ") { link -> "\"${TextEscapeUtils.escapeJson(link)}\"" }
                jsonBuilder.append(",\n                    \"sameAs\": [$linksJson]")
            }
            jsonBuilder.append("\n                }")
        }

        publishedDate?.let {
            jsonBuilder.append(",\n                \"datePublished\": \"$it\"")
        }

        modifiedDate?.let {
            jsonBuilder.append(",\n                \"dateModified\": \"$it\"")
        }

        ogImage?.let {
            jsonBuilder.append(",\n                \"image\": \"$it\"")
        }

        if (keywords.isNotEmpty()) {
            jsonBuilder.append(",\n                \"keywords\": \"${keywords.joinToString(", ") { TextEscapeUtils.escapeJson(it) }}\"")
        }

        jsonBuilder.append("\n            }")

        return jsonBuilder.toString()
    }

    fun generateAllMetaTags(): String =
        buildString {
            appendLine("<!-- Standard Meta Tags -->")
            generateStandardMetaTags().forEach { appendLine(it) }

            appendLine("<!-- Open Graph Meta Tags -->")
            generateOpenGraphTags().forEach { appendLine(it) }

            appendLine("<!-- Twitter Card Meta Tags -->")
            generateTwitterCardTags().forEach { appendLine(it) }

            if (additionalMetaTags.isNotEmpty()) {
                appendLine("<!-- Additional Meta Tags -->")
                additionalMetaTags.forEach { appendLine(it) }
            }

            appendLine("<!-- JSON-LD Structured Data -->")
            appendLine("""<script type="application/ld+json">""")
            appendLine(generateJsonLd())
            appendLine("""</script>""")

            if (additionalJsonLd.isNotBlank()) {
                appendLine("""<script type="application/ld+json">""")
                appendLine(additionalJsonLd.trim())
                appendLine("""</script>""")
            }
        }

    /**
     * Generates a BreadcrumbList JSON-LD string from an explicit list of crumbs.
     *
     * Convenience wrapper around [BreadcrumbGenerator.generate].
     */
    fun generateBreadcrumbJsonLd(
        siteUrl: String,
        crumbs: List<Breadcrumb>,
    ): String = BreadcrumbGenerator.generate(siteUrl, crumbs)

    companion object {
        fun forPage(
            title: String,
            description: String,
            siteUrl: String,
            pagePath: String,
            siteName: String? = null,
            imageUrl: String? = null,
        ): SeoMetadata =
            SeoMetadata(
                title = title,
                description = description.take(160),
                canonicalUrl = "$siteUrl/$pagePath",
                ogTitle = title,
                ogDescription = description.take(160),
                ogImage = imageUrl,
                ogType = "website",
                ogSiteName = siteName,
                twitterCard = "summary",
                twitterTitle = title,
                twitterDescription = description.take(160),
                twitterImage = imageUrl,
            )

        fun forPageWithUrl(
            title: String,
            description: String,
            canonicalUrl: String,
            locale: String = "en_US",
            siteName: String? = null,
            imageUrl: String? = null,
        ): SeoMetadata =
            SeoMetadata(
                title = title,
                description = description.take(160),
                canonicalUrl = canonicalUrl,
                ogTitle = title,
                ogDescription = description.take(160),
                ogImage = imageUrl,
                ogType = "website",
                ogSiteName = siteName,
                twitterCard = "summary",
                twitterTitle = title,
                twitterDescription = description.take(160),
                twitterImage = imageUrl,
                locale = locale,
            )
    }
}
