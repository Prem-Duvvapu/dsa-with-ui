package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.DpService;
import com.dsa.ui.service.LegacyTraceRetiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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
        Set<String> retired = Set.of(
                "matrix-chain-multiplication",
                "burst-balloons",
                "knapsack-01",
                "unbounded-knapsack",
                "climbing-stairs",
                "frog-jump",
                "frog-jump-k-distance",
                "max-sum-non-adjacent",
                "house-robber-2",
                "grid-unique-paths",
                "unique-paths-2",
                "minimum-falling-path-sum",
                "triangle-min-path-sum",
                "ninjas-training",
                "longest-increasing-subsequence",
                "print-lis",
                "lis-binary-search",
                "max-rectangle-area-all-ones",
                "count-square-submatrices",
                "subset-sum-equal-target",
                "partition-equal-subset-sum",
                "count-subsets-with-sum-k",
                "count-partitions-given-diff",
                "minimum-coins-dp",
                "coin-change-2",
                "edit-distance",
                "wildcard-matching",
                "ninja-and-his-friends");
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
