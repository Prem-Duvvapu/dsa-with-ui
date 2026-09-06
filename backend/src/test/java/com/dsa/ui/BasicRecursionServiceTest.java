package com.dsa.ui;

import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.BasicRecursionService;
import com.dsa.ui.service.LegacyTraceRetiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BasicRecursionServiceTest {

    private BasicRecursionService service;

    @BeforeEach
    public void setUp() {
        service = new BasicRecursionService();
    }

    @Test
    public void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(7, problems.size());
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("print-1-to-n");
        assertNotNull(problem);
        assertEquals("Print 1 to N using Recursion", problem.getTitle());
    }

    /**
     * All seven ids in this service now have real tracers in tracer/impl - the legacy
     * generators are gone on purpose, so every id must refuse rather than fall back to
     * another algorithm's steps.
     */
    @Test
    public void testGenerateStepsRetiredForAllBasicRecursionProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            assertThrows(LegacyTraceRetiredException.class,
                    () -> service.generateSteps(p.getId()),
                    p.getId() + " is traced by the v2 layer and must not fall back");
        }
    }
}
