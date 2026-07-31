package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.StringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StringServiceTest {

    private StringService service;

    @BeforeEach
    public void setUp() {
        service = new StringService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertTrue(problems.size() >= 2);
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("longest-substring-without-repeating");
        assertNotNull(problem);
        assertEquals("Longest Substring Without Repeating Characters", problem.getTitle());
    }

    @Test
    public void testGenerateSteps() {
        List<ExecutionStep> steps = service.generateSteps("longest-substring-without-repeating");
        assertNotNull(steps);
        assertFalse(steps.isEmpty());
    }
}
