package com.dsa.ui.tracer;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.GraphEdge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest
class GraphTopologyTraceTest {

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @ParameterizedTest(name = "{0} topology follows alternate caller input")
    @ValueSource(strings = {"bfs-traversal", "dfs-traversal", "dijkstra-min-heap"})
    @DisplayName("Every graph step carries topology derived from caller input")
    void everyGraphStepCarriesCallerTopology(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        ExecutionTrace defaults = runner.runDefaults(tracer);
        ExecutionTrace alternate = runner.run(tracer, tracer.alternateInput());

        assertTopologyMatchesInput(defaults);
        assertTopologyMatchesInput(alternate);

        assertFalse(edgeSignatures(defaults.getSteps().get(0)).equals(
                        edgeSignatures(alternate.getSteps().get(0))),
                id + " emitted the same topology for materially different graph inputs");
    }

    private static void assertTopologyMatchesInput(ExecutionTrace trace) {
        @SuppressWarnings("unchecked")
        Map<String, Object> graph = (Map<String, Object>) trace.getResolvedInput().get("graph");
        int vertexCount = ((Number) graph.get("vertices")).intValue();
        @SuppressWarnings("unchecked")
        List<List<Number>> inputEdges = (List<List<Number>>) graph.get("edges");

        ExecutionStep first = trace.getSteps().get(0);
        assertEquals(vertexCount, first.getGraphNodes().size());
        assertEquals(inputEdges.stream().map(GraphTopologyTraceTest::signature).toList(),
                edgeSignatures(first));

        List<Integer> expectedIds = new ArrayList<>(vertexCount);
        for (int id = 0; id < vertexCount; id++) {
            expectedIds.add(id);
        }
        assertEquals(expectedIds,
                first.getGraphNodes().stream().map(node -> node.getId()).toList());

        for (ExecutionStep step : trace.getSteps()) {
            assertSame(first.getGraphNodes(), step.getGraphNodes(),
                    trace.getProblemId() + " rebuilt or omitted nodes on step " + step.getStepNumber());
            assertSame(first.getGraphEdges(), step.getGraphEdges(),
                    trace.getProblemId() + " rebuilt or omitted edges on step " + step.getStepNumber());
        }
    }

    private static List<String> edgeSignatures(ExecutionStep step) {
        return step.getGraphEdges().stream().map(GraphTopologyTraceTest::signature).toList();
    }

    private static String signature(List<Number> edge) {
        return edge.size() == 3
                ? edge.get(0) + "-" + edge.get(1) + "@" + edge.get(2)
                : edge.get(0) + "-" + edge.get(1);
    }

    private static String signature(GraphEdge edge) {
        return edge.getWeight() == null
                ? edge.getFrom() + "-" + edge.getTo()
                : edge.getFrom() + "-" + edge.getTo() + "@" + edge.getWeight();
    }
}
