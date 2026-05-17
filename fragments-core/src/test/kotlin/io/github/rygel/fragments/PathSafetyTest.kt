package io.github.rygel.fragments

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PathSafetyTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun validateSlugAcceptsValidSlug() {
        PathSafety.validateSlug("my-post")
        PathSafety.validateSlug("hello-world-123")
        PathSafety.validateSlug("a")
    }

    @Test
    fun validateSlugRejectsBlank() {
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("") }
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("   ") }
    }

    @Test
    fun validateSlugRejectsPathTraversal() {
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("../etc/passwd") }
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("..") }
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("foo/../../bar") }
    }

    @Test
    fun validateSlugRejectsUppercase() {
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("My-Post") }
    }

    @Test
    fun validateSlugRejectsSpecialChars() {
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("my post") }
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("my.post") }
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug("my_post") }
    }

    @Test
    fun validateSlugRejectsOverMaxLength() {
        val longSlug = "a".repeat(PathSafety.SLUG_MAX_LENGTH + 1)
        assertThrows<IllegalArgumentException> { PathSafety.validateSlug(longSlug) }
    }

    @Test
    fun validateSlugAcceptsMaxLength() {
        val maxSlug = "a".repeat(PathSafety.SLUG_MAX_LENGTH)
        PathSafety.validateSlug(maxSlug)
    }

    @Test
    fun resolveAndCheckAcceptsValidFileName() {
        val result = PathSafety.resolveAndCheck(tempDir, "test.json")
        assertTrue(result.canonicalPath.startsWith(tempDir.canonicalPath))
    }

    @Test
    fun resolveAndCheckRejectsPathTraversal() {
        assertThrows<IllegalArgumentException> {
            PathSafety.resolveAndCheck(tempDir, "../../etc/passwd")
        }
    }

    @Test
    fun resolveAndCheckRejectsDotDot() {
        assertThrows<IllegalArgumentException> {
            PathSafety.resolveAndCheck(tempDir, "..")
        }
    }

    @Test
    fun resolveAndCheckRejectsAbsolutePath() {
        val absolutePath = if (File.separatorChar == '\\') {
            "C:\\Windows\\System32"
        } else {
            "/etc/passwd"
        }
        assertThrows<IllegalArgumentException> {
            PathSafety.resolveAndCheck(tempDir, absolutePath)
        }
    }

    @Test
    fun slugPatternMatchesExpectedValues() {
        assertTrue(PathSafety.SLUG_PATTERN.matches("hello"))
        assertTrue(PathSafety.SLUG_PATTERN.matches("hello-world"))
        assertTrue(PathSafety.SLUG_PATTERN.matches("a1b2c3"))
        assertTrue(PathSafety.SLUG_PATTERN.matches("my-post-v1-1234567890"))
        assertFalse(PathSafety.SLUG_PATTERN.matches(""))
        assertFalse(PathSafety.SLUG_PATTERN.matches("HELLO"))
        assertFalse(PathSafety.SLUG_PATTERN.matches("hello world"))
        assertFalse(PathSafety.SLUG_PATTERN.matches("../etc"))
    }
}
