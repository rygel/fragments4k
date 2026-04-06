package io.github.rygel.fragments.rss

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

class RssGenerator(
    private val repositories: List<FragmentRepository>,
) {
    constructor(repository: FragmentRepository) : this(listOf(repository))

    suspend fun generateFeed(
        siteTitle: String = "My Blog",
        siteDescription: String = "My Awesome Blog",
        siteUrl: String = "http://localhost:8080",
        feedUrl: String = "http://localhost:8080/rss.xml",
    ): String {
        val fragments =
            repositories
                .flatMap { it.getAllVisible() }
                .distinctBy { it.slug }
                .sortedByDescending { it.date }
                .take(MAX_ITEMS)

        val lastBuildDate =
            fragments.firstOrNull()?.date?.let { formatDate(it) }
                ?: formatDate(LocalDateTime.now())

        val writer = StringWriter()
        val xml = XMLOutputFactory.newInstance().createXMLStreamWriter(writer)

        xml.writeStartDocument("UTF-8", "1.0")
        xml.writeStartElement("rss")
        xml.writeAttribute("version", "2.0")
        xml.writeNamespace("atom", ATOM_NS)

        xml.writeStartElement("channel")
        writeElement(xml, "title", siteTitle)
        writeElement(xml, "description", siteDescription)
        writeElement(xml, "link", siteUrl)
        writeElement(xml, "lastBuildDate", lastBuildDate)

        xml.writeEmptyElement(ATOM_NS, "link")
        xml.writeAttribute("href", feedUrl)
        xml.writeAttribute("rel", "self")
        xml.writeAttribute("type", "application/rss+xml")

        for (fragment in fragments) {
            writeItem(xml, fragment, siteUrl)
        }

        xml.writeEndElement() // channel
        xml.writeEndElement() // rss
        xml.writeEndDocument()
        xml.flush()

        return writer.toString()
    }

    private fun writeItem(
        xml: XMLStreamWriter,
        fragment: Fragment,
        siteUrl: String,
    ) {
        val fullUrl = "$siteUrl${fragment.url}"

        xml.writeStartElement("item")
        writeElement(xml, "title", fragment.title)
        writeElement(xml, "link", fullUrl)
        writeElement(xml, "description", fragment.previewTextOnly)
        fragment.date?.let { writeElement(xml, "pubDate", formatDate(it)) }
        writeElement(xml, "guid", fullUrl)

        for (category in fragment.categories) {
            writeElement(xml, "category", category)
        }
        for (tag in fragment.tags) {
            writeElement(xml, "category", tag)
        }

        xml.writeEndElement()
    }

    private fun writeElement(
        xml: XMLStreamWriter,
        name: String,
        value: String,
    ) {
        xml.writeStartElement(name)
        xml.writeCharacters(value)
        xml.writeEndElement()
    }

    companion object {
        private const val ATOM_NS = "http://www.w3.org/2005/Atom"
        private const val MAX_ITEMS = 20
        private val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss", Locale.US)

        private fun formatDate(dateTime: LocalDateTime): String = dateTime.format(formatter)
    }
}
