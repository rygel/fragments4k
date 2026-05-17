package io.github.rygel.fragments

import java.io.File
import java.io.IOException

object PathSafety {
    val SLUG_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    const val SLUG_MAX_LENGTH = 128

    fun validateSlug(slug: String) {
        require(slug.isNotBlank()) { "Slug must not be blank" }
        require(slug.length <= SLUG_MAX_LENGTH) { "Slug exceeds maximum length of $SLUG_MAX_LENGTH" }
        require(SLUG_PATTERN.matches(slug)) { "Slug contains invalid characters: $slug" }
    }

    fun resolveAndCheck(baseDir: File, fileName: String): File {
        val baseCanonical = baseDir.canonicalPath
        val targetFile = File(baseDir, fileName)
        val targetCanonical = try {
            targetFile.canonicalPath
        } catch (e: IOException) {
            throw IllegalArgumentException("Invalid file path: '$fileName'", e)
        }
        require(targetCanonical == baseCanonical || targetCanonical.startsWith(baseCanonical + File.separator)) {
            "Path traversal detected: '$fileName' escapes base directory"
        }
        return targetFile
    }
}
