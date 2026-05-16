package io.github.rygel.fragments.adapter

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
) {
    companion object {
        fun badRequest(message: String) = ErrorResponse(400, "Bad Request", message)

        fun notFound(message: String) = ErrorResponse(404, "Not Found", message)

        fun internalError() = ErrorResponse(500, "Internal Server Error", "An unexpected error occurred")
    }
}
