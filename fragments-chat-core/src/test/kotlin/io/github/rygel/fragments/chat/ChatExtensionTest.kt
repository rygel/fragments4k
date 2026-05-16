package io.github.rygel.fragments.chat

import io.github.rygel.fragments.MarkdownParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatExtensionTest {
    private val parser = MarkdownParser(extraExtensions = listOf(ChatExtension.create()))

    // -------------------------------------------------------------------------
    // Basic rendering
    // -------------------------------------------------------------------------

    @Test
    fun testChatBlockRendersContainerDiv() {
        val result = render("```chat\nUser: Hello\nAssistant: Hi\n```")
        assertTrue(result.contains("class=\"chat-container\""), "Expected chat-container div")
    }

    @Test
    fun testUserSpeakerGetsUserModifierClass() {
        val result = render("```chat\nUser: Hello there\n```")
        assertTrue(result.contains("chat-message--user"), "Expected user modifier class")
    }

    @Test
    fun testNonUserSpeakerGetsAssistantModifierClass() {
        val result = render("```chat\nAssistant: Hello back\n```")
        assertTrue(result.contains("chat-message--assistant"), "Expected assistant modifier class")
    }

    @Test
    fun testSpeakerLabelRenderedInChatSpeakerSpan() {
        val result = render("```chat\nUser: Hi\n```")
        assertTrue(result.contains("<span class=\"chat-speaker\">User</span>"), "Expected speaker span")
    }

    @Test
    fun testMessageTextRenderedInChatTextSpan() {
        val result = render("```chat\nUser: Hello world\n```")
        assertTrue(result.contains("<span class=\"chat-text\">Hello world</span>"), "Expected text span")
    }

    @Test
    fun testMultipleTurnsProduceMultipleMessageDivs() {
        val result =
            render(
                """
                ```chat
                User: First message
                Assistant: Second message
                User: Third message
                ```
                """.trimIndent(),
            )
        assertEquals(3, Regex("chat-message--").findAll(result).count(), "Expected 3 chat-message divs")
    }

    // -------------------------------------------------------------------------
    // Speaker classification
    // -------------------------------------------------------------------------

    @Test
    fun testUserSpeakerNameIsCaseInsensitive() {
        val result = render("```chat\nUSER: Hello\n```")
        assertTrue(result.contains("chat-message--user"))
    }

    @Test
    fun testHumanIsRecognisedAsUserSpeakerByDefault() {
        val result = render("```chat\nHuman: Hey\n```")
        assertTrue(result.contains("chat-message--user"))
    }

    @Test
    fun testCustomUserSpeakerSetIsRespected() {
        val customParser =
            MarkdownParser(
                extraExtensions = listOf(ChatExtension.create(userSpeakers = setOf("alice"))),
            )
        val result = customParser.parse(wrap("```chat\nAlice: Hi\nBob: Hello\n```")).htmlContent
        assertTrue(result.contains("chat-message--user"), "Alice should be user")
        assertTrue(result.contains("chat-message--assistant"), "Bob should be assistant")
    }

    // -------------------------------------------------------------------------
    // Multi-line messages
    // -------------------------------------------------------------------------

    @Test
    fun testContinuationLinesAppendedToCurrentMessage() {
        val result =
            render(
                """
                ```chat
                User: This is a long message
                that continues on the next line
                Assistant: Got it
                ```
                """.trimIndent(),
            )
        assertTrue(result.contains("This is a long message that continues on the next line"))
    }

    @Test
    fun testBlankLinesInsideBlockAreIgnored() {
        val result =
            render(
                """
                ```chat
                User: Hello

                Assistant: Hi
                ```
                """.trimIndent(),
            )
        assertEquals(2, Regex("chat-message--").findAll(result).count())
    }

    // -------------------------------------------------------------------------
    // HTML escaping
    // -------------------------------------------------------------------------

    @Test
    fun testSpeakerAndTextAreHtmlEscaped() {
        val result = render("```chat\nUser: <script>alert(1)</script>\n```")
        assertFalse(result.contains("<script>"), "Raw script tag must not appear in output")
        assertTrue(result.contains("&lt;script&gt;"), "Script tag must be escaped")
    }

    // -------------------------------------------------------------------------
    // Fallthrough — non-chat fenced blocks must not be affected
    // -------------------------------------------------------------------------

    @Test
    fun testNonChatFencedBlockRenderedNormally() {
        val result = render("```kotlin\nfun hello() = println(\"Hello\")\n```")
        assertFalse(result.contains("chat-container"), "Non-chat block must not produce chat HTML")
        assertTrue(result.contains("<code"), "Non-chat block must produce code element")
    }

    @Test
    fun testPlainFencedBlockWithoutInfoStringRenderedNormally() {
        val result = render("```\nsome code\n```")
        assertFalse(result.contains("chat-container"))
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun testEmptyChatBlockProducesNoOutput() {
        val result = render("```chat\n```")
        assertFalse(result.contains("chat-container"), "Empty block should not produce container")
    }

    @Test
    fun testLineWithSpaceInSpeakerLabelTreatedAsContinuation() {
        // "Dr Smith: Hi" has a space — should NOT start a new turn; treated as continuation
        val result =
            render(
                """
                ```chat
                User: Hello
                Dr Smith: response with space in name
                ```
                """.trimIndent(),
            )
        // "Dr Smith: response..." is a continuation of User's message
        assertEquals(1, Regex("chat-message--").findAll(result).count())
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun render(markdown: String): String = parser.parse(markdown).htmlContent

    private fun wrap(markdown: String): String = markdown
}
