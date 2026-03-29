package io.github.rygel.fragments.chat

import io.github.rygel.fragments.FileSystemFragmentRepository
import io.github.rygel.fragments.MarkdownParser
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

/**
 * End-to-end tests for [ChatExtension] wired through [FileSystemFragmentRepository].
 *
 * These tests exercise the full pipeline:
 *   `.md` file on disk → [MarkdownParser] with [ChatExtension] → [io.github.rygel.fragments.Fragment] → rendered HTML
 *
 * They verify that:
 * - chat blocks inside real content files are rendered to the expected HTML structure
 * - surrounding Markdown (headings, paragraphs, code blocks) is not affected
 * - fragment front matter (title, slug, tags, etc.) is preserved correctly
 * - multiple chat blocks in a single document all render
 * - documents without chat blocks continue to work as before (regression)
 */
class ChatExtensionE2ETest {

    @TempDir
    lateinit var contentDir: Path

    private lateinit var repository: FileSystemFragmentRepository

    @BeforeEach
    fun setUp() {
        repository = FileSystemFragmentRepository(
            basePath = contentDir.toString(),
            parser = MarkdownParser(extraExtensions = listOf(ChatExtension.create())),
        )
    }

    // -------------------------------------------------------------------------
    // Core rendering pipeline
    // -------------------------------------------------------------------------

    @Test
    fun `chat block in article is rendered to chat-container HTML`() = runBlocking {
        write(
            "getting-started.md",
            """
            ---
            title: "Getting Started"
            slug: getting-started
            status: PUBLISHED
            ---
            ## Introduction

            Here is an example conversation:

            ```chat
            User: Where do I start?
            Assistant: Read the quick-start guide first.
            ```

            That covers the basics.
            """.trimIndent(),
        )

        val fragment = repository.getBySlug("getting-started")
        assertNotNull(fragment, "Fragment should be found")
        val html = fragment!!.content

        assertTrue(html.contains("class=\"chat-container\""), "Should contain chat container")
        assertTrue(html.contains("chat-message--user"), "Should contain user bubble")
        assertTrue(html.contains("chat-message--assistant"), "Should contain assistant bubble")
        assertTrue(html.contains("Where do I start?"), "User message text should be present")
        assertTrue(html.contains("Read the quick-start guide first."), "Assistant message text should be present")
    }

    @Test
    fun `surrounding markdown is not affected by chat block`() = runBlocking {
        write(
            "mixed-content.md",
            """
            ---
            title: "Mixed Content"
            slug: mixed-content
            status: PUBLISHED
            ---
            # Main Heading

            A regular paragraph before the chat block.

            ```chat
            User: Is this real?
            Assistant: Yes.
            ```

            A regular paragraph **after** the chat block.

            ## Second Heading
            """.trimIndent(),
        )

        val html = repository.getBySlug("mixed-content")!!.content

        assertTrue(html.contains("<h1>Main Heading</h1>"), "H1 heading should render normally")
        assertTrue(html.contains("<h2>Second Heading</h2>"), "H2 heading should render normally")
        assertTrue(html.contains("A regular paragraph before the chat block."))
        assertTrue(html.contains("<strong>after</strong>"), "Bold text should render normally")
        assertTrue(html.contains("chat-container"), "Chat block should also be present")
    }

    // -------------------------------------------------------------------------
    // Front matter preservation
    // -------------------------------------------------------------------------

    @Test
    fun `front matter is parsed correctly when document contains chat block`() = runBlocking {
        write(
            "annotated-article.md",
            """
            ---
            title: "Annotated Article"
            slug: annotated-article
            status: PUBLISHED
            tags:
              - kotlin
              - tutorial
            categories:
              - guides
            ---
            Some content.

            ```chat
            User: Does metadata still work?
            Assistant: Yes, front matter is parsed before rendering.
            ```
            """.trimIndent(),
        )

        val fragment = repository.getBySlug("annotated-article")!!

        assertEquals("Annotated Article", fragment.title)
        assertEquals("annotated-article", fragment.slug)
        assertEquals(listOf("kotlin", "tutorial"), fragment.tags)
        assertEquals(listOf("guides"), fragment.categories)
        assertTrue(fragment.content.contains("chat-container"))
    }

    // -------------------------------------------------------------------------
    // Multiple chat blocks
    // -------------------------------------------------------------------------

    @Test
    fun `multiple chat blocks in one document all render`() = runBlocking {
        write(
            "multi-chat.md",
            """
            ---
            title: "Multi Chat"
            slug: multi-chat
            status: PUBLISHED
            ---
            First exchange:

            ```chat
            User: First question
            Assistant: First answer
            ```

            Second exchange:

            ```chat
            User: Second question
            Assistant: Second answer
            ```
            """.trimIndent(),
        )

        val html = repository.getBySlug("multi-chat")!!.content

        assertEquals(
            2,
            Regex("chat-container").findAll(html).count(),
            "Both chat blocks should render their own container",
        )
        assertTrue(html.contains("First question"))
        assertTrue(html.contains("Second answer"))
    }

