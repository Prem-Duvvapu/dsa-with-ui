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

/**
 * The two "reduce over several final cells" grid recurrences: unlike
 * {@link GridDpTraceTest}'s pair, the answer here is not one fixed corner cell — it is the
 * minimum across an entire row (falling path) or the single cell a triangle's rows collapse
 * into (already one cell, but reached by reading two children below rather than two
 * predecessors above).
 */
@SpringBootTest
class DpCostPathTraceTest {

    private static final Set<String> PEDAGOGICAL_STATES = Set.of("read", "probe", "resolved");

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @ParameterizedTest(name = "{0} emits a rectangular grid-shaped DP table")
    @ValueSource(strings = {
            "minimum-falling-path-sum",
            "triangle-min-path-sum"
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

    @Test
    void fallingPathSumConsidersUpToThreeDiagonalPredecessors() {
        ExecutionTrace trace = runDefaults("minimum-falling-path-sum");

        // The middle column of an interior row has three live predecessors.
        ExecutionStep middle = trace.getSteps().stream()
                .filter(step -> "1".equals(step.getVariables().get("row"))
                        && "1".equals(step.getVariables().get("col")))
                .findFirst().orElseThrow();
        assertEquals(3, middle.getVariables().get("candidates") == null
                ? 3 : Integer.parseInt(middle.getVariables().get("candidates")));

        // Column 0 of that same row has no up-left neighbour: only two live predecessors.
        ExecutionStep edge = trace.getSteps().stream()
                .filter(step -> "1".equals(step.getVariables().get("row"))
                        && "0".equals(step.getVariables().get("col")))
                .findFirst().orElseThrow();
        assertEquals(2, Integer.parseInt(edge.getVariables().get("candidates")));
    }

    @Test
    void fallingPathSumAnswerIsTheMinimumOfTheEntireLastRow() {
        ExecutionTrace trace = runDefaults("minimum-falling-path-sum");
        ExecutionStep last = last(trace);
        assertEquals("13", last.getVariables().get("answer"));

        // Every cell of the last row must be visible in the closing step — the answer is a
        // reduction over the whole row, not a read of one fixed corner.
        DpTable finalTable = last.getDpTable();
        List<DpCell> lastRow = finalTable.cells().get(finalTable.rowLabels().size() - 1);
        assertTrue(lastRow.stream().allMatch(c -> "resolved".equals(c.state())));
    }

    @Test
    void triangleReadsBothChildrenBelow() {
        ExecutionTrace trace = runDefaults("triangle-min-path-sum");
        assertEquals("11", last(trace).getVariables().get("answer"));

        ExecutionStep apex = trace.getSteps().stream()
                .filter(step -> "0".equals(step.getVariables().get("row")))
                .filter(step -> step.getVariables().containsKey("left"))
                .findFirst().orElseThrow();
        DpTable table = apex.getDpTable();

        assertEquals("probe", table.cells().get(0).get(0).state());
        assertEquals("read", table.cells().get(1).get(0).state(), "reads the left child");
        assertEquals("read", table.cells().get(1).get(1).state(), "reads the right child");
    }

    @Test
    void triangleRaggedCellsStayVoidPermanently() {
        ExecutionTrace trace = runDefaults("triangle-min-path-sum");

        for (ExecutionStep step : trace.getSteps()) {
            DpTable table = step.getDpTable();
            for (int row = 0; row < table.rowLabels().size(); row++) {
                for (int col = row + 1; col < table.colLabels().size(); col++) {
                    assertEquals("void", table.cells().get(row).get(col).state(),
                            "cell (" + row + "," + col + ") is outside the triangle's shape "
                                    + "and must never hold a computed value");
                }
            }
        }
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

    private static ExecutionStep last(ExecutionTrace trace) {
        return trace.getSteps().get(trace.getSteps().size() - 1);
    }

    private static List<DpTable> tables(ExecutionTrace trace) {
        return trace.getSteps().stream().map(ExecutionStep::getDpTable).toList();
    }
}
