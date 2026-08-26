package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.ArrayService;
import com.dsa.ui.service.LegacyTraceRetiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ArrayServiceTest {

    private ArrayService service;

    @BeforeEach
    public void setUp() {
        service = new ArrayService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(40, problems.size());
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("two-sum");
        assertNotNull(problem);
        assertEquals("Two Sum", problem.getTitle());
    }

    @Test
    public void testGenerateStepsForAllArrayProblems() {
        Set<String> retired = Set.of(
                "largest-element",
                "max-consecutive-ones",
                "move-zeros-end",
                "find-missing-number",
                "stock-buy-sell",
                "second-largest-element",
                "check-sorted-ii",
                "remove-duplicates-sorted",
                "left-rotate-one",
                "linear-search",
                "left-rotate-k",
                "single-number",
                "majority-element",
                "leaders-in-array",
                "longest-subarray-sum-k-positives");
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
