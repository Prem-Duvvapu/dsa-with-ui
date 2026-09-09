package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.tracer.FieldType;
import com.dsa.ui.tracer.InputField;
import com.dsa.ui.tracer.InputSpec;
import com.dsa.ui.tracer.Inputs;
import com.dsa.ui.tracer.StepEmitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class StockStateRemainingTracer extends RemainingDpTracer {
    protected abstract List<Integer> defaults();
    protected int transactionCap(Inputs in) { return 0; }
    protected int fee(Inputs in) { return 0; }
    protected boolean cooldown() { return false; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(DpTraceSupport.intArray("prices", "Daily prices", defaults(), 1, 15, 0, 100));
    }

    @Override public Map<String, Object> alternateInput() { return Map.of("prices", List.of(7, 6, 4, 3, 1)); }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] prices = in.getIntArray("prices"); int cap = transactionCap(in);
        if (cap > 0) runCapped(prices, cap, emit); else runUnlimited(prices, fee(in), cooldown(), emit);
    }

    private void runCapped(int[] prices, int cap, StepEmitter emit) {
        int actions = 2 * cap; long[][] dp = new long[prices.length + 1][actions + 1];
        boolean[][] known = new boolean[dp.length][dp[0].length];
        for (int a = 0; a <= actions; a++) known[prices.length][a] = true;
        for (int d = 0; d <= prices.length; d++) known[d][actions] = true;
        emit.at("init").say("dp[day][action] stores maximum future profit; even actions buy, odd actions sell, and %d transactions are allowed.", cap)
                .dpTable(stockTable(dp, known, null, Set.of(), false, "max(skip, execute action)", "terminal states = 0")).step();
        for (int day = prices.length - 1; day >= 0; day--) for (int action = actions - 1; action >= 0; action--) {
            long skip = dp[day + 1][action];
            long execute = (action % 2 == 0 ? -prices[day] : prices[day]) + dp[day + 1][action + 1];
            dp[day][action] = Math.max(skip, execute); known[day][action] = true;
            emit.at("fill").say("Day %d %s state %d: skip=%d, execute=%d, choose %d.", day,
                            action % 2 == 0 ? "buy" : "sell", action, skip, execute, dp[day][action])
                    .var("day", day).var("action", action).var("profit", dp[day][action])
                    .dpTable(stockTable(dp, known, new DpTraceSupport.Coord(day, action),
                            Set.of(new DpTraceSupport.Coord(day + 1, action), new DpTraceSupport.Coord(day + 1, action + 1)), false,
                            "max(skip, execute)", "max(" + skip + ", " + execute + ") = " + dp[day][action])).step();
        }
        emit.at("done").say("Maximum profit is %d.", dp[0][0]).var("answer", dp[0][0])
                .dpTable(stockTable(dp, known, null, Set.of(), true, "answer = dp[0][buy]", "answer = " + dp[0][0])).step();
    }

    private void runUnlimited(int[] prices, int fee, boolean cooldown, StepEmitter emit) {
        int extra = cooldown ? 2 : 1; long[][] dp = new long[prices.length + extra][2];
        boolean[][] known = new boolean[dp.length][2];
        for (int d = prices.length; d < dp.length; d++) { known[d][0] = true; known[d][1] = true; }
        emit.at("init").say("dp[day][buy/sell] stores maximum future profit; terminal days contribute 0%s.",
                        cooldown ? " and selling skips the next day" : fee > 0 ? " with the fee charged on each sale" : "")
                .dpTable(stockTable(dp, known, null, Set.of(), false, "max(skip, trade)", "terminal = 0")).step();
        for (int day = prices.length - 1; day >= 0; day--) {
            long skipBuy = dp[day + 1][0], buy = -prices[day] + dp[day + 1][1];
            dp[day][0] = Math.max(skipBuy, buy); known[day][0] = true;
            emit.at("fill").say("Day %d buy state: skip=%d, buy=%d, choose %d.", day, skipBuy, buy, dp[day][0])
                    .var("day", day).var("state", "buy").var("profit", dp[day][0])
                    .dpTable(stockTable(dp, known, new DpTraceSupport.Coord(day, 0),
                            Set.of(new DpTraceSupport.Coord(day + 1, 0), new DpTraceSupport.Coord(day + 1, 1)), false,
                            "max(skip, -price + sellNext)", "max(" + skipBuy + ", " + buy + ") = " + dp[day][0])).step();
            int next = day + (cooldown ? 2 : 1); long skipSell = dp[day + 1][1], sell = prices[day] - fee + dp[next][0];
            dp[day][1] = Math.max(skipSell, sell); known[day][1] = true;
            emit.at("fill").say("Day %d sell state: skip=%d, sell=%d, choose %d.", day, skipSell, sell, dp[day][1])
                    .var("day", day).var("state", "sell").var("profit", dp[day][1])
                    .dpTable(stockTable(dp, known, new DpTraceSupport.Coord(day, 1),
                            Set.of(new DpTraceSupport.Coord(day + 1, 1), new DpTraceSupport.Coord(next, 0)), false,
                            "max(skip, price - fee + buyNext)", "max(" + skipSell + ", " + sell + ") = " + dp[day][1])).step();
        }
        emit.at("done").say("Maximum profit is %d.", dp[0][0]).var("answer", dp[0][0])
                .dpTable(stockTable(dp, known, null, Set.of(), true, "answer = dp[0][buy]", "answer = " + dp[0][0])).step();
    }

    private static DpTable stockTable(long[][] dp, boolean[][] known, DpTraceSupport.Coord probe,
                                      Set<DpTraceSupport.Coord> reads, boolean done, String formula, String substitution) {
        List<String> rows = DpTraceSupport.labels("day", dp.length); List<String> cols = new ArrayList<>(dp[0].length);
        if (dp[0].length == 2) { cols.add("buy"); cols.add("sell"); }
        else for (int a = 0; a < dp[0].length; a++) cols.add(a == dp[0].length - 1 ? "done" : (a % 2 == 0 ? "buy" : "sell") + (a / 2 + 1));
        return DpTraceSupport.table(dp, known, probe, reads, rows, cols, done, formula, substitution);
    }
}

