package io.github.rygel.fragments.javalin

object SearchFormGenerator {
    fun generate(
        actionUrl: String = "/search",
        paramName: String = "q",
        placeholderText: String = "Search articles...",
        buttonText: String = "Search",
        method: String = "get"
    ): SearchFormConfig {
        return SearchFormConfig(
            actionUrl = actionUrl,
            paramName = paramName,
            placeholderText = placeholderText,
            buttonText = buttonText,
            method = method
        )
    }
}
