package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Walks a caller-supplied graph to make vertices, edges, and degree concrete. */
@Component
public class GraphIntroTracer implements AlgorithmTracer {

    @Override public String id() { return "graph-intro"; }
    @Override public DsType dsType() { return DsType.GRAPH; }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("graph", FieldType.GRAPH)
                .label("Undirected graph")
                .help("Vertex count plus edges [from, to]. The trace inspects every adjacency.")
                .constraint("maxVertices", 16).constraint("maxEdges", 40)
                .defaultValue(Map.of("vertices", 5, "edges", List.of(
                        List.of(0, 1), List.of(0, 2), List.of(1, 2),
                        List.of(2, 3), List.of(3, 4))))
                .build());
    }

    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of("vertices", 6, "edges", List.of(
                List.of(0, 1), List.of(1, 2), List.of(2, 0),
                List.of(3, 4), List.of(4, 5))));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] degrees(int vertices, List<List<Integer>> adjacency) {
                   // @a init
                   int[] degree = new int[vertices];
                   for (int vertex = 0; vertex < vertices; vertex++) {
                       // @a vertex
                       degree[vertex] = adjacency.get(vertex).size();
                       for (int neighbour : adjacency.get(vertex)) {
                           // @a edge
                           inspect(vertex, neighbour);
                       }
                   }
                   // @a done
                   return degree;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adjacency = graph.adjacency();
        Map<Integer, String> states = states(graph.vertices(), "unvisited");
        int[] degree = new int[graph.vertices()];
        int adjacencyEntries = 0;

        emit.at("init").say("A graph has %d vertices and %d undirected edges. Inspect its adjacency list.",
                        graph.vertices(), graph.edges().length)
                .var("vertices", graph.vertices()).var("edges", graph.edges().length)
                .graph(graph).nodes(states).step();

        for (int vertex = 0; vertex < graph.vertices(); vertex++) {
            states.put(vertex, "visiting");
            degree[vertex] = adjacency.get(vertex).size();
            emit.at("vertex").say("Vertex %d has degree %d: %s.", vertex, degree[vertex], adjacency.get(vertex))
                    .var("vertex", vertex).var("degree", degree[vertex])
                    .graph(graph).nodes(states).step();

            for (int neighbour : adjacency.get(vertex)) {
                adjacencyEntries++;
                emit.at("edge").say("Adjacency %d -> %d is one endpoint view of an undirected edge.",
                                vertex, neighbour)
                        .var("from", vertex).var("to", neighbour)
                        .graph(graph).nodes(states).edges(List.of(vertex + "-" + neighbour)).step();
            }
            states.put(vertex, "visited");
        }

        emit.at("done").say("Inspected %d adjacency entries: twice the %d undirected edges. Degrees: %s.",
                        adjacencyEntries, graph.edges().length, Arrays.toString(degree))
                .var("degrees", Arrays.toString(degree)).graph(graph).nodes(states).step();
    }

    private static Map<Integer, String> states(int vertices, String state) {
        Map<Integer, String> out = new LinkedHashMap<>();
        for (int i = 0; i < vertices; i++) out.put(i, state);
        return out;
    }
}
