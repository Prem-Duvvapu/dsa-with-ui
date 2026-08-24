package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.GreedyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dsa.ui.service.LegacyTraceRetiredException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GreedyServiceTest {

    private GreedyService service;

    @BeforeEach
    public void setUp() {
        service = new GreedyService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(15, problems.size(), "Should load 15 Greedy algorithms");
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("n-meetings-in-one-room");
        assertNotNull(problem);
    }

    @Test
    public void testGenerateStepsForAllGreedyProblems() {
        Set<String> retired = Set.of("n-meetings-in-one-room");
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            if (retired.contains(p.getId())) {
                assertThrows(LegacyTraceRetiredException.class,
                        () -> service.generateSteps(p.getId()),
                        p.getId() + " is traced by the v2 layer and must not fall back");
                continue;
            }
            List<ExecutionStep> steps = service.generateSteps(p.getId());
            assertNotNull(steps, "Steps list should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps list should not be empty for " + p.getId());
        }
    }
}
