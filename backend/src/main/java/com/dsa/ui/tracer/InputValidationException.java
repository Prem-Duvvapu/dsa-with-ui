package com.dsa.ui.tracer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carries per-field messages so the UI can mark the offending editor rather than showing
 * one opaque error for the whole form.
 */
public class InputValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InputValidationException(Map<String, String> fieldErrors) {
        super("Invalid input: " + fieldErrors);
        this.fieldErrors = new LinkedHashMap<>(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
