package io.andromeda.fragments.test

import io.andromeda.fragments.Fragment
import io.andromeda.fragments.FragmentRevision
import io.andromeda.fragments.FragmentRevisionRepository
import io.andromeda.fragments.FragmentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class InMemoryFragmentRevisionRepository : FragmentRevisionRepository {
    private val revisions = mutableMapOf<String, FragmentRevision>()
    private val fragmentRevisions = mutableMapOf<String, MutableList<String>>()
    private val versionCounter = mutableMapOf<String, Int>()

    override suspend fun saveRevision(fragment: Fragment, changedBy: String?, reason: String?): FragmentRevision = withContext(Dispatchers.IO) {
        val currentVersion = versionCounter[fragment.slug] ?: 0
        val newVersion = currentVersion + 1
        versionCounter[fragment.slug] = newVersion

        val previousRevisionId = fragmentRevisions[fragment.slug]?.lastOrNull()
        val previousRevision = previousRevisionId?.let { revisions[it] }
        val diff = previousRevision?.let { generateDiff(it, fragment) }

        val revision = FragmentRevision(
            id = "${fragment.slug}-v$newVersion-${System.currentTimeMillis()}",
            fragmentSlug = fragment.slug,
            version = newVersion,
            title = fragment.title,
            content = fragment.content,
            preview = fragment.preview,
            frontMatter = fragment.frontMatter,
            changedBy = changedBy,
            changedAt = LocalDateTime.now(),
            changeReason = reason,
            previousRevisionId = previousRevisionId,
            diff = diff
        )

        revisions[revision.id] = revision
        fragmentRevisions.getOrPut(fragment.slug) { mutableListOf() }.add(revision.id)

        return@withContext revision
    }

    override suspend fun getRevisions(slug: String): List<FragmentRevision> = withContext(Dispatchers.IO) {
        fragmentRevisions[slug]?.mapNotNull { revisions[it] }?.sortedByDescending { it.version } ?: emptyList()
    }

    override suspend fun getRevision(id: String): FragmentRevision? = withContext(Dispatchers.IO) {
        revisions[id]
    }

    override suspend fun getLatestRevision(slug: String): FragmentRevision? = withContext(Dispatchers.IO) {
        fragmentRevisions[slug]?.lastOrNull()?.let { revisions[it] }
    }

    override suspend fun getRevisionAtVersion(slug: String, version: Int): FragmentRevision? = withContext(Dispatchers.IO) {
        fragmentRevisions[slug]?.mapNotNull { revisions[it] }?.firstOrNull { it.version == version }
    }

    override suspend fun compareRevisions(fromId: String, toId: String): String? = withContext(Dispatchers.IO) {
        val fromRevision = revisions[fromId]
        val toRevision = revisions[toId]

        if (fromRevision == null || toRevision == null) {
            return@withContext null
        }

        generateDiff(fromRevision, toRevision)
    }

    override suspend fun revertToRevision(slug: String, revisionId: String, changedBy: String?, reason: String?): Result<Fragment> = withContext(Dispatchers.IO) {
        val revision = revisions[revisionId]
        if (revision == null || revision.fragmentSlug != slug) {
            return@withContext Result.failure(IllegalArgumentException("Revision not found: $revisionId"))
        }

        val revertedFragment = Fragment(
            title = revision.title,
            slug = slug,
            status = FragmentStatus.PUBLISHED,
            date = LocalDateTime.now(),
            publishDate = null,
            preview = revision.preview,
            content = revision.content,
            frontMatter = revision.frontMatter,
            statusChangeHistory = emptyList()
        )

        return@withContext Result.success(revertedFragment)
    }

    override suspend fun deleteRevisions(slug: String): Int = withContext(Dispatchers.IO) {
        val ids = fragmentRevisions[slug] ?: return@withContext 0
        ids.forEach { revisions.remove(it) }
        fragmentRevisions.remove(slug)
        versionCounter.remove(slug)
        return@withContext ids.size
    }

    override suspend fun deleteRevisionsBefore(slug: String, before: LocalDateTime): Int = withContext(Dispatchers.IO) {
        val ids = fragmentRevisions[slug] ?: return@withContext 0
        val toRemove = ids.filter { revisions[it]?.changedAt?.isBefore(before) == true }
        toRemove.forEach { 
            revisions.remove(it)
            fragmentRevisions[slug]?.remove(it)
        }
        return@withContext toRemove.size
    }

    override suspend fun getRevisionCount(slug: String): Int = withContext(Dispatchers.IO) {
        fragmentRevisions[slug]?.size ?: 0
    }

    override suspend fun getAllRevisionSlugs(): List<String> = withContext(Dispatchers.IO) {
        fragmentRevisions.keys.toList()
    }

    fun clear() {
        revisions.clear()
        fragmentRevisions.clear()
        versionCounter.clear()
    }

    private fun generateDiff(from: FragmentRevision, to: Fragment): String {
        return buildString {
            if (from.title != to.title) {
                appendLine("Title changed from \"${from.title}\" to \"${to.title}\"")
            }
            if (from.content != to.content) {
                val contentDiff = simpleDiff(from.content, to.content)
                appendLine("Content changed:")
                append(contentDiff)
            }
        }
    }

    private fun generateDiff(from: FragmentRevision, to: FragmentRevision): String {
        return buildString {
            if (from.title != to.title) {
                appendLine("Title changed from \"${from.title}\" to \"${to.title}\"")
            }
            if (from.content != to.content) {
                val contentDiff = simpleDiff(from.content, to.content)
                appendLine("Content changed:")
                append(contentDiff)
            }
        }
    }

    private fun simpleDiff(original: String, modified: String): String {
        val originalLines = original.lines()
        val modifiedLines = modified.lines()
        val maxLines = maxOf(originalLines.size, modifiedLines.size)

        return buildString {
            for (i in 0 until maxLines) {
                val originalLine = originalLines.getOrNull(i)
                val modifiedLine = modifiedLines.getOrNull(i)

                when {
                    originalLine == null && modifiedLine != null -> appendLine("+ $modifiedLine")
                    originalLine != null && modifiedLine == null -> appendLine("- $originalLine")
                    originalLine != modifiedLine -> {
                        appendLine("- $originalLine")
                        appendLine("+ $modifiedLine")
                    }
                }
            }
        }
    }
}
