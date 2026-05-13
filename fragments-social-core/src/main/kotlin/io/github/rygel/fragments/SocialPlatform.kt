package io.github.rygel.fragments

/**
 * Supported social media platforms for content sharing.
 *
 * Each entry defines a [shareUrlTemplate] with `{title}` and `{url}` placeholders
 * that are resolved by [SocialShareGenerator.generateShareLinks] to produce
 * platform-specific share URLs.
 *
 * @property displayName Human-readable platform name used in UI labels.
 * @property shareUrlTemplate URL template with `{title}` and `{url}` placeholders.
 */
enum class SocialPlatform(
    val displayName: String,
    val shareUrlTemplate: String,
) {
    /** Twitter / X share via intent URL. */
    TWITTER("Twitter", "https://twitter.com/intent/tweet?text={title}&url={url}"),

    /** Facebook share via sharer dialog. */
    FACEBOOK("Facebook", "https://www.facebook.com/sharer/sharer.php?u={url}"),

    /** LinkedIn share via offsite sharing endpoint. */
    LINKEDIN("LinkedIn", "https://www.linkedin.com/sharing/share-offsite/?url={url}"),

    /** Reddit submit link. */
    REDDIT("Reddit", "https://reddit.com/submit?url={url}&title={title}"),

    /** WhatsApp share via deep link. */
    WHATSAPP("WhatsApp", "https://wa.me/?text={title}%20{url}"),

    /** Email share via mailto URI. */
    EMAIL("Email", "mailto:?subject={title}&body={url}"),
}
