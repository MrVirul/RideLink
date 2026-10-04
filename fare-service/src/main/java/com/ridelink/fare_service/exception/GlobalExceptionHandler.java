package com.ridelink.fare_service.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
        String message = fieldError == null ? "Request validation failed" : fieldError.getDefaultMessage();
        return errorResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            Exception exception,
            HttpServletRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST, "Request contains invalid or malformed values", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return errorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> errorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}