    @Test
    fun `multiple turns within a single chat block all render`() = runBlocking {
        write(
            "long-chat.md",
            """
            ---
            title: "Long Chat"
            slug: long-chat
            status: PUBLISHED
            ---
            ```chat
            User: Turn one
            Assistant: Turn two
            User: Turn three
            Assistant: Turn four
            ```
            """.trimIndent(),
        )

        val html = repository.getBySlug("long-chat")!!.content
        assertEquals(4, Regex("chat-message--").findAll(html).count(), "Expected 4 chat bubbles")
    }

    // -------------------------------------------------------------------------
    // Code blocks that are NOT chat blocks
    // -------------------------------------------------------------------------

    @Test
    fun `kotlin fenced block renders as code, not chat`() = runBlocking {
        write(
            "code-article.md",
            """
            ---
            title: "Code Article"
            slug: code-article
            status: PUBLISHED
            ---
            ```kotlin
            fun greet() = println("Hello")
            ```
            """.trimIndent(),
        )

        val html = repository.getBySlug("code-article")!!.content

        assertFalse(html.contains("chat-container"), "Kotlin block must not become a chat block")
        assertTrue(html.contains("<code"), "Kotlin block must render as <code>")
    }

    @Test
    fun `document with no chat blocks renders normally`() = runBlocking {
        write(
            "plain-article.md",
            """
            ---
            title: "Plain Article"
            slug: plain-article
            status: PUBLISHED
            ---
            Just a regular article with **bold**, _italic_, and a [link](https://example.com).
            """.trimIndent(),
        )

        val html = repository.getBySlug("plain-article")!!.content

        assertFalse(html.contains("chat-container"))
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<em>italic</em>"))
        assertTrue(html.contains("<a href="))
    }

    // -------------------------------------------------------------------------
    // Repository operations with chat content
    // -------------------------------------------------------------------------

    @Test
    fun `getAll returns fragment with chat content`() = runBlocking {
        write(
            "listed.md",
            """
            ---
            title: "Listed"
            slug: listed
            status: PUBLISHED
            ---
            ```chat
            User: Am I in the list?
            Assistant: Yes.
            ```
            """.trimIndent(),
        )

        val all = repository.getAll()
        assertEquals(1, all.size)
        assertTrue(all.first().content.contains("chat-container"))
    }

    @Test
    fun `getAllVisible excludes draft fragments regardless of chat content`() = runBlocking {
        write(
            "draft-chat.md",
            """
            ---
            title: "Draft Chat"
            slug: draft-chat
            status: DRAFT
            ---
            ```chat
            User: Can readers see this?
            Assistant: No, it is a draft.
            ```
            """.trimIndent(),
        )

        val visible = repository.getAllVisible()
        assertTrue(visible.isEmpty(), "Draft fragments must not appear in getAllVisible")
    }

    @Test
    fun `getByTag works for fragment containing chat block`() = runBlocking {
        write(
            "tagged-chat.md",
            """
            ---
            title: "Tagged Chat"
            slug: tagged-chat
            status: PUBLISHED
            tags:
              - demo
            ---
            ```chat
            User: Does tagging work?
            Assistant: It does.
            ```
            """.trimIndent(),
        )

        val results = repository.getByTag("demo")
        assertEquals(1, results.size)
        assertTrue(results.first().content.contains("chat-container"))
    }

    // -------------------------------------------------------------------------
    // Custom speaker configuration
    // -------------------------------------------------------------------------

    @Test
    fun `custom user-speaker names are applied end-to-end`() = runBlocking {
        val customRepo = FileSystemFragmentRepository(
            basePath = contentDir.toString(),
            parser = MarkdownParser(
                extraExtensions = listOf(ChatExtension.create(userSpeakers = setOf("alice"))),
            ),
        )

        write(
            "custom-speakers.md",
            """
            ---
            title: "Custom Speakers"
            slug: custom-speakers
            status: PUBLISHED
            ---
            ```chat
            Alice: I am the user here
            Bob: I am the assistant
            ```
            """.trimIndent(),
        )

        val html = customRepo.getBySlug("custom-speakers")!!.content

        assertTrue(html.contains("chat-message--user"), "Alice should get user class")
        assertTrue(html.contains("chat-message--assistant"), "Bob should get assistant class")
        assertTrue(html.contains(">Alice<"), "Alice speaker label should appear")
        assertTrue(html.contains(">Bob<"), "Bob speaker label should appear")
    }

    // -------------------------------------------------------------------------
    // Slug lookup
    // -------------------------------------------------------------------------

    @Test
    fun `getBySlug returns null for non-existent slug`() = runBlocking {
        assertNull(repository.getBySlug("does-not-exist"))
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private fun write(filename: String, content: String) {
        contentDir.resolve(filename).writeText(content)
    }
}
