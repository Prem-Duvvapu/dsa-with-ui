package com.dsa.ui.algorithm;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.service.AdvancedGraphService;
import com.dsa.ui.service.GraphBfsDfsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GraphTracingTest {

    private GraphBfsDfsService bfsDfsService;
    private AdvancedGraphService advancedGraphService;

    @BeforeEach
    void setUp() {
        bfsDfsService = new GraphBfsDfsService();
        advancedGraphService = new AdvancedGraphService();
    }

    @Test
    void testBfsTraversalTracing() {
        List<ExecutionStep> steps = bfsDfsService.generateSteps("bfs-traversal");
        assertNotNull(steps);
        assertTrue(steps.size() >= 8, "BFS Traversal should have >=8 steps, actual: " + steps.size());
    }

    @Test
    void testDfsTraversalTracing() {
        List<ExecutionStep> steps = bfsDfsService.generateSteps("dfs-traversal");
        assertNotNull(steps);
        assertTrue(steps.size() >= 8, "DFS Traversal should have >=8 steps, actual: " + steps.size());
    }

    @Test
    void testRottingOrangesTracing() {
        List<ExecutionStep> steps = bfsDfsService.generateSteps("rotting-oranges");
        assertNotNull(steps);
        assertTrue(steps.size() >= 5, "Rotting Oranges should have >=5 steps, actual: " + steps.size());
    }

    @Test
    void testDijkstraTracing() {
        List<ExecutionStep> steps = advancedGraphService.generateSteps("dijkstra-min-heap");
        assertNotNull(steps);
        assertTrue(steps.size() >= 5, "Dijkstra should have >=5 steps, actual: " + steps.size());
    }
}
