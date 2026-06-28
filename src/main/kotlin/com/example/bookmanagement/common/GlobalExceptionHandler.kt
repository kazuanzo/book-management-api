package com.example.bookmanagement.common

import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(
    val message: String,
    val errors: Map<String, String>? = null,
)

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(e: MethodArgumentNotValidException): ErrorResponse {
        val errors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        return ErrorResponse("Validation failed", errors)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleNotReadable(e: HttpMessageNotReadableException): ErrorResponse = ErrorResponse("Malformed or missing request body")

    @ExceptionHandler(ResourceNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(e: ResourceNotFoundException): ErrorResponse = ErrorResponse(e.message ?: "Not found")

    @ExceptionHandler(BadRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(e: BadRequestException): ErrorResponse = ErrorResponse(e.message ?: "Bad request")

    @ExceptionHandler(InvalidStatusTransitionException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleInvalidTransition(e: InvalidStatusTransitionException): ErrorResponse =
        ErrorResponse(e.message ?: "Invalid status transition")
}
