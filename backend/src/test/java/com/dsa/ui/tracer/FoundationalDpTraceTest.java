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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FoundationalDpTraceTest {

    private static final Set<String> PEDAGOGICAL_STATES = Set.of("read", "probe", "resolved");

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @ParameterizedTest(name = "{0} emits a recurrence-aware DP table")
    @ValueSource(strings = {
            "climbing-stairs",
            "frog-jump",
            "frog-jump-k-distance",
            "max-sum-non-adjacent",
            "house-robber-2"
    })
    void emitsRectangularPedagogicalTablesThatFollowInput(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        assertEquals(DsType.DP_TABLE, tracer.dsType());

        ExecutionTrace defaults = runner.runDefaults(tracer);
        ExecutionTrace alternate = runner.run(tracer, tracer.alternateInput());

        assertTraceContract(defaults);
        assertTraceContract(alternate);
        assertNotEquals(tables(defaults), tables(alternate),
                id + " emitted the same DP simulation for materially different inputs");
    }

    @Test
    void climbingStairsReadsTheTwoPreviousWays() {
        ExecutionTrace trace = runDefaults("climbing-stairs");
        assertEquals("8", last(trace).getVariables().get("answer"));

        ExecutionStep transition = stepWithVariable(trace, "i");
        int i = Integer.parseInt(transition.getVariables().get("i"));
        List<DpCell> ways = transition.getDpTable().cells().get(0);

        assertEquals("probe", ways.get(i).state());
        assertEquals("read", ways.get(i - 1).state());
        assertEquals("read", ways.get(i - 2).state());
    }

    @Test
    void frogJumpReadsBothReachablePredecessors() {
        ExecutionTrace trace = runDefaults("frog-jump");
        assertEquals("40", last(trace).getVariables().get("answer"));

        ExecutionStep transition = trace.getSteps().stream()
                .filter(step -> "2".equals(step.getVariables().get("i")))
                .findFirst().orElseThrow();
        List<DpCell> energy = transition.getDpTable().cells().get(1);

        assertEquals("probe", energy.get(2).state());
        assertEquals("read", energy.get(1).state());
        assertEquals("read", energy.get(0).state());
        assertEquals("50", transition.getVariables().get("jumpOne"));
        assertEquals("30", transition.getVariables().get("jumpTwo"));
    }

    @Test
    void frogJumpKShowsEachCandidateBeforeSettlingTheMinimum() {
        ExecutionTrace trace = runDefaults("frog-jump-k-distance");
        assertEquals("40", last(trace).getVariables().get("answer"));

        ExecutionStep candidate = trace.getSteps().stream()
                .filter(step -> "consider".equals(step.getVariables().get("phase")))
                .filter(step -> "3".equals(step.getVariables().get("distance")))
                .findFirst().orElseThrow();
        int i = Integer.parseInt(candidate.getVariables().get("i"));
        int from = Integer.parseInt(candidate.getVariables().get("from"));
        List<DpCell> energy = candidate.getDpTable().cells().get(1);

        assertEquals("probe", energy.get(i).state());
        assertEquals("read", energy.get(from).state());
        assertEquals(3, i - from);
    }

    @Test
    void maxSumNonAdjacentWeighsTakeAgainstSkip() {
        ExecutionTrace trace = runDefaults("max-sum-non-adjacent");
        assertEquals("19", last(trace).getVariables().get("answer"));

        // i=3 is where taking wins: nums[3]=9 plus best[1]=2 beats best[2]=6.
        ExecutionStep decision = trace.getSteps().stream()
                .filter(step -> "3".equals(step.getVariables().get("i")))
                .findFirst().orElseThrow();
        List<DpCell> best = decision.getDpTable().cells().get(1);

        assertEquals("probe", best.get(3).state());
        assertEquals("read", best.get(2).state(), "skipping reads best[i-1]");
        assertEquals("read", best.get(1).state(), "taking reads best[i-2]");
        assertEquals("11", decision.getVariables().get("take"));
        assertEquals("6", decision.getVariables().get("skip"));
    }

    @Test
    void houseRobber2RunsBothPassesAndExcludesTheWrapHouse() {
        ExecutionTrace trace = runDefaults("house-robber-2");

        // The circle is broken two ways, and the answer is the better of the two.
        ExecutionStep end = last(trace);
        assertEquals("11", end.getVariables().get("skipLast"));
        assertEquals("10", end.getVariables().get("skipFirst"));
        assertEquals("11", end.getVariables().get("answer"));

        for (ExecutionStep step : trace.getSteps()) {
            assertEquals(3, step.getDpTable().rowLabels().size(),
                    "house-robber-2 must show the values and both passes");
        }

        // A house excluded from a pass must never hold a value in that pass's row.
        int lastHouse = trace.getSteps().get(0).getDpTable().colLabels().size() - 1;
        for (ExecutionStep step : trace.getSteps()) {
            List<List<DpCell>> cells = step.getDpTable().cells();
            assertEquals("void", cells.get(1).get(lastHouse).state(),
                    "pass 1 excludes the final house");
            assertEquals("void", cells.get(2).get(0).state(),
                    "pass 2 excludes the first house");
        }

        // Both passes must actually be exercised, not just reported at the end.
        assertTrue(trace.getSteps().stream()
                        .anyMatch(step -> "skip-last".equals(step.getVariables().get("pass"))),
                "no step ran the pass that excludes the final house");
        assertTrue(trace.getSteps().stream()
                        .anyMatch(step -> "skip-first".equals(step.getVariables().get("pass"))),
                "no step ran the pass that excludes the first house");
    }

    private ExecutionTrace runDefaults(String id) {
        return runner.runDefaults(registry.find(id).orElseThrow());
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

    private static ExecutionStep stepWithVariable(ExecutionTrace trace, String variable) {
        return trace.getSteps().stream()
                .filter(step -> step.getVariables().containsKey(variable))
                .findFirst().orElseThrow();
    }

    private static ExecutionStep last(ExecutionTrace trace) {
        return trace.getSteps().get(trace.getSteps().size() - 1);
    }

    private static List<DpTable> tables(ExecutionTrace trace) {
        return trace.getSteps().stream().map(ExecutionStep::getDpTable).toList();
    }
}
