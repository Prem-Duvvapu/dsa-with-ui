package com.dsa.ui.tracer;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Ninja's Training: a 3-way choice per row rather than the 2-way choice
 * {@code max-sum-non-adjacent} and {@code house-robber-2} trace, and the first table here
 * where a cell excludes exactly one same-column predecessor rather than reading a fixed
 * neighbour shape.
 */
@SpringBootTest
class NinjasTrainingTraceTest {

    private static final Set<String> PEDAGOGICAL_STATES = Set.of("read", "probe", "resolved");

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @Test
    void emitsATableThatFollowsInput() {
        AlgorithmTracer tracer = registry.find("ninjas-training").orElseThrow();
        assertEquals(DsType.DP_TABLE, tracer.dsType());

        ExecutionTrace defaults = runner.runDefaults(tracer);
        ExecutionTrace alternate = runner.run(tracer, tracer.alternateInput());

        assertTraceContract(defaults);
        assertTraceContract(alternate);
        assertNotEquals(tables(defaults), tables(alternate),
                "ninjas-training emitted the same DP simulation for materially different"
                        + " inputs");
    }

    @Test
    void answerIsTheMaxOfTheLastDayAcrossAllThreeActivities() {
        ExecutionTrace trace = runner.runDefaults(
                registry.find("ninjas-training").orElseThrow());
        ExecutionStep last = last(trace);
        assertEquals("150", last.getVariables().get("answer"));

        DpTable finalTable = last.getDpTable();
        List<DpCell> lastDay = finalTable.cells().get(finalTable.rowLabels().size() - 1);
        assertEquals(3, lastDay.size());
        assertTrue(lastDay.stream().allMatch(c -> "resolved".equals(c.state())));
    }

    @Test
    void eachChoiceExcludesOnlyItsOwnColumnFromThePreviousDay() {
        ExecutionTrace trace = runner.runDefaults(
                registry.find("ninjas-training").orElseThrow());

        ExecutionStep step = trace.getSteps().stream()
                .filter(s -> "1".equals(s.getVariables().get("day")))
                .filter(s -> "0".equals(s.getVariables().get("task")))
                .findFirst().orElseThrow();
        DpTable table = step.getDpTable();

        assertEquals("probe", table.cells().get(1).get(0).state());
        assertEquals("read", table.cells().get(0).get(1).state(), "reads the other activities");
        assertEquals("read", table.cells().get(0).get(2).state(), "reads the other activities");
        assertNotEquals("read", table.cells().get(0).get(0).state(),
                "must not read yesterday's same activity — that is the whole constraint");
    }

    private static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
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
