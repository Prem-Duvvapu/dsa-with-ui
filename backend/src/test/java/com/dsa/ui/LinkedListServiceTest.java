package com.dsa.ui;

import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.LinkedListService;
import com.dsa.ui.service.LegacyTraceRetiredException;
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
        assertEquals(31, problems.size(), "Should load 31 LinkedList & Doubly LL algorithms");
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("reverse-linked-list");
        assertNotNull(problem);
    }

    @Test
    public void testGenerateStepsForAllLinkedListProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            assertThrows(LegacyTraceRetiredException.class,
                    () -> service.generateSteps(p.getId()),
                    p.getId() + " is traced by the v2 layer and must not fall back");
        }
    }
}
