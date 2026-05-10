package io.github.rygel.fragments.adapter

/** Configuration for the search form rendered on the search page. */
data class SearchFormConfig(
    val actionUrl: String,
    val paramName: String,
    val placeholderText: String,
    val buttonText: String,
    val method: String = "get",
)
