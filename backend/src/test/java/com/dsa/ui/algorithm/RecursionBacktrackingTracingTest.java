package com.dsa.ui.algorithm;

import com.dsa.ui.model.ExecutionStep;
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
        List<ExecutionStep> steps = service.generateSteps("n-queens");
        assertNotNull(steps);
        assertTrue(steps.size() >= 40, "N-Queens trace should have >=40 steps, actual: " + steps.size());
        assertEquals(1, steps.get(0).getStepNumber());
    }

    @Test
    void testRatInMazeTracing() {
        List<ExecutionStep> steps = service.generateSteps("rat-in-a-maze");
        assertNotNull(steps);
        assertTrue(steps.size() >= 10, "Rat in a Maze trace should have >=10 steps, actual: " + steps.size());
    }

    @Test
    void testSudokuSolverTracing() {
        List<ExecutionStep> steps = service.generateSteps("sudoku-solver");
        assertNotNull(steps);
        assertTrue(steps.size() >= 10, "Sudoku Solver trace should have >=10 steps, actual: " + steps.size());
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
        List<ExecutionStep> steps = service.generateSteps("subsets-i");
        assertNotNull(steps);
        assertTrue(steps.size() >= 15, "Subsets trace should have >=15 steps, actual: " + steps.size());
    }

    @Test
    void testCombinationSumTracing() {
        List<ExecutionStep> steps = service.generateSteps("combination-sum-i");
        assertNotNull(steps);
        assertTrue(steps.size() >= 10, "Combination Sum trace should have >=10 steps, actual: " + steps.size());
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
