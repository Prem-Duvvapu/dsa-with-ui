package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.AdvancedGraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedGraphServiceTest {

    private AdvancedGraphService service;

    @BeforeEach
    void setUp() {
        service = new AdvancedGraphService();
    }

    @Test
    @DisplayName("Should load all 10 Striver A2Z Advanced Graph problems")
    void testGetAllProblems() {
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(10, problems.size(), "Should load 10 Advanced Graph problems");
    }

    @Test
    @DisplayName("Should retrieve Dijkstra and Kruskal MST problem details")
    void testGetProblemById() {
        ProblemDetail dijkstra = service.getProblemById("dijkstra-min-heap");
        assertNotNull(dijkstra);
        assertEquals("Dijkstra's Shortest Path Algorithm", dijkstra.getTitle());
        assertEquals("O(E log V)", dijkstra.getComplexity().getTimeComplexity());

        ProblemDetail kruskal = service.getProblemById("kruskals-mst");
        assertNotNull(kruskal);
        assertEquals("Kruskal's MST (Disjoint Set / Union-Find)", kruskal.getTitle());
    }

    @Test
    @DisplayName("Should generate execution steps for Topo Sort, Dijkstra, and Tarjan's Bridges")
    void testGenerateSteps() {
        List<ExecutionStep> topoSteps = service.generateSteps("topo-sort-dfs");
        assertNotNull(topoSteps);

        List<ExecutionStep> dijkstraSteps = service.generateSteps("dijkstra-min-heap");
        assertNotNull(dijkstraSteps);
        assertEquals("PriorityQueue", dijkstraSteps.get(0).getDsType());

        List<ExecutionStep> bridgeSteps = service.generateSteps("tarjan-bridges");
        assertNotNull(bridgeSteps);
        assertTrue(bridgeSteps.stream().anyMatch(s -> s.getDescription().contains("CRITICAL BRIDGE")));
    }
}
