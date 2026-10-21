package com.dsa.ui.approach;

import com.dsa.ui.model.*;
import com.dsa.ui.tracer.StepEmitter;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Request-local boolean instrumentation: false is a computed result, not a missing value. */
final class RecursiveBooleanDpExecution {
    record State(int n, int sum) { String label() { return "n=" + n + ",s=" + sum; } }
    private final StepEmitter emit;
    private final Boolean[][] memo;
    private final List<String> rows, cols;
    private final Set<State> computed = new HashSet<>();
    private int calls, hits, evaluations;

    RecursiveBooleanDpExecution(StepEmitter emit, boolean cached, int n, int sum) {
        this.emit = emit;
        rows = RecursiveDpExecution.labels("n=", n + 1);
        cols = RecursiveDpExecution.labels("s=", sum + 1);
        memo = cached ? new Boolean[n + 1][sum + 1] : null;
    }
    void start(State root, String description) { step("start", "start", root, -1, null, description).step(); }
    void done(State root, boolean answer) {
        step("done", "done", root, -1, answer, "All calls returned. Answer = " + answer + ".")
                .var("answer", answer).step();
    }
    boolean evaluate(State state, BooleanSupplier recurrence) {
        int call = ++calls;
        emit.push("possible(" + state.label() + ") #" + call);
        boolean answer;
        String anchor;
        try {
            step("enter", "enter", state, call, null, "Enter call " + call + ": " + state.label() + ".").step();
            Boolean known = memo == null ? null : memo[state.n][state.sum];
            if (known != null) {
                answer = known;
                hits++;
                anchor = "hit";
                step(anchor, "cache-hit", state, call, answer, "Reuse cached " + state.label() + " = " + answer + "; no children are expanded.").step();
            } else {
                if (state.n != 0) step("branch", "branch", state, call, null,
                        "Evaluate skip first; evaluate take as well when this item's value fits the remaining sum.").step();
                answer = recurrence.getAsBoolean();
                evaluations++;
                computed.add(state);
                if (memo != null) memo[state.n][state.sum] = answer;
                anchor = state.n == 0 ? "base" : "combine";
                step(anchor, state.n == 0 ? "base" : memo != null ? "store" : "combine", state, call, answer,
                        (state.n == 0 ? "No items: only sum zero is reachable. " : "OR the two returned boolean choices. ")
                                + state.label() + " = " + answer + (memo != null ? ". Store this known boolean, including false." : ". No result is cached.")).step();
            }
        } finally { emit.pop(); }
        step(anchor, "return", state, call, answer, "Call " + call + " returns " + answer + "; its frame has left the stack.").step();
        return answer;
    }
    private StepEmitter.Step step(String anchor, String event, State state, int call, Boolean value, String description) {
        var step = emit.at(anchor).say(description).var("event", event).var("state", state.label())
                .var("calls", calls).var("cacheHits", hits).var("stateEvaluations", evaluations).var("computedStates", computed.size());
        if (call >= 0) step.var("callId", call);
        if (value != null) step.var("value", value);
        if (memo != null) {
            List<List<DpCell>> cells = new ArrayList<>();
            for (int r = 0; r < rows.size(); r++) {
                List<DpCell> row = new ArrayList<>();
                for (int c = 0; c < cols.size(); c++) {
                    Boolean known = memo[r][c];
                    String colour = known == null ? "void" : r == state.n && c == state.sum
                            ? "cache-hit".equals(event) ? "read" : Set.of("base", "store").contains(event) ? "probe" : "known" : "known";
                    row.add(new DpCell(known == null ? "·" : known ? "T" : "F", colour));
                }
                cells.add(row);
            }
            step.dpTable(new DpTable(rows, cols, cells)).var("memoDomain", "T = true; F = false (a known result); · = unknown. State (n,s) uses the first n items to reach sum s.");
        }
        return step;
    }
}
