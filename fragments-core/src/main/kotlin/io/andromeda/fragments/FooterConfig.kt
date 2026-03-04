package io.andromeda.fragments

data class FooterConfig(
    val copyrightText: String,
    val year: Int,
    val poweredBy: PoweredByLink?
) {
    val fullCopyrightText: String
        get() = "$copyrightText $year. $poweredByText"

    val poweredByText: String
        get() = if (poweredBy != null) "Powered by ${poweredBy.name}" else ""
}

data class PoweredByLink(
    val name: String,
    val url: String,
    val linkText: String? = null
) {
    val displayText: String
        get() = linkText ?: name
}

object FooterGenerator {
    fun generate(
        copyrightText: String = "©",
        year: Int = java.time.Year.now().value,
        poweredByName: String = "Fragments4k",
        poweredByUrl: String = "https://github.com/rygel/fragments4k",
        poweredByLinkText: String? = null
    ): FooterConfig {
        return FooterConfig(
            copyrightText = copyrightText,
            year = year,
            poweredBy = if (poweredByUrl.isNotEmpty()) {
                PoweredByLink(
                    name = poweredByName,
                    url = poweredByUrl,
                    linkText = poweredByLinkText
                )
            } else null
        )
    }
}
