# Fragments Social Core

Social media share link generation.

## Usage

```kotlin
val links = SocialShareGenerator.generateShareLinks(
    title = "My Blog Post",
    url = "https://example.com/blog/my-post"
)
// Returns share links for: Twitter, Facebook, LinkedIn, Reddit, WhatsApp, Email

// Or specific platforms only:
val links = SocialShareGenerator.generateShareLinks(
    title = "My Post",
    url = "https://example.com/blog/my-post",
    platforms = listOf(SocialPlatform.TWITTER, SocialPlatform.LINKEDIN)
)
```

Each `SocialShareLink` has `platform`, `url` (ready-to-use share URL), and `title`.
