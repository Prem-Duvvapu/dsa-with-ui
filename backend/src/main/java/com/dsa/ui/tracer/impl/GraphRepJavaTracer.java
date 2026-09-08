package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Builds Java's list-of-lists adjacency representation one edge at a time. */
@Component
public class GraphRepJavaTracer implements AlgorithmTracer {
    @Override public String id() { return "graph-rep-java"; }
    @Override public DsType dsType() { return DsType.GRAPH; }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("graph", FieldType.GRAPH)
                .label("Undirected graph")
                .help("Build an ArrayList bucket for every vertex, then add both directions.")
                .constraint("maxVertices", 16).constraint("maxEdges", 40)
                .defaultValue(Map.of("vertices", 4, "edges", List.of(
                        List.of(0, 1), List.of(1, 2), List.of(2, 3), List.of(0, 3))))
                .build());
    }

    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of("vertices", 6, "edges", List.of(
                List.of(0, 1), List.of(0, 2), List.of(0, 3),
                List.of(3, 4), List.of(3, 5))));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> buildAdj(int vertices, int[][] edges) {
                   // @a outer
                   List<List<Integer>> adj = new ArrayList<>();
                   for (int node = 0; node < vertices; node++) {
                       // @a bucket
                       adj.add(new ArrayList<>());
                   }
                   for (int[] edge : edges) {
                       // @a add
                       adj.get(edge[0]).add(edge[1]);
                       adj.get(edge[1]).add(edge[0]);
                   }
                   // @a done
                   return adj;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adjacency = new ArrayList<>();
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < graph.vertices(); i++) states.put(i, "unvisited");

        emit.at("outer").say("Create the outer ArrayList that will hold %d vertex buckets.", graph.vertices())
                .var("bucketCount", 0).graph(graph).nodes(states).step();

        for (int node = 0; node < graph.vertices(); node++) {
            adjacency.add(new ArrayList<>());
            states.put(node, "visited");
            emit.at("bucket").say("Add bucket adj[%d]. The outer list now has %d entries.", node, adjacency.size())
                    .var("bucketCount", adjacency.size()).var("adj", adjacency)
                    .graph(graph).nodes(states).step();
        }

        for (int[] edge : graph.edges()) {
            int u = edge[0], v = edge[1];
            adjacency.get(u).add(v);
            adjacency.get(v).add(u);
            states.put(u, "visiting");
            states.put(v, "visiting");
            emit.at("add").say("Store edge %d-%d in both Java lists: adj[%d] and adj[%d].", u, v, u, v)
                    .var("adj", adjacency).graph(graph).nodes(states).edges(List.of(u + "-" + v)).step();
            states.put(u, "visited");
            states.put(v, "visited");
        }

        emit.at("done").say("Java adjacency list complete: %s.", adjacency)
                .var("adj", adjacency).graph(graph).nodes(states).step();
    }
}
