package com.dsa.ui.algorithm;

import com.dsa.ui.service.LegacyTraceRetiredException;
import com.dsa.ui.service.RecursionBacktrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertThrows(LegacyTraceRetiredException.class,
                () -> service.generateSteps("rat-in-a-maze"),
                "rat-in-a-maze is traced by the v2 layer and must not fall back");
    }

    @Test
    void testSudokuSolverTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> service.generateSteps("sudoku-solver"),
                "sudoku-solver is traced by the v2 layer and must not fall back");
    }

    @Test
    void testMColoringTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> service.generateSteps("m-coloring"),
                "m-coloring is traced by the v2 layer and must not fall back");
    }

    @Test
    void testPalindromePartitioningTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> service.generateSteps("palindrome-partitioning"),
                "palindrome-partitioning is traced by the v2 layer and must not fall back");
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
        assertThrows(LegacyTraceRetiredException.class,
                () -> service.generateSteps("permutations"),
                "permutations is traced by the v2 layer and must not fall back");
    }

    @Test
    void testWordSearchTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> service.generateSteps("word-search"),
                "word-search is traced by the v2 layer and must not fall back");
    }
}
