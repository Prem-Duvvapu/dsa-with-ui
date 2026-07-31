package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.LinkedListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinkedListServiceTest {

    private LinkedListService linkedListService;

    @BeforeEach
    void setUp() {
        linkedListService = new LinkedListService();
    }

    @Test
    @DisplayName("Should return all linked list problems")
    void testGetAllProblems() {
        List<ProblemDetail> problems = linkedListService.getAllProblems();
        assertNotNull(problems);
        assertEquals(5, problems.size(), "Should load 5 linked list problems");
    }

    @Test
    @DisplayName("Should retrieve Floyd's Cycle Detection details")
    void testGetProblemById() {
        ProblemDetail detectLoop = linkedListService.getProblemById("detect-loop-linked-list");
        assertNotNull(detectLoop);
        assertEquals("Detect Loop in Linked List (Floyd's Cycle)", detectLoop.getTitle());
    }

    @Test
    @DisplayName("Should generate execution steps for Reverse and Middle Linked List")
    void testGenerateSteps() {
        List<ExecutionStep> reverseSteps = linkedListService.generateSteps("reverse-linked-list");
        assertNotNull(reverseSteps);
        assertNotNull(reverseSteps.get(0).getListState());

        List<ExecutionStep> middleSteps = linkedListService.generateSteps("middle-linked-list");
        assertNotNull(middleSteps);
        assertFalse(middleSteps.isEmpty());
    }
}
