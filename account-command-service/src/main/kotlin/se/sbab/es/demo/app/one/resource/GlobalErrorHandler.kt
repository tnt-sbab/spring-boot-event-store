package se.sbab.es.demo.app.one.resource

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.util.NoSuchElementException

@ControllerAdvice
class GlobalErrorHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NoSuchElementException::class)
    fun handleAccountNotFound(e: NoSuchElementException) =
        handleError(e, HttpStatus.NOT_FOUND)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException) =
        handleError(e, HttpStatus.BAD_REQUEST)

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(e: IllegalStateException) =
        handleError(e, HttpStatus.CONFLICT)

    @ExceptionHandler(HttpMessageConversionException::class)
    fun handleHttpMessageConversionException(e: HttpMessageConversionException) {
        throw e
    }

    @ExceptionHandler(Exception::class)
    fun handleUnknownException(e: Exception) =
        handleError(e, HttpStatus.INTERNAL_SERVER_ERROR)

    private fun handleError(e: Throwable, responseStatus: HttpStatus): ResponseEntity<ErrorBody> {
        logger.error(e.message)
        return ResponseEntity(
            ErrorBody(
                message = e.message,
            ),
            responseStatus,
        )
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
class ErrorBody(
    @Schema(description = "Description of error", required = false)
    val message: String?,
)
