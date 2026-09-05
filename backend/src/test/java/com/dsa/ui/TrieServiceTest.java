package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.LegacyTraceRetiredException;
import com.dsa.ui.service.TrieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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
        List<ExecutionStep> steps = service.generateSteps("longest-common-prefix");
        assertNotNull(steps);
        assertFalse(steps.isEmpty());
    }

    /**
     * implement-trie and word-break-trie are traced by the v2 tracer layer
     * (ImplementTrieTracer, WordBreakTrieTracer). Their legacy narration is gone on
     * purpose: serving it would substitute a canned trace for the real one.
     */
    @Test
    public void testRetiredIdsRefuseTheLegacyGenerator() {
        Set<String> retired = Set.of("implement-trie", "word-break-trie");
        for (String id : retired) {
            assertThrows(LegacyTraceRetiredException.class,
                    () -> service.generateSteps(id),
                    id + " is traced by the v2 layer and must not fall back");
        }
    }
}
