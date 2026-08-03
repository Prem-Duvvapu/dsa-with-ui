package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.TrieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TrieServiceTest {

    private TrieService service;

    @BeforeEach
    public void setUp() {
        service = new TrieService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertTrue(problems.size() >= 3);
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("implement-trie");
        assertNotNull(problem);
        assertEquals("Implement Trie (Prefix Tree)", problem.getTitle());
    }

    @Test
    public void testGenerateSteps() {
        List<ExecutionStep> steps = service.generateSteps("implement-trie");
        assertNotNull(steps);
        assertFalse(steps.isEmpty());
    }
}
