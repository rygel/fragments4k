package io.github.rygel.fragments

fun Fragment.seoMetadata(
    siteUrl: String,
    siteName: String? = null,
    pagePath: String? = null,
    author: String? = null,
    imageUrl: String? = null,
    ogType: String = "article",
    authorUrl: String? = null,
    authorSocialLinks: List<String> = emptyList(),
): SeoMetadata {
    val canonicalUrl =
        if (pagePath != null) {
            "$siteUrl/$pagePath"
        } else {
            "$siteUrl${this.url}"
        }

    val description =
        this.previewTextOnly.take(160).let {
            if (it.length >= 160) "$it..." else it
        }

    val resolvedAuthor = author ?: this.author
    val resolvedImageUrl = imageUrl ?: this.image?.let { "$siteUrl$it" }

    return SeoMetadata(
        title = this.title,
        description = description,
        canonicalUrl = canonicalUrl,
        ogTitle = this.title,
        ogDescription = description,
        ogImage = resolvedImageUrl,
        ogType = ogType,
        ogSiteName = siteName,
        twitterCard = "summary_large_image",
        twitterTitle = this.title,
        twitterDescription = description,
        twitterImage = resolvedImageUrl,
        keywords = this.tags,
        author = resolvedAuthor,
        authorUrl = authorUrl,
        authorSocialLinks = authorSocialLinks,
        publishedDate = this.date?.toString(),
        locale = this.language.replace("-", "_"),
    )
}

fun Fragment.breadcrumbs(siteUrl: String): String {
    val normalizedSiteUrl = siteUrl.trimEnd('/')
    val path = this.url.trimStart('/')
    val segments = path.split("/").filter { it.isNotEmpty() }

    val crumbs = mutableListOf(Breadcrumb("Home", "$normalizedSiteUrl/"))

    if (segments.isNotEmpty()) {
        var pathSoFar = ""
        for (i in 0 until segments.size - 1) {
            val segment = segments[i]
            pathSoFar += "/$segment"
            if (segment.all { it.isDigit() }) continue
            crumbs.add(Breadcrumb(TextEscapeUtils.titleCase(segment), "$normalizedSiteUrl$pathSoFar"))
        }

        crumbs.add(Breadcrumb(this.title, "$normalizedSiteUrl/$path"))
    }

    return BreadcrumbGenerator.generate(normalizedSiteUrl, crumbs)
}

fun Fragment.faqSchema(): String {
    if (this.faq.isEmpty()) return ""
    return FaqSchemaGenerator.generate(this.faq.map { FaqItem(it.question, it.answer) })
}

fun Author.personSchema(siteUrl: String): String {
    val socialUrls = this.allSocialLinks.map { it.second }
    return PersonSchemaGenerator.generate(
        name = this.name,
        siteUrl = siteUrl,
        authorSlug = this.slug,
        bio = this.bio,
        image = this.avatar,
        url = this.website,
        socialLinks = socialUrls,
    )
}

