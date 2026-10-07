package com.dsa.ui.approach;

import com.dsa.ui.model.*;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Non-bean alternatives. The same recursive state gains an optional actual memo cache. */
public final class ClimbingStairsRecursiveTracer implements AlgorithmTracer {
    private final boolean memoized;
    private final String code;

    public ClimbingStairsRecursiveTracer(boolean memoized) {
        this.memoized = memoized;
        code = SolutionSource.read("/solutions/dp-approaches/climbing-stairs/"
                + (memoized ? "memoization" : "recursion") + ".java");
    }
    public String id() { return "climbing-stairs"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return code; }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("n", FieldType.INT).label("Stairs").range(1, memoized ? 30 : 10)
                .defaultValue(5).help(memoized
                        ? "Each recursive state is computed once, then reused from memo."
                        : "Repeated states are recomputed. Up to 10 stairs keeps every call visible.").build());
    }
    public Map<String, Object> alternateInput() { return Map.of("n", 6); }

    public void run(Inputs input, StepEmitter emit) {
        int n = input.getInt("n");
        Integer[] memo = memoized ? new Integer[n + 1] : null;
        Counters counters = new Counters();
        event(emit, "init", "init", -1, n, null, memo, counters,
                memoized ? "Allocate memo: every entry is unknown, not zero."
                        : "Start the recursive definition. No results will be cached.");
        if (memoized) event(emit, "start", "start", -1, n, null, memo, counters, "Call ways(" + n + ", memo).");
        int answer = ways(n, memo, counters, emit);
        var done = eventBuilder(emit, memoized ? "start" : "init", "done", -1, n, answer, memo, counters,
                "All calls have returned. There are " + answer + " ways to climb " + n + " stairs.");
        done.var("answer", answer).step();
    }

    private int ways(int n, Integer[] memo, Counters counters, StepEmitter emit) {
        int call = ++counters.calls;
        emit.push("ways(" + n + ") #" + call);
        String returnAnchor = memoized ? "result" : "combine";
        int answer;
        try {
            // Every invocation emits before expanding, so trace budgets bound computation.
            event(emit, "enter", "enter", call, n, null, memo, counters, "Enter call " + call + ": ways(" + n + ").");
            if (memo != null && memo[n] != null) {
                counters.hits++;
                answer = memo[n];
                returnAnchor = "hit";
                event(emit, "hit", "cache-hit", call, n, answer, memo, counters,
                        "memo[" + n + "] is already " + answer + ". Return it without expanding children.");
            } else if (n <= 1) {
                answer = 1;
                if (memo != null) memo[n] = answer;
                counters.evaluations++;
                counters.computed.add(n);
                returnAnchor = memoized ? "base-return" : "base";
                event(emit, "base", "base", call, n, answer, memo, counters,
                        "ways(" + n + ") = 1: " + (n == 0 ? "take no further steps." : "take one step."));
            } else {
                event(emit, "left", "call-left", call, n, null, memo, counters, "Reach this stair with a one-step move: compute ways(" + (n - 1) + ").");
                int oneStep = ways(n - 1, memo, counters, emit);
                event(emit, "right", "call-right", call, n, null, memo, counters, "Or arrive with a two-step move: compute ways(" + (n - 2) + ").");
                int twoSteps = ways(n - 2, memo, counters, emit);
                answer = oneStep + twoSteps;
                if (memo != null) memo[n] = answer;
                counters.evaluations++;
                counters.computed.add(n);
                event(emit, memoized ? "store" : "combine", memoized ? "store" : "combine", call, n, answer, memo, counters,
                        "ways(" + n + ") = " + oneStep + " + " + twoSteps + " = " + answer
                                + (memoized ? "; store the result in memo[" + n + "]." : "; this call has recomputed its own result."));
            }
        } finally {
            emit.pop();
        }
        event(emit, returnAnchor, "return", call, n, answer, memo, counters,
                "Call " + call + " returned " + answer + ". Its frame has left the stack.");
        return answer;
    }

    private static final class Counters {
        int calls, hits, evaluations;
        final Set<Integer> computed = new HashSet<>();
    }
    private void event(StepEmitter emit, String anchor, String kind, int call, int n, Integer value,
                       Integer[] memo, Counters counters, String description) {
        eventBuilder(emit, anchor, kind, call, n, value, memo, counters, description).step();
    }
    private StepEmitter.Step eventBuilder(StepEmitter emit, String anchor, String kind, int call, int n,
                                          Integer value, Integer[] memo, Counters counters, String description) {
        var step = emit.at(anchor).say(description).var("event", kind).var("state", n)
                .var("calls", counters.calls).var("cacheHits", counters.hits)
                .var("stateEvaluations", counters.evaluations).var("computedStates", counters.computed.size());
        if (call >= 0) step.var("callId", call);
        if (value != null) step.var("value", value);
        if (memo != null) step.dpTable(table(memo, n, kind));
        return step;
    }
    private static DpTable table(Integer[] memo, int state, String event) {
        List<String> columns = new ArrayList<>();
        List<DpCell> values = new ArrayList<>();
        for (int i = 0; i < memo.length; i++) {
            columns.add(String.valueOf(i));
            String colour = memo[i] == null ? "void" : "known";
            if (i == state && memo[i] != null) colour = "cache-hit".equals(event) ? "read"
                    : ("store".equals(event) || "base".equals(event)) ? "probe" : "known";
            values.add(new DpCell(memo[i] == null ? "·" : String.valueOf(memo[i]), colour));
        }
        return new DpTable(List.of("memo"), columns, List.of(values));
    }
}
