package io.github.rygel.fragments

/**
 * A resolved share link for a single social platform.
 *
 * Produced by [SocialShareGenerator.generateShareLinks] with all placeholder
 * tokens substituted for the actual title and URL values.
 *
 * @property platform The target [SocialPlatform].
 * @property url Fully resolved share URL with encoded title and page URL.
 * @property title Display name for the share button (from [SocialPlatform.displayName]).
 */
data class SocialShareLink(
    val platform: SocialPlatform,
    val url: String,
    val title: String,
)
