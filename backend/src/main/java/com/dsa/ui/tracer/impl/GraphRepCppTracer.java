package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Builds the adjacency-list representation shown in the catalogue's C++ lesson. */
@Component
public class GraphRepCppTracer implements AlgorithmTracer {
    @Override public String id() { return "graph-rep-cpp"; }
    @Override public DsType dsType() { return DsType.GRAPH; }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("graph", FieldType.GRAPH)
                .label("Undirected graph")
                .help("Each edge is inserted into both endpoints' C++ adjacency vectors.")
                .constraint("maxVertices", 16).constraint("maxEdges", 40)
                .defaultValue(Map.of("vertices", 5, "edges", List.of(
                        List.of(0, 1), List.of(0, 4), List.of(1, 2),
                        List.of(1, 3), List.of(3, 4))))
                .build());
    }

    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of("vertices", 6, "edges", List.of(
                List.of(0, 2), List.of(2, 4), List.of(4, 5),
                List.of(1, 3), List.of(3, 5), List.of(0, 5))));
    }

    @Override
    public String annotatedCode() {
        return """
               vector<vector<int>> buildAdj(int V, vector<pair<int,int>> edges) {
                   // @a allocate
                   vector<vector<int>> adj(V);
                   for (auto [u, v] : edges) {
                       // @a forward
                       adj[u].push_back(v);
                       // @a reverse
                       adj[v].push_back(u);
                   }
                   // @a done
                   return adj;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> built = emptyAdjacency(graph.vertices());
        Map<Integer, String> states = nodeStates(graph.vertices());

        emit.at("allocate").say("Allocate %d empty vectors: one adjacency bucket per vertex.", graph.vertices())
                .var("adj", built).graph(graph).nodes(states).step();

        for (int[] edge : graph.edges()) {
            int u = edge[0], v = edge[1];
            states.put(u, "visiting");
            built.get(u).add(v);
            emit.at("forward").say("Edge %d-%d: push %d into adj[%d].", u, v, v, u)
                    .var("adj", built).graph(graph).nodes(states).edges(List.of(u + "-" + v)).step();

            states.put(v, "visiting");
            built.get(v).add(u);
            emit.at("reverse").say("Undirected means the reverse lookup too: push %d into adj[%d].", u, v)
                    .var("adj", built).graph(graph).nodes(states).edges(List.of(u + "-" + v)).step();
            states.put(u, "visited");
            states.put(v, "visited");
        }

        emit.at("done").say("Every edge was stored twice. Final C++ adjacency vectors: %s.", built)
                .var("adj", built).graph(graph).nodes(states).step();
    }

    private static List<List<Integer>> emptyAdjacency(int n) {
        List<List<Integer>> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new ArrayList<>());
        return out;
    }

    private static Map<Integer, String> nodeStates(int n) {
        Map<Integer, String> out = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) out.put(i, "unvisited");
        return out;
    }
}
