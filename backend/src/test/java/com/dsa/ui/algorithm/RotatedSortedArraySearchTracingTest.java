package com.dsa.ui.algorithm;

import com.dsa.ui.algorithm.binarysearch.RotatedSortedArraySearch;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.service.BinarySearchService;
import com.dsa.ui.trace.ListTraceRecorder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RotatedSortedArraySearchTracingTest {

    @Test
    void testRotatedSortedArraySearchTrace() {
        int[] nums = {4, 5, 6, 7, 0, 1, 2};
        int target = 0;

        ListTraceRecorder recorder = new ListTraceRecorder();
        RotatedSortedArraySearch solver = new RotatedSortedArraySearch();
        int foundIdx = solver.solve(nums, target, recorder);

        assertEquals(4, foundIdx, "Target 0 should be found at index 4");

        List<ExecutionStep> steps = recorder.toExecutionSteps();
        assertTrue(steps.size() >= 5, "Binary search on rotated array should produce at least 5 step trace events");
    }

    @Test
    void testBinarySearchServiceRotatedSearchSteps() {
        BinarySearchService service = new BinarySearchService();
        List<ExecutionStep> steps = service.generateSteps("search-rotated-sorted");
        assertNotNull(steps);
        assertTrue(steps.size() >= 5);
    }
}
