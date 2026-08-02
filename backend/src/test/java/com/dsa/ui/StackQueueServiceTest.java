package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.StackQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StackQueueServiceTest {

    private StackQueueService service;

    @BeforeEach
    public void setUp() {
        service = new StackQueueService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(30, problems.size(), "Should load 30 Stack & Queue algorithms");
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("balanced-parentheses");
        assertNotNull(problem);
    }

    @Test
    public void testGenerateStepsForAllStackQueueProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            List<ExecutionStep> steps = service.generateSteps(p.getId());
            assertNotNull(steps, "Steps list should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps list should not be empty for " + p.getId());
        }
    }
}
