package com.dsa.ui.tracer;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two unbounded coin-reuse recurrences: unlike the 0/1-item family (each item taken at
 * most once), a coin may be reused any number of times, so "take this denomination again"
 * reads {@code dp[i][x - coin]} from the SAME row, not {@code dp[i-1][x - coin]} from the row
 * above. That is the one thing this test suite exists to pin down — everything else here is
 * the usual table-shape and cross-input-distinctness scaffolding shared with
 * {@link GridDpTraceTest} and {@link DpCostPathTraceTest}.
 */
@SpringBootTest
class CoinChangeTraceTest {

    private static final Set<String> PEDAGOGICAL_STATES = Set.of("read", "probe", "resolved");

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @ParameterizedTest(name = "{0} emits a rectangular DP table that follows its input")
    @ValueSource(strings = {
            "minimum-coins-dp",
            "coin-change-2"
    })
    void emitsRectangularTablesThatFollowInput(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        assertEquals(DsType.DP_TABLE, tracer.dsType());

        ExecutionTrace defaults = runner.runDefaults(tracer);
        ExecutionTrace alternate = runner.run(tracer, tracer.alternateInput());

        assertTraceContract(defaults);
        assertTraceContract(alternate);
        assertNotEquals(tables(defaults), tables(alternate),
                id + " emitted the same DP simulation for materially different inputs");
    }

    // ---- minimum-coins-dp ----------------------------------------------------------

    @Test
    void minimumCoinsClassicExampleNeedsThreeCoins() {
        // coins = [1,2,5], amount = 11 -> 5+5+1, the textbook LeetCode 322 example.
        ExecutionTrace trace = runDefaults("minimum-coins-dp");
        assertEquals("3", last(trace).getVariables().get("answer"));
    }

    @Test
    void minimumCoinsSanityCheckOnASmallerCell() {
        // With coins [1,2,5] a single 5-coin already makes exactly amount 5.
        ExecutionTrace trace = runDefaults("minimum-coins-dp");
        DpTable finalTable = last(trace).getDpTable();
        // Row 3 is "+coin=5" (0-indexed rows: 0="0 coins", 1="+coin=1", 2="+coin=2",
        // 3="+coin=5"); column 5 is amount 5.
        assertEquals("1", finalTable.cells().get(3).get(5).value());
    }

    @Test
    void minimumCoinsReportsMinusOneWhenUnreachable() {
        // coins = [2], amount = 3: 2 never sums to an odd number.
        ExecutionTrace trace = run("minimum-coins-dp", Map.of("coins", List.of(2), "amount", 3));
        assertEquals("-1", last(trace).getVariables().get("answer"));

        // The impossible case must be genuinely narrated, not silently returning a huge
        // meaningless sentinel with no explanation.
        String finalDescription = last(trace).getDescription().toLowerCase();
        assertTrue(finalDescription.contains("-1")
                        || finalDescription.contains("no combination")
                        || finalDescription.contains("impossible")
                        || finalDescription.contains("unreachable"),
                "the closing step must explain the impossible case, got: "
                        + last(trace).getDescription());
    }

    @Test
    void minimumCoinsRendersInfinityRatherThanARawSentinel() {
        ExecutionTrace trace = run("minimum-coins-dp", Map.of("coins", List.of(2), "amount", 3));
        DpTable finalTable = last(trace).getDpTable();
        // Row 1 ("+coin=2"), column x=3 must still be unreachable and must not display a
        // raw huge integer.
        String cellValue = finalTable.cells().get(1).get(3).value();
        assertFalse(cellValue.matches("\\d{4,}"), "expected an explicit unreachable marker, "
                + "not a raw sentinel number, got: " + cellValue);
    }

    @Test
    void minimumCoinsReuseTransitionReadsTheSameRowNotTheRowAbove() {
        // coins = [1,2,5], amount = 11. At row 1 ("+coin=1"), column x=2, the cell reused
        // coin 1 once more, which must read row 1 (same row) at column 0 — never row 0.
        ExecutionTrace trace = runDefaults("minimum-coins-dp");

        ExecutionStep step = trace.getSteps().stream()
                .filter(s -> "1".equals(s.getVariables().get("row"))
                        && "2".equals(s.getVariables().get("col")))
                .findFirst().orElseThrow();
        DpTable table = step.getDpTable();

        // The probe cell itself.
        assertEquals("probe", table.cells().get(1).get(2).state());
        // Same-row reuse read: row 1, an earlier column (x - coin = 2 - 1 = 1).
        assertEquals("read", table.cells().get(1).get(1).state(),
                "reusing a coin must read the SAME row at a smaller column");
        // Row-above skip read: row 0, same column.
        assertEquals("read", table.cells().get(0).get(2).state(),
                "skipping the coin must read the row above at the same column");
    }

