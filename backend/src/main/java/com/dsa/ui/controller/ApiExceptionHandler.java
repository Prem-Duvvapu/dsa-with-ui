package com.dsa.ui.controller;

import com.dsa.ui.tracer.InputValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns input rejection into a response the UI can attach to individual editors.
 *
 * <p>Without this Spring would answer 500 for a user typing an unsorted array into a
 * binary search — a caller mistake reported as a server fault.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InputValidationException.class)
    public ResponseEntity<Map<String, Object>> onInvalidInput(InputValidationException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "invalid_input");
        body.put("message", "Some inputs need fixing.");
        body.put("fieldErrors", e.getFieldErrors());
        return ResponseEntity.badRequest().body(body);
    }
}
