package com.dsa.ui;

import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.LegacyTraceRetiredException;
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
        assertEquals(16, problems.size(), "Should load 16 String algorithms");
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("longest-substring-without-repeating");
        assertNotNull(problem);
    }

    @Test
    public void testGenerateStepsForAllStringProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            assertThrows(LegacyTraceRetiredException.class,
                    () -> service.generateSteps(p.getId()),
                    p.getId() + " is traced by the v2 layer and must not fall back");
        }
    }
}
