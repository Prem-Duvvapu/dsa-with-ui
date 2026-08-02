package com.dsa.ui.algorithm;

import com.dsa.ui.algorithm.sorting.MergeSort;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.service.SortingService;
import com.dsa.ui.trace.ListTraceRecorder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MergeSortTracingTest {

    @Test
    void testMergeSortFullTraceGeneration() {
        ListTraceRecorder recorder = new ListTraceRecorder();
        MergeSort solver = new MergeSort();

        int[] arr = {13, 46, 24, 52, 20, 9};
        solver.solve(arr, recorder);

        // Check array is sorted in-place
        assertArrayEquals(new int[]{9, 13, 20, 24, 46, 52}, arr);

        List<ExecutionStep> steps = recorder.toExecutionSteps();
        assertTrue(steps.size() >= 25, "Merge sort should generate at least 25 trace steps for 6 elements");
    }

    @Test
    void testSortingServiceMergeSortSteps() {
        SortingService service = new SortingService();
        List<ExecutionStep> steps = service.generateSteps("merge-sort");
        assertNotNull(steps);
        assertTrue(steps.size() >= 25);
    }
}
