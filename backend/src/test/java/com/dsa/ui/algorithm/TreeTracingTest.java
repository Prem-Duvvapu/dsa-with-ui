package com.dsa.ui.algorithm;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.service.TreeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TreeTracingTest {

    private TreeService treeService;

    @BeforeEach
    void setUp() {
        treeService = new TreeService();
    }

    @Test
    void testInorderTraversalTracing() {
        List<ExecutionStep> steps = treeService.generateSteps("tree-inorder");
        assertNotNull(steps);
        assertTrue(steps.size() >= 10, "Tree Inorder should have >=10 steps, actual: " + steps.size());
    }

    @Test
    void testPreorderTraversalTracing() {
        List<ExecutionStep> steps = treeService.generateSteps("tree-preorder");
        assertNotNull(steps);
        assertTrue(steps.size() >= 6, "Tree Preorder should have >=6 steps, actual: " + steps.size());
    }

    @Test
    void testPostorderTraversalTracing() {
        List<ExecutionStep> steps = treeService.generateSteps("tree-postorder");
        assertNotNull(steps);
        assertTrue(steps.size() >= 6, "Tree Postorder should have >=6 steps, actual: " + steps.size());
    }

    @Test
    void testLevelOrderTraversalTracing() {
        List<ExecutionStep> steps = treeService.generateSteps("tree-level-order");
        assertNotNull(steps);
        assertTrue(steps.size() >= 6, "Tree Level Order should have >=6 steps, actual: " + steps.size());
    }
}
