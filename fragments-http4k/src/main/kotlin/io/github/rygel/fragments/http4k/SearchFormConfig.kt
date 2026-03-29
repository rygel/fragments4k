package io.github.rygel.fragments.http4k

data class SearchFormConfig(
    val actionUrl: String,
    val paramName: String,
    val placeholderText: String,
    val buttonText: String,
    val method: String = "get"
)
