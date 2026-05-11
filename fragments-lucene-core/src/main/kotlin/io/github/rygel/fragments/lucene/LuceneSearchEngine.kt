package io.github.rygel.fragments.lucene

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.queryparser.classic.ParseException
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.FuzzyQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.PhraseQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.WildcardQuery
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.slf4j.LoggerFactory
import java.nio.file.Path

data class SearchResult(
    val fragment: Fragment,
    val score: Float,
)

data class SearchOptions(
    val query: String,
    val maxResults: Int = 10,
    val phraseSearch: Boolean = false,
    val fuzzySearch: Boolean = false,
    val fuzzyThreshold: Float = 0.7f,
    val autocomplete: Boolean = false,
    val autocompleteLimit: Int = 10,
)

data class SearchSuggestion(
    val text: String,
    val frequency: Int,
    val type: SuggestionType,
) {
    enum class SuggestionType {
        TITLE,
        TAG,
        CATEGORY,
    }
}

/**
 * Lucene-backed full-text search engine for fragment content.
 *
 * When [indexPath] is provided, the index is persisted to disk via [FSDirectory].
 * Ensure the directory has appropriate OS-level file permissions (read/write for the
 * application user, no access for others) to prevent unauthorized index manipulation.
 * When `null`, an in-memory [ByteBuffersDirectory] is used instead.
 */
