package com.dsa.ui.tracer;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemainingDpTopicTracerTest {

    @Autowired private TracerRegistry registry;
    @Autowired private TraceRunner runner;

    Stream<Arguments> expectedDefaultResults() {
        return Stream.of(
                Arguments.of("longest-common-subsequence", "3"),
                Arguments.of("partition-set-min-abs-diff", "1"),
                Arguments.of("assign-cookies-dp", "1"),
                Arguments.of("target-sum-dp", "2"),
                Arguments.of("rod-cutting-problem", "12"),
                Arguments.of("print-longest-common-subsequence", "bde"),
                Arguments.of("longest-common-substring", "2"),
                Arguments.of("longest-palindromic-subsequence", "4"),
                Arguments.of("min-insertions-palindrome", "2"),
                Arguments.of("min-insertions-deletions-a-b", "3"),
                Arguments.of("shortest-common-supersequence", "cabac"),
                Arguments.of("distinct-subsequences", "3"),
                Arguments.of("best-time-stock-1", "5"),
                Arguments.of("best-time-stock-2", "7"),
                Arguments.of("best-time-stock-3", "6"),
                Arguments.of("best-time-stock-4", "7"),
                Arguments.of("stock-cooldown", "3"),
                Arguments.of("stock-transaction-fee", "8"),
                Arguments.of("longest-string-chain", "4"),
                Arguments.of("longest-bitonic-subsequence", "6"),
                Arguments.of("number-of-lis", "2"),
                Arguments.of("largest-divisible-subset", "4"),
                Arguments.of("mcm-cost-eval", "26000"),
                Arguments.of("evaluate-boolean-expression", "2"),
                Arguments.of("palindrome-partitioning-2", "1"),
                Arguments.of("partition-array-max-sum", "84"),
                Arguments.of("matrix-chain-multiplication-theory", "4500"));
    }

    @ParameterizedTest(name = "{0} returns {1} and emits complete DP snapshots")
    @MethodSource("expectedDefaultResults")
    void defaultResultsAndTablesAreCorrect(String id, String expected) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        ExecutionTrace trace = runner.runDefaults(tracer);
        assertFalse(trace.getSteps().isEmpty(), id);
        assertEquals(DsType.DP_TABLE, tracer.dsType(), id);

        for (ExecutionStep step : trace.getSteps()) {
            DpTable table = step.getDpTable();
            assertNotNull(table, id + " step " + step.getStepNumber());
            assertNotNull(table.formula(), id + " formula");
            assertNotNull(table.substitution(), id + " substitution");
            assertEquals(table.rowLabels().size(), table.cells().size(), id);
            for (java.util.List<DpCell> row : table.cells()) {
                assertEquals(table.colLabels().size(), row.size(), id);
            }
        }

        ExecutionStep last = trace.getSteps().get(trace.getSteps().size() - 1);
        assertEquals(expected, last.getVariables().get("answer"), id);
    }
}
