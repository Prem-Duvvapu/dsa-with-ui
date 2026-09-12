package com.dsa.ui.algorithm;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.service.AdvancedGraphService;
import com.dsa.ui.service.GraphBfsDfsService;
import com.dsa.ui.service.LegacyTraceRetiredException;
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
        // Real tracer now serves this id on /api/problems; the legacy path refuses rather
        // than falling back to a substitute trace.
        assertThrows(LegacyTraceRetiredException.class,
                () -> bfsDfsService.generateSteps("bfs-traversal"));
    }

    @Test
    void testDfsTraversalTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> bfsDfsService.generateSteps("dfs-traversal"));
    }

    @Test
    void testRottingOrangesTracing() {
        assertThrows(LegacyTraceRetiredException.class,
                () -> bfsDfsService.generateSteps("rotting-oranges"));
    }

    @Test
    void testDijkstraTracing() {
        // dijkstra-min-heap is traced by the v2 layer, so the legacy generator is retired.
        // It previously fell through AdvancedGraphService's default: and returned the
        // graph-intro animation, which this assertion could not tell apart from Dijkstra.
        assertThrows(LegacyTraceRetiredException.class,
                () -> advancedGraphService.generateSteps("dijkstra-min-heap"));
    }
}
