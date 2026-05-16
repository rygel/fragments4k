package io.github.rygel.fragments.micronaut

import io.github.rygel.fragments.adapter.ErrorResponse
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class GlobalExceptionHandler : ExceptionHandler<Exception, HttpResponse<ErrorResponse>> {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    override fun handle(
        request: HttpRequest<*>,
        exception: Exception,
    ): HttpResponse<ErrorResponse> =
        when (exception) {
            is IllegalArgumentException -> {
                logger.warn("Bad request: {}", exception.message)
                HttpResponse
                    .status<ErrorResponse>(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse(400, "Bad Request", exception.message ?: "Invalid request"))
            }

            is NoSuchElementException -> {
                logger.warn("Not found: {}", exception.message)
                HttpResponse
                    .status<ErrorResponse>(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse(404, "Not Found", exception.message ?: "Resource not found"))
            }

            else -> {
                logger.error("Unhandled exception", exception)
                HttpResponse
                    .status<ErrorResponse>(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse(500, "Internal Server Error", "An unexpected error occurred"))
            }
        }
}
