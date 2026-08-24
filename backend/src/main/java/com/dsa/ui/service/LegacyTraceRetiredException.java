package com.dsa.ui.service;

/**
 * A problem id whose real execution now lives in an {@code AlgorithmTracer}
 * (tracer/impl). Its legacy generator is gone on purpose: serving anything here would
 * stream another algorithm's animation under this id's name — the exact defect the
 * tracer layer exists to prevent.
 *
 * <p>{@code ApiExceptionHandler} answers 410 Gone so callers are pointed at
 * {@code /api/problems/{id}/execute} instead of receiving substitute steps.
 */
public class LegacyTraceRetiredException extends RuntimeException {

    private final String problemId;

    public LegacyTraceRetiredException(String problemId) {
        super("Problem '" + problemId
                + "' has a real tracer in /api/problems — the legacy trace for this id is retired.");
        this.problemId = problemId;
    }

    public String getProblemId() {
        return problemId;
    }
}
