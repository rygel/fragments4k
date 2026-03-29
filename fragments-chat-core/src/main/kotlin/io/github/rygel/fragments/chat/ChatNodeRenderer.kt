package io.github.rygel.fragments.chat

import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.html.renderer.NodeRendererFactory
import com.vladsch.flexmark.util.data.DataHolder

/**
 * Flexmark [NodeRenderer] that intercepts [FencedCodeBlock] nodes whose info
 * string is exactly `"chat"` and renders them as HTML chat bubbles.
 *
 * All other fenced code blocks are passed through to the default renderer via
 * [NodeRendererContext.delegateRender].
 *
 * Register via [Factory]; do not instantiate directly — use [ChatExtension.create].
 */
internal class ChatNodeRenderer(
    private val userSpeakers: Set<String>,
) : NodeRenderer {

    override fun getNodeRenderingHandlers(): Set<NodeRenderingHandler<*>> =
        setOf(NodeRenderingHandler(FencedCodeBlock::class.java, ::renderFencedCodeBlock))

    private fun renderFencedCodeBlock(
        node: FencedCodeBlock,
        context: NodeRendererContext,
        html: HtmlWriter,
    ) {
        if (node.info.toString().trim() != "chat") {
            context.delegateRender()
            return
        }

        val messages = parseMessages(node.contentChars.toString())
        if (messages.isEmpty()) return

        html.attr("class", "chat-container").withAttr().tag("div").line()
        for (message in messages) {
            val modifier = if (message.isUser) "user" else "assistant"
            html.attr("class", "chat-message chat-message--$modifier").withAttr().tag("div")
            html.attr("class", "chat-speaker").withAttr().tag("span")
            html.text(message.speaker)
            html.closeTag("span")
            html.attr("class", "chat-text").withAttr().tag("span")
            html.text(message.text)
            html.closeTag("span")
            html.closeTag("div").line()
        }
        html.closeTag("div").line()
    }

    /**
     * Parses the raw content of a chat block into a list of [ChatMessage] instances.
     *
     * Rules:
     * - A line matching `Speaker: text` (colon at position > 0, speaker contains no
     *   whitespace) starts a new message.
     * - Subsequent lines that do not match the speaker pattern are treated as
     *   continuation text and appended to the current message with a space.
     * - Blank lines are ignored.
     */
    private fun parseMessages(content: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        var currentSpeaker: String? = null
        val currentLines = mutableListOf<String>()

        fun flush() {
            val speaker = currentSpeaker ?: return
            val text = currentLines.joinToString(" ").trim()
            if (text.isNotEmpty()) {
                messages.add(ChatMessage(speaker, text, speaker.lowercase() in userSpeakers))
            }
            currentLines.clear()
        }

        for (line in content.lines()) {
            if (line.isBlank()) continue

            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val potentialSpeaker = line.substring(0, colonIdx).trim()
                // Treat as a new speaker turn only when the label is a single word
                // (no spaces). This avoids accidentally treating "Note: foo" inside
                // a continuation sentence as a speaker change.
                if (potentialSpeaker.isNotEmpty() && !potentialSpeaker.contains(' ')) {
                    flush()
                    currentSpeaker = potentialSpeaker
                    val text = line.substring(colonIdx + 1).trim()
                    if (text.isNotEmpty()) currentLines.add(text)
                } else {
                    currentLines.add(line.trim())
                }
            } else if (currentSpeaker != null) {
                currentLines.add(line.trim())
            }
        }
        flush()

        return messages
    }

    /** Registered with [ChatExtension] to instantiate this renderer per-render. */
    class Factory(private val userSpeakers: Set<String>) : NodeRendererFactory {
        override fun apply(options: DataHolder): NodeRenderer = ChatNodeRenderer(userSpeakers)
    }
}