    // ---- coin-change-2 --------------------------------------------------------------

    @Test
    void coinChange2ClassicExampleHasFourCombinations() {
        // coins = [1,2,5], amount = 5 -> {5}, {2,2,1}, {2,1,1,1}, {1,1,1,1,1}.
        ExecutionTrace trace = runDefaults("coin-change-2");
        assertEquals("4", last(trace).getVariables().get("answer"));
    }

    @Test
    void coinChange2FullTableMatchesHandDerivation() {
        ExecutionTrace trace = runDefaults("coin-change-2");
        DpTable finalTable = last(trace).getDpTable();

        // rows: 0="0 coins", 1="+coin=1", 2="+coin=2", 3="+coin=5"
        // cols: x=0..5
        assertEquals(List.of("1", "0", "0", "0", "0", "0"), values(finalTable, 0));
        assertEquals(List.of("1", "1", "1", "1", "1", "1"), values(finalTable, 1));
        assertEquals(List.of("1", "1", "2", "2", "3", "3"), values(finalTable, 2));
        assertEquals(List.of("1", "1", "2", "2", "3", "4"), values(finalTable, 3));
    }

    @Test
    void coinChange2StaysZeroWhenNoCombinationReachesTheTarget() {
        // coins = [3], amount = 5: only multiples of 3 are reachable.
        ExecutionTrace trace = run("coin-change-2", Map.of("coins", List.of(3), "amount", 5));
        assertEquals("0", last(trace).getVariables().get("answer"));
    }

    @Test
    void coinChange2ReuseTransitionReadsTheSameRowNotTheRowAbove() {
        // coins = [1,2,5], amount = 5. Row 2 ("+coin=2"), column x=3: reused coin 2, which
        // must read row 2 (same row) at column 1 -- never row 1.
        ExecutionTrace trace = runDefaults("coin-change-2");

        ExecutionStep step = trace.getSteps().stream()
                .filter(s -> "2".equals(s.getVariables().get("row"))
                        && "3".equals(s.getVariables().get("col")))
                .findFirst().orElseThrow();
        DpTable table = step.getDpTable();

        assertEquals("probe", table.cells().get(2).get(3).state());
        assertEquals("read", table.cells().get(2).get(1).state(),
                "reusing a coin must read the SAME row at a smaller column");
        assertEquals("read", table.cells().get(1).get(3).state(),
                "skipping the coin must read the row above at the same column");
    }

    // ---- helpers ----------------------------------------------------------------

    private ExecutionTrace runDefaults(String id) {
        return runner.runDefaults(registry.find(id).orElseThrow());
    }

    private ExecutionTrace run(String id, Map<String, Object> input) {
        return runner.run(registry.find(id).orElseThrow(), input);
    }

    private static List<String> values(DpTable table, int row) {
        return table.cells().get(row).stream().map(DpCell::value).toList();
    }

    private static void assertTraceContract(ExecutionTrace trace) {
        assertFalse(trace.getSteps().isEmpty());
        for (ExecutionStep step : trace.getSteps()) {
            assertEquals(DsType.DP_TABLE, step.getDsType());
            DpTable table = step.getDpTable();
            assertNotNull(table,
                    trace.getProblemId() + " omitted dpTable on step " + step.getStepNumber());
            assertEquals(table.rowLabels().size(), table.cells().size());
            for (List<DpCell> row : table.cells()) {
                assertEquals(table.colLabels().size(), row.size(),
                        trace.getProblemId() + " emitted a ragged table on step "
                                + step.getStepNumber());
            }
        }

        Set<String> states = trace.getSteps().stream()
                .map(ExecutionStep::getDpTable)
                .flatMap(table -> table.cells().stream())
                .flatMap(List::stream)
                .map(DpCell::state)
                .collect(Collectors.toSet());
        assertEquals(PEDAGOGICAL_STATES,
                states.stream().filter(PEDAGOGICAL_STATES::contains).collect(Collectors.toSet()),
                trace.getProblemId() + " must show dependencies read, destinations probed,"
                        + " and completed values resolved");
    }

    private static ExecutionStep last(ExecutionTrace trace) {
        return trace.getSteps().get(trace.getSteps().size() - 1);
    }

    private static List<DpTable> tables(ExecutionTrace trace) {
        return trace.getSteps().stream().map(ExecutionStep::getDpTable).toList();
    }
}
