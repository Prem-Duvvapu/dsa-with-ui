package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.LegacyTraceRetiredException;
import com.dsa.ui.service.SlidingWindowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SlidingWindowServiceTest {

    private SlidingWindowService service;

    @BeforeEach
    public void setUp() {
        service = new SlidingWindowService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(12, problems.size(), "Should load 12 Sliding Window algorithms");
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("longest-substring-without-repeating");
        assertNotNull(problem);
    }

    @Test
    public void testGenerateStepsForAllSlidingWindowProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            assertThrows(LegacyTraceRetiredException.class,
                    () -> service.generateSteps(p.getId()),
                    p.getId() + " is traced by the v2 layer and must not fall back");
        }
    }
}
