package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.TreeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreeServiceTest {

    private TreeService service;

    @BeforeEach
    void setUp() {
        service = new TreeService();
    }

    @Test
    @DisplayName("Should return all 13 Striver A2Z Tree & BST problems")
    void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(13, problems.size(), "Should load 13 Tree & BST problems");
    }

    @Test
    @DisplayName("Should retrieve specific Tree problem details by ID")
    void testGetProblemById() {
        ProblemDetail preorder = service.getProblemById("tree-preorder");
        assertNotNull(preorder);
        assertEquals("Preorder Traversal of Binary Tree", preorder.getTitle());
        assertEquals("Binary Trees", preorder.getCategory());
        assertNotNull(preorder.getComplexity());
        assertEquals("O(N)", preorder.getComplexity().getTimeComplexity());
    }

    @Test
    @DisplayName("Should generate valid execution steps for Preorder, Level Order, and BST Search")
    void testGenerateSteps() {
        List<ExecutionStep> preorderSteps = service.generateSteps("tree-preorder");
        assertNotNull(preorderSteps);
        assertFalse(preorderSteps.isEmpty());

        List<ExecutionStep> levelOrderSteps = service.generateSteps("tree-level-order");
        assertNotNull(levelOrderSteps);
        assertEquals("Queue", levelOrderSteps.get(0).getDsType());

        List<ExecutionStep> bstSearchSteps = service.generateSteps("bst-search");
        assertNotNull(bstSearchSteps);
        assertTrue(bstSearchSteps.stream().anyMatch(s -> s.getDescription().contains("FOUND TARGET NODE")));
    }
}
