package io.andromeda.fragments.test

import io.andromeda.fragments.Author
import io.andromeda.fragments.ContentSeries
import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentStatus
import io.andromeda.fragments.SeriesStatus
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

/**
 * Generator for creating random test data
 */
class RandomDataGenerator(private val random: Random = Random.Default) {
    
    /**
     * Generate a random string
     */
    fun randomString(length: Int = 10): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    /**
     * Generate a random email
     */
    fun randomEmail(): String {
        return "${randomString(8).lowercase()}@${randomString(6).lowercase()}.com"
    }
    
    /**
     * Generate a random URL
     */
    fun randomUrl(): String {
        return "https://${randomString(8).lowercase()}.com/${randomString(10).lowercase()}"
    }
    
    /**
     * Generate a random date within the past year
     */
    fun randomDate(): LocalDateTime {
        val daysBack = random.nextInt(365)
        return LocalDateTime.now().minusDays(daysBack.toLong())
    }
    
    /**
     * Generate a random future date
     */
    fun randomFutureDate(): LocalDateTime {
        val daysForward = random.nextInt(30) + 1
        return LocalDateTime.now().plusDays(daysForward.toLong())
    }
    
    /**
     * Generate a random paragraph
     */
    fun randomParagraph(sentences: Int = 3): String {
        return (1..sentences)
            .map { randomSentence() }
            .joinToString(" ")
    }
    
    /**
     * Generate a random sentence
     */
    fun randomSentence(words: Int = 10): String {
        return (1..words)
            .map { randomWord() }
            .joinToString(" ")
            .replaceFirstChar { it.uppercase() } + "."
    }
    
    /**
     * Generate a random word
     */
    fun randomWord(): String {
        val words = listOf(
            "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed",
            "do", "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore", "magna", "aliqua",
            "test", "example", "fragment", "content", "series", "author", "category", "tag", "search"
        )
        return words[random.nextInt(words.size)]
    }
    
    /**
     * Generate a random list of tags
     */
    fun randomTags(count: Int = 3): List<String> {
        val possibleTags = listOf(
            "kotlin", "java", "python", "javascript", "typescript", "go", "rust",
            "programming", "development", "web", "mobile", "database", "api", "testing",
            "tutorial", "blog", "article", "news", "opinion", "review", "tutorial",
            "frontend", "backend", "devops", "security", "performance", "optimization"
        )
        return possibleTags.shuffled(random).take(count).sorted()
    }
    
    /**
     * Generate a random list of categories
     */
    fun randomCategories(count: Int = 2): List<String> {
        val possibleCategories = listOf(
            "technology", "programming", "tutorials", "news", "opinion", "reviews",
            "web", "mobile", "database", "api", "testing", "security", "performance"
        )
        return possibleCategories.shuffled(random).take(count).sorted()
    }
    
    /**
     * Generate a random fragment
     */
    fun randomFragment(): Fragment {
        return FragmentFactory.Builder()
            .title("Random Fragment ${randomString(4)}")
            .slug("random-fragment-${randomString(6)}")
            .date(randomDate())
            .content(randomParagraph())
            .categories(randomCategories())
            .tags(randomTags())
            .build()
    }
    
    /**
     * Generate a random published fragment
     */
    fun randomPublishedFragment(): Fragment {
        return randomFragment().copy(status = FragmentStatus.PUBLISHED, visible = true)
    }
    
    /**
     * Generate a random draft fragment
     */
    fun randomDraftFragment(): Fragment {
        return randomFragment().copy(status = FragmentStatus.DRAFT, visible = false)
    }
    
    /**
     * Generate multiple random fragments
     */
    fun randomFragments(count: Int): List<Fragment> {
        return (1..count).map { randomPublishedFragment() }
    }
    
    /**
     * Generate a random author
     */
    fun randomAuthor(): Author {
        return AuthorFactory.Builder()
            .id("random-author-${randomString(6)}")
            .name("Random Author ${randomString(6)}")
            .slug("random-author-${randomString(6)}")
            .email(randomEmail())
            .bio(randomParagraph(2))
            .avatar("https://example.com/avatar-${randomString(8)}.jpg")
            .location("Random City, ST")
            .company("Random Company Inc.")
            .role("Random Role")
            .socialLinks(mapOf(
                "twitter" to "@${randomString(8)}",
                "github" to "${randomString(8)}",
                "linkedin" to "${randomString(8)}"
            ))
            .joinedDate(randomDate())
            .build()
    }
    
    /**
     * Generate multiple random authors
     */
    fun randomAuthors(count: Int): List<Author> {
        return (1..count).map { randomAuthor() }
    }
    
    /**
     * Generate a random content series
     */
    fun randomContentSeries(): ContentSeries {
        val statuses = listOf(SeriesStatus.ACTIVE, SeriesStatus.INACTIVE)
        return ContentSeriesFactory.Builder()
            .title("Random Series ${randomString(4)}")
            .slug("random-series-${randomString(6)}")
            .description(randomParagraph(1))
            .status(statuses[random.nextInt(statuses.size)])
            .createdAt(randomDate())
            .updatedAt(randomDate().plusDays(random.nextInt(30).toLong()))
            .build()
    }
    
    /**
     * Generate multiple random content series
     */
    fun randomContentSeriesList(count: Int): List<ContentSeries> {
        return (1..count).map { randomContentSeries() }
    }
    
    /**
     * Generate a random UUID
     */
    fun randomUUID(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Generate a random number within range
     */
    fun randomInt(range: IntRange): Int {
        return random.nextInt(range.first, range.last + 1)
    }
    
    /**
     * Generate a random boolean with probability
     */
    fun randomBoolean(probability: Double = 0.5): Boolean {
        return random.nextDouble() < probability
    }
    
    /**
     * Generate a random element from a list
     */
    fun <T> randomElement(list: List<T>): T {
        return list[random.nextInt(list.size)]
    }
    
    /**
     * Generate a random sublist
     */
    fun <T> randomSublist(list: List<T>, size: Int): List<T> {
        return list.shuffled(random).take(size)
    }
    
    /**
     * Generate Lorem Ipsum text
     */
    fun loremIpsum(words: Int = 50): String {
        val loremWords = listOf(
            "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed",
            "do", "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore", "magna", "aliqua",
            "enim", "ad", "minim", "veniam", "quis", "nostrud", "exercitation", "ullamco", "laboris", "nisi",
            "ut", "aliquip", "ex", "ea", "commodo", "consequat", "duis", "aute", "irure", "dolor"
        )
        return (1..words)
            .map { loremWords[random.nextInt(loremWords.size)] }
            .joinToString(" ")
            .replaceFirstChar { it.uppercase() } + "."
    }
    
    /**
     * Generate HTML content
     */
    fun randomHtmlContent(): String {
        return "<div class=\"content\">" +
                "<h1>${randomWord()}</h1>" +
                "<p>${randomParagraph()}</p>" +
                "<h2>${randomWord()}</h2>" +
                "<p>${randomParagraph()}</p>" +
                "<p>${randomParagraph()}</p>" +
                "</div>"
    }
}
