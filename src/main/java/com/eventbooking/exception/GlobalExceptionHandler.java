package com.eventbooking.exception;

import com.eventbooking.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
                return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
                return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }

        @ExceptionHandler(ValidationException.class)
        public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
                return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        @ExceptionHandler(InsufficientTicketsException.class)
        public ResponseEntity<ErrorResponse> handleInsufficientTickets(InsufficientTicketsException ex) {
                return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
        }

        @ExceptionHandler(BookingExpiredException.class)
        public ResponseEntity<ErrorResponse> handleBookingExpired(BookingExpiredException ex) {
                return buildResponse(HttpStatus.GONE, ex.getMessage());
        }

        @ExceptionHandler(PaymentFailedException.class)
        public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex) {
                return buildResponse(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
                List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                                .map(FieldError::getDefaultMessage)
                                .collect(Collectors.toList());

                ErrorResponse response = ErrorResponse.builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .message("Validation failed")
                                .timestamp(LocalDateTime.now())
                                .errors(errors)
                                .build();

                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }

        private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
                ErrorResponse response = ErrorResponse.builder()
                                .status(status.value())
                                .message(message)
                                .timestamp(LocalDateTime.now())
                                .build();
                return new ResponseEntity<>(response, status);
        }
}
