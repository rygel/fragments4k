package io.andromeda.fragments.lucene

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.nio.file.Path

data class SearchResult(
    val fragment: Fragment,
    val score: Float
)

class LuceneSearchEngine(
    private val repository: FragmentRepository,
    private val indexPath: Path? = null
) {
    private val analyzer = StandardAnalyzer()
    private val directory: Directory = indexPath?.let { FSDirectory.open(it) } ?: ByteBuffersDirectory()
    private var indexWriter: IndexWriter? = null
    private var indexReader: IndexReader? = null

    suspend fun index() = withContext(Dispatchers.IO) {
        val fragments = repository.getAllVisible()
        
        val config = IndexWriterConfig(analyzer)
        val writer = IndexWriter(directory, config)
        
        writer.deleteAll()
        
        fragments.forEach { fragment ->
            val doc = Document()
            doc.add(StringField("slug", fragment.slug, Field.Store.YES))
            doc.add(StringField("title", fragment.title, Field.Store.YES))
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
    }

    suspend fun search(queryString: String, maxResults: Int = 10): List<SearchResult> = withContext(Dispatchers.IO) {
        val reader = DirectoryReader.open(directory)
        val searcher = IndexSearcher(reader)
        
        val parser = QueryParser("content", analyzer)
        val query = parser.parse(queryString)
        
        val topDocs = searcher.search(query, maxResults)
        val results = topDocs.scoreDocs.mapNotNull { scoreDoc ->
            val docId = scoreDoc.doc
            val doc = searcher.storedFields().document(docId)
            val slug = doc.get("slug")
            val fragment = repository.getBySlug(slug)
            fragment?.let { SearchResult(it, scoreDoc.score) }
        }
        
        reader.close()
        results
    }

    suspend fun searchByTag(tag: String): List<Fragment> = withContext(Dispatchers.IO) {
        val reader = DirectoryReader.open(directory)
        val searcher = IndexSearcher(reader)
        
        val query = QueryParser("tag", analyzer).parse(tag)
        val topDocs = searcher.search(query, 100)
        
        val results = topDocs.scoreDocs.mapNotNull { scoreDoc ->
            val doc = searcher.storedFields().document(scoreDoc.doc)
            val slug = doc.get("slug")
            repository.getBySlug(slug)
        }
        
        reader.close()
        results
    }

    suspend fun searchByCategory(category: String): List<Fragment> = withContext(Dispatchers.IO) {
        val reader = DirectoryReader.open(directory)
        val searcher = IndexSearcher(reader)
        
        val query = QueryParser("category", analyzer).parse(category)
        val topDocs = searcher.search(query, 100)
        
        val results = topDocs.scoreDocs.mapNotNull { scoreDoc ->
            val doc = searcher.storedFields().document(scoreDoc.doc)
            val slug = doc.get("slug")
            repository.getBySlug(slug)
        }
        
        reader.close()
        results
    }

    fun close() {
        analyzer.close()
        directory.close()
    }
}
