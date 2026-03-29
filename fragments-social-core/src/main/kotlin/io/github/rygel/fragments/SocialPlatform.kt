package io.github.rygel.fragments

enum class SocialPlatform(
    val displayName: String,
    val shareUrlTemplate: String
) {
    TWITTER("Twitter", "https://twitter.com/intent/tweet?text={title}&url={url}"),
    FACEBOOK("Facebook", "https://www.facebook.com/sharer/sharer.php?u={url}"),
    LINKEDIN("LinkedIn", "https://www.linkedin.com/sharing/share-offsite/?url={url}"),
    REDDIT("Reddit", "https://reddit.com/submit?url={url}&title={title}"),
    WHATSAPP("WhatsApp", "https://wa.me/?text={title}%20{url}"),
    EMAIL("Email", "mailto:?subject={title}&body={url}")
}
