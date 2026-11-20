package com.parkhfx.common;

import java.util.LinkedHashMap;
import java.util.Map;

import com.parkhfx.auth.AuthenticationFailedException;
import com.parkhfx.auth.EmailAlreadyUsedException;
import com.parkhfx.booking.BookingConflictException;
import com.parkhfx.listing.ListingNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** One place that turns exceptions into consistent JSON error responses (RFC 9457). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BookingConflictException.class)
    ProblemDetail handleBookingConflict(BookingConflictException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Spot already booked");
        return problem;
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    ProblemDetail handleEmailTaken(EmailAlreadyUsedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Email already registered");
        return problem;
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ProblemDetail handleAuthFailed(AuthenticationFailedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        problem.setTitle("Authentication failed");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
        problem.setTitle("Invalid request");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    @ExceptionHandler(ListingNotFoundException.class)
    ProblemDetail handleListingNotFound(ListingNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Listing not found");
        return problem;
    }
}
