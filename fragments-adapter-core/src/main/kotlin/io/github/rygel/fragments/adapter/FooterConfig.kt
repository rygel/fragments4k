package io.github.rygel.fragments.adapter

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
