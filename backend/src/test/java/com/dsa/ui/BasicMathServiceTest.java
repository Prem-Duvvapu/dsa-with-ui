package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.BasicMathService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BasicMathServiceTest {

    private BasicMathService service;

    @BeforeEach
    public void setUp() {
        service = new BasicMathService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(7, problems.size());
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("count-digits");
        assertNotNull(problem);
        assertEquals("Count Digits of a Number", problem.getTitle());
    }

    @Test
    public void testGenerateStepsForAllMathProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            List<ExecutionStep> steps = service.generateSteps(p.getId());
            assertNotNull(steps, "Steps should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps should not be empty for " + p.getId());
        }
    }
}
