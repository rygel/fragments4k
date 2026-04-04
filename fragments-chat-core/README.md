# Fragments Chat Core

A flexmark Markdown extension that renders chat/conversation blocks as styled HTML.

## Usage

Register the extension with `MarkdownParser`:

```kotlin
val parser = MarkdownParser(
    extraExtensions = listOf(ChatExtension.create())
)
```

Then use chat blocks in Markdown:

````markdown
```chat
user: How do I deploy this?
assistant: Run `./mvnw deploy` to publish to GitHub Packages.
user: Thanks!
```
````

This renders as a `<div class="chat-container">` with styled message bubbles. Speaker names matching `user`, `human`, `me`, or `you` are rendered as user messages; all others as assistant messages.

### Custom Speaker Names

```kotlin
ChatExtension.create(userSpeakers = setOf("alice", "user"))
```
