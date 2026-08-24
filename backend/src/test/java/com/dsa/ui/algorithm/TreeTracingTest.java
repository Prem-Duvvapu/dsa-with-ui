package com.dsa.ui.algorithm;

import com.dsa.ui.service.LegacyTraceRetiredException;
import com.dsa.ui.service.TreeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * All four traversal ids have real tracers in tracer/impl now, so the legacy path
 * retires them instead of serving substitute steps. Their trace content is pinned by
 * golden files (src/test/resources/golden) and TracerContractTest.
 */
public class TreeTracingTest {

    private TreeService treeService;

    @BeforeEach
    void setUp() {
        treeService = new TreeService();
    }

    @Test
    void testInorderTraversalTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> treeService.generateSteps("tree-inorder"));
    }

    @Test
    void testPreorderTraversalTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> treeService.generateSteps("tree-preorder"));
    }

    @Test
    void testPostorderTraversalTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> treeService.generateSteps("tree-postorder"));
    }

    @Test
    void testLevelOrderTraversalTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> treeService.generateSteps("tree-level-order"));
    }
}
