package io.github.rygel.fragments

import java.time.LocalDateTime

interface FragmentRevisionRepository {
    suspend fun saveRevision(fragment: Fragment, changedBy: String? = null, reason: String? = null): FragmentRevision
    suspend fun getRevisions(slug: String): List<FragmentRevision>
    suspend fun getRevision(id: String): FragmentRevision?
    suspend fun getLatestRevision(slug: String): FragmentRevision?
    suspend fun getRevisionAtVersion(slug: String, version: Int): FragmentRevision?
    suspend fun compareRevisions(fromId: String, toId: String): String?
    suspend fun revertToRevision(slug: String, revisionId: String, changedBy: String? = null, reason: String? = null): Result<Fragment>
    suspend fun deleteRevisions(slug: String): Int
    suspend fun deleteRevisionsBefore(slug: String, before: LocalDateTime): Int
    suspend fun getRevisionCount(slug: String): Int
    suspend fun getAllRevisionSlugs(): List<String>
}
