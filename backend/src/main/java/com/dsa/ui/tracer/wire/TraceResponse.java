package com.dsa.ui.tracer.wire;

import com.dsa.ui.tracer.ExecutionTrace;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * What {@code /api/problems/{id}/execute} returns.
 *
 * <p>Separate from {@link ExecutionTrace} on purpose. The trace is what the runner produced
 * and what every contract test asserts against — complete steps, no encoding. This is how
 * that trace is packed for one particular caller, and {@code encoding} tells them which
 * packing they got.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TraceResponse {

    /** Steps carry only what changed; see {@link DeltaStep} for how to read them. */
    public static final String DELTA = "delta";

    /** Every step is a complete snapshot. The pre-delta shape, kept for one migration. */
    public static final String FULL = "full";

    private final String problemId;
    private final String encoding;
    private final Integer keyframeInterval;
    private final List<?> steps;
    private final int stepCount;
    private final boolean truncated;
    private final String truncationReason;
    private final int maxSteps;
    private final long maxBytes;
    private final String code;
    private final Map<String, Integer> anchors;
    private final Map<String, Object> resolvedInput;

    private TraceResponse(ExecutionTrace trace, String encoding, Integer keyframeInterval,
                          List<?> steps) {
        this.problemId = trace.getProblemId();
        this.encoding = encoding;
        this.keyframeInterval = keyframeInterval;
        this.steps = steps;
        this.stepCount = trace.getStepCount();
        this.truncated = trace.isTruncated();
        this.truncationReason = trace.getTruncationReason();
        this.maxSteps = trace.getMaxSteps();
        this.maxBytes = trace.getMaxBytes();
        this.code = trace.getCode();
        this.anchors = trace.getAnchors();
        this.resolvedInput = trace.getResolvedInput();
    }

    public static TraceResponse delta(ExecutionTrace trace) {
        return new TraceResponse(trace, DELTA, TraceEncoder.KEYFRAME_INTERVAL,
                TraceEncoder.encode(trace.getSteps()));
    }

    public static TraceResponse full(ExecutionTrace trace) {
        return new TraceResponse(trace, FULL, null, trace.getSteps());
    }

    /** Picks the encoding a caller asked for; anything unrecognised gets the default. */
    public static TraceResponse of(ExecutionTrace trace, String requested) {
        return FULL.equalsIgnoreCase(requested) ? full(trace) : delta(trace);
    }

    public String getProblemId() { return problemId; }
    public String getEncoding() { return encoding; }
    public Integer getKeyframeInterval() { return keyframeInterval; }
    public List<?> getSteps() { return steps; }

    /** Always the real number of steps, whichever encoding was used. */
    public int getStepCount() { return stepCount; }

    public boolean isTruncated() { return truncated; }
    public String getTruncationReason() { return truncationReason; }
    public int getMaxSteps() { return maxSteps; }
    public long getMaxBytes() { return maxBytes; }
    public String getCode() { return code; }
    public Map<String, Integer> getAnchors() { return anchors; }
    public Map<String, Object> getResolvedInput() { return resolvedInput; }
}
