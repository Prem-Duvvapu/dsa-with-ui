package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

final class StockFeeRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    StockFeeRecursiveTracer(boolean memo) { this.memo = memo; }
    public String id() { return "stock-transaction-fee"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id()
            + "/" + (memo ? "memoization" : "recursion") + ".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("prices", FieldType.INT_ARRAY).label("Daily prices").length(1, memo ? 15 : 6)
                .values(0, 100).defaultValue(List.of(1, 3, 2, 8, 4, 9))
                .help("State (day,holding): skip or trade; charge the fee exactly once on each sale.").build(),
                InputField.of("fee", FieldType.INT).label("Transaction fee").range(0, 30).defaultValue(2).build());
    }
    public Map<String, Object> alternateInput() { return Map.of("prices", List.of(1, 3, 7, 5, 10, 3), "fee", 3); }
    public void run(Inputs in, StepEmitter emit) {
        int[] prices = in.getIntArray("prices");
        var trace = new RecursiveDpExecution(emit, memo, 1, RecursiveDpExecution.labels("day=", prices.length + 1),
                List.of("not holding / buy", "holding / sell"));
        var root = state(0, 0);
        trace.start(root, "Start without a stock. Skip or trade on each day; the fee is charged only when selling.");
        trace.done(root, solve(0, 0, prices, in.getInt("fee"), trace));
    }
    private int solve(int day, int holding, int[] prices, int fee, RecursiveDpExecution trace) {
        return trace.evaluate(state(day, holding), day == prices.length,
                "Compare skipping day " + day + " with " + (holding == 0 ? "buying" : "selling and paying the fee") + ".", () -> {
            if (day == prices.length) return 0;
            int skip = solve(day + 1, holding, prices, fee, trace);
            int trade = (holding == 0 ? -prices[day] : prices[day] - fee)
                    + solve(day + 1, 1 - holding, prices, fee, trace);
            return Math.max(skip, trade);
        });
    }
    private static RecursiveDpExecution.State state(int day, int holding) {
        return new RecursiveDpExecution.State(0, day, holding, "day=" + day + ",holding=" + holding);
    }
}
