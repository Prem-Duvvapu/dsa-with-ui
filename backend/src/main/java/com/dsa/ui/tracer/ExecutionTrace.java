package com.dsa.ui.tracer;

import com.dsa.ui.model.ExecutionStep;

import java.util.List;
import java.util.Map;

/**
 * A completed run: the steps, the code they highlight, and the input that produced them.
 *
 * <p>Richer than the legacy bare {@code List<ExecutionStep>} because a user-driven run
 * has to report things a fixture-driven one never did — whether the trace was cut short,
 * and which input actually ran after defaults were filled in.
 */
public final class ExecutionTrace {

    private final String problemId;
    private final List<ExecutionStep> steps;
    private final boolean truncated;
    private final String truncationReason;
    private final int maxSteps;
    private final long maxBytes;
    private final String code;
    private final Map<String, Integer> anchors;
    private final Map<String, Object> resolvedInput;

    public ExecutionTrace(String problemId, List<ExecutionStep> steps, boolean truncated,
                          String truncationReason, int maxSteps, long maxBytes, String code,
                          Map<String, Integer> anchors, Map<String, Object> resolvedInput) {
        this.problemId = problemId;
        this.steps = steps;
        this.truncated = truncated;
        this.truncationReason = truncationReason;
        this.maxSteps = maxSteps;
        this.maxBytes = maxBytes;
        this.code = code;
        this.anchors = anchors;
        this.resolvedInput = resolvedInput;
    }

    public String getProblemId() { return problemId; }
    public List<ExecutionStep> getSteps() { return steps; }
    public int getStepCount() { return steps.size(); }

    /**
     * True when the run hit either ceiling and was stopped early.
     *
     * <p>One flag, not two. A caller only ever needs to know the trace is incomplete;
     * {@link #getTruncationReason()} says which limit stopped it.
     */
    public boolean isTruncated() { return truncated; }

    /** Plain-language reason, safe to show a user. Null when the trace ran to completion. */
    public String getTruncationReason() { return truncationReason; }

    public int getMaxSteps() { return maxSteps; }

    public long getMaxBytes() { return maxBytes; }

    /** The source with anchor markers stripped — what the code viewer should render. */
    public String getCode() { return code; }

    public Map<String, Integer> getAnchors() { return anchors; }

    /** The input actually used, with defaults filled in. */
    public Map<String, Object> getResolvedInput() { return resolvedInput; }
}
