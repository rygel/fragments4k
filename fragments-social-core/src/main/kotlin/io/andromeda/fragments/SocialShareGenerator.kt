package io.github.rygel.fragments

object SocialShareGenerator {
    fun generateShareLinks(
        title: String,
        url: String,
        platforms: List<SocialPlatform> = SocialPlatform.entries
    ): List<SocialShareLink> {
        return platforms.map { platform ->
            val shareUrl = platform.shareUrlTemplate
                .replace("{title}", title.encodeURLParameter())
                .replace("{url}", url.encodeURLParameter())
            
            SocialShareLink(platform, shareUrl, platform.displayName)
        }
    }
    
    private fun String.encodeURLParameter(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
            .replace("+", "%20")
    }
}
