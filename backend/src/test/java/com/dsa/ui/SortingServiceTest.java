package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.SortingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SortingServiceTest {

    private SortingService sortingService;

    @BeforeEach
    void setUp() {
        sortingService = new SortingService();
    }

    @Test
    @DisplayName("Should return all 5 sorting algorithm problems")
    void testGetAllProblems() {
        List<ProblemDetail> problems = sortingService.getAllProblems();
        assertNotNull(problems);
        assertEquals(5, problems.size(), "Should return 5 sorting problems");
    }

    @Test
    @DisplayName("Should retrieve specific sorting problem by ID")
    void testGetProblemById() {
        ProblemDetail mergeSort = sortingService.getProblemById("merge-sort");
        assertNotNull(mergeSort);
        assertEquals("Merge Sort (Divide & Conquer)", mergeSort.getTitle());
        assertEquals("O(N log N)", mergeSort.getComplexity().getTimeComplexity());
    }

    @Test
    @DisplayName("Should generate execution steps for ALL sorting algorithms")
    void testGenerateStepsForAllSortingProblems() {
        List<ProblemDetail> problems = sortingService.getAllProblems();
        for (ProblemDetail p : problems) {
            List<ExecutionStep> steps = sortingService.generateSteps(p.getId());
            assertNotNull(steps, "Steps list should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps list should not be empty for " + p.getId());
            assertTrue(steps.get(0).getStepNumber() > 0, "Step number should be positive for " + p.getId());
        }
    }
}