class LuceneSearchEngine(
    private val repositories: List<FragmentRepository>,
    private val indexPath: Path? = null,
) {
    constructor(repository: FragmentRepository, indexPath: Path? = null) : this(listOf(repository), indexPath)

    companion object {
        private const val TITLE_BOOST = 2.0f
        private const val CONTENT_BOOST = 1.0f
        private const val MAX_TAG_CATEGORY_RESULTS = 100
        private val logger = LoggerFactory.getLogger(LuceneSearchEngine::class.java)
    }

    private val analyzer = StandardAnalyzer()
    private val directory: org.apache.lucene.store.Directory =
        indexPath?.let {
            org.apache.lucene.store.FSDirectory
                .open(it)
        } ?: org.apache.lucene.store
            .ByteBuffersDirectory()

    @Volatile private var reader: org.apache.lucene.index.DirectoryReader? = null
    private val indexMutex = Mutex()

    suspend fun index() =
        indexMutex.withLock {
            withContext(Dispatchers.IO) {
                val config =
                    org.apache.lucene.index
                        .IndexWriterConfig(analyzer)
                val writer =
                    org.apache.lucene.index
                        .IndexWriter(directory, config)
                writer.deleteAll()

                val fragments = repositories.flatMap { it.getAllVisible() }

                fragments.forEach { fragment ->
                    val doc = Document()
                    doc.add(StringField("slug", fragment.slug, Field.Store.YES))
                    doc.add(StringField("url", fragment.url, Field.Store.YES))
                    doc.add(TextField("title", fragment.title, Field.Store.YES))
                    doc.add(TextField("content", fragment.contentTextOnly, Field.Store.NO))
                    doc.add(TextField("preview", fragment.previewTextOnly, Field.Store.NO))
                    fragment.tags.forEach { tag ->
                        doc.add(StringField("tag", tag, Field.Store.YES))
                    }
                    fragment.categories.forEach { category ->
                        doc.add(StringField("category", category, Field.Store.YES))
                    }
                    fragment.date?.let { date ->
                        doc.add(StringField("date", date.toString(), Field.Store.YES))
                    }
                    writer.addDocument(doc)
                }

                writer.commit()
                writer.close()

                reader = reader?.let {
                    org.apache.lucene.index.DirectoryReader
                        .openIfChanged(it) ?: it
                }
                    ?: org.apache.lucene.index.DirectoryReader
                        .open(directory)
            }
        }

    private fun requireReader(): org.apache.lucene.index.DirectoryReader =
        reader ?: error("Search index not initialised — call index() first")

    private inline fun <T> withSearcher(block: (IndexSearcher) -> List<T>): List<T> {
        val currentReader = reader ?: return emptyList()
        val searcher = IndexSearcher(currentReader)
        return block(searcher)
    }

    suspend fun search(
        queryString: String,
        maxResults: Int = 10,
    ): List<SearchResult> =
        withContext(Dispatchers.IO) {
            search(SearchOptions(query = queryString, maxResults = maxResults))
        }

    suspend fun search(options: SearchOptions): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val fragmentsBySlug = repositories.flatMap { it.getAllVisible() }.associateBy { it.slug }
            val query = buildQuery(options) ?: return@withContext emptyList()

            withSearcher { searcher ->
                val maxResults = if (options.autocomplete) options.autocompleteLimit else options.maxResults
                val topDocs = searcher.search(query, maxResults)

                topDocs.scoreDocs.mapNotNull { scoreDoc ->
                    val doc = searcher.storedFields().document(scoreDoc.doc)
                    fragmentsBySlug[doc.get("slug")]?.let { SearchResult(it, scoreDoc.score) }
                }
            }
        }

    suspend fun autocomplete(
        query: String,
        limit: Int = 10,
    ): List<SearchSuggestion> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()

            val lowerQuery = query.lowercase()
            withSearcher { searcher ->
                val titleQuery =
                    WildcardQuery(
                        org.apache.lucene.index
                            .Term("title", "$lowerQuery*"),
                    )
                val topDocs = searcher.search(titleQuery, limit * 3)

                val suggestions = mutableSetOf<SearchSuggestion>()
                topDocs.scoreDocs.forEach { scoreDoc ->
                    val doc = searcher.storedFields().document(scoreDoc.doc)

                    val title = doc.get("title")
                    if (title.lowercase().contains(lowerQuery)) {
                        suggestions.add(SearchSuggestion(title, 1, SearchSuggestion.SuggestionType.TITLE))
                    }
                    doc.getValues("tag").forEach { tag ->
                        if (tag.lowercase().contains(lowerQuery)) {
                            suggestions.add(SearchSuggestion(tag, 1, SearchSuggestion.SuggestionType.TAG))
                        }
                    }
                    doc.getValues("category").forEach { category ->
                        if (category.lowercase().contains(lowerQuery)) {
                            suggestions.add(SearchSuggestion(category, 1, SearchSuggestion.SuggestionType.CATEGORY))
                        }
                    }
                }
                suggestions.take(limit).toList()
            }
        }

    suspend fun getSuggestions(
        query: String,
        limit: Int = 10,
    ): List<SearchSuggestion> = autocomplete(query, limit)

    private fun buildQuery(options: SearchOptions): Query? =
        when {
            options.phraseSearch -> buildPhraseQuery(options.query)
            options.fuzzySearch -> buildFuzzyQuery(options.query, options.fuzzyThreshold)
            else -> buildStandardQuery(options.query)
        }

    private fun buildStandardQuery(query: String): Query? {
        val fields = arrayOf("title", "content")
        val boosts = mapOf("title" to TITLE_BOOST, "content" to CONTENT_BOOST)
        val parser = MultiFieldQueryParser(fields, analyzer, boosts)
        parser.defaultOperator = QueryParser.Operator.AND
        return try {
            parser.parse(query)
        } catch (e: ParseException) {
            logger.warn("Failed to parse search query '{}': {}", query, e.message)
            null
        }
    }

    private fun buildPhraseQuery(query: String): Query {
        val terms = query.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        // Build a phrase query for each field, combine with SHOULD so either field can satisfy
        val booleanQuery = BooleanQuery.Builder()
        for (field in arrayOf("title", "content")) {
            val builder = PhraseQuery.Builder()
            builder.setSlop(2)
            terms.forEach { term ->
                builder.add(
                    org.apache.lucene.index
                        .Term(field, term.lowercase()),
                )
            }
            booleanQuery.add(builder.build(), BooleanClause.Occur.SHOULD)
        }
        return booleanQuery.setMinimumNumberShouldMatch(1).build()
    }

    private fun buildFuzzyQuery(
        query: String,
        threshold: Float,
    ): Query {
        val terms = query.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val booleanQuery = BooleanQuery.Builder()

        terms.forEach { term ->
            val maxEdits = kotlin.math.min(((1.0 - threshold) * term.length).toInt(), 2)
            for (field in arrayOf("title", "content")) {
                val luceneTerm =
                    org.apache.lucene.index
                        .Term(field, term.lowercase())
                booleanQuery.add(FuzzyQuery(luceneTerm, maxEdits), BooleanClause.Occur.SHOULD)
            }
        }

        return booleanQuery.setMinimumNumberShouldMatch(1).build()
    }

    suspend fun searchByTag(tag: String): List<Fragment> =
        withContext(Dispatchers.IO) {
            val fragmentsBySlug = repositories.flatMap { it.getAllVisible() }.associateBy { it.slug }

            withSearcher { searcher ->
                val query =
                    TermQuery(
                        org.apache.lucene.index
                            .Term("tag", tag.lowercase()),
                    )
                val topDocs = searcher.search(query, MAX_TAG_CATEGORY_RESULTS)

                topDocs.scoreDocs.mapNotNull { scoreDoc ->
                    fragmentsBySlug[searcher.storedFields().document(scoreDoc.doc).get("slug")]
                }
            }
        }

    suspend fun searchByCategory(category: String): List<Fragment> =
        withContext(Dispatchers.IO) {
            val fragmentsBySlug = repositories.flatMap { it.getAllVisible() }.associateBy { it.slug }

            withSearcher { searcher ->
                val query =
                    TermQuery(
                        org.apache.lucene.index
                            .Term("category", category.lowercase()),
                    )
                val topDocs = searcher.search(query, MAX_TAG_CATEGORY_RESULTS)

                topDocs.scoreDocs.mapNotNull { scoreDoc ->
                    fragmentsBySlug[searcher.storedFields().document(scoreDoc.doc).get("slug")]
                }
            }
        }

    fun close() {
        reader?.close()
        analyzer.close()
        directory.close()
    }
}
