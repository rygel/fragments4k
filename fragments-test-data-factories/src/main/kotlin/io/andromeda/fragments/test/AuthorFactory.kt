package io.andromeda.fragments.test

import io.andromeda.fragments.Author
import java.time.LocalDateTime

/**
 * Factory for creating test Author objects
 */
object AuthorFactory {
    
    /**
     * Create a basic author with required fields
     */
    fun create(
        id: String = "test-author-id",
        name: String = "Test Author",
        slug: String = "test-author",
        email: String = "test@example.com"
    ): Author {
        return Author(
            id = id,
            name = name,
            slug = slug,
            email = email,
            bio = null,
            avatar = null,
            location = null,
            company = null,
            role = null,
            socialLinks = emptyMap(),
            joinedDate = LocalDateTime.now()
        )
    }
    
    /**
     * Builder for creating authors with custom configuration
     */
    class Builder {
        private var id: String = "test-author-id"
        private var name: String = "Test Author"
        private var slug: String = "test-author"
        private var email: String = "test@example.com"
        private var bio: String? = null
        private var avatar: String? = null
        private var location: String? = null
        private var company: String? = null
        private var role: String? = null
        private var socialLinks: Map<String, String> = emptyMap()
        private var joinedDate: LocalDateTime = LocalDateTime.now()
        
        fun id(id: String) = apply { this.id = id }

        fun name(name: String) = apply { this.name = name }

        fun slug(slug: String) = apply { this.slug = slug }

        fun email(email: String) = apply { this.email = email }

        fun bio(bio: String?) = apply { this.bio = bio }

        fun avatar(avatar: String?) = apply { this.avatar = avatar }

        fun location(location: String?) = apply { this.location = location }

        fun company(company: String?) = apply { this.company = company }

        fun role(role: String?) = apply { this.role = role }

        fun socialLinks(socialLinks: Map<String, String>) = apply { this.socialLinks = socialLinks }

        fun joinedDate(joinedDate: LocalDateTime) = apply { this.joinedDate = joinedDate }
        
        fun build(): Author {
            return Author(
                id = id,
                name = name,
                slug = slug,
                email = email,
                bio = bio,
                avatar = avatar,
                location = location,
                company = company,
                role = role,
                socialLinks = socialLinks,
                joinedDate = joinedDate
            )
        }
    }
    
    /**
     * Create an author with full profile
     */
    fun fullProfile(): Author {
        return Builder()
            .id("full-test-author-id")
            .name("Full Test Author")
            .slug("full-test-author")
            .email("full@example.com")
            .bio("This is a test author with a full profile including bio, avatar, location, and social links.")
            .avatar("https://example.com/avatar.jpg")
            .location("San Francisco, CA")
            .company("Test Company")
            .role("Senior Developer")
            .socialLinks(mapOf(
                "twitter" to "@testauthor",
                "github" to "testauthor",
                "linkedin" to "testauthor",
                "website" to "https://example.com"
            ))
            .joinedDate(LocalDateTime.of(2024, 1, 1, 0, 0))
            .build()
    }
    
    /**
     * Create an author with social links
     */
    fun withSocialLinks(vararg links: Pair<String, String>): Author {
        return Builder()
            .socialLinks(links.toMap())
            .build()
    }
    
    /**
     * Create multiple authors
     */
    fun createMany(count: Int): List<Author> {
        return (1..count).map { i ->
            create(
                id = "test-author-id-$i",
                name = "Test Author $i",
                slug = "test-author-$i",
                email = "test$i@example.com"
            )
        }
    }
}
