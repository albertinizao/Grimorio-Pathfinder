package com.grimoriopathfinder.web.error;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(SpellRequestValidationException.class)
    public ProblemDetail handleBadRequest(SpellRequestValidationException ex) {
        return problemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(SpellQueryValidationException.class)
    public ProblemDetail handleUnprocessableEntity(SpellQueryValidationException ex) {
        return problemDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(SpellEditValidationException.class)
    public ProblemDetail handleEditValidation(SpellEditValidationException ex) {
        return problemDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(SpellListNotFoundException.class)
    public ProblemDetail handleListNotFound(SpellListNotFoundException ex) {
        return problemDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(SpellConflictException.class)
    public ProblemDetail handleConflict(SpellConflictException ex) {
        return problemDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SpellNotFoundException.class)
    public ProblemDetail handleNotFound(SpellNotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    private ProblemDetail problemDetail(HttpStatus status, String message) {
        var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setType(URI.create("about:blank"));
        return detail;
    }
}
