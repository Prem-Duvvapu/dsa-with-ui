package com.dsa.ui.approach;

import java.util.Map;

/** Audited recurrence text plus notes keyed to actual event values or source anchors. */
public record ApproachTeaching(String state, String baseCases, String recurrence,
                               String evaluationOrder, String cacheKey,
                               Map<String, String> eventNotes, Map<String, String> anchorNotes) {
    public ApproachTeaching {
        requireText(state, "state");
        requireText(baseCases, "baseCases");
        requireText(recurrence, "recurrence");
        requireText(evaluationOrder, "evaluationOrder");
        if (cacheKey != null) requireText(cacheKey, "cacheKey");
        eventNotes = Map.copyOf(eventNotes);
        anchorNotes = Map.copyOf(anchorNotes);
        validateNotes(eventNotes);
        validateNotes(anchorNotes);
    }

    private static void validateNotes(Map<String, String> notes) {
        notes.forEach((key, value) -> {
            requireText(key, "note key");
            requireText(value, "note");
        });
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Teaching " + field + " must not be blank");
    }
}
