package io.andromeda.fragments.test

import io.andromeda.fragments.Author
import io.andromeda.fragments.ContentSeries
import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentStatus
import io.andromeda.fragments.SeriesStatus
import java.time.LocalDateTime

/**
 * Builder for creating test scenarios
 */
class TestScenario {
    
    /**
     * Scenario for a simple blog with published posts
     */
    companion object {
        /**
         * Create a simple blog scenario with published posts
         */
        fun simpleBlog(postCount: Int = 5): TestScenarioBuilder {
            return TestScenarioBuilder()
                .name("Simple Blog")
                .description("A simple blog with published posts")
                .fragments(
                    FragmentFactory.createMany(postCount).map { it.copy(status = FragmentStatus.PUBLISHED) }
                )
        }
        
        /**
         * Create a blog with draft posts
         */
        fun blogWithDrafts(publishedCount: Int = 5, draftCount: Int = 3): TestScenarioBuilder {
            val publishedFragments = FragmentFactory.createMany(publishedCount)
                .map { it.copy(status = FragmentStatus.PUBLISHED) }
            
            val draftFragments = FragmentFactory.createMany(draftCount)
                .map { it.copy(status = FragmentStatus.DRAFT, visible = false) }
            
            return TestScenarioBuilder()
                .name("Blog with Drafts")
                .description("A blog with both published and draft posts")
                .fragments(publishedFragments + draftFragments)
        }
        
        /**
         * Create a blog with scheduled posts
         */
        fun blogWithScheduledPosts(postCount: Int = 5): TestScenarioBuilder {
            val fragments = FragmentFactory.createMany(postCount).mapIndexed { index, fragment ->
                when (index % 3) {
                    0 -> fragment.copy(
                        status = FragmentStatus.SCHEDULED,
                        publishDate = LocalDateTime.now().plusDays(1)
                    )
                    1 -> fragment.copy(
                        status = FragmentStatus.SCHEDULED,
                        publishDate = LocalDateTime.now().plusDays(7)
                    )
                    else -> fragment
                }
            }
            
            return TestScenarioBuilder()
                .name("Blog with Scheduled Posts")
                .description("A blog with scheduled posts")
                .fragments(fragments)
        }
        
        /**
         * Create a blog with expiring posts
         */
        fun blogWithExpiringPosts(postCount: Int = 5): TestScenarioBuilder {
            val fragments = FragmentFactory.createMany(postCount).mapIndexed { index, fragment ->
                when (index % 3) {
                    0 -> fragment.copy(
                        status = FragmentStatus.PUBLISHED,
                        expiryDate = LocalDateTime.now().plusDays(1)
                    )
                    1 -> fragment.copy(
                        status = FragmentStatus.PUBLISHED,
                        expiryDate = LocalDateTime.now().plusDays(7)
                    )
                    else -> fragment
                }
            }
            
            return TestScenarioBuilder()
                .name("Blog with Expiring Posts")
                .description("A blog with posts that will expire")
                .fragments(fragments)
        }
        
        /**
         * Create a blog with content series
         */
        fun blogWithSeries(
            seriesCount: Int = 2,
            postsPerSeries: Int = 5
        ): TestScenarioBuilder {
            val seriesList = ContentSeriesFactory.createMany(seriesCount)
            val fragments = mutableListOf<Fragment>()
            
            seriesList.forEachIndexed { seriesIndex, series ->
                repeat(postsPerSeries) { postIndex ->
                    fragments.add(
                        FragmentFactory.Builder()
                            .title("Series ${seriesIndex + 1}, Post ${postIndex + 1}")
                            .slug("${series.slug}-post-${postIndex + 1}")
                            .content("<p>Content for series ${seriesIndex + 1}, post ${postIndex + 1}</p>")
                            .status(FragmentStatus.PUBLISHED)
                            .seriesSlug(series.slug)
                            .seriesPart(postIndex + 1)
                            .seriesTitle(series.title)
                            .build()
                    )
                }
            }
            
            return TestScenarioBuilder()
                .name("Blog with Series")
                .description("A blog with multiple content series")
                .fragments(fragments)
                .contentSeries(seriesList)
        }
        
        /**
         * Create a multi-author blog
         */
        fun multiAuthorBlog(
            authorCount: Int = 3,
            postsPerAuthor: Int = 5
        ): TestScenarioBuilder {
            val authors = AuthorFactory.createMany(authorCount)
            val fragments = mutableListOf<Fragment>()
            
            authors.forEachIndexed { authorIndex, author ->
                repeat(postsPerAuthor) { postIndex ->
                    fragments.add(
                        FragmentFactory.Builder()
                            .title("Post ${authorIndex + 1}-${postIndex + 1}")
                            .slug("author-${author.slug}-post-${postIndex + 1}")
                            .content("<p>Content by ${author.name}</p>")
                            .status(FragmentStatus.PUBLISHED)
                            .author(author.name)
                            .authorIds(listOf(author.slug))
                            .build()
                    )
                }
            }
            
            return TestScenarioBuilder()
                .name("Multi-Author Blog")
                .description("A blog with multiple authors")
                .fragments(fragments)
                .authors(authors)
        }
        
        /**
         * Create a complex scenario with multiple features
         */
        fun complexBlog(): TestScenarioBuilder {
            val authors = AuthorFactory.createMany(3)
            val seriesList = ContentSeriesFactory.createMany(2)
            val publishedFragments = FragmentFactory.createMany(10)
                .map { it.copy(status = FragmentStatus.PUBLISHED) }
            val draftFragments = FragmentFactory.createMany(3)
                .map { it.copy(status = FragmentStatus.DRAFT, visible = false) }

            return TestScenarioBuilder()
                .name("Complex Blog")
                .description("A complex blog with multiple features including authors, series, published and draft posts")
                .fragments(publishedFragments + draftFragments)
                .authors(authors)
                .contentSeries(seriesList)
        }
    }
}

