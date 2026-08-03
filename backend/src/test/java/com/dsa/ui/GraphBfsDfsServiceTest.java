package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.GraphBfsDfsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphBfsDfsServiceTest {

    private GraphBfsDfsService service;

    @BeforeEach
    void setUp() {
        service = new GraphBfsDfsService();
    }

    @Test
    @DisplayName("Should load all 11 Striver A2Z Graph BFS & DFS problems")
    void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(11, problems.size(), "Should contain 11 Graph BFS/DFS problems");
    }

    @Test
    @DisplayName("Should retrieve specific problem details by ID")
    void testGetProblemById() {
        ProblemDetail bfs = service.getProblemById("bfs-traversal");
        assertNotNull(bfs);
        assertEquals("BFS Traversal of Graph", bfs.getTitle());
        assertEquals("Easy", bfs.getDifficulty());
        assertNotNull(bfs.getComplexity());
        assertEquals("O(V + 2E)", bfs.getComplexity().getTimeComplexity());

        ProblemDetail nonExistent = service.getProblemById("non-existent");
        assertNull(nonExistent);
    }

    @Test
    @DisplayName("Should generate valid execution steps for BFS Traversal")
    void testGenerateBfsSteps() {
        List<ExecutionStep> steps = service.generateSteps("bfs-traversal");
        assertNotNull(steps);
        assertFalse(steps.isEmpty(), "Steps list should not be empty");

        // Verify sequence order & line numbers
        for (int i = 0; i < steps.size(); i++) {
            ExecutionStep step = steps.get(i);
            assertEquals(i + 1, step.getStepNumber(), "Step numbers should be sequentially 1-indexed");
            assertTrue(step.getActiveLine() > 0, "Active line should be positive");
            assertNotNull(step.getDescription());
        }
    }

    @Test
    @DisplayName("Should generate valid execution steps for DFS Traversal")
    void testGenerateDfsSteps() {
        List<ExecutionStep> steps = service.generateSteps("dfs-traversal");
        assertNotNull(steps);
        assertTrue(steps.size() >= 5);
        assertEquals("Stack", steps.get(0).getDsType());
    }

    @Test
    @DisplayName("Should generate execution steps for grid-based problems (Rotting Oranges, Flood Fill)")
    void testGridProblemSteps() {
        List<ExecutionStep> orangesSteps = service.generateSteps("rotting-oranges");
        assertNotNull(orangesSteps);
        assertNotNull(orangesSteps.get(0).getGridState(), "Rotting oranges step should contain 2D grid state");

        List<ExecutionStep> floodFillSteps = service.generateSteps("flood-fill");
        assertNotNull(floodFillSteps);
        assertNotNull(floodFillSteps.get(0).getGridState(), "Flood fill step should contain 2D grid state");
    }

    @Test
    @DisplayName("Should generate execution steps for cycle detection")
    void testCycleDetectionSteps() {
        List<ExecutionStep> undirectedBfs = service.generateSteps("undirected-cycle-bfs");
        assertNotNull(undirectedBfs);
        assertTrue(undirectedBfs.stream().anyMatch(s -> s.getDescription().contains("CYCLE DETECTED")), "Should flag detected cycle step");

        List<ExecutionStep> directedDfs = service.generateSteps("directed-cycle-dfs");
        assertNotNull(directedDfs);
        assertTrue(directedDfs.stream().anyMatch(s -> s.getDescription().contains("DIRECTED CYCLE DETECTED")), "Should flag directed cycle step");
    }
}
