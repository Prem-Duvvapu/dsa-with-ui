package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.RecursionBacktrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RecursionBacktrackingServiceTest {

    private RecursionBacktrackingService service;

    @BeforeEach
    public void setUp() {
        service = new RecursionBacktrackingService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertTrue(problems.size() >= 9);
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("n-queens");
        assertNotNull(problem);
        assertEquals("N-Queens Problem", problem.getTitle());
    }

    @Test
    public void testGenerateSteps() {
        List<ExecutionStep> steps = service.generateSteps("n-queens");
        assertNotNull(steps);
        assertFalse(steps.isEmpty());
    }
}
