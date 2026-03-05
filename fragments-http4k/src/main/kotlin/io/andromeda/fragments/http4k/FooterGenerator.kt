package io.andromeda.fragments.http4k

object FooterGenerator {
    fun generate(
        copyrightText: String = "©",
        year: Int = java.time.Year.now().value,
        poweredByName: String = "Fragments4k",
        poweredByUrl: String = "https://github.com/rygel/fragments4k"
    ): FooterConfig {
        return FooterConfig(
            copyrightText = copyrightText,
            year = year,
            poweredByName = poweredByName,
            poweredByUrl = poweredByUrl
        )
    }
}
