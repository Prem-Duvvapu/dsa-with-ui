package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.BinarySearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BinarySearchServiceTest {

    private BinarySearchService service;

    @BeforeEach
    void setUp() {
        service = new BinarySearchService();
    }

    @Test
    @DisplayName("Should return all binary search problems")
    void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(4, problems.size(), "Should load 4 binary search problems");
    }

    @Test
    @DisplayName("Should retrieve Koko Eating Bananas details")
    void testGetProblemById() {
        ProblemDetail koko = service.getProblemById("koko-eating-bananas");
        assertNotNull(koko);
        assertEquals("Koko Eating Bananas (BS on Answer)", koko.getTitle());
    }

    @Test
    @DisplayName("Should generate execution steps for 1D Binary Search and Rotated Array Search")
    void testGenerateSteps() {
        List<ExecutionStep> bsSteps = service.generateSteps("binary-search-1d");
        assertNotNull(bsSteps);
        assertFalse(bsSteps.isEmpty());

        List<ExecutionStep> rotatedSteps = service.generateSteps("search-rotated-sorted");
        assertNotNull(rotatedSteps);
        assertFalse(rotatedSteps.isEmpty());
    }
}
