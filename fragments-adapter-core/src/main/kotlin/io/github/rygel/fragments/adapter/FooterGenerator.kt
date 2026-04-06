package io.github.rygel.fragments.adapter

object FooterGenerator {
    fun generate(
        copyrightText: String = "\u00a9",
        year: Int =
            java.time.Year
                .now()
                .value,
        poweredByName: String = "Fragments4k",
        poweredByUrl: String = "https://github.com/rygel/fragments4k",
        githubUrl: String = "",
        discordUrl: String = "",
        twitterUrl: String = "",
        substackUrl: String = "",
    ): FooterConfig =
        FooterConfig(
            copyrightText = copyrightText,
            year = year,
            poweredByName = poweredByName,
            poweredByUrl = poweredByUrl,
            githubUrl = githubUrl,
            discordUrl = discordUrl,
            twitterUrl = twitterUrl,
            substackUrl = substackUrl,
        )
}
