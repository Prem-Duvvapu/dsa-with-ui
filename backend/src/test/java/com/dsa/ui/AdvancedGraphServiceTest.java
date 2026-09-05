package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.AdvancedGraphService;
import com.dsa.ui.service.LegacyTraceRetiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedGraphServiceTest {

    private AdvancedGraphService service;

    @BeforeEach
    void setUp() {
        service = new AdvancedGraphService();
    }

    @Test
    @DisplayName("Should load all 62 Striver Graph and String problems")
    void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(62, problems.size(), "Should load 62 Graph & String problems");
    }

    @Test
    @DisplayName("Should retrieve problem details by ID")
    void testGetProblemById() {
        ProblemDetail graphIntro = service.getProblemById("graph-intro");
        assertNotNull(graphIntro);

        ProblemDetail kmp = service.getProblemById("kmp-lps-algo");
        assertNotNull(kmp);
    }

    @Test
    @DisplayName("Should generate execution steps for all 62 Graph and String problems")
    void testGenerateSteps() {
        Set<String> retired = Set.of("z-function-algo", "kmp-lps-algo",
                "shortest-palindrome", "longest-happy-prefix",
                "bellman-ford", "kosaraju-scc");
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            if (retired.contains(p.getId())) {
                assertThrows(LegacyTraceRetiredException.class,
                        () -> service.generateSteps(p.getId()),
                        p.getId() + " is traced by the v2 layer and must not fall back");
                continue;
            }
            List<ExecutionStep> steps = service.generateSteps(p.getId());
            assertNotNull(steps, "Steps should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps should not be empty for " + p.getId());
        }
    }
}
