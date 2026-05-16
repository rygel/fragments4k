package io.github.rygel.fragments

/**
 * Generates platform-specific social sharing links for content.
 *
 * Replaces `{title}` and `{url}` placeholders in each [SocialPlatform.shareUrlTemplate]
 * with URL-encoded values to produce ready-to-use share URLs.
 */
object SocialShareGenerator {
    /**
     * Builds a [SocialShareLink] for each requested platform.
     *
     * @param title The page or content title to include in the share payload.
     * @param url The canonical URL of the page being shared.
     * @param platforms Platforms to generate links for; defaults to all entries.
     * @return One [SocialShareLink] per platform with the share URL fully resolved.
     */
    fun generateShareLinks(
        title: String,
        url: String,
        platforms: List<SocialPlatform> = SocialPlatform.entries,
    ): List<SocialShareLink> =
        platforms.map { platform ->
            val shareUrl =
                platform.shareUrlTemplate
                    .replace("{title}", title.encodeURLParameter())
                    .replace("{url}", url.encodeURLParameter())

            SocialShareLink(platform, shareUrl, platform.displayName)
        }

    /**
     * URL-encodes this string for use in query parameters, converting `+` to `%20`
     * for space encoding consistency.
     */
    private fun String.encodeURLParameter(): String =
        java.net.URLEncoder
            .encode(this, "UTF-8")
            .replace("+", "%20")
}
