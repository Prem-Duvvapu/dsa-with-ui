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
        assertEquals(18, problems.size(), "Should load 18 Bit & Math algorithms");
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("single-number-1");
        assertNotNull(problem);
    }

    @Test
    public void testGenerateStepsForAllBitMathProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            List<ExecutionStep> steps = service.generateSteps(p.getId());
            assertNotNull(steps, "Steps list should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps list should not be empty for " + p.getId());
        }
    }
}
