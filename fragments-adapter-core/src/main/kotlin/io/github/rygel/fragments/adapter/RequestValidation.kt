package io.github.rygel.fragments.adapter

data class ValidationResult<T>(
    val value: T,
    val isValid: Boolean = true,
    val errorMessage: String? = null,
)

object RequestValidation {
    private val SLUG_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    private const val SLUG_MAX_LENGTH = 128
    private const val QUERY_MAX_LENGTH = 500
    private const val PAGE_MIN = 1
    private const val PAGE_MAX = 10000
    private const val AUTOCOMPLETE_MIN = 1
    private const val AUTOCOMPLETE_MAX = 50
    private const val MAX_RESULTS_MIN = 1
    private const val MAX_RESULTS_MAX = 100
    private const val YEAR_MIN = 1970
    private const val YEAR_MAX = 3000

    fun validateSlug(slug: String): ValidationResult<String> {
        if (slug.isBlank()) {
            return ValidationResult(slug, isValid = false, errorMessage = "Slug must not be blank")
        }
        if (slug.length > SLUG_MAX_LENGTH) {
            return ValidationResult(slug, isValid = false, errorMessage = "Slug exceeds maximum length of $SLUG_MAX_LENGTH")
        }
        if (!SLUG_PATTERN.matches(slug)) {
            return ValidationResult(slug, isValid = false, errorMessage = "Slug contains invalid characters")
        }
        return ValidationResult(slug)
    }

    fun validateTag(tag: String): ValidationResult<String> = validateSlugLike(tag, "Tag")

    fun validateCategory(category: String): ValidationResult<String> = validateSlugLike(category, "Category")

    fun validateAuthorId(authorId: String): ValidationResult<String> = validateSlugLike(authorId, "Author ID")

    fun validatePage(page: Int): ValidationResult<Int> {
        val clamped = page.coerceIn(PAGE_MIN, PAGE_MAX)
        return ValidationResult(clamped)
    }

    fun validateMonth(month: Int): ValidationResult<Int> {
        if (month < 1 || month > 12) {
            return ValidationResult(month, isValid = false, errorMessage = "Month must be between 1 and 12")
        }
        return ValidationResult(month)
    }

    fun validateYear(year: Int): ValidationResult<Int> {
        if (year < YEAR_MIN || year > YEAR_MAX) {
            return ValidationResult(year, isValid = false, errorMessage = "Year must be between $YEAR_MIN and $YEAR_MAX")
        }
        return ValidationResult(year)
    }

    fun validateSearchQuery(query: String): ValidationResult<String> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(trimmed, isValid = false, errorMessage = "Search query must not be blank")
        }
        if (trimmed.length > QUERY_MAX_LENGTH) {
            return ValidationResult(trimmed, isValid = false, errorMessage = "Search query exceeds maximum length of $QUERY_MAX_LENGTH")
        }
        return ValidationResult(trimmed)
    }

    fun validateAutocompleteLimit(limit: Int): ValidationResult<Int> {
        val clamped = limit.coerceIn(AUTOCOMPLETE_MIN, AUTOCOMPLETE_MAX)
        return ValidationResult(clamped)
    }

    fun validateMaxResults(maxResults: Int): ValidationResult<Int> {
        val clamped = maxResults.coerceIn(MAX_RESULTS_MIN, MAX_RESULTS_MAX)
        return ValidationResult(clamped)
    }

    private fun validateSlugLike(
        value: String,
        fieldName: String,
    ): ValidationResult<String> {
        if (value.isBlank()) {
            return ValidationResult(value, isValid = false, errorMessage = "$fieldName must not be blank")
        }
        if (value.length > SLUG_MAX_LENGTH) {
            return ValidationResult(value, isValid = false, errorMessage = "$fieldName exceeds maximum length of $SLUG_MAX_LENGTH")
        }
        if (!SLUG_PATTERN.matches(value)) {
            return ValidationResult(value, isValid = false, errorMessage = "$fieldName contains invalid characters")
        }
        return ValidationResult(value)
    }
}
