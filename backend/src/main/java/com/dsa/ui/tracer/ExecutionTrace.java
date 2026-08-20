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
    private final int maxSteps;
    private final String code;
    private final Map<String, Integer> anchors;
    private final Map<String, Object> resolvedInput;

    public ExecutionTrace(String problemId, List<ExecutionStep> steps, boolean truncated,
                          int maxSteps, String code, Map<String, Integer> anchors,
                          Map<String, Object> resolvedInput) {
        this.problemId = problemId;
        this.steps = steps;
        this.truncated = truncated;
        this.maxSteps = maxSteps;
        this.code = code;
        this.anchors = anchors;
        this.resolvedInput = resolvedInput;
    }

    public String getProblemId() { return problemId; }
    public List<ExecutionStep> getSteps() { return steps; }
    public int getStepCount() { return steps.size(); }

    /** True when the run hit {@link InputSpec#getMaxSteps()} and was stopped early. */
    public boolean isTruncated() { return truncated; }

    public int getMaxSteps() { return maxSteps; }

    /** The source with anchor markers stripped — what the code viewer should render. */
    public String getCode() { return code; }

    public Map<String, Integer> getAnchors() { return anchors; }

    /** The input actually used, with defaults filled in. */
    public Map<String, Object> getResolvedInput() { return resolvedInput; }
}
