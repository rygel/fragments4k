package io.github.rygel.fragments.chat

/**
 * A single turn in a chat conversation as parsed from a `chat` fenced code block.
 *
 * @property speaker The raw speaker label from the source (e.g. `"User"`, `"Assistant"`).
 * @property text The message text, with leading/trailing whitespace removed.
 * @property isUser `true` when [speaker] matches one of the configured user-speaker names
 *   (see [ChatExtension.userSpeakers]). Controls which CSS class is applied in the
 *   rendered output.
 */
data class ChatMessage(
    val speaker: String,
    val text: String,
    val isUser: Boolean,
)
