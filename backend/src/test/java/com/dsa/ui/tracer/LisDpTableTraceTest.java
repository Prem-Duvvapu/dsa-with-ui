package com.dsa.ui.tracer;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
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
class LisDpTableTraceTest {

    private static final Set<String> PEDAGOGICAL_STATES = Set.of("read", "probe", "resolved");

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @ParameterizedTest(name = "{0} emits a recurrence-aware DP table")
    @ValueSource(strings = {
            "longest-increasing-subsequence",
            "lis-binary-search",
            "print-lis"
    })
    void emitsRectangularPedagogicalTablesThatFollowInput(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        assertEquals(DsType.DP_TABLE, tracer.dsType());

        ExecutionTrace defaults = runner.runDefaults(tracer);
        ExecutionTrace alternate = runner.run(tracer, tracer.alternateInput());

        assertTraceContract(defaults);
        assertTraceContract(alternate);
        assertReadCoordinates(id, defaults);
        assertNotEquals(tables(defaults), tables(alternate),
                id + " emitted the same DP simulation for materially different inputs");
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

    private static List<DpTable> tables(ExecutionTrace trace) {
        return trace.getSteps().stream().map(ExecutionStep::getDpTable).toList();
    }

    private static void assertReadCoordinates(String id, ExecutionTrace trace) {
        switch (id) {
            case "longest-increasing-subsequence" -> assertClassicLisReadsDependencies(trace);
            case "lis-binary-search" -> assertTailsReadsTheProbedSlot(trace);
            case "print-lis" -> assertPrintLisReadsOnlyEvaluatedDependencies(trace);
            default -> throw new AssertionError("Unhandled LIS tracer " + id);
        }
    }

    private static void assertClassicLisReadsDependencies(ExecutionTrace trace) {
        ExecutionStep fill = trace.getSteps().stream()
                .filter(step -> step.getVariables().containsKey("p"))
                .findFirst().orElseThrow();
        int i = Integer.parseInt(fill.getVariables().get("i"));
        int p = Integer.parseInt(fill.getVariables().get("p"));
        DpTable table = fill.getDpTable();

        assertEquals("probe", table.cells().get(i).get(p).state());
        assertEquals("read", table.cells().get(i + 1).get(p).state(),
                "dp[i+1][p] is the skip dependency");
        if (!"x".equals(fill.getVariables().get("take"))) {
            assertEquals("read", table.cells().get(i + 1).get(i + 1).state(),
                    "dp[i+1][i+1] is the take dependency");
        }
        assertEquals("void", table.cells().get(0).get(1).state(),
                "p > i is structurally invalid in the triangular recurrence");
    }

    private static void assertTailsReadsTheProbedSlot(ExecutionTrace trace) {
        DpTable probe = trace.getSteps().stream()
                .map(ExecutionStep::getDpTable)
                .filter(table -> table.cells().get(1).stream()
                        .anyMatch(cell -> "read".equals(cell.state())))
                .findFirst().orElseThrow();

        assertEquals(1, probe.cells().get(0).stream()
                .filter(cell -> "probe".equals(cell.state())).count(),
                "the input value being placed must remain visible");
        assertEquals(1, probe.cells().get(1).stream()
                .filter(cell -> "read".equals(cell.state())).count(),
                "binary search reads exactly one tails[mid] per probe");
    }

    private static void assertPrintLisReadsOnlyEvaluatedDependencies(ExecutionTrace trace) {
        ExecutionStep shortCircuit = trace.getSteps().stream()
                .filter(step -> step.getDescription().contains("is not below"))
                .findFirst().orElseThrow();
        int i = Integer.parseInt(shortCircuit.getVariables().get("i"));
        int j = Integer.parseInt(shortCircuit.getVariables().get("j"));
        DpTable compare = shortCircuit.getDpTable();
        assertEquals("read", compare.cells().get(0).get(i).state());
        assertEquals("read", compare.cells().get(0).get(j).state());
        assertEquals("probe", compare.cells().get(1).get(i).state());
        assertNotEquals("read", compare.cells().get(1).get(j).state(),
                "dp[j] must not look read after nums[j] >= nums[i] short-circuits");

        ExecutionStep recurrence = trace.getSteps().stream()
                .filter(step -> step.getDescription().contains("beats dp"))
                .findFirst().orElseThrow();
        i = Integer.parseInt(recurrence.getVariables().get("i"));
        j = Integer.parseInt(recurrence.getVariables().get("j"));
        assertEquals("read", recurrence.getDpTable().cells().get(1).get(j).state());
        assertEquals("probe", recurrence.getDpTable().cells().get(1).get(i).state());

        ExecutionStep backlink = trace.getSteps().stream()
                .filter(step -> step.getVariables().containsKey("cursor"))
                .findFirst().orElseThrow();
        int cursor = Integer.parseInt(backlink.getVariables().get("cursor"));
        DpTable backlinkTable = backlink.getDpTable();
        assertEquals("read", backlinkTable.cells().get(2).get(cursor).state(),
                "backtracking reads parent[cursor]");
        int parent = Integer.parseInt(backlinkTable.cells().get(2).get(cursor).value());
        if (parent >= 0) {
            assertTrue(backlinkTable.cells().stream()
                            .noneMatch(row -> "read".equals(row.get(parent).state())),
                    "the next backlink is not read until the next step");
        }
    }
}
