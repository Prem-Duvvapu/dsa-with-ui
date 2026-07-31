package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.GreedyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GreedyServiceTest {

    private GreedyService service;

    @BeforeEach
    public void setUp() {
        service = new GreedyService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertTrue(problems.size() >= 3);
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("n-meetings-in-one-room");
        assertNotNull(problem);
        assertEquals("N Meetings in One Room", problem.getTitle());
    }

    @Test
    public void testGenerateSteps() {
        List<ExecutionStep> steps = service.generateSteps("n-meetings-in-one-room");
        assertNotNull(steps);
        assertFalse(steps.isEmpty());
    }
}