/**
 * Builder for test scenarios
 */
class TestScenarioBuilder {
    private var name: String = "Test Scenario"
    private var description: String = "A test scenario"
    private val fragments = mutableListOf<Fragment>()
    private val authors = mutableListOf<Author>()
    private val contentSeries = mutableListOf<ContentSeries>()
    
    fun name(name: String): TestScenarioBuilder {
        this.name = name
        return this
    }
    
    fun description(description: String): TestScenarioBuilder {
        this.description = description
        return this
    }
    
    fun fragments(vararg fragments: Fragment): TestScenarioBuilder {
        this.fragments.clear()
        this.fragments.addAll(fragments)
        return this
    }
    
    fun fragments(fragments: List<Fragment>): TestScenarioBuilder {
        this.fragments.clear()
        this.fragments.addAll(fragments)
        return this
    }
    
    fun authors(vararg authors: Author): TestScenarioBuilder {
        this.authors.clear()
        this.authors.addAll(authors)
        return this
    }
    
    fun authors(authors: List<Author>): TestScenarioBuilder {
        this.authors.clear()
        this.authors.addAll(authors)
        return this
    }
    
    fun contentSeries(vararg series: ContentSeries): TestScenarioBuilder {
        this.contentSeries.clear()
        this.contentSeries.addAll(series)
        return this
    }
    
    fun contentSeries(series: List<ContentSeries>): TestScenarioBuilder {
        this.contentSeries.clear()
        this.contentSeries.addAll(series)
        return this
    }
    
    fun build(): Scenario {
        return Scenario(
            name = name,
            description = description,
            fragments = fragments.toList(),
            authors = authors.toList(),
            contentSeries = contentSeries.toList()
        )
    }
}

/**
 * Test scenario with associated data
 */
data class Scenario(
    val name: String,
    val description: String,
    val fragments: List<Fragment>,
    val authors: List<Author>,
    val contentSeries: List<ContentSeries>
)
