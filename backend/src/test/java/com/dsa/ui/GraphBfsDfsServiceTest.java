package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.GraphBfsDfsService;
import com.dsa.ui.service.LegacyTraceRetiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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
    @DisplayName("Should generate valid execution steps for all 11 Graph BFS/DFS problems")
    void testGenerateSteps() {
        // Ids with real tracers refuse the legacy path rather than serve a substitute.
        Set<String> retired = Set.of(
                "bfs-traversal", "dfs-traversal", "number-of-provinces", "rotting-oranges",
                "undirected-cycle-bfs", "undirected-cycle-dfs", "directed-cycle-dfs",
                "distance-nearest-1");
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

            for (int i = 0; i < steps.size(); i++) {
                ExecutionStep step = steps.get(i);
                assertEquals(i + 1, step.getStepNumber(), p.getId() + " step numbers should be sequentially 1-indexed");
                assertTrue(step.getActiveLine() > 0, p.getId() + " active line should be positive");
                assertNotNull(step.getDescription(), p.getId() + " should have a description");
            }
        }
    }

    @Test
    @DisplayName("Should generate execution steps for grid-based problems still on the legacy path (Flood Fill)")
    void testGridProblemSteps() {
        List<ExecutionStep> floodFillSteps = service.generateSteps("flood-fill");
        assertNotNull(floodFillSteps);
        assertNotNull(floodFillSteps.get(0).getGridState(), "Flood fill step should contain 2D grid state");
    }
}
