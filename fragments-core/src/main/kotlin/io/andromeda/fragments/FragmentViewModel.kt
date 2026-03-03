package io.andromeda.fragments

data class FragmentViewModel(
    val fragment: Fragment,
    val isPartialRender: Boolean = false,
    val pageTitle: String? = null,
    val additionalContext: Map<String, Any> = emptyMap()
) {
    companion object {
        const val HTMX_REQUEST_HEADER = "HX-Request"
        const val HTMX_CURRENT_URL_HEADER = "HX-Current-URL"
    }

    fun fromHtmxRequest(headers: Map<String, String>): FragmentViewModel {
        val isHtmxRequest = headers[HTMX_REQUEST_HEADER]?.lowercase() == "true"
        return copy(isPartialRender = isHtmxRequest)
    }

    val title: String
        get() = pageTitle ?: fragment.title

    val content: String
        get() = fragment.content

    val preview: String
        get() = fragment.preview

    val slug: String
        get() = fragment.slug

    val template: String
        get() = fragment.template

    val date
        get() = fragment.date

    val tags
        get() = fragment.tags

    val categories
        get() = fragment.categories
}
