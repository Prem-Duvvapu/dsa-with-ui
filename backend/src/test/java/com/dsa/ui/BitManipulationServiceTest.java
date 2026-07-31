package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.BitManipulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BitManipulationServiceTest {

    private BitManipulationService service;

    @BeforeEach
    public void setUp() {
        service = new BitManipulationService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertTrue(problems.size() >= 2);
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("single-number");
        assertNotNull(problem);
        assertEquals("Single Number (XOR Property)", problem.getTitle());
    }

    @Test
    public void testGenerateSteps() {
        List<ExecutionStep> steps = service.generateSteps("single-number");
        assertNotNull(steps);
        assertFalse(steps.isEmpty());
    }
}
