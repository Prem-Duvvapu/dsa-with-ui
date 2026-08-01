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
}
