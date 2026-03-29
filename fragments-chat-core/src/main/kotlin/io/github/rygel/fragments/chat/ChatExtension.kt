package io.github.rygel.fragments.chat

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.util.data.MutableDataHolder
import com.vladsch.flexmark.util.misc.Extension

/**
 * Flexmark extension that renders fenced code blocks with the info string `chat`
 * as HTML chat-bubble UI instead of `<pre><code>` blocks.
 *
 * **Usage in Markdown:**
 * ````markdown
 * ```chat
 * User: How do I add this extension?
 * Assistant: Pass it to MarkdownParser via the extraExtensions parameter.
 * User: That easy?
 * Assistant: That easy.
 * ```
 * ````
 *
 * **Wiring into MarkdownParser:**
 * ```kotlin
 * val parser = MarkdownParser(extraExtensions = listOf(ChatExtension.create()))
 * ```
 *
 * **Rendered output structure:**
 * ```html
 * <div class="chat-container">
 *   <div class="chat-message chat-message--user">
 *     <span class="chat-speaker">User</span>
 *     <span class="chat-text">How do I add this extension?</span>
 *   </div>
 *   <div class="chat-message chat-message--assistant">
 *     <span class="chat-speaker">Assistant</span>
 *     <span class="chat-text">Pass it to MarkdownParser via the extraExtensions parameter.</span>
 *   </div>
 * </div>
 * ```
 *
 * Style the output with CSS targeting `.chat-container`, `.chat-message--user`, and
 * `.chat-message--assistant`. No inline styles are emitted — layout and colours are
 * entirely up to your stylesheet.
 *
 * **Multi-line messages:** Lines that do not start with a recognised `Speaker:` prefix
 * are appended to the preceding message, joined with a space.
 *
 * @property userSpeakers Set of speaker names (compared case-insensitively) that are
 *   rendered with the `chat-message--user` CSS modifier. All other speakers receive
 *   `chat-message--assistant`. Defaults to [DEFAULT_USER_SPEAKERS].
 */
class ChatExtension private constructor(
    val userSpeakers: Set<String> = DEFAULT_USER_SPEAKERS,
) : HtmlRenderer.HtmlRendererExtension, Extension {

    override fun rendererOptions(options: MutableDataHolder) {}

    override fun extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) {
        htmlRendererBuilder.nodeRendererFactory(ChatNodeRenderer.Factory(userSpeakers))
    }

    companion object {
        /**
         * Speaker names treated as the human side of the conversation.
         * Matched case-insensitively against the parsed speaker label.
         */
        val DEFAULT_USER_SPEAKERS: Set<String> = setOf("user", "human", "me", "you")

        /** Creates an extension with the default user-speaker set. */
        fun create(): ChatExtension = ChatExtension()

        /**
         * Creates an extension with a custom set of user-speaker names.
         *
         * @param userSpeakers Names that should receive the `chat-message--user` CSS class.
         *   Compared case-insensitively, so `"User"` and `"user"` are both matched.
         */
        fun create(userSpeakers: Set<String>): ChatExtension = ChatExtension(userSpeakers)
    }
}
