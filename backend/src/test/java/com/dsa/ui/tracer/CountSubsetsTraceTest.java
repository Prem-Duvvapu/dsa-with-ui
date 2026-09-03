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
 * The counting cousins of the subset-sum reachability family: {@code dp[i][s]} holds how
 * MANY subsets of the first i items sum to exactly s, not merely whether one exists. Cell
 * values are counts (never "true"/"false"), which is the whole difference from a
 * reachability table.
 *
 * <p>Hand-derived worked examples this test pins:
 * <pre>
 * count-subsets-with-sum-k, nums=[1,2,2,3], target=3 -&gt; dp[4][3] = 3
 *   (subsets {1,2(first)}, {1,2(second)}, {3})
 * count-subsets-with-sum-k, nums=[5,10], target=3 -&gt; dp[2][3] = 0 (no subset reaches 3)
 * count-partitions-given-diff, nums=[1,1,2,3], D=1 -&gt; totalSum=7, (7+1)/2=4 -&gt; dp[4][4] = 3
 * count-partitions-given-diff, nums=[1,2,3], D=1 -&gt; totalSum=6, (6+1)=7 is odd -&gt; answer 0,
 *   no table built at all
 * </pre>
 */
@SpringBootTest
class CountSubsetsTraceTest {

    private static final Set<String> PEDAGOGICAL_STATES = Set.of("read", "probe", "resolved");

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @ParameterizedTest(name = "{0} emits a rectangular DP table that follows its input")
    @ValueSource(strings = {
            "count-subsets-with-sum-k",
            "count-partitions-given-diff"
    })
    void emitsRectangularTablesThatFollowInput(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        assertEquals(DsType.DP_TABLE, tracer.dsType());

        ExecutionTrace defaults = runner.runDefaults(tracer);
        ExecutionTrace alternate = runner.run(tracer, tracer.alternateInput());

        assertWellFormed(defaults);
        assertWellFormed(alternate);
        assertNotEquals(tables(defaults), tables(alternate),
                id + " emitted the same DP simulation for materially different inputs");
    }

    @Test
    void countSubsetsDefaultMatchesHandDerivedTable() {
        ExecutionTrace trace = runDefaults("count-subsets-with-sum-k");
        assertEquals("3", last(trace).getVariables().get("answer"));

        DpTable finalTable = last(trace).getDpTable();
        // rows = item 0..4, cols = s=0..3 (target 3)
        assertEquals("3", finalTable.cells().get(4).get(3).value(),
                "dp[4][3] must be 3 for nums=[1,2,2,3], target=3");
        assertEquals("2", finalTable.cells().get(3).get(3).value(),
                "dp[3][3] (ways not using the last 3) must be 2");
        assertEquals("1", finalTable.cells().get(3).get(0).value(),
                "dp[3][0] (the empty subset) must always be 1");

        Set<String> states = statesUsed(trace);
        assertTrue(states.containsAll(PEDAGOGICAL_STATES),
                "a real table build must show reads, a probe, and a resolved close");
    }

    @Test
    void countSubsetsAlternateFindsZeroSubsets() {
        // nums=[5,10], target=3: neither element fits, so the count must stay zero
        // throughout — this exercises the "no subset reaches this sum" branch the
        // default input (which does find subsets) never takes.
        ExecutionTrace trace = runDefaults("count-subsets-with-sum-k");
        ExecutionTrace alt = run("count-subsets-with-sum-k",
                Map.of("nums", List.of(5, 10), "k", 3));
        assertEquals("0", last(alt).getVariables().get("answer"));
        assertNotEquals(last(trace).getVariables().get("answer"),
                last(alt).getVariables().get("answer"));
    }

    @Test
    void countPartitionsDefaultReducesToSubsetCountAndMatchesHandDerivedTable() {
        ExecutionTrace trace = runDefaults("count-partitions-given-diff");
        assertEquals("3", last(trace).getVariables().get("answer"));

        DpTable finalTable = last(trace).getDpTable();
        // nums=[1,1,2,3], D=1 -> totalSum=7, target=(7+1)/2=4; rows = item 0..4, cols = s=0..4
        // Full table (verified against brute-force enumeration of {1,1,2,3}'s subsets):
        //   dp[0] = [1,0,0,0,0]
        //   dp[1] (val=1) = [1,1,0,0,0]
        //   dp[2] (val=1) = [1,2,1,0,0]
        //   dp[3] (val=2) = [1,2,2,2,1]
        //   dp[4] (val=3) = [1,2,2,3,3]
        assertEquals("3", finalTable.cells().get(4).get(4).value(),
                "dp[4][4] must be 3 for nums=[1,1,2,3], D=1 (target=4)");
        assertEquals("1", finalTable.cells().get(3).get(4).value(),
                "dp[3][4] (ways not using the trailing 3 to reach s=4) must be 1: only "
                        + "{1,1,2} reaches 4 among the first 3 items");
        assertEquals("2", finalTable.cells().get(3).get(1).value(),
                "dp[3][1] (ways using the trailing 3 to reach 4-3=1) must be 2");

        Set<String> states = statesUsed(trace);
        assertTrue(states.containsAll(PEDAGOGICAL_STATES),
                "the even-parity branch must actually build and narrate a table");
    }

    @Test
    void countPartitionsAlternateIsImmediatelyZeroWithNoTableBuilt() {
        // nums=[1,2,3], D=1: totalSum=6, (6+1)=7 is odd, so no partition can achieve
        // this exact difference. The answer must be 0 without a real table build.
        ExecutionTrace trace = run("count-partitions-given-diff",
                Map.of("nums", List.of(1, 2, 3), "d", 1));
        assertEquals("0", last(trace).getVariables().get("answer"));
        assertFalse(trace.getSteps().isEmpty(),
                "the zero-without-computation branch must still narrate at least one step");

        Set<String> states = statesUsed(trace);
        assertTrue(Set.of("void").containsAll(states),
                "the odd-parity branch must not fabricate a computed table");
    }

    private ExecutionTrace runDefaults(String id) {
        return runner.runDefaults(registry.find(id).orElseThrow());
    }

    private ExecutionTrace run(String id, Map<String, Object> input) {
        return runner.run(registry.find(id).orElseThrow(), input);
    }

    private static void assertWellFormed(ExecutionTrace trace) {
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

    private static Set<String> statesUsed(ExecutionTrace trace) {
        return trace.getSteps().stream()
                .map(ExecutionStep::getDpTable)
                .flatMap(table -> table.cells().stream())
                .flatMap(List::stream)
                .map(DpCell::state)
                .collect(Collectors.toSet());
    }

    private static ExecutionStep last(ExecutionTrace trace) {
        return trace.getSteps().get(trace.getSteps().size() - 1);
    }

    private static List<DpTable> tables(ExecutionTrace trace) {
        return trace.getSteps().stream().map(ExecutionStep::getDpTable).toList();
    }
}
