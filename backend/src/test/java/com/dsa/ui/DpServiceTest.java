package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.DpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DpServiceTest {

    private DpService service;

    @BeforeEach
    void setUp() {
        service = new DpService();
    }

    @Test
    @DisplayName("Should return all dynamic programming problems")
    void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(4, problems.size(), "Should load 4 DP problems");
    }

    @Test
    @DisplayName("Should retrieve 0/1 Knapsack problem details")
    void testGetProblemById() {
        ProblemDetail knapsack = service.getProblemById("knapsack-01");
        assertNotNull(knapsack);
        assertEquals("0/1 Knapsack Problem", knapsack.getTitle());
    }

    @Test
    @DisplayName("Should generate execution steps for Climbing Stairs and LCS")
    void testGenerateSteps() {
        List<ExecutionStep> stairsSteps = service.generateSteps("climbing-stairs");
        assertNotNull(stairsSteps);
        assertFalse(stairsSteps.isEmpty());

        List<ExecutionStep> lcsSteps = service.generateSteps("longest-common-subsequence");
        assertNotNull(lcsSteps);
        assertNotNull(lcsSteps.get(0).getGridState());
    }
}
