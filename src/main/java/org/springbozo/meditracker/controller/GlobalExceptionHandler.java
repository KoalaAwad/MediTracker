package org.springbozo.meditracker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private ModelAndView buildErrorView(HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", status.value());
        mav.addObject("errorMessage", message);
        return mav;
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ModelAndView handleNotFound(NoHandlerFoundException ex) {
        return buildErrorView(HttpStatus.NOT_FOUND, "The requested page was not found.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return buildErrorView(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not allowed for this endpoint.");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ModelAndView handleBadRequest(Exception ex) {
        return buildErrorView(HttpStatus.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "Bad request");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex) {
        return buildErrorView(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllExceptions(Exception exception) {
        String errorMsg = null;
        if (exception.getMessage() != null) {
            errorMsg = exception.getMessage();
        } else if (exception.getCause() != null) {
            errorMsg = exception.getCause().toString();
        } else {
            errorMsg = exception.toString();
        }
        return buildErrorView(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
    }
}

