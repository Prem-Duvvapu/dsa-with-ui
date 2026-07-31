package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.ArrayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArrayServiceTest {

    private ArrayService arrayService;

    @BeforeEach
    void setUp() {
        arrayService = new ArrayService();
    }

    @Test
    @DisplayName("Should return all array algorithm problems")
    void testGetAllProblems() {
        List<ProblemDetail> problems = arrayService.getAllProblems();
        assertNotNull(problems);
        assertEquals(5, problems.size(), "Should load 5 array problems");
    }

    @Test
    @DisplayName("Should retrieve Kadane's Algorithm details")
    void testGetProblemById() {
        ProblemDetail kadane = arrayService.getProblemById("kadane-algo");
        assertNotNull(kadane);
        assertEquals("Kadane's Algorithm (Max Subarray Sum)", kadane.getTitle());
        assertEquals("O(N)", kadane.getComplexity().getTimeComplexity());
    }

    @Test
    @DisplayName("Should generate execution steps for Two Sum and Dutch National Flag")
    void testGenerateSteps() {
        List<ExecutionStep> twoSumSteps = arrayService.generateSteps("two-sum");
        assertNotNull(twoSumSteps);
        assertFalse(twoSumSteps.isEmpty());

        List<ExecutionStep> dnfSteps = arrayService.generateSteps("sort-0-1-2");
        assertNotNull(dnfSteps);
        assertFalse(dnfSteps.isEmpty());
    }
}
