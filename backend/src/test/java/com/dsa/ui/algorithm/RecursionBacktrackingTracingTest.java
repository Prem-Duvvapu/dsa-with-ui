package com.dsa.ui.algorithm;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.service.LegacyTraceRetiredException;
import com.dsa.ui.service.RecursionBacktrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RecursionBacktrackingTracingTest {

    private RecursionBacktrackingService service;

    @BeforeEach
    void setUp() {
        service = new RecursionBacktrackingService();
    }

    @Test
    void testNQueensTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> service.generateSteps("n-queens"),
                "n-queens is traced by the v2 layer and must not fall back");
    }

    @Test
    void testRatInMazeTracing() {
        List<ExecutionStep> steps = service.generateSteps("rat-in-a-maze");
        assertNotNull(steps);
        assertTrue(steps.size() >= 10, "Rat in a Maze trace should have >=10 steps, actual: " + steps.size());
    }

    @Test
    void testSudokuSolverTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> service.generateSteps("sudoku-solver"),
                "sudoku-solver is traced by the v2 layer and must not fall back");
    }

    @Test
    void testMColoringTracing() {
        List<ExecutionStep> steps = service.generateSteps("m-coloring");
        assertNotNull(steps);
        assertTrue(steps.size() >= 5, "M-Coloring trace should have >=5 steps, actual: " + steps.size());
    }

    @Test
    void testPalindromePartitioningTracing() {
        List<ExecutionStep> steps = service.generateSteps("palindrome-partitioning");
        assertNotNull(steps);
        assertTrue(steps.size() >= 8, "Palindrome Partitioning trace should have >=8 steps, actual: " + steps.size());
    }

    @Test
    void testSubsetsTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> service.generateSteps("subsets-i"),
                "subsets-i is traced by the v2 layer and must not fall back");
    }

    @Test
    void testCombinationSumTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> service.generateSteps("combination-sum-i"),
                "combination-sum-i is traced by the v2 layer and must not fall back");
    }

    @Test
    void testPermutationsTracing() {
        List<ExecutionStep> steps = service.generateSteps("permutations");
        assertNotNull(steps);
        assertTrue(steps.size() >= 20, "Permutations trace should have >=20 steps for 3!, actual: " + steps.size());
    }

    @Test
    void testWordSearchTracing() {
        List<ExecutionStep> steps = service.generateSteps("word-search");
        assertNotNull(steps);
        assertTrue(steps.size() >= 8, "Word Search trace should have >=8 steps, actual: " + steps.size());
    }
}
