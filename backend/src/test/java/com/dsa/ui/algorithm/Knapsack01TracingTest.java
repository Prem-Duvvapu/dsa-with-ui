package com.dsa.ui.algorithm;

import com.dsa.ui.algorithm.dp.Knapsack01;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.service.DpService;
import com.dsa.ui.trace.ListTraceRecorder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Knapsack01TracingTest {

    @Test
    void testKnapsack01FullTraceGeneration() {
        ListTraceRecorder recorder = new ListTraceRecorder();
        Knapsack01 solver = new Knapsack01();

        int[] wt = {1, 2, 3};
        int[] val = {10, 15, 40};
        int W = 5;

        int maxVal = solver.solve(wt, val, W, recorder);
        assertEquals(55, maxVal, "Knapsack max profit should be 55");

        List<ExecutionStep> steps = recorder.toExecutionSteps();
        assertEquals(20, steps.size(), "Knapsack 2D DP should record exactly 20 trace steps for 3 items & W=5");
    }

    @Test
    void testDpServiceKnapsackSteps() {
        DpService service = new DpService();
        List<ExecutionStep> steps = service.generateSteps("knapsack-01");
        assertNotNull(steps);
        assertEquals(23, steps.size());
    }
}
