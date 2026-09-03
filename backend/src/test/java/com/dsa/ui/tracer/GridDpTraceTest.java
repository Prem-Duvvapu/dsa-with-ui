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

/**
 * The genuinely two-dimensional DP tables, as distinct from the 1-D recurrences covered by
 * {@link FoundationalDpTraceTest}: the table's rows and columns are the problem's own grid,
 * not a single row of positions.
 */
@SpringBootTest
class GridDpTraceTest {

    private static final Set<String> PEDAGOGICAL_STATES = Set.of("read", "probe", "resolved");

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @ParameterizedTest(name = "{0} emits a rectangular grid-shaped DP table")
    @ValueSource(strings = {
            "grid-unique-paths",
            "unique-paths-2"
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
    void gridUniquePathsSumsFromAboveAndLeft() {
        ExecutionTrace trace = runDefaults("grid-unique-paths");
        assertEquals("6", last(trace).getVariables().get("answer"));

        ExecutionStep interior = trace.getSteps().stream()
                .filter(step -> "2".equals(step.getVariables().get("row"))
                        && "2".equals(step.getVariables().get("col")))
                .findFirst().orElseThrow();
        DpTable table = interior.getDpTable();

        assertEquals("probe", table.cells().get(2).get(2).state());
        assertEquals("read", table.cells().get(1).get(2).state(), "reads the cell above");
        assertEquals("read", table.cells().get(2).get(1).state(), "reads the cell to the left");
        assertEquals("3", table.cells().get(1).get(2).value());
        assertEquals("3", table.cells().get(2).get(1).value());
    }

    @Test
    void gridUniquePathsEdgesHaveExactlyOnePath() {
        ExecutionTrace trace = runDefaults("grid-unique-paths");
        DpTable last = last(trace).getDpTable();

        for (int c = 0; c < last.colLabels().size(); c++) {
            assertEquals("1", last.cells().get(0).get(c).value(), "top row is always 1");
        }
        for (int r = 0; r < last.rowLabels().size(); r++) {
            assertEquals("1", last.cells().get(r).get(0).value(), "left column is always 1");
        }
    }

    @Test
    void uniquePathsWithObstaclesVoidsTheBlockedCellPermanently() {
        ExecutionTrace trace = runDefaults("unique-paths-2");
        assertEquals("2", last(trace).getVariables().get("answer"));

        for (ExecutionStep step : trace.getSteps()) {
            DpCell blocked = step.getDpTable().cells().get(1).get(1);
            assertEquals("void", blocked.state(),
                    "the obstacle cell must never hold a computed value, on any step");
        }
    }

    @Test
    void uniquePathsWithObstaclesTreatsMissingNeighboursAsZero() {
        // An obstacle at the start makes the whole grid unreachable: every cell is 0,
        // not merely the obstacle itself.
        ExecutionTrace trace = run("unique-paths-2",
                java.util.Map.of("grid", List.of(List.of(1, 0), List.of(0, 0))));
        assertEquals("0", last(trace).getVariables().get("answer"));

        DpTable finalTable = last(trace).getDpTable();
        assertEquals("void", finalTable.cells().get(0).get(0).state(),
                "the starting cell is itself the obstacle");
        assertEquals("0", finalTable.cells().get(0).get(1).value());
        assertEquals("0", finalTable.cells().get(1).get(0).value());
        assertEquals("0", finalTable.cells().get(1).get(1).value());
    }

    private ExecutionTrace runDefaults(String id) {
        return runner.runDefaults(registry.find(id).orElseThrow());
    }

    private ExecutionTrace run(String id, java.util.Map<String, Object> input) {
        return runner.run(registry.find(id).orElseThrow(), input);
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
