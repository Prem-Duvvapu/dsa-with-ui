package com.dsa.ui.tracer;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.model.TreeNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a tracer writes its execution log to.
 *
 * <p>Steps are built fluently and named by code anchor, never by line number:
 *
 * <pre>
 * emit.at("loop.compare")
 *     .say("i = %d: sum %d beats maxi %d", i, sum, maxi)
 *     .var("i", i).var("sum", sum)
 *     .array(nums, i)
 *     .step();
 * </pre>
 *
 * <p>The emitter enforces the step budget, so a tracer never has to think about it.
 */
public final class StepEmitter {

    private final AnnotatedCode code;
    private final int maxSteps;
    private final List<ExecutionStep> steps = new ArrayList<>();
    private final List<String> callStack = new ArrayList<>();
    private String dsType = "Array";

    StepEmitter(AnnotatedCode code, int maxSteps) {
        this.code = code;
        this.maxSteps = maxSteps;
    }

    /** Sets the visualization mode for every subsequent step. */
    public StepEmitter using(String dsType) {
        this.dsType = dsType;
        return this;
    }

    public Step at(String anchor) {
        return new Step(code.resolve(anchor));
    }

    /** Pushes a frame onto the call stack shown alongside recursive traces. */
    public void push(String frame) {
        callStack.add(frame);
    }

    public void pop() {
        if (!callStack.isEmpty()) {
            callStack.remove(callStack.size() - 1);
        }
    }

    public int count() {
        return steps.size();
    }

    List<ExecutionStep> collected() {
        return steps;
    }

    /** One step under construction. */
    public final class Step {
        private final int line;
        private String description = "";
        private final Map<String, String> variables = new LinkedHashMap<>();
        private List<ArrayElement> arrayState;
        private int[][] gridState;
        private List<ListNode> listState;
        private List<TreeNode> treeNodes;
        private Map<Integer, String> nodeStates;
        private List<String> activeEdges;

        private Step(int line) {
            this.line = line;
        }

        /** The human-readable narration. Accepts {@link String#format} arguments. */
        public Step say(String format, Object... args) {
            this.description = args.length == 0 ? format : String.format(format, args);
            return this;
        }

        public Step var(String name, Object value) {
            variables.put(name, String.valueOf(value));
            return this;
        }

        /** Highlights up to two positions in the array being traced. */
        public Step array(int[] values, int primary, int secondary) {
            List<ArrayElement> state = new ArrayList<>(values.length);
            for (int i = 0; i < values.length; i++) {
                String s = i == primary ? "current" : i == secondary ? "target" : "default";
                state.add(new ArrayElement(i, values[i], s));
            }
            this.arrayState = state;
            return this;
        }

        public Step array(int[] values, int primary) {
            return array(values, primary, -1);
        }

        public Step array(int[] values) {
            return array(values, -1, -1);
        }

        /** Full control over per-index states when the four defaults are not enough. */
        public Step arrayState(List<ArrayElement> state) {
            this.arrayState = state;
            return this;
        }

        public Step grid(int[][] values) {
            int[][] copy = new int[values.length][];
            for (int r = 0; r < values.length; r++) {
                copy[r] = values[r].clone();
            }
            this.gridState = copy;
            return this;
        }

        public Step list(List<ListNode> nodes) {
            this.listState = nodes;
            return this;
        }

        public Step tree(List<TreeNode> nodes) {
            this.treeNodes = nodes;
            return this;
        }

        public Step nodes(Map<Integer, String> states) {
            this.nodeStates = new LinkedHashMap<>(states);
            return this;
        }

        public Step edges(List<String> active) {
            this.activeEdges = List.copyOf(active);
            return this;
        }

        /** Commits the step. Throws once the budget is spent. */
        public void step() {
            if (steps.size() >= maxSteps) {
                throw new StepBudgetExceededException(maxSteps);
            }
            steps.add(new ExecutionStep(
                    steps.size() + 1,
                    line,
                    description,
                    List.copyOf(callStack),
                    nodeStates == null ? Map.of() : nodeStates,
                    activeEdges == null ? List.of() : activeEdges,
                    new LinkedHashMap<>(variables),
                    dsType,
                    gridState,
                    arrayState,
                    listState,
                    null,
                    treeNodes
            ));
        }
    }
}
