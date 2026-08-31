package com.dsa.ui.tracer.wire;

import com.dsa.ui.model.ExecutionStep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Turns a list of full snapshots into a delta stream.
 *
 * <p>Encoding happens at the boundary rather than inside {@link com.dsa.ui.tracer.StepEmitter}
 * so that a tracer, a golden file and every contract test keep working against complete
 * steps. How the trace is packed for transport is a presentation concern.
 */
public final class TraceEncoder {

    /**
     * Steps between keyframes. A viewer scrubbing to step 900 replays from step 900 back to
     * the nearest keyframe rather than from the beginning, so this bounds the work a jump
     * costs; smaller means faster seeks and a larger payload.
     */
    public static final int KEYFRAME_INTERVAL = 50;

    private TraceEncoder() {
    }

    public static List<DeltaStep> encode(List<ExecutionStep> steps) {
        return encode(steps, KEYFRAME_INTERVAL);
    }

    public static List<DeltaStep> encode(List<ExecutionStep> steps, int keyframeInterval) {
        List<DeltaStep> encoded = new ArrayList<>(steps.size());
        ExecutionStep previous = null;

        for (int i = 0; i < steps.size(); i++) {
            ExecutionStep step = steps.get(i);

            // A field that goes back to null cannot be expressed by omission, because
            // omission already means "unchanged". Promote the step to a keyframe instead of
            // growing the wire format to say "clear this one".
            boolean keyframe = previous == null
                    || i % keyframeInterval == 0
                    || wentNull(previous, step);

            ExecutionStep baseline = keyframe ? null : previous;
            encoded.add(new DeltaStep(
                    step.getStepNumber(),
                    step.getActiveLine(),
                    step.getDescription(),
                    keyframe ? Boolean.TRUE : null,
                    changed(baseline, step, ExecutionStep::getQueueOrStackState),
                    changed(baseline, step, ExecutionStep::getCallStack),
                    changed(baseline, step, ExecutionStep::getNodeStates),
                    changed(baseline, step, ExecutionStep::getActiveEdges),
                    changed(baseline, step, ExecutionStep::getVariables),
                    changed(baseline, step, ExecutionStep::getDsType),
                    changedGrid(baseline, step),
                    changed(baseline, step, ExecutionStep::getArrayState),
                    changed(baseline, step, ExecutionStep::getListState),
                    changed(baseline, step, ExecutionStep::getTrieState),
                    changed(baseline, step, ExecutionStep::getTreeNodes),
                    changed(baseline, step, ExecutionStep::getGraphNodes),
                    changed(baseline, step, ExecutionStep::getGraphEdges),
                    changed(baseline, step, ExecutionStep::getDpTable)));

            previous = step;
        }
        return encoded;
    }

    /** The value when it differs from the baseline, null when it does not. */
    private static <T> T changed(ExecutionStep baseline, ExecutionStep step,
                                 Function<ExecutionStep, T> field) {
        T value = field.apply(step);
        if (baseline == null) {
            return value;
        }
        return Objects.equals(value, field.apply(baseline)) ? null : value;
    }

    /** int[][] needs deep comparison; Objects.equals would compare references. */
    private static int[][] changedGrid(ExecutionStep baseline, ExecutionStep step) {
        int[][] value = step.getGridState();
        if (baseline == null) {
            return value;
        }
        return Arrays.deepEquals(value, baseline.getGridState()) ? null : value;
    }

    private static boolean wentNull(ExecutionStep previous, ExecutionStep step) {
        return nulled(previous.getQueueOrStackState(), step.getQueueOrStackState())
                || nulled(previous.getCallStack(), step.getCallStack())
                || nulled(previous.getNodeStates(), step.getNodeStates())
                || nulled(previous.getActiveEdges(), step.getActiveEdges())
                || nulled(previous.getVariables(), step.getVariables())
                || nulled(previous.getDsType(), step.getDsType())
                || nulled(previous.getGridState(), step.getGridState())
                || nulled(previous.getArrayState(), step.getArrayState())
                || nulled(previous.getListState(), step.getListState())
                || nulled(previous.getTrieState(), step.getTrieState())
                || nulled(previous.getTreeNodes(), step.getTreeNodes())
                || nulled(previous.getGraphNodes(), step.getGraphNodes())
                || nulled(previous.getGraphEdges(), step.getGraphEdges())
                || nulled(previous.getDpTable(), step.getDpTable());
    }

    private static boolean nulled(Object before, Object after) {
        return before != null && after == null;
    }
}
