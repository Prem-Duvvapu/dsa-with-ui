package com.dsa.ui.approach;

import com.dsa.ui.model.*;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Actual minimum-energy recursion; optional memoization caches zero as well as nonzero costs. */
public final class FrogJumpRecursiveTracer implements AlgorithmTracer {
    private final boolean memoized;
    private final String code;

    public FrogJumpRecursiveTracer(boolean memoized) {
        this.memoized = memoized;
        code = SolutionSource.read("/solutions/dp-approaches/frog-jump/"
                + (memoized ? "memoization" : "recursion") + ".java");
    }
    public String id() { return "frog-jump"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return code; }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("heights", FieldType.INT_ARRAY).label("Stair heights")
                .length(2, memoized ? 20 : 10).values(0, 999).defaultValue(List.of(10, 50, 40, 30))
                .help(memoized ? "The recursive minimum-energy states are computed once; a cached zero is still known."
                        : "Repeated energy states are recomputed. Up to 10 heights keeps every call visible.").build());
    }
    public Map<String, Object> alternateInput() { return Map.of("heights", List.of(30, 10, 60, 10, 20)); }

    public void run(Inputs input, StepEmitter emit) {
        int[] heights = input.getIntArray("heights");
        Integer[] memo = memoized ? new Integer[heights.length] : null;
        Counters counters = new Counters();
        event(emit, memoized ? "init" : "start", "init", -1, heights.length - 1, null, memo, counters,
                memoized ? "Allocate memo energy: every entry is unknown, not zero."
                        : "Find the minimum energy to reach stair " + (heights.length - 1) + ". Repeated states will not be cached.");
        if (memoized) event(emit, "start", "start", -1, heights.length - 1, null, memo, counters,
                "Call energy(" + (heights.length - 1) + ", heights, memo).");
        int answer = energy(heights.length - 1, heights, memo, counters, emit);
        step(emit, "start", "done", -1, heights.length - 1, answer, memo, counters,
                "All calls returned. Minimum total energy to reach stair " + (heights.length - 1) + " is " + answer + ".")
                .var("answer", answer).step();
    }

    private int energy(int i, int[] heights, Integer[] memo, Counters counters, StepEmitter emit) {
        int call = ++counters.calls;
        emit.push("energy(" + i + ") #" + call);
        int answer;
        String returnAnchor = memoized ? "result" : "combine";
        try {
            // Emit before descent so reaching the trace budget stops computation, not just recording.
            event(emit, "enter", "enter", call, i, null, memo, counters,
                    "Enter call " + call + ": energy(" + i + "). Stair height is " + heights[i] + ".");
            if (memo != null && memo[i] != null) {
                answer = memo[i];
                counters.hits++;
                returnAnchor = "hit";
                event(emit, "hit", "cache-hit", call, i, answer, memo, counters,
                        "memo[" + i + "] already records energy " + answer + ". Return it without expanding children.");
            } else if (i == 0) {
                answer = 0;
                if (memo != null) memo[i] = answer;
                counters.evaluations++;
                counters.computed.add(i);
                returnAnchor = memoized ? "base-return" : "base";
                event(emit, "base", "base", call, i, answer, memo, counters,
                        "Starting on stair 0 costs zero energy; no jump has been taken.");
            } else {
                int oneGap = Math.abs(heights[i] - heights[i - 1]);
                step(emit, "one", "call-one", call, i, null, memo, counters,
                        "Arrive from stair " + (i - 1) + ": first compute its energy, then add height gap " + oneGap + ".")
                        .var("from", i - 1).var("heightGap", oneGap).step();
                int one = energy(i - 1, heights, memo, counters, emit) + oneGap;
                int two = Integer.MAX_VALUE;
                if (i > 1) {
                    int twoGap = Math.abs(heights[i] - heights[i - 2]);
                    step(emit, "two", "call-two", call, i, null, memo, counters,
                            "Or arrive from stair " + (i - 2) + ": compute its energy, then add height gap " + twoGap + ".")
                            .var("from", i - 2).var("heightGap", twoGap).step();
                    two = energy(i - 2, heights, memo, counters, emit) + twoGap;
                }
                answer = Math.min(one, two);
                if (memo != null) memo[i] = answer;
                counters.evaluations++;
                counters.computed.add(i);
                String candidates = i == 1 ? "Stair 1 has only one predecessor: total energy " + one + "."
                        : "One-step total " + one + ", two-step total " + two + "; choose minimum " + answer + ".";
                step(emit, memoized ? "store" : "combine", memoized ? "store" : "combine", call, i, answer, memo, counters,
                        candidates + (memoized ? " Store this cost in memo[" + i + "]." : " This call does not cache its result."))
                        .var("jumpOne", one).var("jumpTwo", i > 1 ? two : "not reachable").step();
            }
        } finally { emit.pop(); }
        event(emit, returnAnchor, "return", call, i, answer, memo, counters,
                "Call " + call + " returned minimum energy " + answer + ". Its frame has left the stack.");
        return answer;
    }

    private static final class Counters {
        int calls, hits, evaluations;
        final Set<Integer> computed = new HashSet<>();
    }
    private void event(StepEmitter emit, String anchor, String event, int call, int state, Integer value,
                       Integer[] memo, Counters counters, String description) {
        step(emit, anchor, event, call, state, value, memo, counters, description).step();
    }
    private StepEmitter.Step step(StepEmitter emit, String anchor, String event, int call, int state, Integer value,
                                  Integer[] memo, Counters counters, String description) {
        var step = emit.at(anchor).say(description).var("event", event).var("state", state)
                .var("calls", counters.calls).var("cacheHits", counters.hits)
                .var("stateEvaluations", counters.evaluations).var("computedStates", counters.computed.size());
        if (call >= 0) step.var("callId", call);
        if (value != null) step.var("value", value);
        if (memo != null) step.dpTable(table(memo, state, event));
        return step;
    }
    private static DpTable table(Integer[] memo, int state, String event) {
        List<String> columns = new ArrayList<>();
        List<DpCell> cells = new ArrayList<>();
        for (int i = 0; i < memo.length; i++) {
            columns.add(String.valueOf(i));
            String colour = memo[i] == null ? "void" : "known";
            if (i == state && memo[i] != null) colour = "cache-hit".equals(event) ? "read"
                    : Set.of("store", "base").contains(event) ? "probe" : "known";
            cells.add(new DpCell(memo[i] == null ? "·" : String.valueOf(memo[i]), colour));
        }
        return new DpTable(List.of("memo energy"), columns, List.of(cells));
    }
}
