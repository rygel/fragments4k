package io.github.rygel.fragments.sitemap

import io.github.rygel.fragments.FragmentRepository
import io.github.rygel.fragments.sitemap.SitemapGenerator
import io.github.rygel.fragments.sitemap.SitemapService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class SitemapService(
    private val repository: FragmentRepository,
    private val generator: SitemapGenerator,
    private val siteUrl: String,
    private val siteTitle: String
) {

    fun generate(): String {
        return try {
            runBlocking {
                val sitemap = generator.generateSitemap()
                logger.info("Generated sitemap at $siteUrl/sitemap.xml with ${sitemap.lines().size} URLs")
                sitemap
            }
        } catch (e: Exception) {
            logger.error("Failed to generate sitemap", e)
            throw e
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("SitemapService")
    }
}
