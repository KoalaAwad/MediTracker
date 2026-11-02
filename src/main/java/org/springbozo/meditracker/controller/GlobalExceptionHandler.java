package org.springbozo.meditracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Turns uncaught exceptions into JSON with a generic message. Details go to the log only,
 * so stack traces, SQL and class names never reach the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN);
    }

    // unreadable JSON body, or a path/query value of the wrong type
    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleBadInput(Exception ex) {
        log.debug("Bad request input: {}", ex.toString());
        return error(HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAll(Exception ex) {
        // Spring's own request errors (bad JSON, missing header, wrong method...) carry their status
        if (ex instanceof ErrorResponse er && er.getStatusCode().value() < 500) {
            log.debug("Request error: {}", ex.toString());
            return error(er.getStatusCode());
        }
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    static ResponseEntity<Map<String, String>> error(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        String message = status.value() >= 500 ? "Internal server error"
                : known != null ? known.getReasonPhrase() : "Request failed";
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
