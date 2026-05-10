package io.github.rygel.fragments.adapter

/** Configuration for the site footer, including copyright notice, social links, and powered-by credit. */
data class FooterConfig(
    val copyrightText: String,
    val year: Int,
    val poweredByName: String,
    val poweredByUrl: String,
    val githubUrl: String = "",
    val discordUrl: String = "",
    val twitterUrl: String = "",
    val substackUrl: String = "",
)
