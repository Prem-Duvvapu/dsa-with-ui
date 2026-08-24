package com.dsa.ui.tracer;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Inputs.GraphInput#weightedAdjacency}. Plain {@code adjacency()} already has
 * coverage through the BFS/DFS tracers; this is the new weighted variant, added for
 * Dijkstra and every shortest-path tracer after it.
 */
class InputsTest {

    private static InputSpec graphSpec(boolean weighted) {
        InputField.Builder builder = InputField.of("graph", FieldType.GRAPH).label("Graph");
        if (weighted) builder.weighted();
        return InputSpec.of(builder.defaultValue(Map.of("vertices", 1, "edges", List.of())).build());
    }

    private Inputs.GraphInput graphOf(boolean weighted, int vertices, List<List<Integer>> edges) {
        InputSpec spec = graphSpec(weighted);
        Inputs in = InputValidator.validate(spec, Map.of("graph", Map.of("vertices", vertices, "edges", edges)));
        return in.getGraph("graph");
    }

    @Test
    void undirectedWeightedEdgeAppearsOnBothEndpoints() {
        Inputs.GraphInput graph = graphOf(true, 3, List.of(List.of(0, 1, 5)));
        List<List<Inputs.GraphInput.Neighbor>> adj = graph.weightedAdjacency();

        assertEquals(1, adj.get(0).size());
        assertEquals(new Inputs.GraphInput.Neighbor(1, 5), adj.get(0).get(0));
        assertEquals(1, adj.get(1).size());
        assertEquals(new Inputs.GraphInput.Neighbor(0, 5), adj.get(1).get(0));
        assertTrue(adj.get(2).isEmpty());
    }

    @Test
    void directedWeightedEdgeAppearsOnlyOnTheSourceEndpoint() {
        Inputs.GraphInput graph = graphOf(true, 2, List.of(List.of(0, 1, 7)));
        List<List<Inputs.GraphInput.Neighbor>> adj = graph.weightedAdjacency(true);

        assertEquals(1, adj.get(0).size());
        assertEquals(new Inputs.GraphInput.Neighbor(1, 7), adj.get(0).get(0));
        assertTrue(adj.get(1).isEmpty());
    }

    @Test
    void multipleEdgesFromTheSameVertexAllAppear() {
        Inputs.GraphInput graph = graphOf(true, 3, List.of(List.of(0, 1, 2), List.of(0, 2, 9)));
        List<List<Inputs.GraphInput.Neighbor>> adj = graph.weightedAdjacency();

        assertEquals(2, adj.get(0).size());
    }

    @Test
    void rejectsUnweightedEdgesRatherThanDefaultingTheWeight() {
        // A two-element edge only reaches weightedAdjacency() if the field wasn't declared
        // .weighted(), or a caller bypasses InputValidator. Either way, silently treating
        // it as weight 1 would make a shortest-path tracer quietly wrong.
        Inputs.GraphInput graph = new Inputs.GraphInput(2, new int[][]{{0, 1}});
        IllegalStateException ex = assertThrows(IllegalStateException.class, graph::weightedAdjacency);
        assertTrue(ex.getMessage().contains("weighted"));
    }
}