@Component
class BestTimeStockOneRemainingTracer extends StockStateRemainingTracer {
    @Override public String id() { return "best-time-stock-1"; }
    @Override protected List<Integer> defaults() { return List.of(7, 1, 5, 3, 6, 4); }
    @Override protected int transactionCap(Inputs in) { return 1; }
}

@Component
class BestTimeStockTwoRemainingTracer extends StockStateRemainingTracer {
    @Override public String id() { return "best-time-stock-2"; }
    @Override protected List<Integer> defaults() { return List.of(7, 1, 5, 3, 6, 4); }
}

@Component
class BestTimeStockThreeRemainingTracer extends StockStateRemainingTracer {
    @Override public String id() { return "best-time-stock-3"; }
    @Override protected List<Integer> defaults() { return List.of(3, 3, 5, 0, 0, 3, 1, 4); }
    @Override protected int transactionCap(Inputs in) { return 2; }
}

@Component
class BestTimeStockFourRemainingTracer extends StockStateRemainingTracer {
    @Override public String id() { return "best-time-stock-4"; }
    @Override protected List<Integer> defaults() { return List.of(3, 2, 6, 5, 0, 3); }
    @Override public InputSpec inputSpec() { return InputSpec.of(
            DpTraceSupport.intArray("prices", "Daily prices", defaults(), 1, 15, 0, 100),
            InputField.of("k", FieldType.INT).label("Maximum transactions").range(1, 5).defaultValue(2).build()); }
    @Override public Map<String, Object> alternateInput() { return Map.of("prices", List.of(2, 4, 1, 7), "k", 1); }
    @Override protected int transactionCap(Inputs in) { return in.getInt("k"); }
}

@Component
class StockCooldownRemainingTracer extends StockStateRemainingTracer {
    @Override public String id() { return "stock-cooldown"; }
    @Override protected List<Integer> defaults() { return List.of(1, 2, 3, 0, 2); }
    @Override protected boolean cooldown() { return true; }
}

@Component
class StockTransactionFeeRemainingTracer extends StockStateRemainingTracer {
    @Override public String id() { return "stock-transaction-fee"; }
    @Override protected List<Integer> defaults() { return List.of(1, 3, 2, 8, 4, 9); }
    @Override public InputSpec inputSpec() { return InputSpec.of(
            DpTraceSupport.intArray("prices", "Daily prices", defaults(), 1, 15, 0, 100),
            InputField.of("fee", FieldType.INT).label("Transaction fee").range(0, 30).defaultValue(2).build()); }
    @Override public Map<String, Object> alternateInput() { return Map.of("prices", List.of(1, 3, 7, 5, 10, 3), "fee", 3); }
    @Override protected int fee(Inputs in) { return in.getInt("fee"); }
}
