package io.github.rygel.fragments.rss

import io.github.rygel.fragments.Fragment
import io.github.rygel.fragments.FragmentRepository
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

class AtomGenerator(
    private val repositories: List<FragmentRepository>,
) {
    constructor(repository: FragmentRepository) : this(listOf(repository))

    suspend fun generateFeed(
        siteTitle: String = "My Blog",
        siteDescription: String = "My Awesome Blog",
        siteUrl: String = "http://localhost:8080",
        feedUrl: String = "http://localhost:8080/atom.xml",
    ): String {
        val fragments =
            repositories
                .flatMap { it.getAllVisible() }
                .distinctBy { it.slug }
                .sortedByDescending { it.date }
                .take(MAX_ITEMS)

        val updated =
            fragments.firstOrNull()?.date?.let { formatDateTime(it) }
                ?: formatDateTime(LocalDateTime.now())

        val writer = StringWriter()
        val xml = XMLOutputFactory.newInstance().createXMLStreamWriter(writer)

        xml.writeStartDocument("UTF-8", "1.0")
        xml.writeStartElement("feed")
        xml.writeNamespace("atom", ATOM_NS)

        writeElement(xml, "title", siteTitle)
        writeElement(xml, "subtitle", siteDescription)
        writeElement(xml, "updated", updated)
        writeElement(xml, "id", feedUrl)

        xml.writeEmptyElement("link")
        xml.writeAttribute("href", siteUrl)
        xml.writeAttribute("rel", "alternate")
        xml.writeAttribute("type", "text/html")

        xml.writeEmptyElement("link")
        xml.writeAttribute("href", feedUrl)
        xml.writeAttribute("rel", "self")
        xml.writeAttribute("type", "application/atom+xml")

        for (fragment in fragments) {
            writeEntry(xml, fragment, siteUrl)
        }

        xml.writeEndElement()
        xml.writeEndDocument()
        xml.flush()

        return writer.toString()
    }

    private fun writeEntry(
        xml: XMLStreamWriter,
        fragment: Fragment,
        siteUrl: String,
    ) {
        val fullUrl = "$siteUrl${fragment.url}"

        xml.writeStartElement("entry")
        writeElement(xml, "title", fragment.title)
        writeElement(xml, "id", fullUrl)
        writeElement(xml, "updated", fragment.date?.let { formatDateTime(it) } ?: formatDateTime(LocalDateTime.now()))

        xml.writeEmptyElement("link")
        xml.writeAttribute("href", fullUrl)
        xml.writeAttribute("rel", "alternate")
        xml.writeAttribute("type", "text/html")

        fragment.date?.let { writeElement(xml, "published", formatDateTime(it)) }

        if (fragment.previewTextOnly.isNotBlank()) {
            xml.writeStartElement("summary")
            xml.writeAttribute("type", "text")
            xml.writeCharacters(fragment.previewTextOnly)
            xml.writeEndElement()
        }

        fragment.primaryAuthor?.let { author ->
            xml.writeStartElement("author")
            writeElement(xml, "name", author)
            xml.writeEndElement()
        }

        for (category in fragment.categories) {
            xml.writeEmptyElement("category")
            xml.writeAttribute("term", category)
        }
        for (tag in fragment.tags) {
            xml.writeEmptyElement("category")
            xml.writeAttribute("term", tag)
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
        private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        private fun formatDateTime(dateTime: LocalDateTime) = formatter.format(dateTime.atZone(ZoneOffset.UTC))
    }
}
