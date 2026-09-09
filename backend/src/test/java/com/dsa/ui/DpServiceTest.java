package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.DpService;
import com.dsa.ui.service.LegacyTraceRetiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DpServiceTest {

    private DpService service;

    @BeforeEach
    public void setUp() {
        service = new DpService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(55, problems.size(), "Should load 55 DP problems");
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("climbing-stairs");
        assertNotNull(problem);
        assertEquals("Climbing Stairs (1D DP)", problem.getTitle());
    }

    @Test
    public void testGenerateStepsForAllDpProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            assertThrows(LegacyTraceRetiredException.class,
                    () -> service.generateSteps(p.getId()),
                    p.getId() + " is traced by the v2 layer and must not fall back");
        }
        assertThrows(IllegalArgumentException.class, () -> service.generateSteps("not-a-dp-problem"));
    }
}
