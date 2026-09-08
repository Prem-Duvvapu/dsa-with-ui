package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.HeapService;
import com.dsa.ui.service.LegacyTraceRetiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class HeapServiceTest {

    private HeapService service;

    @BeforeEach
    public void setUp() {
        service = new HeapService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(17, problems.size(), "Should load 17 Heap algorithms");
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("kth-largest-element");
        assertNotNull(problem);
    }

    @Test
    public void testGenerateStepsForAllHeapProblems() {
        Set<String> retired = Set.of(
                "kth-largest-element", "kth-smallest-element", "task-scheduler", "top-k-frequent-elements",
                "hand-of-straights", "min-cost-connect-sticks", "median-data-stream", "merge-k-sorted-lists",
                "heaps-theory", "implement-min-heap", "check-min-heap", "min-to-max-heap",
                "sort-k-sorted-array", "replace-rank-array", "design-twitter",
                "kth-largest-stream", "maximum-sum-combination");
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            if (retired.contains(p.getId())) {
                assertThrows(LegacyTraceRetiredException.class,
                        () -> service.generateSteps(p.getId()),
                        p.getId() + " is traced by the v2 layer and must not fall back");
                continue;
            }
            List<ExecutionStep> steps = service.generateSteps(p.getId());
            assertNotNull(steps, "Steps list should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps list should not be empty for " + p.getId());
        }
    }
}
