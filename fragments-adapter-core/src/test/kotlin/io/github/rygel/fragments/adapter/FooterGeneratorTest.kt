package io.github.rygel.fragments.adapter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Year

class FooterGeneratorTest {
    @Test
    fun testDefaultFooter() {
        val config = FooterGenerator.generate()

        assertEquals("\u00a9", config.copyrightText)
        assertEquals(Year.now().value, config.year)
        assertEquals("Fragments4k", config.poweredByName)
        assertEquals("https://github.com/rygel/fragments4k", config.poweredByUrl)
        assertEquals("", config.githubUrl)
        assertEquals("", config.discordUrl)
        assertEquals("", config.twitterUrl)
        assertEquals("", config.substackUrl)
    }

    @Test
    fun testCustomFooter() {
        val config =
            FooterGenerator.generate(
                copyrightText = "All rights reserved",
                year = 2024,
                poweredByName = "CustomEngine",
                poweredByUrl = "https://example.com",
                githubUrl = "https://github.com/example",
                discordUrl = "https://discord.gg/example",
                twitterUrl = "https://twitter.com/example",
                substackUrl = "https://substack.com/example",
            )

        assertEquals("All rights reserved", config.copyrightText)
        assertEquals(2024, config.year)
        assertEquals("CustomEngine", config.poweredByName)
        assertEquals("https://example.com", config.poweredByUrl)
        assertEquals("https://github.com/example", config.githubUrl)
        assertEquals("https://discord.gg/example", config.discordUrl)
        assertEquals("https://twitter.com/example", config.twitterUrl)
        assertEquals("https://substack.com/example", config.substackUrl)
        assertTrue(config.year == 2024)
    }
}
