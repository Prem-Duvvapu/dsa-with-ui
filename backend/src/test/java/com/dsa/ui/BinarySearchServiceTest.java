package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.BinarySearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertEquals(4, problems.size());
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("binary-search-1d");
        assertNotNull(problem);
        assertEquals("Binary Search on 1D Array", problem.getTitle());
    }

    @Test
    public void testGenerateStepsForAllBinarySearchProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            List<ExecutionStep> steps = service.generateSteps(p.getId());
            assertNotNull(steps, "Steps list should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps list should not be empty for " + p.getId());
        }
    }
}
