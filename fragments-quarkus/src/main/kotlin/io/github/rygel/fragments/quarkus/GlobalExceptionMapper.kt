package io.github.rygel.fragments.quarkus

import io.github.rygel.fragments.adapter.ErrorResponse
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.slf4j.LoggerFactory

@Provider
class GlobalExceptionMapper : ExceptionMapper<Exception> {
    private val logger = LoggerFactory.getLogger(GlobalExceptionMapper::class.java)

    override fun toResponse(exception: Exception): Response =
        when (exception) {
            is IllegalArgumentException -> {
                logger.warn("Bad request: {}", exception.message)
                Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse(400, "Bad Request", exception.message ?: "Invalid request"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build()
            }

            is NoSuchElementException -> {
                logger.warn("Not found: {}", exception.message)
                Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse(404, "Not Found", exception.message ?: "Resource not found"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build()
            }

            else -> {
                logger.error("Unhandled exception", exception)
                Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponse(500, "Internal Server Error", "An unexpected error occurred"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build()
            }
        }
}
