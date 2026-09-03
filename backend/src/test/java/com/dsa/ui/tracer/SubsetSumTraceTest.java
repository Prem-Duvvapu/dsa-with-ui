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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Subset-sum reachability tables: {@code subset-sum-equal-target} and its reduction,
 * {@code partition-equal-subset-sum}. Both build {@code dp[i][s] = "does some subset of
 * the first i items sum to exactly s"} — the first genuinely boolean-reachability
 * {@code DP_TABLE} traced in this catalogue, distinct from {@link GridDpTraceTest}'s
 * numeric accumulation.
 */
@SpringBootTest
class SubsetSumTraceTest {

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @ParameterizedTest(name = "{0} emits a well-formed rectangular reachability table")
    @ValueSource(strings = {
            "subset-sum-equal-target",
            "partition-equal-subset-sum"
    })
    void emitsWellFormedTablesThatFollowInput(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        assertEquals(DsType.DP_TABLE, tracer.dsType());

        ExecutionTrace defaults = runner.runDefaults(tracer);
        ExecutionTrace alternate = runner.run(tracer, tracer.alternateInput());

        assertRectangular(defaults);
        assertRectangular(alternate);
        assertNotEquals(tables(defaults), tables(alternate),
                id + " emitted the same DP simulation for materially different inputs");
    }

    @Test
    void subsetSumEqualTargetFindsAReachableSum() {
        // nums = [1, 2, 3, 7], target = 6 -> subset {1, 2, 3} sums to 6.
        ExecutionTrace trace = runDefaults("subset-sum-equal-target");
        assertEquals("true", last(trace).getVariables().get("answer"));

        DpTable finalTable = last(trace).getDpTable();
        // dp[4][6] is the answer cell: row "item 4 (7)", column s=6.
        assertEquals("T", finalTable.cells().get(4).get(6).value());
        // Worked-example row i=1 (value 1), s=0..6: T T F F F F F.
        assertEquals(List.of("T", "T", "F", "F", "F", "F", "F"),
                finalTable.cells().get(1).stream().map(DpCell::value).toList());
        // Worked-example row i=3 (value 3), s=0..6: every sum becomes reachable.
        assertEquals(List.of("T", "T", "T", "T", "T", "T", "T"),
                finalTable.cells().get(3).stream().map(DpCell::value).toList());
    }

    @Test
    void subsetSumEqualTargetReportsAnUnreachableSum() {
        // nums = [2, 4], target = 3 -> no subset of even numbers sums to an odd target.
        ExecutionTrace trace = run("subset-sum-equal-target",
                Map.of("nums", List.of(2, 4), "target", 3));
        assertEquals("false", last(trace).getVariables().get("answer"));

        DpTable finalTable = last(trace).getDpTable();
        assertEquals("F", finalTable.cells().get(2).get(3).value(), "dp[2][3] must be false");
    }

    @Test
    void partitionEqualSubsetSumFindsAnEqualSplit() {
        // nums = [1, 5, 11, 5], totalSum = 22 (even) -> target 11, e.g. {11} vs {1, 5, 5}.
        ExecutionTrace trace = runDefaults("partition-equal-subset-sum");
        assertEquals("true", last(trace).getVariables().get("answer"));

        DpTable finalTable = last(trace).getDpTable();
        assertEquals(12, finalTable.colLabels().size(), "s=0..11 is 12 columns");
        assertEquals("T", finalTable.cells().get(4).get(11).value(), "dp[4][11] must be true");
    }

    @Test
    void partitionEqualSubsetSumTakesTheOddSumShortCircuit() {
        // nums = [1, 2, 3, 5], totalSum = 11, odd -> no subset-sum table is built at all.
        ExecutionTrace trace = run("partition-equal-subset-sum",
                Map.of("nums", List.of(1, 2, 3, 5)));
        assertEquals("false", last(trace).getVariables().get("answer"));
        assertEquals("11", last(trace).getVariables().get("totalSum"));
        assertEquals(1, trace.getSteps().size(),
                "an odd total sum must short-circuit before any subset-sum table work");
    }

    private ExecutionTrace runDefaults(String id) {
        return runner.runDefaults(registry.find(id).orElseThrow());
    }

    private ExecutionTrace run(String id, Map<String, Object> input) {
        return runner.run(registry.find(id).orElseThrow(), input);
    }

    private static void assertRectangular(ExecutionTrace trace) {
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
    }

    private static ExecutionStep last(ExecutionTrace trace) {
        return trace.getSteps().get(trace.getSteps().size() - 1);
    }

    private static List<DpTable> tables(ExecutionTrace trace) {
        return trace.getSteps().stream().map(ExecutionStep::getDpTable).toList();
    }
}
