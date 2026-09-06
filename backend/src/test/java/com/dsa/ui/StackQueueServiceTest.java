package com.dsa.ui;

import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.LegacyTraceRetiredException;
import com.dsa.ui.service.StackQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StackQueueServiceTest {

    private StackQueueService service;

    @BeforeEach
    public void setUp() {
        service = new StackQueueService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(30, problems.size(), "Should load 30 Stack & Queue algorithms");
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("balanced-parentheses");
        assertNotNull(problem);
    }

    @Test
    public void testGenerateStepsForAllStackQueueProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            assertThrows(LegacyTraceRetiredException.class,
                    () -> service.generateSteps(p.getId()),
                    p.getId() + " is traced by the v2 layer and must not fall back");
        }
    }
}
