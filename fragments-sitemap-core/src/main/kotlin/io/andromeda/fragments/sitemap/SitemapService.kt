package io.andromeda.fragments.sitemap

import io.andromeda.fragments.FragmentRepository
import io.andromeda.fragments.sitemap.SitemapGenerator
import io.andromeda.fragments.sitemap.SitemapService
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
