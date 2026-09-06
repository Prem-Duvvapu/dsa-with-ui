package com.dsa.ui;

import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.LegacyTraceRetiredException;
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

    /**
     * All five sorting problems now have real tracers in tracer/impl - the legacy
     * generators are gone on purpose, so every id must refuse rather than fall back to
     * another algorithm's steps.
     */
    @Test
    @DisplayName("Should refuse the legacy execute path for every sorting algorithm")
    void testGenerateStepsRetiredForAllSortingProblems() {
        List<ProblemDetail> problems = sortingService.getAllProblems();
        assertEquals(5, problems.size());
        for (ProblemDetail p : problems) {
            assertThrows(LegacyTraceRetiredException.class,
                    () -> sortingService.generateSteps(p.getId()),
                    p.getId() + " is traced by the v2 layer and must not fall back");
        }
    }
}
