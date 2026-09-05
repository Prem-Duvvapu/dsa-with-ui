package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.TreeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.dsa.ui.service.LegacyTraceRetiredException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TreeServiceTest {

    private TreeService service;

    @BeforeEach
    void setUp() {
        service = new TreeService();
    }

    @Test
    @DisplayName("Should return all 54 Striver A2Z Tree & BST problems")
    void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(54, problems.size(), "Should load 54 Tree & BST problems");
    }

    @Test
    @DisplayName("Should retrieve specific Tree problem details by ID")
    void testGetProblemById() {
        ProblemDetail preorder = service.getProblemById("tree-preorder");
        assertNotNull(preorder);
    }

    @Test
    @DisplayName("Should generate valid execution steps for all 54 Tree and BST problems")
    void testGenerateSteps() {
        // Ids with real tracers refuse the legacy path rather than serve a substitute.
        Set<String> retired = Set.of("tree-preorder", "tree-inorder", "tree-postorder",
                "tree-level-order", "tree-max-path-sum", "serialize-deserialize-bt",
                "zigzag-traversal", "tree-lca");
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            if (retired.contains(p.getId())) {
                assertThrows(LegacyTraceRetiredException.class,
                        () -> service.generateSteps(p.getId()),
                        p.getId() + " is traced by the v2 layer and must not fall back");
                continue;
            }
            List<ExecutionStep> steps = service.generateSteps(p.getId());
            assertNotNull(steps, "Steps list should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps list should not be empty for " + p.getId());
        }
    }
}
