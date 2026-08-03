package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.BasicRecursionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BasicRecursionServiceTest {

    private BasicRecursionService service;

    @BeforeEach
    public void setUp() {
        service = new BasicRecursionService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(7, problems.size());
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("print-1-to-n");
        assertNotNull(problem);
        assertEquals("Print 1 to N using Recursion", problem.getTitle());
    }

    @Test
    public void testGenerateStepsForAllBasicRecursionProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            List<ExecutionStep> steps = service.generateSteps(p.getId());
            assertNotNull(steps, "Steps should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps should not be empty for " + p.getId());
        }
    }
}
