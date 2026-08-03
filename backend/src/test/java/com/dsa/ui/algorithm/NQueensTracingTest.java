package com.dsa.ui.algorithm;

import com.dsa.ui.algorithm.backtracking.NQueens;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.service.RecursionBacktrackingService;
import com.dsa.ui.trace.ListTraceRecorder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NQueensTracingTest {

    @Test
    void testNQueensFullTraceGeneration() {
        ListTraceRecorder recorder = new ListTraceRecorder();
        NQueens solver = new NQueens();
        List<List<String>> solutions = solver.solve(4, recorder);

        // N-Queens for N=4 must find exactly 2 distinct solutions
        assertEquals(2, solutions.size(), "N-Queens 4x4 should find exactly 2 solutions");

        List<ExecutionStep> steps = recorder.toExecutionSteps();
        // A complete execution trace for N=4 includes ~60-70 steps (checking all branches, placements, conflicts, backtracks, and solutions)
        assertTrue(steps.size() > 40, "Trace should contain full execution steps (>40 steps), actual: " + steps.size());

        // Verify first step is initialization
        assertEquals("start", recorder.getEvents().get(0).getOperation());

        // Verify last step is completion
        assertEquals("complete", recorder.getEvents().get(recorder.getEvents().size() - 1).getOperation());

        // Verify that solution steps exist in the trace
        long solutionCount = recorder.getEvents().stream()
                .filter(e -> "solution".equals(e.getOperation()))
                .count();
        assertEquals(2, solutionCount, "Should record 2 solution events in the trace");
    }

    @Test
    void testServiceNQueensExecutionSteps() {
        RecursionBacktrackingService service = new RecursionBacktrackingService();
        List<ExecutionStep> steps = service.generateSteps("n-queens");
        assertNotNull(steps);
        assertTrue(steps.size() > 40, "Service should return full execution steps for N-Queens");
    }
}
