package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.BinarySearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dsa.ui.service.LegacyTraceRetiredException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BinarySearchServiceTest {

    private BinarySearchService service;

    @BeforeEach
    public void setUp() {
        service = new BinarySearchService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(32, problems.size(), "Should load 32 Binary Search algorithms");
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("binary-search-1d");
        assertNotNull(problem);
    }

    @Test
    public void testGenerateStepsForAllBinarySearchProblems() {
        Set<String> retired = Set.of("search-rotated-sorted");
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
