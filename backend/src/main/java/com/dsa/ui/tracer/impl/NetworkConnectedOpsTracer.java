package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Every existing cable that is redundant (both its endpoints already reachable from each
 * other) is exactly a spare cable available to bridge a disconnected component elsewhere -
 * DSU counts both quantities at once. If there are fewer cables than the n-1 a spanning tree
 * needs, no rewiring can possibly succeed regardless of which cables are moved.
 */
@Component
public class NetworkConnectedOpsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "network-connected-ops";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Computers and cables")
                        .help("Vertex count is the number of computers; edges are existing direct cables.")
                        .constraint("maxVertices", 20)
                        .constraint("maxEdges", 48)
                        .defaultValue(Map.of(
                                "vertices", 4,
                                "edges", List.of(List.of(0, 1), List.of(0, 2), List.of(1, 2))))
                        .build());
    }

    /** Too few cables to ever connect every computer, however they are rearranged. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 6,
                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 3))));
    }

    @Override
    public String annotatedCode() {
        return """
               public int makeConnected(int n, int[][] connections) {
                   if (connections.length < n - 1) {
                       // @a insufficient
                       return -1;
                   }
                   int[] parent = new int[n];
                   for (int i = 0; i < n; i++) parent[i] = i;

                   for (int[] c : connections) {
                       // @a union
                       int ru = find(c[0], parent), rv = find(c[1], parent);
                       if (ru != rv) parent[ru] = rv;
                   }

                   // @a count
                   Set<Integer> roots = new HashSet<>();
                   for (int i = 0; i < n; i++) roots.add(find(i, parent));

                   // @a done
                   return roots.size() - 1;
               }

               private int find(int x, int[] parent) {
                   if (parent[x] != x) parent[x] = find(parent[x], parent);
                   return parent[x];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int n = graph.vertices();
        int[][] connections = graph.edges();

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) states.put(i, "unvisited");

        if (connections.length < n - 1) {
            emit.at("insufficient")
                    .say("%d computers need at least %d cables to ever be fully connected, but only %d exist. Answer: -1.",
                            n, n - 1, connections.length)
                    .var("answer", -1).graph(graph).nodes(states).step();
            return;
        }

        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        for (int[] c : connections) {
            int u = c[0], node = c[1];
            String edgeKey = u + "-" + node;
            int ru = find(u, parent);
            int rv = find(node, parent);

            if (ru == rv) {
                emit.at("union").say("%d and %d are already connected - this cable is redundant (a spare).", u, node)
                        .var("edge", edgeKey).graph(graph).nodes(states).edges(List.of(edgeKey)).step();
            } else {
                parent[ru] = rv;
                states.put(u, "visited");
                states.put(node, "visited");
                emit.at("union").say("Connect %d and %d - merge their components.", u, node)
                        .var("edge", edgeKey).graph(graph).nodes(states).edges(List.of(edgeKey)).step();
            }
        }

        Set<Integer> roots = new HashSet<>();
        for (int i = 0; i < n; i++) roots.add(find(i, parent));
        emit.at("count").say("Distinct components after union: %d (roots %s).", roots.size(), roots)
                .var("components", roots.size()).graph(graph).nodes(states).step();

        int answer = roots.size() - 1;
        emit.at("done").say("%d components need %d cable move(s) to merge into one connected network.",
                        roots.size(), answer)
                .var("answer", answer).graph(graph).nodes(states).step();
    }

    private static int find(int x, int[] parent) {
        if (parent[x] != x) {
            parent[x] = find(parent[x], parent);
        }
        return parent[x];
    }
}
