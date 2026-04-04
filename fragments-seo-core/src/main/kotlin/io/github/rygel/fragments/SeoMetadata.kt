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
    val publishedDate: String? = null,
    val modifiedDate: String? = null,
    val locale: String = "en_US",
    val robots: String = "index, follow"
) {
    fun generateOpenGraphTags(): List<String> {
        val tags = mutableListOf<String>()
        
        tags.add("""<meta property="og:title" content="${escapeHtml(ogTitle ?: title)}">""")
        tags.add("""<meta property="og:description" content="${escapeHtml(ogDescription ?: description)}">""")
        tags.add("""<meta property="og:url" content="$canonicalUrl">""")
        tags.add("""<meta property="og:type" content="$ogType">""")
        
        ogSiteName?.let { tags.add("""<meta property="og:site_name" content="${escapeHtml(it)}">""") }
        ogImage?.let { tags.add("""<meta property="og:image" content="$it">""") }
        
        if (ogType == "article") {
            author?.let { tags.add("""<meta property="article:author" content="${escapeHtml(it)}">""") }
            publishedDate?.let { tags.add("""<meta property="article:published_time" content="$it">""") }
            modifiedDate?.let { tags.add("""<meta property="article:modified_time" content="$it">""") }
            keywords.forEach { tag ->
                tags.add("""<meta property="article:tag" content="${escapeHtml(tag)}">""")
            }
        }
        
        tags.add("""<meta property="og:locale" content="$locale">""")
        
        return tags
    }
    
    fun generateTwitterCardTags(): List<String> {
        val tags = mutableListOf<String>()
        
        tags.add("""<meta name="twitter:card" content="$twitterCard">""")
        tags.add("""<meta name="twitter:title" content="${escapeHtml(twitterTitle ?: title)}">""")
        tags.add("""<meta name="twitter:description" content="${escapeHtml(twitterDescription ?: description)}">""")
        
        twitterImage?.let { tags.add("""<meta name="twitter:image" content="$it">""") }
        
        return tags
    }
    
    fun generateStandardMetaTags(): List<String> {
        val tags = mutableListOf<String>()
        
        tags.add("""<meta name="description" content="${escapeHtml(description)}">""")
        
        if (keywords.isNotEmpty()) {
            tags.add("""<meta name="keywords" content="${keywords.joinToString(", ") { escapeHtml(it) }}">""")
        }
        
        author?.let { tags.add("""<meta name="author" content="${escapeHtml(it)}">""") }
        
        tags.add("""<meta name="robots" content="$robots">""")
        tags.add("""<link rel="canonical" href="$canonicalUrl">""")
        
        return tags
    }
    
    fun generateJsonLd(): String {
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("""
            {
                "@context": "https://schema.org",
                "@type": "${if (ogType == "article") "BlogPosting" else "WebPage"}",
                "headline": "${escapeJson(title)}",
                "description": "${escapeJson(description)}",
                "url": "$canonicalUrl"
        """.trimIndent())
        
        author?.let {
            jsonBuilder.append(",\n                \"author\": {\n                    \"@type\": \"Person\",\n                    \"name\": \"${escapeJson(it)}\"\n                }")
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
            jsonBuilder.append(",\n                \"keywords\": \"${keywords.joinToString(", ") { escapeJson(it) }}\"")
        }
        
        jsonBuilder.append("\n            }")
        
        return jsonBuilder.toString()
    }
    
    fun generateAllMetaTags(): String {
        return buildString {
            appendLine("<!-- Standard Meta Tags -->")
            generateStandardMetaTags().forEach { appendLine(it) }
            
            appendLine("<!-- Open Graph Meta Tags -->")
            generateOpenGraphTags().forEach { appendLine(it) }
            
            appendLine("<!-- Twitter Card Meta Tags -->")
            generateTwitterCardTags().forEach { appendLine(it) }
            
            appendLine("<!-- JSON-LD Structured Data -->")
            appendLine("""<script type="application/ld+json">""")
            appendLine(generateJsonLd())
            appendLine("""</script>""")
        }
    }
    
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
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
    
    companion object {
        fun fromFragment(
            fragment: Fragment,
            siteUrl: String,
            siteName: String? = null,
            pagePath: String? = null,
            author: String? = null,
            imageUrl: String? = null
        ): SeoMetadata {
            val canonicalUrl = if (pagePath != null) {
                "$siteUrl/$pagePath"
            } else {
                "$siteUrl/page/${fragment.slug}"
            }
            
            val description = fragment.previewTextOnly.take(160).let {
                if (it.length >= 160) "$it..." else it
            }

            val resolvedImageUrl = imageUrl ?: fragment.image?.let { "$siteUrl$it" }

            return SeoMetadata(
                title = fragment.title,
                description = description,
                canonicalUrl = canonicalUrl,
                ogTitle = fragment.title,
                ogDescription = description,
                ogImage = resolvedImageUrl,
                ogType = "article",
                ogSiteName = siteName,
                twitterCard = "summary_large_image",
                twitterTitle = fragment.title,
                twitterDescription = description,
                twitterImage = resolvedImageUrl,
                keywords = fragment.tags,
                author = author,
                publishedDate = fragment.date?.toString(),
                locale = fragment.language.replace("-", "_")
            )
        }
        
        fun forPage(
            title: String,
            description: String,
            siteUrl: String,
            pagePath: String,
            siteName: String? = null,
            imageUrl: String? = null
        ): SeoMetadata {
            return SeoMetadata(
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
                twitterImage = imageUrl
            )
        }
    }
}
