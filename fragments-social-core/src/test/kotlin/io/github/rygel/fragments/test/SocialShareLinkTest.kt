package io.github.rygel.fragments.test

import io.github.rygel.fragments.SocialPlatform
import io.github.rygel.fragments.SocialShareGenerator
import io.github.rygel.fragments.SocialShareLink
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SocialShareLinkTest {
    @Test
    fun testSocialShareGeneration() {
        val shareLinks = SocialShareGenerator.generateShareLinks(
            title = "Test Post Title",
            url = "https://example.com/blog/test-post"
        )

        assertEquals(SocialPlatform.entries.size, shareLinks.size)
    }

    @Test
    fun testTwitterShareLink() {
        val shareLinks = SocialShareGenerator.generateShareLinks(
            title = "Test Post",
            url = "https://example.com/blog/test",
            platforms = listOf(SocialPlatform.TWITTER)
        )

        assertEquals(1, shareLinks.size)
        val twitterLink = shareLinks[0]
        assertEquals(SocialPlatform.TWITTER, twitterLink.platform)
        assertEquals("Twitter", twitterLink.title)
        assertTrue(twitterLink.url.startsWith("https://twitter.com/intent/tweet?"))
        assertTrue(twitterLink.url.contains("text=Test%20Post"))
        assertTrue(twitterLink.url.contains("url=https%3A%2F%2Fexample.com%2Fblog%2Ftest"))
    }

    @Test
    fun testFacebookShareLink() {
        val shareLinks = SocialShareGenerator.generateShareLinks(
            title = "Test Post",
            url = "https://example.com/blog/test",
            platforms = listOf(SocialPlatform.FACEBOOK)
        )

        assertEquals(1, shareLinks.size)
        val facebookLink = shareLinks[0]
        assertEquals(SocialPlatform.FACEBOOK, facebookLink.platform)
        assertEquals("Facebook", facebookLink.title)
        assertTrue(facebookLink.url.startsWith("https://www.facebook.com/sharer/sharer.php?"))
        assertTrue(facebookLink.url.contains("u=https%3A%2F%2Fexample.com%2Fblog%2Ftest"))
    }

    @Test
    fun testLinkedInShareLink() {
        val shareLinks = SocialShareGenerator.generateShareLinks(
            title = "Test Post",
            url = "https://example.com/blog/test",
            platforms = listOf(SocialPlatform.LINKEDIN)
        )

        assertEquals(1, shareLinks.size)
        val linkedInLink = shareLinks[0]
        assertEquals(SocialPlatform.LINKEDIN, linkedInLink.platform)
        assertEquals("LinkedIn", linkedInLink.title)
        assertTrue(linkedInLink.url.startsWith("https://www.linkedin.com/sharing/share-offsite/?"))
        assertTrue(linkedInLink.url.contains("url=https%3A%2F%2Fexample.com%2Fblog%2Ftest"))
    }

    @Test
    fun testRedditShareLink() {
        val shareLinks = SocialShareGenerator.generateShareLinks(
            title = "Test Post",
            url = "https://example.com/blog/test",
            platforms = listOf(SocialPlatform.REDDIT)
        )

        assertEquals(1, shareLinks.size)
        val redditLink = shareLinks[0]
        assertEquals(SocialPlatform.REDDIT, redditLink.platform)
        assertEquals("Reddit", redditLink.title)
        assertTrue(redditLink.url.startsWith("https://reddit.com/submit?"))
        assertTrue(redditLink.url.contains("url=https%3A%2F%2Fexample.com%2Fblog%2Ftest"))
        assertTrue(redditLink.url.contains("title=Test%20Post"))
    }

    @Test
    fun testWhatsAppShareLink() {
        val shareLinks = SocialShareGenerator.generateShareLinks(
            title = "Test Post",
            url = "https://example.com/blog/test",
            platforms = listOf(SocialPlatform.WHATSAPP)
        )

        assertEquals(1, shareLinks.size)
        val whatsappLink = shareLinks[0]
        assertEquals(SocialPlatform.WHATSAPP, whatsappLink.platform)
        assertEquals("WhatsApp", whatsappLink.title)
        assertTrue(whatsappLink.url.startsWith("https://wa.me/?"))
        assertTrue(whatsappLink.url.contains("text=Test%20Post%20https%3A%2F%2Fexample.com%2Fblog%2Ftest"))
    }

    @Test
    fun testEmailShareLink() {
        val shareLinks = SocialShareGenerator.generateShareLinks(
            title = "Test Post",
            url = "https://example.com/blog/test",
            platforms = listOf(SocialPlatform.EMAIL)
        )

        assertEquals(1, shareLinks.size)
        val emailLink = shareLinks[0]
        assertEquals(SocialPlatform.EMAIL, emailLink.platform)
        assertEquals("Email", emailLink.title)
        assertTrue(emailLink.url.startsWith("mailto:?"))
        assertTrue(emailLink.url.contains("subject=Test%20Post"))
        assertTrue(emailLink.url.contains("body=https%3A%2F%2Fexample.com%2Fblog%2Ftest"))
    }

    @Test
    fun testAllPlatformsShareLink() {
        val shareLinks = SocialShareGenerator.generateShareLinks(
            title = "Test Post",
            url = "https://example.com/blog/test"
        )

        assertTrue(shareLinks.any { it.platform == SocialPlatform.TWITTER })
        assertTrue(shareLinks.any { it.platform == SocialPlatform.FACEBOOK })
        assertTrue(shareLinks.any { it.platform == SocialPlatform.LINKEDIN })
        assertTrue(shareLinks.any { it.platform == SocialPlatform.REDDIT })
        assertTrue(shareLinks.any { it.platform == SocialPlatform.WHATSAPP })
        assertTrue(shareLinks.any { it.platform == SocialPlatform.EMAIL })
    }

    @Test
    fun testUrlEncoding() {
        val shareLinks = SocialShareGenerator.generateShareLinks(
            title = "Test Post with Spaces & Special! Chars@#",
            url = "https://example.com/blog/test?param=value&other=123"
        )

        val twitterLink = shareLinks.find { it.platform == SocialPlatform.TWITTER }
        assertNotNull(twitterLink)
        assertTrue(twitterLink?.url?.contains("text=Test%20Post%20with%20Spaces%20%26%20Special%21%20Chars%40%23") == true)
        assertTrue(twitterLink?.url?.contains("url=https%3A%2F%2Fexample.com%2Fblog%2Ftest%3Fparam%3Dvalue%26other%3D123") == true)
    }

    @Test
    fun testEmptyTitleAndUrl() {
        val shareLinks = SocialShareGenerator.generateShareLinks(
            title = "",
            url = ""
        )

        val twitterLink = shareLinks.find { it.platform == SocialPlatform.TWITTER }
        assertNotNull(twitterLink)
        assertTrue(twitterLink?.url?.contains("text=") == true)
        assertTrue(twitterLink?.url?.contains("url=") == true)
    }

    @Test
    fun testMultiplePlatformsSelection() {
        val selectedPlatforms = listOf(
            SocialPlatform.TWITTER,
            SocialPlatform.FACEBOOK,
            SocialPlatform.LINKEDIN
        )

        val shareLinks = SocialShareGenerator.generateShareLinks(
            title = "Test Post",
            url = "https://example.com/blog/test",
            platforms = selectedPlatforms
        )

        assertEquals(3, shareLinks.size)
        assertEquals(SocialPlatform.TWITTER, shareLinks[0].platform)
        assertEquals(SocialPlatform.FACEBOOK, shareLinks[1].platform)
        assertEquals(SocialPlatform.LINKEDIN, shareLinks[2].platform)
    }

    @Test
    fun testPlatformDisplayName() {
        assertEquals("Twitter", SocialPlatform.TWITTER.displayName)
        assertEquals("Facebook", SocialPlatform.FACEBOOK.displayName)
        assertEquals("LinkedIn", SocialPlatform.LINKEDIN.displayName)
        assertEquals("Reddit", SocialPlatform.REDDIT.displayName)
        assertEquals("WhatsApp", SocialPlatform.WHATSAPP.displayName)
        assertEquals("Email", SocialPlatform.EMAIL.displayName)
    }

    @Test
    fun testPlatformUrlTemplates() {
        assertTrue(SocialPlatform.TWITTER.shareUrlTemplate.contains("{title}"))
        assertTrue(SocialPlatform.TWITTER.shareUrlTemplate.contains("{url}"))
        assertTrue(SocialPlatform.FACEBOOK.shareUrlTemplate.contains("{url}"))
        assertFalse(SocialPlatform.FACEBOOK.shareUrlTemplate.contains("{title}"))
    }
}
