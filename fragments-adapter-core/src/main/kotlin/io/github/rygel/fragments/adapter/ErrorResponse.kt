package io.github.rygel.fragments.adapter

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
)
