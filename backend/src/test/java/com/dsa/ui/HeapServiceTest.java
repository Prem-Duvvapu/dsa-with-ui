package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.HeapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertTrue(problems.size() >= 2);
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("kth-largest-element");
        assertNotNull(problem);
        assertEquals("Kth Largest Element in an Array", problem.getTitle());
    }

    @Test
    public void testGenerateSteps() {
        List<ExecutionStep> steps = service.generateSteps("kth-largest-element");
        assertNotNull(steps);
        assertFalse(steps.isEmpty());
    }
}
