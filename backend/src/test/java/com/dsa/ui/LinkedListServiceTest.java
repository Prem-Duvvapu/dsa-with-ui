package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.LinkedListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LinkedListServiceTest {

    private LinkedListService service;

    @BeforeEach
    public void setUp() {
        service = new LinkedListService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(5, problems.size());
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("reverse-linked-list");
        assertNotNull(problem);
        assertEquals("Reverse Linked List", problem.getTitle());
    }

    @Test
    public void testGenerateStepsForAllLinkedListProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            List<ExecutionStep> steps = service.generateSteps(p.getId());
            assertNotNull(steps, "Steps list should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps list should not be empty for " + p.getId());
        }
    }
}
