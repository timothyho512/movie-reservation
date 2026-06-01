package com.example.moviereservation.Exception;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<ApiError> handleSeatUnavailable(
            SeatUnavailableException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiError> handleIdempotencyConflict(
            IdempotencyConflictException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(CheckoutExpiredException.class)
    public ResponseEntity<ApiError> handleCheckoutExpired(
                CheckoutExpiredException ex,
                HttpServletRequest request
        ) {
        return buildErrorResponse(
                HttpStatus.GONE,
                ex.getMessage(),
                request.getRequestURI()
        );
        }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "A database constraint was violated",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error",
                request.getRequestURI()
        );
    }

        @ExceptionHandler(DuplicateEmailException.class)
        public ResponseEntity<ApiError> handleDuplicateEmail(
                DuplicateEmailException ex,
                HttpServletRequest request
        ) {
                return buildErrorResponse(
                        HttpStatus.CONFLICT,
                        ex.getMessage(),
                        request.getRequestURI()
                );
        }

        @ExceptionHandler(AuthenticationFailedException.class)
        public ResponseEntity<ApiError> handleAuthenticationFailed(
                AuthenticationFailedException ex,
                HttpServletRequest request
        ) {
                return buildErrorResponse(
                        HttpStatus.UNAUTHORIZED,
                        ex.getMessage(),
                        request.getRequestURI()
                );
        }


    private ResponseEntity<ApiError> buildErrorResponse(
            HttpStatus status,
            String message,
            String path
    ) {
        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );

        return new ResponseEntity<>(error, status);
    }
}
